package com.alibaba.middleware.race.mom.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月16日下午10:23:37
 *
 * @description 
 **/

public class BrokerSimpleNettyChannelPoolHandler extends SimpleChannelInboundHandler<Object>{

	private BrokerSimpleNettyChannelPool simpleNettyChannelPool;
	
	public BrokerSimpleNettyChannelPoolHandler(BrokerSimpleNettyChannelPool simpleNettyChannelPool) {
		// TODO 自动生成的构造函数存根
		this.simpleNettyChannelPool = simpleNettyChannelPool;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO 自动生成的方法存根
		if(arg1 instanceof CloseFlag){
			arg0.channel().close();
			return;
		}
		if(null != arg1){
			BrokerSimpleNettyResponseFutureUtil.done(arg0.channel(), arg1);
			simpleNettyChannelPool.returnChannel(arg0.channel());
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// TODO 自动生成的方法存根
		if(evt instanceof IdleStateHandler){
			ctx.channel().close();
		}else{
			ctx.fireUserEventTriggered(evt);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		// TODO 自动生成的方法存根
		//System.out.println("通道被关闭");
	}
}
