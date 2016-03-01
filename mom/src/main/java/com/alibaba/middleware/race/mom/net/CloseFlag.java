package com.alibaba.middleware.race.mom.net;

import java.io.Serializable;

public class CloseFlag implements Serializable{
	private String closeString = "gg";
	public CloseFlag(){		
	}
	public String toString(){
		return closeString;
	}
}
