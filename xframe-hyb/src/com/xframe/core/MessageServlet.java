package com.xframe.core;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.hyb.utils.Configuration;
import com.xframe.utils.AppHelper;

public class MessageServlet extends HttpServlet {
	private static final long serialVersionUID = 2777356452903223772L;
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(MessageServlet.class);
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		// TODO Auto-generated method stub
		String clientId=req.getParameter("cid");
		if(clientId==null || clientId.indexOf("@")<0)
			return;
		
		if(!MqttMessageProcessor.topics.containsKey(clientId)){
			return;
		}
		
		//log.info("Retrieve message:"+clientId+",topics:"+MqttMessageProcessor.topics);
		resp.setCharacterEncoding("UTF-8");
		
		java.util.HashSet<String> topics=MqttMessageProcessor.topics.get(clientId);
		org.json.JSONObject json=new org.json.JSONObject();
		for(String topic:topics){
			if(MqttMessageProcessor.msgQueue.containsKey(topic)){
				java.util.ArrayList<String> queue=MqttMessageProcessor.msgQueue.get(topic);
				int size=queue.size();
				org.json.JSONArray al=new org.json.JSONArray();
				for(int i=0;i<size;i++){
					al.put(queue.get(0));
					queue.remove(0);
				}
				try {
					json.put(topic, al);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		if(clientId.startsWith("hykc@")){
			org.json.JSONArray al=new org.json.JSONArray();
			try {
				//log.info("===>message itmes:"+Configuration.getRzRequests()+",size="+Configuration.getRzRequests().size());
				al.put("broadcast"+(char)0x03+""+MqttMessageProcessor.broadcastQueue.size());
				json.put("VirtualTopic.HYB.App", al);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		//添加广播消息
		/*
		String[] broadcasts=MqttMessageProcessor.broadcastQueue.toArray(new String[MqttMessageProcessor.broadcastQueue.size()]);
		try {
			json.put("Broadcast", broadcasts);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		resp.getWriter().write(json.toString());
		resp.getWriter().flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String clientId=req.getParameter("cid");
		if(clientId==null || clientId.indexOf("@")<0)
			return;
		String option=req.getParameter("option");
		if(option==null)
			return;
		
		//log.info("post message:"+clientId+",option="+option);
		AppHelper.printParameters(req);
		if(option.equals("login")){
			String strTopics=req.getParameter("topics");
			AppHelper.updateOnlineUser(clientId, strTopics);
		}else if(option.equals("logout")){
			AppHelper.removeOnlineUser(clientId);
		}else if(option.equals("send")){
			String strDest=req.getParameter("dest");
			String strMsg=req.getParameter("msg");
			MqttManagerV3.getInstance().handleMessage(strDest, strMsg);
		}
	}
}
