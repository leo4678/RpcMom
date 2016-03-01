package com.alibaba.middleware.race.mom.broker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.alibaba.middleware.race.mom.ConsumeResult;
import com.alibaba.middleware.race.mom.ConsumeStatus;
import com.alibaba.middleware.race.mom.DefaultConsumer;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.SendResult;
import com.alibaba.middleware.race.mom.SendStatus;
import com.alibaba.middleware.race.mom.converter.ObjectDecoder;
import com.alibaba.middleware.race.mom.converter.ObjectEncoder;
import com.alibaba.middleware.race.mom.files.MappedFileProxy;
import com.alibaba.middleware.race.mom.net.BrokerNettyClientProxy;
import com.alibaba.middleware.race.mom.net.BrokerSimpleNettyResponse;
import com.alibaba.middleware.race.mom.net.BrokerSimpleNettyResponseFuture;
import com.alibaba.middleware.race.mom.subinfo.ConsumerSubscribeInfor;
import com.alibaba.middleware.race.mom.subinfo.SubConsumersByID;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time :2015年8月13日 下午4:49:23
 */
public class BrokerNettyServer {

	private int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap bootstrap;
	public static MappedFileProxy fileProxy;
	String home = System.getProperty("user.home") + "/store/";
	String prefix = "data";
	String suffix = ".txt";
	public static AtomicInteger messageID = new AtomicInteger(0);
	public static AtomicLong consumerID = new AtomicLong(0L);
	public static HashMap<String, ArrayList<String>> failPushHashMap = new HashMap<String, ArrayList<String>>();

	public BrokerNettyServer(int port) {
		this.port = port;
		bossGroup = new NioEventLoopGroup(4);
		workerGroup = new NioEventLoopGroup(Runtime.getRuntime()
				.availableProcessors() * 2);
		try {
			fileProxy = new MappedFileProxy.ConfigBuild().home(home)
					.pathPrefix(prefix).pathSuffix(suffix)
					.totalCapacity(1024 * 1024).indexCapacity(1024)
					.fileCount(10).build();
			bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {

						@Override
						protected void initChannel(SocketChannel arg0)
								throws Exception {
							// TODO 自动生成的方法存根
							arg0.pipeline().addLast("decoder",
									new ObjectDecoder());
							arg0.pipeline().addLast("encoder",
									new ObjectEncoder());
							arg0.pipeline().addLast("handler",
									new NettyServerHandler());
						}
					}).option(ChannelOption.SO_KEEPALIVE, true);
			ChannelFuture future = bootstrap.bind(port).sync();
			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO: handle exception
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}

	}

	private class NettyServerHandler extends
			SimpleChannelInboundHandler<Object> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			// TODO 自动生成的方法存根
			// System.out.println("Broker Server Message:Channel Active...");
		}

		@Override
		protected void channelRead0(ChannelHandlerContext arg0, Object arg1)
				throws Exception {
			// Future<Object> future = null;
			// System.out.println("RPC Provider Message:Strat read RpcRequest...");
			// 拿到一个消息以后所做的处理

			if (arg1 instanceof Message) {
				final Message message = (Message) arg1;
				message.setMsgId(String.valueOf(messageID.getAndIncrement()));
				if (message.getpropertyHashMap().containsKey("filter")
						&& message.getProperty("filter").equals("null")) {
					//System.out.println("Broker Server:message(msgId = "
					//		+ message.getMsgId() + ")miss property!");
					SendResult sendResult = new SendResult();
					sendResult.setInfo("the message miss property!");
					sendResult.setMsgId(message.getMsgId());
					sendResult.setStatus(SendStatus.SUCCESS);
					arg0.writeAndFlush(sendResult);
					return;
				}
				//System.out.println("Message A:" + message.toString());
				String storeString = message.getMsgId() + "|"
						+ message.getTopic() + "|"
						+ new String(message.getBody()).toString() + "|"
						+ message.getBornTime() + "|" + message.isConsume()
						+ "|" + message.getpropertyHashMap();
				// ID|topic|body |boreTime |isConsumer|property
				// 0|T-test|Hello MOM|1439966526765|false|{area=us}
				Charset charset = Charset.forName("utf-8");
				byte[] storeBytes = storeString.getBytes(charset);
				// 将storeBytes写入磁盘中
				// ***********************************
				fileProxy.putAndForce(Integer.valueOf(message.getMsgId()),
						storeBytes);
				String string = new String(storeBytes);

				SendResult sendResult = new SendResult();
				sendResult.setInfo(message.getTopic());
				sendResult.setMsgId(message.getMsgId());
				sendResult.setStatus(SendStatus.SUCCESS);
				arg0.writeAndFlush(sendResult);
				//System.out.println("Broker Server:recieve message(msgId = "
				//		+ message.getMsgId() + ")");
				// 获取关注该topic类型的consumer，将消息推送给他们
				HashMap<String, ArrayList<DefaultConsumer>> consumersHashMap = ConsumerSubscribeInfor
						.getConsumerList().get(message.getTopic());
				ArrayList<DefaultConsumer> consumers = new ArrayList<DefaultConsumer>();
				// 处理consumers，将同一组的consumer只保留一个
				for (ArrayList<DefaultConsumer> consumerArrayList : consumersHashMap
						.values()) {
					Random random = new Random();
					consumers.add(consumerArrayList.get(random
							.nextInt(consumerArrayList.size())));
				}
				// 根据消息的筛选属性来排除不满足的consumer
				ArrayList<DefaultConsumer> removeConsumers = new ArrayList<DefaultConsumer>();
				for (DefaultConsumer consumer : consumers) {
					if (consumer.getProperties().containsKey("filter")
							&& consumer.getProperties().get("filter")
									.equals("null"))
						continue;
					// 剔除不满足条件的消费者
					Map<String, String> messagePropertyMap = message
							.getpropertyHashMap();
					Set<String> messagePropertyKeySet = messagePropertyMap
							.keySet();
					for (String propertyKeyString : messagePropertyKeySet) {
						if (!consumer.getProperties().containsKey(
								propertyKeyString)) {
							removeConsumers.add(consumer);
							break;
						}
						if (!consumer
								.getProperties()
								.get(propertyKeyString)
								.equals(messagePropertyMap
										.get(propertyKeyString))) {
							removeConsumers.add(consumer);
							break;
						}
					}
				}
				// 删除不符合要求的消费者
				consumers.removeAll(removeConsumers);

				// 准备将消息推送给consumer
//				System.out.println("Broker Server:send message(msgId = "
//						+ message.getMsgId() + ") to consumers");

				for (DefaultConsumer consumer : consumers) {
//					System.out.println("Broker Server:Consumer"
//							+ consumer.getConsumerId() + " = "
//							+ consumer.toString());

					final InetSocketAddress remoteAddress = new InetSocketAddress(
							consumer.getConsumerIp(),
							consumer.getConsumerPort());
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
						Broker.failQueue.put(new FailTask(message.getMsgId(),
								consumer.getConsumerId()));
						if (failPushHashMap.containsKey(consumer
								.getConsumerId())) {
							failPushHashMap.get(consumer.getConsumerId()).add(
									message.getMsgId());
						} else {
							ArrayList<String> arrayList = new ArrayList<String>();
							arrayList.add(message.getMsgId());
							failPushHashMap.put(consumer.getConsumerId(),
									arrayList);
						}
						//e.printStackTrace();
						continue;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Broker.failQueue.put(new FailTask(message.getMsgId(),
								consumer.getConsumerId()));
						if (failPushHashMap.containsKey(consumer
								.getConsumerId())) {
							failPushHashMap.get(consumer.getConsumerId()).add(
									message.getMsgId());
						} else {
							ArrayList<String> arrayList = new ArrayList<String>();
							arrayList.add(message.getMsgId());
							failPushHashMap.put(consumer.getConsumerId(),
									arrayList);
						}
						//e.printStackTrace();
						continue;
					}
					if (!((ConsumeResult) response.getResponse()).getStatus()
							.equals(ConsumeStatus.SUCCESS)) {
						Broker.failQueue.put(new FailTask(message.getMsgId(),
								consumer.getConsumerId()));
						if (failPushHashMap.containsKey(consumer
								.getConsumerId())) {
							failPushHashMap.get(consumer.getConsumerId()).add(
									message.getMsgId());
						} else {
							ArrayList<String> arrayList = new ArrayList<String>();
							arrayList.add(message.getMsgId());
							failPushHashMap.put(consumer.getConsumerId(),
									arrayList);
						}
					}
				}
				return;
			}
			// 消费者注册
			if (arg1 instanceof DefaultConsumer) {
				// 将Consumer的信息注册到ConsumerSubscr信息中
				SocketAddress consumerClientAddress = arg0.channel()
						.remoteAddress();
				// consumerClientAddress.toString() = /127.0.0.1:xxxx
				String consumerListenerIp = consumerClientAddress.toString()
						.split(":")[0].substring(1);
				DefaultConsumer consumer = (DefaultConsumer) arg1;
				consumer.setConsumerIp(consumerListenerIp);
				// 生成唯一的consumerID
				if (consumer.getConsumerId().equals("0")) {
					consumer.setConsumerId(String.valueOf(consumerID
							.incrementAndGet()));
				}
				ConsumerSubscribeInfor.addProp(consumer.getTopic(), consumer);
				SubConsumersByID.consumers.put(consumer.getConsumerId(),
						consumer);
//				System.out
//						.println("Broker Server:Recieve consumer register information,consumer = "
//								+ consumer.toString());
				// 将结果根据consumer对象的consumerIp和consumerPort将结果写回给consumerServer端
				/******************************
				 * 
				 * 需要补充 netty客户端的东西
				 ******************************/
				ConsumeResult consumerResult = new ConsumeResult();
				consumerResult.setInfo("Consumer subscribe success!"
						+ consumer.getConsumerId());
				consumerResult.setStatus(ConsumeStatus.SUCCESS);
				arg0.writeAndFlush(consumerResult);

				if (failPushHashMap.containsKey(consumer.getConsumerId())) {
					ArrayList<String> msgIDSet = failPushHashMap.get(consumer
							.getConsumerId());
					for (String msgID : msgIDSet) {
						// 根据msgID获得message
						String string = new String(fileProxy.get(Integer
								.valueOf(msgID)));
						String[] messageInfor = string.split("\\|");
						Message m = new Message();
						m.setMsgId(messageInfor[0]);
						m.setTopic(messageInfor[1]);
						m.setBody(messageInfor[2].getBytes());
						m.setBornTime(Long.valueOf(messageInfor[3]));
						m.setConsume(Boolean.valueOf(messageInfor[4]));
						String tempProperty = messageInfor[5];
						tempProperty = tempProperty.replace("{", "");
						tempProperty = tempProperty.replace("}", "");
						String[] temps = tempProperty.split(",");
						int len = temps.length;
						for (int i = 0; i < len; i++) {
							String[] s = temps[i].trim().split("=");
							m.setProperty(s[0], s[1]);
						}
						// *************************************************

						final InetSocketAddress remoteAddress = new InetSocketAddress(
								consumer.getConsumerIp(),
								consumer.getConsumerPort());
						int maxConnNum = 10;
						int timeout = 3000;

						BrokerNettyClientProxy client = new BrokerNettyClientProxy.ConfigBuilder()
								.maxConnNum(maxConnNum).timeout(timeout)
								.forbidForceConnect(false).build();

						BrokerSimpleNettyResponseFuture future;
						BrokerSimpleNettyResponse response = null;
						try {
							future = client.request(remoteAddress, m);
							response = future.get();
							Broker.failQueue.remove(new FailTask(msgID,
									consumer.getConsumerId()));
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							Broker.failQueue.put(new FailTask(m.getMsgId(),
									consumer.getConsumerId()));
							if (failPushHashMap.containsKey(consumer
									.getConsumerId())) {
								failPushHashMap.get(consumer.getConsumerId())
										.add(m.getMsgId());
							} else {
								ArrayList<String> arrayList = new ArrayList<String>();
								arrayList.add(m.getMsgId());
								failPushHashMap.put(consumer.getConsumerId(),
										arrayList);
							}
							//e.printStackTrace();
							continue;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							Broker.failQueue.put(new FailTask(m.getMsgId(),
									consumer.getConsumerId()));
							if (failPushHashMap.containsKey(consumer
									.getConsumerId())) {
								failPushHashMap.get(consumer.getConsumerId())
										.add(m.getMsgId());
							} else {
								ArrayList<String> arrayList = new ArrayList<String>();
								arrayList.add(m.getMsgId());
								failPushHashMap.put(consumer.getConsumerId(),
										arrayList);
							}
							//e.printStackTrace();
							continue;
						}
						if (!((ConsumeResult) response.getResponse())
								.getStatus().equals(ConsumeStatus.SUCCESS)) {
							Broker.failQueue.put(new FailTask(m.getMsgId(),
									consumer.getConsumerId()));
							if (failPushHashMap.containsKey(consumer
									.getConsumerId())) {
								failPushHashMap.get(consumer.getConsumerId())
										.add(m.getMsgId());
							} else {
								ArrayList<String> arrayList = new ArrayList<String>();
								arrayList.add(m.getMsgId());
								failPushHashMap.put(consumer.getConsumerId(),
										arrayList);
							}
						}

					}
				}
				return;
			}
		}

		// 向用户投递消息

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			// TODO 自动生成的方法存根
			ctx.channel().close();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			// TODO 自动生成的方法存根
			// System.out.println("except");
//			SocketAddress consumerClientAddress = ctx.channel().remoteAddress();
//			System.out.println("Broker Server:exeception remoteAddress = "
//					+ consumerClientAddress.toString());
//			cause.printStackTrace();
			ctx.channel().close();
		}

		/*
		 * class CallableTask implements Callable<Object>{
		 * 
		 * private int sleepTime; private String task; public
		 * CallableTask(String task,int sleepTime) { // TODO 自动生成的构造函数存根
		 * this.sleepTime = sleepTime; this.task = task; }
		 * 
		 * @Override public Object call() throws Exception { // TODO 自动生成的方法存根
		 * try { Thread.sleep(sleepTime); } catch (InterruptedException e) { }
		 * Response response = new Response(task.toUpperCase()); return
		 * response; }
		 */

	}
}
