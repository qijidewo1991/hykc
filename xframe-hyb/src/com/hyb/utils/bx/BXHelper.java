package com.hyb.utils.bx;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import com.hyb.utils.Configuration;
import com.xframe.utils.AppHelper;
import com.xframe.utils.Tools;

public class BXHelper {
	/**
	 * �б���������
	 */
	public static final String[] HWZL = { "01=��е���豸���������Ǳ����������Ӳ�Ʒ", "02=������",
			// 03 ��ȼ�ױ�Σ��Ʒ(���б�)
			// 04 �����մ�(���б�)
			// 05 ����Ʒ���ղ�Ʒ��������(���б�)
			// 06 ��ֲ����(���б�)
			"07=���豸����װ�豸",
			// 08 ��ó�������(���б�)
			"09=���ࡢ��ɰ�ȴ���ɢװ�׶��ٻ���", "10=��ҵ������", "11=��֯Ʒ��", "12=��ͨ��е������豸��", "13=��ͨͰװ����װ����Ʒ��", "14=��ͨ�ṤƷ��",
			// 15 ������һ�������б���
			"16=һ�����",
			// 17 �������(���б�)
			// 18 �ر��������(���б�)
			"19=�������"
	// 20 ���»���(���б�)
	// 21 ���»���(���б�)
	};
	/**
	 * ֤������
	 */
	public static final String[] ZJXX = { "01=�������֤", "02=����֤", "03=����", "04=���񻧿ڲ�", "05=ʿ��֤", "06=ѧ��֤", "07=��ʻ֤", "08=��������֤", "09=����˾���֤", "10=����֤", "11=����", "12=�۰ľ��������ڵ�ͨ��֤", "13=̨����������ڵ�ͨ��֤" };

	/**
	 * ������ղ���
	 * 
	 * @param token
	 * @param coop
	 * @param tradeNo
	 *            ������ˮ��
	 * 
	 * @return
	 * @throws Exception
	 */
	public static org.json.JSONObject packRequest(String token, String coop, String tradeNo, String fromProv, String invoiceNo, String ydbh, String toProv, String hwjz, String hwmc, String hwlb_dm,
			String packing, String hwsl, String bf, String fl, String qyrq, String zjlx, // ��������֤������
			String zjhm, // ��������֤������
			String hzdz, // ������ַ
			String hzmc, // ��������
			String hzlx, // ��������
			String hzdh, // �����绰
			java.util.HashMap<String, String> conf, org.json.JSONObject jsonSource, String parentdept) throws Exception {

		org.json.JSONObject arg = new org.json.JSONObject();

		org.json.JSONObject sig = Signature.getSignature(token);

		// requestHead ����ͷ��Ϣ
		org.json.JSONObject requestHead = new org.json.JSONObject();
		requestHead.put("cooperation", coop);
		requestHead.put("nonce", sig.getString("nonce"));
		requestHead.put("sign", sig.getString("sign"));
		requestHead.put("timestamp", sig.getString("timestamp"));
		requestHead.put("tradeNo", tradeNo);
		requestHead.put("tradeDate", org.mbc.util.Tools.formatDate("yyyy-MM-dd'T'HH:mm:ss.SSS", new java.util.Date()));

		// cargoProposalObj ��������Ϣ
		org.json.JSONObject cargoProposalObj = new org.json.JSONObject();

		/**
		 * 1. ecargoItemObj ��������Ϣ
		 */
		org.json.JSONObject ecargoItemObj = new org.json.JSONObject();

		// ecargoItemObj.put("atartCountryCode","name1"); //���˸����ڹ��ҵ�������
		// ecargoItemObj.put("atartSiteCode", "100001"); //���˸�
		ecargoItemObj.put("atartSiteName", fromProv); // ��ʼ������
		ecargoItemObj.put("invoiceAmount", hwjz); // ��Ʊ���
		ecargoItemObj.put("invoiceNo", invoiceNo); // ��Ʊ��
		ecargoItemObj.put("ladingNo", ydbh); // �ᵥ�ţ��˵��ţ�
		ecargoItemObj.put("transMode", "4"); // �̶� "���䷽ʽ"
		// ecargoItemObj.put("viasiteCountryCode", "121"); //;�������ڹ��ҵ�������
		// ecargoItemObj.put("viasiteCode", "121"); //;����
		// ecargoItemObj.put("viasiteName", "����"); //;��������
		// ecargoItemObj.put("wndCountryCode", "150"); //Ŀ�ĸ����ڹ��ҵ�������
		// ecargoItemObj.put("wndSiteCode", "2000001"); //Ŀ�ĸ�
		ecargoItemObj.put("wndSiteName", toProv); // Ŀ�ĵ�����

		String ydDriver = jsonSource.getString("yd_driver");
		org.json.JSONObject driverObj = Configuration.loadUserDataByClientId(null, ydDriver);

		ecargoItemObj.put("conveyanceName", "���� " + driverObj.getString("data:rz#cph")); // ���乤������
		ecargoItemObj.put("cargoTotalPrice", hwjz); // �����ܼ�

		org.json.JSONArray ecargoDetailObj = new org.json.JSONArray(); // ��������ϸ��Ϣ
		org.json.JSONObject detailItem = new org.json.JSONObject();
		detailItem.put("goodsDetailName", hwmc); // ��������
		detailItem.put("goodsSortDtlCode", hwlb_dm); // ��Ʒ��ϸ����
		if (jsonSource != null)
			detailItem.put("marker", jsonSource.getString("from_city") + "-" + jsonSource.getString("to_city")); // ���
		else
			detailItem.put("marker", "-");
		detailItem.put("packing", packing); // ��װ
		detailItem.put("quantity", hwsl); // ����

		ecargoDetailObj.put(detailItem);
		ecargoItemObj.put("ecargoDetailObj", ecargoDetailObj);

		/**
		 * 2. insuredObj Ͷ������Ϣ/����������Ϣ
		 */

		// Ͷ����
		org.json.JSONArray insuredObj = new org.json.JSONArray();
		org.json.JSONObject insureItem1 = new org.json.JSONObject();
		if (parentdept.equals("ZGS410724")) {// ��ηֹ�˾
			insureItem1.put("identifyNumber", "G10410724000716409"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS410225")) {// �����ֹ�˾
			insureItem1.put("identifyNumber", "G1041022500111640C"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "�����ز�ҵ��������������E��9��"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾�����ֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS410727")) {// ����ֹ�˾
			insureItem1.put("identifyNumber", "G1041072700060570P"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS454950")) {// ����ֹ�˾
			insureItem1.put("identifyNumber", "G10410823001010402"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "������������վ����Դ·�жΣ���¥307��"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾����ֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else if (parentdept.equals("ZGS472000")) {// ����Ͽ�ֹ�˾
			insureItem1.put("identifyNumber", "G1041072700060570P"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else if (parentdept.equals("ZGS471100")) {// �Ͻ�ֹ�˾
			insureItem1.put("identifyNumber", "G1041072700060570P"); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", "����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else{
			insureItem1.put("identifyNumber", conf.get("identifyNumber")); // ����֤������/������֯��������
			insureItem1.put("identifyType", conf.get("identifyType")); // ֤������
			insureItem1.put("insuredAddress", conf.get("insuredAddress")); // ��ϵ�˵�ַ
			insureItem1.put("insuredFlag", "1"); // ��ϵ�˱�־
			insureItem1.put("insuredName", conf.get("insuredName")); // ��ϵ������
			insureItem1.put("insuredType", conf.get("insuredType")); // ��ϵ������
			// insureItem1.put("mobile", "13000001111"); //�ƶ��绰�͵绰����������дһ��
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}
		insuredObj.put(insureItem1);

		// ��������
		org.json.JSONObject insureItem2 = new org.json.JSONObject();
		insureItem2.put("identifyNumber", zjhm);
		insureItem2.put("identifyType", zjlx); // ֤������
		insureItem2.put("insuredAddress", hzdz);
		insureItem2.put("insuredFlag", "2");
		insureItem2.put("insuredName", hzmc);
		insureItem2.put("insuredType", hzlx);
		insureItem2.put("phoneNumber", hzdh);
		insuredObj.put(insureItem2);
		/**
		 * 3. itemKindObj ����������Ϣ
		 */
		org.json.JSONArray itemKindObj = new org.json.JSONArray();
		org.json.JSONObject kind1 = new org.json.JSONObject();
		kind1.put("amout", hwjz); // ���ս��/�⳥�޶�
		kind1.put("kindCode", "0605001"); // �ձ����
		kind1.put("kindFlag", "Y"); // �Ƿ�����
		kind1.put("premium", bf); // ����
		kind1.put("rate", fl); // ����
		itemKindObj.put(kind1);

		/*
		 * org.json.JSONObject kind2=new org.json.JSONObject();
		 * kind2.put("amout", hwjz); kind2.put("deductibledetail", "null");
		 * kind2.put("kindCode", "0605001"); kind2.put("kindDetail", "");
		 * kind2.put("kindFlag", "1"); kind2.put("kindName", "���ڻ�����");
		 * kind2.put("premium", bf); kind2.put("rate", fl);
		 * 
		 * itemKindObj.put(kind2);
		 */
		/**
		 * 4. proposalBaseObj ������Ϣ��
		 */
		org.json.JSONObject proposalBaseObj = new org.json.JSONObject();
		if (parentdept.equals("ZGS410225")) { // �����ֹ�˾
			proposalBaseObj.put("appliAddress", "�����ز�ҵ��������������E��9��"); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", "����ʡ��ӱʵҵ���޹�˾�����ֹ�˾"); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", "91410225MA44RL264A"); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", "�����ز�ҵ��������������E��9��"); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", "�Ϻ��ֶ���չ����"); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000078"); // �����˺ţ���Ʊ��
		} else if (parentdept.equals("ZGS453300")) { // ����ֹ�˾
			proposalBaseObj.put("appliAddress", "������������������Ժ��"); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", "����ʡ��ӱʵҵ���޹�˾����ֹ�˾"); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", "91410727MA44KDTU2P"); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", "������������������Ժ��"); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", "�Ϻ��ֶ���չ����"); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000054"); // �����˺ţ���Ʊ��
		} else if (parentdept.equals("ZGS410724")) { // ��ηֹ�˾
			proposalBaseObj.put("appliAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", "	����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", "�Ϻ��ֶ���չ����"); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000040"); // �����˺ţ���Ʊ��
		} else if (parentdept.equals("ZGS454950")) { // ����ֹ�˾
			proposalBaseObj.put("appliAddress", "������������վ����Դ·�жΣ���¥307��"); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", "	����ʡ��ӱʵҵ���޹�˾����ֹ�˾"); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", "������������վ����Դ·�жΣ���¥307��"); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", "�Ϻ��ֶ���չ����"); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000095"); // �����˺ţ���Ʊ��
		} else if (parentdept.equals("ZGS410724")) { // ����Ͽ�ֹ�˾
			proposalBaseObj.put("appliAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", "	����ʡ��ӱʵҵ���޹�˾��ηֹ�˾"); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", "����ʡ�����ͬ�˴�����̴���5¥"); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", "�Ϻ��ֶ���չ����"); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000040"); // �����˺ţ���Ʊ��
		} else {
			proposalBaseObj.put("appliAddress", conf.get("insuredAddress")); // Ͷ���˵�ַ
			proposalBaseObj.put("appliName", conf.get("insuredName")); // Ͷ��������
			proposalBaseObj.put("taxpayerIdentityNo", conf.get("taxpayerIdentityNo")); // ��˰��ʶ���
			proposalBaseObj.put("taxpayerAddress", conf.get("taxpayerAddress")); // ��ַ����Ʊ��
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // �绰����Ʊ��
			proposalBaseObj.put("taxpayerOpenAccount", conf.get("taxpayerOpenAccount")); // ���������ƣ���Ʊ��
			proposalBaseObj.put("taxpayerBankNo", conf.get("taxpayerBankNo")); // �����˺ţ���Ʊ��
		}
		proposalBaseObj.put("isPrintFeeRate", "Y"); // �Ƿ��ӡ���ѷ���
		proposalBaseObj.put("openBill", "1"); // ��Ʊ����1ΪͶ����
		proposalBaseObj.put("taxpayerId", "1"); // ��˰�����
		proposalBaseObj.put("piFlag", "1"); // �迪רƱ
		proposalBaseObj.put("insuredName", hzmc);// ������������
		proposalBaseObj.put("amountCurrency", "CNY"); // ������� �̶�ֵ
		proposalBaseObj.put("premium", bf); // ����
		proposalBaseObj.put("premiumCurrency", "CNY"); // Ӧ�ձ��ѱ���
		proposalBaseObj.put("startTransportDate", qyrq); // ��������
		proposalBaseObj.put("dutyRange", conf.get("dutyRange")); // ���η�Χ
		proposalBaseObj.put("agreement", conf.get("agreement")); // �ر�Լ��

		/**
		 * 5. userObj ��������Ϣ
		 */
		org.json.JSONObject userObj = new org.json.JSONObject();

		userObj.put("name", "0100000001"); // �û���
		userObj.put("password", "0000"); // ����
		userObj.put("transGUID", tradeNo); // ������ˮ��
		userObj.put("cooperation", coop); // ������

		cargoProposalObj.put("ecargoItemObj", ecargoItemObj);
		cargoProposalObj.put("insuredObj", insuredObj);
		cargoProposalObj.put("itemKindObj", itemKindObj);
		cargoProposalObj.put("proposalBaseObj", proposalBaseObj);
		cargoProposalObj.put("userObj", userObj);

		// operation ��������
		String operation = "saveProposal";

		arg.put("requestHead", requestHead);
		arg.put("cargoProposalObj", cargoProposalObj);
		arg.put("operation", operation);

		return arg;
	}

	public synchronized static String getTradeNo() {
		String no = org.mbc.util.Tools.formatDate("'TY'yyyyMMddHHmmssSSS", new java.util.Date());
		return no;
	}

	public static java.util.HashMap<String, String> getSystemConf() {

		Properties pps = new Properties();
		try {
			// org.mbc.util.Tools.loadTextFile("d:\\output\\sysconf.properties");
			pps.load(new java.io.InputStreamReader(new FileInputStream("d:\\output\\sysconf.properties"), "utf-8"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		java.util.HashMap<String, String> hm = new java.util.HashMap<String, String>();
		java.util.Enumeration<?> enum1 = (java.util.Enumeration<?>) pps.propertyNames();// �õ������ļ�������
		while (enum1.hasMoreElements()) {
			String strKey = enum1.nextElement().toString();
			String strValue = pps.getProperty(strKey);
			hm.put(strKey, strValue);
		}
		return hm;
	}

	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		try {
			java.util.HashMap<String, String> conf = getSystemConf();
			String token = "7caefe35d6bc19a67e6e8d0e564";
			String coop = "huowangtong";
			String tradeNo = getTradeNo();
			String invoceNo = tradeNo;
			String qyrq = org.mbc.util.Tools.formatDate("yyyy-MM-dd'T'HH:mm:ss.SSS", new java.util.Date());
			org.json.JSONObject jsonParams = BXHelper.packRequest(token, coop, tradeNo, "����", invoceNo, "111-444-222", "����", "300000", "�Ҿ�", "01", "��", "30����", "1234.11", "0.035", qyrq, "zjlx",
					"zjhm", "hzdz", "hzmc", "hzlx", "hzdh", conf, null, "");
			System.out.println("" + jsonParams);
			// String
			// strReturn=Tools.post("http://180.168.131.21/uiep/ecargo/insuranceService",params);
			// org.json.JSONObject jsonRet=new org.json.JSONObject(strReturn);

			// System.out.println("\t���մ�����:"+jsonRet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
