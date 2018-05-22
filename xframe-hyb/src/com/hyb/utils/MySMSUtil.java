package com.hyb.utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import com.xframe.utils.AppHelper;

public class MySMSUtil {
	public static String[] templates=new String[]{
		"【货运快车】您的注册验证码为@，当前验证码请勿告知他人，一分钟内有效。",//0
		"【货运快车】提醒您，您通过“我的钱包”提现功能提现@元，当前余额@元。",//1
		"【货运快车】提醒您，您通过@为货运快车钱包充值@元，当前余额@元。",//2
		"【货运快车】您的登录验证码为@，当前验证码请勿告知他人，一分钟内有效。",//3
		"【货运快车】您的验证码为@，一分钟内有效，您在进行密码重置操作，请勿将此验证码告知他人。",//4
		"【货运快车】您申请注册的账户已受理成功，登录信息如下：地址：http://tuoying.huoyunkuaiche.com；机构号：@；用户名：1001；初始密码：111111，请妥善保管您的登录信息并及时修改密码" ,//5
		"【货运快车】您有一个新运单【@】，请打开app接收",//6
		"【货运快车】@有@吨@，从@到@，请联系@"//7
	};
	
	public static boolean sendRegSms(String code,String mobiles){
		String content=templates[0].replace("@", code);
		return sendSms(content,mobiles);
	}
	/**
	 * 发布运单信息 以短信形式
	 * @param code
	 * @param mobiles
	 * @return
	 */
	public static boolean sendYDSms(String code,String mobiles){
		String content=templates[6].replace("@", code);
		return sendSms(content,mobiles);
	}
	public static boolean sendAll(String wlgsname,String weight,String name,String from,String to,String lxdh,String mobiles){
		String content=templates[7].replaceFirst("@", wlgsname);
		content=content.replaceFirst("@", weight);
		content=content.replaceFirst("@", name);
		content=content.replaceFirst("@", from);
		content=content.replaceFirst("@", to);
		content=content.replaceFirst("@", lxdh);
		return sendSms(content,mobiles);
	}
	public static boolean sendCashSms(String amount,String balance,String mobiles){
		String content=templates[1].replaceFirst("@", amount);
		content=content.replaceFirst("@", balance);
		return sendSms(content,mobiles);
	}
	
	public static boolean sendPaySms(String payType,String amount,String balance,String mobiles){
		String content=templates[2].replaceFirst("@", payType);
		content=content.replaceFirst("@", amount);
		content=content.replaceFirst("@", balance);
		
		return sendSms(content,mobiles);
	}
	
	public static boolean sendLoginSms(String code,String mobiles){
		String content=templates[3].replace("@", code);
		return sendSms(content,mobiles);
	}
	
	public static boolean sendResetSms(String code,String mobiles){
		String content=templates[4].replace("@", code);
		return sendSms(content,mobiles);
	}
	
	public static boolean sendWlgsSms(String jgbh,String mobiles){
		String content=templates[5].replace("@", jgbh);
		return sendSms(content,mobiles);
	}
	
	@SuppressWarnings("unchecked")
	public static boolean sendSms(String content,String mobiles) {

		try {
			java.util.HashMap<String, String> conf=AppHelper.getSystemConf();
			//org.apache.log4j.BasicConfigurator.configure();
			HttpClient httpclient = new DefaultHttpClient();
                        //Secure Protocol implementation.
			SSLContext ctx = SSLContext.getInstance("SSL");
                        //Implementation of a trust manager for X509 certificates
			X509TrustManager tm = new X509TrustManager() {

				public void checkClientTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {

				}

				public void checkServerTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);

			ClientConnectionManager ccm = httpclient.getConnectionManager();
                        //register https protocol in httpclient's scheme registry
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", 443, ssf));
			
			StringBuffer sb=new StringBuffer();
			//sb.append("https://dx.ipyy.net/smsJson.aspx?action=send&userid=");//sms_ip=114.113.154.5
			sb.append(""+conf.get("sms_ip")+"/smsJson.aspx?action=send&userid=");
			sb.append("&account="+conf.get("sms_account"));
			sb.append("&password="+conf.get("sms_password"));
			sb.append("&mobile="+mobiles);
			sb.append("&content="+content);
			sb.append("&sendTime=&extno=");
			
			HttpGet httpget = new HttpGet(sb.toString());
			HttpParams params = httpclient.getParams();

			httpget.setParams(params);
			System.out.println("REQUEST:" + httpget.getURI());
			@SuppressWarnings("rawtypes")
			ResponseHandler responseHandler = new BasicResponseHandler();
			String responseBody;

			responseBody = httpclient.execute(httpget, responseHandler);

			System.out.println(responseBody);

			// Create a response handler
			org.json.JSONObject ret= new org.json.JSONObject(responseBody.trim());
			return ret.has("returnstatus") && ret.getString("returnstatus").equals("Success");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();

		}
		
		return false;
	}
}
