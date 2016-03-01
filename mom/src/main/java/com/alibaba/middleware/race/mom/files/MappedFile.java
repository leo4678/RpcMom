package com.alibaba.middleware.race.mom.files;

import java.awt.List;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author tokysky (HIT-CS-ICES)
 * @time 于2015年8月14日上午11:14:39
 *
 * @description
 **/

public class MappedFile{

	// 文件路径
	private String filepath;
	/** 内存映射文件变量 **/
	// 映射缓冲区
	private MappedByteBuffer cacheBuffer;
	// 映射缓冲区总容量
	private int mappedTotalCapacity;
	// 映射缓冲区索引容量
	private int mappedIndexCapacity;
	//当前索引可写位置
	private int mappedIndexIndex;
	//当前内容写位置
	private int mappedContentIndex; 
	// 文件通道
	private FileChannel fileChannel;
	// 文件
	private File file;
	//
	private CountDownLatch latch = new CountDownLatch(0);
	//索引的内存队列
	private ArrayList<IndexObject> indexList;
	
	
	public MappedFile(String filepath,int totalCapacity,int indexCapacity) throws IOException {
		this.filepath = filepath;
		mappedTotalCapacity = totalCapacity;
		mappedIndexCapacity = indexCapacity;
		mappedIndexIndex = 0;
		mappedContentIndex = mappedIndexCapacity;
		indexList =  new ArrayList<IndexObject>();
		file = new File(filepath);
		boolean fpExist = file.exists();
		RandomAccessFile raf = null;
		//文件存在即打开
		//文件不存在则新建
		try {
			raf = new RandomAccessFile(file, "rws");
			fileChannel = raf.getChannel();
			cacheBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE,
					0, mappedTotalCapacity);
			if(fpExist){
				initializeIndexList();
			}else{
				initializeMappedFile();
			}
		} catch (IOException e) {
			// TODO: handle exception
			//logger.error(e,"fail to mapped file");
			throw e;
		}finally{
			raf.close();
		}
	}
	
	/**
	 * 
	 * @description 当映射文件存在时，读取创建时写入的索引总容量和当前索引总数，
	 *				然后根据当前索引总数初始化索引列表
	 *
	 */
	private void initializeIndexList(){
		int tmp_IndexCapacity = cacheBuffer.getInt();
		mappedIndexCapacity = 0 == tmp_IndexCapacity? mappedIndexCapacity : tmp_IndexCapacity;
		int indexCounter = cacheBuffer.getInt();
		IndexObject object = null;
		for(int i = 0;i< indexCounter;++i){
			object = new IndexObject(cacheBuffer.getInt(),cacheBuffer.getShort());
			object.setIsDone(cacheBuffer.get());
			cacheBuffer.get();
			indexList.add(object);
		}
		if(null == object){
			mappedContentIndex = mappedIndexCapacity;
		}else{
			mappedIndexIndex = 8 + 8 * indexCounter; 
			mappedContentIndex = indexList.get(indexCounter -1).getPosition() + indexList.get(indexCounter -1).getLength();
		}
	}
	/**
	 * 
	 * @description 初始化新建的文件，写入索引总容量和当前索引总数
	 *
	 */
	private void initializeMappedFile(){
		cacheBuffer.putInt(mappedIndexCapacity);
		cacheBuffer.putInt(0);
		cacheBuffer.force();
		mappedIndexIndex = 8;
	}
	
	public void close() throws IOException {
		try {
			fileChannel.close();
		} catch (Exception e) {
			// TODO: handle exception
			//logger.error(e,"fail to close file channel");
			//throw e;
		}
	}

	public void put(byte[] src) {
		cacheBuffer.put(src);
		cacheBuffer.force();
	}

	public byte[] get(int offset){
		if(offset >= indexList.size()){
			return null;
		}
		IndexObject indexObject = indexList.get(offset);
		byte[] dst = new byte[indexObject.getLength()];
		int position = cacheBuffer.position();
		cacheBuffer.position(indexObject.getPosition());
		cacheBuffer.get(dst);
		cacheBuffer.position(position);
		return dst;
	}
	
	public synchronized void putAndForce(byte[] src) {
		try {
			latch.await();
		} catch (Exception e) {
			// TODO: handle exception
		}
		latch = new CountDownLatch(1);
		IndexObject indexObject = new IndexObject(mappedContentIndex, (short)src.length);
		indexList.add(indexObject);
		cacheBuffer.position(mappedContentIndex);
		cacheBuffer.put(src);
		cacheBuffer.position(mappedIndexIndex);
		cacheBuffer.putInt(mappedContentIndex);
		cacheBuffer.putShort((short)src.length);
		cacheBuffer.put((byte)0);
		cacheBuffer.put((byte)0);
		mappedIndexIndex += 8;
		mappedContentIndex += src.length;
		cacheBuffer.position(4);
		cacheBuffer.putInt(indexList.size());
		cacheBuffer.position(mappedContentIndex);
		cacheBuffer.force();
		latch.countDown();
	}
	
	public static void main(String[] args) throws IOException {

		String filepath = "test.txt";
		int mappedTotalSize = 1024* 1024 * 1;
		int mappedIndexSize = 1024* 10; 
		MappedFile mappedFile = new MappedFile(filepath,mappedTotalSize,mappedIndexSize);
		long st,sp;
		for(int i =1 ;i<= 10; i++){
			st = System.currentTimeMillis();
			mappedFile.putAndForce(("hellohello" + i).getBytes());
			sp =  System.currentTimeMillis();
			System.out.println("落盘耗时: "+(sp- st));
		}
		for(int i=0;i< 10;++i){
			st = System.currentTimeMillis();
			System.out.println(new String(mappedFile.get(i)));
			sp =  System.currentTimeMillis();
			System.out.println("读出耗时: "+(sp- st));
		}
		for(int i =1 ;i<= 10; i++){
			st = System.currentTimeMillis();
			mappedFile.putAndForce(("hellohello" + i).getBytes());
			sp =  System.currentTimeMillis();
			System.out.println("落盘耗时: "+(sp- st));
		}
		for(int i=10;i< 20;++i){
			st = System.currentTimeMillis();
			System.out.println(new String(mappedFile.get(i)));
			sp =  System.currentTimeMillis();
			System.out.println("读出耗时: "+(sp- st));
		}
	}
}
