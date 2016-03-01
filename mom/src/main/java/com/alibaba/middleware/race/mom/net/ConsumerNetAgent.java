package com.alibaba.middleware.race.mom.net;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.mom.ConsumeResult;
import com.alibaba.middleware.race.mom.DefaultConsumer;
import com.alibaba.middleware.race.mom.Message;
import com.alibaba.middleware.race.mom.converter.ByteObjConverter;

/**
 * @author tokysky (HIT-CS-ICES)
 * @time 于2015年8月10日上午10:47:03
 *
 * @description
 **/

public class ConsumerNetAgent {

	// broker远程ip
	private String brokerIp;
	// broker远程端口
	private int brokerPort;
	// 消费者自身绑定的端口
	private int consumerPort;
	// NIO服务器
	private ServerSocketChannel serverSocketChannel;
	// 选择器
	private Selector selector;
	// 运行标志
	private boolean runFlag;
	// 客户端发送缓存
	private ByteBuffer sendBuffer;
	// 对象处理函数接口
	private ConsumerNetAgentHandler handler;
	private CountDownLatch stopLatch;

	public static String consumerID = "";

	public ConsumerNetAgent(String brokerIp, int brokerPort, int consumerPort)
			throws IOException {
		// TODO 自动生成的构造函数存根
		runFlag = true;
		this.brokerIp = brokerIp;
		this.brokerPort = brokerPort;
		this.consumerPort = consumerPort;
		// 开启服务器套接字通道
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		while (true) {
			try {
				serverSocketChannel.socket().bind(
						new InetSocketAddress(this.consumerPort));
//				System.out.println("Consumer Server:bind port = "
//						+ this.consumerPort);
				break;
			} catch (Exception e) {
				++this.consumerPort;
			}
		}
		// 开启选择器
		selector = Selector.open();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	public void writeObjectToBuffer(Object object) {
		byte[] data = ByteObjConverter.ObjectToByte(object);
		if (data.length == 0) {
			return;
		}
		sendBuffer = ByteBuffer.allocate(data.length + 4);
		sendBuffer.putInt(data.length);
		sendBuffer.put(data);
		sendBuffer.flip();
	}

	// 同步注册
	
	public boolean syncRegister(Object object) throws IOException,
			TimeoutException {
		if (object == null) {
			return false;
		}
		// NIO客户端
		SocketChannel socketChannel;
		// 开启客户端通道
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		Selector regselector = Selector.open();
		socketChannel.register(regselector, SelectionKey.OP_CONNECT);
		socketChannel.connect(new InetSocketAddress(brokerIp, brokerPort));
		Set<SelectionKey> selectionKeys;
		Iterator<SelectionKey> iterator;
		SelectionKey key;
		byte[] data = ByteObjConverter.ObjectToByte(object);
		ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
		buffer.putInt(data.length);
		buffer.put(data);
		long st = System.currentTimeMillis();
		boolean waiting = true;
		while (waiting) {
			regselector.selectNow();
			selectionKeys = regselector.selectedKeys();
			iterator = selectionKeys.iterator();
			while (iterator.hasNext()) {
				key = iterator.next();
				iterator.remove();
				if (key.isConnectable()) {
					SocketChannel server = (SocketChannel) key.channel();
					server.configureBlocking(false);
					if (server.isConnectionPending()) {
						server.finishConnect();
					}
					server.register(regselector, SelectionKey.OP_WRITE);
				} else if (key.isWritable()) {
					buffer.flip();
					while (buffer.hasRemaining()) {
						int len = ((SocketChannel) key.channel()).write(buffer);
						if (len < 0) {
							throw new EOFException();
						}
						if (len == 0) {
							key.interestOps(key.interestOps()
									| SelectionKey.OP_WRITE);
							regselector.wakeup();
							break;
						}
					}
					((SocketChannel) key.channel()).register(regselector,
							SelectionKey.OP_READ);
				} else if (key.isReadable()) {
					Object response = readObjectFromChannel(key.channel());
					// 注册成功
					if (response instanceof ConsumeResult) {
						waiting = false;
						consumerID = ((ConsumeResult) response).toString()
								.split("!")[1];
						//将consumerID存储进本地文件
						File file = new File(DefaultConsumer.storeConsumerIDFilePath);
						if (!file.exists()){
							FileOutputStream fos = new FileOutputStream(file);
							DataOutputStream dos = new DataOutputStream(fos);
							dos.writeUTF(consumerID);
							dos.flush();
							dos.close();
							fos.close();
						}
						
						regselector.close();
						socketChannel.close();
					} else {
						this.stop();
						throw new RuntimeException();
					}
				}
			}
			if ((System.currentTimeMillis() - st) >= 5000) {
				regselector.close();
				socketChannel.close();
				throw new TimeoutException("timeout for register");
			}
		}
		return true;
	}

	public void registerHandler(ConsumerNetAgentHandler handler) {
		this.handler = handler;
	}

	private Object readObjectFromChannel(Channel channel) throws IOException {
		ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
		int dataLength;
		ByteBuffer dataBuffer;
		((SocketChannel) channel).read(lengthBuffer);
		dataLength = ByteObjConverter.byteArrayToInt(lengthBuffer.array());
		dataBuffer = ByteBuffer.allocate(dataLength);
		((SocketChannel) channel).read(dataBuffer);
		return ByteObjConverter.ByteToObject(dataBuffer.array());
	}

	private void selectionKeyHandler(SelectionKey key) throws IOException {
		// 作为服务器与客户端连接
		if (key.isAcceptable()) {
			SocketChannel client = ((ServerSocketChannel) key.channel())
					.accept();
			client.configureBlocking(false);
			client.register(selector, SelectionKey.OP_READ
					| SelectionKey.OP_WRITE);
		} else {
			if (key.isReadable()) {
				Object object = readObjectFromChannel(key.channel());
//				System.out.println("Consumer Server:recieve object class = "
//						+ object.getClass().getName());
//				System.out.println("Consumer Server:recieve message ID = "
//						+ ((Message) object).getMsgId());
				// if(null == object){
				// return;
				// }
				object = handler.actionPerform(object);
				// 发送给对方
				if (object != null) {
					writeObjectToBuffer(object);
				} else {
					throw new RuntimeException(
							"Consumer Server:Broker send message class fail.");
				}
			}
			if (key.isWritable()) {
				if (sendBuffer != null) {
					// ((SocketChannel) key.channel()).write(sendBuffer);
					while (sendBuffer.hasRemaining()) {
						int len = ((SocketChannel) key.channel())
								.write(sendBuffer);
						if (len < 0) {
							throw new EOFException();
						}
						if (len == 0) {
							key.interestOps(key.interestOps()
									| SelectionKey.OP_WRITE);
							selector.wakeup();
							break;
						}
					}
					sendBuffer = null;
				}
			}
		}
	}

	class RunTask implements Runnable {
		Set<SelectionKey> selectionKeys;
		Iterator<SelectionKey> iterator;
		SelectionKey selectionKey;

		public void run() {
			// TODO 自动生成的方法存根
			while (runFlag) {
				try {
					selector.selectNow();
					selectionKeys = selector.selectedKeys();
					iterator = selectionKeys.iterator();
					while (iterator.hasNext()) {
						selectionKey = iterator.next();
						iterator.remove();
						selectionKeyHandler(selectionKey);
					}
				} catch (Exception e) {
					if (selectionKeys != null) {
						try {
							selectionKey.channel().close();
							selectionKey.cancel();
						} catch (Exception e2) {
						}
					}
					e.printStackTrace();
				}
			}
			try {
				if (null != selectionKey) {
//					((SocketChannel) selectionKey.channel()).close();
					byte [] data = ByteObjConverter.ObjectToByte(new CloseFlag());
					ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
					buffer.putInt(data.length);
					buffer.put(data);
					buffer.flip();
					((SocketChannel) selectionKey.channel()).write(buffer);
					//System.out.println("send gg success");
				}
				selectionKeys = null;
				iterator = null;
				selectionKey = null;
				selector.close();
				selector = null;
				serverSocketChannel.close();
				serverSocketChannel = null;
				sendBuffer = null;
				handler = null;
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			} finally {
				stopLatch.countDown();
			}
		}
	}

	public void start() throws IllegalStateException {
		if (handler == null) {
			throw new IllegalStateException(
					"Do not register object handler yet.");
		}
		Thread thread = new Thread(new RunTask());
		thread.start();
	}

	public synchronized void stop() {
		//writeObjectToBuffer(new CloseFlag());
		stopLatch = new CountDownLatch(1);
		runFlag = false;
		try {
			stopLatch.await();
		} catch (Exception e) {
		}
	}

	public String getBrokerIp() {
		return brokerIp;
	}

	public void setBrokerIp(String brokerIp) {
		this.brokerIp = brokerIp;
	}

	public int getBrokerPort() {
		return brokerPort;
	}

	public void setBrokerPort(int brokerPort) {
		this.brokerPort = brokerPort;
	}

	public int getConsumerPort() {
		return consumerPort;
	}

	public void setConsumerPort(int consumerPort) {
		this.consumerPort = consumerPort;
	}

	public static void main(String[] args) {
		ConsumerNetAgent agent;
		// // try {
		// // agent = new ConsumerNetAgent("127.0.0.1", 8888, 10001);
		// // agent.registerHandler(new ConsumerNetAgentHandler() {
		// //
		// // public Object actionPerform(Object object) {
		// // // TODO 自动生成的方法存根
		// // System.out.println(object.toString());
		// // return null;
		// // }
		// // });
		// // agent.start();
		// // System.out.println("dhuaeigfeagufig");
		// // try {
		// // agent.syncRegister(new Person("zcc", "男", 24));
		// // } catch (TimeoutException e1) {
		// // // TODO 自动生成的 catch 块
		// // }
		// // agent.stop();
		// // agent = new ConsumerNetAgent("127.0.0.1", 8888, 10001);
		// // agent.registerHandler(new ConsumerNetAgentHandler() {
		// //
		// // @Override
		// // public Object actionPerform(Object object) {
		// // // TODO 自动生成的方法存根
		// // System.out.println(object.toString());
		// // return null;
		// // }
		// // });
		// // agent.start();
		// // try {
		// // agent.syncRegister(new Person("affe", "男", 24));
		// // } catch (TimeoutException e) {
		// // // TODO 自动生成的 catch 块
		// // e.printStackTrace();
		// // }
		// // System.out.println("adkoppopoajfkopoejop");
		// // System.out.println(agent.getConsumerPort());
		// //agent.stop();
		// } catch (IOException e) {
		// // TODO 自动生成的 catch 块
		// }
	}
}
