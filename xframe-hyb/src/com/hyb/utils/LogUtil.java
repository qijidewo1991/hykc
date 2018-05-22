package com.hyb.utils;

import java.io.IOException;

import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.xframe.utils.HBaseUtils;

public class LogUtil {
	public static final String DEBUG="debug";
	public static final String INFO="info";
	public static final String WARN="warn";
	public static final String ERROR="error";
	
	public static final String CAT_ACCOUNT="ACCOUNT";
	public static final String CAT_YD="YD";
	public static final String CAT_MAINTAIN="MAINTAIN";
	
	public static void debug(String category,String msg,String detail){
		log(DEBUG,category,msg,detail);
	}
	
	public static void info(String category,String msg,String detail){
		log(INFO,category,msg,detail);
	}
	
	public static void warn(String category,String msg,String detail){
		log(WARN,category,msg,detail);
	}
	
	public static void warn(String category,String msg,String detail,String user){
		log(WARN,category,msg,detail,user);
	}
	
	public static void error(String category,String msg,String detail){
		log(ERROR,category,msg,detail);
	}
	
	private static void log(String level,String category,String msg,String detail){
		log(level,category,msg,detail,null);
	}
	
	private static void log(String level,String category,String msg,String detail,String user){
		java.util.Date now=new java.util.Date();
		String time=org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss S", now);
		HConnection connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			userTable=connection.getTable("syslogs");
			Put put=new Put(Bytes.toBytes(level+"-"+category+"-"+org.mbc.util.Tools.formatDate("yyyyMMdd_HHmmssS", now)));
			put.add(Bytes.toBytes("data"),Bytes.toBytes("time"),Bytes.toBytes(time));
			put.add(Bytes.toBytes("data"),Bytes.toBytes("level"),Bytes.toBytes(level));
			put.add(Bytes.toBytes("data"),Bytes.toBytes("category"),Bytes.toBytes(category));
			put.add(Bytes.toBytes("data"),Bytes.toBytes("msg"),Bytes.toBytes(msg));
			put.add(Bytes.toBytes("data"),Bytes.toBytes("detail"),Bytes.toBytes(detail));
			if(user!=null)
				put.add(Bytes.toBytes("data"),Bytes.toBytes("user"),Bytes.toBytes(user));
            userTable.put(put);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(userTable!=null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection(connection);
		}
	}
}
