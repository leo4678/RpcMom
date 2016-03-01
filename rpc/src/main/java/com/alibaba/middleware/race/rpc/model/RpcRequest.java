package com.alibaba.middleware.race.rpc.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by huangsheng.hs on 2015/5/7.
 */
public class RpcRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4273785902461753988L;

	private String interfaceName;	
	private String methodName;
	private Object[] params;
	
	public String getInterfaceName() {
		return interfaceName;
	}

	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}

	
	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String toString() {
		return this.interfaceName + "." + methodName + "(" + params.toString() + ")";
		//return interfaces.getName() + "." + methodName;
	}	
}
