package com.xframe.data;

import com.xframe.utils.CacheService;
import com.xframe.utils.XHelper;
import org.json.*;

public class UserAppConfig {
	private String khh;
	private String khmc;
	private String gzh;
	private org.json.JSONObject accountInfo;
	private org.json.JSONObject userDef;
	private org.json.JSONObject	appInfo;
	private org.json.JSONArray menus;
	private java.util.HashMap<String, org.json.JSONObject> objectsMap=new java.util.HashMap<String, JSONObject>();
	private java.util.HashMap<String, org.json.JSONObject> usersMap=new java.util.HashMap<String, JSONObject>();
	private java.util.HashMap<String, String> varsMap=new java.util.HashMap<String, String>();
	
	private UserAppConfig(){
		
	}
	
	public String getKhh() {
		return khh;
	}
	public String getKhmc() {
		return khmc;
	}
	public String getGzh() {
		return gzh;
	}
	public org.json.JSONObject getAccountInfo() {
		return accountInfo;
	}
	public org.json.JSONObject getUserDef() {
		return userDef;
	}
	public org.json.JSONObject getAppInfo() {
		return appInfo;
	}
	
	public org.json.JSONArray getMenus() {
		return menus;
	}
	public java.util.HashMap<String, org.json.JSONObject> getObjects() {
		return objectsMap;
	}
	public java.util.HashMap<String, org.json.JSONObject> getUsers() {
		return usersMap;
	}
	
	public static UserAppConfig getConfigByGzh(String gzh){
		UserAppConfig config=null;
		if(CacheService.getInstance().containsKey(gzh)){
			config=(UserAppConfig)CacheService.getInstance().getFromCache(gzh);
		}
		
		if(config!=null)
			return config;
		
		org.json.JSONArray cols=new org.json.JSONArray();
		try{
			org.json.JSONObject param=new org.json.JSONObject();
			param.put("name","data:gzh");
			param.put("value", gzh);
			cols.put(param);
			
			org.json.JSONArray list=XHelper.listRowsByFilter("syscfg", "khxx", cols);
			if(list.length()>0){
				JSONObject row=list.getJSONObject(0);
				return getConfig(row.getString("data:khbh"));
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public static UserAppConfig getConfig(String khh){
		UserAppConfig config=null;
		if(!CacheService.getInstance().containsKey(khh)){
			config=loadConfig(khh);
		}else
			config=(UserAppConfig)CacheService.getInstance().getFromCache(khh);
		
		
		return config;
	}
	
	public static void reloadAllUsers(String khh){
		UserAppConfig config=getConfig(khh);
		if(config!=null){
			try {
				config.reloadUsers();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void realodAllObjects(String khh){
		UserAppConfig config=getConfig(khh);
		if(config!=null){
			try {
				config.reloadObjects();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static UserAppConfig loadConfig(String khh){
		UserAppConfig config=null;
		if(CacheService.getInstance().containsKey(khh)){
			CacheService.getInstance().removeEntry(khh);
		}
		
		try{
			config=new UserAppConfig();
			org.json.JSONObject khxxObject=XHelper.loadTableRow("khxx"+khh,"syscfg");
			if(khxxObject!=null){
				config.khh=khxxObject.getString("entity:id");
				config.khmc=khxxObject.getString("data:khmc");
				config.gzh=khxxObject.getString("data:gzh");
				String strAccount=khxxObject.has("data:account")?khxxObject.getString("data:account"):null;
				if(strAccount!=null){
					config.accountInfo=new JSONObject(strAccount);
				}else{
					config.accountInfo=new JSONObject();
				}
				
				String strUserDef=khxxObject.has("data:def")?khxxObject.getString("data:def"):null;
				if(strUserDef!=null){
					config.userDef=new JSONObject(strUserDef);
				}else{
					config.userDef=new JSONObject();
				}
				
				String strAppInfo=khxxObject.has("data:appInfo")?khxxObject.getString("data:appInfo"):null;
				if(strAppInfo!=null){
					config.appInfo=new org.json.JSONObject(strAppInfo);
				}else{
					config.appInfo=new org.json.JSONObject();
				}
				
				
				
				
				String strMenus=khxxObject.has("data:menus")?khxxObject.getString("data:menus"):null;
				//System.out.println("khh="+config.getKhh()+",khmc="+config.getKhmc()+",strMenus="+strMenus);
				config.menus=strMenus==null?new JSONArray():new JSONArray(strMenus);
				
				config.reloadUsers();
				config.reloadObjects();
				
				//以客户号为ID保存
				CacheService.getInstance().putInCache(khh, config);
				//以公众号为ID保存
				CacheService.getInstance().putInCache(khxxObject.getString("data:gzh"), config);
			}
		}catch(Exception ex){
			config=null;
			ex.printStackTrace();
		}
		
		
		return config;
	}
	
	
	private void reloadUsers() throws Exception{
		this.usersMap.clear();
		org.json.JSONArray users=XHelper.listUsers(this.khh);
		for(int i=0;i<users.length();i++){
			org.json.JSONObject u=users.getJSONObject(i);
			this.usersMap.put(u.getString("data:YHID"), u);
		}
	}
	
	private void reloadObjects() throws Exception{
		this.objectsMap.clear();
		org.json.JSONArray objects=XHelper.listObjects(this.khh);
		for(int i=0;i<objects.length();i++){
			JSONObject o=objects.getJSONObject(i);
			this.objectsMap.put(o.getString("rowid"), o);
		}
	}
	
	public void putVariable(String key,String value){
		this.varsMap.put(key, value);
	}
	
	public String getVariable(String key){
		return varsMap.containsKey(key)?varsMap.get(key):"";
	}
}
