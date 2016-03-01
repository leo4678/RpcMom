package com.alibaba.middleware.race.rpc.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author tokysky (HIT-CS-ICES)
 * @time 于2015年8月3日下午7:12:28
 *
 * @description
 **/

public class ByteObjConverter {
	public static Object ByteToObject(byte[] bytes) {
		Object obj = null;
		ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
		// System.out.println("bytes:" + new String(bytes,0,bytes.length));
		ObjectInputStream oi = null;
		try {
			oi = new ObjectInputStream(bi);
			obj = oi.readObject();
		} catch (Exception e) {
		} finally {
			try {
				bi.close();
				oi.close();
			} catch (IOException e) {
			}
		}
		return obj;
	}

	public static byte[] ObjectToByte(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream oo = null;
		try {
			oo = new ObjectOutputStream(bo);
			oo.writeObject(obj);
			bytes = bo.toByteArray();
		} catch (Exception e) {
		} finally {
			try {
				bo.close();
				oo.close();
			} catch (IOException e) {
			}
		}
		return (bytes);
	}

	public static int byteArrayToInt(byte[] bytes) {
		return (bytes[3] & 0xFF) | (bytes[2] & 0xFF) << 8
				| (bytes[1] & 0xFF) << 16 | (bytes[0] & 0xFF) << 24;
	}
}
