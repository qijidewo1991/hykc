package com.cp.mqttutils;

import java.util.ArrayList;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.xframe.utils.AppHelper;

public class MyMQTT {
	private MqttClient client;
	private MqttMessage message;
	private MqttTopic mqtopic;
	private ArrayList<String> topicAll;
	public MyMQTT(String clientid){
		init(clientid);	
	}
	
	public void init(String clientid){
		java.util.HashMap<String, String> hm=AppHelper.getSystemConf();
		String server=hm.get("mq_server");
		String port=hm.get("mq_port");
		String user=hm.get("mq_user");
		String password=hm.get("mq_password");
		String broker = "tcp://"+server+":"+port;
		try {
			topicAll = new ArrayList<>();
			client = new MqttClient(broker, clientid, new MemoryPersistence());
			MqttConnectOptions options = new MqttConnectOptions();
			options.setCleanSession(false);
			options.setUserName(user);
			options.setPassword(password.toCharArray());
			// 设置超时时间
			options.setConnectionTimeout(10);
			// 设置会话心跳时间
			options.setKeepAliveInterval(20);
			PushCallback callBack=new PushCallback();
			try {
				client.setCallback(callBack);
				client.connect(options);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
}
	
}
