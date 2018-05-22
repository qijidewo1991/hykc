package com.jindie.k2kutils;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;
import com.hyb.utils.Configuration;
import com.xframe.utils.XHelper;

public class SendToK2K {
	public String YINHANG_NO = "76110154800007802";
	public String ZFB_NO = "tysyfhb@163.com";
	public String WX_NO = "1431003902";

	public void send(JSONObject json_item) {
		SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			String rowid = json_item.getString("rowid");// 单据编号
			String fromAccount = json_item.getString("from_account");// 组织编码
			if (fromAccount.equals("hykc")) {// 总公司
				fromAccount = "100";
			} else if (fromAccount.equals("ZGS410225")) {// 兰考分公司
				fromAccount = "1003";
				YINHANG_NO = "76110078801800000078";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS410724")) {// 获嘉分公司
				fromAccount = "1001";
				YINHANG_NO = "76110078801800000040";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS410727")) {// 封丘分公司
				fromAccount = "1002";
				YINHANG_NO = "76110078801800000054";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS454950")) {// 武陟分公司
				fromAccount = "1004";
				YINHANG_NO = "76110078801800000095";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS472000")) {// 三门峡分公司
				fromAccount = "1005";
//				YINHANG_NO = "76110078801800000095";
//				ZFB_NO = "tysyfhb@163.com";
//				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS471100")) {// 孟津分公司
				fromAccount = "1006";
//				YINHANG_NO = "76110078801800000095";
//				ZFB_NO = "tysyfhb@163.com";
//				WX_NO = "1431003902";
			}
			String source_id = json_item.getString("source_id");
			String cid = json_item.getString("cid");// 人员编号
			String order_amount = json_item.getString("order_amount").replace("-", "");// 单据金额
			if (!(Double.parseDouble(order_amount) > 0)) {
				return;
			}
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
			jsonobj.put("id", rowid);// 单据唯一标识
			jsonobj.put("organCode", fromAccount);// 组织编码
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
			if (type.equals("DEPS") || type.equals("DEPR") || type.equals("FEEPF") || type.equals("FEER") || type.equals("WYJ") || type.equals("PCJ")) {
				JSONObject j_yd = new JSONObject();
				j_yd = XHelper.loadRow(source_id, "yd_list");
				String sid = j_yd.getString("sid");
				jsonobj.put("remark", sid);// 备注
			}

			// ↖(^ω^)↗ 单据分类筛选 start↖(^ω^)↗
			if (json_item.getString("order_name").equals("账户调整")) {
				return;
			}
			if (type.equals("PAY")) { // 充值单
				jsonobj.put("purpose", "SFKYT02_SYS");// 收款用途（与K3中相对应）
				jsonobj.put("businessType", "1");// 5月5日为充值收款单收款用途没生效，李金峰加的，不传默认是3，传了是1
				jsonobj.put("billType", "AR_RECEIVEBILL");// 单据类型：收款单（AR_RECEIVEBILL）、付款单（AP_PAYBILL）（与K3中相对应）
				jsonobj.put("billType2", "0001");// 单据小类（与K3中相对应）

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
				K2KTools.sendBill(jsonobj);

				return;
			} else if (type.equals("CASH")) { // 提现单
				jsonobj.put("purpose", "SFKYT009");// 收款用途（与K3中相对应）
				jsonobj.put("billType", "AP_PAYBILL");// 单据类型：收款单（AR_RECEIVEBILL）、付款单（AP_PAYBILL）（与K3中相对应）
				jsonobj.put("billType2", "002");// 单据小类（与K3中相对应）
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
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("DEPS")) { // 司机是保证金、货主是运费
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");// 单据小类（与K3中相对应）
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");// 结算方式（与K3中相对应） 11是内部记账
				if (cid.indexOf("driver") >= 0) {
					jsonobj.put("purpose", "SFKYT004");// 收款用途（与K3中相对应）
				} else {
					jsonobj.put("purpose", "SFKYT003");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("FEER")) { // 退回运费/保证金 司机是保证金 货主是运费
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				if (cid.indexOf("driver") >= 0) {
					jsonobj.put("purpose", "SFKYT008");
				} else {
					jsonobj.put("purpose", "SFKYT007");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("PCJ")) {// 司机/货主/物流公司 被违约赔偿金
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT010");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("WYJ")) {// 违约金
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT005");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("DEPR")) {// 运单完成 退回保证金
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT008");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("FEEPF")) {// 支付运费给司机
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT006");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("DFCZ")) {// 垫付充值
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "12");
				jsonobj.put("purpose", "SFKYT013");
				jsonobj.remove("cashAccount");
				jsonobj.put("cashAccount", "888");// 垫付充值账户
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HPCZ")) {// 汇票充值
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "0001");
				jsonobj.put("businessType", "3");
				jsonobj.put("purpose", "SFKYT006");
				jsonobj.put("aboutBillNo", json_item.getString("hpm"));// 汇票码
				jsonobj.put("aboutBillSum", order_amount);
				if (json_item.getString("hp_type").equals("yinhang")) {
					jsonobj.put("settleType", "JSFS07_SYS");
				} else {
					jsonobj.put("settleType", "JSFS06_SYS");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("XJHK")) {// 现金回款
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "JSFS01_SYS");
				jsonobj.put("purpose", "SFKYT014");
				jsonobj.remove("cashAccount");
				jsonobj.put("cashAccount", "888");// 垫付充值账户
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HPHK")) {// 汇票回款 如果为汇票业务，不需要填现金账号
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("purpose", "SFKYT014");
				jsonobj.put("aboutBillNo", json_item.getString("hpm"));
				jsonobj.put("aboutBillSum", order_amount);
				if (json_item.getString("hp_type").equals("yinhang")) {
					jsonobj.put("settleType", "JSFS07_SYS");
				} else {
					jsonobj.put("settleType", "JSFS06_SYS");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HSHCF")) {// 付货损货差
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT016");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HSHCS")) {// 收货损货差
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT015");
				K2KTools.sendBill(jsonobj);
				return;
			}
			// ↖(^ω^)↗ 单据分类筛选 end↖(^ω^)↗

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
