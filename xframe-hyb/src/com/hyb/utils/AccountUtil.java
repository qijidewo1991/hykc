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
	public static final String PAY = "PAY";// ��ֵ
	/**
	 * �渶��ֵ
	 */
	public static final String DFCZ = "DFCZ";
	/**
	 * ��Ʊ��ֵ
	 */
	public static final String HPCZ = "HPCZ";
	/**
	 * �ֽ�ؿ�
	 */
	public static final String XJHK = "XJHK";
	/**
	 * ��Ʊ�ؿ�
	 */
	public static final String HPHK = "HPHK";
	public static final String CASH = "CASH";// :����,
	public static final String DEPS = "DEPS";// :��֤��֧��,
	public static final String DEPR = "DEPR";// :��֤���˻�,
	public static final String FEEP = "FEEPF";// :����֧��,
	public static final String FEER = "FEER";// :������ȡ
	public static final String CHARGE = "CHARGE";// :��������ȡ
	public static final String BX = "BX";// ����
	public static final String WYJ = "WYJ";// ΥԼ����ȡ
	public static final String PCJ = "PCJ";// ΥԼ�⳥��

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
	 * �յ�֧����ɺ����֧�����
	 * 
	 * @param orgOrderNo
	 *            ������
	 * @param time
	 *            ����ʱ��
	 * @param tradeNo
	 *            ���׺�
	 * @param buyer_id
	 *            ������ID
	 * @param amount
	 *            ���
	 * @param payFrom
	 *            ������Դ
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
				json.put("message", "����������!");
				return;
			}

			if (row.has("trade_no")) {
				log.info("�ظ���������:" + orgOrderNo + ",time=" + time + ",amount:" + amount + ",from:" + payFrom);
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

			LogUtil.info(LogUtil.CAT_ACCOUNT, orderName + "����ɹ�.", "type:" + rowType + ",orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);

			// ����
			updateAccount_1(cid, orgOrderNo, tokenid);

			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("success", "true");
			obj.put("tokenid", tokenid);
			obj.put("notify_type", "account");
			obj.put("pay", "1");
			obj.put("type", rowType);
			obj.put("amount", amount);
			String title = "��ͨ��" + payFrom + "�ɹ�֧��" + amount + "Ԫ.";
			AppUtil.sendNotify(row.has("uid") ? row.getString("uid") : cid, LogUtil.INFO, "��ʾ", title, obj.toString());

			String mobiles = "";
			if (cid.endsWith("driver") || cid.endsWith("huozhu")) {
				mobiles = cid.substring(0, cid.indexOf("-"));
				double balance = loadBalance(cid);
				MySMSUtil.sendPaySms(payFrom, amount, String.valueOf(balance), mobiles);
			}

		} catch (Exception ex) {
			log.info("�쳣:" + ex.toString());
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
	 * ��ֵ�����˻�����
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
				json.put("message", "����������!");
				return json;
			}

			// log.info("\trow=="+row);
			if (!row.has("tokenid") || !row.getString("tokenid").equals(tokenid)) {
				json.put("message", "������Ч!");
				return json;
			}

			String rowType = row.getString("type");

			double amount = row.getDouble("order_amount");
			String partent_dept = Configuration.adminAccount;
			if (cid.indexOf("JGXX") == 0) {
				JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(cid, "syscfg");
				partent_dept = json_cid_info.getString("parent_dept");
			}
			System.out.println("��ֵpartent_dept������=" + partent_dept);
			if (rowType.endsWith(PAY)) {
				addToAccount(connection, "total", partent_dept, PAY, "��ֵ", amount, cid, rowid);
				addToAccount(connection, "mobile", cid, PAY, "��ֵ", amount, partent_dept, rowid);
			} else if (rowType.endsWith(CASH)) {
				// �����
				if (cid.indexOf("driver") >= 0) {
					String from_dep = row.getString("from_dept");
					addToAccount(connection, "total", from_dep, CASH, "����", -amount, cid, rowid);
					addToAccount(connection, "mobile", cid, CASH, "����", -amount, from_dep, rowid);
				} else {
					addToAccount(connection, "total", partent_dept, CASH, "����", -amount, cid, rowid);
					addToAccount(connection, "mobile", cid, CASH, "����", -amount, partent_dept, rowid);
				}

			}
			json.put("amount", amount);

			if (row.has("pay_type"))
				json.put("pay_type", row.getString("pay_type"));

			// ���˻����
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

			LogUtil.info(LogUtil.CAT_ACCOUNT, "�˻���ϸ���³ɹ�.", "orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);
			json.put("success", "true");
			// log.info("update result:"+XHelper.loadRow(null,"USER-"+cid,
			// "mobile_users", false,new
			// org.json.JSONObject("{\"data:balance\":\""+HBaseUtils.TYPE_NUMBER+"\"}")));

		} catch (Exception ex) {
			log.info("�쳣:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	/**
	 * 2017��10��12��15:42:47 wdx �����ֵ
	 * 
	 * @param cid
	 * @param amount
	 *            ���
	 * @param reason
	 *            ��ע
	 * @param acctName
	 *            ������˾����
	 * @param hp_type
	 *            ��Ʊ����   ���гжһ�Ʊ��yinhang��  ��ҵ�жһ�Ʊ��shangye��
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
			System.out.println("�����ֵ" + cid + "*****type=" + type);
			// �渶��ֵ
			if (type.equals("dfcz")) {
				System.out.println("�渶��ֵ");
				addToAccount2(connection, "total", of_dept, DFCZ, "�渶��ֵ", amount, cid, rowid, oper,"", "");
				addToAccount2(connection, "mobile", cid, DFCZ, "�渶��ֵ", amount, of_dept, rowid, oper,"", "");

			}

			// ��Ʊ��ֵ
			if (type.equals("hpcz")) {
				System.out.println("��Ʊ��ֵ");
				addToAccount2(connection, "total", of_dept, HPCZ, "��Ʊ��ֵ", amount, cid, rowid, oper,hp_type, reason);
				addToAccount2(connection, "mobile", cid, HPCZ, "��Ʊ��ֵ", amount, of_dept, rowid, oper, hp_type,reason);
			}

			// �յ渶���ֽ�
			if (type.equals("sdfxj")) {
				System.out.println("�յ渶���ֽ�");
				addToAccount2(connection, "total", of_dept, XJHK, "�ֽ𻹿�", amount, cid, rowid, oper,"", "");
				addToAccount2(connection, "mobile", cid, XJHK, "�ֽ𻹿�", amount, of_dept, rowid, oper,"", "");
			}

			// �յ渶����Ʊ
			if (type.equals("sdfhp")) {
				System.out.println("�յ渶����Ʊ");
				addToAccount2(connection, "total", of_dept, HPHK, "��Ʊ����", amount, cid, rowid, oper,hp_type, reason);
				addToAccount2(connection, "mobile", cid, HPHK, "��Ʊ����", amount, of_dept, rowid, oper,hp_type, reason);
			}
			json.put("amount", amount);
			json.put("success", "true");

		} catch (Exception ex) {
			try {
				json.put("success", "false");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			log.info("�쳣:" + ex.toString());
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
			addToAccount(connection, "total", Configuration.adminAccount, PAY, "�˻�����", amount, cid, rowid);
			addToAccount(connection, "mobile", cid, PAY, "�˻�����", amount, Configuration.adminAccount, rowid);
			json.put("amount", amount);

			org.json.JSONObject argsObj = new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}");
			if (cid.startsWith("JGXX")) {
				row = XHelper.loadRow(null, cid, "syscfg", false, argsObj);
			} else {
				row = XHelper.loadRow(null, "USER-" + cid, "mobile_users", false, argsObj);
			}
			if (row.has("balance"))
				json.put("balance", row.getDouble("balance"));

			LogUtil.warn(LogUtil.CAT_MAINTAIN, "�˻����� " + cid + "(" + acctName + ")", "ԭ��:" + reason + ",���:" + amount + ",�������:" + row.getDouble("balance"), oper);
			json.put("success", "true");

		} catch (Exception ex) {
			log.info("�쳣:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	/**
	 * ƽ̨��֤���˻��͸����˻����ʽ�����
	 * 
	 * @param cid
	 * @param orderNo
	 * @param tokenid
	 * @return
	 */
	private static org.json.JSONObject updateAccount_2(String cid, String orgOrderNo, String tokenid) {
		org.json.JSONObject json = new org.json.JSONObject();
		boolean isZfbzj = false;
		String sourceId = null;// ��ԴID;
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			String rowid = orgOrderNo;
			org.json.JSONObject row = XHelper.loadRow(null, rowid, "account_detail", false, new org.json.JSONObject("{\"data:order_amount\":\"" + HBaseUtils.TYPE_NUMBER + "\"}"));
			if (row == null) {
				json.put("message", "����������!");
				return json;
			}

			// log.info("\trow=="+row);
			if (!row.has("tokenid") || !row.getString("tokenid").equals(tokenid)) {
				json.put("message", "������Ч!");
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
					System.out.println("֧����֤��partent_dept=" + partent_dept);
					addToAccount(connection, "temp", partent_dept, DEPS, "��֤��֧��", amount, cid, json_ext.getString("rowid"));
					addToAccount(connection, "mobile", cid, DEPS, "��֤��֧��", -amount, partent_dept, json_ext.getString("rowid"));
				}
			} else if (rowType.endsWith(DEPR)) {
				String partent_dept = Configuration.adminAccount;
				if (cid.indexOf("JGXX") == 0) {
					JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(cid, "syscfg");
					partent_dept = json_cid_info.getString("parent_dept");
				}
				System.out.println("�˻���֤��partent_dept=" + partent_dept);
				addToAccount(connection, "temp", partent_dept, DEPR, "��֤���˻�", -amount, cid, json_ext.getString("rowid"));
				addToAccount(connection, "mobile", cid, DEPR, "��֤���˻�", amount, partent_dept, json_ext.getString("rowid"));
			}

			if (rowType.equals(DEPS) && row.has("ext") && row.getString("ext").trim().length() > 0) {
				try {
					String ext = row.getString("ext");
					org.json.JSONObject o = new org.json.JSONObject(ext);
					String s_rowid = o.getString("rowid");
					String flag = o.getString("flag");
					String actType = o.has("act_type") ? o.getString("act_type") : "0";

					// �����˵���Ϣ
					HTableInterface utable = connection.getTable("yd_list");
					Put put1 = new Put(Bytes.toBytes(s_rowid));
					put1.add(Bytes.toBytes("data"), Bytes.toBytes(flag), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
					if (flag.equals("driver_deps")) {// ˾��֧���걣֤�𣬱���˵�״̬
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
					} else if (flag.equals("huozhu_deps")) {// ������֤��

						String strStatus = actType.equals("0") ? "�ѷ���" : "���ɵ�";
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes(strStatus));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_" + actType + "_time"), Bytes.toBytes(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date())));
						put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes(actType));
						// ���»����л�Դ��ID
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

			LogUtil.info(LogUtil.CAT_ACCOUNT, "�˻���ϸ���³ɹ�.", "orderNo:" + orgOrderNo + ",amount:" + amount + ",cid:" + cid);
			json.put("success", "true");

			// ����ǻ�Դ��������������Ͷ��
			if (isZfbzj) {
				log.info("���ɱ�����Ϣ:" + sourceId);
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
			log.info("�쳣:" + ex.toString());
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
		return json;
	}

	public static double loadBalance(String cid) {
		double localBalance = 0;
		try {
			// ���˻����
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
			if (Double.parseDouble(amount) == 0) { // ���Ϊ0������
				return json;
			}

			java.util.Date now = new java.util.Date();
			String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
			// String timeid=org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS",
			// now);
			// String orderNo=timeid+"#PAY";
			String orderNo = getOrderNo(12);
			String orderName = "��ֵ";

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
			json.put("type", "PAY"); // ��¼����
			json.put("sfzf", "0"); // �Ƿ�֧��
			json.put("pay_type", payType);

			// ��������
			if (payType.equals("wx")) {
				@SuppressWarnings("unchecked")
				SortedMap<String, String> map = new TreeMap();

				// �������-begin
				map.put("service", "unified.trade.pay");
				map.put("version", "2.0");
				map.put("charset", "UTF-8");
				map.put("sign_type", "MD5");
				map.put("out_trade_no", orderNo);
				map.put("body", orderName);
				map.put("total_fee", (int) (Double.parseDouble(amount) * 100) + "");
				map.put("mch_create_ip", "127.0.0.1");

				// ΢��֧��

				map.put("sub_appid", cid.endsWith("-driver") ? DriverAppID : HuozhuAppID);
				map.put("limit_credit_pay", "1");
				// �������-end

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
						System.out.println("������:" + res);
						if (resultMap.containsKey("sign")) {
							if (!SignUtils.checkParam(resultMap, SwiftpassConfig.key)) {
								res = "��֤ǩ����ͨ��";
							} else {
								// if("0".equals(resultMap.get("status")) &&
								// "0".equals(resultMap.get("result_code"))){
								if ("0".equals(resultMap.get("status"))) {
									String token_id = resultMap.get("token_id");
									String services = resultMap.get("services");
									json.put("success", "true");
									json.put("tokenid", token_id);
									json.put("services", services);
									LogUtil.debug(LogUtil.CAT_ACCOUNT, "��ֵ����", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
									XHelper.createRow("account_detail", rowid, json);
								} else {
									json.put("result", res);
								}
							}
						} else {
							json.put("result", "ǩ����Ч,֧�����󱻾ܾ�.");
						}
					} else {
						res = "����ʧ��";
						json.put("result", res);
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else if (payType.endsWith("zfb")) {
				String orderInfo = getZfbOrderInfo(orderName, "�����˻���ֵ", amount, orderNo);
				// System.out.println("orderInfo="+orderInfo);
				String sign = sign(orderInfo);
				json.put("sign_type", getSignType());
				json.put("order_info", orderInfo);
				json.put("sign", sign);
				System.out.println("SING==" + sign);
				json.put("success", "true");
				json.put("tokenid", "" + System.currentTimeMillis());
				LogUtil.debug(LogUtil.CAT_ACCOUNT, "��ֵ����", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
				XHelper.createRow("account_detail", rowid, json);
			} else if (payType.endsWith("wy")) { // ����֧��
				json.put("success", "true");
				json.put("tokenid", "" + System.currentTimeMillis());
				LogUtil.debug(LogUtil.CAT_ACCOUNT, "������ֵ����", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + cid);
				XHelper.createRow("account_detail", rowid, json);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return json;
	}

	/**
	 * get the sign type we use. ��ȡǩ����ʽ
	 * 
	 */
	private static String getSignType() {
		return "sign_type=\"RSA\"";
	}

	/**
	 * sign the order info. �Զ�����Ϣ����ǩ��
	 * 
	 * @param content
	 *            ��ǩ��������Ϣ
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

		// ǩԼ���������ID
		String orderInfo = "partner=" + "\"" + conf.get("PARTNER") + "\"";

		// ǩԼ����֧�����˺�
		orderInfo += "&seller_id=" + "\"" + conf.get("SELLER") + "\"";

		// �̻���վΨһ������
		orderInfo += "&out_trade_no=" + "\"" + tradeNo + "\"";

		// ��Ʒ����
		orderInfo += "&subject=" + "\"" + subject + "\"";

		// ��Ʒ����
		orderInfo += "&body=" + "\"" + body + "\"";

		// ��Ʒ���
		orderInfo += "&total_fee=" + "\"" + price + "\"";

		String serverIP = conf.containsKey("mq_server_remote") ? conf.get("mq_server_remote") : "122.114.76.38";
		// �������첽֪ͨҳ��·��
		orderInfo += "&notify_url=" + "\"" + "https://" + serverIP + "/shared/zfb_notify.jsp" + "\"";

		// ����ӿ����ƣ� �̶�ֵ
		orderInfo += "&service=\"mobile.securitypay.pay\"";

		// ֧�����ͣ� �̶�ֵ
		orderInfo += "&payment_type=\"1\"";

		// �������룬 �̶�ֵ
		orderInfo += "&_input_charset=\"utf-8\"";

		// ����δ����׵ĳ�ʱʱ��
		// Ĭ��30���ӣ�һ����ʱ���ñʽ��׾ͻ��Զ����رա�
		// ȡֵ��Χ��1m��15d��
		// m-���ӣ�h-Сʱ��d-�죬1c-���죨���۽��׺�ʱ����������0��رգ���
		// �ò�����ֵ������С���㣬��1.5h����ת��Ϊ90m��
		orderInfo += "&it_b_pay=\"30m\"";

		// extern_tokenΪ���������Ȩ��ȡ����alipay_open_id,���ϴ˲����û���ʹ����Ȩ���˻�����֧��
		// orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

		// ֧��������������󣬵�ǰҳ����ת���̻�ָ��ҳ���·�����ɿ�
		orderInfo += "&return_url=\"m.alipay.com\"";

		return orderInfo;
	}

	/**
	 * �˻��䶯����
	 * 
	 * @param cid
	 *            �ͻ���
	 * @param amount
	 *            ���
	 * @param type
	 *            ���ID
	 * @param typeName
	 *            �������
	 * @return tokenid �� order_no
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

				if (Double.parseDouble(amount) == 0 && !type.equals(DEPS)) { // ���Ϊ0������
					return json2;
				}

				// ȷ�ϵ�ǰ�Ƿ����δ��������ּ�¼
				if (type.equals(CASH)) {
					try {
						org.json.JSONArray cols = new org.json.JSONArray();
						cols.put(AppHelper.createFilter("data:type", "CASH", "="));
						cols.put(AppHelper.createFilter("data:sfzf", "0", "="));
						cols.put(AppHelper.createFilter("data:cid", cid, "="));
						org.json.JSONArray rows = XHelper.listRowsByFilter("account_detail", "", cols);
						if (rows.length() > 0) {
							json2.put("message", "�Ѵ�����δ��������ּ�¼!");
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
					json2.put("message", "֧�����������ȷ���Ƿ�����֧�����벢��������!");
					return json2;
				}
				java.util.Date now = new java.util.Date();
				String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
				String orderName = typeName;
				json.put("order_name", orderName);
				json.put("cid", cid);
				json.put("order_time", strTime);
				json.put("type", type); // ��¼����
				json.put("sfzf", "0"); // �Ƿ�֧��
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

				if (Double.parseDouble(amount) == 0 && !type.equals(DEPS)) { // ���Ϊ0������
					return json;
				}

				// ȷ�ϵ�ǰ�Ƿ����δ��������ּ�¼
				if (type.equals(CASH)) {
					try {
						org.json.JSONArray cols = new org.json.JSONArray();
						cols.put(AppHelper.createFilter("data:type", "CASH", "="));
						cols.put(AppHelper.createFilter("data:sfzf", "0", "="));
						cols.put(AppHelper.createFilter("data:cid", cid, "="));
						org.json.JSONArray rows = XHelper.listRowsByFilter("account_detail", "", cols);
						if (rows.length() > 0) {
							json.put("message", "�Ѵ�����δ��������ּ�¼!");
							return json;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}

				// ����֤��֧��
				if (type.equals(DEPS) && ext != null && ext.trim().length() > 0) {
					try {
						org.json.JSONObject o = new org.json.JSONObject(ext);
						String rowid = o.getString("rowid");
						String flag = o.getString("flag");
						org.json.JSONObject row = XHelper.loadRow(null, rowid, "yd_list", true);
						if (row.has("data:" + flag) && row.getString("data:" + flag).trim().length() > 0) {
							json.put("message", "��֤����֧��!");
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
					json.put("message", "֧�����������ȷ���Ƿ�����֧�����벢��������!");
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
				json.put("type", type); // ��¼����
				json.put("sfzf", "0"); // �Ƿ�֧��
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
	 *            �˻�����: total:���˻�, temp:��֤���˻�,income:�����˻�,mobile:�û������˻�
	 * @param acctId
	 *            �������� �˺�
	 * @param transType
	 *            ��������
	 * @param typeName
	 *            ������������
	 * @param amount
	 *            ���
	 * @param fromAccount
	 *            ��Դ���ĸ��˺�
	 * @param srcId
	 *            �˵�rowid
	 * @return
	 * @throws Exception
	 */
	private static boolean addToAccount(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId) throws Exception {
		System.out.println("**********************************************************************");
		System.out.println("**���������˺�acctId=" + acctId);
		System.out.println("**��������transType=" + transType);
		System.out.println("**������������typeName=" + typeName);
		System.out.println("**���׽��amount=" + amount);
		System.out.println("**������ԴfromAccount=" + fromAccount);
		System.out.println("**�˵�srcId=" + srcId);
		System.out.println("**********************************************************************");
		boolean bRet = false;

		double balance = 0;
		double result = 0;
		org.json.JSONObject row = null;
		// ����ָ�����
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

			// ������ϸ
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

			bRet = XHelper.createRow(connection, "syscfg", "JGXX-" + acctId, col1);
			//System.out.println("acctId=" + acctId + "***col1==" + col1);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
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
			if (typeName.equals("֧���˷�") && !fromAccount.equals("hykc")) {
				String str_underbalanceinfo = "";
				JSONObject jj1 = new JSONObject();
				jj1.put("balance", new_balance);
				JSONObject jj2 = new JSONObject();
				jj2.put("" + fromAccount, jj1);
				if (row.has("underbalance")) {
					str_underbalanceinfo = row.getString("underbalance");
					//System.out.println("˾�������ӹ�˾����Ϣ" + str_underbalanceinfo);
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
			// ����ǵ渶���ֵ������yfk�ı�ǣ���������˻�һ��Ƿ�˶���Ǯ(yfk)��
			if (transType == DFCZ) {
				double yfk = 0;
				if (row.has("yfk")) {
					yfk = row.getDouble("yfk");
				}
				yfk = ComputingTool.bigDecimalAddition(yfk, amount);
				yfk = Math.ceil(yfk * 100.0) / 100.0;
				col1.put("yfk", new org.json.JSONObject("{\"type\":\"" + HBaseUtils.TYPE_NUMBER + "\",\"value\":\"" + (yfk) + "\"}"));
			}

			// ������ϸ
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			if (acctId.startsWith("JGXX")) {
				bRet = XHelper.createRow(connection, "syscfg", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			} else {
				bRet = XHelper.createRow(connection, "mobile_users", "USER-" + acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			}

		}
		return true;
	}

	/**
	 * -wdx �����ֵ �����˼�¼oper�����˵��ֶ�
	 * 
	 * @param acctType
	 *            �˻�����: total:���˻�, temp:��֤���˻�,income:�����˻�,mobile:�û������˻�
	 * @param acctId
	 *            �˺�
	 * @param transType
	 * @param typeName
	 * @param amount
	 * @param oper
	 * @return
	 */

	private static boolean addToAccount2(HConnection connection, String acctType, String acctId, String transType, String typeName, double amount, String fromAccount, String srcId, String oper,
			String hp_type,String reason) throws Exception {
		// ��Ϊ�����ֵ����Ҫ���뾭���˵�����
		// ������ϸ
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
		// ����ָ�����
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
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}

			bRet = XHelper.createRow(connection, "syscfg", "JGXX-" + acctId, col1);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
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

			// ����ǵ渶���ֵ������yfk�ı�ǣ���������˻�һ��Ƿ�˶���Ǯ(yfk)��
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

			// ������ϸ
			bRet = createAccountDetail(connection, acctType, acctId, transType, typeName, amount, balance, result, fromAccount, srcId, jbj);
			if (!bRet) {
				LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
				return false;
			}
			if (acctId.startsWith("JGXX")) {
				bRet = XHelper.createRow(connection, "syscfg", acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
					return false;
				}
			} else {
				bRet = XHelper.createRow(connection, "mobile_users", "USER-" + acctId, col1);
				if (!bRet) {
					LogUtil.warn(LogUtil.CAT_ACCOUNT, "������ϸ����ʧ��.", "acctType:" + acctType + ",acctId:" + acctId + ",amount:" + amount + ",transType:" + transType + ",typeName:" + typeName);
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
		json.put("type", transType); // ��¼����
		json.put("sfzf", "1"); // �Ƿ�֧��
		json.put("from_account", fromAccount); // �Է��˻�
		json.put("source_id", srcId); // ��ԴID

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
			json.put("message", "addAccountDetail�˻�����ʧ��(1)");
			LogUtil.warn(LogUtil.CAT_ACCOUNT, "�˻���ϸ����ʧ��.", "orderNo:" + orderNo + ",amount:" + amount + ",cid:" + acctId);
			return false;
		}
		//����˵�ͬ����ͨ���ж�acctType==mobile
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
	 * ȡ���˵�
	 * 
	 * @param rowid
	 * @param hzId
	 * @param driverId
	 * @param hzCancel
	 *            ����ȡ��
	 * @return
	 */
	public static org.json.JSONObject cancelTrans(String rowid, String hzId, String driverId, boolean hzCancel, int ydStatus) {
		org.json.JSONObject json = new org.json.JSONObject();
		HConnection connection = HBaseUtils.getHConnection();
		try {
			json.put("success", "false");
			// ��ȡ�˵���Ϣ
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			if (row.has("data:trans_ok")) {
				json.put("message", "��������ɡ�");
				return json;
			}

			if (row.has("data:canceled") && row.getString("data:canceled").equals("1")) {
				json.put("message", "�˵��ѳ�����");
				return json;
			}

			System.out.println("����:" + rowid + ",hzId=" + hzId + ",driver:" + driverId + ",hzCancel:" + hzCancel + ",ydStatus=" + ydStatus);

			double yf = Double.parseDouble(row.getString("data:yf")); // �˷�
			double driver_yjJe = row.has("data:yj_driver_je") ? row.getDouble("data:yj_driver_je") : 0.0; // ������֤����

			json.put("driver", driverId);
			json.put("huozhu", hzId);
			String statusNew = "0";
			String statusNewName = "δ�ӵ�";
			double wyj_amount = 0;
			if (ydStatus < 1) { // �˵�״̬Ϊδ�ӵ�����˾��δ֧����֤��
				if (hzCancel) { // ����ȡ������
					// �۳�����ΥԼ��
					double wyj = Configuration.WYJ_HUOZHU * yf;
					wyj = Math.ceil(wyj * 100.0) / 100.0;
					String partent_dept = Configuration.adminAccount;
					if (hzId.indexOf("JGXX") == 0) {
						JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
						partent_dept = json_cid_info.getString("parent_dept");
					}
					System.out.println("1������partent_dept������=" + partent_dept);
					// �˷�ʣ�ಿ���˻�������
					addToAccount(connection, "temp", partent_dept, FEER, "�˷��˻�", -yf, hzId, rowid);
					addToAccount(connection, "mobile", hzId, FEER, "�˷��˻�", yf, partent_dept, rowid);
					AppUtil.updateUserCancelTotals(connection, hzId, 1, true);
					// �����˵�״̬Ϊ9 �˵�ȡ��
					statusNew = "9";
					statusNewName = "�ѳ���";
				} else {// ˾��δ֧��ȡ���˵�
						// �����˵�״̬Ϊ0 δ�ӵ�
					if (row.has("data:tmp_cid")) {
						statusNew = "7";
						statusNewName = "���ɵ�";
					} else {
						statusNew = "0";
						statusNewName = "δ�ӵ�";
					}
					AppUtil.updateUserCancelTotals(connection, driverId, 1, true);
				}
			} else {
				if (hzCancel) { // ����ȡ������
						if(driverId!=null&&!(driverId.equals(""))&&!(driverId.equals("-"))){
							// �˻�˾����֤����
							addToAccount(connection, "temp", Configuration.adminAccount, DEPR, "��֤���˻�", -driver_yjJe, driverId, rowid);
							addToAccount(connection, "mobile", driverId, DEPR, "��֤���˻�", driver_yjJe, Configuration.adminAccount, rowid);

							// ֧��˾���⳥��
							double sxfbl=0;
							sxfbl=Double.parseDouble(row.getString("data:sxfbl"));
							double pcj = Configuration.PCJ_DRIVER * yf * (1 - sxfbl);
							pcj = Math.ceil(pcj * 100.0) / 100.0;
							addToAccount(connection, "income", Configuration.adminAccount, PCJ, "ΥԼ�⳥��", -pcj, driverId, rowid);
							addToAccount(connection, "mobile", driverId, PCJ, "ΥԼ�⳥��", pcj, Configuration.adminAccount, rowid);
						}
						// �۳�����ΥԼ��
						// ΥԼ�����ƽ̨ΥԼ���˾���⳥��
						double wyj = Configuration.WYJ_HUOZHU * yf;
						wyj = Math.ceil(wyj * 100.0) / 100.0;

						wyj_amount = wyj;

						// �˷�ʣ�ಿ���˻�������
						String partent_dept = Configuration.adminAccount;
						if (hzId.indexOf("JGXX") == 0) {
							JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
							partent_dept = json_cid_info.getString("parent_dept");
						}
						System.out.println("2������partent_dept������=" + partent_dept);
						addToAccount(connection, "temp", partent_dept, FEER, "�˷��˻�", -yf, hzId, rowid);
						addToAccount(connection, "mobile", hzId, FEER, "�˷��˻�", yf, partent_dept, rowid);

						addToAccount(connection, "mobile", hzId, WYJ, "����ΥԼ����ȡ", -wyj, partent_dept, rowid);
						addToAccount(connection, "income", partent_dept, WYJ, "����ΥԼ����ȡ", wyj, hzId, rowid);

						AppUtil.updateUserCancelTotals(connection, hzId, 1, true);
						// �����˵�״̬Ϊ9 �˵�ȡ��
						statusNew = "9";
						statusNewName = "�ѳ���";
				} else {
					// ֧�������⳥��
					String partent_dept = Configuration.adminAccount;
					double sxfbl=0;
					sxfbl=Double.parseDouble(row.getString("data:sxfbl"));
					if (hzId.indexOf("JGXX") == 0) {
						JSONObject json_cid_info = com.xframe.utils.XHelper.loadRow(hzId, "syscfg");
						partent_dept = json_cid_info.getString("parent_dept");
					}

					double pcj = Configuration.PCJ_HUOZHU * yf * (1 - sxfbl);
					pcj = Math.ceil(pcj * 100.0) / 100.0;
				
					System.out.println("3������partent_dept������=" + partent_dept);
					addToAccount(connection, "income", partent_dept, PCJ, "ΥԼ�⳥��", -pcj, hzId, rowid);
					addToAccount(connection, "mobile", hzId, PCJ, "ΥԼ�⳥��", pcj, partent_dept, rowid);

					// �۳�˾��ΥԼ��
					double wyj = Configuration.WYJ_DRIVER * yf * (1 - sxfbl);
					wyj = Math.ceil(wyj * 100.0) / 100.0;
					wyj_amount = wyj;
					// double rest=driver_yjJe-wyj;
					addToAccount(connection, "mobile", driverId, WYJ, "˾��ΥԼ����ȡ", -wyj, Configuration.adminAccount, rowid);
					addToAccount(connection, "income", Configuration.adminAccount, WYJ, "˾��ΥԼ����ȡ", wyj, driverId, rowid);

					// �˻�˾����֤��
					addToAccount(connection, "temp", Configuration.adminAccount, FEER, "˾����֤���˻�", -driver_yjJe, driverId, rowid);
					addToAccount(connection, "mobile", driverId, FEER, "˾����֤���˻�", driver_yjJe, Configuration.adminAccount, rowid);

					AppUtil.updateUserCancelTotals(connection, driverId, 1, true);
					// �����˵�״̬Ϊ0 δ�ӵ�

					if (row.has("data:tmp_cid")) {
						statusNew = "7";
						statusNewName = "���ɵ�";
					} else {
						statusNew = "0";
						statusNewName = "δ�ӵ�";
					}
				}
			}

			log.info("����:" + rowid + ",newStatus=" + statusNew + ",newStausName=" + statusNewName + ",hzCancel:" + hzCancel + ",status=" + ydStatus);
			// �����˵���Ϣ
			HTableInterface utable = connection.getTable("yd_list");
			String strTime = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date());

			Put put1 = new Put(Bytes.toBytes(rowid));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_status"), Bytes.toBytes(statusNew));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_driver"), Bytes.toBytes(""));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes(statusNewName));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_time"), Bytes.toBytes(strTime));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("canceled"), Bytes.toBytes("1"));
			utable.put(put1);

			// ɾ��tmp_cid,tmp_cph,tmp_owner
			log.info("ɾ����ʱ�ɵ���Ϣ..." + rowid);
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
			LogUtil.info(LogUtil.CAT_YD, "�˵�����.", "rowid:" + rowid + ",��������:" + hzCancel);
			utable.delete(delete);
			utable.close();

			// ������·��Դ����������Դ�仯֪ͨ
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
				com.hyb.utils.Configuration.removeSourceFromCache(rowid);//���Ϊ0���⣬˵�����׳����ˣ����������ģ���redis���Ҳɾ����
			}
			json.put("success", "true");

			// ����ΥԼ��۳�֪ͨ
			if (wyj_amount > 0) {
				org.json.JSONObject detail = new org.json.JSONObject();
				detail.put("rowid", rowid);
				detail.put("type", "WYJKC");
				if (hzCancel) {
					com.hyb.utils.AppUtil.sendNotify(hzId, "warn", "ΥԼ��۳�֪ͨ", "�����˻��۳�ΥԼ��" + wyj_amount + "Ԫ", detail.toString());
				} else {
					com.hyb.utils.AppUtil.sendNotify(driverId, "warn", "ΥԼ��۳�֪ͨ", "�����˻��۳�ΥԼ��" + wyj_amount + "Ԫ", detail.toString());
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
			// ��ȡ�˵���Ϣ
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			if (row.has("data:trans_ok")) {
				json.put("message", "��������ɡ�");
				return json;
			}
			double sxfbl=0;//ȷ���ʹ��ʱ�������һ�� ���˵����������ѱ������ֶΣ�Ϊ�˽����ʽ����Բ�Э������ʽ��û���ڴ����˵���ʱ����������ֶΣ����Ե����ﱨ���ˡ�������µĻ�����ȥ�� �������ֶε���Э�ֶΡ�
			sxfbl=Double.parseDouble(row.getString("data:sxfbl"));

			double yf = Double.parseDouble(row.getString("data:yf")); // �˷�
			double bf = Double.parseDouble(row.has("data:bf") ? row.getString("data:bf") : "0");
			// String hzyjNo=row.getString("data:yj_huozh_no"); //������֤��id;
			//System.out.println("ROW::" + row);
			double driver_yjJe = row.getDouble("data:yj_driver_je"); 
			double charge = Math.ceil(sxfbl * yf * 100.0) / 100.0; // ƽ̨������
																							// �˷�*�����ѱ���

			json.put("driver", driverId);
			json.put("huozhu", hzId);

			// 1.�˻�������֤�� ==>��֤��-�˷� //�����˻��ޱ䶯

			// 2���˻�˾����֤�� ==>��֤��+�˷�-������
			// �˱�֤��
			/*
			 * boolean
			 * bRet=addAccountDetail(connection,DEPR,"��֤���˻�",driver_yjJe,
			 * driverId,true,driver_yjNo,false); if(!bRet){ json.put("message",
			 * "˾����֤���˻�ʧ��(2)"); LogUtil.error(LogUtil.CAT_ACCOUNT, "˾����֤���˻�ʧ��.",
			 * "orderNo:"+driver_yjNo+",amount:"+driver_yjJe+",cid:"+driverId);
			 * return json; }
			 */

			addToAccount(connection, "temp", Configuration.adminAccount, DEPR, "��֤���˻�", -driver_yjJe, driverId, rowid);
			addToAccount(connection, "mobile", driverId, DEPR, "��֤���˻�", driver_yjJe, Configuration.adminAccount, rowid);

			// ֧���˷�
			/*
			 * bRet=addAccountDetail(connection,FEEP,"֧���˷�",yf-charge,driverId,true
			 * ,null,false); if(!bRet){ json.put("message", "˾���˷�֧��ʧ��(3)");
			 * LogUtil.error(LogUtil.CAT_ACCOUNT, "˾���˷�֧��ʧ��.",
			 * "rowid:"+rowid+",amount:"+(yf-charge)+",cid:"+driverId); return
			 * json; }
			 */
			String partent_dept = Configuration.adminAccount;
			if (row.has("data:parent_dept")) {
				partent_dept = row.getString("data:parent_dept");
			}
			System.out.println("�˵�����֧���˷�partent_dept������=" + partent_dept);
			addToAccount(connection, "temp", partent_dept, FEEP, "֧���˷�", -(yf - charge), driverId, rowid);
			addToAccount(connection, "mobile", driverId, FEEP, "֧���˷�", (yf - charge), partent_dept, rowid);

			// ��ɽ��������������˾����˾���˷ѹ����ڴ˴����� Strat
			JSONObject yd_info = XHelper.loadRow(rowid, "yd_list");
			if (yd_info.has("pdwlgs")) {// �ж��Ƿ�Ϊpc�ɵ���
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
			// ��ɽ��������������˾����˾���˷ѹ����ڴ˴����� end

			// �����ʽ��˻��ޱ䶯

			// ���ӱ�֤���˻�������ϸ==>����ƽ̨����������
			/*
			 * bRet=addAccountDetail(connection,AccountUtil.CHARGE,"ƽ̨��������ȡ",charge
			 * ,"JGXX"+Configuration.adminAccount,false,"",false,true);
			 * if(!bRet){ json.put("message", "����˾��˫����֤���˻�(5)");
			 * LogUtil.error(LogUtil.CAT_ACCOUNT, "����˾��˫����֤���˻�(5).",
			 * "rowid:"+rowid+",amount:"+charge+",cid:"+driverId); return json;
			 * }
			 */
			addToAccount(connection, "temp", partent_dept, CHARGE, "ƽ̨��������ȡ", -charge, "income", rowid);
			addToAccount(connection, "income", partent_dept, CHARGE, "ƽ̨��������ȡ", charge, "temp", rowid);

			addToAccount(connection, "temp", partent_dept, BX, "֧�����շ�", -bf, "income", rowid);
			addToAccount(connection, "income", partent_dept, BX, "��ȡ���շ�", bf, "temp", rowid);

			if (row.has("data:deptid") && row.has("data:userid"))
				hzId = row.getString("data:deptid") + "@" + row.getString("data:userid");
			// ˫������������
			AppUtil.updateUserTransTotals(connection, hzId, 1, true);
			AppUtil.updateUserTransTotals(connection, driverId, 1, true);

			// �����˵���Ϣ
			HTableInterface utable = connection.getTable("yd_list");
			Put put1 = new Put(Bytes.toBytes(rowid));
			// put1.add(Bytes.toBytes("data"),Bytes.toBytes("yd_status"),Bytes.toBytes("4"));
			put1.add(Bytes.toBytes("data"), Bytes.toBytes("yd_trans_status"), Bytes.toBytes("�����"));
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
				json.put("message", "���״���ʧ��!");
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
		log.info("������������:" + reqSize);
		if (reqSize == 0)
			return;
		org.json.JSONObject o = null;
		try {
			String strReq = RedisClient.getInstance().popFromList(ACCTLIST_NAME);
			log.info("����������:" + strReq);
			o = new org.json.JSONObject(strReq);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("�����˻�����:" + o);
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
								payType = "֧����";
							else if (payType.equals("wx")) {
								payType = "΢��";
								// double amount=obj.getDouble("amount")/100;
								// obj.put("amount",
								// AppUtil.formatAmount(amount));
							} else
								payType = "������ʽ";
							MySMSUtil.sendPaySms(payType, obj.getString("amount"), obj.getString("balance"), mobiles);
						} else {
							MySMSUtil.sendCashSms(obj.getString("amount"), obj.getString("balance"), mobiles);
						}
					}

				}

				String title = type.equals(PAY) ? "�˻���ֵ֪ͨ" : "�˻�����֪ͨ";
				AppUtil.sendNotify(userid.length() == 0 ? cid : userid, LogUtil.INFO, "��ʾ", title, obj.toString());
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
					AppUtil.sendNotify(userid.length() == 0 ? huozhu : userid, LogUtil.INFO, "��ʾ", "���������.", obj.toString());
					AppUtil.sendNotify(driver, LogUtil.INFO, "��ʾ", "���������.", obj.toString());
					AppUtil.sendBroadcast("add", obj.toString());
				}
			} else if (type.equals(DEPS) || type.equals(DEPR)) {
				connection = HBaseUtils.getHConnection();
				org.json.JSONObject obj = updateAccount_2(cid, orderNo, tokenid);
				obj.put("tokenid", tokenid);
				obj.put("notify_type", "account");
				obj.put("type", type);

				String title = type.equals(DEPS) ? "��֤��֧��֪ͨ" : "��֤���˻�֪ͨ";
				if (cid.endsWith("huozhu"))
					title = type.equals(DEPS) ? "�˷�֧��֪ͨ" : "�˷��˻�֪ͨ";
				AppUtil.sendNotify(userid.length() == 0 ? cid : userid, LogUtil.INFO, "��ʾ", title, obj.toString());

				// CacheService.getInstance().putInCache(tokenid, obj);
				RedisClient.getInstance().setStr(tokenid, obj.toString());
				// �������,������ɱ�־
			} else if (type.equals("CANCEL")) {
				String huozhu = o.getString("huozhu");
				String driver = o.has("driver") ? o.getString("driver") : "";
				String strStatus = o.getString("yd_status");
				boolean hzCancel = o.has("hz_cancel") && o.getString("hz_cancel").equals("true");
				org.json.JSONObject obj = cancelTrans(rowid, huozhu, driver, hzCancel, Integer.parseInt(strStatus));
				obj.put("notify_type", "account");
				obj.put("type", type);
				obj.put("rowid", rowid);
				AppUtil.sendNotify(userid.length() == 0 ? huozhu : userid, LogUtil.ERROR, "��ʾ", "�˵��ѳ���.", obj.toString());
				if (driver.length() > 0)
					AppUtil.sendNotify(driver, LogUtil.ERROR, "��ʾ", "�˵��ѳ���.", obj.toString());
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
