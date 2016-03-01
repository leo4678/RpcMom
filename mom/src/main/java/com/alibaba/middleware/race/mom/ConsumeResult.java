package com.alibaba.middleware.race.mom;

import java.io.Serializable;

public class ConsumeResult implements Serializable{
	
	private static final long serialVersionUID = -3268674239546547541L;
	private ConsumeStatus status=ConsumeStatus.FAIL;
	private String info;
	public void setStatus(ConsumeStatus status) {
		this.status = status;
	}
	public ConsumeStatus getStatus() {
		return status;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public String getInfo() {
		return info;
	}
	public String toString(){
		return "ConsumeResult:{info = " + this.info + ",status = " + this.status + "}";
	}
}
