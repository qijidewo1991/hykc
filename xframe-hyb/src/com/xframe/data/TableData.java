package com.xframe.data;

public class TableData implements java.io.Serializable {
	private static final long serialVersionUID = -3394214364156331560L;
	private String tableName;
	private RecordObject[] records;
	public RecordObject[] getRecords() {
		return records;
	}
	public void setRecords(RecordObject[] records) {
		this.records = records;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	
}

