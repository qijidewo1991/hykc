package com.hyb.utils;

public class WebUser implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8686878464643314719L;
	private String deptId;
	private String deptName;
	private String userId;
	private String userName;
	private String gw;
	private String rowid;
	private boolean online=false;
	private String uesrType;
	public String getDeptId() {
		return deptId;
	}
	public void setDeptId(String deptId) {
		this.deptId = deptId;
	}
	public String getDeptName() {
		return deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getGw() {
		return gw;
	}
	public void setGw(String gw) {
		this.gw = gw;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "WebUser:"+deptId+"|"+deptName+"|"+userId+"|"+userName;
	}
	public String getRowid() {
		return rowid;
	}
	public void setRowid(String rowid) {
		this.rowid = rowid;
	}
	public boolean isOnline() {
		return online;
	}
	public void setOnline(boolean online) {
		this.online = online;
	}
	public String getUesrType() {
		return uesrType;
	}
	public void setUesrType(String uesrType) {
		this.uesrType = uesrType;
	}
}
