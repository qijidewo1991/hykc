package com.xframe.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hyb.utils.Configuration;
import com.hyb.utils.WebUser;
import com.xframe.core.MqttMessageProcessor;

public class AppHelper {
	public static String SYSCONF="";
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(AppHelper.class);
	private static java.util.HashMap<String, String> conf=null;
	public static String getItemUrl(String khh,org.json.JSONObject btnDef){
		String strUrl="#";
		try{
			 //log.info("========="+btnDef);
			 String oper=btnDef.getString("oper");
			 String obj=btnDef.getString("object"); 
			 String label=btnDef.getString("label");
			 String menus=btnDef.getString("menus");
			 String styles=btnDef.getString("styles");
			 if(oper.equals("menu.jsp")){
				 strUrl=""+oper+"?khh="+khh+"&menus="+menus+"&m="+java.net.URLEncoder.encode(label, "utf-8")+"&styles="+java.net.URLEncoder.encode(styles, "utf-8")+"";
			 }else{
				 strUrl=""+oper+"?khh="+khh+"&object="+obj+"&m="+java.net.URLEncoder.encode(label, "utf-8");
			 }
			 log.info("uRL="+strUrl);
		}catch(Exception ex){
			log.info("getItemUrl error:"+ex.toString());
		}
		return strUrl;
	}
	
	public static org.json.JSONArray loadUserTempData(org.json.JSONObject params,javax.servlet.http.HttpServletRequest request){
		org.json.JSONArray list=new org.json.JSONArray();
		try{
			@SuppressWarnings("unchecked")
			java.util.HashMap<String, org.json.JSONObject> ctxMap=(java.util.HashMap<String, org.json.JSONObject>)request.getSession().getAttribute(XHelper.CONTEXT_PARAMS);
			if(ctxMap!=null){
				String object=params.getString("object");
				String key=""+XHelper.OPER_LIST_SUB+"_"+object+"_temp";
				if(ctxMap.containsKey(key)){
					org.json.JSONObject cache=ctxMap.get(key);
					if(cache.has("items")){
						org.json.JSONArray items=cache.getJSONArray("items");
						for(int i=0;i<items.length();i++){
							org.json.JSONObject row=items.getJSONObject(i);
							row.put("rowid", row.getString("cur_rowid"));
							list.put(row);
						}
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return list;
	}
	
	public static org.json.JSONArray loadUserData(String filterStr,javax.servlet.http.HttpServletRequest request){
		org.json.JSONArray list=new org.json.JSONArray();
		HConnection connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			org.json.JSONObject params=new org.json.JSONObject(filterStr);
			if(params.has("temp")){
				return loadUserTempData(params,request);
			}
			
			String khh=params.getString("khh");
			String object=params.getString("object");
			org.json.JSONArray columns=params.getJSONArray("columns");
			org.json.JSONArray filters=params.getJSONArray("filters");
			org.json.JSONArray children=params.getJSONArray("children");
			String parent_row=params.has("parent_row")?params.getString("parent_row"):null;
			String last_row=params.has("last_row")?params.getString("last_row"):null;
			
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.add("data:khbh","=",khh);
			flistBuilder.add("data:object","=",object);
			for(int i=0;i<filters.length();i++){
				org.json.JSONObject item=filters.getJSONObject(i);
				String name=item.getString("name");
				String oper=item.getString("colOper");
				String val=item.getString("colValue");
				flistBuilder.add("data:"+name,oper,val);
			}
			
			if(parent_row!=null)
				flistBuilder.add("data:parent_rowid","=",parent_row);
			
			if(params.has("search")){
				try{
					org.json.JSONObject sp=params.getJSONObject("search");
					flistBuilder.add("data:"+sp.getString("field"),"=",sp.getString("value"),FilterListBuilder.SubstrCompare);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
			
			PageFilter pageFilter=new PageFilter(10);
			FilterList flist=flistBuilder.getFilterList();
			flist.addFilter(pageFilter);
			
			userTable=connection.getTable("userdata"); 
			Scan scan=new Scan();
			
			if(last_row!=null){
				byte[] lastRow=Bytes.toBytes(last_row);
				byte[] startRow=Bytes.add(lastRow,FilterListBuilder.POSTFIX);
			    scan.setStartRow(startRow);
			}
			
			scan.setFilter(flist);
			ResultScanner scanner = userTable.getScanner(scan);
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
				
				String t_rowid=new String(scannerResult.getRow(),"utf-8");
				org.json.JSONObject row=new org.json.JSONObject();
				row.put("rowid", t_rowid);
				for(int i=0;i<columns.length();i++){
					String colName=columns.getString(i);
					byte[] rawData=scannerResult.getValue(Bytes.toBytes("data"),Bytes.toBytes(colName));
					String val=rawData!=null?new String(rawData,"utf-8"):"-";
					row.put(colName, val);
					
					//获取对应的子对象数量
					for(int j=0;j<children.length();j++){
						org.json.JSONObject t=children.getJSONObject(j);
						//String child=children.getString(j);
						FilterListBuilder listBuilder=new FilterListBuilder();
						listBuilder.addRowFilter(FilterListBuilder.SubstrCompare, "=", t.getString("id"));
						listBuilder.add("data:khbh","=",khh);
						listBuilder.add("data:object","=",t.getString("id"));
						listBuilder.add("data:parent_rowid","=",t_rowid);
						long total=0;
						try {
							total=HBaseUtils.getRowCount("userdata",listBuilder.getFilterStr());
						} catch (Throwable e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						row.put(t.getString("id"), total);
					}
					
				}
				list.put(row);
			}
			
		}catch(Exception ex){
			log.error("loadUserData:"+ex.toString());
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
		return list;
	}
	
	public static void updateOnlineUser(String clientId,String strTopics){
		log.info("用户上线:"+clientId);
		if(clientId.indexOf("@")<0)
			return;
		String deptid=clientId.substring(0,clientId.indexOf("@"));
		String userId=clientId.substring(clientId.indexOf("@")+1);
		java.util.ArrayList<WebUser> userList=Configuration.getDeptUsers(deptid);
		for(WebUser user:userList){
			if(user.getUserId().equals(userId)){
				user.setOnline(true);
				break;
			}
		}
		
		String[] topics=strTopics.split(",");
		if(MqttMessageProcessor.topics.contains(clientId))
			MqttMessageProcessor.topics.remove(clientId);
		
		java.util.HashSet<String> topicSet=new java.util.HashSet<String>();
		for(String t:topics){
			topicSet.add(t);
		}
		MqttMessageProcessor.topics.put(clientId, topicSet);
	}
	
	public static void removeOnlineUser(String clientId){
		log.info("用户下线:"+clientId);
		if(clientId.indexOf("@")<0)
			return;
		if(MqttMessageProcessor.topics.contains(clientId))
			MqttMessageProcessor.topics.remove(clientId);
		String deptid=clientId.substring(0,clientId.indexOf("@"));
		String userId=clientId.substring(clientId.indexOf("@")+1);
		java.util.ArrayList<WebUser> userList=Configuration.getDeptUsers(deptid);
		for(WebUser user:userList){
			if(user.getUserId().equals(userId)){
				user.setOnline(false);
				break;
			}
		}
		/*
		HConnection connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			userTable=connection.getTable("session_data");
			Delete dele=new Delete(Bytes.toBytes("session-"+clientId));        
			userTable.delete(dele);
		}catch(Exception ex){
			log.error("loadUserData:"+ex.toString());
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
		}*/
	}
	
	public static java.util.HashMap<String, String> getSystemConf(){
		if(conf!=null)
			return conf;
		Properties pps = new Properties();
		try{
			pps.load(new java.io.InputStreamReader(new FileInputStream(SYSCONF), "utf-8"));
			//pps.load(new FileInputStream(SYSCONF));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		java.util.HashMap<String, String> hm=new java.util.HashMap<String, String>();
		java.util.Enumeration<?> enum1 =(java.util.Enumeration<?>)pps.propertyNames();//得到配置文件的名字
        while(enum1.hasMoreElements()) {
            String strKey = enum1.nextElement().toString();
            String strValue = pps.getProperty(strKey);
            hm.put(strKey, strValue);
        }
        conf=hm;
        return hm;
	}
	
	public static org.json.JSONObject syncData(javax.servlet.http.HttpServletRequest request){
		org.json.JSONObject json=new org.json.JSONObject();
		HConnection connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			int fromId=0;
			int total=0;
			int inputCount=0;
			//获取当前已更新到的ID
			
			userTable=connection.getTable("mobile_users");
			Get get=new Get(Bytes.toBytes("sync-max-id"));
			Result result=userTable.get(get);
			Put put_max=new Put(Bytes.toBytes("sync-max-id"));
			if(!result.isEmpty()){
				fromId=Bytes.toInt(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("max-id")));
			}else{
				
				put_max.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes("maxid"));
				put_max.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("sync"));
				put_max.add(Bytes.toBytes("data"),Bytes.toBytes("max-id"),Bytes.toBytes(fromId));
				userTable.put(put_max);
			}
			
			java.util.HashMap<String, String> hm=getSystemConf();
	        log.info("连接参数:"+hm);
	        //连接数据库
	        Class.forName(hm.get("className"));
	        java.sql.Connection con=null;
	        java.sql.Statement statement=null;
	        java.sql.ResultSet rs=null;
	        try{
	        	java.util.List<Put> putList=new java.util.ArrayList<Put>();
	        	java.util.ArrayList<java.util.HashMap<String, String>> exList=new java.util.ArrayList<HashMap<String,String>>();
	        	con=DriverManager.getConnection(hm.get("url"),hm.get("user"),hm.get("password"));
	        	statement=con.createStatement();
	        	rs=statement.executeQuery("select * from Vehicle where Id>"+fromId);
	        	java.sql.ResultSetMetaData meta=rs.getMetaData();
	        	java.util.HashMap<String, String> tmp=null;
	        	while(rs.next()){
	        		tmp=new java.util.HashMap<String, String>();
	        		for(int i=0;i<meta.getColumnCount();i++){
	        			String name=meta.getColumnName(i+1);
	        			String val=rs.getString(name);
	        			//System.out.println("name="+name+",value="+val);
	        			tmp.put(name, val);
	        		}
	        		
	        		try{
	        			Put put=new Put(Bytes.toBytes("USER-"+tmp.get("OwnerNo")+"-driver"));
	        			put.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes(tmp.get("OwnerNo")));
	    				put.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("driver-imp"));
	    				put.add(Bytes.toBytes("data"),Bytes.toBytes("from"),Bytes.toBytes("import"));
	    				put.add(Bytes.toBytes("data"),Bytes.toBytes("username"),Bytes.toBytes(tmp.get("OwnerName")));
	        			for(String k:tmp.keySet()){
	        				String val=tmp.get(k);
	        				if(val==null)
	        					val="-";
	        				put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(val));
	        			}
	        			putList.add(put);
	        			inputCount++;
	        		}catch(Exception ex){
	        			exList.add(tmp);
	        			ex.printStackTrace();
	        		}
	        		total++;
	        	}
	        	userTable.put(putList);
	        	json.put("input", inputCount);
	        	json.put("exceptions", exList);
	        	json.put("total", total);
	        	
	        	//增加机构注册信息
	        	putList.clear();
	        	HTableInterface table2=connection.getTable("syscfg");
	        	rs=statement.executeQuery("select * from VehGroupMain");
	        	while(rs.next()){
	        		Put put=new Put(Bytes.toBytes("JGXX-"+rs.getString("VehGroupID")));
	        		put.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes(rs.getString("VehGroupID")));
    				put.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("JGXX"));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("bh"),Bytes.toBytes(rs.getString("VehGroupID")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("jglx"),Bytes.toBytes("ysgs"));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("jgmc"),Bytes.toBytes(rs.getString("VehGroupName")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("lxdh"),Bytes.toBytes(rs.getString("sTel1")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("lxr"),Bytes.toBytes(rs.getString("Contact")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("parent_dept"),Bytes.toBytes("hykc"));
    				putList.add(put);
	        	}
	        	table2.put(putList);
	        	table2.close();
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
				HBaseUtils.closeConnection(connection);
				
	        	if(rs!=null)
	        		rs.close();
	        	if(statement!=null)
	        		statement.close();
	        	if(con!=null)
	        		con.close();
	        }
		}catch(Exception ex){
			ex.printStackTrace();
			try {
				json.put("total", -1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return json;
	}
	
	
	public static org.json.JSONObject syncDataFromFile(javax.servlet.http.HttpServletRequest request){
		org.json.JSONObject json=new org.json.JSONObject();
		HConnection connection=HBaseUtils.getHConnection();
		HTableInterface userTable=null;
		try{
			int fromId=0;
			int total=0;
			int inputCount=0;
			//获取当前已更新到的ID
			
			userTable=connection.getTable("tmp_users");
			Get get=new Get(Bytes.toBytes("sync-max-id"));
			Result result=userTable.get(get);
			Put put_max=new Put(Bytes.toBytes("sync-max-id"));
			if(!result.isEmpty()){
				fromId=Bytes.toInt(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("max-id")));
			}else{
				put_max.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes("maxid"));
				put_max.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("sync"));
				put_max.add(Bytes.toBytes("data"),Bytes.toBytes("max-id"),Bytes.toBytes(fromId));
				userTable.put(put_max);
			}
			
			String dir=request.getServletContext().getRealPath("WEB-INF/temp");
			String data1=org.mbc.util.Tools.loadTextFile(dir+"/Vehicle_0");
			String data2=org.mbc.util.Tools.loadTextFile(dir+"/VehGroupMain");
			org.json.JSONArray vehs=new org.json.JSONArray(data1);
			org.json.JSONArray groups=new org.json.JSONArray(data2);
			java.util.HashMap<String, String> hm=getSystemConf();
	        log.info("连接参数:"+hm);
	        //连接数据库
	       
	        try{
	        	java.util.List<Put> putList=new java.util.ArrayList<Put>();
	        	java.util.ArrayList<org.json.JSONObject> exList=new java.util.ArrayList<org.json.JSONObject>();
	        	
	        	for(int i=0;i<vehs.length();i++){
	        		org.json.JSONObject o=vehs.getJSONObject(i);
	        		
	        		try{
	        			Put put=new Put(Bytes.toBytes("USER-"+o.getString("OwnerNo")+"-driver"));
	        			put.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes(o.getString("OwnerNo")));
	    				put.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("driver-imp"));
	    				put.add(Bytes.toBytes("data"),Bytes.toBytes("from"),Bytes.toBytes("import"));
	    				put.add(Bytes.toBytes("data"),Bytes.toBytes("username"),Bytes.toBytes(o.getString("OwnerName")));
	        			java.util.Iterator<String> keys=o.keys();
	    				while(keys.hasNext()){
	    					String k=keys.next();
	        				String val=o.getString(k);
	        				if(val==null)
	        					val="-";
	        				put.add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes(val));
	        			}
	        			putList.add(put);
	        			inputCount++;
	        		}catch(Exception ex){
	        			exList.add(o);
	        			ex.printStackTrace();
	        		}
	        		total++;
	        	}
	        	userTable.put(putList);
	        	
	        	
	        	putList.clear();
	        	HTableInterface table2=connection.getTable("tmp_jgxx");
	        	//创建机构
	        	for(int i=0;i<groups.length();i++){
	        		org.json.JSONObject row=groups.getJSONObject(i);
	        		Put put=new Put(Bytes.toBytes("JGXX-"+row.getString("VehGroupID")));
	        		put.add(Bytes.toBytes("entity"),Bytes.toBytes("id"),Bytes.toBytes(row.getString("VehGroupID")));
    				put.add(Bytes.toBytes("entity"),Bytes.toBytes("type"),Bytes.toBytes("JGXX"));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("bh"),Bytes.toBytes(row.getString("VehGroupID")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("jglx"),Bytes.toBytes("ysgs"));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("jgmc"),Bytes.toBytes(row.getString("VehGroupName")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("lxdh"),Bytes.toBytes(row.getString("sTel1")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("lxr"),Bytes.toBytes(row.getString("Contact")));
    				put.add(Bytes.toBytes("data"),Bytes.toBytes("parent_dept"),Bytes.toBytes("hykc"));
	        	}
	        	table2.put(putList);
	        	table2.close();
	        	
	        	
	        	json.put("input", inputCount);
	        	json.put("exceptions", exList);
	        	json.put("total", total);
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
				HBaseUtils.closeConnection(connection);
	        }
		}catch(Exception ex){
			ex.printStackTrace();
			try {
				json.put("total", -1);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return json;
	}
	
	public static org.json.JSONObject createFilter(String name,String value,String oper) throws Exception{
		org.json.JSONObject item=new org.json.JSONObject();
		item.put("name",name);
		item.put("value",value);
		if(oper!=null)
			item.put("oper",oper);
		
		return item;
	}
	
	public static org.json.JSONObject loadContacts(String cid) throws Exception{
		org.json.JSONArray filters=new org.json.JSONArray();
		filters.put(createFilter("data:read","0","="));
		filters.put(createFilter("data:from",cid,"<>"));
		org.json.JSONArray rows=com.xframe.utils.XHelper.listRowsByFilter("chat_his","|"+cid,filters);
		filters=new org.json.JSONArray();
		org.json.JSONArray contacts=com.xframe.utils.XHelper.listRowsByFilter("recent_contacts",cid,filters);
		org.json.JSONObject json=new org.json.JSONObject();
		org.json.JSONObject contactsMap=new org.json.JSONObject();
		for(int i=0;i<contacts.length();i++){
			org.json.JSONObject item=contacts.getJSONObject(i);
			String rowid=item.getString("rowid");
			String uid="";
			if(rowid.endsWith("|"+cid)){
				uid=rowid.split("\\|")[0];
				org.json.JSONObject userObj=com.hyb.utils.Configuration.loadUserDataByClientId(null, uid);
				if(userObj!=null){
					if(userObj.has("data:deptid")){
						item.put("head","-");
						String jgxxId="JGXX-"+userObj.getString("data:deptid");
						org.json.JSONObject jgxxDef=(org.json.JSONObject)com.xframe.utils.CacheService.getInstance().getFromCache(jgxxId);
						//out.println("JGXX:"+jgxxDef);
						item.put("data:deptname",jgxxDef.getString("data:jgmc"));
						item.put("data:deptid",jgxxDef.getString("entity:id"));
						item.put("name",userObj.getString("data:username"));
					}else{
						item.put("head",userObj.has("data:head")?userObj.getString("data:head"):"files/default_mobile_head.png");
						item.put("name",userObj.getString("data:username"));
					}
					//out.println("user item:"+item+"<br/><br/>");
					
				}
			}else{
				uid=rowid.split("\\|")[1];
			}
			
			if(!contactsMap.has(uid)){
				contactsMap.put(uid,item);
			}else{
				org.json.JSONObject o=contactsMap.getJSONObject(uid);
				String time=item.getString("data:time");
				String time1=o.getString("data:time");
				if(time.compareTo(time1)>0){
					o.put("data:time",time);
					if(item.has("data:body")){
						o.put("data:body",item.getString("data:body"));
					}
				}
				
			}
		}
		json.put("contacts",contactsMap);
		json.put("chatlogs",rows);
		return json;
	}
	
	public static void printParameters(javax.servlet.http.HttpServletRequest req){
		
		System.out.println("URI参数:"+req.getRequestURI());
		java.util.Enumeration<String> eu=req.getParameterNames();
		while(eu.hasMoreElements()){
			String key=eu.nextElement();
			System.out.println("\tPARAM["+key+"],VALUE:"+req.getParameter(key));
		}
	}
	
	public static JSONObject changeParametersToJson(javax.servlet.http.HttpServletRequest req){
		JSONObject json=new JSONObject();
		java.util.Enumeration<String> eu=req.getParameterNames();
		while(eu.hasMoreElements()){
			String key=eu.nextElement();
			try {
				json.put(key, req.getParameter(key));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("\tPARAM["+key+"],VALUE:"+req.getParameter(key));
		}
		return json;
	}	
	
}
