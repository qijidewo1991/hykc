package com.xframe.security;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.hyb.utils.Configuration;
import com.xframe.utils.HBaseUtils;

public class HBaseAuthMethod implements IAuthMethod {
	private String className;
	private String debug = "1";

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getDebug() {
		return debug;
	}

	public void setDebug(String debug) {
		this.debug = debug;
	}

	@Override
	public AuthedUser authenticate(String userName, String password, String authCode, String answer) throws Exception {
		// TODO Auto-generated method stub
		AuthedUser user = new AuthedUser();
		// System.out.println("debug="+debug+",className="+this.className);
		if (debug.equals("1"))
			System.out.println("authcode=[" + authCode + "],answer=[" + answer + "]");
		if (authCode != null && answer != null) {
			if (!authCode.equals(answer)) {
				user.setMessage("验证码不正确!");
				return user;
			}
		}

		org.apache.hadoop.hbase.client.Connection connection = HBaseUtils.getConnection();
		Table table = null;
		try {
			table = connection.getTable(TableName.valueOf("syscfg"));
			Get get = new Get(Bytes.toBytes("DLXX-" + userName));
			Result result = table.get(get);
			if (!result.isEmpty()) {

				String uid = new String(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("userid")));
				String uname = new String(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("username")));
				String urole = new String(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("gw")));
				String utype = new String(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("usertype")));
				String upwd = new String(result.getValue(Bytes.toBytes("data"), Bytes.toBytes("userpwd")));
				byte[] deptid = result.getValue(Bytes.toBytes("data"), Bytes.toBytes("deptid"));
				String depttype = "-";
				String deptname = "-";
				String parent_dept = "-";
				String sxfbl = "-";
				String permissions = "";
				// 获取当前登录人机构信息
				Get get2 = new Get(Bytes.toBytes("JGXX-" + new String(deptid)));
				Result result2 = table.get(get2);
				if (!result2.isEmpty()) {
					if (result2.containsColumn(Bytes.toBytes("data"), Bytes.toBytes("parent_dept"))) {// 机构所属公司
																										// 分公司zgs...或者hykc总公司
						parent_dept = new String(result2.getValue(Bytes.toBytes("data"), Bytes.toBytes("parent_dept")));
					}
					if (result2.containsColumn(Bytes.toBytes("data"), Bytes.toBytes("sxfbl"))) {// 手续费比例
						sxfbl = new String(result2.getValue(Bytes.toBytes("data"), Bytes.toBytes("sxfbl")));
						if (!(sxfbl.trim().length() > 0)) {
							sxfbl = "" + Configuration.CHARGE_PERCENT;
						}
					} else {
						sxfbl = "" + Configuration.CHARGE_PERCENT;
					}
					depttype = new String(result2.getValue(Bytes.toBytes("data"), Bytes.toBytes("jglx")));
					deptname = new String(result2.getValue(Bytes.toBytes("data"), Bytes.toBytes("jgmc")));
					if (result2.containsColumn(Bytes.toBytes("data"), Bytes.toBytes("roles"))) {
						String strRoles = new String(result2.getValue(Bytes.toBytes("data"), Bytes.toBytes("roles")));
						// System.out.println("--------------------------------strRoles="+strRoles);
						// System.out.println("--------------------------------urole="+urole);
						try {
							org.json.JSONObject json = new org.json.JSONObject(strRoles);
							permissions = json.has(urole) ? json.getJSONObject(urole).getString("selBox") : "";
							// System.out.println("--------------------------------permissions="+permissions);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}

				String pwd = org.mbc.util.MD5Tools.createMessageDigest(password);
				if (upwd != null && upwd.equals(pwd)) {
					user.setName(uid);
					user.setUserName(uname);
					user.addRole(urole);
					user.setUserType(utype);
					user.setDeptType(depttype);
					user.setPermissions(permissions);
					user.setDeptName(deptname);
					user.setParentDept(parent_dept);
					user.setSxfbl(sxfbl);// 手续费比例
					user.setCreatedTime(org.mbc.util.Tools.formatDate("yyyy/MM/dd HH:mm:ss-S", new java.util.Date()));
					if (deptid != null) {
						user.setDeptid(new String(deptid));
					}

					// 设置当前用户的环境参数信息
					/*
					 * org.json.JSONObject atts=user.getAttributes();
					 * if(sysconf==null) sysconf=AppHelper.getSystemConf();
					 * 
					 * atts.put("server",sysconf.containsKey("mq_server_remote")?
					 * sysconf.get("mq_server_remote"):"122.114.76.38");
					 * atts.put("port","1883"); atts.put("user","system");
					 * atts.put("password","manager");
					 */
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (table != null)
				table.close();
			HBaseUtils.closeConnection2(connection);
		}

		return user;
	}

}
