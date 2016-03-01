package com.alibaba.middleware.race.rpc.context;

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月12日 下午3:38:19
 */
public class RpcAddContextImpl implements RpcAddContext{
	/*private 
	public RpcAddContext(){
		
	}*/
	public String addContextMessage(String key ,Object value){
		RpcContext.props.put(key,value);					
		return "RpcContext add success!";
	}
}
