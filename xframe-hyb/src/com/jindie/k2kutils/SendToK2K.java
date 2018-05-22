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
			String rowid = json_item.getString("rowid");// ���ݱ��
			String fromAccount = json_item.getString("from_account");// ��֯����
			if (fromAccount.equals("hykc")) {// �ܹ�˾
				fromAccount = "100";
			} else if (fromAccount.equals("ZGS410225")) {// �����ֹ�˾
				fromAccount = "1003";
				YINHANG_NO = "76110078801800000078";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS410724")) {// ��ηֹ�˾
				fromAccount = "1001";
				YINHANG_NO = "76110078801800000040";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS410727")) {// ����ֹ�˾
				fromAccount = "1002";
				YINHANG_NO = "76110078801800000054";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS454950")) {// ����ֹ�˾
				fromAccount = "1004";
				YINHANG_NO = "76110078801800000095";
				ZFB_NO = "tysyfhb@163.com";
				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS472000")) {// ����Ͽ�ֹ�˾
				fromAccount = "1005";
//				YINHANG_NO = "76110078801800000095";
//				ZFB_NO = "tysyfhb@163.com";
//				WX_NO = "1431003902";
			} else if (fromAccount.equals("ZGS471100")) {// �Ͻ�ֹ�˾
				fromAccount = "1006";
//				YINHANG_NO = "76110078801800000095";
//				ZFB_NO = "tysyfhb@163.com";
//				WX_NO = "1431003902";
			}
			String source_id = json_item.getString("source_id");
			String cid = json_item.getString("cid");// ��Ա���
			String order_amount = json_item.getString("order_amount").replace("-", "");// ���ݽ��
			if (!(Double.parseDouble(order_amount) > 0)) {
				return;
			}
			String submit_time = json_item.getString("order_time");// ����ʱ��
			String type = json_item.getString("type");
			String name = "��ɾ��";

			// �I(^��^)�J ��Ϊ��Щcid ������Ϊ������ɾ��������û�������û�������Ա��Ϣ �ж� start�I(^��^)�J
			if (cid.indexOf("JGXX") >= 0) {
				// System.out.println("json_item=" + json_item.toString());
				try {
					name = Configuration.loadUserDataByClientId(null, cid).getString("data:jgmc");// ��Ա����
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					name = Configuration.loadUserDataByClientId(null, cid).getString("data:username");// ��Ա����
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// �I(^��^)�J ��Ϊ��Щcid ������Ϊ������ɾ��������û�������û�������Ա��Ϣ �ж� end�I(^��^)�J

			JSONObject jsonobj = new JSONObject();
			jsonobj.put("id", rowid);// ����Ψһ��ʶ
			jsonobj.put("organCode", fromAccount);// ��֯����
			jsonobj.put("amount", order_amount);// ���
			jsonobj.put("businessDate", submit_time);// ҵ������
			jsonobj.put("addTime", formatter2.format(new Date()));// ���ݲ���ʱ��
			jsonobj.put("checkCode", "123457");// У����
			JSONObject jsonobj_son = new JSONObject();
			jsonobj_son.put("code", cid);// �ͻ���ţ�ҵ��ϵͳ��֤��Ψһ�ԣ�
			jsonobj_son.put("name", name);// �ͻ�����
			jsonobj_son.put("currId", "PRE001");// �Ǳ���ұ𣺲���ʱ��Ĭ������� PRE001
			jsonobj_son.put("tradingCurrId", "PRE001");
			if (cid.indexOf("driver") >= 0) {// �ͻ����ͣ�����PAEZ_HuoMain��˾��PAEZ_SiJi����K3�����Ӧ��
				jsonobj_son.put("type", "PAEZ_SiJi");
			} else {
				jsonobj_son.put("type", "PAEZ_HuoMain");
			}
			jsonobj.put("customer", jsonobj_son);// �����û�������Ϣ��
			if (type.equals("DEPS") || type.equals("DEPR") || type.equals("FEEPF") || type.equals("FEER") || type.equals("WYJ") || type.equals("PCJ")) {
				JSONObject j_yd = new JSONObject();
				j_yd = XHelper.loadRow(source_id, "yd_list");
				String sid = j_yd.getString("sid");
				jsonobj.put("remark", sid);// ��ע
			}

			// �I(^��^)�J ���ݷ���ɸѡ start�I(^��^)�J
			if (json_item.getString("order_name").equals("�˻�����")) {
				return;
			}
			if (type.equals("PAY")) { // ��ֵ��
				jsonobj.put("purpose", "SFKYT02_SYS");// �տ���;����K3�����Ӧ��
				jsonobj.put("businessType", "1");// 5��5��Ϊ��ֵ�տ�տ���;û��Ч������ӵģ�����Ĭ����3��������1
				jsonobj.put("billType", "AR_RECEIVEBILL");// �������ͣ��տ��AR_RECEIVEBILL���������AP_PAYBILL������K3�����Ӧ��
				jsonobj.put("billType2", "0001");// ����С�ࣨ��K3�����Ӧ��

				String pay_type = XHelper.loadRow(source_id, "account_detail").getString("pay_type");
				if (pay_type.indexOf("zfb") >= 0) {
					jsonobj.put("settleType", "09");// ���㷽ʽ����K3�����Ӧ��
					jsonobj.put("cashAccount", ZFB_NO);// �ҷ������˺�
				} else if (pay_type.indexOf("wx") >= 0) {
					jsonobj.put("settleType", "10");
					jsonobj.put("cashAccount", WX_NO);
				} else {
					jsonobj.put("settleType", "JSFS04_SYS");
					jsonobj.put("mybankAccount", YINHANG_NO);
				}
				K2KTools.sendBill(jsonobj);

				return;
			} else if (type.equals("CASH")) { // ���ֵ�
				jsonobj.put("purpose", "SFKYT009");// �տ���;����K3�����Ӧ��
				jsonobj.put("billType", "AP_PAYBILL");// �������ͣ��տ��AR_RECEIVEBILL���������AP_PAYBILL������K3�����Ӧ��
				jsonobj.put("billType2", "002");// ����С�ࣨ��K3�����Ӧ��
				String ext = XHelper.loadRow(source_id, "account_detail").getString("ext");
				if (cid.indexOf("JGXX") >= 0) {
					String ext_arry[] = ext.split(",");
					if (ext_arry[0].indexOf("zfb") >= 0 || ext_arry[0].indexOf("wx") >= 0) {
						jsonobj.put("settleType", ext_arry[0].indexOf("zfb") >= 0 ? "09" : "10");// ���㷽ʽ����K3�����Ӧ��
						jsonobj.put("cashAccount", ext_arry[0].indexOf("zfb") >= 0 ? ZFB_NO : WX_NO);// �ҷ������˺�
						jsonobj.put("bankAccount", ext_arry[1]);// �Է������˺�
						jsonobj.put("bankAccountName", ext_arry[2]);// �Է��˻�����
						jsonobj.put("bank", ext_arry[0].indexOf("zfb") >= 0 ? "֧����" : "΢��"); // �Է�������
					} else {
						jsonobj.put("settleType", "JSFS04_SYS");
						jsonobj.put("mybankAccount", YINHANG_NO);// �ҷ������˺�
						jsonobj.put("bankAccount", ext_arry[2]);// �Է������˺�
						jsonobj.put("bankAccountName", ext_arry[1]);// �Է��˻�����
						jsonobj.put("bank", ext_arry[0]); // �Է�������
					}
				} else {
					JSONObject ext_json = new JSONObject(ext);
					String ext_type = ext_json.getString("type");
					if (ext_type.equals("֧����")) {
						jsonobj.put("settleType", "09");// ���㷽ʽ����K3�����Ӧ��
						jsonobj.put("cashAccount", ZFB_NO);// �ҷ������˺�
						jsonobj.put("bankAccount", ext_json.getString("account"));// �Է������˺�
						jsonobj.put("bankAccountName", ext_json.getString("name"));// �Է��˻�����
						jsonobj.put("bank", ext_json.getString("type")); // �Է�������
					} else if (ext_type.equals("΢��")) {
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
			} else if (type.equals("DEPS")) { // ˾���Ǳ�֤�𡢻������˷�
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");// ����С�ࣨ��K3�����Ӧ��
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");// ���㷽ʽ����K3�����Ӧ�� 11���ڲ�����
				if (cid.indexOf("driver") >= 0) {
					jsonobj.put("purpose", "SFKYT004");// �տ���;����K3�����Ӧ��
				} else {
					jsonobj.put("purpose", "SFKYT003");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("FEER")) { // �˻��˷�/��֤�� ˾���Ǳ�֤�� �������˷�
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
			} else if (type.equals("PCJ")) {// ˾��/����/������˾ ��ΥԼ�⳥��
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT010");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("WYJ")) {// ΥԼ��
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT005");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("DEPR")) {// �˵���� �˻ر�֤��
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT008");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("FEEPF")) {// ֧���˷Ѹ�˾��
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT006");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("DFCZ")) {// �渶��ֵ
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "12");
				jsonobj.put("purpose", "SFKYT013");
				jsonobj.remove("cashAccount");
				jsonobj.put("cashAccount", "888");// �渶��ֵ�˻�
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HPCZ")) {// ��Ʊ��ֵ
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "0001");
				jsonobj.put("businessType", "3");
				jsonobj.put("purpose", "SFKYT006");
				jsonobj.put("aboutBillNo", json_item.getString("hpm"));// ��Ʊ��
				jsonobj.put("aboutBillSum", order_amount);
				if (json_item.getString("hp_type").equals("yinhang")) {
					jsonobj.put("settleType", "JSFS07_SYS");
				} else {
					jsonobj.put("settleType", "JSFS06_SYS");
				}
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("XJHK")) {// �ֽ�ؿ�
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "JSFS01_SYS");
				jsonobj.put("purpose", "SFKYT014");
				jsonobj.remove("cashAccount");
				jsonobj.put("cashAccount", "888");// �渶��ֵ�˻�
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HPHK")) {// ��Ʊ�ؿ� ���Ϊ��Ʊҵ�񣬲���Ҫ���ֽ��˺�
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
			} else if (type.equals("HSHCF")) {// ���������
				jsonobj.put("billType", "AP_PAYBILL");
				jsonobj.put("billType2", "001");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT016");
				K2KTools.sendBill(jsonobj);
				return;
			} else if (type.equals("HSHCS")) {// �ջ������
				jsonobj.put("billType", "AR_RECEIVEBILL");
				jsonobj.put("billType2", "SKDLX01_SYS");
				jsonobj.put("businessType", "3");
				jsonobj.put("settleType", "11");
				jsonobj.put("purpose", "SFKYT015");
				K2KTools.sendBill(jsonobj);
				return;
			}
			// �I(^��^)�J ���ݷ���ɸѡ end�I(^��^)�J

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
