package com.alibaba.middleware.race.rpc.context;

import java.io.Serializable;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月12日 下午3:37:55
 */
public class RpcContextRequest implements Serializable{
	private String keyString;
	private Object valueString;

	public RpcContextRequest(String key,Object value){
		this.keyString = key;
		this.valueString = value;
	}
	public String getKeyString() {
		return keyString;
	}
	public void setKeyString(String keyString) {
		this.keyString = keyString;
	}
	public Object getValueString() {
		return valueString;
	}
	public void setValueString(Object valueString) {
		this.valueString = valueString;
	}
	public String toString(){
		return "key = " + this.keyString + ";value = " + this.valueString; 
	}
}
