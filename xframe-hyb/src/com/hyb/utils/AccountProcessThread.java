package com.hyb.utils;

import com.xframe.core.CoreServlet;

public class AccountProcessThread extends Thread {
	private static final org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(AccountProcessThread.class);
	private int checkInterval=3; //每隔3秒钟检测
	//private boolean shouldProcessMessage=false;
	public AccountProcessThread(){
		//java.util.HashMap<String, String> props=AppHelper.getSystemConf();
		//if(props.containsKey("mq_process_msg") && props.get("mq_process_msg").trim().equals("true"))
		//	this.shouldProcessMessage=true;
	}
	public void run(){
		while(true){
			try{
				Thread.sleep(checkInterval*1000);
				if(!CoreServlet.zkMonitor.isMaster){
					continue;
				}
				log.info("检查是否有账户请求？"+AccountUtil.hasRequest());
				while(AccountUtil.hasRequest()){
					if(!AccountUtil.isBusy()){
						AccountUtil.processRequest();
					}
					Thread.sleep(1000);
				}
				
			}catch(Exception ex){
				log.error("异常:"+ex.toString());
			}
		}
	}
}
