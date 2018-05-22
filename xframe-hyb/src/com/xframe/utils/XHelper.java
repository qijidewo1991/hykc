package com.xframe.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.jruby.compiler.ir.operands.Array;
import org.json.JSONObject;

public class XHelper {
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(XHelper.class);
	public static final String CONTEXT_PARAMS="context_params";
	public static final String OPER_CREATE="create";
	public static final String OPER_LIST_SUB="list_sub";
	public static final String OPER_QUERY="query";
	public static final String OPER_UPDATE="update";
	public static final int SORT_ASC=0;
	public static final int SORT_DESC=1;
	private static java.util.HashMap<String, java.util.HashMap<String, String>> dictMap=new java.util.HashMap<String, HashMap<String,String>>();
	public static void applySort(java.util.ArrayList<java.util.HashMap<String, String>> srcList,String column,int sort){
		Collections.sort(srcList,new JSONComparator(column,sort));
	
	}
	
	
	
	public static java.util.HashMap<String, String> getDictCategory(String cat){
		java.util.HashMap<String, String> itemMap=null;
		if(!dictMap.containsKey(cat)){
			itemMap=new java.util.HashMap<String, String>();
			HConnection connection=HBaseUtils.getHConnection();
			try{
				HTableInterface userTable=connection.getTable("code_category");
				Get get=new Get(Bytes.toBytes(cat));
				Result result=userTable.get(get);
				if(!result.isEmpty()){
					Cell[] cells=result.rawCells();
					for(int i=0;i<cells.length;i++){
						Cell cell=cells[i];
						String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
						String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
						if(!faml.equals("code"))
							continue;
						if(qual.endsWith("desc"))
						    continue;
						String code_id=qual;
					    String code_name=new String(result.getValue(Bytes.toBytes("code"),Bytes.toBytes(qual)),"utf-8");
					    itemMap.put(code_id,code_name);
					}
				}

				userTable.close();
			}catch(Exception ex){
				ex.printStackTrace();
			}finally{
				HBaseUtils.closeConnection(connection);
			}
			dictMap.put(cat, itemMap);
		}else
			itemMap=dictMap.get(cat);
		return itemMap;
	}
	
	public static String getDictValue(String cat,String name){
		java.util.HashMap<String, String> itemMap=getDictCategory(cat);
		return itemMap.containsKey(name)?itemMap.get(name):"-";
	}
	
	public static JSONObject toJsonObject(Object o){
		JSONObject json=new JSONObject();
		@SuppressWarnings("rawtypes")
		Class cls=o.getClass();
		Field[] fields=cls.getDeclaredFields();
		org.springframework.beans.BeanWrapper wraper=new org.springframework.beans.BeanWrapperImpl(o);
		for(Field f:fields){
			try{
				json.put(f.getName(), wraper.getPropertyValue(f.getName()));
			}catch(Exception ex){
				//ex.printStackTrace();
			}
		}
		return json;
	}
	
	public static org.json.JSONObject loadTableRow(String rowid,String table){
		org.json.JSONObject def=null;
		HConnection connection=HBaseUtils.getHConnection();
		try{
			HTableInterface userTable=connection.getTable(table); 
			Get get=new Get(Bytes.toBytes(rowid));
			Result result=userTable.get(get);
			if(!result.isEmpty()){
				def=new org.json.JSONObject();
				def.put("rowid",rowid);
				Cell[] cells=result.rawCells();
				for(Cell cell:cells){
					String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
			        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
			        String val=new String(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        def.put(faml+":"+qual, val);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			HBaseUtils.closeConnection(connection);
		}
		return def;
	}
	
	public static org.json.JSONObject loadRow(String rowid,String table){
		return loadRow(rowid,table,false);
	}
	
	public static org.json.JSONObject loadRow(String rowid,String table,boolean hasFamily){
		return loadRow(null,rowid,table,hasFamily);
	}
	
	/*
	public static org.json.JSONObject loadRow(String rowid,String table,boolean hasFamily){
		org.json.JSONObject def=null;
		HConnection connection=HBaseUtils.getHConnection();
		try{
			HTableInterface userTable=connection.getTable(table); 
			Get get=new Get(Bytes.toBytes(rowid));
			Result result=userTable.get(get);
			if(!result.isEmpty()){
				def=new org.json.JSONObject();
				def.put("rowid",rowid);
				Cell[] cells=result.rawCells();
				for(Cell cell:cells){
					String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
			        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
			        String val=new String(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        def.put(hasFamily?(faml+":"+qual):qual, val);
				}
				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			HBaseUtils.closeConnection(connection);
		}
		return def;
	}*/
	
	public static org.json.JSONObject loadRow(HConnection con,String rowid,String table,boolean hasFamily){
		return loadRow(con,rowid,table,hasFamily,null);
		
	}
	
	public static org.json.JSONObject loadRow(HConnection con,String rowid,String table,boolean hasFamily,org.json.JSONObject argDesc){
		org.json.JSONObject def=null;
		HConnection connection=con;
		if(con==null)
			connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null; 
		try{
			
			userTable=connection.getTable(table); 
			Get get=new Get(Bytes.toBytes(rowid));
			Result result=userTable.get(get);
			//log.info("LOADING ROW:"+table+",row="+rowid+",result="+result+",empty=="+result.isEmpty());
			if(!result.isEmpty()){
				def=new org.json.JSONObject();
				def.put("rowid",rowid);
				Cell[] cells=result.rawCells();
				for(Cell cell:cells){
					String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
			        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
			        if(qual.equals("rowid"))
			        	continue;
			        if(argDesc==null || !argDesc.has(faml+":"+qual)){
			        	String val=new String(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        	def.put(hasFamily?(faml+":"+qual):qual, val);
			        }else{
			        	if(argDesc.has(faml+":"+qual)){
			        		String type=argDesc.getString(faml+":"+qual);
			        		if(type.equals(HBaseUtils.TYPE_NUMBER)){
			        			double val=Bytes.toDouble(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)));
			        			def.put(hasFamily?(faml+":"+qual):qual, val);
			        		}else if(type.equals(HBaseUtils.TYPE_INT)){
			        			double val=Bytes.toInt(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)));
			        			def.put(hasFamily?(faml+":"+qual):qual, val);
			        		}else{
			        			String val=new String(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
					        	def.put(hasFamily?(faml+":"+qual):qual, val);
			        		}
			        	}
			        }
				}
				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(userTable!=null){
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(con==null)
				HBaseUtils.closeConnection(connection);
		}
		return def;
	}
	
	public static org.json.JSONObject loadObjectDef(String rowid){
		return loadObjectDef(rowid,null);
	}
	
	public static org.json.JSONObject loadObjectDef(String rowid,String table){
		org.json.JSONObject def=null;
		HConnection connection=HBaseUtils.getHConnection();
		try{
			HTableInterface userTable=connection.getTable(table==null?"syscfg":table); 
			Get get=new Get(Bytes.toBytes(rowid));
			Result result=userTable.get(get);
			if(!result.isEmpty()){
				def=new org.json.JSONObject();
				def.put("rowid",rowid);
				Cell[] cells=result.rawCells();
				for(Cell cell:cells){
					String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
			        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
			        String val=new String(result.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        def.put(faml+":"+qual, val);
				}
			}

			org.json.JSONArray items=getAllChildObject(rowid,connection);
			def.put("children", items);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			HBaseUtils.closeConnection(connection);
		}
		return def;
	}
	
	private static org.json.JSONArray getAllChildObject(String parentRowid,org.apache.hadoop.hbase.client.HConnection connection){
		org.json.JSONArray items=new org.json.JSONArray();
		try{
			HTable table=(HTable)connection.getTable(Bytes.toBytes("syscfg"));
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.add("data:parent","=",parentRowid);
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","objects");
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = table.getScanner(scan);
			for (Result scannerResult:scanner) {
				String rowid=new String(scannerResult.getRow(),"utf-8");
				
				if(rowid!=null){
					org.json.JSONObject item=new org.json.JSONObject();
					item.put("rowid", rowid);
					Cell[] cells=scannerResult.rawCells();
					for(Cell cell:cells){
						String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
				        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
				        String val=new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
				        item.put(faml+":"+qual, val);
					}
					items.put(item);
				}
			}
		}catch(Exception ex){
			log.error("getAllChildObject异常:"+ex.toString());
		}finally{
		}
		return items;
	}
	
	
	public static org.json.JSONObject loadUserDef(String khh){
		org.json.JSONObject userDef=null;
		HConnection connection=HBaseUtils.getHConnection();
		try{
			HTableInterface userTable=connection.getTable("syscfg"); 
			Get get=new Get(Bytes.toBytes("khxx"+khh));
			Result result=userTable.get(get);
			if(!result.isEmpty()){
				String def=new String(result.getValue(Bytes.toBytes("data"),Bytes.toBytes("def")),"utf-8");
				userDef=new org.json.JSONObject(def);
			}
		}catch(Exception ex){
			log.error("异常:"+ex.toString());
		}finally{
			HBaseUtils.closeConnection(connection);
		}	
		return userDef;
	}
	
	public static org.json.JSONArray listUsers(String khh){
		org.json.JSONArray list=new org.json.JSONArray();
		org.apache.hadoop.hbase.client.HConnection connection=HBaseUtils.getHConnection();
		HTable table=null;
		try{
			table=(HTable)connection.getTable(Bytes.toBytes("userdata"));
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.add("data:khbh","=",khh);
			flistBuilder.add("data:object","=","YHXX_X");
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","YHXX_X");
			try {
				long total=HBaseUtils.getRowCount("userdata",flistBuilder.getFilterStr());
				log.info("\t记录数:"+total+",Filter:"+flistBuilder.getFilterList());
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = table.getScanner(scan);
			for (Result scannerResult:scanner) {
				String rowid=new String(scannerResult.getRow(),"utf-8");
				if(rowid!=null){
					org.json.JSONObject userInfo=new org.json.JSONObject();
					userInfo.put("rowid", rowid);
					Cell[] cells=scannerResult.rawCells();
					for(Cell cell:cells){
						String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
				        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
				        String val=new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
				        userInfo.put(faml+":"+qual, val);
				        
					}
					list.put(userInfo);
				}
			}
			
		}catch(Exception ex){
			log.error("getAccountInfo异常:"+ex.toString());
		}finally{
			if(table!=null){
				try {
					table.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			HBaseUtils.closeConnection(connection);
		}
		return list;
	}
	
	public static org.json.JSONArray listObjects(String khh){
		org.json.JSONArray list=new org.json.JSONArray();
		org.apache.hadoop.hbase.client.HConnection connection=HBaseUtils.getHConnection();
		HTable table=null;
		try{
			table=(HTable)connection.getTable(Bytes.toBytes("syscfg"));
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.add("data:khbh","=",khh);
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","objects");
			try {
				long total=HBaseUtils.getRowCount("userdata",flistBuilder.getFilterStr());
				log.info("\t记录数:"+total+",Filter:"+flistBuilder.getFilterList());
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = table.getScanner(scan);
			for (Result scannerResult:scanner) {
				String rowid=new String(scannerResult.getRow(),"utf-8");
				if(rowid!=null){
					org.json.JSONObject userInfo=new org.json.JSONObject();
					userInfo.put("rowid", rowid);
					Cell[] cells=scannerResult.rawCells();
					for(Cell cell:cells){
						String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
				        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
				        String val=new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
				        userInfo.put(faml+":"+qual, val);
				        
					}
					
					org.json.JSONArray items=getAllChildObject(rowid,connection);
					userInfo.put("children", items);
					list.put(userInfo);
				}
			}
			
		}catch(Exception ex){
			log.error("getAccountInfo异常:"+ex.toString());
		}finally{
			if(table!=null){
				try {
					table.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			HBaseUtils.closeConnection(connection);
		}
		return list;
	}
	
	public static org.json.JSONArray listRowsByFilter(String tableName,String rowLike,org.json.JSONArray cols){
		return listRowsByFilter(tableName,rowLike,cols,true);
	}
	
	public static org.json.JSONArray listRowsByFilter(String tableName,String rowLike,org.json.JSONArray cols,boolean hasFamily){
		return listRowsByFilter(tableName,rowLike,cols,hasFamily,null);
	}
	
	public static org.json.JSONArray listRowsByFilter(String tableName,String rowLike,org.json.JSONArray cols,boolean hasFamily,org.json.JSONObject argDesc){
		org.json.JSONArray list=null;
		org.apache.hadoop.hbase.client.HConnection connection=HBaseUtils.getHConnection();
		HTable table=null;
		PageFilter pageFilter=null;
		String lastRow=null;
		try{
			table=(HTable)connection.getTable(Bytes.toBytes(tableName));
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			for(int i=0;i<cols.length();i++){
				org.json.JSONObject o=cols.getJSONObject(i);
				//log.info("addFilter:"+o);
				String name=o.getString("name");
				if(name.startsWith("##")){
					if(name.equals(FilterListBuilder.FLAG_REC_COUNT)){
						pageFilter=new PageFilter(Integer.parseInt(o.getString("value")));
					}else if(name.equals(FilterListBuilder.FLAG_LAST_ROW)){
						lastRow=o.getString("value");
					}
					continue;
				}
				
				String value=o.getString("value");
				if(name.indexOf(":")<0)
					name="data:"+name;
				if(o.has("oper")){
					if(o.getString("oper").equals("includes") || o.getString("oper").equals("excludes")){
	                    if(o.getString("oper").equals("includes"))
	                    		flistBuilder.add(name,"=",o.getString("val"),FilterListBuilder.SubstrCompare);
	                    else
	                        	flistBuilder.add(name,"<>",o.getString("val"),FilterListBuilder.SubstrCompare);
					}else      
						flistBuilder.add(name,o.getString("oper"),value);
				}else
					flistBuilder.add(name,"=",value);
			}
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=",rowLike);
			long total=0;
			try {
				total=HBaseUtils.getRowCount(tableName,flistBuilder.getFilterStr());
				log.info("\t记录数:"+total+",Filter:"+flistBuilder.getFilterList());
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			FilterList flist=flistBuilder.getFilterList();
			if(pageFilter!=null){
				flist.addFilter(pageFilter);
			}
			if(lastRow!=null){
				byte[] bytsLastRow=Bytes.toBytes(lastRow);
				bytsLastRow=Bytes.add(bytsLastRow,FilterListBuilder.POSTFIX);
			    scan.setStartRow(bytsLastRow);
			}
			
			scan.setFilter(flist);
			
			//ＳＣＡＮ　ｔａｂｌｅ　ｂｅｇｉｎ
			for(int i=0;i<3;i++){
				try{
					list=scanTable(table,scan,flistBuilder,total,argDesc,hasFamily);
					break;
				}catch(Exception ex){
					log.info("SCAN TABLE("+i+")异常:"+ex.toString());
					Thread.sleep(300);
				}
			}
			
		}catch(Exception ex){
			log.error("listRowsByFilter异常:"+ex.toString());
			ex.printStackTrace();
		}finally{
			if(table!=null){
				try {
					table.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			HBaseUtils.closeConnection(connection);
		}
		return list;
	}
	
	
	private static org.json.JSONArray scanTable(HTable table,Scan scan,FilterListBuilder flistBuilder,
			long total,org.json.JSONObject argDesc,
			boolean hasFamily) throws Exception{
		org.json.JSONArray list=new org.json.JSONArray();
		ResultScanner scanner = table.getScanner(scan);
		for (Result scannerResult:scanner) {
			boolean missingColumn=false;
			for(int i=0;i<flistBuilder.getColumns().size();i++){
				FilterColumn fcol=flistBuilder.getColumns().get(i);
				if(!scannerResult.containsColumn(Bytes.toBytes(fcol.getFamily()), Bytes.toBytes(fcol.getQualifier()))){
					missingColumn=true;
					break;
				}
			}
			if(missingColumn)
				continue;
			
			String rowid=new String(scannerResult.getRow(),"utf-8");
			if(rowid!=null){
				org.json.JSONObject userInfo=new org.json.JSONObject();
				userInfo.put("total",total);
				Cell[] cells=scannerResult.rawCells();
				for(Cell cell:cells){
					String qual=new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength(),"utf-8");
			        String faml=new String(cell.getFamilyArray(),cell.getFamilyOffset(),cell.getFamilyLength(),"utf-8");
			        if(qual.equals("rowid"))
			        	continue;
			        
			        if(argDesc==null || !argDesc.has(faml+":"+qual)){
			        	String val=new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        	userInfo.put(hasFamily?(faml+":"+qual):qual, val);
			        }else{
			        	if(argDesc.has(faml+":"+qual)){
			        		String type=argDesc.getString(faml+":"+qual);
			        		if(type.equals(HBaseUtils.TYPE_NUMBER)){
			        			double val=Bytes.toDouble(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)));
			        			userInfo.put(hasFamily?(faml+":"+qual):qual, val);
			        		}else if(type.equals(HBaseUtils.TYPE_INT)){
			        			double val=Bytes.toInt(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)));
			        			userInfo.put(hasFamily?(faml+":"+qual):qual, val);
			        		}else{
			        			String val=new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)),"utf-8");
			        			userInfo.put(hasFamily?(faml+":"+qual):qual, val);
			        		}
			        	}
			        }
			        
				}
				userInfo.put("rowid", rowid);
				list.put(userInfo);
			}
		}
		
		return list;
	}
	
	public static org.json.JSONArray loadDatasFromObject(String khh,String object,org.json.JSONArray filters){
		org.json.JSONArray rows=listRowsByFilter("userdata",object+khh,filters);
		return rows;
	}
	
	public static void createRow(String tableName,String rowid,org.json.JSONObject data){
		createRow(null,tableName,rowid,data);
	}
	
	public static boolean createRow(HConnection con,String tableName,String rowid,org.json.JSONObject data){
		log.info("CREATE ROW["+tableName+"]=>"+data);
		HConnection connection=con;
		if(con==null)
			connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			userTable=connection.getTable(tableName);
			Put put=new Put(Bytes.toBytes(rowid));
			//log.info("["+tableName+"]put row value==="+data);
			@SuppressWarnings("unchecked")
			java.util.Iterator<String> keys=data.keys();
			while(keys.hasNext()){
				String k=keys.next();
				if(data.get(k) instanceof org.json.JSONObject){
					org.json.JSONObject col=data.getJSONObject(k);
					try{
						String type=col.getString("type");
						String val=col.getString("value");
						if(type.equals(HBaseUtils.TYPE_NUMBER)){
							put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(Double.parseDouble(val)));
						}else if(type.equals(HBaseUtils.TYPE_INT)){
							put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(Integer.parseInt(val)));
						}else
							put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(type+":"+val));
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}else
					put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(data.getString(k)));
			}
            userTable.put(put);
            
		}catch(Exception ex){
			ex.printStackTrace();
			return false;
		}finally{
			if(userTable!=null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(con==null)
				HBaseUtils.closeConnection(connection);
		}
		return true;
	}
	
	public static void deleteRows(String tablename, java.util.List<String> rowid) {
		try {
			org.apache.hadoop.hbase.client.HConnection connection = com.xframe.utils.HBaseUtils.getHConnection();
			org.apache.hadoop.hbase.client.HTable table = (org.apache.hadoop.hbase.client.HTable) connection.getTable(tablename);
			java.util.List<Delete> ap = new java.util.ArrayList<org.apache.hadoop.hbase.client.Delete>();
			for (int x = 0; x < rowid.size(); x++) {
				org.apache.hadoop.hbase.client.Delete delete = new org.apache.hadoop.hbase.client.Delete(Bytes.toBytes(rowid.get(x)));
				ap.add(delete);
			}
			table.delete(ap);
			table.close();
			com.xframe.utils.HBaseUtils.closeConnection(connection);
		} catch (java.io.IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public static org.json.JSONObject getSumTemp(org.json.JSONArray sumCols,String tableName,String rowLike,org.json.JSONArray fcols) throws Exception{
		org.json.JSONObject ret=new org.json.JSONObject();
		for(int i=0;i<sumCols.length();i++){
			ret.put(sumCols.getString(i), 0.0);
		}
		
		org.json.JSONArray cols=new org.json.JSONArray();
		for(int i=0;i<fcols.length();i++){
			org.json.JSONObject o=fcols.getJSONObject(i);
			o.put("name",o.getString("field"));
			o.put("oper", o.getString("oper"));
			o.put("value", o.getString("val"));
			cols.put(o);
		}
		
		org.json.JSONArray items=listRowsByFilter(tableName,rowLike,cols);
		if(items.length()>0){
			int rowSize=30;
			org.json.JSONObject row=items.getJSONObject(items.length()-1);
			int total=row.getInt("total");
			int times=total%rowSize==0?total/rowSize:(total/rowSize)+1;
			
			String lastRow="";
			for(int i=0;i<times;i++){
				
				boolean rowCountFound=false;
				boolean rowLastFound=false;
				for(int k=0;k<cols.length();k++){
					org.json.JSONObject f=cols.getJSONObject(k);
					if(cols.getJSONObject(k).getString("name").equals(FilterListBuilder.FLAG_REC_COUNT)){
						cols.getJSONObject(k).put("value", rowSize);
						rowCountFound=true;
						continue;
					}else if(cols.getJSONObject(k).getString("name").equals(FilterListBuilder.FLAG_LAST_ROW)){
						rowLastFound=true;
						if(lastRow.length()>0)
							cols.getJSONObject(k).put("value", lastRow);
						continue;
					}
				}
				
				if(!rowCountFound){
					org.json.JSONObject o=new org.json.JSONObject();
					o.put("name", FilterListBuilder.FLAG_REC_COUNT);
					o.put("value", rowSize);
					cols.put(o);
				}
				
				if(!rowLastFound && lastRow.length()>0){
					org.json.JSONObject o=new org.json.JSONObject();
					o.put("name", FilterListBuilder.FLAG_LAST_ROW);
					o.put("value", lastRow);
					cols.put(o);
				}
				
				
				log.info("("+i+"/"+times+")==>统计:"+cols.toString());
				
				items=listRowsByFilter(tableName,rowLike,cols);
				lastRow=items.getJSONObject(items.length()-1).getString("rowid");
				for(int j=0;j<items.length();j++){ //依次计算
					org.json.JSONObject t=items.getJSONObject(j);
					for(int k=0;k<sumCols.length();k++){
						String name=sumCols.getString(k);
						if(t.has(name)){
							double d=t.getDouble(name);
							ret.put(name, ret.getDouble(name)+d);
						}
					}
				}
			}
		}
		log.info("统计结果:"+ret);
		return ret;
	}
}
