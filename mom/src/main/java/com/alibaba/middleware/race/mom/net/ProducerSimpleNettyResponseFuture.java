package com.alibaba.middleware.race.mom.net;

import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午5:00:45
 *
 * @description 
 **/

public class  ProducerSimpleNettyResponseFuture {

	private final CountDownLatch latch;
	private volatile boolean isDone;   
	private volatile boolean isCancel;
	private final AtomicBoolean isProcessed;
 	private volatile Channel channel;
 	ProducerSimpleNettyResponse simpleNettyResponse;
 	public ProducerSimpleNettyResponseFuture(){
 		latch = new CountDownLatch(1);
 		isProcessed = new AtomicBoolean(false);
 		isDone = false;
 		isCancel = false;
 	}
 	
 	public boolean cancel(Throwable cause){
 		if(isProcessed.getAndSet(true)){
 			return false;
 		}
 		isCancel = true;
 		latch.countDown();
 		simpleNettyResponse.setSuccess(false);
 		simpleNettyResponse.setCause(cause);
 		return true;
 	}
 	
 	public ProducerSimpleNettyResponse get() throws InterruptedException{
 		latch.await();
 		return simpleNettyResponse;
 	}
 	
 	public ProducerSimpleNettyResponse get(long timeout,TimeUnit timeUnit) throws InterruptedException,TimeoutException{
 		if(!latch.await(timeout, timeUnit)){
 			throw new TimeoutException();
 		}
 		return simpleNettyResponse;
 	}
 	
 	public boolean done(ProducerSimpleNettyResponse simpleNettyResponse){
 		if(isProcessed.getAndSet(true)){
 			return false;
 		}
 		this.simpleNettyResponse = simpleNettyResponse;
 		isDone = true;
 		latch.countDown();
 		return true;
 	}
 	
 	public boolean isCancelled(){
 		return isCancel;
 	}
 	
 	public boolean isDone(){
 		return isDone;
 	}
 	
 	public Channel getChannel(){
 		return channel;
 	}
 	
 	public void setChannel(Channel channel){
 		this.channel = channel;
 	}
 	
}
