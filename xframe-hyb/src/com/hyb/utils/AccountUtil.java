package com.hyb.utils;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mbc.util.MD5Tools;

import com.jindie.k2kutils.SendToK2K;
import com.mongodb.util.JSON;
import com.swiftpass.config.SwiftpassConfig;
import com.swiftpass.util.MD5;
import com.swiftpass.util.SignUtils;
import com.swiftpass.util.XmlUtils;
import com.xframe.core.CoreServlet;
import com.xframe.utils.AppHelper;
import com.xframe.utils.ComputingTool;
//import com.xframe.utils.CacheService;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.XHelper;

public class AccountUtil {
	private static final Logger log = Logger.getLogger(AccountUtil.class);
	public static final String PAY = "PAY";// 充值
	/**
	 * 垫付充值
	 */
	public static final String DFCZ = "DFCZ";
	/**
	 * 汇票充值
	 */
	public static final String HPCZ = "HPCZ";
	/**
	 * 现金回款
	 */
	public static final String XJHK = "XJHK";
	/**
	 * 汇票回款
	 */
	public static final String HPHK = "HPHK";
	public static final String CASH = "CASH";// :提现,
	public static final String DEPS = "DEPS";// :保证金支付,
	public static final String DEPR = "DEPR";// :保证金退还,
	public static final String FEEP = "FEEPF";// :费用支付,
	public static final String FEER = "FEER";// :费用收取
	public static final String CHARGE = "CHARGE";// :手续费收取
	public static final String BX = "BX";// 保险
	public static final String WYJ = "WYJ";// 违约金收取
	public static final String PCJ = "PCJ";// 违约赔偿金

	public static final String ACCTLIST_NAME = "_account_request_";

	public static int OrderBaseNumber = 0;

	private static final String HuozhuAppID = "wxbd95aaac264a18ec";
	private static final String DriverAppID = "wx7c4b7be9da0b8dde";

	private static java.util.HashMap<String, String> conf = null;

	private static boolean isProcessing = false;

	private static java.text.NumberFormat numFormat;

	// private static java.util.ArrayList<org.json.JSONObject> reqList=new
	// java.util.ArrayList<org.json.JSONObject>();

	private synchronized static String getOrderNo(int len) {
		if (numFormat == null) {
			numFormat = java.text.NumberFormat.getInstance();
			numFormat.setGroupingUsed(false);
			numFormat.setMinimumIntegerDigits(4);
		}
		if (len == 0) {
			int t = new java.util.Random().nextInt(10000);
			String strRandom = numFormat.format(t);
			java.util.Date now = new java.util.Date();
			String timeid = org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS", now);
			return timeid + strRandom;
		} else {
			java.util.Date now = new java.util.Date();
			String timeid = org.mbc.util.Tools.formatDate("yyMMddHHmm", now);
			int t = OrderBaseNumber;
			OrderBaseNumber++;
			if (OrderBaseNumber > 99)
				OrderBaseNumber = 0;
			if (OrderBaseNumber < 10)
				timeid += "0" + OrderBaseNumber;
			else
				timeid += "" + OrderBaseNumber;
			return timeid;
		}
	}

	private synchronized static String getOrderNo() {
		return getOrderNo(0);
	}

	/**
	 * 收到支付完成后更新支付结果
	 * 
	 * @param orgOrderNo
	 *            订单号
	 * @param time
	 *            交易时间
	 * @param tradeNo
	 *            交易号
	 * @param buyer_id
	 *            购买人ID
	 * @param amount
	 *            金额
	 * @param payFrom
	 *            交易来源
	 */
	public static void updateZfResult(String orgOrderNo, String time, String tradeNo, String buyer_id, String amount, String payFrom) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		HTableInterface userTable = null;
		try {
			json.put("success", "false");
			String rowid = orgOrderNo;
			org.json.JSONObject row = XHelper.loadRow(null, rowid, "account_detail", false, new org.json.JSONObject("{\"data:order_amount\":\"" + HBaseUtils.TYPE_NUMBER + "\"}"));
			if (row == null) {
				json.put("message", "订单不存在!");
				return;
			}

			if (row.has("trade_no")) {
				log.info("重复交易提醒:" + orgOrderNo + ",time=" + time + ",amount:" + amount + ",from:" + payFrom);
				return;
			}

			// CacheService.getInstance().putInCache(orgOrderNo, "1");
			RedisClient.getInstance().setStr(orgOrderNo, "1");

			String cid = row.getString("cid");
			String rowType = row.getString("type");
			String orderName = row.getString("order_name");
			String tokenid = row.getString("tokenid");

			userTable = connection.getTable("account_detail");
			Put put = new Put(Bytes.toBytes(rowid));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("trade_no"), Bytes.toBytes(tradeNo));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("buyer_id"), Bytes.toBytes(buyer_id));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("sfzf"), Bytes.toBytes("1"));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("pay_time"), Bytes.toBytes(time));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("pay_amount"), Bytes.toBytes(amount));
			userTable.put(put);

			LogUtil.info(LogUtil.CAT_ACCOUNT, orderName + "付款成功.", "type:" + rowType + ",orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);

			// 记账
			updateAccount_1(cid, orgOrderNo, tokenid);

			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("success", "true");
			obj.put("tokenid", tokenid);
			obj.put("notify_type", "account");
			obj.put("pay", "1");
			obj.put("type", rowType);
			obj.put("amount", amount);
			String title = "您通过" + payFrom + "成功支付" + amount + "元.";
			AppUtil.sendNotify(row.has("uid") ? row.getString("uid") : cid, LogUtil.INFO, "提示", title, obj.toString());

			String mobiles = "";
			if (cid.endsWith("driver") || cid.endsWith("huozhu")) {
				mobiles = cid.substring(0, cid.indexOf("-"));
				double balance = loadBalance(cid);
				MySMSUtil.sendPaySms(payFrom, amount, String.valueOf(balance), mobiles);
			}

		} catch (Exception ex) {
			log.info("异常:" + ex.toString());
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

	/**
	 * 充值提现账户更新
	 * 
	 * @param cid
	 * @param orderNo
	 * @param tokenid
	 * @return
	 */
	private static org.json.JSONObject updateAccount_1(String cid, String orgOrderNo, String tokenid) {
		System.out.println("---------------------" + cid + "------" + orgOrderNo + "-------" + tokenid);
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			String rowid = orgOrderNo;
			org.json.JSONObject row = XHelper.loadRow(null, rowid, "account_detail", false, new org.json.JSONObject("{\"data:order_amount\":\"" + HBaseUtils.TYPE_NUMBER + "\"}"));
			if (row == null) {
				json.put("message", "订单不存在!");
				return json;
			}

			// log.info("\trow=="+row);
			if (!row.has("tokenid") || !row.getString("tokenid").equals(tokenid)) {
				json.put("message", "订单无效!");
				return json;
			}

			String rowType = row.getString("type");

			double amount = row.getDouble("order_amount");
			String partent_dept = Configuration.adminAccount;
			if (cid.indexOf("JGXX") == 0) {
				JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(cid, "syscfg");
				partent_dept = json_cid_info.getString("parent_dept");
			}
			System.out.println("充值partent_dept机构号=" + partent_dept);
			if (rowType.endsWith(PAY)) {
				addToAccount(connection, "total", partent_dept, PAY, "充值", amount, cid, rowid);
				addToAccount(connection, "mobile", cid, PAY, "充值", amount, partent_dept, rowid);
			} else if (rowType.endsWith(CASH)) {
				// 待添加
				if (cid.indexOf("driver") >= 0) {
					String from_dep = row.getString("from_dept");
					addToAccount(connection, "total", from_dep, CASH, "提现", -amount, cid, rowid);
					addToAccount(connection, "mobile", cid, CASH, "提现", -amount, from_dep, rowid);
				} else {
					addToAccount(connection, "total", partent_dept, CASH, "提现", -amount, cid, rowid);
					addToAccount(connection, "mobile", cid, CASH, "提现", -amount, partent_dept, rowid);
				}

			}
			json.put("amount", amount);

			if (row.has("pay_type"))
				json.put("pay_type", row.getString("pay_type"));

			// 本账户余额
			// row=XHelper.loadRow(connection,"USER-"+cid, "mobile_users",
			// false,new
			// org.json.JSONObject("{\"data:balance\":\""+HBaseUtils.TYPE_NUMBER+"\"}"));
			org.json.JSONObject argsObj = new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}");
			if (cid.startsWith("JGXX")) {
				row = XHelper.loadRow(null, cid, "syscfg", false, argsObj);
			} else {
				row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false, argsObj);
			}
			if (row.has("balance"))
				json.put("balance", row.getDouble("balance"));

			LogUtil.info(LogUtil.CAT_ACCOUNT, "账户明细更新成功.", "orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);
			json.put("success", "true");
			// log.info("update result:"+XHelper.loadRow(null,"USER-"+cid,
			// "mobile_users", false,new
			// org.json.JSONObject("{\"data:balance\":\""+HBaseUtils.TYPE_NUMBER+"\"}")));

		} catch (Exception ex) {
			log.info("异常:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	/**
	 * 2017年10月12日15:42:47 wdx 财务充值
	 * 
	 * @param cid
	 * @param amount
	 *            金额
	 * @param reason
	 *            备注
	 * @param acctName
	 *            物流公司名称
	 * @param hp_type
	 *            汇票类型   银行承兑汇票（yinhang）  商业承兑汇票（shangye）
	 * @param oper
	 *            authedUser
	 * @return
	 */
	public static org.json.JSONObject cwczAdjustAccount(String cid, double amount, String type, String acctName, String oper,String hp_type,String reason) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");

			String rowid = "";
			org.json.JSONObject row = null;
			String of_dept = XHelper.loadRow(cid, "syscfg").getString("parent_dept");
			System.out.println("财务充值" + cid + "*****type=" + type);
			// 垫付充值
			if (type.equals("dfcz")) {
				System.out.println("垫付充值");
				addToAccount2(connection, "total", of_dept, DFCZ, "垫付充值", amount, cid, rowid, oper,"", "");
				addToAccount2(connection, "mobile", cid, DFCZ, "垫付充值", amount, of_dept, rowid, oper,"", "");

			}

			// 汇票充值
			if (type.equals("hpcz")) {
				System.out.println("汇票充值");
				addToAccount2(connection, "total", of_dept, HPCZ, "汇票充值", amount, cid, rowid, oper,hp_type, reason);
				addToAccount2(connection, "mobile", cid, HPCZ, "汇票充值", amount, of_dept, rowid, oper, hp_type,reason);
			}

			// 收垫付，现金
			if (type.equals("sdfxj")) {
				System.out.println("收垫付，现金");
				addToAccount2(connection, "total", of_dept, XJHK, "现金还款", amount, cid, rowid, oper,"", "");
				addToAccount2(connection, "mobile", cid, XJHK, "现金还款", amount, of_dept, rowid, oper,"", "");
			}

			// 收垫付，汇票
			if (type.equals("sdfhp")) {
				System.out.println("收垫付，汇票");
				addToAccount2(connection, "total", of_dept, HPHK, "汇票还款", amount, cid, rowid, oper,hp_type, reason);
				addToAccount2(connection, "mobile", cid, HPHK, "汇票还款", amount, of_dept, rowid, oper,hp_type, reason);
			}
			json.put("amount", amount);
			json.put("success", "true");

		} catch (Exception ex) {
			try {
				json.put("success", "false");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			log.info("异常:" + ex.toString());
			System.out.println(ex);
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	public static org.json.JSONObject adjustAccount(String cid, double amount, String reason, String acctName, String oper) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");

			String rowid = "";
			org.json.JSONObject row = null;
			addToAccount(connection, "total", Configuration.adminAccount, PAY, "账户调整", amount, cid, rowid);
			addToAccount(connection, "mobile", cid, PAY, "账户调整", amount, Configuration.adminAccount, rowid);
			json.put("amount", amount);

			org.json.JSONObject argsObj = new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}");
			if (cid.startsWith("JGXX")) {
				row = XHelper.loadRow(null, cid, "syscfg", false, argsObj);
			} else {
				row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false, argsObj);
			}
			if (row.has("balance"))
				json.put("balance", row.getDouble("balance"));

			LogUtil.warn(LogUtil.CAT_MAINTAIN, "账户调整 " + cid + "(" + acctName + ")", "原因:" + reason + ",金额:" + amount + ",调后余额:" + row.getDouble("balance"), oper);
			json.put("success", "true");

		} catch (Exception ex) {
			log.info("异常:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	/**
	 * 平台保证金账户和个人账户的资金往来
	 * 
	 * @param cid
	 * @param orderNo
	 * @param tokenid
	 * @return
	 */
	private static org.json.JSONObject updateAccount_2(String cid, String orgOrderNo, String tokenid) {
		org.json.JSONObject json = new org.json.JSONObject();
		boolean isZfbzj = false;
		String sourceId = null;// 货源ID;
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			String rowid = orgOrderNo;
			org.json.JSONObject row = XHelper.loadRow(null, rowid, "account_detail", false, new org.json.JSONObject("{\"data:order_amount\":\"" + HBaseUtils.TYPE_NUMBER + "\"}"));
			if (row == null) {
				json.put("message", "订单不存在!");
				return json;
			}

			// log.info("\trow=="+row);
			if (!row.has("tokenid") || !row.getString("tokenid").equals(tokenid)) {
				json.put("message", "订单无效!");
				return json;
			}

			double amount = row.getDouble("order_amount");
			String rowType = row.getString("type");
			JSONObject json_ext=new JSONObject(row.getString("ext"));
			if (rowType.endsWith(DEPS)) {
				if (amount > 0) {
					String partent_dept = Configuration.adminAccount;
					if (cid.indexOf("JGXX") == 0) {
						JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(cid, "syscfg");
						partent_dept = json_cid_info.getString("parent_dept");
					}
					System.out.println("支付保证金partent_dept=" + partent_dept);
					addToAccount(connection, "temp", partent_dept, DEPS, "保证金支付", amount, cid, json_ext.getString("rowid"));
					addToAccount(connection, "mobile", cid, DEPS, "保证金支付", -amount, partent_dept, json_ext.getString("rowid"));
				}
			} else if (rowType.endsWith(DEPR)) {
				String partent_dept = Configuration.adminAccount;
				if (cid.indexOf("JGXX") == 0) {
					JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(cid, "syscfg");
					partent_dept = json_cid_info.getString("parent_dept");
				}
				System.out.println("退还保证金partent_dept=" + partent_dept);
				addToAccount(connection, "temp", partent_dept, DEPR, "保证金退还", -amount, cid, json_ext.getString("rowid"));
				addToAccount(connection, "mobile", cid, DEPR, "保证金退还", amount, partent_dept, json_ext.getString("rowid"));
			}

			if (rowType.equals(DEPS) && row.has("ext") && row.getString("ext").trim().length() > 0) {
				try {
					String ext = row.getString("ext");
					org.json.JSONObject o = new org.json.JSONObject(ext);
					String s_rowid = o.getString("rowid");
					String flag = o.getString("flag");
					String actType = o.has("act_type") ? o.getString("act_type") : "0";

					// 更新运单信息
					HTableInterface utable = connection.getTable("yd_list");
					Put put1 = new Put(Bytes.toBytes(s_rowid));
					put1.add(Bytes.toBytes("data"), Bytes.toBytes(flag), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
					if (flag.equals("driver_deps")) {// 司机支付完保证金，变更运单状态
						sourceId = s_rowid;
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes(Configuration.statusNames[1]));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_" + 1 + "_time"), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes("1"));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yj_driver_je"), Bytes.toBytes("" + amount));
						isZfbzj = true;
						org.json.JSONObject jsonSource = Configuration.getSoureFromCache(s_rowid);
						if (jsonSource != null) {
							jsonSource.put("yd_status", "1");
							Configuration.putSourceToCache(s_rowid, jsonSource);
							/*
							 * if(actType.equals("0")){
							 * Configuration.updateSourceCategory
							 * (jsonSource.getString("source_id"),
							 * s_rowid,true);
							 * AppUtil.sendSourceChanged(jsonSource
							 * .getString("source_id"
							 * ),jsonSource.getString("fbsj"),s_rowid); }
							 */
						}
					} else if (flag.equals("huozhu_deps")) {// 货主保证金

						String strStatus = actType.equals("0") ? "已发布" : "已派单";
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes(strStatus));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_" + actType + "_time"), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes(actType));
						// 更新缓存中货源的ID
						// System.out.println("containesKey("+rowid+")?"+Configuration.getSourcesMap().containsKey(rowid));
						org.json.JSONObject jsonSource = Configuration.getSoureFromCache(s_rowid);
						if (jsonSource != null) {
							jsonSource.put("yd_status", actType);
							Configuration.putSourceToCache(s_rowid, jsonSource);
							if (actType.equals("0")) {
								Configuration.updateSourceCategory(jsonSource.getString("source_id"), s_rowid, true);
								AppUtil.sendSourceChanged(jsonSource.getString("source_id"), jsonSource.getString("fbsj"), s_rowid);
							}
						}

					}
					utable.put(put1);
					utable.close();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}

			json.put("amount", amount);

			LogUtil.info(LogUtil.CAT_ACCOUNT, "账户明细更新成功.", "orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);
			json.put("success", "true");

			// 如果是货源发布，进行在线投保
			if (isZfbzj) {
				log.info("生成保单信息:" + sourceId);
				org.json.JSONObject s_row = XHelper.loadRow(sourceId, "yd_list");
				org.json.JSONObject bxObj = AppUtil.createBxxx(s_row);
				if (bxObj.getString("success").equals("true")) {
					json.put("bdxx", bxObj);
				}
			}

			// log.info("update result:"+XHelper.loadRow(null,"USER-"+cid,
			// "mobile_users", false,new
			// org.json.JSONObject("{\"data:balance\":\""+HBaseUtils.TYPE_NUMBER+"\"}")));
		} catch (Exception ex) {
			log.info("异常:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	public static double loadBalance(String cid) {
		double localBalance = 0;
		try {
			// 本账户余额
			org.json.JSONObject row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false, new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}"));
			if (row != null && row.has("balance"))
				localBalance = row.getDouble("balance");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return localBalance;
	}

	public static org.json.JSONObject requestOrder(String cid, String amount, String payType) {
		return requestOrder(cid, amount, payType, null);
	}

	public static org.json.JSONObject requestOrder(String cid, String amount, String payType, String uid) {
		if (conf == null)
			conf = AppHelper.getSystemConf();

		org.json.JSONObject json = new org.json.JSONObject();
		try {
			json.put("success", "false");
			if (Double.parseDouble(amount) == 0) { // 金额为0不处理
				return json;
			}

			java.util.Date now = new java.util.Date();
			String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
			// String timeid=org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS",
			// now);
			// String orderNo=timeid+"#PAY";
			String orderNo = getOrderNo(12);
			String orderName = "充值";

			if (cid == null || amount == null) {
				return json;
			}

			final String rowid = orderNo;

			json.put("order_no", orderNo);
			json.put("order_name", orderName);
			json.put("cid", cid);
			if (uid != null)
				json.put("uid", uid);
			json.put("order_time", strTime);
			org.json.JSONObject amt = new org.json.JSONObject();
			amt.put("type", HBaseUtils.TYPE_NUMBER);
			amt.put("value", amount);
			json.put("order_amount", amt);
			json.put("type", "PAY"); // 记录类型
			json.put("sfzf", "0"); // 是否支付
			json.put("pay_type", payType);

			// 创建订单
			if (payType.equals("wx")) {
				@SuppressWarnings("unchecked")
				SortedMap<String, String> map = new TreeMap();

				// 请求参数-begin
				map.put("service", "unified.trade.pay");
				map.put("version", "2.0");
				map.put("charset", "UTF-8");
				map.put("sign_type", "MD5");
				map.put("out_trade_no", orderNo);
				map.put("body", orderName);
				map.put("total_fee", (int) (Double.parseDouble(amount) * 100) + "");
				map.put("mch_create_ip", "127.0.0.1");

				// 微信支付

				map.put("sub_appid", cid.endsWith("-driver") ? DriverAppID : HuozhuAppID);
				map.put("limit_credit_pay", "1");
				// 请求参数-end

				map.put("mch_id", SwiftpassConfig.mch_id);
				map.put("notify_url", SwiftpassConfig.notify_url);
				map.put("nonce_str", String.valueOf(new Date().getTime()));

				Map<String, String> params = SignUtils.paraFilter(map);
				StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
				SignUtils.buildPayParams(buf, params, false);
				String preStr = buf.toString();
				String sign = MD5.sign(preStr, "&key=" + SwiftpassConfig.key, "utf-8");
				map.put("sign", sign);

				String reqUrl = SwiftpassConfig.req_url;
				// System.out.println("reqUrl:"+ reqUrl);

				System.out.println("javax.net.ssl.trustStore==" + System.getProperty("javax.net.ssl.trustStore"));
				System.getProperties().remove("javax.net.ssl.trustStore");

				// System.out.println("reqParams:" + XmlUtils.parseXML(map));
				CloseableHttpResponse response = null;
				CloseableHttpClient client = null;
				String res = null;
				try {
					HttpPost httpPost = new HttpPost(reqUrl);
					StringEntity entityParams = new StringEntity(XmlUtils.parseXML(map), "utf-8");
					httpPost.setEntity(entityParams);
					client = HttpClients.createDefault();
					response = client.execute(httpPost);
					if (response != null && response.getEntity() != null) {
						Map<String, String> resultMap = XmlUtils.toMap(EntityUtils.toByteArray(response.getEntity()), "utf-8");
						res = XmlUtils.toXml(resultMap);
						System.out.println("请求结果:" + res);
						if (resultMap.containsKey("sign")) {
							if (!SignUtils.checkParam(resultMap, SwiftpassConfig.key)) {
								res = "验证签名不通过";
							} else {
								// if("0".equals(resultMap.get("status")) &&
								// "0".equals(resultMap.get("result_code"))){
								if ("0".equals(resultMap.get("status"))) {
									String token_id = resultMap.get("token_id");
									String services = resultMap.get("services");
									json.put("success", "true");
									json.put("tokenid", token_id);
									json.put("services", services);
									LogUtil.debug(LogUtil.CAT_ACCOUNT, "充值申请", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
									XHelper.createRow("account_detail", rowid, json);
								} else {
									json.put("result", res);
								}
							}
						} else {
							json.put("result", "签名无效,支付请求被拒绝.");
						}
					} else {
						res = "操作失败";
						json.put("result", res);
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else if (payType.endsWith("zfb")) {
				String orderInfo = getZfbOrderInfo(orderName, "个人账户充值", amount, orderNo);
				// System.out.println("orderInfo="+orderInfo);
				String sign = sign(orderInfo);
				json.put("sign_type", getSignType());
				json.put("order_info", orderInfo);
				json.put("sign", sign);
				System.out.println("SING==" + sign);
				json.put("success", "true");
				json.put("tokenid", "" + System.currentTimeMillis());
				LogUtil.debug(LogUtil.CAT_ACCOUNT, "充值申请", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
				XHelper.createRow("account_detail", rowid, json);
			} else if (payType.endsWith("wy")) { // 网银支付
				json.put("success", "true");
				json.put("tokenid", "" + System.currentTimeMillis());
				LogUtil.debug(LogUtil.CAT_ACCOUNT, "网银充值申请", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
				XHelper.createRow("account_detail", rowid, json);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return json;
	}

	/**
	 * get the sign type we use. 获取签名方式
	 * 
	 */
	private static String getSignType() {
		return "sign_type=\"RSA\"";
	}

	/**
	 * sign the order info. 对订单信息进行签名
	 * 
	 * @param content
	 *            待签名订单信息
	 */
	private static String sign(String content) {
		// System.out.println("KEY=="+conf.get("RSA_PRIVATE"));
		return com.hyb.utils.zfb.SignUtils.sign(content, conf.get("RSA_PRIVATE"));
	}

	public static org.json.JSONObject getOrderInfoJson(String subject, String price) {
		if (conf != null)
			conf = AppHelper.getSystemConf();
		org.json.JSONObject o = new org.json.JSONObject();
		try {
			o.put("out_trade_no", getOrderNo());
			o.put("total_amount", price);
			o.put("subject", subject);
			o.put("seller_id", conf.get("SELLER"));
			o.put("product_code", "QUICK_WAP_PAY");
		} catch (Exception ex) {

		}
		return o;
	}

	private static String getZfbOrderInfo(String subject, String body, String price, String tradeNo) {

		// 签约合作者身份ID
		String orderInfo = "partner=" + "\"" + conf.get("PARTNER") + "\"";

		// 签约卖家支付宝账号
		orderInfo += "&seller_id=" + "\"" + conf.get("SELLER") + "\"";

		// 商户网站唯一订单号
		orderInfo += "&out_trade_no=" + "\"" + tradeNo + "\"";

		// 商品名称
		orderInfo += "&subject=" + "\"" + subject + "\"";

		// 商品详情
		orderInfo += "&body=" + "\"" + body + "\"";

		// 商品金额
		orderInfo += "&total_fee=" + "\"" + price + "\"";

		String serverIP = conf.containsKey("mq_server_remote") ? conf.get("mq_server_remote") : "122.114.76.38";
		// 服务器异步通知页面路径
		orderInfo += "&notify_url=" + "\"" + "https://" + serverIP + "/shared/zfb_notify.jsp" + "\"";

		// 服务接口名称， 固定值
		orderInfo += "&service=\"mobile.securitypay.pay\"";

		// 支付类型， 固定值
		orderInfo += "&payment_type=\"1\"";

		// 参数编码， 固定值
		orderInfo += "&_input_charset=\"utf-8\"";

		// 设置未付款交易的超时时间
		// 默认30分钟，一旦超时，该笔交易就会自动被关闭。
		// 取值范围：1m～15d。
		// m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
		// 该参数数值不接受小数点，如1.5h，可转换为90m。
		orderInfo += "&it_b_pay=\"30m\"";

		// extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
		// orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

		// 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
		orderInfo += "&return_url=\"m.alipay.com\"";

		return orderInfo;
	}

	/**
	 * 账户变动申请
	 * 
	 * @param cid
	 *            客户号
	 * @param amount
	 *            金额
	 * @param type
	 *            类别ID
	 * @param typeName
	 *            类别名称
	 * @return tokenid 和 order_no
	 */
	public static org.json.JSONObject requestAccount(String cid, String amount, String type, String typeName, String password, String ext) {
		if (type.equals(CASH) && cid.indexOf("driver") >= 0) {
			org.json.JSONObject json = new org.json.JSONObject();
			org.json.JSONObject json2 = new org.json.JSONObject();

			try {
				json2.put("type", type);
				json2.put("notify_type", "account");
				json2.put("success", "false");
				if (cid == null || amount == null) {
					return json2;
				}

				if (Double.parseDouble(amount) == 0 && !type.equals(DEPS)) { // 金额为0不处理
					return json2;
				}

				// 确认当前是否存在未处理的提现记录
				if (type.equals(CASH)) {
					try {
						org.json.JSONArray cols = new org.json.JSONArray();
						cols.put(AppHelper.createFilter("data:type", "CASH", "="));
						cols.put(AppHelper.createFilter("data:sfzf", "0", "="));
						cols.put(AppHelper.createFilter("data:cid", cid, "="));
						org.json.JSONArray rows = XHelper.listRowsByFilter("account_detail", "", cols);
						if (rows.length() > 0) {
							json2.put("message", "已存在尚未处理的提现记录!");
							return json2;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}

				String acctpwd = "";
				org.json.JSONObject row = null;
				row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false);
				acctpwd = row.has("acct_pwd") ? row.getString("acct_pwd") : "-";
				if (password == null || !MD5Tools.createMessageDigest(password).equals(acctpwd)) {
					json2.put("message", "支付密码错误，请确认是否设置支付密码并输入无误!");
					return json2;
				}
				java.util.Date now = new java.util.Date();
				String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
				String orderName = typeName;
				json.put("order_name", orderName);
				json.put("cid", cid);
				json.put("order_time", strTime);
				json.put("type", type); // 记录类型
				json.put("sfzf", "0"); // 是否支付
				json.put("ext", ext);
				double allmoney = Double.parseDouble(amount);
				if (row.has("underbalance")) {
					String underbalance = row.getString("underbalance");
					JSONObject underbalance_json = new JSONObject(underbalance);
					JSONObject underbalance_json_then = underbalance_json;
					Iterator<String> sIterator = underbalance_json.keys();
					JSONObject j_balance = new JSONObject();
					while (sIterator.hasNext()) {

						String key = sIterator.next();
						System.out.println(underbalance_json.toString());
						double value = underbalance_json.getJSONObject(key).getDouble("balance");
						if (value <= 0) {
							continue;
						}
						if (allmoney > value) {
							double value_then = 0;
							allmoney = ComputingTool.bigDecimalAddition(allmoney, -value);
							allmoney = Math.ceil(allmoney * 100.0) / 100.0;
							underbalance_json_then.put(key, j_balance.put("balance", value_then));
							org.json.JSONObject amt = new org.json.JSONObject();
							amt.put("type", HBaseUtils.TYPE_NUMBER);
							amt.put("value", value);
							json.put("order_amount", amt);
							json.put("from_dept", key);
							String orderNo = getOrderNo();
							final String rowid = orderNo;
							json.put("order_no", orderNo);
							json.put("tokenid", org.mbc.util.MD5Tools.createMessageDigest(orderNo + orderName));
							XHelper.createRow("account_detail", rowid, json);
						} else {
							double value_then = ComputingTool.bigDecimalAddition(value, -allmoney);

							value_then = Math.ceil(value_then * 100.0) / 100.0;
							underbalance_json_then.put(key, j_balance.put("balance", value_then));
							org.json.JSONObject amt = new org.json.JSONObject();
							amt.put("type", HBaseUtils.TYPE_NUMBER);
							amt.put("value", allmoney);
							json.put("order_amount", amt);
							json.put("from_dept", key);
							String orderNo = getOrderNo();
							final String rowid = orderNo;
							json.put("order_no", orderNo);
							json.put("tokenid", org.mbc.util.MD5Tools.createMessageDigest(orderNo + orderName));
							XHelper.createRow("account_detail", rowid, json);
							allmoney = 0;
						}

					}
					JSONObject json_then = new JSONObject();
					json_then.put("underbalance", underbalance_json_then.toString());
					XHelper.createRow("mobile_users", "USER-" + cid, json_then);
					if (allmoney > 0) {
						org.json.JSONObject amt = new org.json.JSONObject();
						amt.put("type", HBaseUtils.TYPE_NUMBER);
						amt.put("value", allmoney);
						json.put("order_amount", amt);
						json.put("from_dept", "hykc");
						String orderNo = getOrderNo();
						final String rowid = orderNo;
						json.put("order_no", orderNo);
						json.put("tokenid", org.mbc.util.MD5Tools.createMessageDigest(orderNo + orderName));
						XHelper.createRow("account_detail", rowid, json);
					}
				} else {
					org.json.JSONObject amt = new org.json.JSONObject();
					amt.put("type", HBaseUtils.TYPE_NUMBER);
					amt.put("value", amount);
					json.put("order_amount", amt);
					json.put("from_dept", "hykc");
					String orderNo = getOrderNo();
					final String rowid = orderNo;
					json.put("order_no", orderNo);
					json.put("tokenid", org.mbc.util.MD5Tools.createMessageDigest(orderNo + orderName));
					XHelper.createRow("account_detail", rowid, json);
				}
				json2.put("success", "true");

				return json2;
			} catch (Exception ex) {
				ex.printStackTrace();
				return json2;
			}

		} else {
			org.json.JSONObject json = new org.json.JSONObject();
			try {
				json.put("success", "false");
				if (cid == null || amount == null) {
					return json;
				}

				if (Double.parseDouble(amount) == 0 && !type.equals(DEPS)) { // 金额为0不处理
					return json;
				}

				// 确认当前是否存在未处理的提现记录
				if (type.equals(CASH)) {
					try {
						org.json.JSONArray cols = new org.json.JSONArray();
						cols.put(AppHelper.createFilter("data:type", "CASH", "="));
						cols.put(AppHelper.createFilter("data:sfzf", "0", "="));
						cols.put(AppHelper.createFilter("data:cid", cid, "="));
						org.json.JSONArray rows = XHelper.listRowsByFilter("account_detail", "", cols);
						if (rows.length() > 0) {
							json.put("message", "已存在尚未处理的提现记录!");
							return json;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}

				// 处理保证金支付
				if (type.equals(DEPS) && ext != null && ext.trim().length() > 0) {
					try {
						org.json.JSONObject o = new org.json.JSONObject(ext);
						String rowid = o.getString("rowid");
						String flag = o.getString("flag");
						org.json.JSONObject row = XHelper.loadRow(null, rowid, "yd_list", true);
						if (row.has("data:" + flag) && row.getString("data:" + flag).trim().length() > 0) {
							json.put("message", "保证金已支付!");
							return json;
						}
					} catch (Exception ex) {
						// ex.printStackTrace();
					}
				}

				String acctpwd = "";
				org.json.JSONObject row = null;
				String of_dept = "";
				if (cid.startsWith("JGXX")) {
					row = XHelper.loadRow(null, cid, "syscfg", false);
					of_dept = row.getString("parent_dept");
				} else {
					row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false);
					of_dept = "hykc";

				}
				acctpwd = row.has("acct_pwd") ? row.getString("acct_pwd") : "-";
				if (password == null || !MD5Tools.createMessageDigest(password).equals(acctpwd)) {
					json.put("message", "支付密码错误，请确认是否设置支付密码并输入无误!");
					return json;
				}

				java.util.Date now = new java.util.Date();
				String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
				String orderNo = getOrderNo();
				String orderName = typeName;
				final String rowid = orderNo;

				json.put("order_no", orderNo);
				json.put("order_name", orderName);
				json.put("cid", cid);
				json.put("order_time", strTime);
				org.json.JSONObject amt = new org.json.JSONObject();
				amt.put("type", HBaseUtils.TYPE_NUMBER);
				amt.put("value", amount);
				json.put("from_dept", of_dept);
				json.put("order_amount", amt);
				json.put("type", type); // 记录类型
				json.put("sfzf", "0"); // 是否支付
				json.put("success", "true");
				json.put("tokenid", org.mbc.util.MD5Tools.createMessageDigest(orderNo + orderName));
				json.put("ext", ext);
				XHelper.createRow("account_detail", rowid, json);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return json;

		}
	}

	/**
	 * @param connection
	 * @param acctType
	 *            账户类型: total:总账户, temp:保证金账户,income:收益账户,mobile:用户个人账户
	 * @param acctId
	 *            操作对象 账号
	 * @param transType
	 *            交易类型
	 * @param typeName
	 *            交易类型名称
	 * @param amount
	 *            金额
	 * @param fromAccount
	 *            来源于哪个账号
	 * @param srcId
	 *            账单rowid
	 * @return
	 * @throws Exception
	 */
	private static boolean addToAccount(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId) throws Exception {
		System.out.println("**********************************************************************");
		System.out.println("**操作对象账号acctId=" + acctId);
		System.out.println("**交易类型transType=" + transType);
		System.out.println("**交易类型名称typeName=" + typeName);
		System.out.println("**交易金额amount=" + amount);
		System.out.println("**交易来源fromAccount=" + fromAccount);
		System.out.println("**账单srcId=" + srcId);
		System.out.println("**********************************************************************");
		boolean bRet = false;

		double balance = 0;
		double result = 0;
		org.json.JSONObject row = null;
		// 更新指定余额
		org.json.JSONObject argsObj = new org.json.JSONObject();
		argsObj.put("data:balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:tmp_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:income_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:yfk", HBaseUtils.TYPE_NUMBER);
		org.json.JSONObject col1 = new org.json.JSONObject();
		if (acctType.equals("temp") || acctType.equals("income") || acctType.equals("total")) {
			row = XHelper.loadRow(null, "JGXX-" + acctId, "syscfg", false, argsObj);
			//System.out.println("row====" + row.toString());
			if (acctType.equals("temp")) {
				if (row.has("tmp_balance"))
					balance = row.getDouble("tmp_balance");
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				col1.put("tmp_balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			} else if (acctType.equals("income")) {
				if (row.has("income_balance"))
					balance = row.getDouble("income_balance");
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				col1.put("income_balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			} else {
				if (row.has("balance"))
					balance = row.getDouble("balance");
				//System.out.println("balance====" + balance);
				result = ComputingTool.bigDecimalAddition(balance, amount);
				result = Math.ceil(result * 100.0) / 100.0;
				//System.out.println("result====" + result);
				col1.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));

			}

			// 创建明细
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

			bRet = XHelper.createRow(connection, "syscfg", "JGXX-" + acctId, col1);
			//System.out.println("acctId=" + acctId + "***col1==" + col1);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

		} else {
			if (acctId.startsWith("JGXX")) {
				row = XHelper.loadRow(null, acctId, "syscfg", false, argsObj);
			} else {
				row = XHelper.loadRow(null, "USER-" + acctId, "mobile_users", false, argsObj);
			}

			if (row.has("balance"))
				balance = row.getDouble("balance");
			result = ComputingTool.bigDecimalAddition(balance, amount);
			result = Math.ceil(result * 100.0) / 100.0;
			col1.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));
			double new_balance = amount;
			if (typeName.equals("支付运费") && !fromAccount.equals("hykc")) {
				String str_underbalanceinfo = "";
				JSONObject jj1 = new JSONObject();
				jj1.put("balance", new_balance);
				JSONObject jj2 = new JSONObject();
				jj2.put("" + fromAccount, jj1);
				if (row.has("underbalance")) {
					str_underbalanceinfo = row.getString("underbalance");
					//System.out.println("司机各个子公司下信息" + str_underbalanceinfo);
					JSONObject json_underbalanceinfo = new JSONObject(str_underbalanceinfo);
					if (json_underbalanceinfo.has("" + fromAccount)) {
						JSONObject json_under_balance = json_underbalanceinfo.getJSONObject("" + fromAccount);
						double ba = json_under_balance.getDouble("balance");
						new_balance = ComputingTool.bigDecimalAddition(ba, amount);
						jj1.put("balance", new_balance);
						json_underbalanceinfo.put("" + fromAccount, jj1);
						col1.put("underbalance", json_underbalanceinfo.toString());
					} else {
						json_underbalanceinfo.put("" + fromAccount, jj1);
						col1.put("underbalance", json_underbalanceinfo.toString());
					}

				} else {
					col1.put("underbalance", jj2.toString());
				}

			}
			// 如果是垫付款充值，加上yfk的标记，就是这个账户一共欠了多少钱(yfk)。
			if (transType == DFCZ) {
				double yfk = 0;
				if (row.has("yfk")) {
					yfk = row.getDouble("yfk");
				}
				yfk = ComputingTool.bigDecimalAddition(yfk, amount);
				yfk = Math.ceil(yfk * 100.0) / 100.0;
				col1.put("yfk", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (yfk) + "\"}"));
			}

			// 创建明细
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			if (acctId.startsWith("JGXX")) {
				bRet = XHelper.createRow(connection, "syscfg", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			} else {
				bRet = XHelper.createRow(connection, "mobile_users", "USER-" + acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			}

		}
		return true;
	}

	/**
	 * -wdx 财务充值 加入了记录oper操作人的字段
	 * 
	 * @param acctType
	 *            账户类型: total:总账户, temp:保证金账户,income:收益账户,mobile:用户个人账户
	 * @param acctId
	 *            账号
	 * @param transType
	 * @param typeName
	 * @param amount
	 * @param oper
	 * @return
	 */

	private static boolean addToAccount2(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId, String oper,
			String hp_type,String reason) throws Exception {
		// 若为财务充值，需要加入经办人的名称
		// 创建明细
//		System.out.println("----------------------------acctType=" + acctType);
//		System.out.println("----------------------------acctId=" + acctId);
//		System.out.println("----------------------------transType=" + transType);
//		System.out.println("----------------------------typeName=" + typeName);
//		System.out.println("----------------------------amount=" + amount);
//		System.out.println("----------------------------fromAccount=" + fromAccount);
//		System.out.println("----------------------------srcId=" + srcId);
//		System.out.println("----------------------------oper=" + oper);
//		System.out.println("----------------------------reason=" + reason);
		JSONObject jbj = new JSONObject();
		jbj.put("jsr", oper);
		if (transType == HPCZ || transType == HPHK) {
			jbj.put("hpm", reason);
			jbj.put("hp_type", hp_type);
		}
		boolean bRet = false;
		double balance = 0;
		double result = 0;
		org.json.JSONObject row = null;
		// 更新指定余额
		org.json.JSONObject argsObj = new org.json.JSONObject();
		argsObj.put("data:balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:tmp_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:income_balance", HBaseUtils.TYPE_NUMBER);
		argsObj.put("data:yfk", HBaseUtils.TYPE_NUMBER);
		org.json.JSONObject col1 = new org.json.JSONObject();
		if (acctType.equals("total")) {
			row = XHelper.loadRow(null, "JGXX-" + acctId, "syscfg", false, argsObj);
			if (row.has("balance"))
				balance = row.getDouble("balance");
			if (transType == HPHK || transType == XJHK) {
				result = balance;
			} else {
				result = ComputingTool.bigDecimalAddition(balance, amount);
				// result = balance + amount;
			}
			result = Math.ceil(result * 100.0) / 100.0;
			col1.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));

			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId, jbj);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

			bRet = XHelper.createRow(connection, "syscfg", "JGXX-" + acctId, col1);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

		} else {
			if (acctId.startsWith("JGXX")) {
				row = XHelper.loadRow(null, acctId, "syscfg", false, argsObj);
			} else {
				row = XHelper.loadRow(null, "USER-" + acctId, "mobile_users", false, argsObj);
			}

			if (row.has("balance")) {
				balance = row.getDouble("balance");
			}

			if (transType == HPHK || transType == XJHK) {
				result = balance;
			} else {
				result = ComputingTool.bigDecimalAddition(balance, amount);
			}
			result = Math.ceil(result * 100.0) / 100.0;
			col1.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (result) + "\"}"));

			// 如果是垫付款充值，加上yfk的标记，就是这个账户一共欠了多少钱(yfk)。
			if (transType == DFCZ || transType == HPHK || transType == XJHK) {
				if (transType == HPHK || transType == XJHK) {
					amount = -(amount);
				}
				double yfk = 0;
				if (row.has("yfk")) {
					yfk = row.getDouble("yfk");
				}
				yfk = ComputingTool.bigDecimalAddition(yfk, amount);
				yfk = Math.ceil(yfk * 100.0) / 100.0;
				col1.put("yfk", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (yfk) + "\"}"));
			}

			// 创建明细
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId, jbj);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			if (acctId.startsWith("JGXX")) {
				bRet = XHelper.createRow(connection, "syscfg", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			} else {
				bRet = XHelper.createRow(connection, "mobile_users", "USER-" + acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "交易明细创建失败.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			}

		}
		
		return true;
	}

	private static boolean createAccountDetail(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount0, double balance, double rest,
			String fromAccount, String srcId) throws Exception {
		return createAccountDetail(connection, acctType, acctId, transType, typeName, amount0, balance, rest, fromAccount, srcId, null);
	}

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
		org.json.JSONObject amt = new org.json.JSONObject();
		amt.put("type", HBaseUtils.TYPE_NUMBER);
		amt.put("value", amount);
		json.put("order_amount", amt);
		json.put("type", transType); // 记录类型
		json.put("sfzf", "1"); // 是否支付
		json.put("from_account", fromAccount); // 对方账户
		json.put("source_id", srcId); // 货源ID

		json.put("submit_time", org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now));
		json.put("change_amount", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + amount + "\"}"));
		json.put("balance", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + balance + "\"}"));
		json.put("balance_rest", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + rest + "\"}"));

		if (ext != null) {
			@SuppressWarnings("unchecked")
			java.util.Iterator<String> keys = ext.keys();
			while (keys.hasNext()) {
				String k = keys.next();
				String val = ext.getString(k);
				json.put(k, val);
			}
		}

		boolean bRet = XHelper.createRow(connection, "account_detail", rowid, json);
		if (!bRet) {
			json.put("message", "addAccountDetail账户更新失败(1)");
			LogUtil.warn(LogUtil.CAT_ACCOUNT, "账户明细更新失败.", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + acctId);
			return false;
		}
		//金蝶账单同步，通过判断acctType==mobile
		if(acctType.equals("mobile")){
			new Thread(new Runnable() {
				@Override
				public void run() {
					org.json.JSONObject argDesc = new org.json.JSONObject();
					try {
						argDesc = new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}");
						argDesc.put("data:change_amount", HBaseUtils.TYPE_NUMBER);
						argDesc.put("data:balance_rest", HBaseUtils.TYPE_NUMBER);
						argDesc.put("data:balance", HBaseUtils.TYPE_NUMBER);
						argDesc.put("data:order_amount", HBaseUtils.TYPE_NUMBER);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					JSONObject json_jindie=com.xframe.utils.XHelper.loadRow(null,rowid, "account_detail",false,argDesc);
					SendToK2K k2k=new SendToK2K();
					k2k.send(json_jindie);	
				}
			}).start();
			
		}
		return true;
	}

	/**
	 * 取消运单
	 * 
	 * @param rowid
	 * @param hzId
	 * @param driverId
	 * @param hzCancel
	 *            货主取消
	 * @return
	 */
	public static org.json.JSONObject cancelTrans(String rowid, String hzId, String driverId, boolean hzCancel, int ydStatus) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			// 获取运单信息
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			if (row.has("data:trans_ok")) {
				json.put("message", "交易已完成。");
				return json;
			}

			if (row.has("data:canceled") && row.getString("data:canceled").equals("1")) {
				json.put("message", "运单已撤销。");
				return json;
			}

			System.out.println("撤销:" + rowid + ",hzId=" + hzId + ",driver:" + driverId + ",hzCancel:" + hzCancel + ",ydStatus=" + ydStatus);

			double yf = Double.parseDouble(row.getString("data:yf")); // 运费
			double driver_yjJe = row.has("data:yj_driver_je") ? row.getDouble("data:yj_driver_je") : 0.0; // 货主保证金金额

			json.put("driver", driverId);
			json.put("huozhu", hzId);
			String statusNew = "0";
			String statusNewName = "未接单";
			double wyj_amount = 0;
			if (ydStatus < 1) { // 运单状态为未接单或者司机未支付保证金
				if (hzCancel) { // 货主取消订单
					// 扣除货主违约金
					double wyj = Configuration.WYJ_HUOZHU * yf;
					wyj = Math.ceil(wyj * 100.0) / 100.0;
					String partent_dept = Configuration.adminAccount;
					if (hzId.indexOf("JGXX") == 0) {
						JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
						partent_dept = json_cid_info.getString("parent_dept");
					}
					System.out.println("1撤单子partent_dept机构号=" + partent_dept);
					// 运费剩余部分退还给货主
					addToAccount(connection, "temp", partent_dept, FEER, "运费退还", -yf, hzId, rowid);
					addToAccount(connection, "mobile", hzId, FEER, "运费退还", yf, partent_dept, rowid);
					AppUtil.updateUserCancelTotals(connection, hzId, 1, true);
					// 更新运单状态为9 运单取消
					statusNew = "9";
					statusNewName = "已撤销";
				} else {// 司机未支付取消运单
						// 更新运单状态为0 未接单
					if (row.has("data:tmp_cid")) {
						statusNew = "7";
						statusNewName = "待派单";
					} else {
						statusNew = "0";
						statusNewName = "未接单";
					}
					AppUtil.updateUserCancelTotals(connection, driverId, 1, true);
				}
			} else {
				if (hzCancel) { // 货主取消订单
						if(driverId!=null&&!(driverId.equals(""))&&!(driverId.equals("-"))){
							// 退还司机保证金金额
							addToAccount(connection, "temp", Configuration.adminAccount, DEPR, "保证金退还", -driver_yjJe, driverId, rowid);
							addToAccount(connection, "mobile", driverId, DEPR, "保证金退还", driver_yjJe, Configuration.adminAccount, rowid);

							// 支付司机赔偿金
							double sxfbl=0;
							sxfbl=Double.parseDouble(row.getString("data:sxfbl"));
							double pcj = Configuration.PCJ_DRIVER * yf * (1 - sxfbl);
							pcj = Math.ceil(pcj * 100.0) / 100.0;
							addToAccount(connection, "income", Configuration.adminAccount, PCJ, "违约赔偿金", -pcj, driverId, rowid);
							addToAccount(connection, "mobile", driverId, PCJ, "违约赔偿金", pcj, Configuration.adminAccount, rowid);
						}
						// 扣除货主违约金
						// 违约金包含平台违约金和司机赔偿金
						double wyj = Configuration.WYJ_HUOZHU * yf;
						wyj = Math.ceil(wyj * 100.0) / 100.0;

						wyj_amount = wyj;

						// 运费剩余部分退还给货主
						String partent_dept = Configuration.adminAccount;
						if (hzId.indexOf("JGXX") == 0) {
							JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
							partent_dept = json_cid_info.getString("parent_dept");
						}
						System.out.println("2撤单子partent_dept机构号=" + partent_dept);
						addToAccount(connection, "temp", partent_dept, FEER, "运费退还", -yf, hzId, rowid);
						addToAccount(connection, "mobile", hzId, FEER, "运费退还", yf, partent_dept, rowid);

						addToAccount(connection, "mobile", hzId, WYJ, "货主违约金收取", -wyj, partent_dept, rowid);
						addToAccount(connection, "income", partent_dept, WYJ, "货主违约金收取", wyj, hzId, rowid);

						AppUtil.updateUserCancelTotals(connection, hzId, 1, true);
						// 更新运单状态为9 运单取消
						statusNew = "9";
						statusNewName = "已撤销";
				} else {
					// 支付货主赔偿金
					String partent_dept = Configuration.adminAccount;
					double sxfbl=0;
					sxfbl=Double.parseDouble(row.getString("data:sxfbl"));
					if (hzId.indexOf("JGXX") == 0) {
						JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
						partent_dept = json_cid_info.getString("parent_dept");
					}

					double pcj = Configuration.PCJ_HUOZHU * yf * (1 - sxfbl);
					pcj = Math.ceil(pcj * 100.0) / 100.0;
				
					System.out.println("3撤单子partent_dept机构号=" + partent_dept);
					addToAccount(connection, "income", partent_dept, PCJ, "违约赔偿金", -pcj, hzId, rowid);
					addToAccount(connection, "mobile", hzId, PCJ, "违约赔偿金", pcj, partent_dept, rowid);

					// 扣除司机违约金
					double wyj = Configuration.WYJ_DRIVER * yf * (1 - sxfbl);
					wyj = Math.ceil(wyj * 100.0) / 100.0;
					wyj_amount = wyj;
					// double rest=driver_yjJe-wyj;
					addToAccount(connection, "mobile", driverId, WYJ, "司机违约金收取", -wyj, Configuration.adminAccount, rowid);
					addToAccount(connection, "income", Configuration.adminAccount, WYJ, "司机违约金收取", wyj, driverId, rowid);

					// 退还司机保证金
					addToAccount(connection, "temp", Configuration.adminAccount, FEER, "司机保证金退还", -driver_yjJe, driverId, rowid);
					addToAccount(connection, "mobile", driverId, FEER, "司机保证金退还", driver_yjJe, Configuration.adminAccount, rowid);

					AppUtil.updateUserCancelTotals(connection, driverId, 1, true);
					// 更新运单状态为0 未接单

					if (row.has("data:tmp_cid")) {
						statusNew = "7";
						statusNewName = "待派单";
					} else {
						statusNew = "0";
						statusNewName = "未接单";
					}
				}
			}

			log.info("撤销:" + rowid + ",newStatus=" + statusNew + ",newStausName=" + statusNewName + ",hzCancel:" + hzCancel + ",status=" + ydStatus);
			// 更新运单信息
			HTableInterface utable = connection.getTable("yd_list");
			String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date());

			Put put1 = new Put(Bytes.toBytes(rowid));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes(statusNew));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_driver"), Bytes.toBytes(""));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes(statusNewName));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_time"), Bytes.toBytes(strTime));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("canceled"), Bytes.toBytes("1"));
			utable.put(put1);

			// 删除tmp_cid,tmp_cph,tmp_owner
			log.info("删除临时派单信息..." + rowid);
			Delete delete = new Delete(Bytes.toBytes(rowid));
			if (!hzCancel) {
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("tmp_cid"));
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("tmp_cph"));
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("tmp_ip"));
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("tmp_owner"));
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("driver_deps"));
			} else {
				delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("huozhu_deps"));
			}
			delete.deleteColumns(Bytes.toBytes("data"), Bytes.toBytes("policyNo"));
			LogUtil.info(LogUtil.CAT_YD, "运单撤销.", "rowid:" + rowid + ",货主撤销:" + hzCancel);
			utable.delete(delete);
			utable.close();

			// 更新线路货源数并发布货源变化通知
			// java.util.concurrent.ConcurrentHashMap<String,
			// org.json.JSONObject>
			// sourceList=com.hyb.utils.Configuration.getSourcesMap();
			// org.json.JSONObject jsonSource=sourceList.get(rowid);
			org.json.JSONObject jsonSource = Configuration.getSoureFromCache(rowid);
			String sourceId = jsonSource.getString("source_id");
			jsonSource.put("yd_status", statusNew);
			if (statusNew.equals("0")) {
				com.hyb.utils.Configuration.updateSourceCategory(sourceId, rowid, true);
				com.hyb.utils.AppUtil.sendSourceChanged(sourceId, strTime, rowid);
			}else{
				com.hyb.utils.Configuration.removeSourceFromCache(rowid);//如果为0以外，说明彻底撤销了（货主撤销的）把redis里的也删掉。
			}
			json.put("success", "true");

			// 发送违约金扣除通知
			if (wyj_amount > 0) {
				org.json.JSONObject detail = new org.json.JSONObject();
				detail.put("rowid", rowid);
				detail.put("type", "WYJKC");
				if (hzCancel) {
					com.hyb.utils.AppUtil.sendNotify(hzId, "warn", "违约金扣除通知", "您的账户扣除违约金" + wyj_amount + "元", detail.toString());
				} else {
					com.hyb.utils.AppUtil.sendNotify(driverId, "warn", "违约金扣除通知", "您的账户扣除违约金" + wyj_amount + "元", detail.toString());
				}
			}
		} catch (Exception ex) {
			log.info("rowid:" + rowid + ",hzid=" + hzId + ",driverId=" + driverId);
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	public static org.json.JSONObject commitTrans(String rowid, String hzId, String driverId) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			// 获取运单信息
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			if (row.has("data:trans_ok")) {
				json.put("message", "交易已完成。");
				return json;
			}
			double sxfbl=0;//确认送达的时候，添加了一句 给运单增加手续费比例的字段，为了解决正式与测试不协调，正式中没有在创建运单的时候增加这个字段，所以到这里报错了。往后更新的话会再去掉 这个添加字段的妥协手段。
			sxfbl=Double.parseDouble(row.getString("data:sxfbl"));

			double yf = Double.parseDouble(row.getString("data:yf")); // 运费
			double bf = Double.parseDouble(row.has("data:bf") ? row.getString("data:bf") : "0");
			// String hzyjNo=row.getString("data:yj_huozh_no"); //货主保证金id;
			//System.out.println("ROW::" + row);
			double driver_yjJe = row.getDouble("data:yj_driver_je"); 
			double charge = Math.ceil(sxfbl * yf * 100.0) / 100.0; // 平台手续费
																							// 运费*手续费比例

			json.put("driver", driverId);
			json.put("huozhu", hzId);

			// 1.退还货主保证金 ==>保证金-运费 //货主账户无变动

			// 2。退还司机保证金 ==>保证金+运费-手续费
			// 退保证金
			/*
			 * boolean
			 * bRet=addAccountDetail(connection,DEPR,"保证金退还",driver_yjJe,
			 * driverId,true,driver_yjNo,false); if(!bRet){ json.put("message",
			 * "司机保证金退还失败(2)"); LogUtil.error(LogUtil.CAT_ACCOUNT, "司机保证金退还失败.",
			 * "orderNo:"+driver_yjNo+",amount:"+driver_yjJe+",cid:"+driverId);
			 * return json; }
			 */

			addToAccount(connection, "temp", Configuration.adminAccount, DEPR, "保证金退还", -driver_yjJe, driverId, rowid);
			addToAccount(connection, "mobile", driverId, DEPR, "保证金退还", driver_yjJe, Configuration.adminAccount, rowid);

			// 支付运费
			/*
			 * bRet=addAccountDetail(connection,FEEP,"支付运费",yf-charge,driverId,true
			 * ,null,false); if(!bRet){ json.put("message", "司机运费支付失败(3)");
			 * LogUtil.error(LogUtil.CAT_ACCOUNT, "司机运费支付失败.",
			 * "rowid:"+rowid+",amount:"+(yf-charge)+",cid:"+driverId); return
			 * json; }
			 */
			String partent_dept = Configuration.adminAccount;
			if (row.has("data:parent_dept")) {
				partent_dept = row.getString("data:parent_dept");
			}
			System.out.println("运单结束支付运费partent_dept机构号=" + partent_dept);
			addToAccount(connection, "temp", partent_dept, FEEP, "支付运费", -(yf - charge), driverId, rowid);
			addToAccount(connection, "mobile", driverId, FEEP, "支付运费", (yf - charge), partent_dept, rowid);

			// 金山巨象这类物流公司管理司机运费管理，在此处记账 Strat
			JSONObject yd_info = XHelper.loadRow(rowid, "yd_list");
			if (yd_info.has("pdwlgs")) {// 判断是否为pc派单。
				String pdwlgs = yd_info.getString("pdwlgs");
				if (!pdwlgs.equals("")) {
					JSONObject driver_info = XHelper.loadRow("USER-" + driverId, "mobile_users");
					if (driver_info.has("ofwlgsinfo")) {
						String ofwlgsinfo = driver_info.getString("ofwlgsinfo");
						JSONObject ofwlgsinfo_json = new JSONObject(ofwlgsinfo);
						if (ofwlgsinfo_json.has(pdwlgs)) {
							JSONObject info_json = new JSONObject();
							info_json = ofwlgsinfo_json.getJSONObject(pdwlgs);
							String balance_paidan = info_json.getString("tx_balance");
							Double double_banlance = Double.parseDouble(balance_paidan);
							double_banlance = ComputingTool.bigDecimalAddition(double_banlance, yf - charge);
							double_banlance = Math.ceil(double_banlance * 100.0) / 100.0;
							info_json.put("tx_balance", "" + double_banlance);
							ofwlgsinfo_json.put(pdwlgs, info_json);
							JSONObject newjsn = new JSONObject();
							newjsn.put("ofwlgsinfo", ofwlgsinfo_json.toString());
							XHelper.createRow("mobile_users", "USER-" + driverId, newjsn);

						}
					}
				}

			}
			// 金山巨象这类物流公司管理司机运费管理，在此处记账 end

			// 货主资金账户无变动

			// 增加保证金账户交易明细==>增加平台手续费收入
			/*
			 * bRet=addAccountDetail(connection,AccountUtil.CHARGE,"平台手续费收取",charge
			 * ,"JGXX"+Configuration.adminAccount,false,"",false,true);
			 * if(!bRet){ json.put("message", "货主司机双方保证金退还(5)");
			 * LogUtil.error(LogUtil.CAT_ACCOUNT, "货主司机双方保证金退还(5).",
			 * "rowid:"+rowid+",amount:"+charge+",cid:"+driverId); return json;
			 * }
			 */
			addToAccount(connection, "temp", partent_dept, CHARGE, "平台手续费收取", -charge, "income", rowid);
			addToAccount(connection, "income", partent_dept, CHARGE, "平台手续费收取", charge, "temp", rowid);

			addToAccount(connection, "temp", partent_dept, BX, "支付保险费", -bf, "income", rowid);
			addToAccount(connection, "income", partent_dept, BX, "收取保险费", bf, "temp", rowid);

			if (row.has("data:deptid") && row.has("data:userid"))
				hzId = row.getString("data:deptid") + "@" + row.getString("data:userid");
			// 双方交易数增加
			AppUtil.updateUserTransTotals(connection, hzId, 1, true);
			AppUtil.updateUserTransTotals(connection, driverId, 1, true);

			// 更新运单信息
			HTableInterface utable = connection.getTable("yd_list");
			Put put1 = new Put(Bytes.toBytes(rowid));
			// put1.add(Bytes.toBytes("data"),Bytes.toBytes("yd_status"),Bytes.toBytes("4"));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes("已完成"));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_" + 3 + "_time"), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes("4"));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("trans_ok"), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
			utable.put(put1);
			utable.close();

			json.put("success", "true");

			DataSender.addElecReceipt(rowid, driverId);
		} catch (Exception ex) {
			try {
				json.put("success", "false");
				json.put("message", "交易处理失败!");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	public static void addRequest(org.json.JSONObject o) {
		RedisClient.getInstance().addToList(ACCTLIST_NAME, o.toString());
		if (CoreServlet.zkMonitor.isMaster) {
			if (!AccountUtil.isBusy()) {
				AccountUtil.processRequest();
			}
		}
	}

	public static boolean isBusy() {
		return isProcessing;
	}

	public static boolean hasRequest() {
		return RedisClient.getInstance().getListSize(ACCTLIST_NAME) > 0;
	}

	public static void processRequest() {
		long reqSize = RedisClient.getInstance().getListSize(ACCTLIST_NAME);
		log.info("待处理请求数:" + reqSize);
		if (reqSize == 0)
			return;
		org.json.JSONObject o = null;
		try {
			String strReq = RedisClient.getInstance().popFromList(ACCTLIST_NAME);
			log.info("待处理请求:" + strReq);
			o = new org.json.JSONObject(strReq);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("处理账户请求:" + o);
		if (o == null)
			return;
		isProcessing = true;
		HConnection connection = null;
		try {
			String type = o.getString("type");
			String cid = o.getString("cid");
			String orderNo = o.has("order_no") ? o.getString("order_no") : "-";
			String tokenid = o.has("tokenid") ? o.getString("tokenid") : "";
			String rowid = o.has("rowid") ? o.getString("rowid") : "-";
			String userid = o.has("userid") ? o.getString("userid") : "";
			if (type.equals(PAY) || type.equals(CASH)) {
				org.json.JSONObject obj = updateAccount_1(cid, orderNo, tokenid);
				obj.put("tokenid", tokenid);
				obj.put("notify_type", "account");
				obj.put("type", type);

				if (obj.getString("success").equals("true")) {
					String mobiles = "";
					if (cid.endsWith("driver") || cid.endsWith("huozhu")) {
						mobiles = cid.substring(0, cid.indexOf("-"));
					}
					if (mobiles.length() > 0) {
						if (type.equals("PAY")) {
							String payType = "";
							if (obj.has("pay_type"))
								payType = obj.getString("pay_type");
							if (payType.equals("zfb"))
								payType = "支付宝";
							else if (payType.equals("wx")) {
								payType = "微信";
								// double amount=obj.getDouble("amount")/100;
								// obj.put("amount",
								// AppUtil.formatAmount(amount));
							} else
								payType = "其它方式";
							MySMSUtil.sendPaySms(payType, obj.getString("amount"), obj.getString("balance"), mobiles);
						} else {
							MySMSUtil.sendCashSms(obj.getString("amount"), obj.getString("balance"), mobiles);
						}
					}

				}

				String title = type.equals(PAY) ? "账户充值通知" : "账户提现通知";
				AppUtil.sendNotify(userid.length() == 0 ? cid : userid, LogUtil.INFO, "提示", title, obj.toString());
			} else if (type.equals("FINISH")) {
				System.out.println("------------------------FINISH----------------------------------");
				String huozhu = o.getString("huozhu");
				String driver = o.getString("driver");
				System.out.println("------------------------" + huozhu + "----------------------------------");
				System.out.println("------------------------" + driver + "----------------------------------");
				System.out.println("------------------------" + rowid + "----------------------------------");
				org.json.JSONObject obj = commitTrans(rowid, huozhu, driver);
				obj.put("notify_type", "account");
				obj.put("type", type);
				obj.put("rowid", rowid);
				if (obj.getString("success").equals("true")) {
					AppUtil.sendNotify(userid.length() == 0 ? huozhu : userid, LogUtil.INFO, "提示", "交易已完成.", obj.toString());
					AppUtil.sendNotify(driver, LogUtil.INFO, "提示", "交易已完成.", obj.toString());
					AppUtil.sendBroadcast("add", obj.toString());
				}
			} else if (type.equals(DEPS) || type.equals(DEPR)) {
				connection = HBaseUtils.getHConnection();
				org.json.JSONObject obj = updateAccount_2(cid, orderNo, tokenid);
				obj.put("tokenid", tokenid);
				obj.put("notify_type", "account");
				obj.put("type", type);

				String title = type.equals(DEPS) ? "保证金支付通知" : "保证金退还通知";
				if (cid.endsWith("huozhu"))
					title = type.equals(DEPS) ? "运费支付通知" : "运费退还通知";
				AppUtil.sendNotify(userid.length() == 0 ? cid : userid, LogUtil.INFO, "提示", title, obj.toString());

				// CacheService.getInstance().putInCache(tokenid, obj);
				RedisClient.getInstance().setStr(tokenid, obj.toString());
				// 处理完成,设置完成标志
			} else if (type.equals("CANCEL")) {
				String huozhu = o.getString("huozhu");
				String driver = o.has("driver") ? o.getString("driver") : "";
				String strStatus = o.getString("yd_status");
				boolean hzCancel = o.has("hz_cancel") && o.getString("hz_cancel").equals("true");
				org.json.JSONObject obj = cancelTrans(rowid, huozhu, driver, hzCancel, Integer.parseInt(strStatus));
				obj.put("notify_type", "account");
				obj.put("type", type);
				obj.put("rowid", rowid);
				AppUtil.sendNotify(userid.length() == 0 ? huozhu : userid, LogUtil.ERROR, "提示", "运单已撤销.", obj.toString());
				if (driver.length() > 0)
					AppUtil.sendNotify(driver, LogUtil.ERROR, "提示", "运单已撤销.", obj.toString());
			}
			// reqList.remove(0);
		} catch (Exception ex) {
			// reqList.remove(0);
			ex.printStackTrace();
		} finally {
			isProcessing = false;
			if (connection != null)
				HBaseUtils.closeConnection(connection);
		}

	}
}
