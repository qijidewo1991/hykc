package com.hyb.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.JSONException;
import org.json.JSONObject;

import com.hyb.utils.bx.BXHelper;
import com.xframe.core.MqttManagerV3;
import com.xframe.utils.AppHelper;
import com.xframe.utils.CacheService;
import com.xframe.utils.FilterColumn;
import com.xframe.utils.FilterListBuilder;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.Tools;
import com.xframe.utils.XHelper;

public class AppUtil {
	public static final String SYS_MODULES = "_sys_modules";
	private static java.text.NumberFormat numberFormat;
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AppUtil.class);

	public static org.json.JSONArray getUserMenus(com.xframe.security.AuthedUser user) {
		String type = user.getDeptType();
		String permissions = user.getPermissions();
		String[] perms = permissions.split(",");
		java.util.HashSet<String> permsSet = new java.util.HashSet<String>();
//		System.out.println("!!!!!!!!!!!deptid=" + user.getDeptid());
//		System.out.println("!!!!!!!!!!!type=" + type.toString());
		for (int j = 0; j < perms.length; j++) {
			//System.out.println("!!!!!!!!!!!perms" + j + "=" + perms[j]);
		}
		if (!user.getUserType().equals("default")) {
			if (perms.length > 0) {
				for (String k : perms) {
					permsSet.add(k);
				}
			} else {
				permsSet.add("-");
			}
		}

		org.json.JSONArray list = loadModulesList(type);
//		System.out.println("!!!!!!!!!!!list=" + list.toString());
//		System.out.println("!!!!!!!!!!!permsSet=" + permsSet.toString());
		org.json.JSONArray menus = new org.json.JSONArray();

		try {
			// 合并模块列表为父-子的树形结构
			java.util.HashMap<String, org.json.JSONObject> tmp = new java.util.HashMap<String, org.json.JSONObject>();
			for (int i = list.length() - 1; i >= 0; i--) {
				org.json.JSONObject o = list.getJSONObject(i);
				//System.out.println("!!!!!!!!!!!o=" + o.toString());
				if (o.getString("data:parentid").equals("-")) {
					tmp.put(o.getString("rowid"), o);
					list.remove(i);
				} else { // 当前项为功能模块，根据权限进行过滤
					if (permsSet.size() > 0 && !permsSet.contains(o.getString("data:id"))) {
						list.remove(i);
					}
				}
			}
			//System.out.println("---------------1-----------------" + tmp);
			for (int i = list.length() - 1; i >= 0; i--) {
				org.json.JSONObject o = list.getJSONObject(i);
				String pid = o.getString("data:parentid");
				if (tmp.containsKey(pid)) {
					org.json.JSONObject oParent = tmp.get(pid);
					if (!oParent.has("children"))
						oParent.put("children", new org.json.JSONArray());
					org.json.JSONArray children = oParent.getJSONArray("children");
					children.put(o);
					list.remove(i);
				}
			}

			java.util.ArrayList<org.json.JSONObject> tempList = new java.util.ArrayList<org.json.JSONObject>();
			for (String k : tmp.keySet()) {
				org.json.JSONObject mItem = tmp.get(k);
				// 根据用户岗位权限过滤功能列表
				if (!mItem.has("children")) {
					if (permsSet.size() > 0 && !permsSet.contains(mItem.getString("data:id")))
						continue;
				} else {
					org.json.JSONArray children = mItem.getJSONArray("children");
					if (children.length() == 0)
						continue;
				}
				tempList.add(mItem);
			}

			// 排序功能组
			AppUtil.sortJsonList("data:order", false, tempList);

			// 排序所有功能项
			for (org.json.JSONObject item : tempList) {
				if (item.has("children")) {
					org.json.JSONArray children = item.getJSONArray("children");
					java.util.ArrayList<org.json.JSONObject> childrenList = new java.util.ArrayList<org.json.JSONObject>();
					for (int i = 0; i < children.length(); i++) {
						childrenList.add(children.getJSONObject(i));
					}
					AppUtil.sortJsonList("data:order", false, childrenList);
					item.put("children", childrenList);
				}
				menus.put(item);
			}

			//log.info("==>getUserMenus:" + user.toJsonObject().toString() + "\nmenus:" + menus.toString());
		} catch (Exception ex) {
			log.error("loadUserData:" + ex.toString());
			ex.printStackTrace();
		} finally {

		}

		return menus;
	}

	private static org.json.JSONArray loadModulesList(String deptType) {
		String type = deptType;
		if (type.equals("admin"))
			type = "huoyunbao-platform";
		else if (type.equals("ysgs")) {
			type = "huoyunbao";
		} else if (type.equals("wlgs")) {
			type = "huoyunbao-wlgs";
		} else if (type.equals("ZGS")) {
			System.out.println("机构类型是子公司");
			type = "huoyunbao-ZGS";
		} else {
			log.warn("无效的机构类型:" + type);
		}
		org.json.JSONArray list = new org.json.JSONArray();
		if (RedisClient.getInstance().exists(SYS_MODULES)) {
			String strModules = RedisClient.getInstance().getStr(SYS_MODULES);
			try {
				org.json.JSONArray modules = new org.json.JSONArray(strModules);
				for (int i = 0; i < modules.length(); i++) {
					org.json.JSONObject mObj = modules.getJSONObject(i);
					//System.out.println("**********" + mObj.toString());
					if (mObj.has("data:khbh") && mObj.getString("data:khbh").equals(type)) {
					//	System.out.println("符合");
						list.put(mObj);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return list;
		}

		Connection connection = HBaseUtils.getHConnection();
		Table userTable = null;
		try {

			FilterListBuilder flistBuilder = new FilterListBuilder();
			// flistBuilder.add("data:khbh","=",type);
			flistBuilder.add("entity:type", "=", "module");
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare, "=", "module");

			userTable = connection.getTable(TableName.valueOf("syscfg"));
			Scan scan = new Scan();
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = userTable.getScanner(scan);
			for (Result scannerResult : scanner) {
				boolean missingColumn = false;
				for (int i = 0; i < flistBuilder.getColumns().size(); i++) {
					FilterColumn fcol = flistBuilder.getColumns().get(i);
					if (!scannerResult.containsColumn(Bytes.toBytes(fcol.getFamily()), Bytes.toBytes(fcol.getQualifier()))) {
						missingColumn = true;
						break;
					}
				}
				if (missingColumn)
					continue;

				String t_rowid = new String(scannerResult.getRow(), "utf-8");
				org.json.JSONObject row = new org.json.JSONObject();
				row.put("rowid", t_rowid);
				Cell[] cells = scannerResult.rawCells();
				for (Cell cell : cells) {
					String qual = new String(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength(), "utf-8");
					String faml = new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength(), "utf-8");
					String val = new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)), "utf-8");
					row.put(faml + ":" + qual, val);
				}
				if (!row.has("data:order"))
					row.put("data:order", "0");
				list.put(row);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection2(connection);
		}

		RedisClient.getInstance().setStr(SYS_MODULES, list.toString());

		return list;
	}

	@SuppressWarnings("unchecked")
	public static java.util.concurrent.ConcurrentHashMap<String, VehPosition> getCachedPoints() {
		java.util.concurrent.ConcurrentHashMap<String, VehPosition> pointsMap = null;
		String cacheId = "__points";
		if (CacheService.getInstance().containsKey(cacheId)) {
			pointsMap = (java.util.concurrent.ConcurrentHashMap<String, VehPosition>) CacheService.getInstance().getFromCache(cacheId);
		} else {
			pointsMap = new java.util.concurrent.ConcurrentHashMap<String, VehPosition>();
			CacheService.getInstance().putInCache(cacheId, pointsMap);
		}
		return pointsMap;
	}

	public static void sortJsonList(final String attName, final boolean isAsc, java.util.ArrayList<org.json.JSONObject> list) {
		Comparator<org.json.JSONObject> comparator = new Comparator<org.json.JSONObject>() {
			public int compare(org.json.JSONObject s1, org.json.JSONObject s2) {
				if (!s1.has(attName) || !s2.has(attName))
					return 0;
				try {
					String att1 = s1.getString(attName);
					String att2 = s2.getString(attName);
					return isAsc ? att1.compareTo(att2) : att2.compareTo(att1);
				} catch (Exception ex) {
					return 0;
				}
			}
		};

		Collections.sort(list, comparator);
	}

	public static void sortHashMapList(final String attName, final boolean isAsc, java.util.ArrayList<java.util.HashMap<String, ?>> list) {
		Comparator<java.util.HashMap<String, ?>> comparator = new Comparator<java.util.HashMap<String, ?>>() {
			public int compare(java.util.HashMap<String, ?> s1, java.util.HashMap<String, ?> s2) {
				if (!s1.containsKey(attName) || !s2.containsKey(attName))
					return 0;
				try {
					String att1 = s1.get(attName).toString();
					String att2 = s2.get(attName).toString();
					return isAsc ? att1.compareTo(att2) : att2.compareTo(att1);
				} catch (Exception ex) {
					return 0;
				}
			}
		};
		Collections.sort(list, comparator);
	}

	public static boolean saveRzRequest(String xm, String sfzh, String mobile, String app, String cph, String cx, String cc, String zz, String pp, String nf,
			String dlysz, String cplx, String clfl, String fileDir) {
		Connection connection = HBaseUtils.getConnection();
		Table userTable = null;
		try {

			java.util.Date date = new java.util.Date();
			String strDate = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", date);
			userTable = connection.getTable(TableName.valueOf("rz_request"));

			// 删除当前用户已经存在的认证申请记录
			org.json.JSONArray cols = new org.json.JSONArray();
			org.json.JSONArray results = XHelper.listRowsByFilter("rz_request", mobile + "-" + app + "-" + sfzh, cols);

			if (results.length() > 0) {
				System.out.println("RESULTS:" + results);
				java.util.ArrayList<Delete> delList = new java.util.ArrayList<Delete>();
				for (int i = 0; i < results.length(); i++) {
					org.json.JSONObject row = results.getJSONObject(i);
					String rid = row.getString("rowid");
					Delete delete = new Delete(Bytes.toBytes(rid));
					delList.add(delete);
				}
				userTable.delete(delList);
			}

			// /添加到缓存，用以通知操作人员及时处理
			org.json.JSONObject rzObj = new org.json.JSONObject();
			rzObj.put("xm", xm);
			rzObj.put("sfzh", sfzh);
			rzObj.put("mobile", mobile);
			rzObj.put("app", app);
			Configuration.getRzRequests().put(mobile + "-" + app + "-" + sfzh, rzObj);
			AppUtil.sendBroadcast("add", rzObj.toString());
			// 添加到缓存end

			String rowid = org.mbc.util.Tools.formatDate("yyyyMMddHHmmss", date) + "_" + mobile + "-" + app + "-" + sfzh;
			Put put = new Put(Bytes.toBytes(rowid));
			put.addColumn(Bytes.toBytes("entity"), Bytes.toBytes("id"), Bytes.toBytes(mobile + "-" + app + "-" + sfzh));
			put.addColumn(Bytes.toBytes("entity"), Bytes.toBytes("type"), Bytes.toBytes("REQUEST"));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("xm"), Bytes.toBytes(xm));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("sfzh"), Bytes.toBytes(sfzh));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("mobile"), Bytes.toBytes(mobile));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("app"), Bytes.toBytes(app));

			if (app.equals("driver")) {
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("cph"), Bytes.toBytes(cph));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("cx"), Bytes.toBytes(cx));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("cc"), Bytes.toBytes(cc));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("zz"), Bytes.toBytes(zz));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("pp"), Bytes.toBytes(pp));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("nf"), Bytes.toBytes(nf));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("dlysz"), Bytes.toBytes(dlysz));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("cplx"), Bytes.toBytes(cplx));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("clfl"), Bytes.toBytes(clfl));
			} else {
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("deptid"), Bytes.toBytes(cph));
				put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("deptname"), Bytes.toBytes(cx));
			}

			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("req_time"), Bytes.toBytes(strDate));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("file_dir"), Bytes.toBytes(fileDir));

			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("zt"), Bytes.toBytes("0"));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("spr"), Bytes.toBytes("-"));
			put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("spsj"), Bytes.toBytes("-"));
			userTable.put(put);
		} catch (Exception ex) {
			log.error("saveRzRequest:" + ex.toString());
			ex.printStackTrace();
			return false;
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection2(connection);
		}
		return true;
	}

	public static String saveSource(String cid, String strDetail) {
		Connection connection = HBaseUtils.getHConnection();
		Table userTable = null;
		String rowid = null;
		org.json.JSONObject json = null;
		try {
			// log.info("SaveSource:"+strDetail);
			java.util.Date date = new java.util.Date();
			json = new org.json.JSONObject(strDetail);
			// String sid=System.nanoTime()+"-"+new
			// java.util.Random().nextInt(10);
//			String sid = org.mbc.util.Tools.formatDate("yyMMdd-HHmmssS", date) + "-" + new java.util.Random().nextInt(10);
			String sid=com.hyb.utils.GetUUID.uuid();
			//if (json.has("sid")) {//2018年4月24日  发现监测平台有重复单据，所以在此全改为uuid
				sid = json.getString("sid");
//			} else {
//				json.put("sid", sid);
//			}
			String fromProv = json.getString("from_prov");
			String fromCity = json.getString("from_city");
			String fromCounty = json.getString("from_county");
			String createTime = json.getString("create_time");
			String toProv = json.getString("to_prov");
			String toCity = json.getString("to_city");
			String toCounty = json.getString("to_county");
			String vehType = json.getString("req_type");
			String vehLength = json.getString("req_length");
			String jgxxid = json.getString("jgxxid");
			if (jgxxid.indexOf("JGXX") == 0) {
				JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(jgxxid, "syscfg");
				jgxxid = json_cid_info.getString("parent_dept");
			} else {
				jgxxid = Configuration.adminAccount;
			}
			json.put("parent_dept", jgxxid);
			json.remove("jgxxid");
			String sourceId = fromProv.substring(0, 2) + fromCity.substring(0, 2) + "-" + toProv.substring(0, 2) + toCity.substring(0, 2);
			String strDate = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", date);
			userTable = connection.getTable(TableName.valueOf("yd_list"));
			// rowid=cid+"|"+fromCity+" "+fromCounty+"-"+toCity+" "+toCounty+"|"+vehLength+"|"+vehType+"|"+createTime;
			rowid = createTime + "|" + cid + "|" + fromCity + " " + fromCounty + "-" + toCity + " " + toCounty + "|" + vehLength + "|" + vehType;

			if (!json.has("fbsj"))
				json.put("fbsj", strDate);
			json.put("fbzt", "-");
			json.put("ydzt", "-");
			json.put("fbr", cid);
			json.put("bf", "0"); // 保费默认0

			Put put = new Put(Bytes.toBytes(rowid));
			put.add(Bytes.toBytes("entity"), Bytes.toBytes("id"), Bytes.toBytes(cid));
			put.add(Bytes.toBytes("entity"), Bytes.toBytes("type"), Bytes.toBytes("SOURCE"));

			json.put("source_id", sourceId);
			java.util.Iterator<?> keys = json.keys();
			while (keys.hasNext()) {
				String name = keys.next().toString();
				String val = json.getString(name);
				put.add(Bytes.toBytes("data"), Bytes.toBytes(name), Bytes.toBytes(val));
			}
			/*
			 * put.add(Bytes.toBytes("data"),Bytes.toBytes("fbzt"),Bytes.toBytes(
			 * "-"));
			 * put.add(Bytes.toBytes("data"),Bytes.toBytes("ydzt"),Bytes.toBytes
			 * ("-"));
			 * put.add(Bytes.toBytes("data"),Bytes.toBytes("fbsj"),Bytes.
			 * toBytes(strDate));
			 * put.add(Bytes.toBytes("data"),Bytes.toBytes("fbr"
			 * ),Bytes.toBytes(cid));
			 */

			userTable.put(put);

			Configuration.putSourceToCache(rowid, json);
			// 更新货源分类
			if (json.has("yd_status") && json.getString("yd_status").equals("0")) { // 公开发布的货源通知订阅人及分类计数
				Configuration.updateSourceCategory(sourceId, rowid, true);

				sendSourceChanged(sourceId, strDate, rowid);
			} else if (json.has("yd_status") && json.getString("yd_status").equals("1")) { // 派单给司机
				String destCid = json.getString("tmp_cid");
				String strHeader = "SOURCE" + (char) 0x02 + MqttManagerV3.TOPIC_PREFIX + cid + (char) 0x02 + MqttManagerV3.TOPIC_PREFIX + destCid + (char) 0x02;
				String strContent = getSourceDetail(cid, rowid, json);
				String strBody = strHeader + cid + (char) 0x03 + "05" + (char) 0x03 + strContent;
				MqttManagerV3.getInstance().sendWithThread(strBody, MqttManagerV3.TOPIC_LOCAL);
			}

			log.info("\tSOURCE SAVED!" + System.currentTimeMillis());

			// 手机发布的货源生成保单信息
			/*
			 * if(cid.indexOf("@")<0){ org.json.JSONObject
			 * row=XHelper.loadRow(rowid, "yd_list"); row.put("rowid", rowid);
			 * processBxxx(row); }
			 */
		} catch (Exception ex) {
			log.error("saveSource异常:" + ex.toString());
			ex.printStackTrace();
			return null;
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection2(connection);
		}
		return rowid;
	}

	public static org.json.JSONObject createBxxx(org.json.JSONObject json) throws Exception {
		org.json.JSONObject result = new org.json.JSONObject();
		try {
			result.put("success", "false");

			// 计算保费
			double hwjz = json.has("hwjz") ? json.getDouble("hwjz") : 0.0;
			if (hwjz > 0) {
				double bf = 0.0;
				double fl = 0.0;
				if (hwjz > 30 * 10000) {
					fl = 0.003;
					bf = hwjz * fl;
				} else {
					fl = 0.005;
					bf = hwjz * fl;
				}

				String fbr = json.getString("fbr");
				org.json.JSONObject userObj = Configuration.loadUserDataByClientId(null, fbr);
				java.util.HashMap<String, String> conf = AppHelper.getSystemConf();
				String token = conf.get("token");
				String coop = conf.get("cooperation");
				String api = conf.get("openapi_url");
				String tradeNo = BXHelper.getTradeNo();
				String invoceNo = tradeNo;
				boolean isIndividual = fbr.indexOf("@") < 0;

				// 如果是物流公司操作人员，获取物流公司信息
				if (!isIndividual) {
					org.json.JSONObject jgxx = XHelper.loadRow("JGXX-" + fbr.substring(0, fbr.indexOf("@")), "syscfg");
					userObj.put("data:addr", jgxx.getString("addr"));
					userObj.put("data:lxdh", jgxx.getString("lxdh"));
				}

				String strHwlb = "16一般货物";
				if (json.has("hwlb") && json.getString("hwlb").trim().length() > 0) {
					strHwlb = json.getString("hwlb");
				}
				
				// System.out.println("[用户信息]======"+userObj);
				String qyrq = org.mbc.util.Tools.formatDate("yyyy-MM-dd'T'HH:mm:ss.SSS", new java.util.Date());
				String zjlx = isIndividual ? "01" : "3"; // 身份证号或营业执照号码
				String zjhm = isIndividual ? userObj.getString("data:rz#sfzh") : (json.has("yyzzhm") ? json.getString("yyzzhm") : json.getString("deptid"));
				org.json.JSONObject jsonParams = BXHelper.packRequest(token, coop, tradeNo, json.getString("from_prov") + " " + json.getString("from_city"),
						invoceNo, json.getString("sid"), json.getString("to_prov") + " " + json.getString("to_city"), "" + hwjz, json.getString("hwmc"),
						strHwlb.substring(0, 2), "是", json.getString("hwzl") + "吨", "1.00",// AppUtil.formatAmountNoGroup(bf),
						"" + fl, qyrq, zjlx,// 证件类型
						zjhm, // 证件号码
						isIndividual ? userObj.getString("data:rz#mobile") : userObj.getString("data:addr"), // 货主地址
						isIndividual ? userObj.getString("data:rz#xm") : json.getString("deptname"), // 货主名称
						isIndividual ? "1" : "2", // 货主类型
						isIndividual ? userObj.getString("data:rz#mobile") : userObj.getString("data:lxdh"), // 货主电话
						conf, json,json.getString("parent_dept"));
				System.out.println("\t保险信息:" + jsonParams);

				String strReturn = Tools.post(api, jsonParams.toString());
				String strTmp = java.net.URLDecoder.decode(strReturn, "utf-8");
				System.out.println("\t返回原始信息:" + strTmp);
				org.json.JSONObject jsonRet = new org.json.JSONObject(strTmp);
				// System.out.println("\t保险处理返回:"+jsonRet);

				if (jsonRet.has("resultDTO")) {
					org.json.JSONObject resultDTO = jsonRet.getJSONObject("resultDTO");
					if (resultDTO.getString("resultCode").equals("1")) {// 投保成功
						org.json.JSONObject cargoPartProposalObj = jsonRet.getJSONObject("cargoPartProposalObj");
						result.put("success", "true");
						result.put("cargoPartProposalObj", cargoPartProposalObj);
						String rowid = json.getString("rowid");
						cargoPartProposalObj.put("zjlx", zjlx);
						cargoPartProposalObj.put("zjhm", zjhm);
						cargoPartProposalObj.put("resultCode", "1");
						XHelper.createRow("yd_list", rowid, cargoPartProposalObj);

						// 更新缓存中运单的保单编号
						org.json.JSONObject jsonSource = Configuration.getSoureFromCache(rowid);// Configuration.getSourcesMap().get(rowid);
						log.info("缓存运单信息rowid=" + rowid + ",jsonSource=" + jsonSource);
						if (jsonSource != null) {
							jsonSource.put("policyNo", cargoPartProposalObj.getString("policyNo"));
							Configuration.putSourceToCache(rowid, jsonSource);
							System.out.println("已更新缓存运单:" + rowid);
						}
					} else {
						String rowid = json.getString("rowid");
						XHelper.createRow("yd_list", rowid, resultDTO);
						result.put("message", resultDTO.getString("resultMess"));

					}
				} else {
					result.put("message", "无效的返回结果:" + strReturn);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			result.put("message", ex.toString());
		}

		return result;
	}

	public static String getSourceDetail(String cid, String rowid, org.json.JSONObject o) {
		// java.util.concurrent.ConcurrentHashMap<String,
		// java.util.concurrent.ConcurrentHashMap<String, String>>
		// cats=Configuration.getSourceCategoryMap();
		org.json.JSONObject json = o != null ? o : Configuration.getSoureFromCache(rowid);
		String strContent = "";
		try {
			strContent = rowid + "$" + json.getString("from_prov") + "|" + json.getString("to_prov") + "|" + json.getString("sid");
			strContent += "|" + json.getString("hwmc");
			strContent += "|" + json.getString("hwtj");
			strContent += "|" + json.getString("hwzl");
			strContent += "|" + json.getString("yqsj");
			if (cid.indexOf("@") > 0) {
				strContent += "|" + (json.has("deptname") ? json.getString("deptname") : json.getString("deptid"));
				strContent += "|files/default_app_head.png";
			} else {
				org.json.JSONObject userObj = Configuration.loadUserDataByClientId(null, cid);
				if (userObj != null) {
					strContent += "|" + userObj.getString("data:username");
					if (userObj.has("data:head"))
						strContent += "|" + userObj.getString("data:head");
					else
						strContent += "|files/default_mobile_head.png";
				} else {
					System.out.println("\t!!!无效的:CID==" + cid);
					strContent += "|" + cid;
					strContent += "|files/default_mobile_head.png";
				}
			}

			strContent += "|" + json.getString("yf");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return strContent;
	}

	public static void updateSubscribes(String cid, String lines, String ready) {
		HConnection connection = HBaseUtils.getHConnection();
		HTableInterface userTable = null;
		try {
			userTable = connection.getTable("mobile_users");
			org.json.JSONObject userObj = Configuration.loadUserDataByClientId(connection, cid);
			log.info("==>CID=" + cid + ",name=" + userObj);
			String rowid = userObj.getString("rowid");
			Put put = new Put(Bytes.toBytes(rowid));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("ready"), Bytes.toBytes(ready));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("lines"), Bytes.toBytes(lines));
			userTable.put(put);

			// 更新缓存
			userObj.put("data:ready", ready);
			userObj.put("data:lines", lines);
			if (ready.equals("1")) {
				Configuration.setUserSubscribes(cid, lines);
			} else {
				Configuration.removeUserSubscribe(cid);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection(connection);
		}
	}

	public static void sendSourceChanged(String sourceId, String strTime, String rowid) {
		String strContent = sourceId + ("" + (char) 0x04) + Configuration.getSourceCategoryMap().get(sourceId).size() + (char) 0x04 + rowid;
		String strBody = "-" + (char) 0x03 + "04" + (char) 0x03 + strContent;
		// 根据用户订阅发布货源通知信息
		/*
		 * java.util.concurrent.ConcurrentHashMap<String, UserSubscribe>
		 * readyUsers=Configuration.getReadyUsersMap(); for(String
		 * k:readyUsers.keySet()){ String
		 * strHeader="-"+(char)0x02+"SOURCE"+(char)0x02+strDate+(char)0x02;
		 * UserSubscribe subscribe=readyUsers.get(k);
		 * if(subscribe.getLines().contains(sourceId)){ //String
		 * strHeader="SOURCE"
		 * +(char)0x02+MqttManagerV3.TOPIC_LOCAL+(char)0x02+MqttManagerV3
		 * .TOPIC_PREFIX+k+(char)0x02;
		 * MqttManagerV3.getInstance().sendWithThread(strHeader+strBody,
		 * MqttManagerV3.TOPIC_PREFIX+k); } }
		 */
		// 发送广播消息通知货源更新
		String strHeader = "-" + (char) 0x02 + "SUBS_CHNGED" + (char) 0x02 + strTime + (char) 0x02;
		MqttManagerV3.getInstance().sendWithThread(strHeader + strBody, MqttManagerV3.TOPIC_BROADCAST);
	}

	public static void sendAppMsg(String category, String event, String content) {
		String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date());
		StringBuffer sb = new StringBuffer();
		sb.append(category);
		sb.append((char) 0x02);
		sb.append(strTime);
		sb.append((char) 0x02);
		sb.append(event);
		sb.append((char) 0x02);
		sb.append(content);
		MqttManagerV3.getInstance().send(sb.toString(), MqttManagerV3.TOPIC_APP);
	}

	public static void sendNotify(String cid, String type, String title, String content, String detail) {
		StringBuffer sb = new StringBuffer();
		sb.append("NOTIFY");
		sb.append((char) 0x02);
		sb.append(MqttManagerV3.TOPIC_LOCAL);
		sb.append((char) 0x02);
		sb.append(MqttManagerV3.TOPIC_PREFIX + cid);
		sb.append((char) 0x02); // header end

		sb.append(title); // 标题
		sb.append((char) 0x03);
		sb.append(content);
		sb.append((char) 0x03);
		sb.append(type);
		sb.append((char) 0x03);
		sb.append(detail); // JSONObject

		MqttManagerV3.getInstance().sendWithThread(sb.toString(), MqttManagerV3.TOPIC_LOCAL);
	}

	/**
	 * 发送广播消息
	 * 
	 * @param cid
	 *            固定值 "all"
	 * @param type
	 *            固定值 "BROADCAST"
	 * @param title
	 *            消息ID
	 * @param content
	 *            add:添加消息,rm:删除消息
	 * @param detail
	 *            JSON格式的消息内容
	 */
	public static void sendBroadcast(String content, String detail) {
		String cid = "all";
		String type = "BROADCAST";
		String title = "BROAD" + System.nanoTime();
		StringBuffer sb = new StringBuffer();
		sb.append("BROADCAST");
		sb.append((char) 0x02);
		sb.append(MqttManagerV3.TOPIC_LOCAL);
		sb.append((char) 0x02);
		sb.append(MqttManagerV3.TOPIC_PREFIX + cid);
		sb.append((char) 0x02); // header end

		sb.append(title); // 标题
		sb.append((char) 0x03);
		sb.append(content);
		sb.append((char) 0x03);
		sb.append(type);
		sb.append((char) 0x03);
		sb.append(detail); // JSONObject
		sb.append((char) 0x03);
		sb.append(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())); // JSONObject

		MqttManagerV3.getInstance().sendWithThread(sb.toString(), MqttManagerV3.TOPIC_LOCAL);
	}

	public static void resetAccount(String cid) {
		HConnection connection = HBaseUtils.getHConnection();
		HTableInterface userTable = null;
		try {
			String rowid = cid;
			Put put = new Put(Bytes.toBytes(rowid));
			if (cid.startsWith("JGXX-")) {
				userTable = connection.getTable("syscfg");
				put.add(Bytes.toBytes("data"), Bytes.toBytes("balance"), Bytes.toBytes(0.0));
				put.add(Bytes.toBytes("data"), Bytes.toBytes("tmp_balance"), Bytes.toBytes(0.0));
				put.add(Bytes.toBytes("data"), Bytes.toBytes("income_balance"), Bytes.toBytes(0.0));
			} else {
				userTable = connection.getTable("mobile_users");
				put.add(Bytes.toBytes("data"), Bytes.toBytes("balance"), Bytes.toBytes(0.0));
			}

			userTable.put(put);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection(connection);
		}
	}

	public static void saveTraceToFile(String date, String toDir) {
		HConnection connection = HBaseUtils.getHConnection();
		HTableInterface userTable = null;
		try {
			userTable = connection.getTable("veh_trace");
			java.io.File destDir = new java.io.File(toDir);
			if (!destDir.exists())
				destDir.mkdir();
			long start = System.nanoTime();
			log.info("处理轨迹信息:" + date + ",dir=" + toDir);
			org.json.JSONArray filters = new org.json.JSONArray();
			org.json.JSONArray rows = com.xframe.utils.XHelper.listRowsByFilter("veh_trace", "-" + date, filters, true);
			System.out.println("\t记录数==" + rows.length());
			java.util.HashMap<String, String> hm = new java.util.HashMap<String, String>();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < rows.length(); i++) {
				org.json.JSONObject row = rows.getJSONObject(i);
				String strIp = row.getString("data:Ip");
				String lon = row.getString("data:Lon");
				String lat = row.getString("data:Lat");
				String time = row.getString("data:Time");

				sb.append(lon);
				sb.append(",");
				sb.append(lat);
				sb.append(",");
				sb.append(time);
				String strTmp = null;
				if (hm.containsKey(strIp)) {
					strTmp = hm.get(strIp);
				} else {
					strTmp = "";
				}
				if (strTmp.length() > 0)
					strTmp += "|";
				strTmp += sb.toString();
				hm.put(strIp, strTmp);
				sb.delete(0, sb.length());

				Delete del = new Delete(Bytes.toBytes(row.getString("rowid")));
				userTable.delete(del);
			}

			log.info("生成文件......");
			for (String k : hm.keySet()) {
				log.info("处理:" + k);
				org.mbc.util.Tools.writeTextFile(destDir.getAbsolutePath() + "/" + k, hm.get(k), "utf-8");
			}
			log.info("处理完成,车辆数:" + hm.size() + ",花费时间:" + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (userTable != null)
				try {
					userTable.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			HBaseUtils.closeConnection(connection);
		}
	}

	public static String formatAmount(double amount) {
		if (numberFormat == null) {
			numberFormat = java.text.NumberFormat.getInstance();
			numberFormat.setMinimumFractionDigits(2);
			numberFormat.setGroupingUsed(true);
		}
		return numberFormat.format(amount);
	}

	public static String formatAmountNoGroup(double amount) {
		return formatAmountNoGroup(amount, 2);
	}

	public static String formatAmountNoGroup(double amount, int len) {
		java.text.NumberFormat numberFormat = null;// 发现numberFormat
													// 不为空，可能是其他代码用了，重新！
		if (numberFormat == null) {
			numberFormat = java.text.NumberFormat.getInstance();
			numberFormat.setMinimumFractionDigits(len);
			numberFormat.setGroupingUsed(false);
		}
		return numberFormat.format(amount);
	}

	public static void updateUserCancelTotals(HConnection connection, String userId, int count, boolean added) throws Exception {
		// 获取对方用户当前交易信息
		try {
			org.json.JSONObject userRow = XHelper.loadRow(connection, "USER-" + userId, "mobile_users", true);
			int cancel_total = userRow.has("data:cancel_total") ? userRow.getInt("data:cancel_total") : 0;
			HTableInterface utable = connection.getTable("mobile_users");
			Put put1 = new Put(Bytes.toBytes("USER-" + userId));
			int totalNew = added ? cancel_total + count : count;
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("cancel_total"), Bytes.toBytes(totalNew + ""));
			utable.put(put1);
			utable.close();
		} catch (Exception ex) {
			log.warn("更新用户取消订单个数:" + ex.toString());
		}
	}

	/**
	 * 更新用户交易总数
	 * 
	 * @param connection
	 * @param userId
	 * @param count
	 * @param added
	 * @throws Exception
	 */
	public static void updateUserTransTotals(HConnection connection, String userId, int count, boolean added) throws Exception {
		log.info("updateUserTransTotals:userId=" + userId + ",count=" + count);
		// 获取对方用户当前交易信息
		org.json.JSONObject userRow = null;// XHelper.loadRow(connection,"USER-"+userId,"mobile_users",true);
		String rowid = userId;
		if (userId.indexOf("@") > 0) {
			rowid = "JGXX-" + userId.substring(0, userId.indexOf("@"));
			userRow = XHelper.loadRow(connection, rowid, "syscfg", true);
		} else {
			rowid = "USER-" + userId;
			userRow = XHelper.loadRow(connection, rowid, "mobile_users", true);
		}
		int trans_total = userRow.has("data:trans_total") ? userRow.getInt("data:trans_total") : 0;

		HTableInterface utable = connection.getTable("mobile_users");
		Put put1 = null;
		if (userId.indexOf("@") > 0) {
			utable = connection.getTable("syscfg");
			put1 = new Put(Bytes.toBytes(rowid));
		} else {
			utable = connection.getTable("mobile_users");
			put1 = new Put(Bytes.toBytes(rowid));
		}

		int totalNew = added ? trans_total + count : count;
		put1.add(Bytes.toBytes("data"), Bytes.toBytes("trans_total"), Bytes.toBytes(totalNew + ""));
		utable.put(put1);
		utable.close();
	}

	public static void updateUserTushuTotals(HConnection connection, String userId, int tousu_to, int tousu_from, boolean added) throws Exception {
		// 获取对方用户当前交易信息
		org.json.JSONObject userRow = null;// XHelper.loadRow(connection,"USER-"+userId,"mobile_users",true);
		String rowid = userId;
		if (userId.indexOf("@") > 0) {
			rowid = "JGXX-" + userId.substring(0, userId.indexOf("@"));
			userRow = XHelper.loadRow(connection, rowid, "syscfg", true);
		} else {
			rowid = "USER-" + userId;
			userRow = XHelper.loadRow(connection, rowid, "mobile_users", true);
		}
		int trans_tousu_to = userRow.has("data:trans_tousu_to") ? userRow.getInt("data:trans_tousu_to") : 0;
		int trans_tousu_from = userRow.has("data:trans_tousu_from") ? userRow.getInt("data:trans_tousu_from") : 0; // 来自对方的投诉
		HTableInterface utable = connection.getTable("mobile_users");
		Put put1 = null;
		if (userId.indexOf("@") > 0) {
			utable = connection.getTable("syscfg");
			put1 = new Put(Bytes.toBytes(rowid));
		} else {
			utable = connection.getTable("mobile_users");
			put1 = new Put(Bytes.toBytes(rowid));
		}

		int tousuToNew = added ? trans_tousu_to + tousu_to : tousu_to;
		int tousuFromNew = added ? trans_tousu_from + tousu_from : tousu_from;
		put1.add(Bytes.toBytes("data"), Bytes.toBytes("trans_tousu_from"), Bytes.toBytes(tousuFromNew + ""));
		put1.add(Bytes.toBytes("data"), Bytes.toBytes("trans_tousu_to"), Bytes.toBytes(tousuToNew + ""));
		utable.put(put1);
		utable.close();
	}

	public static void updateUserScoreTotals(HConnection connection, String userId, int score, boolean added) throws Exception {
		// 获取对方用户当前交易信息
		org.json.JSONObject userRow = null;

		String rowid = userId;
		if (userId.indexOf("@") > 0) {
			rowid = "JGXX-" + userId.substring(0, userId.indexOf("@"));
			userRow = XHelper.loadRow(connection, rowid, "syscfg", true);
		} else {
			rowid = "USER-" + userId;
			userRow = XHelper.loadRow(connection, rowid, "mobile_users", true);
		}
		// System.out.println("@@@userID="+rowid+",userRow="+userRow);
		int score_total = userRow.has("data:score_total") ? userRow.getInt("data:score_total") : 0;
		int trans_total = userRow.has("data:trans_total") ? userRow.getInt("data:trans_total") : 1;

		int scoreNew = added ? score + score_total : score;

		double trans_avg_score = scoreNew / trans_total;
		trans_avg_score = Math.ceil(trans_avg_score * 10) / 10;// 保留一位小数
		HTableInterface utable = null;
		Put put1 = null;
		if (userId.indexOf("@") > 0) {
			utable = connection.getTable("syscfg");
			put1 = new Put(Bytes.toBytes(rowid));
		} else {
			utable = connection.getTable("mobile_users");
			put1 = new Put(Bytes.toBytes(rowid));
		}

		put1.add(Bytes.toBytes("data"), Bytes.toBytes("score_total"), Bytes.toBytes(scoreNew + ""));
		put1.add(Bytes.toBytes("data"), Bytes.toBytes("trans_avg_score"), Bytes.toBytes(trans_avg_score + ""));
		utable.put(put1);
		utable.close();
	}

	public static void main(String[] args) {
		double n = 1234.8468;
		double m = 223;
		System.out.println(formatAmount(Math.ceil(n * 100) / 100));
		System.out.println(formatAmount(m));
		try {
			createDemoYd();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void createDemoYd() throws Exception {
		String strDemo = "{\"req_length\":\"18米\",\"fhrdh\":\"5132258\",\"yqsj\":\"\",\"fhr\":\"宋\",\"yd_0_time\":\"2016-11-29 16:45:57\",\"yd_trans_status\":\"已完成\",\"hwzl\":\"35\",\"from_prov\":\"河南省\",\"ydzt\":\"-\",\"yd_time\":\"2016-11-29 16:45:46\",\"huozhu_time\":\"2016-11-30 23:56:11\",\"fbr\":\"13073708392-huozhu\",\"driver_time\":\"2016-11-29 16:47:40\",\"hwmc\":\"速冻食品\",\"type\":\"SOURCE\",\"to_lat\":\"40.056404\",\"from_addr\":\"召陵区后谢乡双汇集团(双汇路)\",\"id\":\"13073708392-huozhu\",\"shr\":\"胡\",\"fbzt\":\"-\",\"to_prov\":\"天津\",\"huozhu_score\":\"5\",\"yd_2_time\":\"2016-11-29 16:46:23\",\"bf\":\"0\",\"req_type\":\"保温冷藏\",\"shrdh\":\"02258455583\",\"yd_1_time\":\"2016-11-29 16:46:12\",\"yd_3_time\":\"2016-11-29 16:46:53\",\"huozhu_appraise\":\"不错哦?\",\"sid\":\"161129-164458685-8\",\"yf\":\"1\",\"source_id\":\"河南漯河-天津天津\",\"yd_driver\":\"13015533627-driver\",\"from_city\":\"漯河市\",\"to_city\":\"天津市\",\"from_county\":\"召陵区\",\"driver_appraise\":\"可口可乐了蓝精灵\",\"driver_score\":\"5\",\"from_lon\":\"114.059581\",\"to_lon\":\"117.407396\",\"to_county\":\"蓟县\",\"yd_status\":\"5\",\"from_lat\":\"33.564489\",\"create_time\":\"2016-11-29 14:43:26\",\"hwtj\":\"40\",\"yj_driver_je\":\"0.2\",\"rowid\":\"13073708392-huozhu|漯河市 召陵区-天津市 蓟县|18米|保温冷藏|2016-11-29 14:43:26\",\"to_addr\":\"天津市蓟县渔阳镇津燕路\",\"fbsj\":\"2016-11-29 16:44:58\"}";
		org.json.JSONObject json = new org.json.JSONObject(strDemo);
		System.out.println("json=" + json);
		String fromCity = "郑州市";
		String[] fromCounties = new String[] {};
	}

	public static String getCookieByName(javax.servlet.http.Cookie[] cookies, String name) {
		// System.out.println("getCookieByName,cookies="+cookies);
		if (cookies == null)
			return null;
		for (javax.servlet.http.Cookie cookie : cookies) {
			// System.out.println("\tget cookie="+cookie.getName()+",value="+cookie.getValue());
			if (cookie.getName().equals(name)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static void setCookie(javax.servlet.ServletResponse res, String name, String value) {
		setCookie(res, name, value, 0);
	}

	public static void setCookie(javax.servlet.ServletResponse res, String name, String value, int expired) {
		Cookie cookie = new Cookie(name, value);
		if (expired > 0)
			cookie.setMaxAge(expired);
		((javax.servlet.http.HttpServletResponse) res).addCookie(cookie);
	}
}
