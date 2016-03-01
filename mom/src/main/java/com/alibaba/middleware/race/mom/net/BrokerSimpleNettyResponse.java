package com.alibaba.middleware.race.mom.net;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月16日下午9:02:48
 *
 * @description 
 **/

public class BrokerSimpleNettyResponse {
	private boolean success;
	private Throwable cause;
	private Object response;
	
	public BrokerSimpleNettyResponse(Object response){
		super();
		this.response = response;
	}
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public Throwable getCause() {
		return cause;
	}
	
	public void setCause(Throwable cause) {
		this.cause = cause;
	}
	
	public Object getResponse() {
		return response;
	}
	
	public void setResponse(Object response) {
		this.response = response;
	}
}
