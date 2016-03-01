package com.alibaba.middleware.race.rpc.net;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午10:15:15
 *
 * @description 
 **/

public class SimpleNettyResponseFutureUtil {
	private static final AttributeKey<Object> RESPONSE_ATTR = AttributeKey.valueOf("SimpleNettyResponseFuture");
	private static final AttributeKey<Object> FORCE_CONN_ATTR = AttributeKey.valueOf("forceConnect");

	public static void attributeForceConnect(Channel channel,
			boolean forceConnect) {
		if (forceConnect) {
			channel.attr(FORCE_CONN_ATTR).set(true);
		}
	}

	public static void attributeSimpleResponseFuture(Channel channel,
			SimpleNettyResponseFuture responseFuture) {
		channel.attr(RESPONSE_ATTR).set(responseFuture);
		responseFuture.setChannel(channel);
	}
	
	public static SimpleNettyResponseFuture getResponseFuture(Channel channel){
		return (SimpleNettyResponseFuture)channel.attr(RESPONSE_ATTR).get();
	}
	
	public static boolean getForceConnect(Channel channel){
		Object forceConnect = channel.attr(FORCE_CONN_ATTR).get();
		if(null == forceConnect){
			return false;
		}
		return true;
	}
	
	public static boolean done(Channel channel,Object object){
		SimpleNettyResponseFuture future = getResponseFuture(channel);
		SimpleNettyResponse simpleNettyResponse = new SimpleNettyResponse(object);
		if(null != future){
			return future.done(simpleNettyResponse);
		}
		return false;
	}
	
	public static boolean cancel(Channel channel,Throwable cause){
		SimpleNettyResponseFuture future = getResponseFuture(channel);
		return future.cancel(cause);
	}
}
