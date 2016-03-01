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
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.alibaba.middleware.race.mom.converter.ObjectDecoder;
import com.alibaba.middleware.race.mom.converter.ObjectEncoder;



/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午4:04:34
 *
 * @description 
 **/

public class ProducerSimpleNettyChannelPool {
	
	//远程地址
	private SocketAddress remoteAddress;
	//通道队列
	private static LinkedBlockingQueue<Channel> channelsPool;
	//最大连接数
	private int maxConnNum;
	//超时(毫秒)
	private  int timeout;
	//空闲时间
	private static int idleTime;
	//空闲时间单位
	private static TimeUnit timeUnit;
	//工作组
	private static EventLoopGroup group;
	//Bootstrap
	private static Bootstrap bootstrap = null;
	//
	private boolean forbidForceConnect;
	//可用核心数
	private final static int availableProcessors;
	
	static {
		availableProcessors = Runtime.getRuntime().availableProcessors();
		idleTime = 10;
		timeUnit = TimeUnit.SECONDS;
		bootstrap = new Bootstrap();
		bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		group =  new NioEventLoopGroup(availableProcessors * 2);
		bootstrap.group(group);
		channelsPool = new LinkedBlockingQueue<Channel>();
		bootstrap.channel(NioSocketChannel.class)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.handler(new ChannelInitializer() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				// TODO 自动生成的方法存根
				ch.pipeline().addLast("decoder", new ObjectDecoder());
				ch.pipeline().addLast("encoder", new ObjectEncoder());
				ch.pipeline().addLast("handler",new ProducerSimpleNettyChannelPoolHandler(ProducerNettyClient.getSimpleNettyChannelPool()));
						ch.pipeline().addLast(
								IdleStateHandler.class.getSimpleName(),
								new IdleStateHandler(0, 0, idleTime, timeUnit));
			}
		});
	}

	public ProducerSimpleNettyChannelPool(SocketAddress remoteAddress, int maxConnNum,
			int idleTime, TimeUnit timeUnit, int timeout) {
		this.remoteAddress = remoteAddress;
		this.maxConnNum = maxConnNum;
		this.timeout = timeout;
	}
	
	public ProducerSimpleNettyResponseFuture sendRequest(Object request) throws InterruptedException,IOException{
		ProducerSimpleNettyResponseFuture responseFuture = new ProducerSimpleNettyResponseFuture();
		if(sendRequestByPooledChannel(request, responseFuture, false)){
			return responseFuture;
		}
		if(sendRequestByNewChannel(request, responseFuture, forbidForceConnect)){
			return responseFuture;
		}
		if(sendRequestByPooledChannel(request, responseFuture, true)){
			return responseFuture;
		}
		throw new IOException("send request fail");
	}
	
	private boolean sendRequestByPooledChannel(Object request,
			ProducerSimpleNettyResponseFuture responseFuture,boolean waiting) throws InterruptedException{
		Channel channel = channelsPool.poll();
		//寻找可用的channel
		while(null != channel && !channel.isActive()){
			channel = channelsPool.poll();
		}
		//没有找到channel或者最后一个channel非激活
		if(null == channel || !channel.isActive()){
			if(!waiting){
				return false;
			}
			channel = channelsPool.poll(idleTime, timeUnit);
			if(null == channel || !channel.isActive()){
				return false;
			}
		}
		ProducerSimpleNettyResponseFutureUtil.attributeSimpleResponseFuture(channel, responseFuture);
		channel.writeAndFlush(request).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		return true;
	}

	private boolean sendRequestByNewChannel(Object request,
			ProducerSimpleNettyResponseFuture responseFuture, boolean forceConnect) throws InterruptedException {
		ChannelFuture future = createChannelFuture(forceConnect);
		if(null != future){
			Channel channel = future.sync().channel();
			ProducerSimpleNettyResponseFutureUtil.attributeSimpleResponseFuture(channel, responseFuture);
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
	
	public void close() throws InterruptedException {
		ChannelGroup channelGroup = new DefaultChannelGroup(
				GlobalEventExecutor.INSTANCE);
		for (Channel channel : channelsPool) {
			removeChannel(channel, null);
			channelGroup.add(channel);
		}
		channelGroup.close().sync();
		//group.shutdownGracefully();
	}
	
	public void returnChannel(Channel channel){
		if(ProducerSimpleNettyResponseFutureUtil.getForceConnect(channel)){
			return;
		}
		if(null != channel && channel.isActive()){
			channelsPool.offer(channel);
		}
	}
	
	private void removeChannel(Channel channel, Throwable cause) {
		ProducerSimpleNettyResponseFutureUtil.cancel(channel, cause);
		if(!ProducerSimpleNettyResponseFutureUtil.getForceConnect(channel)){
			channelsPool.remove(channel);
		}
	}
	
	private ChannelFuture createChannelFuture(boolean forceConnect){
		if(channelsPool.size() == maxConnNum){
			return null;
		}
		ChannelFuture future = bootstrap.connect(remoteAddress);
		if (null != future) {
			if(forceConnect){
				ProducerSimpleNettyResponseFutureUtil.attributeForceConnect(
						future.channel(), forceConnect);
			}
			return future;
		}
		return null;
	}
}
