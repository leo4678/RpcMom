package com.alibaba.middleware.race.mom.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.alibaba.middleware.race.mom.converter.ObjectDecoder;
import com.alibaba.middleware.race.mom.converter.ObjectEncoder;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月16日下午9:05:08
 *
 * @description 
 **/

public class BrokerSimpleNettyChannelPool {

	//链路通道映射，链路以IP地址:端口号唯一确认
	private static ConcurrentHashMap<String, LinkedBlockingQueue<Channel>> routeChannelsPool;
	//
	private static Bootstrap bootstrap;
	private static EventLoopGroup group;
	private final String SEGMENT = ":";
	private int timeout;
	private int maxConn;
	private boolean forbidForceConnect;
	//可用核心数
	private final static int availableProcessors;
	
	static{
		
		availableProcessors = Runtime.getRuntime().availableProcessors();
		bootstrap = new Bootstrap();
		bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		group =  new NioEventLoopGroup(availableProcessors);
		bootstrap.group(group);
		bootstrap.channel(NioSocketChannel.class)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.handler(new ChannelInitializer(){

			@Override
			protected void initChannel(Channel ch) throws Exception {
				// TODO 自动生成的方法存根
				ch.pipeline().addLast("decoder", new ObjectDecoder());
				ch.pipeline().addLast("encoder", new ObjectEncoder());
				ch.pipeline().addLast("handler",new BrokerSimpleNettyChannelPoolHandler(BrokerNettyClientProxy.getSimpleNettyChannelPool()));
				ch.pipeline().addLast(
						IdleStateHandler.class.getSimpleName(),
						new IdleStateHandler(0, 0, 10, TimeUnit.SECONDS));
			}
		});
		routeChannelsPool = new ConcurrentHashMap<String, LinkedBlockingQueue<Channel>>();
	}
	
	/**
	 * 
	 */
	public BrokerSimpleNettyChannelPool(int maxCoon,int timeout,boolean forbidForceConnect) {
		// TODO 自动生成的构造函数存根
		this.maxConn = maxCoon;
		this.timeout = timeout;
		this.forbidForceConnect = forbidForceConnect;
	}
	
	
	public BrokerSimpleNettyResponseFuture sendRequest(InetSocketAddress remoteAddr,Object request) throws InterruptedException, IOException{
		BrokerSimpleNettyResponseFuture responseFuture = new BrokerSimpleNettyResponseFuture();
		
		if(sendRequestByPooledChannel(remoteAddr,request,responseFuture,false)){
			//System.out.println("使用缓存的通道");
			return responseFuture;	
		}
		
		if(sendRequestByNewChannel(remoteAddr, request, responseFuture, forbidForceConnect)){
			//System.out.println("使用新建的通道");
			return responseFuture;
		}
		
		if(sendRequestByPooledChannel(remoteAddr,request,responseFuture,true)){
			//System.out.println("使用新建的通道");
			return responseFuture;
		}
		
		throw new IOException("send request fail");
	}
	
	private boolean sendRequestByPooledChannel(InetSocketAddress remoteAddr,Object request,
			BrokerSimpleNettyResponseFuture responseFuture,boolean waiting) throws InterruptedException{
		LinkedBlockingQueue<Channel> channels = routeChannels(remoteAddr);
		Channel channel = channels.poll();
		while (null != channel && !channel.isWritable()) {
			channel = channels.poll();
		}
		if(null == channel || !channel.isWritable()){
			if(!waiting){
				return false;
			}
			channel = channels.poll(timeout, TimeUnit.MILLISECONDS);
			if(null == channel || !channel.isWritable()){
				return false;
			}
		}
		
		BrokerSimpleNettyResponseFutureUtil.attributeSimpleResponseFuture(channel, responseFuture);
		channel.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		return true;
	}
	
	public boolean sendRequestByNewChannel(InetSocketAddress remoteAddr,Object request,
			BrokerSimpleNettyResponseFuture responseFuture, boolean forceConnect) throws InterruptedException{
		ChannelFuture future = createChannelFuture(remoteAddr, forceConnect);
		if(null != future){
			Channel channel = future.sync().channel();
			BrokerSimpleNettyResponseFutureUtil.attributeSimpleResponseFuture(channel, responseFuture);
			BrokerSimpleNettyResponseFutureUtil.attributeRoute(channel, remoteAddr);
			channel.writeAndFlush(request);
			future.addListener(new ChannelFutureListener(){

				public void operationComplete(ChannelFuture arg0)
						throws Exception {
					// TODO 自动生成的方法存根
					if(arg0.isSuccess()){
						arg0.channel().closeFuture().addListener(new ChannelFutureListener(){

							public void operationComplete(ChannelFuture arg0)
									throws Exception {
								// TODO 自动生成的方法存根
								removeChannel(arg0.channel(),arg0.cause());
							}
						});
					}
				}
			});
			return true;
		}
		return false;
	}
	
	public void close()throws InterruptedException{
		ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		for(LinkedBlockingQueue<Channel> pools : routeChannelsPool.values()){
			for(Channel channel : pools){
				removeChannel(channel, null);
                channelGroup.add(channel);
			}
		}
		channelGroup.close();
		group.shutdownGracefully();
	}
	
	private void removeChannel(Channel channel, Throwable cause){
		InetSocketAddress route = (InetSocketAddress) channel.remoteAddress();
		BrokerSimpleNettyResponseFutureUtil.cancel(channel, cause);
		if(!BrokerSimpleNettyResponseFutureUtil.getForceConnect(channel)){
			LinkedBlockingQueue<Channel> channels = routeChannels(route);
			channels.remove(channel);
		}
	}
	
	public void returnChannel(Channel channel){
		if(BrokerSimpleNettyResponseFutureUtil.getForceConnect(channel)){
			return;
		}
		if(null != channel && channel.isActive()){
			InetSocketAddress route = BrokerSimpleNettyResponseFutureUtil.getRoute(channel);
			LinkedBlockingQueue<Channel> poolChannels = routeChannels(route);
			poolChannels.offer(channel);
		}
	}
	
	private ChannelFuture createChannelFuture(InetSocketAddress remoteAddr,boolean forceConnect){
		LinkedBlockingQueue<Channel> poolChannels = routeChannels(remoteAddr);
		if(poolChannels.size() == maxConn){
			return null;
		}
		ChannelFuture future = bootstrap.connect(remoteAddr);
		if(null != future){
			if(forceConnect){
				BrokerSimpleNettyResponseFutureUtil.attributeForceConnect(future.channel(), forceConnect);
			}
			return future;
		}
		return null;
	}
	
	private String getKey(InetSocketAddress remoteAddr){
		return remoteAddr.getHostString() + SEGMENT + remoteAddr.getPort();
	}
	
	private LinkedBlockingQueue<Channel> routeChannels(InetSocketAddress remoteAddr){
		String key = getKey(remoteAddr);
		LinkedBlockingQueue<Channel> poolChannels = routeChannelsPool.get(key);
		
		if(null == poolChannels){
			poolChannels = new LinkedBlockingQueue<Channel>();
			routeChannelsPool.put(key, poolChannels);
		}
		return poolChannels;
	}
}
