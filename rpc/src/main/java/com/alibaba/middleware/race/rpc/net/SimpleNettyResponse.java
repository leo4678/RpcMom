package com.alibaba.middleware.race.rpc.net;

/**
 * @author 	 tokysky (HIT-CS-ICES) 
 * @time	  于2015年8月12日下午6:46:54
 *
 * @description 
 **/

public class SimpleNettyResponse {
	private boolean success;
	private Throwable cause;
	private Object response;
	
	public SimpleNettyResponse(Object response){
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
