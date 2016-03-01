package com.alibaba.middleware.race.mom.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月16日下午10:29:11
 *
 * @description 
 **/

public class BrokerNettyClientProxy {
	
	private static BrokerSimpleNettyChannelPool channelPool;
	
	private static ConfigBuilder configBuilder;
	

	private BrokerNettyClientProxy(ConfigBuilder configBuilder) {
		// TODO 自动生成的构造函数存根
		BrokerNettyClientProxy.configBuilder = configBuilder;
		BrokerNettyClientProxy.channelPool = new BrokerSimpleNettyChannelPool(
				configBuilder.maxConnNum,
				configBuilder.timeout,
				configBuilder.forbidForceConnect);
	}
	
	public BrokerSimpleNettyResponseFuture request(InetSocketAddress remoteAddr,Object request) throws InterruptedException, IOException{
		return channelPool.sendRequest(remoteAddr, request);
	}
	
	public static BrokerSimpleNettyChannelPool getSimpleNettyChannelPool(){
		return channelPool;
	}
	
	public static final class ConfigBuilder{
		//最大连接数
		private int maxConnNum;
		//超时(毫秒)
		private int timeout;
		private boolean forbidForceConnect;
		public ConfigBuilder maxConnNum(int maxConnNum){
			this.maxConnNum = maxConnNum;
			return this;
		}
		public ConfigBuilder timeout(int timeout){
			this.timeout = timeout;
			return this;
		}
		public ConfigBuilder forbidForceConnect(boolean forbidForceConnect) {
            this.forbidForceConnect = forbidForceConnect;
            return this;
        }
		public int getMaxConnNum() {
			return maxConnNum;
		}

		public int getTimeout() {
			return timeout;
		}
		public boolean isForbidForceConnect() {
			return forbidForceConnect;
		}
		
		public BrokerNettyClientProxy build(){
			return new BrokerNettyClientProxy(this);
		}
	}
	public static void main(String[] args) throws UnknownHostException, InterruptedException, IOException {
		BrokerNettyClientProxy proxy = new BrokerNettyClientProxy.ConfigBuilder()
				.maxConnNum(5).timeout(3000).forbidForceConnect(false).build();
		BrokerSimpleNettyResponseFuture future;
		BrokerSimpleNettyResponse response;
		long st = System.currentTimeMillis();
		for(int i=0;i< 4000;i++){
			future = proxy.request(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),8888), "hello world");
			response = future.get();
		}
		System.out.println(System.currentTimeMillis() - st);
		//System.out.println(response.getResponse());
	}
}
