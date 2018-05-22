package com.hyb.utils;

import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.client.HConnection;
import org.json.JSONException;
import org.json.JSONObject;

import com.xframe.utils.AppHelper;
import com.xframe.utils.CacheService;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.XHelper;

public class Configuration {
	/**
	 * 运单状态:	9 已取消
	 * 			8 未发布
	 * 			7已派单
	 * 			6协商中
	 * 			5已完成
	 * 			4已确认
	 * 			3已送达
	 * 			2配送中
	 * 			1待装货
	 * 			0待支付
	 */
	
	public static final String[] statusNames =new String[]{"待支付",	"待装货"  ,"配送中"	,"待确认"	,"待评价"	,"已完成"	,"协商中"};
	public static final String[] actionNames1=new String[]{"支付保证金","开始配送"	,"送达客户",""		,"评价货主",""		,""};
	public static final String[] actionNames2=new String[]{"取消运单"	,"取消运单",""		,"确认送达","评价司机",""		,""};
	public static final String[] optionNames1=new String[]{"取消运单",	"取消运单"	,""		,""		,"我要投诉","我要投诉",""};
	public static final String[] optionNames2=new String[]{"",		""		,""		,""		,"我要投诉","我要投诉",""};
	
	public static final String adminAccount="hykc";
	private static final org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(Configuration.class);
	private static final String PARAMS_MAP="__params";
	private static final String WEB_USERS_MAP="__web_users_map";
	private static final String SOURCES_MAP="__sources_map";
	private static final String READY_USERS="__ready_users";
	private static final String SOURCE_CATEGORY="__source_category";
	private static final String VEH_MAPS="__veh_maps";
	private static final String RZ_REQ_MAP="__rz_req_map";
	
	public static double CHARGE_PERCENT=0.20;//手续费比例20%
	public static double DEPOSIT_PERCENT=0.2; //司机押金比例
	public static double WYJ_HUOZHU=0.2;//货主违约金比例
	public static double WYJ_DRIVER=0.2;//司机违约金比例
	public static double PCJ_HUOZHU=0.05;//货主赔偿金比例
	public static double PCJ_DRIVER=0.05;//司机赔偿金比例
	public static void loadConfiguration(){
		loadConfiguration("JGXX-hykc");
		loadWebAppUsersInCache();
		//启动后装载当日未处理的货源
		loadSourcesToCache();
		//启动后载入所有状态为ready=1的用户
		loadReadyUsers();
		
		//添加所有的车辆信息到内存中，以IP为Key
		loadVehsToCache();
		
		//Load所有未处理的实名认证请求
		loadRzRequests();
	}
	
	
	
	
	private static void loadReadyUsers(){
		try{
			org.json.JSONArray filters=new org.json.JSONArray();
			filters.put(AppHelper.createFilter("data:ready", "1", "="));
			org.json.JSONArray uesrsList=XHelper.listRowsByFilter("mobile_users","-driver",filters,true);
			
			for(int i=0;i<uesrsList.length();i++){
				org.json.JSONObject o=uesrsList.getJSONObject(i);
				String rowid=o.getString("rowid");
				String lines=o.has("data:lines")?o.getString("data:lines"):"";
				String ip=o.has("data:IpAddress")?o.getString("data:IpAddress"):"-";
				rowid=rowid.substring(5);
				setUserSubscribes(rowid, lines,ip);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void removeUserSubscribe(String cid){
		java.util.concurrent.ConcurrentHashMap<String, UserSubscribe> usersMap=getReadyUsersMap();
		if(usersMap.containsKey(cid))
			usersMap.remove(cid);
	}
	
	public static void setUserSubscribes(String cid,String lines){
		setUserSubscribes(cid,lines,null);
	}
	
	public static void setUserSubscribes(String cid,String lines,String ip){
		java.util.concurrent.ConcurrentHashMap<String, UserSubscribe> usersMap=getReadyUsersMap();
		
		UserSubscribe item=null;
		if(usersMap.containsKey(cid))
			item=usersMap.get(cid);
		else{
			item=new UserSubscribe();
			String uip=ip;
			if(uip==null){
				org.json.JSONObject o=Configuration.loadUserDataByClientId(null, cid);
				if(o!=null && o.has("data:IpAddress"))
					try {
						uip=o.getString("data:IpAddress");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			item.setIp(uip);
			usersMap.put(cid, item);
		}
		item.setRawLines(lines);
		if(lines.length()>0){
			String[] items=lines.split(""+(char)0x02);
			item.getLines().clear();
			for(int i=0;i<items.length;i++){
				String[] sects=items[i].split("\\|");
				item.getLines().add(sects[0]);
			}
		}
	}
	
	
	private static void loadRzRequests(){
		try{
			org.json.JSONArray filters=new org.json.JSONArray();
			filters.put(AppHelper.createFilter("data:zt", "0", "="));
			org.json.JSONArray uesrsList=XHelper.listRowsByFilter("rz_request","",filters,false);
			
			for(int i=0;i<uesrsList.length();i++){
				org.json.JSONObject o=uesrsList.getJSONObject(i);
				String id=o.getString("id");
				getRzRequests().put(id, o);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> getRzRequests(){
		java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> reqMap=null;
		if(CacheService.getInstance().containsKey(RZ_REQ_MAP))
			reqMap=(java.util.concurrent.ConcurrentHashMap<String,org.json.JSONObject>)CacheService.getInstance().getFromCache(RZ_REQ_MAP);
		else{
			reqMap=new java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject>();
			CacheService.getInstance().putInCache(RZ_REQ_MAP, reqMap);
		}
		return reqMap;
	}
	
	
	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> getVehsMap(){
		java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> vehsMap=null;
		if(CacheService.getInstance().containsKey(VEH_MAPS))
			vehsMap=(java.util.concurrent.ConcurrentHashMap<String,org.json.JSONObject>)CacheService.getInstance().getFromCache(VEH_MAPS);
		else{
			vehsMap=new java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject>();
			CacheService.getInstance().putInCache(VEH_MAPS, vehsMap);
		}
		return vehsMap;
	}
	
	/**
	 * @deprecated
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> getSourceCategoryMap(){
		java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> uesrsMap=null;
		if(CacheService.getInstance().containsKey(SOURCE_CATEGORY))
			uesrsMap=(java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>)CacheService.getInstance().getFromCache(SOURCE_CATEGORY);
		else{
			uesrsMap=new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>();
			CacheService.getInstance().putInCache(SOURCE_CATEGORY, uesrsMap);
		}
		return uesrsMap;
	}
	
	public static void updateSourceCategory(String sourceId,String rowid,boolean add){
		java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> catsMap=getSourceCategoryMap();
		java.util.concurrent.ConcurrentHashMap<String, String> cate=null;
		if(catsMap.containsKey(sourceId)){
			cate=catsMap.get(sourceId);
		}else{
			cate=new java.util.concurrent.ConcurrentHashMap<String, String>();
			catsMap.put(sourceId, cate);
		}
		
		if(add)
			cate.put(rowid, sourceId);
		else{
			if(cate.containsKey(rowid))
				cate.remove(rowid);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, UserSubscribe> getReadyUsersMap(){
		java.util.concurrent.ConcurrentHashMap<String, UserSubscribe> uesrsMap=null;
		if(CacheService.getInstance().containsKey(READY_USERS))
			uesrsMap=(java.util.concurrent.ConcurrentHashMap<String, UserSubscribe>)CacheService.getInstance().getFromCache(READY_USERS);
		else{
			uesrsMap=new java.util.concurrent.ConcurrentHashMap<String, UserSubscribe>();
			CacheService.getInstance().putInCache(READY_USERS, uesrsMap);
		}
		return uesrsMap;
		
	}
	
	
	/**
	 * @deprecated
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> getSourcesMap_noUse(){
		java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject> sourcesMap=null;
		if(CacheService.getInstance().containsKey(SOURCES_MAP))
			sourcesMap=(java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject>)CacheService.getInstance().getFromCache(SOURCES_MAP);
		else{
			sourcesMap=new java.util.concurrent.ConcurrentHashMap<String, org.json.JSONObject>();
			CacheService.getInstance().putInCache(SOURCES_MAP, sourcesMap);
		}
		return sourcesMap;
	}
	
	public static void putSourceToCache(String sourceId,org.json.JSONObject source){
		RedisClient.getInstance().addToHash(Configuration.SOURCES_MAP, sourceId, source.toString());
	}
	
	public static java.util.Set<String> getSourcesKeys(){
		return RedisClient.getInstance().getAllHashKeys(Configuration.SOURCES_MAP);
	}
	
	public static org.json.JSONObject getSoureFromCache(String sourceId){
		org.json.JSONObject source=null;
		try{
			String strSource=RedisClient.getInstance().getFromHash(Configuration.SOURCES_MAP, sourceId);
			if(strSource!=null){
				source=new org.json.JSONObject(strSource);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return source;
	}
	
	public static void removeSourceFromCache(String sourceId){
		RedisClient.getInstance().removeFromHash(Configuration.SOURCES_MAP, sourceId);
	}
	
	public static long getSourcesCount(){
		return RedisClient.getInstance().getHashSize(Configuration.SOURCES_MAP);
	}
	
	
	private static void loadVehsToCache(){
		loadVehsToCache(null);
	}
	
	public static void loadVehsToCache(String ipAddr){
		try{
			org.json.JSONArray filters=new org.json.JSONArray();
			if(ipAddr!=null)
				filters.put(AppHelper.createFilter("data:IpAddress", ipAddr, "="));
			org.json.JSONArray rows=XHelper.listRowsByFilter("mobile_users","driver",filters,true);
			for(int i=0;i<rows.length();i++){
				org.json.JSONObject o=rows.getJSONObject(i);
				String rowid=o.getString("rowid");
				if(o.has("data:IpAddress") && rowid.startsWith("USER-")){
					String ip=o.getString("data:IpAddress");
					String cid=rowid.substring(5);
					CacheService.getInstance().putInCache(cid, o);
					getVehsMap().put(ip, o);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private static void loadSourcesToCache(){
		try{
			org.json.JSONArray filters=new org.json.JSONArray();
			filters.put(AppHelper.createFilter("data:ydzt", "-", "="));
			org.json.JSONArray sourceList=XHelper.listRowsByFilter("yd_list","|",filters,false);
			for(int i=0;i<sourceList.length();i++){
				org.json.JSONObject o=sourceList.getJSONObject(i);
				if(!o.has("yd_status"))
					continue;
				String status=o.getString("yd_status");
				if(status.compareTo("5")<0 || status.equals("8") || status.equals("7")){
					String rowid=o.getString("rowid");
					//getSourcesMap().put(rowid, o);
					Configuration.putSourceToCache(rowid, o);
					//if(o.has("source_id") && o.has("yd_status") && !(o.getString("yd_status").compareTo("1")>0)){
					if(o.has("source_id") && o.has("yd_status") && (o.getString("yd_status").equals("0"))){
						updateSourceCategory(o.getString("source_id"),rowid,true);
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static java.util.HashMap<String, String> getParamsMap(){
		java.util.HashMap<String, String> paramsMap=null;
		if(CacheService.getInstance().containsKey(PARAMS_MAP))
			paramsMap=(java.util.HashMap<String, String>)CacheService.getInstance().getFromCache(PARAMS_MAP);
		else{
			paramsMap=new java.util.HashMap<String, String>();
			CacheService.getInstance().putInCache(PARAMS_MAP, paramsMap);
		}
		return paramsMap;
	}
	
	public static void loadConfiguration(String rowid){
		java.util.HashMap<String, String> paramsMap=getParamsMap();
		org.json.JSONObject row=com.xframe.utils.XHelper.loadRow(rowid, "syscfg", true);
		if(row!=null){
			try{
				if(row.has("data:parameters")){
					org.json.JSONObject params=new org.json.JSONObject(row.getString("data:parameters"));
					java.util.Iterator<?> keys=params.keys();
					while(keys.hasNext()){
						String k=keys.next().toString();
						org.json.JSONObject item=params.getJSONObject(k);
						String name=item.getString("pdesc");
						String value=item.getString("pvalue");
						paramsMap.put(name, value);
					}
				}
				
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		for(String k:paramsMap.keySet()){
			try{
				if(k.equals("CHARGE_PERCENT")){
					Configuration.CHARGE_PERCENT=Double.parseDouble(paramsMap.get(k));
				}else if(k.equals("DEPOSIT_PERCENT")){
					Configuration.DEPOSIT_PERCENT=Double.parseDouble(paramsMap.get(k));
				}else if(k.equals("WYJ_HUOZHU")){
					Configuration.WYJ_HUOZHU=Double.parseDouble(paramsMap.get(k));
				}else if(k.equals("WYJ_DRIVER")){
					Configuration.WYJ_DRIVER=Double.parseDouble(paramsMap.get(k));
				}else if(k.equals("PCJ_HUOZHU")){
					Configuration.PCJ_HUOZHU=Double.parseDouble(paramsMap.get(k));
				}else if(k.equals("PCJ_DRIVER")){
					Configuration.PCJ_DRIVER=Double.parseDouble(paramsMap.get(k));
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
	}
	
	public static String getParameter(String paramName){
		java.util.HashMap<String, String> paramsMap=getParamsMap();
		if(paramsMap.containsKey(paramName))
			return paramsMap.get(paramName);
		return "";
	}
	
	/**
	 * 载入所有的WEB端用户到缓存
	 */
	private static void loadWebAppUsersInCache(){
		log.info("============loadWebAppUsersInCache...");
		if(CacheService.getInstance().containsKey(WEB_USERS_MAP))
			return;
		java.util.HashMap<String, java.util.ArrayList<WebUser>> hm=new java.util.HashMap<String, java.util.ArrayList<WebUser>>();
		try{			
			java.util.ArrayList<WebUser> userList=null;
			org.json.JSONArray filters=new org.json.JSONArray();
			org.json.JSONArray depts=XHelper.listRowsByFilter("syscfg","JGXX",filters);
			for(int i=0;i<depts.length();i++){
				org.json.JSONObject row=depts.getJSONObject(i);
				CacheService.getInstance().putInCache(row.getString("rowid"), row);
			}

			
			org.json.JSONArray rows=XHelper.listRowsByFilter("syscfg","DLXX",filters);
			for(int i=0;i<rows.length();i++){
				org.json.JSONObject row=rows.getJSONObject(i);
				String userid=row.getString("data:userid");
				String username=row.getString("data:username");
				String gw=row.getString("data:gw");
				String deptid=row.getString("data:deptid");
				String userType=row.has("data:usertype")?row.getString("data:usertype"):"user";
				org.json.JSONObject jsonDept=(org.json.JSONObject)CacheService.getInstance().getFromCache("JGXX-"+deptid);
				//log.info("loading["+deptid+"]..."+jsonDept);
				if(jsonDept==null){
					log.info("loading["+deptid+"],not exists...skipped!");
					continue;
				}
				
				if(!userType.equals("user"))
					continue;
				
				String deptName=jsonDept.getString("data:jgmc");
				WebUser user=new WebUser();
				user.setUserId(userid);
				user.setUserName(username);
				user.setDeptId(deptid);
				user.setDeptName(deptName);
				user.setGw(gw);
				user.setUesrType(userType);
				user.setRowid(row.getString("rowid"));
				if(hm.containsKey(deptid)){
					userList=hm.get(deptid);
				}else{
					userList=new java.util.ArrayList<WebUser>();
					hm.put(deptid, userList);
				}
				userList.add(user);
			}
			CacheService.getInstance().putInCache(WEB_USERS_MAP, hm);
			log.info("============loadWebAppUsersInCache..."+CacheService.getInstance().getFromCache(WEB_USERS_MAP));
		}catch(Exception ex){
			log.error("异常:"+ex.toString());
			ex.printStackTrace();
		}	
	}
	
	public static java.util.ArrayList<WebUser> getDeptUsers(String deptid){
		@SuppressWarnings("unchecked")
		java.util.HashMap<String, java.util.ArrayList<WebUser>> hm=(java.util.HashMap<String, java.util.ArrayList<WebUser>>)CacheService.getInstance().getFromCache(WEB_USERS_MAP);
		if(hm!=null){
			java.util.ArrayList<WebUser> userList=hm.get(deptid);
			return userList;
		}
		
		return null;
	}
	
	public static String chooseChatUser(org.json.JSONObject userFrom,String deptid){
		String key=deptid+"@";
		try{
			if(userFrom.has(key)){
				org.json.JSONObject o=userFrom.getJSONObject(key);
				String uid=o.getString("uid");
				long last=o.getLong("last");
				if(TimeUnit.NANOSECONDS.toHours(System.nanoTime()-last)<24){ //超过24小时自动选择
					return uid;
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		@SuppressWarnings("unchecked")
		java.util.HashMap<String, java.util.ArrayList<WebUser>> hm=(java.util.HashMap<String, java.util.ArrayList<WebUser>>)CacheService.getInstance().getFromCache(WEB_USERS_MAP);
		if(hm!=null){
			java.util.ArrayList<WebUser> userList=hm.get(deptid);
			if(userList.size()==0)
				return null;
			
			java.util.ArrayList<WebUser> tempList=new java.util.ArrayList<WebUser>();
			for(WebUser user:userList){
				if(user.isOnline())
					tempList.add(user);
			}
			
			log.info("ONLINE USER COUNT="+tempList.size()+",users="+tempList);
			
			WebUser wUser=null;
			java.util.Random random=new java.util.Random();
			if(tempList.size()>0){
				int idx=random.nextInt(tempList.size());
				wUser=tempList.get(idx);
			}else{
				int idx=random.nextInt(userList.size());
				wUser=userList.get(idx);
			}
			tempList.clear();
			tempList=null;
			try{
				org.json.JSONObject o=new org.json.JSONObject();
				o.put("uid",wUser.getUserId());
				o.put("last", System.nanoTime());
				userFrom.put(key, o);
			}catch(Exception ex){
				ex.printStackTrace();
			}
			return wUser.getUserId();
		}
		return null;
	}
	
	public static void removeDeptFromCache(String rowid){
		CacheService.getInstance().removeEntry(rowid);
	}
	
	public static void addDeptToCache(String rowid){
		org.json.JSONObject deptJson=XHelper.loadRow( rowid, "syscfg", true);
		CacheService.getInstance().putInCache(rowid, deptJson);
	}
	
	public static void removeWebUserFromCache(String deptid,String rowid){
		@SuppressWarnings("unchecked")
		java.util.HashMap<String, java.util.ArrayList<WebUser>> hm=(java.util.HashMap<String, java.util.ArrayList<WebUser>>)CacheService.getInstance().getFromCache(WEB_USERS_MAP);
		if(hm!=null){
			java.util.ArrayList<WebUser> userList=hm.get(deptid);
			if(userList!=null){
				for(int i=0;i<userList.size();i++){
					if(userList.get(i).getRowid().equals(rowid)){
						userList.remove(i);
						break;
					}
				}
			}
		}
	}
	
	public static void addWebUserToCache(String deptid,String rowid){
		@SuppressWarnings("unchecked")
		java.util.HashMap<String, java.util.ArrayList<WebUser>> hm=(java.util.HashMap<String, java.util.ArrayList<WebUser>>)CacheService.getInstance().getFromCache(WEB_USERS_MAP);
		if(hm!=null){
			java.util.ArrayList<WebUser> userList=hm.get(deptid);
			if(userList!=null){
				try{
					//log.info("Add User toCache::"+deptid+",rowid="+rowid);
					org.json.JSONObject row=XHelper.loadRow( rowid, "syscfg", true);
					String userid=row.getString("data:userid");
					String username=row.getString("data:username");
					String gw=row.getString("data:gw");
					org.json.JSONObject jsonDept=(org.json.JSONObject)CacheService.getInstance().getFromCache("JGXX-"+deptid);
					String deptName=jsonDept.getString("data:jgmc");
					WebUser user=new WebUser();
					user.setUserId(userid);
					user.setUserName(username);
					user.setDeptId(deptid);
					user.setDeptName(deptName);
					user.setGw(gw);
					user.setRowid(row.getString("rowid"));
					userList.add(user);
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
	}
	
	
	public static org.json.JSONObject loadUserDataByClientId(HConnection hconn,String clientId){
		if(CacheService.getInstance().containsKey(clientId)){
			return (org.json.JSONObject)CacheService.getInstance().getFromCache(clientId);
		}
		
		HConnection connection=hconn;
		if(hconn==null)
			connection=HBaseUtils.getHConnection();
		org.json.JSONObject json=null;
		try{
			String rowid="";
			String table="";
			if(clientId.endsWith("-driver") || clientId.endsWith("-huozhu")){
				rowid="USER-"+clientId;
				table="mobile_users";
			}else {
				rowid="DLXX-"+clientId.replace('@', '-');
				table="syscfg";
			}
			json=XHelper.loadRow(connection, rowid, table, true);
			if(json!=null){
				if(json.getString("entity:type").startsWith("DLXX")){
					String deptid=json.getString("data:deptid");
					org.json.JSONObject deptJson=null;
					String deptRowId="JGXX-"+deptid;
					deptJson=(org.json.JSONObject)CacheService.getInstance().getFromCache(deptRowId);
					json.put("data:deptname", deptJson.getString("data:jgmc"));
				}
				CacheService.getInstance().putInCache(clientId, json);
				
			}
		}catch(Exception ex){
			System.out.println("ex=="+ex.toString()+",ARGS[clientid]="+clientId);
			ex.printStackTrace();
		}finally{
			if(hconn==null)
				HBaseUtils.closeConnection(connection);
		}
		//log.info("["+clientId+"]JSON="+json);
		return json;
	}
	
}
