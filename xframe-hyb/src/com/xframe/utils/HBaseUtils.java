package com.xframe.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.*;
import org.json.JSONObject;

import com.google.protobuf.ByteString;

import processors.generated.HelperTools;
import processors.generated.HelperTools.HelperService;

public class HBaseUtils {
	public static String TYPE_NUMBER="NUMBER";
	public static String TYPE_INT="INTEGER";
	
	//private static HConnection hConnection=null;
	private static final Logger log=Logger.getLogger(HBaseUtils.class);
	private static Configuration hbaseConfig=null;
	public static boolean initializeHBase(String hMaster,String zooKeeperQuorum,String clientPort){
		try{
			hbaseConfig = HBaseConfiguration.create();
			hbaseConfig.set("hbase.master", hMaster);  
			hbaseConfig.set("hbase.zookeeper.quorum",zooKeeperQuorum); 
			hbaseConfig.set("hbase.zookeeper.property.clientPort",clientPort); 

			boolean isOk=false;
			HConnection hConnection = getHConnection();
			if(hConnection!=null){
				isOk=true;
				closeConnection(hConnection);
			}
			return isOk;
		}catch(Exception ex){
			log.error("≥ı ºªØHBase“Ï≥£:"+ex.toString());
			return false;
		}
	}
	
	public static Connection getConnection(){
		org.apache.hadoop.hbase.client.Connection conn=null;
		try {
			conn=ConnectionFactory.createConnection(hbaseConfig);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	}
	
	public static void closeConnection2(Connection conn){
		try{
			if(conn!=null)
				conn.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	
	@SuppressWarnings("deprecation")
	public static HConnection getHConnection(){
		HConnection connection=null;
		try{
			connection=HConnectionManager.createConnection(hbaseConfig);
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return connection;
	}
	
	public static void closeConnection(@SuppressWarnings("deprecation") HConnection connection){
		try{
			if(connection!=null)
				connection.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	public static long getRowCount(String tableName,String filterStr) throws Throwable{
		long total=0;
		HConnection connection=getHConnection();
		try{
	       	 HTable table=(HTable)connection.getTable(Bytes.toBytes(tableName));
	       	 
	       	 final HelperTools.RowCountRequest req=HelperTools.RowCountRequest.newBuilder().setFilterStr(ByteString.copyFromUtf8(filterStr)).build();
	       	 
	       	 Map<byte[], Long> results=table.coprocessorService(HelperTools.HelperService.class, null, null, new Batch.Call<HelperService, Long>() {
	
					@Override
					public Long call(HelperService helperService) throws IOException {
						// TODO Auto-generated method stub
						ServerRpcController controller = new ServerRpcController();
						BlockingRpcCallback<HelperTools.RowCountResponse> rpc=new BlockingRpcCallback<HelperTools.RowCountResponse>();
						helperService.getRowCount(controller, req, rpc);
						HelperTools.RowCountResponse response=rpc.get();
						if (controller.failedOnException()) {
	                       throw controller.getFailedOn();
	                     }
	                   return (long)((response != null && response.hasRows()) ? response.getRows() : 0);
					}
				});
	       	 
	       	 Iterator<Long> iter = results.values().iterator();
	       	 
	       	 while(iter.hasNext()){
	       		 total+=iter.next();
	       	 }
	       	 table.close();
        }catch(Exception ex){
        	ex.printStackTrace();
        }finally{
        	closeConnection(connection);
        }
		return total;
	}
	
	public static double getSum(String tableName,String filterStr,String colName,String colType) throws Throwable{
		double total=0;
		HConnection connection=getHConnection();
		try{
	       	 HTable table=(HTable)connection.getTable(Bytes.toBytes(tableName));
	       	 
	       	 final HelperTools.SumRequest req=HelperTools.SumRequest.newBuilder()
	       			 .setFilterStr(ByteString.copyFromUtf8(filterStr))
	       			 .setColumn(ByteString.copyFromUtf8(colName))
	       			 .setColumnType(ByteString.copyFromUtf8(colType))
	       			 .build();
	       	 
	       	 Map<byte[], Double> results=table.coprocessorService(HelperTools.HelperService.class, null, null, new Batch.Call<HelperService, Double>() {
	
					@Override
					public Double call(HelperService helperService) throws IOException {
						// TODO Auto-generated method stub
						ServerRpcController controller = new ServerRpcController();
						BlockingRpcCallback<HelperTools.SumResponse> rpc=new BlockingRpcCallback<HelperTools.SumResponse>();
						helperService.sumColumn(controller, req, rpc);
						HelperTools.SumResponse response=rpc.get();
						if (controller.failedOnException()) {
	                       throw controller.getFailedOn();
	                     }
	                   return ((response != null && response.hasSum()) ? response.getSum() : 0.0d);
					}
				});
	       	 
	       	 Iterator<Double> iter = results.values().iterator();
	       	 
	       	 while(iter.hasNext()){
	       		 total+=iter.next();
	       	 }
	       	 table.close();
        }catch(Exception ex){
        	ex.printStackTrace();
        }finally{
        	closeConnection(connection);
        }
		return total;
	}
	
	public static org.json.JSONObject getGroupResult(String tableName,String filterStr,org.json.JSONObject argObj) throws Throwable{
		org.json.JSONObject groupResult=new org.json.JSONObject();
		org.json.JSONObject tCountsObj=new org.json.JSONObject();
		org.json.JSONObject tSumObj=new org.json.JSONObject();
		HConnection connection=getHConnection();
		try{
	       	 HTable table=(HTable)connection.getTable(Bytes.toBytes(tableName));
	       	 
	       	 final HelperTools.GroupRequest req=HelperTools.GroupRequest.newBuilder()
	       			 .setFilterStr(ByteString.copyFromUtf8(filterStr))
	       			 .setArgs(ByteString.copyFromUtf8(argObj.toString()))
	       			 .build();
	       	 
	       	 Map<byte[], String> results=table.coprocessorService(HelperTools.HelperService.class, null, null, new Batch.Call<HelperService, String>() {
	
					@Override
					public String call(HelperService helperService) throws IOException {
						// TODO Auto-generated method stub
						ServerRpcController controller = new ServerRpcController();
						BlockingRpcCallback<HelperTools.GroupResponse> rpc=new BlockingRpcCallback<HelperTools.GroupResponse>();
						helperService.groupColumn(controller, req, rpc);
						HelperTools.GroupResponse response=rpc.get();
						if (controller.failedOnException()) {
	                       throw controller.getFailedOn();
	                     }
	                   return ((response != null && response.hasResult()) ? response.getResult().toStringUtf8() : "");
					}
				});
	       	 
	       	 Iterator<String> iter = results.values().iterator();
	       	 
	       	 while(iter.hasNext()){
	       		 String str=iter.next();
	       		 if(str.length()>0){
	       			 try{
	       				 System.out.println("\tgetGroupResult return:"+str);
	       				 org.json.JSONObject o=new org.json.JSONObject(str);
	       				 if(o.has("counts")){
	       					 org.json.JSONObject countsObj=o.getJSONObject("counts");
	       					java.util.Iterator<String> keys=countsObj.keys();
		       				 while(keys.hasNext()){
		       					 String key=keys.next();
		       					 if(tCountsObj.has(key)){
		       						long val=tCountsObj.getLong(key);
		       						tCountsObj.put(key, val+countsObj.getLong(key));
		       					 }else{
		       						tCountsObj.put(key, countsObj.getLong(key));
		       					 }
		       				 }
	       				 }
	       				 
	       				 if(o.has("sum")){
	       					org.json.JSONObject sumObj=o.getJSONObject("sum");
	       					java.util.Iterator<String> keys=sumObj.keys();
		       				 while(keys.hasNext()){
		       					 String key=keys.next();
		       					 if(tSumObj.has(key)){
		       						tSumObj.put(key, tSumObj.getDouble(key)+sumObj.getDouble(key));
		       					 }else{
		       						tSumObj.put(key, sumObj.getDouble(key)); 
		       					 }
		       				 }
	       				 }
	       				 
	       				 
	       			 }catch(Exception ex){
	       				 ex.printStackTrace();
	       			 }
	       		 }
	       	 }
	       	 table.close();
	       	 
	       	 groupResult.put("sum",tSumObj);
	       	 groupResult.put("counts", tCountsObj);
        }catch(Exception ex){
        	ex.printStackTrace();
        }finally{
        	closeConnection(connection);
        }
		return groupResult;
	}
}
