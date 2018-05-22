package com.zhiyun.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.alibaba.fastjson.JSON;
import com.hyb.utils.RedisClient;
import com.openapi.sdk.service.DataExchangeService;
import com.openapi.sdk.service.TransCode;
import com.zhiyun.config.ZhiyunConfig;

public class ZyApi {
	static String client_id = "";
	static String apiUrl = "";
	static String token = "";
	static String apiUser = "";
	static String password = "";
	public static RedisClient redis = RedisClient.getInstance(""+ZhiyunConfig.redisurl);// "10.1.1.3:7000,10.1.1.4:7000,10.1.1.38:7000"

	public ZyApi() {
		client_id = ZhiyunConfig.zhiyun_client_id;
		apiUrl = ZhiyunConfig.zhiyun_apiUrl;
		apiUser = ZhiyunConfig.zhiyun_apiUser;
		password = ZhiyunConfig.zhiyun_password;

		// DemoReturnBean bean = login();
		// token = bean.getResult() + "";

		token = redis.getAtList("zhiyun_token", 0) != null ? redis.getAtList("zhiyun_token", 0) : "";
		System.out.println("******************zytoken=" + token);
		Date date1 = null;
		Date date2 = null;
		String[] tokenar = null;
		String date = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		date = sdf.format(new Date());
		if (token != "") {
			tokenar = token.split(",");
			String token_time = tokenar[1];
			try {
				date1 = sdf.parse(date);
				date2 = sdf.parse(token_time);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (token == "" || daysBetween(date2, date1) >= 1) {// 要求三天一换token
															// 这里设置为2天 防止特殊情况发生
			DemoReturnBean bean = login();
			token = bean.getResult() + "";
			redis.delStr("zhiyun_token");
			redis.addToList("zhiyun_token", token + "," + date);
			System.out.println("智运token超过1天"+ token + "," + date);
		} else {

			token = tokenar[0];
		}

	}

	/**
	 * 计算相差天数
	 * 
	 * @param smdate
	 *            之前
	 * @param bdate
	 *            之后
	 * @return
	 */
	public static int daysBetween(Date smdate, Date bdate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			smdate = sdf.parse(sdf.format(smdate));
			bdate = sdf.parse(sdf.format(bdate));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(smdate);
		long time1 = cal.getTimeInMillis();
		cal.setTime(bdate);
		long time2 = cal.getTimeInMillis();
		long between_days = (time2 - time1) / (1000 * 3600 * 24);

		return Integer.parseInt(String.valueOf(between_days));
	}

	/**
	 * 查询车辆24小时内的最新位置
	 * 
	 * @param cph
	 *            车牌号
	 * @return 
	 *         {"result":{"adr":"安徽省安庆市怀宁县长琳塑业，向西方向，148米","drc":"225","lat":"18451089"
	 *         ,"lon":"70094469","spd":"73.0","utc":"1496826420000","province":
	 *         "安徽省","city":"安庆市","country":"怀宁县"},"status":1001}
	 */
	public JSONObject getLastLocation(String cph) {
		String res = "";
		JSONObject json = new JSONObject();

		try {
			json.put("success", "false");
			String p = "token=" + token + "&vclN=" + cph + "&timeNearby=24";
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/vLastLocationV3/" + p + "?client_id=" + client_id;
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			// System.out.println("返回:" + res);
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);
			if (jb.get("success").equals("false")) {
				json.put("info", jb.get("info"));
				return json;
			} else {
				json.put("success", "true");
				json.put("info", res);
			}
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
		}
		return json;

	}

	/**
	 * 车辆轨迹查询（车牌号）
	 * 
	 * @param cph
	 *            车牌号
	 * @param qryBtm
	 *            开始时间 2017-05-03 01:00:00
	 * @param qryEtm
	 *            结束时间 2017-05-03 01:00:00
	 * @return
	 */
	public JSONObject geTtrace(String cph, String qryBtm, String qryEtm) {
		String res = "";
		JSONObject json = new JSONObject();
		try {
			json.put("success", "false");
			String p = "token=" + token + "&vclN=" + cph + "&qryBtm=" + qryBtm + "&qryEtm=" + qryEtm;
			// System.out.println("车辆轨迹查询（车牌号）接口，参数:" + p);
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/vHisTrack24/" + p + "?client_id=" + client_id;
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			// System.out.println("返回:" + res);
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);
			if (jb.get("success").equals("false")) {
				json.put("info", jb.get("info"));
				return json;
			} else {
				json.put("success", "true");
				json.put("info", res);
			}
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
		}
		return json;
	}

	/**
	 * 车辆入网验证
	 * 
	 * @param cph
	 *            车牌号
	 * @return 返回结果 {"result":"yes","status":1001}
	 */
	public JSONObject checkTruckExist(String cph) {
		String res = "";
		JSONObject json = new JSONObject();
		try {
			json.put("success", "false");
			String p = "token=" + token + "&vclN=" + cph;
			// System.out.println("车辆入网验证,参数:" + p);
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/checkTruckExist/" + p + "?client_id=" + client_id;
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			// System.out.println("返回:" + res);
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);// 解析接口返回状态
			if (jb.get("success").equals("false")) {
				json.put("info", jb.get("info"));
				return json;
			} else {
				json.put("success", "true");
				json.put("info", res);
			}
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
		}
		return json;
	}

	/**
	 * 找车接口->多维度找车 提供按始发地、目的地、周边距离、货物位置、常运货物类型、车辆类型、车长、载重等信息进行精细化找车查询服务
	 * 本接口提供按照车型、车长、载重、起始地、目的地、多维度查询指定经纬度坐标点为中心查询指定公里范围内的前50辆在线车辆。
	 * 
	 * @param lon
	 * @param lat
	 * @return 示例：{"result":[{"img":
	 *         "https://vims.sinoiov.com/images/20160608/1465374511927_3.jpg"
	 *         ,"lat":"34.657181","lon":"88.629231","ratifyLoad":"7000.0","utc":
	 *         "1477717360000"
	 *         ,"vcltype":"栏板车","vco":"3","vehicleLength":"3500.0"
	 *         ,"vid":"4864052000021002994","vno":"陕X***31"}],"status":1001}
	 */
	public JSONObject queryVclByMulFs(String lon, String lat, String type) {
		String res = "";
		JSONObject json = new JSONObject();
		switch (type) {
		case "栏板车":
			type="1";
			break;
		case "高栏车":
			type="2";
			break;
		case "厢式货车":
			type="3";
			break;
		case "自卸车":
			type="4";
			break;
		case "冷藏车":
			type="5";
			break;
		case "混凝土车":
			type="6";
			break;
		case "平板车":
			type="7";
			break;
		case "其他":
			type="8";
			break;
		default:
			type="1";
			break;
		}
		try {
			json.put("success", "false");
			String p = "token=" + token + "&lon=" + lon + "&lat=" + lat + "&type="+type;
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/queryVclByMulFs/" + p + "?client_id=" + client_id;
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);// 解析接口返回状态
			if (jb.get("success").equals("false")) {
				json.put("info", jb.get("info"));
				return json;
			} else {
				json.put("success", "true");
				json.put("info", res);
			}
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
		}
		return json;
	}

	/**
	 * @param vid
	 *            车辆id
	 * @return {"result":{"platecolorid":"2","vehicleOwnerName":"宋慧乔",
	 *         "vehicleOwnerPhone"
	 *         :"13800138000","vehicleno":"陕XB0031"},"status":1001}
	 */
	public JSONObject queryInfoByVid(String vid) {
		String res = "";
		JSONObject json = new JSONObject();
		try {
			json.put("success", "false");
			String p = "token=" + token + "&vid=" + vid;
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/queryInfoByVid/" + p + "?client_id=" + client_id;
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			System.out.println("请求地址:" + url);
			res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			System.out.println("返回:" + res);
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);// 解析接口返回状态
			if (jb.get("success").equals("false")) {
				json.put("info", jb.get("info"));
				return json;
			} else {
				json.put("success", "true");
				json.put("info", res);
			}
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
		}
		return json;
	}

	/**
	 * API用户登陆 用户首次调用接口，须先登录，认证通过后生成令牌。
	 * 令牌有效期默认为3天，登录后之前的令牌将立即失效，多服务调用业务接口时，建议由统一服务调用登录接口将令牌缓存起来
	 * ，多个服务统一从共享缓存中获取令牌。
	 * 令牌失效后再调用登录接口获取令牌，避免频繁调用登录接口，建议一天内登录次数不超过10次，超过10次将触发安全系统报警。 返回值示例：
	 * {"result":"b1b5d200-588b-4b56-90c6-42ea5435cf2b","status":1001}
	 * */
	public DemoReturnBean login() {
		try {
			System.out.println("API用户登陆 ");
			String p = "user=" + apiUser + "&pwd=" + password;
			System.out.println(p);
			p = TransCode.encode(p);// DES加密
			String url = apiUrl + "/login/" + p + "?client_id=" + client_id;
			System.out.println(url);
			DataExchangeService des = new DataExchangeService(5000, 5000);// 请求访问超时时间,读取数据超时时间
			String res = des.accessHttps(url, "POST");
			res = TransCode.decode(res);// DES解密
			DemoReturnBean bean = JSON.parseObject(res, DemoReturnBean.class);
			JSONObject jb = new JSONObject();
			jb = analysisStatus(bean);// 解析接口返回状态
			System.out.println("智运api登陆结果=" + jb.toString());
			return bean;
		} catch (Exception e) {
			System.out.println("e:" + e.getMessage());
			return null;
		}
	}

	/** 解析接口返回状态 */
	private JSONObject analysisStatus(DemoReturnBean bean) {
		JSONObject json = new JSONObject();
		try {
			json.put("success", "false");
			if ("1001".equals(bean.getStatus())) {
				json.put("success", "true");
				json.put("info", "登陆成功");
			} else if ("1002".equals(bean.getStatus())) {
				json.put("info", "参数不正确（参数为空、查询时间范围不正确、参数数量不正确)");
			} else if ("1003".equals(bean.getStatus())) {
				json.put("info", "车辆调用数量已达上限");
			} else if ("1004".equals(bean.getStatus())) {
				json.put("info", "接口调用次数已达上限");
			} else if ("1005".equals(bean.getStatus())) {
				json.put("info", "该API账号未授权指定所属行政区划数据范");
			} else if ("1006".equals(bean.getStatus())) {
				json.put("info", "无结果");
			} else if ("1010".equals(bean.getStatus())) {
				json.put("info", "用户名或密码不正确");
			} else if ("1011".equals(bean.getStatus())) {
				json.put("info", "IP不在白名单列表");
			} else if ("1012".equals(bean.getStatus())) {
				json.put("info", "账号已禁用");
			} else if ("1013".equals(bean.getStatus())) {
				json.put("info", "账号已过有效期");
			} else if ("1014".equals(bean.getStatus())) {
				json.put("info", "无接口权限");
			} else if ("1015".equals(bean.getStatus())) {
				json.put("info", "用户认证系统已升级，请使用令牌访问");
			} else if ("1016".equals(bean.getStatus())) {
				json.put("info", "令牌失效");
			} else if ("1017".equals(bean.getStatus())) {
				json.put("info", "账号欠费");
			} else if ("1018".equals(bean.getStatus())) {
				json.put("info", "授权的接口已禁用");
			} else if ("1019".equals(bean.getStatus())) {
				json.put("info", "授权的接口已过期");
			} else if ("1020".equals(bean.getStatus())) {
				json.put("info", "该车调用次数已达上限");
			} else if ("9001".equals(bean.getStatus())) {
				json.put("info", "智运系统异常");
			}
		} catch (JSONException e) {
			try {
				json.put("info", "" + e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return json;
	}

	
}
