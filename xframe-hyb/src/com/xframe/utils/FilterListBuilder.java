package com.xframe.utils;

import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.FamilyFilter;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.filter.RegexStringComparator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.JSONException;

public class FilterListBuilder {
	public static final int BinaryCompare=0;
	public static final int RegexCompare=1;
	public static final int SubstrCompare=2;
	public static final byte[] POSTFIX=new byte[1];
	
	public static final String FLAG_REC_COUNT="##REC_COUNT";
	public static final String FLAG_LAST_ROW="##LAST_ROW";
	
	private java.util.ArrayList<Filter> list=new java.util.ArrayList<Filter>();
	private org.json.JSONArray items=new org.json.JSONArray();
	private java.util.ArrayList<FilterColumn> columns=new java.util.ArrayList<FilterColumn>();
	
	
	private CompareFilter.CompareOp getOperator(String op){
		CompareFilter.CompareOp cop=CompareFilter.CompareOp.EQUAL;
		if(op.equals(">"))
			cop=CompareOp.GREATER;
		else if(op.equals(">="))
			cop=CompareOp.GREATER_OR_EQUAL;
		else if(op.equals("<>"))
			cop=CompareOp.NOT_EQUAL;
		else if(op.equals("<"))
			cop=CompareOp.LESS;
		else if(op.equals("<="))
			cop=CompareOp.LESS_OR_EQUAL;
		else if(op.equals("="))
			cop=CompareOp.EQUAL;
		else	
			cop=CompareOp.NO_OP;
		return cop;
	}
	
	public void addFamilyFilter(int type,String op,String value) throws Exception{
		Filter filter=null;
		CompareFilter.CompareOp cop=getOperator(op);
		filter=new FamilyFilter(cop,getComparator(type,value));
		list.add(filter);
		
		
		org.json.JSONObject o=new org.json.JSONObject();
		o.put("field", "_family_"+type);
		o.put("op", op);
		o.put("value", value);
		o.put("type", type);
		items.put(o);
	}
	
	public void addQualifierFilter(int type,String op,String value) throws Exception{
		Filter filter=null;
		CompareFilter.CompareOp cop=getOperator(op);
		filter=new QualifierFilter(cop,getComparator(type,value));
		list.add(filter);
		
		
		org.json.JSONObject o=new org.json.JSONObject();
		o.put("field", "_qualifier_"+type);
		o.put("op", op);
		o.put("value", value);
		o.put("type", type);
		items.put(o);
	}
	
	public void addRowFilter(int type,String op,String value) throws Exception{
		Filter filter=null;
		CompareFilter.CompareOp cop=getOperator(op);
		filter=new RowFilter(cop,getComparator(type,value));
		list.add(filter);
		
		org.json.JSONObject o=new org.json.JSONObject();
		o.put("field", "_row_"+type);
		o.put("op", op);
		o.put("value", value);
		o.put("type", type);
		items.put(o);
		
	}
	
	private  ByteArrayComparable getComparator(int compType,String value){
		switch(compType){
		case BinaryCompare:
			return new BinaryComparator(Bytes.toBytes(value));
		case RegexCompare:
			return new RegexStringComparator(value);
		default:
			return new SubstringComparator(value);
		}
	}
	
	public void add(String column,String op,String value) throws Exception{	
		add(column,op,value,BinaryCompare);
	}
	
	public void add(String column,String op,String value,int type) throws Exception{
		if(column!=null && op!=null && value!=null){
			String[] fs=column.split(":");
			if(fs.length!=2)
				return;
			columns.add(new FilterColumn(fs[0],fs[1]));
			try{
				org.json.JSONObject o=new org.json.JSONObject();
				
				o.put("field", column);
				o.put("op", op);
				o.put("value", value);
				o.put("type",type);
				items.put(o);
				
				CompareFilter.CompareOp cop=getOperator(op);
				
				SingleColumnValueFilter scvf=new SingleColumnValueFilter(
						Bytes.toBytes(fs[0]),Bytes.toBytes(fs[1]),
						cop,getComparator(type,value));
				scvf.setFilterIfMissing(true);
				list.add(scvf);
				
				//SkipFilter skipFilter=new SkipFilter(scvf);
				//list.add(skipFilter);
				
				//System.out.println("ADDING::"+column+",oper="+op+",vlalue="+value+",type="+type+",list size="+list.size());
			}catch(Exception ex){
				ex.printStackTrace();
			}	
		}
		
	}
	
	public java.util.ArrayList<FilterColumn> getColumns(){
		return this.columns;
	}
	
	public String getFilterStr(){
		return getFilterStr(null);
	}
	
	public String getFilterStr(org.json.JSONArray filters){
		org.json.JSONObject params=new org.json.JSONObject();
		try {
			
			if(filters==null){
				params.put("version", "1.0");
				params.put("items", items);
			}else{
				params.put("version", "2.0");
				params.put("items", filters);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return params.toString();
	}
	
	public FilterList getFilterList(){
		return getFilterList("and");
	}
	
	public FilterList getFilterList(String type){
		String opType=type.toLowerCase();
		if(opType.equals("and"))
			return new FilterList(FilterList.Operator.MUST_PASS_ALL,list);
		else
			return new FilterList(FilterList.Operator.MUST_PASS_ONE,list);
	}
}
