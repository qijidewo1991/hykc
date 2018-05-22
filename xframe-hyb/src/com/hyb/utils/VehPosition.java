package com.hyb.utils;

public class VehPosition implements java.io.Serializable {
	private static final long serialVersionUID = -8548386483923553343L;
	private long lastUpdated;
	private String id;
	private java.util.HashMap<String, String> data;
	public long getLastUpdated() {
		return lastUpdated;
	}
	public void setLastUpdated(long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	public java.util.HashMap<String, String> getData() {
		return data;
	}
	public void setData(java.util.HashMap<String, String> data) {
		this.data = data;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
}
