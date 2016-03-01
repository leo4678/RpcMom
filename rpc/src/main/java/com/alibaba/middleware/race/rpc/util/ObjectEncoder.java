package com.alibaba.middleware.race.rpc.util;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月3日下午7:04:05
 *
 * @description 
 **/

public class ObjectEncoder extends MessageToByteEncoder<Object>{

	@Override
	protected void encode(ChannelHandlerContext arg0, Object arg1, ByteBuf arg2)
			throws Exception {
		// TODO 自动生成的方法存根
		byte[] datas = ByteObjConverter.ObjectToByte(arg1);
		arg2.writeInt(datas.length);
		arg2.writeBytes(datas);
		arg0.flush();
	}
}
