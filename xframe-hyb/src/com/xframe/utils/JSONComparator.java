package com.xframe.utils;

import java.util.Comparator;
import java.util.HashMap;

public class JSONComparator implements Comparator<HashMap<String,String>> {
	private String column;
	private int sort;
	
	public JSONComparator(String col,int sort){
		this.column=col;
		this.sort=sort;
	}
	
	@Override
	public int compare(HashMap<String,String> o1, HashMap<String,String> o2) {
		// TODO Auto-generated method stub
		String val1=o1.get(column);
		String val2=o2.get(column);
		
		if(sort==XHelper.SORT_ASC){
			return val1.compareTo(val2);
		}else{
			return val2.compareTo(val1);
		}
	}

}
