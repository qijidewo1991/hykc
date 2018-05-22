package com.cp.mqttutils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class PushCallback implements MqttCallback {

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		if(arg0.isComplete()){
				try {
					System.out.println("deliveryComplete---�������------"+new String(arg0.getMessage().getPayload()));
				} catch (MqttException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}else{
			System.out.println("deliveryComplete---����ʧ��------");
		}
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		System.out.println("������Ϣ����:"+arg0);
		//System.out.println("������ϢQos:"+arg1.getQos());
		System.out.println("������Ϣ����:"+new String(arg1.getPayload()));
	}

	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("���ӶϿ�������������");
	}

}
