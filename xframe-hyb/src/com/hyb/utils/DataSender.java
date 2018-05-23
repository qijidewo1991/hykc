package com.hyb.utils;

import java.util.Properties;

import org.apache.hadoop.hbase.client.HConnection;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.wondersgroup.cuteinfo.client.exchangeserver.exchangetransport.dao.IMessageTransporterDAO;
import com.wondersgroup.cuteinfo.client.exchangeserver.exchangetransport.factory.ITransportClientFactory;
import com.wondersgroup.cuteinfo.client.exchangeserver.exchangetransport.impl.USendRequset;
import com.wondersgroup.cuteinfo.client.exchangeserver.exchangetransport.impl.USendResponse;
import com.wondersgroup.cuteinfo.client.exchangeserver.usersecurty.UserToken;
import com.wondersgroup.cuteinfo.client.util.UserTokenUtils;
import com.xframe.core.CoreServlet;
import com.xframe.utils.AppHelper;
import com.xframe.utils.HBaseUtils;
import com.xframe.utils.XHelper;

public class DataSender {
	private static final Logger log = Logger.getLogger(DataSender.class);

	/**
	 * 电子录单
	 * 
	 * @param rowid
	 * @param driverId
	 */
	public static void addElecReceipt(String rowid, String driverId) {
		HConnection connection = HBaseUtils.getHConnection();
		try {
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			// org.json.JSONObject
			// hzObj=Configuration.loadUserDataByClientId(connection, hzId);
			org.json.JSONObject driverObj = Configuration.loadUserDataByClientId(connection, driverId);
			if (!driverObj.has("data:rz#cplx")) {
				log.info("司机信息不完整:" + driverId);
				return;
			}

			if (!row.has("data:business")) {
				log.info("运单信息不完整:" + rowid);
				return;
			}

			java.util.HashMap<String, String> conf = AppHelper.getSystemConf();

			org.json.JSONObject data = new org.json.JSONObject();
			java.util.Date now = new java.util.Date();
			data.put("MessageReferenceNumber", org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS", now));
			data.put("DocumentVersionNumber", conf.get("DocumentVersionNumber"));
			data.put("SenderCode", conf.get("SenderCode"));
			data.put("RecipientCode", conf.get("RecipientCode"));
			data.put("MessageSendingDateTime", org.mbc.util.Tools.formatDate("yyyyMMddHHmmss", now));

			data.put("OriginalDocumentNumber", row.getString("data:sid"));
			data.put("ShippingNoteNumber", row.getString("data:sid"));
			// data.put("Carrier", driverObj.getString("data:rz#xm"));
			data.put("Carrier", "河南省脱颖实业有限公司");
			data.put("ConsignmentDateTime", row.getString("data:yd_1_time"));
			data.put("BusinessTypeCode", row.getString("data:business"));
			data.put("DespatchActualDateTime", row.getString("data:yd_1_time"));
			data.put("GoodsReceiptDateTime", row.getString("data:yd_2_time"));

			data.put("CountrySubdivisionCode_fh", row.getString("data:from_code"));
			data.put("CountrySubdivisionCode_sh", row.getString("data:to_code"));

			data.put("TotalMonetaryAmount", AppUtil.formatAmountNoGroup(row.getDouble("data:yf"), 3));
 //徐超
			data.put("LicensePlateTypeCode", driverObj.getString("data:rz#cplx"));
			data.put("VehicleNumber", driverObj.get("data:rz#cph"));
			data.put("VehicleClassificationCode", driverObj.getString("data:rz#clfl"));
			data.put("VehicleTonnage", AppUtil.formatAmountNoGroup(driverObj.getDouble("data:rz#zz")));
			data.put("RoadTransportCertificateNumber", driverObj.getString("data:rz#dlysz"));
			data.put("DescriptionOfGoods", row.getString("data:hwmc"));
			data.put("CargoTypeClassificationCode", row.getString("data:category"));
			double weight = row.getDouble("data:hwzl");
			weight = weight * 1000;
			data.put("GoodsItemGrossWeight", AppUtil.formatAmountNoGroup(weight, 3));

			data.put("yd_id", rowid);

			String rid = data.getString("MessageReferenceNumber");
			// 判断路单是否完整准确
			String inspectresult = ldInspect(data);

			if (inspectresult.length() > 0) {
				data.put("inspect", inspectresult);
				XHelper.createRow(connection, "elec_receipts", rid, data);
			} else {
				XHelper.createRow(connection, "elec_receipts", rid, data);
				String strMessage = getMessageToXML(data);
				execSend(conf, strMessage, rid, "ludan");
				addCapitalReceipt(rowid, driverId);//发送资金流对接
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}
	}

	public static void addCapitalReceipt(String rowid, String driverId) {
		// 第一步 拿出需要的信息 并 记录到数据库
		HConnection connection = HBaseUtils.getHConnection();
		try {
			org.json.JSONObject row = XHelper.loadRow(connection, rowid, "yd_list", true);
			org.json.JSONObject argDesc = new org.json.JSONObject("{\"data:balance\":\"" + HBaseUtils.TYPE_NUMBER + "\"}");
			argDesc.put("data:change_amount", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:balance_rest", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:balance", HBaseUtils.TYPE_NUMBER);
			argDesc.put("data:order_amount", HBaseUtils.TYPE_NUMBER);
			org.json.JSONArray f = new org.json.JSONArray();
			org.json.JSONObject c1 = new org.json.JSONObject();
			org.json.JSONObject c2 = new org.json.JSONObject();
			org.json.JSONObject c3 = new org.json.JSONObject();
			c1.put("name", "data:type");
			c1.put("oper", "=");
			c1.put("value", "FEEPF");
			c2.put("name", "data:account_type");
			c2.put("oper", "=");
			c2.put("value", "mobile");//
			c3.put("name", "data:source_id");
			c3.put("oper", "=");
			c3.put("value", rowid);
			f.put(c1);
			f.put(c2);
			f.put(c3);
			org.json.JSONArray rows = com.xframe.utils.XHelper.listRowsByFilter("account_detail", "", f, false, argDesc);
			org.json.JSONObject driverObj = Configuration.loadUserDataByClientId(connection, driverId);
			if (!driverObj.has("data:rz#cplx")) {
				log.info("司机信息不完整:" + driverId);
				return;
			}

			if (!row.has("data:business")) {
				log.info("运单信息不完整:" + rowid);
				return;
			}

			java.util.HashMap<String, String> conf = AppHelper.getSystemConf();
			org.json.JSONObject data = new org.json.JSONObject();
			java.util.Date now = new java.util.Date();
			data.put("type", "Capital");
			data.put("MessageReferenceNumber", java.util.UUID.randomUUID().toString().replace("-", ""));// 单据唯一id
			data.put("SenderCode", conf.get("SenderCode"));
			data.put("RecipientCode", conf.get("RecipientCode"));
			data.put("MessageSendingDateTime", org.mbc.util.Tools.formatDate("yyyyMMddHHmmss", now));
			data.put("DocumentNumber", "Capital" + org.mbc.util.Tools.formatDate("yyyyMMddHHmmssS", now));// 本单资金流水号
			data.put("Carrier", driverObj.getString("data:rz#xm"));// 司机姓名
			data.put("VehicleNumber", driverObj.get("data:rz#cph"));// 车牌号			
			data.put("LicensePlateTypeCode", driverObj.getString("data:rz#cplx").length()>1?driverObj.getString("data:rz#cplx"):"0"+driverObj.getString("data:rz#cplx").length());// 车牌类型代码
			data.put("ShippingNoteNumber", row.getString("data:sid"));// 托运单号路单id
			data.put("Remark", rowid);// 备注
			data.put("SequenceCode", rows.getJSONObject(0).getString("order_no"));// 银行或第三方支付平台的资金流水单号，现金等其他方式可填财务记账号
			data.put("MonetaryAmount", AppUtil.formatAmountNoGroup(row.getDouble("data:yf"), 3));// 运费
			data.put("DateTime", org.mbc.util.Tools.formatDate("yyyyMMddHHmmss", now));// 资金发生时间
			String rid = data.getString("MessageReferenceNumber");
			XHelper.createRow(connection, "elec_receipts", rid, data);
			String strMessage = getMessageToXML_Capital(data);
			execSend(conf, strMessage, rid, "zijin");
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			HBaseUtils.closeConnection(connection);
		}

	}

	/**
	 * 返回值若不是空“” 就是不符合
	 * 
	 * @param data
	 * @return
	 * @throws JSONException
	 */
	private static String ldInspect(JSONObject data) throws JSONException {
		String result = "";
		// 报文参考号
		if (!data.has("MessageReferenceNumber")) {
			result = result + "无MessageReferenceNumber,";
		} else {
			if (!lengthInspect(data.get("MessageReferenceNumber").toString(), 35)) {
				result = result + "MessageReferenceNumber长度,";
			}
		}
		// 报文版本号判断最大17
		if (!data.has("DocumentVersionNumber")) {
			result = result + "无DocumentVersionNumber,";
		} else {
			if (!lengthInspect(data.get("DocumentVersionNumber").toString(), 35)) {
				result = result + "DocumentVersionNumber长度,";
			}
		}
		// // 发送方代码判断等于
		// if (!data.has("SenderCode")) {
		// result = result + "无SenderCode,";
		// } else {
		// if (data.get("SenderCode").toString() != "13982") {
		// result = result + "SenderCode不是13982,";
		// }
		// }
		// // 接收方代码判断等于14057
		// if (!data.has("RecipientCode")) {
		// result = result + "无RecipientCode,";
		// } else {
		// if (data.get("RecipientCode").toString() != "14057") {
		// result = result + "RecipientCode不是14057,";
		// }
		// }
		// 发送日期时间判断
		if (!data.has("MessageSendingDateTime")) {
			result = result + "无MessageSendingDateTime,";
		} else {
			if (!lengthInspect(data.get("MessageSendingDateTime").toString(), 14)) {
				result = result + "MessageSendingDateTime长度,";
			}
		}
		// 原始单号判断最长35
		if (!data.has("OriginalDocumentNumber")) {
			result = result + "无OriginalDocumentNumber,";
		} else {
			if (!lengthInspect(data.get("OriginalDocumentNumber").toString(), 35)) {
				result = result + "OriginalDocumentNumber长度,";
			}
		}
		// 托运单号判断最长20
		if (!data.has("ShippingNoteNumber")) {
			result = result + "无ShippingNoteNumber,";
		} else {
			if (!lengthInspect(data.get("ShippingNoteNumber").toString(), 20)) {
			result = result + "ShippingNoteNumber长度,";
			}
		}
		// 托运日期时间判断
		if (!data.has("ConsignmentDateTime")) {
			result = result + "无ConsignmentDateTime,";
		} else {
			if (!lengthInspect(data.get("ConsignmentDateTime").toString(), 19)) {
				result = result + "ConsignmentDateTime长度,";
			}
		}
		// 业务类型代码判断最长7
		if (!data.has("BusinessTypeCode")) {
			result = result + "无BusinessTypeCode,";
		} else {
			if (!lengthInspect(data.get("BusinessTypeCode").toString(), 7)) {
				result = result + "BusinessTypeCode长度,";
			}
		}
		// 发运实际日期时间判断
		if (!data.has("DespatchActualDateTime")) {
			result = result + "无DespatchActualDateTime,";
		} else {
			if (!lengthInspect(data.get("DespatchActualDateTime").toString(), 19)) {
				result = result + "DespatchActualDateTime长度,";
			}
		}
		// 收货日期时间判断
		if (!data.has("GoodsReceiptDateTime")) {
			result = result + "无GoodsReceiptDateTime,";
		} else {
			if (!lengthInspect(data.get("GoodsReceiptDateTime").toString(), 19)) {
				result = result + "GoodsReceiptDateTime长度,";
			}
		}
		// 国家行政区划代码出发地判断
		if (!data.has("CountrySubdivisionCode_fh")) {
			result = result + "无CountrySubdivisionCode_fh,";
		} else {
			if (!lengthInspect(data.get("CountrySubdivisionCode_fh").toString(), 12)) {
				result = result + "CountrySubdivisionCode_fh长度,";
			}
		}
		// 国家行政区划代码目的地判断
		if (!data.has("CountrySubdivisionCode_sh")) {
			result = result + "无CountrySubdivisionCode_sh,";
		} else {
			if (!lengthInspect(data.get("CountrySubdivisionCode_sh").toString(), 12)) {
				result = result + "CountrySubdivisionCode_sh长度,";
			}
		}
		// 货币总金额地判断 最长18 保留三位小数
		if (!data.has("TotalMonetaryAmount")) {
			result = result + "无TotalMonetaryAmount,";
		} else {
			if (!lengthInspect(data.get("TotalMonetaryAmount").toString(), 18)) {
				result = result + "TotalMonetaryAmount长度,";
			} else {
				if (!xiaoshuInspect(data.get("TotalMonetaryAmount").toString(), 3)) {
					result = result + "TotalMonetaryAmount小数格式不符合,";
				}
			}
		}
		// 牌照类型代码判断 最长20
		if (!data.has("LicensePlateTypeCode")) {
			result = result + "无LicensePlateTypeCode,";
		} else {
			if (!lengthInspect(data.get("VehicleNumber").toString(), 20)) {
				result = result + "LicensePlateTypeCode长度,";
			}
		}
		// 车辆牌照号判断 最长35
		if (!data.has("VehicleNumber")) {
			result = result + "无VehicleNumber,";
		} else {
			if (!lengthInspect(data.get("VehicleNumber").toString(), 35)) {
				result = result + "VehicleNumber长度,";
			}
		}
		// 车辆分类代码判断 最长12
		if (!data.has("VehicleClassificationCode")) {
			result = result + "无VehicleClassificationCode,";
		} else {
			if (!lengthInspect(data.get("VehicleClassificationCode").toString(), 12)) {
				result = result + "VehicleClassificationCode长度,";
			}
		}
		// 车辆载质量判断 最长9 两位小数
		if (!data.has("VehicleTonnage")) {
			result = result + "无VehicleTonnage,";
		} else {
			if (!lengthInspect(data.get("VehicleTonnage").toString(), 9)) {
				result = result + "VehicleTonnage长度,";
			} else {
				if (!xiaoshuInspect(data.get("VehicleTonnage").toString(), 2)) {
					result = result + "VehicleTonnage小数格式不符合,";
				}
			}
		}
		// 道路运输证号判断 最长12
		if (!data.has("RoadTransportCertificateNumber")) {
			result = result + "无RoadTransportCertificateNumber,";
		} else {
			if (!lengthInspect(data.get("RoadTransportCertificateNumber").toString(), 12)) {
				result = result + "RoadTransportCertificateNumber长度,";
			}
		}
		// 货物名称判断 最长512
		if (!data.has("DescriptionOfGoods")) {
			result = result + "无DescriptionOfGoods,";
		} else {
			if (!lengthInspect(data.get("DescriptionOfGoods").toString(), 512)) {
				result = result + "DescriptionOfGoods长度,";
			}
		}
		// 货物类型分类代码判断 最长3
		if (!data.has("CargoTypeClassificationCode")) {
			result = result + "无CargoTypeClassificationCode,";
		} else {
			if (!lengthInspect(data.get("CargoTypeClassificationCode").toString(), 3)) {
				result = result + "CargoTypeClassificationCode长度,";
			}
		}
		// 货物项毛重判断 最长14 三位小数
		if (!data.has("GoodsItemGrossWeight")) {
			result = result + "无GoodsItemGrossWeight,";
		} else {
			if (data.get("GoodsItemGrossWeight").toString().length() > 14) {
				result = result + "GoodsItemGrossWeight长度超过14,";
			} else {
				if (!xiaoshuInspect(data.get("GoodsItemGrossWeight").toString(), 3)) {
					result = result + "GoodsItemGrossWeight小数格式不符合,";
				}
			}
		}
		return result;

	}

	/**
	 * 长度除去空格后不为0且小于最大长度的时候返回true
	 * 
	 * @param str
	 * @param ma最大长度
	 * @return
	 */
	private static boolean lengthInspect(String str, int ma) {
		if (str.length() > ma) {
			return false;
		} else {
			str = str.trim();
			if (str.length() == 0) {
				return false;
			} else {
				return true;
			}

		}

	}

	/**
	 * 小数判断，必须符合纯数字，除了. 并且只有一个. 开头不能为0
	 * 
	 * @param str
	 * @param i
	 *            保留几位小数
	 * @return 符合是true 不符合是false
	 */
	private static boolean xiaoshuInspect(String str, int i) {
		if (str.indexOf(".") >= 0 && str.indexOf("0") != 0) {
			String[] strs = str.split("\\.");
			if (strs.length != 2) {
				return false;
			} else {
				if (strs[1].length() == i) {
					if (strs[0].matches("[0-9]+") && strs[1].matches("[0-9]+")) {
						return true;
					} else {
						return false;
					}
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	private static String formatDateField(String strIn) {
		String strTmp = strIn.replaceAll("\\-", "");
		strTmp = strTmp.replaceAll("\\:", "");
		strTmp = strTmp.replaceAll(" ", "");
		return strTmp;
	}

	private static String getMessageToXML(org.json.JSONObject raw) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<Root>");
		sb.append("<Header>");
		sb.append("<MessageReferenceNumber>" + raw.getString("MessageReferenceNumber") + "</MessageReferenceNumber>");
		sb.append("<DocumentName>无车承运人电子路单</DocumentName>");
		sb.append("<DocumentVersionNumber>" + raw.getString("DocumentVersionNumber") + "</DocumentVersionNumber>");// 报文版本号、定值
		sb.append("<SenderCode>" + raw.getString("SenderCode") + "</SenderCode>");// 发送方代码
		sb.append("<RecipientCode>" + raw.getString("RecipientCode") + "</RecipientCode>");// 接收方代码
																							// 、定值
																							// ,14057是测试，正式的为wcjc0001
		sb.append("<MessageSendingDateTime>" + raw.getString("MessageSendingDateTime") + "</MessageSendingDateTime>");// 发送日期时间
		sb.append("<MessageFunctionCode>9</MessageFunctionCode>");// 报文功能代码、定值
		sb.append("</Header>");
		sb.append("<Body>");
		sb.append("<OriginalDocumentNumber>" + raw.getString("OriginalDocumentNumber") + "</OriginalDocumentNumber>");// 原始单号
		sb.append("<ShippingNoteNumber>" +raw.getString("ShippingNoteNumber")+ "</ShippingNoteNumber>");// 托运单号
		sb.append("<Carrier>" + raw.getString("Carrier") + "</Carrier>");// 承运人
		sb.append("<ConsignmentDateTime>" + formatDateField(raw.getString("ConsignmentDateTime")) + "</ConsignmentDateTime>");// 托运日期时间
		sb.append("<BusinessTypeCode>" + raw.getString("BusinessTypeCode") + "</BusinessTypeCode>");// 业务类型代码
		sb.append("<DespatchActualDateTime>" + formatDateField(raw.getString("DespatchActualDateTime")) + "</DespatchActualDateTime>");// 发运实际日期时间
		sb.append("<GoodsReceiptDateTime>" + formatDateField(raw.getString("GoodsReceiptDateTime")) + "</GoodsReceiptDateTime>");// 收货日期时间
		sb.append("<ConsignorInfo>");// 发货方信息
		// sb.append( "<Consignor>发货人</Consignor>");//选填、发货人
		// sb.append(
		// "<PersonalIdentityDocument>411303199209136738</PersonalIdentityDocument>");//选填、个人证件号
		// sb.append( "<PlaceOfLoading>测试地点</PlaceOfLoading>");//选填、装货地点
		sb.append("<CountrySubdivisionCode>" + raw.getString("CountrySubdivisionCode_fh") + "</CountrySubdivisionCode>");// 国家行政区划代码
		sb.append("</ConsignorInfo>");
		sb.append("<ConsigneeInfo>");// 收货方信息
		// sb.append( "<GoodsReceiptPlace>测试地点2</GoodsReceiptPlace>");//选填、收货地点
		sb.append("<CountrySubdivisionCode>" + raw.getString("CountrySubdivisionCode_sh") + "</CountrySubdivisionCode>");// 国家行政区划代码
		sb.append("</ConsigneeInfo>");
		sb.append("<PriceInfo>");// 费用信息
		sb.append("<TotalMonetaryAmount>" + raw.getString("TotalMonetaryAmount") + "</TotalMonetaryAmount>");// 货币总金额
		sb.append("</PriceInfo>");
		sb.append("<VehicleInfo>");// 车辆信息
		sb.append("<LicensePlateTypeCode>" + (raw.getString("LicensePlateTypeCode").length() > 1 ? raw.getString("LicensePlateTypeCode") : "0" + raw.getString("LicensePlateTypeCode"))
				+ "</LicensePlateTypeCode>");// 牌照类型代码
		sb.append("<VehicleNumber>" + raw.getString("VehicleNumber") + "</VehicleNumber>");// 车辆牌照号
		sb.append("<VehicleClassificationCode>" + raw.getString("VehicleClassificationCode") + "</VehicleClassificationCode>");// 车辆分类代码
		sb.append("<VehicleTonnage>" + raw.getString("VehicleTonnage") + "</VehicleTonnage>");// 车辆载质量
		sb.append("<RoadTransportCertificateNumber>" + raw.getString("RoadTransportCertificateNumber") + "</RoadTransportCertificateNumber>");// 道路运输证号
		sb.append("<GoodsInfo>");// 货物信息
		sb.append("<DescriptionOfGoods>" + raw.getString("DescriptionOfGoods") + "</DescriptionOfGoods>");// 货物名称
		sb.append("<CargoTypeClassificationCode>" + raw.getString("CargoTypeClassificationCode") + "</CargoTypeClassificationCode>");// 货物类型分类代码

		sb.append("<GoodsItemGrossWeight>" + raw.getString("GoodsItemGrossWeight") + "</GoodsItemGrossWeight>");// 货物项毛重
		// sb.append( "<Cube>1.000</Cube>");//选填、体积
		sb.append("</GoodsInfo>");
		sb.append("</VehicleInfo>");
		sb.append("</Body>");
		sb.append("</Root>");
		return sb.toString();
	}

	public static String getMessageToXML_Capital(org.json.JSONObject data) throws JSONException {
		String xml = "";
		xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml = xml + "<Root>";
		xml = xml + "<Header>";
		xml = xml + " <MessageReferenceNumber>" + data.getString("MessageReferenceNumber") + "</MessageReferenceNumber>";// 唯一单据id
																															// //
																															// 35位
		xml = xml + "<DocumentName>资金流水单</DocumentName>";
		xml = xml + "<DocumentVersionNumber>2015WCCYR</DocumentVersionNumber>";
		xml = xml + "<SenderCode>" + data.getString("SenderCode") + "</SenderCode>";
		xml = xml + "<RecipientCode>" + data.getString("RecipientCode") + "</RecipientCode>";
		xml = xml + "<MessageSendingDateTime>" + data.getString("MessageSendingDateTime") + "</MessageSendingDateTime>";
		xml = xml + "<MessageFunctionCode>9</MessageFunctionCode>";// 报文功能代码
																	// 9最新的
		xml = xml + "</Header>";
		xml = xml + "<Body>";
		xml = xml + "<DocumentNumber>" + data.getString("DocumentNumber") + "</DocumentNumber>";// 资金流水号单号35位
		xml = xml + "<Carrier>" + data.getString("Carrier") + "</Carrier>";// 司机姓名
		xml = xml + "<VehicleNumber>" + data.getString("VehicleNumber") + "</VehicleNumber>";// 车牌号
		xml = xml + "<LicensePlateTypeCode>" + data.getString("LicensePlateTypeCode") + "</LicensePlateTypeCode>";// 车牌类型代码
		xml = xml + "<ShippingNoteList>";
		xml = xml + "<ShippingNoteNumber>" + data.getString("ShippingNoteNumber") + "</ShippingNoteNumber>";// 托运单号，为路单单号
		xml = xml + "<Remark>" + data.getString("Remark") + "</Remark>";// 备注
		xml = xml + "</ShippingNoteList>";

		xml = xml + "<Financiallist>";
		xml = xml + "<PaymentMeansCode>42</PaymentMeansCode>";// 付款方式 9其他
//		xml = xml + "<BankCode>9999</BankCode>";
		xml = xml + "<SequenceCode>" + data.getString("SequenceCode") + "</SequenceCode>";// 银行或第三方支付平台的资金流水单号，现金等其他方式可填财务记账号
		xml = xml + "<MonetaryAmount>" + data.getString("MonetaryAmount") + "</MonetaryAmount>";// 资金流水金额
																									// 默认人民币
		xml = xml + "<DateTime>" + data.getString("DateTime") + "</DateTime>";// 资金流水实际发生时间YYYYMMDDHHMMSS
		xml = xml + "</Financiallist>";
		xml = xml + "</Body>";
		xml = xml + "</Root>";
		return xml;
	}

	private static void execSend(final java.util.HashMap<String, String> conf, final String strMsg, final String rid, final String type) {
		new Thread() {
			public void run() {
				for (int i = 0; i < 3; i++) { // 重试33次，成功为止

					boolean isSuccess = invokeSenderThread(conf, strMsg, type);
					if (isSuccess) {

						// 添加成功标志
						org.json.JSONObject data = new org.json.JSONObject();
						try {
							data.put("send_time", org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date()));
							XHelper.createRow("elec_receipts", rid, data);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						break;
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public static boolean resendReceipt(String rowid) throws JSONException {
		org.json.JSONObject row = XHelper.loadRow(null, rowid, "elec_receipts", false);
		String inspectresult = ldInspect(row);
		String strMessage = "";
		//System.out.println("row=========" + ldInspect(row));
		boolean isSuccess = false;
		if (inspectresult.length() > 0) {
			org.json.JSONObject data = new org.json.JSONObject();
			isSuccess = false;
			data.put("inspect", inspectresult);
			data.put("send_time", "失败");
			XHelper.createRow("elec_receipts", rowid, data);
		} else {
			try {
				strMessage = getMessageToXML(row);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			java.util.HashMap<String, String> conf = AppHelper.getSystemConf();
			isSuccess = invokeSenderThread(conf, strMessage, "ludan");
			
		}
		try {

			if (isSuccess) {
				String yd_rowid=com.xframe.utils.XHelper.loadRow(rowid,"elec_receipts").getString("yd_id");
				String driverId=com.xframe.utils.XHelper.loadRow(yd_rowid,"yd_list").getString("yd_driver");
				addCapitalReceipt(yd_rowid, driverId);//发送资金流对接
				// 添加成功标志
				org.json.JSONObject data = new org.json.JSONObject();
				try {
					data.put("send_time", org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date()));
					XHelper.createRow("elec_receipts", rowid, data);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return isSuccess;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	private static boolean invokeSenderThread(java.util.HashMap<String, String> conf, String strMsg, String type) {
		// System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		// String truststoreFile = CoreServlet.CONF_ROOT + "/"
		// + "cuteinfo_client.trustStore";
		// System.setProperty("javax.net.ssl.trustStore", truststoreFile);
		// System.out.println(truststoreFile);
		// 读取配置文件
		String targetURL = conf.get("targetURL");
		// String securityURL = conf.get("securityURL");

		IMessageTransporterDAO transporter = null;
		try {
			// 首先需要通过平台的认证服务，使用物流交换代码和密码取得令牌。有两种方式，选一种即可
			// 第一种方式，调用数据交换的令牌认证服务
			// UserSecurityClient
			// security=UserSecurityClientFactory.createUserSecurityClient(securityURL);
			// UserToken
			// token=security.authenticate(properties.getProperty("username"),
			// properties.getProperty("password"));
			// System.out.println("tokenid:"+token.getTokenID());
			// 第二种方式，调用统一认证的令牌认证服务
			UserToken token = UserTokenUtils.getTicket(conf.get("username"), conf.get("password"), conf.get("resourceId"), conf.get("authURL"));
			System.out.println("tokenid:" + token.getTokenID());

			// 接下来需要设置目标地址，发送报文的内容，包括报文类型Actiontype和具体报文xml
			USendRequset sendReq = new USendRequset();
			sendReq.setToaddress(conf.get("toaddress").split(","));

			// 设置待发送的附件
			// File f = new File("c:\\warning.sql");
			// System.out.println("[" + f.getName() + "]文件大小为：" +
			// FileUtils.convertFileSize(f.length()));
			// sendReq.setFile(f);

			// sendReq.setSendRequestTypeSimpleData("ZJWL_LOGINK_BZGL_DataUnitDownloadRequest",
			// "DataElementID", "3229");

			// sendReq.setSendRequestTypeFILE("ZJWL_LOGINK_FAQ_InfoIssueRequest",
			// FileUtils.toByteArray("c:\\warning.sql"));

			// 设置待发送的业务报文
			if (type.equals("ludan")) {
				sendReq.setSendRequestTypeXML("LOGINK_CN_FREIGHTBROKER_WAYBILL", strMsg);
			} else if (type.equals("zijin")) {
				sendReq.setSendRequestTypeXML("LOGINK_CN_FREIGHTCHARGES", strMsg);
			}

			// 最后，调用平台提供的发送服务发送报文
			transporter = ITransportClientFactory.createMessageTransporter(token, targetURL);

			long start = System.currentTimeMillis();
			USendResponse response = transporter.send(sendReq);
			long end = System.currentTimeMillis();
			System.out.println("发送耗时：" + (end - start) + "毫秒，报文长度" + strMsg.getBytes().length);

			if (response.isSendResult()) {
				System.out.println("send success");
				return true;
			} else {
				// 错误的情况下，会返回异常代码以及异常信息。异常代码请参照《3.2 共建指导性文件：交换接入》中的异常代码信息
				System.out.println("send error");
				System.out.println(response.getGenericFault().getCode());
				System.out.println(response.getGenericFault().getMessage());
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
