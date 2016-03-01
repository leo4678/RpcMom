package com.alibaba.middleware.race.rpc.context;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.alibaba.middleware.race.rpc.api.impl.RpcConsumerImpl;
import com.alibaba.middleware.race.rpc.async.ResponseFuture;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.net.SimpleNettyResponse;
import com.alibaba.middleware.race.rpc.net.SimpleNettyResponseFuture;

/**
 * Created by huangsheng.hs on 2015/4/8.
 */

/**
 * @author:Leo(Li Da Wei) HIT-ICES
 * @time  :2015年8月12日 下午3:37:35
 */
public class RpcContext {

	//TODO how can I get props as a provider? tip:ThreadLocal
    public static Map<String,Object> props = new HashMap<String, Object>();
    private static RpcContextRequest contextRequest = null;

    public static void addProp(String key ,Object value){
    	//远程过程调用修改上下文环境
    	contextRequest = new RpcContextRequest(key,value);
    	//System.out.println("RPC consumer Message:contextRequest = " + contextRequest.toString());
    	//2.send request to server
//    	ExecutorService service = Executors.newSingleThreadExecutor();
//    	Future<Object> future = service.submit(new Callable<Object>() {
//
//			public Object call() throws Exception {
//				// TODO Auto-generated method stub
//				RpcConsumerImpl.nettyClient.write(contextRequest);
//				Object responeObject = RpcConsumerImpl.nettyClient.read();
//				return responeObject;
//			}
//		});
//    	try {
//			Object responeObject = ResponseFuture.getResponse(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}   	
    	SimpleNettyResponseFuture future;
		SimpleNettyResponse responseFuture;
    	try {
			future = RpcConsumerImpl.nettyClient.request(contextRequest);
			responseFuture = future.get(3000,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }

    public static Object getProp(String key){
        return props.get(key);
    }

    public static Map<String,Object> getProps(){
       return Collections.unmodifiableMap(props);
    }
    
}
