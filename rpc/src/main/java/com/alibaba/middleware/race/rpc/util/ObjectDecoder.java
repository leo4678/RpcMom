package com.alibaba.middleware.race.rpc.util;

import java.util.List;

import io.netty.buffer.ByteBuf;  
import io.netty.channel.ChannelHandlerContext;  
import io.netty.handler.codec.ByteToMessageDecoder; 
/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月3日下午7:03:52
 *
 * @description 
 **/

public class ObjectDecoder extends ByteToMessageDecoder{

	@Override
	protected void decode(ChannelHandlerContext arg0, ByteBuf arg1,
			List<Object> arg2) throws Exception {
		// TODO 自动生成的方法存根
		if(arg1.readableBytes() < 4){
			return;
		}
		arg1.markReaderIndex();
		int dataLength = arg1.readInt();
		if(dataLength < 0){
			arg0.close();
			return;
		}
		if(arg1.readableBytes() < dataLength){
			arg1.resetReaderIndex();
			return;
		}
		byte[] datas = new byte[dataLength];
		arg1.readBytes(datas);
		Object obj = ByteObjConverter.ByteToObject(datas);
		if(obj != null){
			arg2.add(obj);
		}
	}
}
