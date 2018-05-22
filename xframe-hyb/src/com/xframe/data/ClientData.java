package com.xframe.data;

public class ClientData implements java.io.Serializable {
	private static final long serialVersionUID = -3394214364156331560L;
	private String khh;
	private RecordObject[] records;
	private RecordObject userObject;
	private RecordObject khxx;
	public String getKhh() {
		return khh;
	}
	public void setKhh(String khh) {
		this.khh = khh;
	}
	public RecordObject[] getRecords() {
		return records;
	}
	public void setRecords(RecordObject[] records) {
		this.records = records;
	}
	public RecordObject getUserObject() {
		return userObject;
	}
	public void setUserObject(RecordObject userObject) {
		this.userObject = userObject;
	}
	public RecordObject getKhxx() {
		return khxx;
	}
	public void setKhxx(RecordObject khxx) {
		this.khxx = khxx;
	}
	
}

