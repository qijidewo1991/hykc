package com.xframe.data;

public class RecordObject implements java.io.Serializable{
	private static final long serialVersionUID = 6862215697466809502L;
	private byte[] rowid;
	private String name;
	private ColumnObject[] columns;
	public byte[] getRowid() {
		return rowid;
	}
	public void setRowid(byte[] rowid) {
		this.rowid = rowid;
	}
	public ColumnObject[] getColumns() {
		return columns;
	}
	public void setColumns(ColumnObject[] columns) {
		this.columns = columns;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}
