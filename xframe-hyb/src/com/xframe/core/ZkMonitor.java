package com.xframe.core;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.DataUpdater;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher.Event.KeeperState;

public class ZkMonitor {
	private ZkClient zkClient=null;
	public static final String node = "/tomcats";
	public static final String nodeUpdated=node+"/update";
	private String serverId="";
	public boolean isMaster=false;
	private static long lastUpdated=System.currentTimeMillis();
	
	public ZkMonitor(String serverId){
		this.serverId=serverId;
	}
	
	public void setData(String path,final String data){
		zkClient.updateDataSerialized(path, new DataUpdater<String>() {
            public String update(String currentData) {
                return data;
            }
        });
	}
	
	public boolean exists(String path){
		return zkClient.exists(path);
	}
	
	public void removePath(String path){
		zkClient.delete(path);
	}
	
	
	public void createPath(String path,String value){
		zkClient.create(path, value, CreateMode.PERSISTENT);
	}
	
	public String getData(String path){
		return zkClient.readData(path);
	}
	
	public void init(String strQuorum){
		String[] zkServers=strQuorum.split(",");
		StringBuffer sbZks=new StringBuffer();
		for(int i=0;i<zkServers.length;i++){
			if(sbZks.length()>0)
				sbZks.append(",");
			sbZks.append(zkServers[i]+":2181");
		}
		zkClient = new ZkClient(sbZks.toString());
		
		// 订阅监听事件
        childChangesListener(zkClient, node);
        dataChangesListener(zkClient, nodeUpdated);
        stateChangesListener(zkClient);

        if (!zkClient.exists(node)) {
            zkClient.createPersistent(node, this.serverId);
            zkClient.createPersistent(nodeUpdated,String.valueOf(System.currentTimeMillis()));
        }
        
        try {
        	checkIsMaster();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        System.out.println("ZkMonitor start working....");
	}
	
	private void checkIsMaster() throws Exception{
		final String strMaster=zkClient.readData(node);
		System.out.println("当前主服务器为:"+strMaster);
		/*
		if(!strMaster.equals(this.serverId)){
			String strUpdated=zkClient.readData(nodeUpdated);
			if(strUpdated!=null){
				long lUpdate=Long.parseLong(strUpdated);
				long duration=TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-lUpdate);
				System.out.println("\t1.上次更新间隔:"+duration+"秒");
				if(duration>30){
					changeToMaster(strMaster);
				}
			}
		}else{
			zkClient.writeData(node, this.serverId);
	    	zkClient.writeData(nodeUpdated, String.valueOf(System.currentTimeMillis()));
		}*/
		
		if(!strMaster.equals(this.serverId)){ //启动检查线程
			new Thread(){
				public void run(){
					while(!isMaster){
													
						//System.out.println("I am worker("+serverId+"),master is "+strMaster);
						long duration=TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()-lastUpdated);
						System.out.println("\tWorker(I am"+serverId+",master is "+strMaster+") checking....上次更新间隔:"+duration+"秒,now="+System.currentTimeMillis()+",last="+lastUpdated);
						if(duration>30){
							changeToMaster(strMaster);
							break;
						}

						try{
							Thread.sleep(5000);
						}catch(Exception ex){
							ex.printStackTrace();
						}
					}
				}
			}.start();
		}else{
			changeToMaster("OnStart");
		}
    	
	}
	
	
	public void changeToMaster(String from){
		System.out.println("切换主服务器为:"+this.serverId+"(FROM:"+from+")");
		zkClient.writeData(node, this.serverId);
		this.isMaster=true;
		new Thread(){
			public void run(){
				while(true){
					try{
						System.out.println("I am MASTER("+serverId+"),更新状态!");
						zkClient.writeData(nodeUpdated, System.currentTimeMillis()+"");
						Thread.sleep(10000);
					}catch(Exception ex){
					}
				}
			}
		}.start();
	}
	
	
	/**
     * 订阅children变化
     *
     * @param zkClient
     * @param path
     * @author SHANHY
     * @create  2016年3月11日
     */
    public static void childChangesListener(ZkClient zkClient, final String path) {
        zkClient.subscribeChildChanges(path, new IZkChildListener() {
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                System.out.println("clildren of path " + parentPath + ":" + currentChilds);
            }

        });
        
    }

    /**
     * 订阅节点数据变化
     *
     * @param zkClient
     * @param path
     * @author SHANHY
     * @create  2016年3月11日
     */
    public static void dataChangesListener(ZkClient zkClient, final String path){
        zkClient.subscribeDataChanges(path, new IZkDataListener(){

            public void handleDataChange(String dataPath, Object data) throws Exception {
                //System.out.println("Data of " + dataPath + " has changed.("+data.toString()+")");
                
                if(dataPath.equals(nodeUpdated)){ //监听nodeUpdated的变换
                	lastUpdated=System.currentTimeMillis();
                	//System.out.println("\tlastUPdate==="+lastUpdated);
                }
            }

            public void handleDataDeleted(String dataPath) throws Exception {
                System.out.println("Data of " + dataPath + " has deleted.");
            }

        });
    }

    /**
     * 订阅状态变化
     *
     * @param zkClient
     * @author SHANHY
     * @create  2016年3月11日
     */
    public static void stateChangesListener(ZkClient zkClient){
        zkClient.subscribeStateChanges(new IZkStateListener() {

            public void handleStateChanged(KeeperState state) throws Exception {
                System.out.println("handleStateChanged");
            }

            public void handleSessionEstablishmentError(Throwable error) throws Exception {
                System.out.println("handleSessionEstablishmentError");
            }

            public void handleNewSession() throws Exception {
                System.out.println("handleNewSession");
            }
        });
    }
}
