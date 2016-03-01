package com.alibaba.middleware.race.mom;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.mom.net.CloseFlag;
import com.alibaba.middleware.race.mom.net.ConsumerNetAgent;
import com.alibaba.middleware.race.mom.net.ConsumerNetAgentHandler;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time :2015年8月10日 下午4:56:40
 */
public class DefaultConsumer implements Consumer, Serializable {

	transient private String brokerIp;
	private String groupId; // 标识同一个集群 --

	public static String consumerId = "0"; // 标识唯一的消费者
	private String topic; // --

	private Map<String, String> properties = new HashMap<String, String>(); // --

	transient private MessageListener messageListener;

	public MessageListener getMessageListener() {
		return messageListener;
	}

	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	private String consumerIp = "127.0.0.1"; // 存储consumer监听的ip --

	private int consumerPort;// 存储consumer监听的端口 --

	public int getConsumerPort() {
		return consumerPort;
	}

	public void setConsumerPort(int consumerPort) {
		this.consumerPort = consumerPort;
	}

	public String getConsumerId() {
		return consumerId;
	}

	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getConsumerIp() {
		return consumerIp;
	}

	public void setConsumerIp(String consumerIp) {
		this.consumerIp = consumerIp;
	}

	public DefaultConsumer() {
		brokerIp = System.getProperty("SIP");
	}

	/**
	 * 启动消费者，初始化底层资源。要在属性设置和订阅操作发起之后执行
	 * 
	 */
	transient private ConsumerNetAgent consumerNetAgent = null;
	transient private ConsumeResult consumeResult = null;

	public void start() {
		// TODO Auto-generated method stub
		int brokerPort = 9999;
		// 构建Consumer的网络处理对象

		try {
			consumerNetAgent = new ConsumerNetAgent(
					DefaultConsumer.this.brokerIp, brokerPort, 10000);
			DefaultConsumer.this.consumerPort = consumerNetAgent
					.getConsumerPort();
//			System.out.println("Consumer Server:listener Port = "
//					+ this.consumerPort);

			// 注册监听返回结果的处理方法
			consumerNetAgent.registerHandler(new ConsumerNetAgentHandler() {
				ConsumeResult result = null;

				// 接收消息要做的处理
				public Object actionPerform(Object object) {
					// TODO 自动生成的方法存根
					// System.out.println("Consumer Server:recieve object class = "
					// + object.getClass().getName());
					if (object instanceof Message) {
						ConsumeResult result;
						Message message = (Message) object;
//						System.out
//								.println("Consumer Server:recieve broker message(msgId = "
//										+ message.getMsgId()
//										+ ") to consumer("
//										+ ConsumerNetAgent.consumerID + ")");
						result = messageListener.onMessage(message);
						return result;
					} else {
						return null;
					}
				}
			});
			consumerNetAgent.start();
			consumerNetAgent.syncRegister(DefaultConsumer.this);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}

	/**
	 * 发起订阅操作
	 * 
	 * @param topic
	 *            只接受该topic的消息
	 * @param filter
	 *            属性过滤条件，例如 area=hz，表示只接受area属性为hz的消息。消息的过滤要在服务端进行
	 *            如果改属性为null或者空串，那么表示接收这个topic下的所有消息
	 * @param listener
	 *            这个Lister是用来做消费者获得消息以后的处理
	 */
	public static String storeConsumerIDFilePath = System.getProperty("user.home") + "/store/" + "ConsumerIDFile";

	public void storeConsumerID() {

	}

	public void subscribe(String topic, String filter, MessageListener listener) {
		// TODO Auto-generated method stub
		File consumerIDFile = new File(storeConsumerIDFilePath);
		if (consumerIDFile.exists()){
			try {
				FileInputStream fis = new FileInputStream(consumerIDFile);
				DataInputStream dis = new DataInputStream(fis);
				this.consumerId = dis.readUTF();
				dis.close();
				fis.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		this.topic = topic;
		if (filter == null || filter.equals("")) {
			this.properties.put("filter", "null");
		} else {
			String[] filters = filter.split("=");
			this.properties.put(filters[0], filters[1]);
		}

		this.messageListener = listener;
	}

	/**
	 * 设置消费者组id，broker通过这个id来识别消费者机器
	 * 
	 * @param groupId
	 */
	public void setGroupId(String groupId) {
		// TODO Auto-generated method stub
		this.groupId = groupId;
	}

	/**
	 * 停止消费者，broker不再投递消息给此消费者机器。
	 */
	public void stop() {
		// TODO Auto-generated method stub
		consumerNetAgent.writeObjectToBuffer(new CloseFlag());
		consumerNetAgent.stop();
	}

	// transient private String brokerIp;
	// private String groupId; // 标识同一个集群 --
	//
	// transient private String consumerId = "0"; // 标识唯一的消费者
	// private String topic; // --
	//
	// private Map<String, String> properties = new HashMap<String, String>();
	// // --
	//
	// transient private MessageListener messageListener;
	//
	// private String consumerIp = "null"; // 存储consumer监听的ip --
	//
	// private int consumerPort;// 存储consumer监听的端口 --
	public String toString() {
		return "Consumer: groupId = " + this.groupId + ",consumerId = "
				+ this.consumerId + ",topic = " + this.topic + ",properties = "
				+ this.properties.toString() + ",consumerIp = "
				+ this.consumerIp + ",consumerPort = " + this.consumerPort;
	}
}
