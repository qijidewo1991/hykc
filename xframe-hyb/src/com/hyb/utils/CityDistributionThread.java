package com.hyb.utils;

import org.json.JSONException;
import org.json.JSONObject;

import personalmqttofw.MyMessage;
import personalmqttofw.MyMqttClient;
import personalmqttofw.MyMqttListener;

import com.xframe.core.CoreServlet;

public class CityDistributionThread extends Thread {
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(CityDistributionThread.class);
	private int checkInterval = 3; // ÿ��3���Ӽ��

	public void run() {
		// ��������mq
				MyMqttClient mq = new MyMqttClient("tcp://59.110.159.178:1883", "hykc_chengpei_server12", new MyMqttListener() {

					@Override
					public void sendChatSuccess(String arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void sendChatFailed(MyMessage arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void lostConnect() {
						// TODO Auto-generated method stub

					}

					@Override
					public void getHeartMessage(MyMessage arg0) {
						// TODO Auto-generated method stub
						try {
							// System.out.println("�յ���Ϣ��"+arg0.getMessageInfo());
							JSONObject obj = new JSONObject(arg0.getMessageInfo());
							double lat = obj.has("lat")?Double.parseDouble(obj.getString("lat")):0;
							double lon =obj.has("lon")?Double.parseDouble(obj.getString("lon")):0;
							double dir = obj.has("dir")?Double.parseDouble(obj.getString("dir")):0;
							String rowid = obj.getString("rowid");
							// String ms = obj.getString("MESSAGE");
							String app = "cp_arr_" + obj.getString("app");
							com.hyb.utils.RedisClient redis = com.hyb.utils.RedisClient.getInstance("");// ���geo
							redis.addPoint(app, lon, lat, rowid);// arr_driver,1,1,zhangsan
							System.out.println("�յ���Ϣheart��" + rowid + "," + lat + "," + lon + ",app=" + app);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void getChatMessage(MyMessage arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void getBrocastMessage(MyMessage arg0) {
						// TODO Auto-generated method stub

					}
				});
				String[] receiveTopic = { "chengpei_heart" };// ���ý��ܵ�topic
				mq.addReceiveTopic(receiveTopic);

				System.out.println("--------------��������������mq�ɹ�---------------");
		// �����߳�
		while (true) {
			try {
				Thread.sleep(checkInterval * 1000);
				if (!CoreServlet.zkMonitor.isMaster) {
					continue;
				}

				log.info("�������û��ӵ�����" + CityDistributionUtil.hasCityDisRequest());
				while (CityDistributionUtil.hasCityDisRequest()) {
					if (!CityDistributionUtil.isBusy()) {
						CityDistributionUtil.processRequest();
					}
					Thread.sleep(1000);
				}

			} catch (Exception ex) {
				log.error("�쳣:" + ex.toString());
			}
		}
	}
}
