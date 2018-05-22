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
		"�����˿쳵������ע����֤��Ϊ@����ǰ��֤�������֪���ˣ�һ��������Ч��",//0
		"�����˿쳵������������ͨ�����ҵ�Ǯ�������ֹ�������@Ԫ����ǰ���@Ԫ��",//1
		"�����˿쳵������������ͨ��@Ϊ���˿쳵Ǯ����ֵ@Ԫ����ǰ���@Ԫ��",//2
		"�����˿쳵�����ĵ�¼��֤��Ϊ@����ǰ��֤�������֪���ˣ�һ��������Ч��",//3
		"�����˿쳵��������֤��Ϊ@��һ��������Ч�����ڽ����������ò��������𽫴���֤���֪���ˡ�",//4
		"�����˿쳵��������ע����˻�������ɹ�����¼��Ϣ���£���ַ��http://tuoying.huoyunkuaiche.com�������ţ�@���û�����1001����ʼ���룺111111�������Ʊ������ĵ�¼��Ϣ����ʱ�޸�����" ,//5
		"�����˿쳵������һ�����˵���@�������app����",//6
		"�����˿쳵��@��@��@����@��@������ϵ@"//7
	};
	
	public static boolean sendRegSms(String code,String mobiles){
		String content=templates[0].replace("@", code);
		return sendSms(content,mobiles);
	}
	/**
	 * �����˵���Ϣ �Զ�����ʽ
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
