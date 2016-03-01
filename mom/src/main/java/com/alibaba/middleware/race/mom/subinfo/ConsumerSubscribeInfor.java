package com.alibaba.middleware.race.mom.subinfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.alibaba.middleware.race.mom.DefaultConsumer;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月13日 下午4:50:16
 */
public class ConsumerSubscribeInfor {
	private static HashMap<String,HashMap<String, ArrayList<DefaultConsumer>>>  consumerSubscribeList = new HashMap<String,HashMap<String, ArrayList<DefaultConsumer>>>();
	
	public static void addProp(String key ,DefaultConsumer value){
		if (ConsumerSubscribeInfor.consumerSubscribeList.containsKey(key)){
			if ( ConsumerSubscribeInfor.consumerSubscribeList.get(key).containsKey(value.getGroupId()) ){
				ConsumerSubscribeInfor.consumerSubscribeList.get(key).get(value.getGroupId()).add(value);
			}else{
				ArrayList<DefaultConsumer> arrayList = new ArrayList<DefaultConsumer>();
				arrayList.add(value);
				ConsumerSubscribeInfor.consumerSubscribeList.get(key).put(value.getGroupId(), arrayList);
			}
		}else {			
			ArrayList<DefaultConsumer> arrayList = new ArrayList<DefaultConsumer>();
			arrayList.add(value);
			HashMap<String, ArrayList<DefaultConsumer>> hashMap = new HashMap<String, ArrayList<DefaultConsumer>>();
			hashMap.put(value.getGroupId(),arrayList);
			ConsumerSubscribeInfor.consumerSubscribeList.put(key, hashMap);
		}
    }
	public static HashMap<String,HashMap<String, ArrayList<DefaultConsumer>>> getConsumerList(){
		return ConsumerSubscribeInfor.consumerSubscribeList;
	}
}
