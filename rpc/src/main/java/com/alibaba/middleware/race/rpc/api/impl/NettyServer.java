package com.alibaba.middleware.race.rpc.api.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.RepaintManager;

import com.alibaba.middleware.race.rpc.context.RpcAddContext;
import com.alibaba.middleware.race.rpc.context.RpcAddContextImpl;
import com.alibaba.middleware.race.rpc.context.RpcContextRequest;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.util.ObjectDecoder;
import com.alibaba.middleware.race.rpc.util.ObjectEncoder;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentEncoder.Result;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author tokysky (HIT-CS-ICES)
 * @time 于2015年7月31日上午10:23:31
 *
 * @description
 **/

public class NettyServer {

	private int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap bootstrap;
	//private ExecutorService service;

	public NettyServer(int port) {
		this.port = port;
		bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() / 2);
		workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
		try {
			bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel arg0)
								throws Exception {
							// TODO 自动生成的方法存根
							// 字符串解码 和 编码
							// arg0.pipeline().addLast("decoder", new
							// StringDecoder());
							// arg0.pipeline().addLast("encoder", new
							// StringEncoder());
							arg0.pipeline().addLast("decoder",
									new ObjectDecoder());
							arg0.pipeline().addLast("encoder",
									new ObjectEncoder());
							arg0.pipeline().addLast("handler",
									new NettyServerHandler());
						}
					}).option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
			ChannelFuture future = bootstrap.bind(port).sync();
			future.channel().closeFuture().sync();
			
		} catch (InterruptedException e) {
			// TODO: handle exception
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}

	}

	private class NettyServerHandler extends
			SimpleChannelInboundHandler<Object> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			// TODO 自动生成的方法存根
			//System.out.println("RPC Provider Message:Netty server channel start active.");
		}

		@Override
		protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
				throws Exception {
			if (arg1 instanceof RpcRequest) {
				RpcRequest request = (RpcRequest) arg1;
				Object interfaceImpl = RpcProviderImpl.serviceEngine
						.get(request.getInterfaceName());
				RpcResponse response = new RpcResponse();
				Object appResponse;
				if(interfaceImpl == null){
					//异常
					response.setErrorMsg("E");	
					response.setAppResponse(new Boolean(true));
					arg0.writeAndFlush(response);
					return;
				}
				//获得方法
				Method method = interfaceImpl.getClass().getMethod(
						request.getMethodName());
				//获得参数
				if(request.getParams().length==1 && request.getParams()[0].equals("NULL")){
					try {
						appResponse = method.invoke(interfaceImpl, null);
						response.setErrorMsg("S");
						response.setAppResponse(appResponse);
					} catch (InvocationTargetException e) {
						// TODO: handle exception
						response.setErrorMsg("E");	
						response.setAppResponse(e.getTargetException());
					}
				}else{
					try {
						appResponse = method.invoke(interfaceImpl, request.getParams());
						response.setErrorMsg("S");
						response.setAppResponse(appResponse);
					} catch (InvocationTargetException e) {
						response.setErrorMsg("E");
						response.setAppResponse(e.getTargetException());
					}
				}
				arg0.writeAndFlush(response);
				return;
			}
			if (arg1 instanceof RpcContextRequest) {
				RpcContextRequest contextRequest = (RpcContextRequest) arg1;
				Object interfaceImpl = RpcProviderImpl.serviceEngine
						.get(RpcAddContext.class.getName());
				Method method;
				Object appResponse;
				RpcResponse response = new RpcResponse();
				try {	
					Class[] parameterTypes = new Class[2];
					parameterTypes[0] = String.class;
					parameterTypes[1] = Object.class;
					method = interfaceImpl.getClass().getMethod("addContextMessage",parameterTypes);
					Object[] params = new Object[2];
					params[0] = contextRequest.getKeyString();
					params[1] = contextRequest.getValueString();
					appResponse = method.invoke(interfaceImpl, params);
					response.setAppResponse(appResponse);
					response.setErrorMsg("S");
				} catch (Exception e) {
					response.setErrorMsg("E");
					response.setAppResponse(new Boolean(true));
				}
				arg0.writeAndFlush(response);
			}
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			// TODO 自动生成的方法存根
			//System.out.println("RPC Provider Message:Netty server channel stop active.");
			// super.channelInactive(ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			// TODO 自动生成的方法存根
			//System.out.println("RPC Provider Message:Netty server catch exception!");
			//cause.printStackTrace();
		}
	}

	public static void main(String[] args) {
		NettyServer server = new NettyServer(8888);
	}
}
