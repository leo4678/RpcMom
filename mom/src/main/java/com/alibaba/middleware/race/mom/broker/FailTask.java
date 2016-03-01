package com.alibaba.middleware.race.mom.broker;

import java.util.ArrayList;

public class FailTask{
	private String msgID;
	private String consumerID;
	public FailTask(String msgID,String consumerID){
		this.msgID = msgID;
		this.consumerID = consumerID;
	}
	public String getMsgID() {
		return msgID;
	}
	public void setMsgID(String msgID) {
		this.msgID = msgID;
	}
	public String getConsumerID() {
		return consumerID;
	}
	public void setConsumerID(String consumerID) {
		this.consumerID = consumerID;
	}
}
