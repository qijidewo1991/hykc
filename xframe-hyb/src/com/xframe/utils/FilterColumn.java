package com.xframe.utils;

public class FilterColumn {
	private String family;
	private String qualifier;
	public String getFamily() {
		return family;
	}
	public void setFamily(String family) {
		this.family = family;
	}
	public String getQualifier() {
		return qualifier;
	}
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	
	public FilterColumn(String faml,String qual){
		this.family=faml;
		this.qualifier=qual;
	}
}
