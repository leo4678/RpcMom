package com.alibaba.middleware.race.rpc.api.impl;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.rpc.aop.ConsumerHook;
import com.alibaba.middleware.race.rpc.api.RpcConsumer;
import com.alibaba.middleware.race.rpc.async.ResponseCallbackListener;
import com.alibaba.middleware.race.rpc.async.ResponseFuture;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.net.NettyClient;
import com.alibaba.middleware.race.rpc.net.SimpleNettyResponse;
import com.alibaba.middleware.race.rpc.net.SimpleNettyResponseFuture;
import com.alibaba.middleware.race.rpc.util.ObjectDecoder;
import com.alibaba.middleware.race.rpc.util.ObjectEncoder;


/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月12日 下午3:36:12
 */
public class RpcConsumerImpl extends RpcConsumer implements InvocationHandler {

	private Class<?> interfaceClazz;
	private String version;
	private int clientTimeout;
	private ConsumerHook hook;

	public static String Ip = System.getProperty("SIP");
	public static int port = 8888;
	public static NettyClient nettyClient = null;

	private Map<String, Boolean> methodIsAsyn = new HashMap<String, Boolean>();
	private ResponseCallbackListener responseCallbackListener = null;

	private RpcRequest rpcRequest;
	private RpcResponse rpcResponse;

	@Override
	public RpcConsumer interfaceClass(Class<?> interfaceClass) {
		this.interfaceClazz = interfaceClass;
		return this;
	}

	public Class<?> getInterfaceClazz() {
		return interfaceClazz;
	}

	public void setInterfaceClazz(Class<?> interfaceClazz) {
		this.interfaceClazz = interfaceClazz;
	}

	@Override
	public RpcConsumer version(String version) {
		this.version = version;
		return this;
	}

	@Override
	public RpcConsumer clientTimeout(int clientTimeout) {
		InetSocketAddress remoteAddress = new InetSocketAddress(Ip,port);
		this.clientTimeout = clientTimeout;
		nettyClient = new NettyClient.ConfigBuilder().remoteAddress(remoteAddress)
				.maxConnNum(Runtime.getRuntime().availableProcessors() * 4)
				.timeout(clientTimeout)
				.forbidForceConnect(false)
				.build();
		return this;
	}

	@Override
	public RpcConsumer hook(ConsumerHook hook) {
		this.hook = hook;
		return this;
	}

	// 根据interfaceClazz 构建一个代理对象 返回回去
	@Override
	public Object instance() {
		// public static Object newProxyInstance(ClassLoader loader, Class<?>[]
		// interfaces, InvocationHandler h) throws IllegalArgumentException
		// loader:　　一个ClassLoader对象，定义了由哪个ClassLoader对象来对生成的代理对象进行加载
		// interfaces:　　一个Interface对象的数组，表示的是我将要给我需要代理的对象提供一组什么接口，如果我提供了一组接口给它，那么这个代理对象就宣称实现了该接口(多态)，这样我就能调用这组接口中的方法了
		// h:　　一个InvocationHandler对象，表示的是当我这个动态代理对象在调用方法的时候，会关联到哪一个InvocationHandler对象上
		return Proxy.newProxyInstance(this.getClass().getClassLoader(),
				new Class[] { this.interfaceClazz }, this);
	}

	// 新开一个线程提交请求，并将结果写入到ResponeFuture中
	@Override
	public void asynCall(final String methodName) {
		// TODO Auto-generated method stub
		methodIsAsyn = new HashMap<String, Boolean>();
		methodIsAsyn.put(methodName, true);
	}

	@Override
	public <T extends ResponseCallbackListener> void asynCall(
			String methodName, T callbackListener) {
		// TODO Auto-generated method stub
		methodIsAsyn.put(methodName, true);
		this.responseCallbackListener = callbackListener;
	}

	// 异步调用
	@Override
	public void cancelAsyn(String methodName) {
		// TODO Auto-generated method stub
		methodIsAsyn.remove(methodName);
	}


	SimpleNettyResponseFuture future;
	SimpleNettyResponse responseFuture;
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		// TODO Auto-generated method stub
		// 构建请求对象
		rpcRequest = new RpcRequest();
		rpcRequest.setInterfaceName(this.interfaceClazz.getName());
		rpcRequest.setMethodName(method.getName());
		if (null == args) {
			Object[] params = new Object[1];
			params[0] = "NULL";
			rpcRequest.setParams(params);
		} else {
			rpcRequest.setParams(args);
		}
		// 记录将要进行的操作
		this.hook.before(rpcRequest);
		// 判断是否以异步的方式来发送请求
		boolean flag = false;
		if (methodIsAsyn.containsKey(method.getName())
				&& methodIsAsyn.get(method.getName()))
			flag = true;
		// 发送请求到服务端	
		// 1.异步处理
		if (flag) {			
			ExecutorService service = Executors.newSingleThreadExecutor();
			Future<Object> futureObject = service.submit(new Callable<Object>() {

				public Object call() throws Exception {
					// TODO Auto-generated method stub
					future = nettyClient.request(rpcRequest);
					responseFuture = future.get(clientTimeout,TimeUnit.MILLISECONDS);
					rpcResponse = (RpcResponse)responseFuture.getResponse();
					rpcResponse.setErrorMsg(null);
					return rpcResponse;
				}
			});
			ResponseFuture.class.newInstance().setFuture(futureObject);
			// 1.1异步处理有回调函数
			if (null != responseCallbackListener) {
				try {
					Object appResponse = ResponseFuture
							.getResponse(clientTimeout);
					responseCallbackListener.onResponse(appResponse);
				} catch (RuntimeException e) {
					if (e.getMessage().equals("execution")) {
						responseCallbackListener.onException(e);
					} else {
						responseCallbackListener.onTimeout();
					}
				}
			}
			// 1.2异步处理没有回调函数
			return null;
		} else {// 2.非异步处理
			future = nettyClient.request(rpcRequest);
			responseFuture = future.get(clientTimeout,TimeUnit.MILLISECONDS);
			rpcResponse = (RpcResponse)responseFuture.getResponse() ;
			if (rpcResponse.getAppResponse() instanceof RuntimeException) {
				throw (RuntimeException) rpcResponse.getAppResponse();
			}
			if (rpcResponse.getAppResponse() instanceof TimeoutException){
				throw (TimeoutException) rpcResponse.getAppResponse();
			}
			return rpcResponse.getAppResponse();
		}
	}
}