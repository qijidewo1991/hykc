package com.hyb.utils;

import java.util.concurrent.TimeUnit;
import com.xframe.utils.AppHelper;

public class VehCheckThread extends Thread {
	private static final org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(VehCheckThread.class);
	private int checkInterval=0;
	private int offlineTimeout=0;
	public VehCheckThread(){
		java.util.HashMap<String, String> props=AppHelper.getSystemConf();
		checkInterval=Integer.parseInt(props.get("check_interval"));
		offlineTimeout=Integer.parseInt(props.get("offline_timeout"));
	}
	public void run(){
		while(true){
			try{
				Thread.sleep(checkInterval*1000);
				java.util.concurrent.ConcurrentHashMap<String, VehPosition> pointsMap=AppUtil.getCachedPoints();
				//log.info("检查车辆状态.."+strTime+",活跃车辆总数:"+pointsMap.size());
				AppUtil.sendAppMsg("VehCheckThread", "TOTAL", ""+pointsMap.size());
				for(String k:pointsMap.keySet()){
					VehPosition vp=pointsMap.get(k);
					if(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-vp.getLastUpdated())>offlineTimeout){
						AppUtil.sendAppMsg("VehCheckThread", "OFFLINE", ""+k);
					}
				}
				
			}catch(Exception ex){
				log.error("异常:"+ex.toString());
			}
		}
	}
}
