package com.alibaba.middleware.race.mom.net;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月16日下午9:46:27
 *
 * @description 
 **/

public class BrokerSimpleNettyResponseFutureUtil {
	private static final AttributeKey<Object> RESPONSE_ATTR = AttributeKey.valueOf("SimpleNettyResponseFuture");
	private static final AttributeKey<Object> FORCE_CONN_ATTR = AttributeKey.valueOf("forceConnect");
	private static final AttributeKey<Object> ROUTE_ATTR = AttributeKey.valueOf("route");

	public static void attributeForceConnect(Channel channel,
			boolean forceConnect) {
		if (forceConnect) {
			channel.attr(FORCE_CONN_ATTR).set(true);
		}
	}
	public static void attributeSimpleResponseFuture(Channel channel,
			BrokerSimpleNettyResponseFuture responseFuture) {
		channel.attr(RESPONSE_ATTR).set(responseFuture);
		responseFuture.setChannel(channel);
	}
	
	public static void attributeRoute(Channel channel,InetSocketAddress route){
		channel.attr(ROUTE_ATTR).set(route);
	}
	
	public static InetSocketAddress getRoute(Channel channel){
		return (InetSocketAddress)channel.attr(ROUTE_ATTR).get();
	}
	
	public static BrokerSimpleNettyResponseFuture getResponseFuture(Channel channel){
		return (BrokerSimpleNettyResponseFuture)channel.attr(RESPONSE_ATTR).get();
	}
	
	public static boolean getForceConnect(Channel channel){
		Object forceConnect = channel.attr(FORCE_CONN_ATTR).get();
		if(null == forceConnect){
			return false;
		}
		return true;
	}
	
	public static boolean done(Channel channel,Object object){
		BrokerSimpleNettyResponseFuture future = getResponseFuture(channel);
		BrokerSimpleNettyResponse simpleNettyResponse = new BrokerSimpleNettyResponse(object);
		if(null != future){
			return future.done(simpleNettyResponse);
		}
		return false;
	}
	
	public static boolean cancel(Channel channel,Throwable cause){
		BrokerSimpleNettyResponseFuture future = getResponseFuture(channel);
		return future.cancel(cause);
	}
}
