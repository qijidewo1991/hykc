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
 * 2018��3��4��13:46:16 �Ż�����Խ�
 * 
 * @author wdx
 * 
 */
public class SearchAccount {
	public static String YINHANG_NO = "76110154800007802";
	public static String ZFB_NO = "tysyfhb@163.com";
	public static String WX_NO = "1431003902";
	public static String ZONGGONGSI = "100";// �ܹ�˾��֯��
	public static String HUOJIA = "1001";// ��ηֹ�˾��֯�� ZGS7788
	public static String FENGQIU = "1002";// ����ֹ�˾��֯��
	public static String LANKAO = "1003";// �����ֹ�˾��֯��

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
		// �������Ϊ4Сʱ
		Date currentTime1 = new Date(new Date().getTime() - 1000 * 60 * 60 * 4);// 4Сʱǰ
		Date currentTime2 = new Date(new Date().getTime() - 1000 * 60 * 60 * 1);// 1Сʱǰ
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
				String rowid = json_item.getString("rowid");// ���ݱ��
				String cid = json_item.getString("cid");// ��Ա���
				String frome_account = json_item.getString("parent_dept");// �����ĸ��ֹ�˾
				if (frome_account.equals("hykc")) {
					organCode = ZONGGONGSI;
				} else if (frome_account.equals("ZGS7788")) {
					organCode = HUOJIA;
				}

				String order_amount = json_item.getString("order_amount").replace("-", "");// ���ݽ��
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
				jsonobj.put("organCode", organCode);// ��֯��
				jsonobj.put("id", rowid);// ����Ψһ��ʶ
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
				jsonobj.put("note ", json_item.getString("source_id"));// ��ע

				// �I(^��^)�J ���ݷ���ɸѡ start�I(^��^)�J
				if (json_item.getString("order_name").equals("�˻�����")) {
					continue;
				}
				if (type.equals("PAY")) { // ��ֵ��
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("purpose", "SFKYT02_SYS");// �տ���;����K3�����Ӧ��
					jsonobj.put("businessType", "1");// 5��5��Ϊ��ֵ�տ�տ���;û��Ч������ӵģ�����Ĭ����3��������1
					jsonobj.put("billType", "AR_RECEIVEBILL");// �������ͣ��տ��AR_RECEIVEBILL���������AP_PAYBILL������K3�����Ӧ��
					jsonobj.put("billType2", "0001");// ����С�ࣨ��K3�����Ӧ��
					String source_id = json_item.getString("source_id");
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
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
					System.out.println(jsonobj.toString());
					// go �ϴ� jsonobj �����
				} else if (type.equals("CASH")) { // ���ֵ�
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("purpose", "SFKYT009");// �տ���;����K3�����Ӧ��
					jsonobj.put("billType", "AP_PAYBILL");// �������ͣ��տ��AR_RECEIVEBILL���������AP_PAYBILL������K3�����Ӧ��
					jsonobj.put("billType2", "002");// ����С�ࣨ��K3�����Ӧ��
					String source_id = json_item.getString("source_id");
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
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go �ϴ� jsonobj �����

				} else if (type.equals("DEPS")) { // ˾���Ǳ�֤�𡢻������˷�
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AR_RECEIVEBILL");
					jsonobj.put("billType2", "SKDLX01_SYS");// ����С�ࣨ��K3�����Ӧ��
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");// ���㷽ʽ����K3�����Ӧ�� 11���ڲ�����
					if (cid.indexOf("driver") >= 0) {
						jsonobj.put("purpose", "SFKYT004");// �տ���;����K3�����Ӧ��
					} else {
						jsonobj.put("purpose", "SFKYT003");
					}
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go �ϴ� jsonobj �����
				} else if (type.equals("FEER")) { // �˻��˷�/��֤�� ˾���Ǳ�֤�� �������˷�
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

					// go �ϴ� jsonobj �����
				} else if (type.equals("PCJ")) {// ˾��/����/������˾ ��ΥԼ�⳥��
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT010");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go �ϴ� jsonobj �����
				} else if (type.equals("WYJ")) {// ΥԼ��
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AR_RECEIVEBILL");
					jsonobj.put("billType2", "SKDLX01_SYS");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT005");
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					System.out.println(jsonobj.toString());
					// go �ϴ� jsonobj �����
				} else if (type.equals("DEPR")) {// �˵���� �˻ر�֤��
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT008");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());

					// go �ϴ� jsonobj �����
				} else if (type.equals("FEEPF")) {// ֧���˷Ѹ�˾��
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "11");
					jsonobj.put("purpose", "SFKYT006");
					System.out.println(jsonobj.toString());
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
					// go �ϴ� jsonobj �����
				} else if (type.equals("DFCZ")) {// �渶��ֵ
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "004");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "12");
					jsonobj.put("purpose", "SFKYT013");
					jsonobj.put("cashAccount", "111111");// �渶��ֵ�˻�
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("HPCZ")) {// ��Ʊ��ֵ
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "001");
					jsonobj.put("businessType", "3");
					jsonobj.put("purpose", "SFKYT006");
					jsonobj.put("aboutBillNo", json_item.getString("hpm"));// ��Ʊ��
					if (json_item.getString("hp_type").equals("yinhang")) {
						jsonobj.put("settleType", "JSFS07_SYS");
					} else {
						jsonobj.put("settleType", "JSFS06_SYS");
					}
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("XJHK")) {// �ֽ�ؿ�
					jsonobj.put("zhangtao", json_item.get("from_account"));
					jsonobj.put("billType", "AP_PAYBILL");
					jsonobj.put("billType2", "0003");
					jsonobj.put("businessType", "3");
					jsonobj.put("settleType", "JSFS01_SYS");
					jsonobj.put("purpose", "SFKYT014");
					jsonobj.put("cashAccount", "111111");// �渶��ֵ�˻�
					saveAsFileWriter("/data/apache-tomcat-7.0.70/webapps/ROOT/WEB-INF/classes/com/jindie/main/" + "logj.txt", jsonobj.toString());
				} else if (type.equals("HPHK")) {// ��Ʊ�ؿ�
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
				// �I(^��^)�J ���ݷ���ɸѡ end�I(^��^)�J

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
