package com.xframe.core;

public interface MessageListener {
	public void onMessageArrived(String topic,String me);
	public void onConnectionEvent(String evtName);
}
