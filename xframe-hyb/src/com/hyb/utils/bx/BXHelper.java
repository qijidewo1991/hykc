package com.hyb.utils.bx;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import com.hyb.utils.Configuration;
import com.xframe.utils.AppHelper;
import com.xframe.utils.Tools;

public class BXHelper {
	/**
	 * 承保货物种类
	 */
	public static final String[] HWZL = { "01=机械、设备、仪器、仪表、电器、电子产品", "02=粮油类",
			// 03 易燃易爆危险品(不承保)
			// 04 玻璃陶瓷(不承保)
			// 05 艺术品，收藏品，谷物类(不承保)
			// 06 动植物类(不承保)
			"07=旧设备、裸装设备",
			// 08 非贸易类货物(不承保)
			"09=油类、矿砂等大宗散装易短少货物", "10=工业化工类", "11=纺织品类", "12=普通机械、五金、设备类", "13=普通桶装、袋装化工品类", "14=普通轻工品类",
			// 15 其他类一般货物（不承保）
			"16=一般货物",
			// 17 易损货物(不承保)
			// 18 特别易损货物(不承保)
			"19=冻结货物"
	// 20 冷温货物(不承保)
	// 21 凉温货物(不承保)
	};
	/**
	 * 证件类型
	 */
	public static final String[] ZJXX = { "01=居民身份证", "02=军官证", "03=护照", "04=居民户口簿", "05=士兵证", "06=学生证", "07=驾驶证", "08=军官退休证", "09=外国人居留证", "10=警官证", "11=其他", "12=港澳居民来往内地通行证", "13=台湾居民来往内地通行证" };

	/**
	 * 打包保险参数
	 * 
	 * @param token
	 * @param coop
	 * @param tradeNo
	 *            交易流水号
	 * 
	 * @return
	 * @throws Exception
	 */
	public static org.json.JSONObject packRequest(String token, String coop, String tradeNo, String fromProv, String invoiceNo, String ydbh, String toProv, String hwjz, String hwmc, String hwlb_dm,
			String packing, String hwsl, String bf, String fl, String qyrq, String zjlx, // 被保险人证件类型
			String zjhm, // 被保险人证件号码
			String hzdz, // 货主地址
			String hzmc, // 货主名称
			String hzlx, // 货主类型
			String hzdh, // 货主电话
			java.util.HashMap<String, String> conf, org.json.JSONObject jsonSource, String parentdept) throws Exception {

		org.json.JSONObject arg = new org.json.JSONObject();

		org.json.JSONObject sig = Signature.getSignature(token);

		// requestHead 请求头信息
		org.json.JSONObject requestHead = new org.json.JSONObject();
		requestHead.put("cooperation", coop);
		requestHead.put("nonce", sig.getString("nonce"));
		requestHead.put("sign", sig.getString("sign"));
		requestHead.put("timestamp", sig.getString("timestamp"));
		requestHead.put("tradeNo", tradeNo);
		requestHead.put("tradeDate", org.mbc.util.Tools.formatDate("yyyy-MM-dd'T'HH:mm:ss.SSS", new java.util.Date()));

		// cargoProposalObj 货运险信息
		org.json.JSONObject cargoProposalObj = new org.json.JSONObject();

		/**
		 * 1. ecargoItemObj 货物标的信息
		 */
		org.json.JSONObject ecargoItemObj = new org.json.JSONObject();

		// ecargoItemObj.put("atartCountryCode","name1"); //起运港所在国家地区编码
		// ecargoItemObj.put("atartSiteCode", "100001"); //起运港
		ecargoItemObj.put("atartSiteName", fromProv); // 起始地名称
		ecargoItemObj.put("invoiceAmount", hwjz); // 发票金额
		ecargoItemObj.put("invoiceNo", invoiceNo); // 发票号
		ecargoItemObj.put("ladingNo", ydbh); // 提单号（运单号）
		ecargoItemObj.put("transMode", "4"); // 固定 "运输方式"
		// ecargoItemObj.put("viasiteCountryCode", "121"); //途经港所在国家地区编码
		// ecargoItemObj.put("viasiteCode", "121"); //途经港
		// ecargoItemObj.put("viasiteName", "江苏"); //途经地名称
		// ecargoItemObj.put("wndCountryCode", "150"); //目的港所在国家地区编码
		// ecargoItemObj.put("wndSiteCode", "2000001"); //目的港
		ecargoItemObj.put("wndSiteName", toProv); // 目的地名称

		String ydDriver = jsonSource.getString("yd_driver");
		org.json.JSONObject driverObj = Configuration.loadUserDataByClientId(null, ydDriver);

		ecargoItemObj.put("conveyanceName", "汽车 " + driverObj.getString("data:rz#cph")); // 运输工具名称
		ecargoItemObj.put("cargoTotalPrice", hwjz); // 货物总价

		org.json.JSONArray ecargoDetailObj = new org.json.JSONArray(); // 货物标的详细信息
		org.json.JSONObject detailItem = new org.json.JSONObject();
		detailItem.put("goodsDetailName", hwmc); // 货物名称
		detailItem.put("goodsSortDtlCode", hwlb_dm); // 商品详细编码
		if (jsonSource != null)
			detailItem.put("marker", jsonSource.getString("from_city") + "-" + jsonSource.getString("to_city")); // 标记
		else
			detailItem.put("marker", "-");
		detailItem.put("packing", packing); // 包装
		detailItem.put("quantity", hwsl); // 数量

		ecargoDetailObj.put(detailItem);
		ecargoItemObj.put("ecargoDetailObj", ecargoDetailObj);

		/**
		 * 2. insuredObj 投保人信息/被保险人信息
		 */

		// 投保人
		org.json.JSONArray insuredObj = new org.json.JSONArray();
		org.json.JSONObject insureItem1 = new org.json.JSONObject();
		if (parentdept.equals("ZGS410724")) {// 获嘉分公司
			insureItem1.put("identifyNumber", "G10410724000716409"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司获嘉分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS410225")) {// 兰考分公司
			insureItem1.put("identifyNumber", "G1041022500111640C"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "兰考县产业集聚区创新中心E区9号"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司兰考分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS410727")) {// 封丘分公司
			insureItem1.put("identifyNumber", "G1041072700060570P"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司获嘉分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		} else if (parentdept.equals("ZGS454950")) {// 武陟分公司
			insureItem1.put("identifyNumber", "G10410823001010402"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "武陟县汽车总站（龙源路中段）三楼307室"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司武陟分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else if (parentdept.equals("ZGS472000")) {// 三门峡分公司
			insureItem1.put("identifyNumber", "G1041072700060570P"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司获嘉分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else if (parentdept.equals("ZGS471100")) {// 孟津分公司
			insureItem1.put("identifyNumber", "G1041072700060570P"); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", "河南省脱颖实业有限公司获嘉分公司"); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}else{
			insureItem1.put("identifyNumber", conf.get("identifyNumber")); // 个人证件号码/法人组织机构号码
			insureItem1.put("identifyType", conf.get("identifyType")); // 证件类型
			insureItem1.put("insuredAddress", conf.get("insuredAddress")); // 关系人地址
			insureItem1.put("insuredFlag", "1"); // 关系人标志
			insureItem1.put("insuredName", conf.get("insuredName")); // 关系人名称
			insureItem1.put("insuredType", conf.get("insuredType")); // 关系人类型
			// insureItem1.put("mobile", "13000001111"); //移动电话和电话两者至少填写一个
			insureItem1.put("phoneNumber", conf.get("phoneNumber"));
		}
		insuredObj.put(insureItem1);

		// 被保险人
		org.json.JSONObject insureItem2 = new org.json.JSONObject();
		insureItem2.put("identifyNumber", zjhm);
		insureItem2.put("identifyType", zjlx); // 证件类型
		insureItem2.put("insuredAddress", hzdz);
		insureItem2.put("insuredFlag", "2");
		insureItem2.put("insuredName", hzmc);
		insureItem2.put("insuredType", hzlx);
		insureItem2.put("phoneNumber", hzdh);
		insuredObj.put(insureItem2);
		/**
		 * 3. itemKindObj 保险责任信息
		 */
		org.json.JSONArray itemKindObj = new org.json.JSONArray();
		org.json.JSONObject kind1 = new org.json.JSONObject();
		kind1.put("amout", hwjz); // 保险金额/赔偿限额
		kind1.put("kindCode", "0605001"); // 险别代码
		kind1.put("kindFlag", "Y"); // 是否主险
		kind1.put("premium", bf); // 保费
		kind1.put("rate", fl); // 费率
		itemKindObj.put(kind1);

		/*
		 * org.json.JSONObject kind2=new org.json.JSONObject();
		 * kind2.put("amout", hwjz); kind2.put("deductibledetail", "null");
		 * kind2.put("kindCode", "0605001"); kind2.put("kindDetail", "");
		 * kind2.put("kindFlag", "1"); kind2.put("kindName", "国内货运险");
		 * kind2.put("premium", bf); kind2.put("rate", fl);
		 * 
		 * itemKindObj.put(kind2);
		 */
		/**
		 * 4. proposalBaseObj 基本信息表
		 */
		org.json.JSONObject proposalBaseObj = new org.json.JSONObject();
		if (parentdept.equals("ZGS410225")) { // 兰考分公司
			proposalBaseObj.put("appliAddress", "兰考县产业集聚区创新中心E区9号"); // 投保人地址
			proposalBaseObj.put("appliName", "河南省脱颖实业有限公司兰考分公司"); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", "91410225MA44RL264A"); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", "兰考县产业集聚区创新中心E区9号"); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", "上海浦东发展银行"); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000078"); // 银行账号（发票）
		} else if (parentdept.equals("ZGS453300")) { // 封丘分公司
			proposalBaseObj.put("appliAddress", "封丘县尹岗镇镇政府院内"); // 投保人地址
			proposalBaseObj.put("appliName", "河南省脱颖实业有限公司封丘分公司"); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", "91410727MA44KDTU2P"); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", "封丘县尹岗镇镇政府院内"); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", "上海浦东发展银行"); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000054"); // 银行账号（发票）
		} else if (parentdept.equals("ZGS410724")) { // 获嘉分公司
			proposalBaseObj.put("appliAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 投保人地址
			proposalBaseObj.put("appliName", "	河南省脱颖实业有限公司获嘉分公司"); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", "上海浦东发展银行"); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000040"); // 银行账号（发票）
		} else if (parentdept.equals("ZGS454950")) { // 武陟分公司
			proposalBaseObj.put("appliAddress", "武陟县汽车总站（龙源路中段）三楼307室"); // 投保人地址
			proposalBaseObj.put("appliName", "	河南省脱颖实业有限公司武陟分公司"); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", "武陟县汽车总站（龙源路中段）三楼307室"); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", "上海浦东发展银行"); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000095"); // 银行账号（发票）
		} else if (parentdept.equals("ZGS410724")) { // 三门峡分公司
			proposalBaseObj.put("appliAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 投保人地址
			proposalBaseObj.put("appliName", "	河南省脱颖实业有限公司获嘉分公司"); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", "91410724MA44GY2P5W"); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", "河南省获嘉县同盟大道电商大厦5楼"); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", "上海浦东发展银行"); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", "76110078801800000040"); // 银行账号（发票）
		} else {
			proposalBaseObj.put("appliAddress", conf.get("insuredAddress")); // 投保人地址
			proposalBaseObj.put("appliName", conf.get("insuredName")); // 投保人名称
			proposalBaseObj.put("taxpayerIdentityNo", conf.get("taxpayerIdentityNo")); // 纳税人识别号
			proposalBaseObj.put("taxpayerAddress", conf.get("taxpayerAddress")); // 地址（发票）
			proposalBaseObj.put("taxpayerMobile", conf.get("taxpayerMobile")); // 电话（发票）
			proposalBaseObj.put("taxpayerOpenAccount", conf.get("taxpayerOpenAccount")); // 开户行名称（发票）
			proposalBaseObj.put("taxpayerBankNo", conf.get("taxpayerBankNo")); // 银行账号（发票）
		}
		proposalBaseObj.put("isPrintFeeRate", "Y"); // 是否打印保费费率
		proposalBaseObj.put("openBill", "1"); // 开票对象1为投保人
		proposalBaseObj.put("taxpayerId", "1"); // 纳税人身份
		proposalBaseObj.put("piFlag", "1"); // 需开专票
		proposalBaseObj.put("insuredName", hzmc);// 被保险人名称
		proposalBaseObj.put("amountCurrency", "CNY"); // 保额币种 固定值
		proposalBaseObj.put("premium", bf); // 保费
		proposalBaseObj.put("premiumCurrency", "CNY"); // 应收保费币种
		proposalBaseObj.put("startTransportDate", qyrq); // 起运日期
		proposalBaseObj.put("dutyRange", conf.get("dutyRange")); // 责任范围
		proposalBaseObj.put("agreement", conf.get("agreement")); // 特别约定

		/**
		 * 5. userObj 货物标的信息
		 */
		org.json.JSONObject userObj = new org.json.JSONObject();

		userObj.put("name", "0100000001"); // 用户名
		userObj.put("password", "0000"); // 密码
		userObj.put("transGUID", tradeNo); // 交易流水号
		userObj.put("cooperation", coop); // 合作方

		cargoProposalObj.put("ecargoItemObj", ecargoItemObj);
		cargoProposalObj.put("insuredObj", insuredObj);
		cargoProposalObj.put("itemKindObj", itemKindObj);
		cargoProposalObj.put("proposalBaseObj", proposalBaseObj);
		cargoProposalObj.put("userObj", userObj);

		// operation 操作类型
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
		java.util.Enumeration<?> enum1 = (java.util.Enumeration<?>) pps.propertyNames();// 得到配置文件的名字
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
			org.json.JSONObject jsonParams = BXHelper.packRequest(token, coop, tradeNo, "河南", invoceNo, "111-444-222", "安徽", "300000", "家具", "01", "是", "30立方", "1234.11", "0.035", qyrq, "zjlx",
					"zjhm", "hzdz", "hzmc", "hzlx", "hzdh", conf, null, "");
			System.out.println("" + jsonParams);
			// String
			// strReturn=Tools.post("http://180.168.131.21/uiep/ecargo/insuranceService",params);
			// org.json.JSONObject jsonRet=new org.json.JSONObject(strReturn);

			// System.out.println("\t保险处理返回:"+jsonRet);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
