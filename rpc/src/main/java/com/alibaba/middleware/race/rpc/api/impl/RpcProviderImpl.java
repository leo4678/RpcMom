package com.alibaba.middleware.race.rpc.api.impl;

import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.Port;

import com.alibaba.middleware.race.rpc.api.RpcProvider;
import com.alibaba.middleware.race.rpc.context.RpcAddContext;
import com.alibaba.middleware.race.rpc.context.RpcAddContextImpl;
import com.alibaba.middleware.race.rpc.context.RpcContext;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;


/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月12日 下午3:36:57
 */
public class RpcProviderImpl extends RpcProvider{

	private Class<?> serviceInterface;
	private String version;
	private Object serviceInstance;
	private int timeout;
	private String serializeType;
	public static Map<String, Object> serviceEngine = new HashMap<String, Object>();
	private RpcResponse response;
	private int port = 8888;

	// 返回提供的服务的类
	@Override
	public RpcProvider serviceInterface(Class<?> serviceInterface) {
		this.serviceInterface = serviceInterface;
		return this;
	}

	@Override
	public RpcProvider version(String version) {
		this.version = version;
		return this;
	}

	@Override
	public RpcProvider impl(Object serviceInstance) {
		this.serviceInstance = serviceInstance;
		return this;
	}

	@Override
	public RpcProvider timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	public RpcProvider serializeType(String serializeType) {
		this.serializeType = serializeType;
		return this;
	}

	@Override
	public void publish() {
		RpcProviderImpl.serviceEngine.put(serviceInterface.getName(), serviceInstance);
		RpcProviderImpl.serviceEngine.put(RpcAddContext.class.getName(), new RpcAddContextImpl());
		//创建netty框架
		NettyServer server = new NettyServer(port);		
	}
}
