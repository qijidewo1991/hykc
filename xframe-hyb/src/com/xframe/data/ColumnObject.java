package com.xframe.data;

public class ColumnObject implements java.io.Serializable {
	private static final long serialVersionUID = -3125313922034663757L;
	private byte[] family;
	private byte[] qualifier;
	private byte[] data;
	
	public ColumnObject(byte[] faml,byte[] qual,byte[] data){
		this.family=faml;
		this.qualifier=qual;
		this.data=data;
	}
	
	public byte[] getFamily() {
		return family;
	}
	public void setFamily(byte[] family) {
		this.family = family;
	}
	
	public byte[] getQualifier() {
		return qualifier;
	}

	public void setQualifier(byte[] qualifier) {
		this.qualifier = qualifier;
	}

	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
