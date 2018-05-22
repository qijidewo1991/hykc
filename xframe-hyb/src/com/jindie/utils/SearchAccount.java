package com.jindie.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.hyb.utils.Configuration;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.XHelper;

/**
 * 2018年3月4日13:46:16 优化金蝶对接
 * 
 * @author wdx
 * 
 */
public class SearchAccount {
	public static String YINHANG_NO = "76110154800007802";
	public static String ZFB_NO = "tysyfhb@163.com";
	public static String WX_NO = "1431003902";
	public static String ZONGGONGSI = "100";// 总公司组织码
	public static String HUOJIA = "1001";// 获嘉分公司组织码 ZGS7788
	public static String FENGQIU = "1002";// 封丘分公司组织码
	public static String LANKAO = "1003";// 兰考分公司组织码

	public static void doSearch() {
		org.json.JSONObject argDesc = new org.json.JSONObject();
		try {
			argDesc.put("data:change_amount", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:balance_rest", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:balance", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:order_amount", HBaseUtils.TYPE_NUMBER);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		JSONArray f = new JSONArray();
		JSONObject c1 = new JSONObject();
		JSONObject c2 = new JSONObject();
		JSONObject c3 = new JSONObject();
		JSONObject c4 = new JSONObject();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH");
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 搜索间隔为4小时
		Date currentTime1 = new Date(new Date().getTime() - 1000 * 60 * 60 * 4);// 4小时前
		Date currentTime2 = new Date(new Date().getTime() - 1000 * 60 * 60 * 1);// 1小时前
		String dateString1 = formatter.format(currentTime1);
		String dateString2 = formatter.format(currentTime2);
		try {
			c1.put("name", "data:sfzf");
			c1.put("oper", "=");
			c1.put("value", "1");
			c2.put("name", "data:order_time");
			c2.put("oper", ">=");
			c2.put("value", "2018-03-04 16" + ":30:00");
			c3.put("name", "data:order_time");
			c3.put("oper", "<=");
			c3.put("value", dateString2 + ":59:59");
			c4.put("name", "data:account_type");
			c4.put("oper", "=");
			c4.put("value", "mobile");
		} catch (Exception e) {
			// TODO: handle exception
		}
		f.put(c1);
		f.put(c2);
		f.put(c3);
		f.put(c4);
		JSONArray rows = XHelper.listRowsByFilter("account_detail", "", f, false, argDesc);
		for (int i = 0; i < rows.length(); i++) {
			JSONObject json_item = new JSONObject();
			String organCode = "";
			try {
				json_item = rows.getJSONObject(i);
				String rowid = json_item.getString("rowid");// 单据编号
				String cid = json_item.getString("cid");// 人员编号
				String frome_account = json_item.getString("parent_dept");// 属于哪个分公司
				if (frome_account.equals("hykc")) {
					organCode = ZONGGONGSI;
				} else if (frome_account.equals("ZGS7788")) {
					organCode = HUOJIA;
				}

				String order_amount = json_item.getString("order_amount").replace("-", "");// 单据金额
				String submit_time = json_item.getString("order_time");// 单据时间
				String type = json_item.getString("type");
				String name = "已删除";
				// ↖(^ω^)↗ 因为有些cid 由于认为操作，删除了这个用户，导致没有这个人员信息 判断 start↖(^ω^)↗
				if (cid.indexOf("JGXX") >= 0) {
					// System.out.println("json_item=" + json_item.toString());
					try {
						name = Configuration.loadUserDataByClientId(null, cid).getString("data:jgmc");// 人员名称
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						name = Configuration.loadUserDataByClientId(null, cid).getString("data:username");// 人员名称
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// ↖(^ω^)↗ 因为有些cid 由于认为操作，删除了这个用户，导致没有这个人员信息 判断 end↖(^ω^)↗

				JSONObject jsonobj = new JSONObject();
				jsonobj.put("organCode", organCode);// 组织码
				jsonobj.put("id", rowid);// 单据唯一标识
				jsonobj.put("amount", order_amount);// 金额
				jsonobj.put("businessDate", submit_time);// 业务日期
				jsonobj.put("addTime", formatter2.format(new Date()));// 单据产生时间
				jsonobj.put("checkCode", "123457");// 校验码
				JSONObject jsonobj_son = new JSONObject();
				jsonobj_son.put("code", cid);// 客户编号（业务系统保证其唯一性）
				jsonobj_son.put("name", name);// 客户名称
				jsonobj_son.put("currId", "PRE001");// 非必填，币别：不填时，默认人民币 PRE001
				jsonobj_son.put("tradingCurrId", "PRE001");
				if (cid.indexOf("driver") >= 0) {// 客户类型：货主PAEZ_HuoMain，司机PAEZ_SiJi（与K3中相对应）
					jsonobj_son.put("type", "PAEZ_SiJi");
				} else {
					jsonobj_son.put("type", "PAEZ_HuoMain");
				}
				jsonobj.put("customer", jsonobj_son);// 加入用户基础信息。
				jsonobj.put("note ", json_item.getString("source_id"));// 备注

				// ↖(^ω^)↗ 单据分类筛选 start↖(^ω^)↗
				if (json_item.getString("order_name").equals("账户调整")) {
					continue;
				}
				if (type.equals("PAY")) { // 充值单
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("purpose", "SFKYT02_SYS");// 收款用途（与K3中相对应）
					jsonobj.put("businessType", "1");// 5月5日为充值收款单收款用途没生效，李金峰加的，不传默认是3，传了是1
					jsonobj.put("billType", "AR_RECEIVEBILL");// 单据类型：收款单（AR_RECEIVEBILL）、付款单（AP_PAYBILL）（与K3中相对应）
					jsonobj.put("billType2", "0001");// 单据小类（与K3中相对应）
					String source_id = json_item.getString("source_id");
					String pay_type = XHelper.loadRow(source_id, "account_detail").getString("pay_type");
					if (pay_type.indexOf("zfb") >= 0) {
						jsonobj.put("settleType", "09");// 结算方式（与K3中相对应）
						jsonobj.put("cashAccount", ZFB_NO);// 我方银行账号
					} else if (pay_type.indexOf("wx") >= 0) {
						jsonobj.put("settleType", "10");
						jsonobj.put("cashAccount", WX_NO);
					} else {
						jsonobj.put("settleType", "JSFS04_SYS");
						jsonobj.put("mybankAccount", YINHANG_NO);
					}
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
					System.out.println(jsonobj.toString());
					// go 上传 jsonobj 待添加
				} else if (type.equals("CASH")) { // 提现单
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("purpose", "SFKYT009");// 收款用途（与K3中相对应）
					jsonobj.put("billType", "AP_PAYBILL");// 单据类型：收款单（AR_RECEIVEBILL）、付款单（AP_PAYBILL）（与K3中相对应）
					jsonobj.put("billType2", "002");// 单据小类（与K3中相对应）
					String source_id = json_item.getString("source_id");
					String ext = XHelper.loadRow(source_id, "account_detail").getString("ext");
					if (cid.indexOf("JGXX") >= 0) {
						String ext_arry[] = ext.split(",");
						if (ext_arry[0].indexOf("zfb") >= 0 || ext_arry[0].indexOf("wx") >= 0) {
							jsonobj.put("settleType", ext_arry[0].indexOf("zfb") >= 0 ? "09" : "10");// 结算方式（与K3中相对应）
							jsonobj.put("cashAccount", ext_arry[0].indexOf("zfb") >= 0 ? ZFB_NO : WX_NO);// 我方银行账号
							jsonobj.put("bankAccount", ext_arry[1]);// 对方银行账号
							jsonobj.put("bankAccountName", ext_arry[2]);// 对方账户名称
							jsonobj.put("bank", ext_arry[0].indexOf("zfb") >= 0 ? "支付宝" : "微信"); // 对方开记行
						} else {
							jsonobj.put("settleType", "JSFS04_SYS");
							jsonobj.put("mybankAccount", YINHANG_NO);// 我方银行账号
							jsonobj.put("bankAccount", ext_arry[2]);// 对方银行账号
							jsonobj.put("bankAccountName", ext_arry[1]);// 对方账户名称
							jsonobj.put("bank", ext_arry[0]); // 对方开记行
						}

					} else {
						JSONObject ext_json = new JSONObject(ext);
						String ext_type = ext_json.getString("type");
						if (ext_type.equals("支付宝")) {
							jsonobj.put("settleType", "09");// 结算方式（与K3中相对应）
							jsonobj.put("cashAccount", ZFB_NO);// 我方银行账号
							jsonobj.put("bankAccount", ext_json.getString("account"));// 对方银行账号
							jsonobj.put("bankAccountName", ext_json.getString("name"));// 对方账户名称
							jsonobj.put("bank", ext_json.getString("type")); // 对方开记行
						} else if (ext_type.equals("微信")) {
							jsonobj.put("settleType", "10");
							jsonobj.put("cashAccount", WX_NO);
							jsonobj.put("bankAccount", ext_json.getString("account"));
							jsonobj.put("bankAccountName", ext_json.getString("name"));
							jsonobj.put("bank", ext_json.getString("type"));
						} else {
							jsonobj.put("settleType", "JSFS04_SYS");
							jsonobj.put("mybankAccount", YINHANG_NO);
							jsonobj.put("bankAccount", ext_json.getString("account"));
							jsonobj.put("bankAccountName", ext_json.getString("name"));
							if (ext_json.has("bank")) {
								jsonobj.put("bank", ext_json.getString("address"));
							} else {
								jsonobj.put("bank", "null");
							}
						}
					}
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go 上传 jsonobj 待添加

				} else if (type.equals("DEPS")) { // 司机是保证金、货主是运费
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AR_RECEIVEBILL");
					jsonobj.put("billType2", "SKDLX01_SYS");// 单据小类（与K3中相对应）
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");// 结算方式（与K3中相对应） 11是内部记账
					if (cid.indexOf("driver") >= 0) {
						jsonobj.put("purpose", "SFKYT004");// 收款用途（与K3中相对应）
					} else {
						jsonobj.put("purpose", "SFKYT003");
					}
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go 上传 jsonobj 待添加
				} else if (type.equals("FEER")) { // 退回运费/保证金 司机是保证金 货主是运费
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					if (cid.indexOf("driver") >= 0) {
						jsonobj.put("purpose", "SFKYT008");
					} else {
						jsonobj.put("purpose", "SFKYT007");
					}
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go 上传 jsonobj 待添加
				} else if (type.equals("PCJ")) {// 司机/货主/物流公司 被违约赔偿金
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT010");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go 上传 jsonobj 待添加
				} else if (type.equals("WYJ")) {// 违约金
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AR_RECEIVEBILL");
					jsonobj.put("billType2", "SKDLX01_SYS");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT005");
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					System.out.println(jsonobj.toString());
					// go 上传 jsonobj 待添加
				} else if (type.equals("DEPR")) {// 运单完成 退回保证金
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT008");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go 上传 jsonobj 待添加
				} else if (type.equals("FEEPF")) {// 支付运费给司机
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT006");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
					// go 上传 jsonobj 待添加
				} else if (type.equals("DFCZ")) {// 垫付充值
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "004");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "12");
					jsonobj.put("purpose", "SFKYT013");
					jsonobj.put("cashAccount", "111111");// 垫付充值账户
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("HPCZ")) {// 汇票充值
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("purpose", "SFKYT006");
					jsonobj.put("aboutBillNo", json_item.getString("hpm"));// 汇票码
					if (json_item.getString("hp_type").equals("yinhang")) {
						jsonobj.put("settleType", "JSFS07_SYS");
					} else {
						jsonobj.put("settleType", "JSFS06_SYS");
					}
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("XJHK")) {// 现金回款
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "0003");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "JSFS01_SYS");
					jsonobj.put("purpose", "SFKYT014");
					jsonobj.put("cashAccount", "111111");// 垫付充值账户
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("HPHK")) {// 汇票回款
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "0003");
					jsonobj.put("businessType", "3");
					jsonobj.put("purpose", "SFKYT014");
					jsonobj.put("aboutBillNo", json_item.getString("hpm"));
					if (json_item.getString("hp_type").equals("yinhang")) {
						jsonobj.put("settleType", "JSFS07_SYS");
					} else {
						jsonobj.put("settleType", "JSFS06_SYS");
					}
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				}
				// ↖(^ω^)↗ 单据分类筛选 end↖(^ω^)↗

			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}

	public static void saveAsFileWriter(String file, String conent) {
		BufferedWriter out = null;
		File file1 = new File(file);
		File fileParent = file1.getParentFile();
		if (!fileParent.exists()) {
			fileParent.mkdirs();
		}
		try {
			file1.createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(conent + "\r\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
