package com.xframe.security;

import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;

import com.xframe.utils.XHelper;

public class AuthedUser implements java.io.Serializable {
	private static final long serialVersionUID = -4299126237345430889L;
	private String name;
	private String userName;
	private String message;
	private String uid;
	private String userType;
	private String createdTime;
	private String deptid;
	private String deptType;
	private String permissions;
	private String deptName;
	private String Sxfbl;
	private String parent_dept;
	private String theme = "bootstrap-cerulean.min.css";
	// private org.json.JSONObject attributes=new org.json.JSONObject();

	private java.util.ArrayList<String> roles = new java.util.ArrayList<String>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.uid = org.mbc.util.MD5Tools.createMessageDigest(name);
	}

	public String[] getRoles() {
		return roles.toArray(new String[roles.size()]);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void addRole(String name) {
		roles.add(name);
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getDeptid() {
		return deptid;
	}

	public void setDeptid(String deptid) {
		this.deptid = deptid;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		org.json.JSONObject o = XHelper.toJsonObject(this);
		return o.toString();
	}

	public String getDeptType() {
		return deptType;
	}

	public void setDeptType(String deptType) {
		if (deptType != null && deptType.equals("admin")) {
			theme = "bootstrap-cerulean.min.css";
		} else if (deptType != null && deptType.equals("ZGS")) {
			theme = "bootstrap-cerulean.min.css";
		} else if (deptType != null && deptType.equals("wlgs")) {
			theme = "bootstrap-united.min.css";
		}

		this.deptType = deptType;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}

	public void setParentDept(String parent_dept) {
		this.parent_dept = parent_dept;
	}

	public String getParentDept() {
		return parent_dept;
	}

	public void setSxfbl(String Sxfbl) {
		this.Sxfbl = Sxfbl;
	}

	public String getSxfbl() {
		return Sxfbl;
	}
	
	public String getTheme() {
		return theme;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	public JSONObject toJsonObject() {
		JSONObject json = new JSONObject();
		@SuppressWarnings("rawtypes")
		Class cls = this.getClass();
		Field[] fields = cls.getDeclaredFields();
		org.springframework.beans.BeanWrapper wraper = new org.springframework.beans.BeanWrapperImpl(this);
		for (Field f : fields) {
			try {
				json.put(f.getName(), wraper.getPropertyValue(f.getName()));
			} catch (Exception ex) {
				// ex.printStackTrace();
			}

		}

		try {
			json.put("roles", roles);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return json;
	}

	@SuppressWarnings("unchecked")
	public static AuthedUser fromJson(org.json.JSONObject o) {
		AuthedUser au = new AuthedUser();
		org.springframework.beans.BeanWrapper wraper = new org.springframework.beans.BeanWrapperImpl(au);
		try {
			java.util.Iterator<String> keys = o.keys();
			while (keys.hasNext()) {
				String k = keys.next();
				String val = o.getString(k);
				try {
					if (k.equals("roles")) {
						org.json.JSONArray jRoles = o.getJSONArray(k);
						for (int i = 0; i < jRoles.length(); i++) {
							String role = jRoles.getString(i);
							au.addRole(role);
						}
					} else {
						wraper.setPropertyValue(k, val);
					}
				} catch (Exception ex) {
					System.out.println("AuthedUser fromJson error:" + ex.getMessage());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return au;
	}

}
