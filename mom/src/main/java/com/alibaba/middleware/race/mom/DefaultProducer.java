package com.alibaba.middleware.race.mom;

import io.netty.util.concurrent.Future;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.mom.net.ProducerNettyClient;
import com.alibaba.middleware.race.mom.net.ProducerSimpleNettyResponse;
import com.alibaba.middleware.race.mom.net.ProducerSimpleNettyResponseFuture;

public class DefaultProducer implements Producer {
	private String brokerIp;
	private String topic;
	private String groupId;

	private int port = 9999;
	private String messageID = "0";
	private ProducerNettyClient clientProxy = null;

	public DefaultProducer() {
		brokerIp = System.getProperty("SIP");
		//System.out.println("SIP = " + brokerIp);
	}

	/**
	 * 启动生产者，初始化底层资源。在所有属性设置完毕后，才能调用这个方法 预测为创建网络连接,建立通道
	 */
	public void start() {
		// TODO Auto-generated method stub
		InetSocketAddress remoteAddress = new InetSocketAddress(brokerIp, port);
		clientProxy = new ProducerNettyClient.ConfigBuilder()
				.remoteAddress(remoteAddress).maxConnNum(10).timeout(3000)
				.forbidForceConnect(false).build();
		// TODO Auto-generated catch block
	}

	/**
	 * 设置生产者可发送的topic
	 * 
	 * @param topic
	 */
	public void setTopic(String topic) {
		// TODO Auto-generated method stub
		this.topic = topic;
	}

	/**
	 * 设置生产者id，broker通过这个id来识别生产者集群
	 * 
	 * @param groupId
	 */
	public void setGroupId(String groupId) {
		// TODO Auto-generated method stub
		this.groupId = groupId;
	}

	/**
	 * 发送消息 到 broker代理
	 * 
	 * @param message
	 * @return 需要补充消息的 topic,msgId,bornTime
	 */
	public SendResult sendMessage(Message message) {
		// TODO Auto-generated method stub
		// 设置Message的其他属性topic,msgId,bornTime
		message.setTopic(this.topic);
		message.setBornTime(System.currentTimeMillis());
		message.setConsume(false);
		message.setMsgId(messageID);
//		System.out.println("Producer Client Message:send message. message = "
//				+ message.toString());
		if (0 == message.getpropertyHashMap().size())
			message.setProperty("filter", "null");
		ProducerSimpleNettyResponseFuture future;
		ProducerSimpleNettyResponse responseFuture = null;
		try {
			future = clientProxy.request(message);
			responseFuture = future.get(3000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		//System.out.println("Producer Client Message:the message send success!");
		return (SendResult)responseFuture.getResponse();
	}

	/**
	 * 异步callback发送消息，当前线程不阻塞。broker返回ack后，触发callback
	 * 
	 * @param message
	 * @param callback
	 */
	public void asyncSendMessage(Message msg, SendCallback callback) {
		// TODO Auto-generated method stub
		SendResult sendResult = null;
		// 设置Message的其他属性topic,msgId,bornTime
		final Message message = msg;
		message.setTopic(this.topic);
		message.setBornTime(System.currentTimeMillis());
		// 如下是获取唯一的消息ID
		message.setConsume(false);
		message.setMsgId(messageID);
		if (null == message.getpropertyHashMap())
			message.setProperty("filter", "null");
		//System.out.println(message.toString());
		ExecutorService service = Executors.newFixedThreadPool(1);
		java.util.concurrent.Future<Object> future = service
				.submit(new Callable<Object>() {

					public Object call() throws Exception {
						// TODO Auto-generated method stub
						ProducerSimpleNettyResponseFuture future;
						ProducerSimpleNettyResponse responseFuture;
						future = clientProxy.request(message);
						responseFuture = future
								.get(3000, TimeUnit.MILLISECONDS);
						return responseFuture.getResponse();
					}
				});
		try {
			sendResult = (SendResult) future.get(3000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		callback.onResult(sendResult);
		service.shutdown();
	}

	/**
	 * 关闭网络通道，销毁网络连接 停止生产者，销毁资源
	 */
	public void stop() {
		// TODO Auto-generated method stub
		clientProxy.close();
	}

}
