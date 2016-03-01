package com.alibaba.middleware.race.mom.net;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午3:20:01
 *
 * @description 
 **/

public class ProducerNettyClient {
	
	private static ProducerSimpleNettyChannelPool channelPool;
	private static ConfigBuilder configBuilder;
	
	private ProducerNettyClient(ConfigBuilder configBuilder){
		ProducerNettyClient.configBuilder = configBuilder;
		ProducerNettyClient.channelPool = new ProducerSimpleNettyChannelPool(configBuilder.remoteAddress,
				configBuilder.maxConnNum,
				configBuilder.idleTime,
				configBuilder.timeUnit,
				configBuilder.timeout);
	}
	
	public ProducerSimpleNettyResponseFuture request(Object request) throws InterruptedException, IOException{
		return channelPool.sendRequest(request);
	}
	
	public static ProducerSimpleNettyChannelPool getSimpleNettyChannelPool(){
		return ProducerNettyClient.channelPool;
	}
	public void close(){
		try {
			ProducerNettyClient.channelPool.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	public static final class ConfigBuilder{
		//
		SocketAddress remoteAddress;
		//最大连接数
		private int maxConnNum;
		//超时(毫秒)
		private int timeout;
		//空闲时间
		private int idleTime;
		//空闲时间单位
		private TimeUnit timeUnit;
		
		private boolean forbidForceConnect;
	
		
		public ConfigBuilder remoteAddress(SocketAddress remoteAddress){
			this.remoteAddress = remoteAddress;
			return this;
		}
		
		public ConfigBuilder maxConnNum(int maxConnNum){
			this.maxConnNum = maxConnNum;
			return this;
		}
		
		public ConfigBuilder timeout(int timeout){
			this.timeout = timeout;
			return this;
		}
		
		public ConfigBuilder idleTime(int idleTime,TimeUnit timeUnit){
			this.idleTime = idleTime;
			this.timeUnit = timeUnit;
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

		public int getIdleTime() {
			return idleTime;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public boolean isForbidForceConnect() {
			return forbidForceConnect;
		}

		public ProducerNettyClient build(){
			return new ProducerNettyClient(this);
		}
	}
	
	/*public static void main(String[] args) throws InterruptedException, IOException, TimeoutException {
		InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.92",8888);
		SimpleNettyResponseFuture future;
		SimpleNettyResponse responseFuture;
		long st = System.currentTimeMillis();
		NettyClient client;
		for(int i = 0;i < 100000;i++){
			client = new NettyClient.ConfigBuilder()
			.remoteAddress(remoteAddress)
			.maxConnNum(10)
			.timeout(3000)
			.forbidForceConnect(false)
			.build();
			future = client.request(new Person("超超" + i, "男", 23));
			responseFuture = future.get(3000,TimeUnit.MILLISECONDS);
		}
		System.out.println(System.currentTimeMillis() - st);
	}*/
}
