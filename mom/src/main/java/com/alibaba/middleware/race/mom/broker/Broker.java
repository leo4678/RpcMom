package com.alibaba.middleware.race.mom.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.Port;

import com.alibaba.middleware.race.mom.ConsumeResult;
import com.alibaba.middleware.race.mom.ConsumeStatus;
import com.alibaba.middleware.race.mom.DefaultConsumer;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.net.BrokerNettyClientProxy;
import com.alibaba.middleware.race.mom.net.BrokerSimpleNettyResponse;
import com.alibaba.middleware.race.mom.net.BrokerSimpleNettyResponseFuture;
import com.alibaba.middleware.race.mom.net.ConsumerNetAgent;
import com.alibaba.middleware.race.mom.subinfo.SubConsumersByID;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月13日 下午4:49:32
 */
public class Broker {
	private static int port = 9999;
	public static LinkedBlockingQueue<FailTask> failQueue = new LinkedBlockingQueue<FailTask>();
	public static int stressMax = Runtime.getRuntime().availableProcessors()*2 + 8;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//主服务处理线程
		new Thread(){
			public void run() {
				// TODO Auto-generated method stub
				BrokerNettyServer brokerNettyServer = new BrokerNettyServer(port);
			}
		}.start();
		System.out.println("Broker Server:The service start!");
		//性能监测，当处于低性能时处理失败的任务
		new Thread(){
			public void run() {
				// TODO Auto-generated method stub
				while(true){
					int threads = Thread.getAllStackTraces().keySet().size();
					//System.out.println(threads);
					if (stressMax > threads){
						try {
							FailTask failTask = failQueue.take();
							String consumerID = failTask.getConsumerID();
							String msgID = failTask.getMsgID();
							
							DefaultConsumer consumer = SubConsumersByID.consumers.get(consumerID);
							//****根据msgID拿到message
							
							String string = new String(BrokerNettyServer.fileProxy.get(Integer.valueOf(msgID)));						
							String[] messageInfor = string.split("\\|");
							Message message = new Message();
							message.setMsgId(messageInfor[0]);
							message.setTopic(messageInfor[1]);
							message.setBody(messageInfor[2].getBytes());
							message.setBornTime(Long.valueOf(messageInfor[3]));
							message.setConsume(Boolean.valueOf(messageInfor[4]));
							String tempProperty = messageInfor[5];
							tempProperty = tempProperty.replace("{", "");
							tempProperty = tempProperty.replace("}", "");				
							String[] temps = tempProperty.split(",");
							int len = temps.length;
							for (int i = 0; i < len; i++){
								String[] s = temps[i].trim().split("=");
								message.setProperty(s[0], s[1]);
							}
							
							final InetSocketAddress remoteAddress = new InetSocketAddress(
									consumer.getConsumerIp(), consumer.getConsumerPort());
							int maxConnNum = 10;
							int timeout = 3000;

							BrokerNettyClientProxy client = new BrokerNettyClientProxy.ConfigBuilder()
									.maxConnNum(maxConnNum).timeout(timeout)
									.forbidForceConnect(false).build();

							BrokerSimpleNettyResponseFuture future;
							BrokerSimpleNettyResponse response = null;
							try {
								future = client.request(remoteAddress, message);
								response = future.get();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								Broker.failQueue.put(new FailTask(message.getMsgId(), consumer.getConsumerId()));													
								//e.printStackTrace();
								continue;
							} catch (IOException e) {
								// TODO Auto-generated catch block
								Broker.failQueue.put(new FailTask(message.getMsgId(), consumer.getConsumerId()));									
								//e.printStackTrace();
								continue;
							}
							if (!((ConsumeResult) response.getResponse()).getStatus()
									.equals(ConsumeStatus.SUCCESS)){
								Broker.failQueue.put(new FailTask(message.getMsgId(), consumer.getConsumerId()));	
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}					
				}				
			}
		}.start();
		
	}
}
