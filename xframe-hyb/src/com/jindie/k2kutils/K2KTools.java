package com.jindie.k2kutils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

import com.xframe.utils.AppHelper;
import com.xframe.utils.Tools;
import com.xframe.utils.XHelper;

public class K2KTools {

	/**
	 * @param BillNo
	 * @return {"code":"01","msg":"BR0011","other":"1000000.0"} 04是无 false 是搜索失败
	 */
	public JSONObject searchByNo(String BillNo) {
		java.util.HashMap<String, String> conf=AppHelper.getSystemConf();
		String URL=conf.get("jindie_url");
		JSONObject json = new JSONObject();
		String res = "";
		try {
			java.util.HashMap<String, String> params = new HashMap<>();
			params.put("billNo", BillNo);
			res = Tools.post(URL + "/queryBillNo", params);
			json = new JSONObject(res);
		} catch (Exception e) {
			try {
				json.put("code", "false");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return json;
	}

	/**
	 * 传单子 若上传失败，则记录到k3k_info表里 rowid 为json 中的“id” 若上传成功，回写account_detail里 sfsc
	 * 为1
	 * 
	 * @param json
	 * @return
	 */
	public static JSONObject sendBill(JSONObject json) {
		java.util.HashMap<String, String> conf=AppHelper.getSystemConf();
		String URL=conf.get("jindie_url");
		JSONObject result = new JSONObject();
		String res = "";
		res = Tools.post2(URL + "/save", json.toString());
		System.out.println(URL + "/save"+"------------"+res+"-------------"+ json.toString());
		try {
			result = new JSONObject(res);
			JSONObject j_update = new JSONObject();
			if (result.has("code")&&(result.getString("code").equals("01") || result.getString("code").equals("06"))) {
				System.out.println("vvvv上传成功" + result.toString());
				j_update.put("sfsc", "1");
				XHelper.createRow("account_detail", json.getString("id"), j_update);
				saveAsFileWriter("/home/apps/apache-tomcat-7.0.70/webapps/" + "logj.txt", "成功----" + json.toString() + "------" + result.toString());
			} else {
				System.out.println("xxxx上传失败" + result.toString());
				j_update.put("info", json.toString());
				j_update.put("k2kreturn", result.toString());
				XHelper.createRow("k3k_info", json.getString("id"), j_update);
				saveAsFileWriter("/home/apps/apache-tomcat-7.0.70/webapps/" + "logj.txt", "失败----" + json.toString() + "------" + result.toString());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public static void saveAsFileWriter(String file, String conent) {
		BufferedWriter out = null;
		;
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
