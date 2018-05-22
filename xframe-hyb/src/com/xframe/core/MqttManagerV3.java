package com.xframe.core;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.xframe.utils.AppHelper;



public class MqttManagerV3 implements INetManager {
	public static final String TOPIC_PREFIX="VirtualTopic.HYB.";
	public static final String TOPIC_BROADCAST="VirtualTopic.HYB.Broadcast";
	public static final String TOPIC_APP="VirtualTopic.HYB.App";
	public static final String TOPIC_GPS="VirtualTopic.HYB.Gps";
	public static final String TOPIC_LOCAL="VirtualTopic.HYB.Local";
	private static final org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(MqttManagerV3.class);
	private boolean isWorking=false;
	//private final static boolean CLEAN_START = true; 
	//private final static short KEEP_ALIVE = 30;//�ͺ����磬��������Ҫ��ʱ��ȡ���ݣ�����30s 
	//private boolean shouldProcessMessage=false;
	
	private final static int[] QOS_VALUES = {0,0}; 
	
	private MqttClient mqttClient = null; 
	
	private final String[] topics={TOPIC_GPS,TOPIC_LOCAL};
	
	private MessageListener msgListener=null;
	
	private static MqttManagerV3 instance=null;
	
	private MemoryPersistence persistence = new MemoryPersistence();
	
	public static MqttManagerV3 getInstance(){
		if(instance==null)
			instance=new MqttManagerV3();
		
		return instance;
	}
	
	private MqttManagerV3(){
		//java.util.HashMap<String, String> props=AppHelper.getSystemConf();
		
		//if(props.containsKey("mq_process_msg") && props.get("mq_process_msg").trim().equals("true"))
		//	this.shouldProcessMessage=true;
		//log.info("��������:"+props+",�Ƿ�����Ϣ:"+shouldProcessMessage);
		
	}
	
	@Override
	public void setRelayServer(String server) {
		// TODO Auto-generated method stub
	}
	

	@Override
	public void startManager() {
		// TODO Auto-generated method stub
		isWorking=true;
		
		new Thread(){
			public void run(){
				while(isWorking){
					if(mqttClient!=null && mqttClient.isConnected()){
						try{
							Thread.sleep(3000);
						}catch(Exception ex){
						}
						continue;
					}
					notifyConnectionEvent(INetManager.CONNECTING);
					boolean isCreated=createClient(topics);
					if(isCreated && mqttClient!=null){
						log.info("���ӳɹ�!");
						notifyConnectionEvent(INetManager.CONNECTED);
					}
					
					try{
						Thread.sleep(3000);
					}catch(Exception ex){
					}
				}
			}
		}.start();
	}

	@Override
	public void setMsgListener(MessageListener l) {
		// TODO Auto-generated method stub
		this.msgListener=l;
	}

	@Override
	public void notifyConnectionEvent(String evt) {
		// TODO Auto-generated method stub
		if(this.msgListener!=null)
			msgListener.onConnectionEvent(evt);
	}

	@Override
	public void send(String msg, String to) {
		// TODO Auto-generated method stub
		//if(!shouldProcessMessage)
		//	return;
		if(mqttClient!=null && mqttClient.isConnected()){
			try {
				//log.info("������Ϣ(=>"+to+"):"+msg);
				mqttClient.publish(to,msg.getBytes(),  0, false);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}

	@Override
	public void sendWithThread(final String msg,final String to) {
		// TODO Auto-generated method stub
		new Thread(){
			public void run(){
				send(msg,to);
			}
		}.start();
	}

	@Override
	public void stopManager() {
		// TODO Auto-generated method stub
		isWorking=false;
		if(mqttClient!=null){
			try {
				mqttClient.disconnect();
			}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				mqttClient=null;
			}
		}
	}
	
	private boolean createClient(String[] topics){
		if(QOS_VALUES.length==0 || QOS_VALUES.length!=topics.length){
			log.warn("��Ч��QOS_VALUES��TOPICS����!");
			return false;
		}
		
		try{ 
			
			
			java.util.HashMap<String, String> hm=AppHelper.getSystemConf();
			String server=hm.get("mq_server");
			String port=hm.get("mq_port");
			String user=hm.get("mq_user");
			String password=hm.get("mq_password");
			
			
			String broker = "tcp://"+server+":"+port;
			String clientId="tomcat-"+System.currentTimeMillis();
			mqttClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			connOpts.setUserName(user);
			connOpts.setPassword(password.toCharArray());
			System.out.println("Connecting to broker: "+broker);
			mqttClient.connect(connOpts);
			mqttClient.setCallback(new MqttCallbackHandler());
			mqttClient.subscribe(topics, QOS_VALUES);
			 /**
			 *��ɶ��ĺ󣬿���������������������ͨ����Ҳ���Է����Լ�����Ϣ 
			 */ 
			mqttClient.publish(topics[0], "keepalive".getBytes(), QOS_VALUES[0], true); 
		}catch (MqttException e) { 
			//e.printStackTrace(); 
			mqttClient=null;
			return false;
		} 
		
		return true;
	} 
	
	class MqttCallbackHandler implements MqttCallback{
    	@Override
		public void connectionLost(Throwable arg0) {
			// TODO Auto-generated method stub
			//System.out.println(">>>>>>>>>>>>>connectionLost!");
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken arg0) {
			// TODO Auto-generated method stub
		}
		
		/**
		 * ��Ϣ���մ�����
		 * ��Ϣ��ʽ����: ���Ͷ�ID+","+"��Ϣ����"+","+��Ϣ���� �� "CLIENT001,01,��¼֪ͨ"
		 */
		@Override
		public void messageArrived(String topic, MqttMessage msg)
				throws Exception {
			String strmsg=new String(msg.getPayload(),"utf-8");
			//log.info("��Ϣ:"+topic+",BODY="+msg);
			if(!CoreServlet.zkMonitor.isMaster)
				return;
			if(msgListener!=null){
				msgListener.onMessageArrived(topic, strmsg);
			}
		}
	};
	
	public void handleMessage(String topic,String msg){
		if(msgListener!=null){
			msgListener.onMessageArrived(topic, msg);
		}
	}

}
