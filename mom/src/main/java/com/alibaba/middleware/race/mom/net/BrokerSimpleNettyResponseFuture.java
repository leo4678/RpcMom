package com.alibaba.middleware.race.mom.net;

import io.netty.channel.Channel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author tokysky (HIT-CS-ICES)
 * @time 于2015年8月16日下午9:21:53
 *
 * @description
 **/

public class BrokerSimpleNettyResponseFuture {
	
	private final CountDownLatch latch;
	private volatile boolean isDone;
	private volatile boolean isCancel;
	private final AtomicBoolean isProcessed;
	private volatile Channel channel;
	BrokerSimpleNettyResponse simpleNettyResponse;

	public BrokerSimpleNettyResponseFuture() {
		latch = new CountDownLatch(1);
		isProcessed = new AtomicBoolean(false);
		isDone = false;
		isCancel = false;
		simpleNettyResponse = new BrokerSimpleNettyResponse(null);
	}

	public boolean cancel(Throwable cause) {
		if (isProcessed.getAndSet(true)) {
			return false;
		}
		isCancel = true;
		latch.countDown();
		simpleNettyResponse.setSuccess(false);
		simpleNettyResponse.setCause(cause);
		return true;
	}

	public BrokerSimpleNettyResponse get() throws InterruptedException {
		latch.await();
		return simpleNettyResponse;
	}

	public BrokerSimpleNettyResponse get(long timeout, TimeUnit timeUnit)
			throws InterruptedException, TimeoutException {
		if (!latch.await(timeout, timeUnit)) {
			throw new TimeoutException();
		}
		return simpleNettyResponse;
	}

	public boolean done(BrokerSimpleNettyResponse simpleNettyResponse) {
		if (isProcessed.getAndSet(true)) {
			return false;
		}
		this.simpleNettyResponse = simpleNettyResponse;
		isDone = true;
		latch.countDown();
		return true;
	}

	public boolean isCancelled() {
		return isCancel;
	}

	public boolean isDone() {
		return isDone;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
}
