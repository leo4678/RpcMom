package com.alibaba.middleware.race.rpc.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午4:45:31
 *
 * @description 
 **/

public class SimpleNettyChannelPoolHandler extends SimpleChannelInboundHandler<Object>{

	private SimpleNettyChannelPool simpleNettyChannelPool;
	public SimpleNettyChannelPoolHandler(SimpleNettyChannelPool simpleNettyChannelPool) {
		// TODO 自动生成的构造函数存根
		this.simpleNettyChannelPool = simpleNettyChannelPool;
	}
	@Override
	protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
			throws Exception {
		// TODO 自动生成的方法存根
		if(null != arg1){
			//设置Channel，还给连接池
			SimpleNettyResponseFutureUtil.done(arg0.channel(),arg1);
			simpleNettyChannelPool.returnChannel(arg0.channel());
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// TODO 自动生成的方法存根
		if(evt instanceof IdleStateEvent){
			ctx.channel().close();
		}else{
			ctx.fireUserEventTriggered(evt);
		}
	}
}
