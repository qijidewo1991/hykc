package com.hyb.utils;

import org.apache.hadoop.hbase.client.HConnection;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.xframe.core.CoreServlet;
import com.xframe.utils.ComputingTool;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.XHelper;

public class CityDistributionUtil {
	public static final String CHENGPEI = "JGXX_chengpei";
	public static final String CITYDISLIST_NAME = "_citydis_request_";
	public static final String CP_PAY = "PAY";// 充值
	public static final String CP_ZFYF = "ZFYF";// 支付运费
	public static final String CP_ZFBZJ = "ZFBZJ";// 支付保证金
	public static final String CP_YDJS = "YDJS";// 运单结束
	public static final String CP_CXYD = "CXYD";// 撤销运单
	public static final String CP_CLTX = "CLTX";// 提现申请处理

	public static final String CITYDIS_DEPS = "DEPS";// 货主支付运费,司机支付保证金，zz徐超
	public static final String CITYDIS_FINISH = "FINISH";// 运单结束
	public static final String CITYDIS_PAY = "PAY";// 充值
	private static boolean isProcessing = false;
	private static final Logger log = Logger.getLogger(CityDistributionUtil.class);

	// 城配检索
	public static boolean hasCityDisRequest() {
		return RedisClient.getInstance().getListSize(CITYDISLIST_NAME) > 0;
	}

	public static boolean isBusy() {
		return isProcessing;
	}

	public static void addRequest(org.json.JSONObject o) {
		com.hyb.utils.RedisClient redis = com.hyb.utils.RedisClient.getInstance("");
		redis.addToList(CITYDISLIST_NAME, o.toString());
		log.info("添加管道数据" + o.toString());
		if (CoreServlet.zkMonitor.isMaster) {
			log.info("MMMMMMM=" + CoreServlet.zkMonitor.isMaster);
			if (!CityDistributionUtil.isBusy()) {
				log.info("redis是空闲=" + !CityDistributionUtil.isBusy());
				processRequest();
				log.info("管道执行完毕");
			}
		}
	}

	public static void processRequest() {
		com.hyb.utils.RedisClient redis = com.hyb.utils.RedisClient.getInstance("");
		long reqSize = redis.getListSize(CITYDISLIST_NAME);
		log.info("城配待处理请求数:" + reqSize);
		if (reqSize == 0) {
			return;
		}
		org.json.JSONObject o = null;
		String requestId = "";
		String strReq = redis.popFromList(CITYDISLIST_NAME);
		log.info("待处理请求:" + strReq);
		try {
			o = new org.json.JSONObject(strReq);
			requestId = o.getString("requestid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (o == null) {
			return;
		}
		isProcessing = true;
		JSONObject retJson = new JSONObject();
		try {
			retJson = handleRequest(o);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				retJson.put("success", "false");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		RedisClient.getInstance().setStr(requestId, retJson.toString());
		isProcessing = false;
	}

	public static JSONObject handleRequest(JSONObject o) throws Exception {
		java.util.Date now = new java.util.Date();
		String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
		JSONObject retJson = new JSONObject();
		retJson.put("success", "false");
		retJson.put("message", "处理成功");
		JSONObject desc = new JSONObject();
		desc.put("data:price", "NUMBER");
		desc.put("data:balance", "NUMBER");
		String requestType = o.getString("requesttype");
		String rowid = o.has("rowid") ? o.getString("rowid") : "";
		JSONObject ydInfo = rowid.equals("") ? new JSONObject() : XHelper.loadRow(null, rowid, "cp_orderInfo", false, desc);
		if (requestType.equals(CP_PAY)) {// 充值
			if (o.getString("paysoft").equals("zfb")) {
				String notify_time = o.getString("notify_time");
				String app_id = o.getString("app_id");
				String out_trade_no = o.getString("out_trade_no");
				String total_amount = o.getString("total_amount");
				String buyer_logon_id = o.getString("buyer_logon_id");
				JSONObject orderinfo = XHelper.loadRow(out_trade_no, "cp_account_detail");
				String amount = orderinfo.getString("amount");
				if (Double.parseDouble(amount) != Double.parseDouble(total_amount)) {
					return retJson;
				}
				JSONObject col = new JSONObject();
				col.put("sfzf", "1");
				col.put("notify_time", notify_time);
				col.put("app_id", app_id);
				col.put("buyer_logon_id", buyer_logon_id);
				col.put("total_amount", total_amount);
				col.put("body", o.toString());
				XHelper.createRow("cp_account_detail", out_trade_no, col);
				boolean retb1 = false, retb2 = false;
				retb1 = updateAccount(null, "mobile", orderinfo.getString("cid"), "PAY", "充值", Double.parseDouble(total_amount), CHENGPEI, out_trade_no);
				retb2 = updateAccount(null, "total", CHENGPEI, "PAY", "充值", Double.parseDouble(total_amount), orderinfo.getString("cid"), out_trade_no);
				if (!(retb1 && retb2)) {
					retJson.put("message", "账户更新失败");
					return retJson;
				}
				retJson.put("success", "true");
				return retJson;
			}
		} else if (requestType.equals(CP_ZFYF)) {// 支付运费
			String cid = o.getString("cid");
			JSONObject hzInfo = XHelper.loadRow(null, cid, "cp_users", false, desc);
			double ydPrice = Double.parseDouble(ydInfo.getString("price"));
			double balance = Double.parseDouble(hzInfo.getString("balance"));
			if (!(ydInfo.getString("status").equals("0"))) {
				retJson.put("message", "运单状态错误");
				return retJson;
			}
			if (balance < ydPrice) {
				retJson.put("message", "账户余额不足");
				return retJson;
			}
			boolean retb1 = false, retb2 = false, retb3 = false;
			retb1 = updateAccount(null, "mobile", cid, "DEPS", "支付运费", -ydPrice, CHENGPEI, rowid);
			retb2 = updateAccount(null, "temp", CHENGPEI, "DEPS", "支付运费", ydPrice, cid, rowid);
			if (!(retb1 && retb2)) {
				retJson.put("message", "账户更新失败");
				return retJson;
			}
			JSONObject col = new JSONObject();
			col.put("actual_yf", "" + ydPrice);
			col.put("status", "1");
			retb3 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
			if (!retb3) {
				retJson.put("message", "运单更新失败");
				return retJson;
			}
			retJson.put("success", "true");
			return retJson;
		} else if (requestType.equals(CP_ZFBZJ)) {// 支付保证金
			String cid = o.getString("cid");
			JSONObject driverInfo = XHelper.loadRow(null, cid, "cp_users", false, desc);
			double ydPrice = Double.parseDouble(ydInfo.getString("price"));
			double ydSxfbl = Double.parseDouble(ydInfo.getString("sxfbl"));
			double balance = Double.parseDouble(driverInfo.getString("balance"));
			double bzj = ComputingTool.bigDecimalAddition(ydPrice, -ydPrice * ydSxfbl) * 0.1;
			if (!(ydInfo.getString("status").equals("1"))) {
				retJson.put("message", "运单状态错误");
				return retJson;
			}
			if (balance < bzj) {
				retJson.put("message", "账户余额不足");
				return retJson;
			}
			boolean retb1 = false, retb2 = false, retb3 = false;
			retb1 = updateAccount(null, "mobile", cid, "DEPS", "支付保证金", -bzj, CHENGPEI, rowid);
			retb2 = updateAccount(null, "temp", CHENGPEI, "DEPS", "支付保证金", bzj, cid, rowid);
			if (!(retb1 && retb2)) {
				retJson.put("message", "账户更新失败");
				return retJson;
			}
			JSONObject col = new JSONObject();
			col.put("actual_bzj", "" + bzj);
			col.put("driver", cid);
			col.put("driver_name", driverInfo.getString("name"));
			col.put("jd_time", strTime);
			col.put("status", "3");
			retb3 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
			if (!retb3) {
				retJson.put("message", "运单更新失败");
				return retJson;
			}
			retJson.put("success", "true");
			return retJson;
		} else if (requestType.equals(CP_YDJS)) {// 运单结束
			double ydPrice = Double.parseDouble(ydInfo.getString("price"));
			double ydSxfbl = Double.parseDouble(ydInfo.getString("sxfbl"));
			double bzj = Double.parseDouble(ydInfo.getString("actual_bzj"));
			double getYf = ComputingTool.bigDecimalAddition(ydPrice, -ydPrice * ydSxfbl);
			double income = ydPrice * ydSxfbl;
			String driverId = ydInfo.getString("driver");
			if (!(ydInfo.getString("status").equals("5"))) {
				retJson.put("message", "运单状态错误");
				return retJson;
			}
			boolean retb1 = false, retb2 = false, retb3 = false, retb4 = false, retb5 = false, retb6 = false, retb7 = false;
			retb1 = updateAccount(null, "mobile", driverId, "DEPR", "支付运费", getYf, CHENGPEI, rowid);
			retb2 = updateAccount(null, "temp", CHENGPEI, "DEPR", "支付运费", -getYf, driverId, rowid);
			retb3 = updateAccount(null, "temp", CHENGPEI, "CHARGE", "手续费收入", -income, "income", rowid);
			retb4 = updateAccount(null, "income", CHENGPEI, "CHARGE", "手续费收入", income, "temp", rowid);
			retb6 = updateAccount(null, "mobile", driverId, "FEER", "退回保证金", bzj, CHENGPEI, rowid);
			retb7 = updateAccount(null, "temp", CHENGPEI, "FEER", "退回保证金", -bzj, driverId, rowid);
			if (!(retb1 && retb2 && retb3 && retb4 && retb6 && retb7)) {
				retJson.put("message", "账户更新失败");
				return retJson;
			}
			JSONObject col = new JSONObject();
			col.put("finish_time", strTime);
			col.put("status", "6");
			retb5 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
			if (!retb5) {
				retJson.put("message", "运单更新失败");
				return retJson;
			}
			retJson.put("success", "true");
			return retJson;
		} else if (requestType.equals(CP_CXYD)) {// 撤销运单
			double ydPrice = Double.parseDouble(ydInfo.getString("price"));
			double ydSxfbl = Double.parseDouble(ydInfo.getString("sxfbl"));
			double bzj = ydInfo.has("actual_bzj") ? Double.parseDouble(ydInfo.getString("actual_bzj")) : 0;
			double pcj = ydPrice * 0.03;
			double wyj = ydPrice * 0.03;
			String cid = o.getString("cid");
			String driverId = ydInfo.has("driver") ? ydInfo.getString("driver") : "";
			String cxtype = o.getString("usertype");
			int ydStatus = Integer.parseInt(ydInfo.getString("status"));
			if (cxtype.equals("huozhu")) {
				if (4 >= ydStatus && ydStatus >= 2) {// 需要扣除违约金，给司机赔偿金
					boolean retb1 = false, retb2 = false, retb3 = false, retb4 = false, retb5 = false, retb6 = false, retb7 = false, retb8 = false;
					retb1 = updateAccount(null, "mobile", cid, "FEER", "退回运费", ydPrice, CHENGPEI, rowid);
					retb2 = updateAccount(null, "temp", CHENGPEI, "FEER", "退回运费", -ydPrice, cid, rowid);

					retb3 = updateAccount(null, "mobile", cid, "WYJ", "违约金", -wyj, CHENGPEI, rowid);
					retb4 = updateAccount(null, "income", CHENGPEI, "WYJ", "违约金", wyj, cid, rowid);

					retb5 = updateAccount(null, "mobile", driverId, "PCJ", "赔偿金", pcj, CHENGPEI, rowid);
					retb6 = updateAccount(null, "income", CHENGPEI, "PCJ", "赔偿金", -pcj, driverId, rowid);

					retb7 = updateAccount(null, "mobile", driverId, "PCJ", "退回保证金", bzj, CHENGPEI, rowid);
					retb8 = updateAccount(null, "temp", CHENGPEI, "PCJ", "退回保证金", -bzj, driverId, rowid);
					if (!(retb1 && retb2 && retb3 && retb4 && retb5 && retb6 && retb7 && retb8)) {
						retJson.put("message", "账户更新失败");
						return retJson;
					}
					JSONObject col = new JSONObject();
					col.put("cancel_type", cxtype);
					col.put("cancel_id", cid);
					col.put("cancel_time", strTime);
					col.put("wyj", wyj);
					col.put("pcj", pcj);
					col.put("status", "9");
					retb5 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
					if (!retb5) {
						retJson.put("message", "运单更新失败");
						return retJson;
					}
					retJson.put("success", "true");
					return retJson;
				} else if (ydStatus == 1) {
					boolean retb1 = false, retb2 = false, retb3 = false;
					retb1 = updateAccount(null, "mobile", cid, "FEER", "退回运费", ydPrice, CHENGPEI, rowid);
					retb2 = updateAccount(null, "temp", CHENGPEI, "FEER", "退回运费", -ydPrice, cid, rowid);
					if (!(retb1 && retb2)) {
						retJson.put("message", "账户更新失败");
						return retJson;
					}
					JSONObject col = new JSONObject();
					col.put("cancel_type", cxtype);
					col.put("cancel_id", cid);
					col.put("cancel_time", strTime);
					col.put("status", "9");
					retb3 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
					if (!retb3) {
						retJson.put("message", "运单更新失败");
						return retJson;
					}
				} else {
					retJson.put("message", "不可撤销状态");
					return retJson;
				}
			} else {
				if (ydStatus == 2) {
					boolean retb1 = false;
					JSONObject col = new JSONObject();
					col.put("driver", "-");
					col.put("driver_name", "-");
					col.put("cancel_id", cid);
					col.put("cancel_time", strTime);
					col.put("status", "1");
					retb1 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
					if (!retb1) {
						retJson.put("message", "运单更新失败");
						return retJson;
					}
				} else if (ydStatus >= 3 && ydStatus <= 4) {
					boolean retb1 = false, retb2 = false, retb3 = false;
					retb1 = updateAccount(null, "mobile", driverId, "PCJ", "退回保证金", bzj, CHENGPEI, rowid);
					retb2 = updateAccount(null, "temp", CHENGPEI, "PCJ", "退回保证金", -bzj, driverId, rowid);
					if (!(retb1 && retb2)) {
						retJson.put("message", "账户更新失败");
						return retJson;
					}
					JSONObject col = new JSONObject();
					col.put("driver", "-");
					col.put("cancel_id", cid);
					col.put("cancel_time", strTime);
					col.put("status", "1");
					retb3 = XHelper.createRow(null, "cp_orderInfo", rowid, col);
					if (!retb3) {
						retJson.put("message", "运单更新失败");
						return retJson;
					}
				} else {
					retJson.put("message", "不可撤销状态");
					return retJson;
				}
			}
			retJson.put("success", "true");
			return retJson;
		} else if (requestType.equals(CP_CLTX)) {// 提现申请处理
			JSONObject json = new JSONObject();
			String cid = o.getString("cid");
			String deal_id = o.getString("deal_id");
			String ext = o.getString("ext");
			json.put("ext", ext);
			json.put("deal_id", deal_id);
			double cashAccount = Double.parseDouble(o.getString("account"));
			JSONObject userInfo = XHelper.loadRow(null, cid, "cp_users", false, desc);
			double balance = Double.parseDouble(userInfo.getString("balance"));
			if (balance < cashAccount) {
				retJson.put("message", "账户余额不足");
				return retJson;
			}
			boolean retb1 = false, retb2 = false;
			retb1 = updateAccount(null, "mobile", cid, "CASH", "提现", -cashAccount, CHENGPEI, rowid, json);
			retb2 = updateAccount(null, "total", CHENGPEI, "CASH", "提现", -cashAccount, cid, rowid, json);
			if (!(retb1 && retb2)) {
				retJson.put("message", "账户更新失败");
				return retJson;
			}
			retJson.put("success", "true");
			return retJson;

		}
		return retJson;
	}

	/**
	 * 用以创建账单详情
	 * 
	 * @param connection
	 *            数据库连接
	 * @param acctType
	 *            mobile、total、temp、income 账户类型
	 * @param acctId
	 *            交易主体id
	 * @param transType
	 *            交易类型
	 * @param typeName
	 *            交易描述
	 * @param amount0
	 *            交易金额
	 * @param balance
	 *            交易主体原来余额
	 * @param rest
	 *            交易主体交易后余额
	 * @param fromAccount
	 *            交易对象id
	 * @param srcId
	 *            交易关联业务id
	 * @param ext
	 *            其他备注
	 * @return
	 * @throws Exception
	 */
	public static boolean createAccountDetail(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount0, double balance, double rest,
			String fromAccount, String srcId, JSONObject ext) throws Exception {
		java.util.Date now = new java.util.Date();
		String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
		String timeid = org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS", now);
		String orderNo = transType + timeid;
		String orderName = typeName;
		double amount = Math.ceil(amount0 * 100.0) / 100.0;
		final String rowid = timeid + "-" + orderNo + "-" + acctId;
		org.json.JSONObject json = new org.json.JSONObject();
		json.put("account_type", acctType);
		json.put("order_no", orderNo);
		json.put("order_name", orderName);
		json.put("cid", acctId);
		json.put("order_time", strTime);
		json.put("type", transType);
		json.put("sfzf", "1");
		json.put("from_account", fromAccount);
		json.put("source_id", srcId);
		json.put("submit_time", org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now));
		json.put("order_amount", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + amount + "\"}"));
		json.put("change_amount", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + amount + "\"}"));
		json.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + balance + "\"}"));
		json.put("balance_rest", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + rest + "\"}"));
		if (ext != null) {
			java.util.Iterator<String> keys = ext.keys();
			while (keys.hasNext()) {
				String k = keys.next();
				String val = ext.getString(k);
				json.put(k, val);
			}
		}
		boolean bRet = XHelper.createRow(connection, "cp_account_detail", rowid, json);
		if (!bRet) {
			LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配账户更新失败(1)", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + acctId);
			return false;
		}
		return true;
	}

	/**
	 * 用以更新账目信息
	 * 
	 * @param connection
	 *            数据库连接
	 * @param acctType
	 *            mobile、total、temp、income 账户类型
	 * @param acctId
	 *            交易主体id
	 * @param transType
	 *            交易类型
	 * @param typeName
	 *            交易描述
	 * @param amount
	 *            交易金额
	 * @param fromAccount
	 *            交易对象
	 * @param srcId
	 *            交易关联业务id
	 * @return
	 * @throws Exception
	 */
	public static boolean updateAccount(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId, JSONObject ext)
			throws Exception {
		boolean bRet = false;
		double balance = 0;
		double result = 0;
		JSONObject row = new JSONObject();
		JSONObject argsObj = new JSONObject();
		argsObj.put("data:balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:tmp_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:income_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:total_balance", HBaseUtils.TYPE_NUMBER);
		JSONObject col1 = new JSONObject();
		if (acctType.equals("temp") || acctType.equals("income") || acctType.equals("total")) {
			JSONObject sysinfo = XHelper.loadRow(null, acctId, "cp_syscfg", false, argsObj);
			row = (sysinfo == null) ? new JSONObject() : sysinfo;
			if (acctType.equals("temp")) {
				if (row.has("tmp_balance")) {
					balance = row.getDouble("tmp_balance");
				}
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				col1.put("tmp_balance", new JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			} else if (acctType.equals("income")) {
				if (row.has("income_balance")) {
					balance = row.getDouble("income_balance");
				}
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				col1.put("income_balance", new JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			} else if (acctType.equals("total")) {
				if (row.has("total_balance")) {
					balance = row.getDouble("total_balance");
				}
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				col1.put("total_balance", new JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			}
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId, ext);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			bRet = XHelper.createRow(connection, "cp_syscfg", acctId, col1);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配交易改变余额失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
		} else {
			row = XHelper.loadRow(null, acctId, "cp_users", false, argsObj);
			if (row.has("balance")) {
				balance = row.getDouble("balance");
			}
			result = ComputingTool.bigDecimalAddition(balance, amount);
			result = Math.ceil(result * 100.0) / 100.0;
			col1.put("balance", new JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId, ext);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			if (acctId.startsWith("JGXX")) {
				bRet = XHelper.createRow(connection, "cp_syscfg", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配交易改变余额失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			} else {
				bRet = XHelper.createRow(connection, "cp_users", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "城配交易改变余额失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			}

		}
		return true;
	}

	/**
	 * @param connection
	 * @param acctType
	 * @param acctId
	 * @param transType
	 * @param typeName
	 * @param amount
	 * @param fromAccount
	 * @param srcId
	 * @return
	 * @throws Exception
	 */
	public static boolean updateAccount(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId) throws Exception {
		return updateAccount(connection, acctType, acctId, transType, typeName, amount, fromAccount, srcId, null);
	}
}
