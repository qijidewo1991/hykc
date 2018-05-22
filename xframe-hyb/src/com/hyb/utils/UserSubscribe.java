package com.hyb.utils;

public class UserSubscribe {
	private String vehType;
	private String vehLength;
	private String rawLines;
	private String ip;
	private java.util.HashSet<String> lines=new java.util.HashSet<String>();
	public String getVehType() {
		return vehType;
	}
	public void setVehType(String vehType) {
		this.vehType = vehType;
	}
	public String getVehLength() {
		return vehLength;
	}
	public void setVehLength(String vehLength) {
		this.vehLength = vehLength;
	}
	public java.util.HashSet<String> getLines() {
		return lines;
	}
	public void setLines(java.util.HashSet<String> lines) {
		this.lines = lines;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer sb=new StringBuffer();
		sb.append("ÏßÂ·:"+this.lines);
		return sb.toString();
	}
	public String getRawLines() {
		return rawLines;
	}
	public void setRawLines(String rawLines) {
		this.rawLines = rawLines;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
}
