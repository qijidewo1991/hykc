package com.xframe.core;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hyb.utils.AccountUtil;
import com.xframe.utils.AppHelper;

public class TestPayServlet extends HttpServlet {

	private static final long serialVersionUID = 4029836360856460971L;

	@Override
	public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		java.util.HashMap<String, String> conf=AppHelper.getSystemConf();
		
		/*
		AlipayClient alipayClient = new DefaultAlipayClient("https://mapi.alipay.com/gateway.do?"//"https://openapi.alipay.com/gateway.do"
				,conf.get("APPID")
				,conf.get("RSA_PRIVATE")
				,"json"
				,"utf-8"
				,conf.get("RSA_PUBLIC")
				,"RSA");
		AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();//����API��Ӧ��request
		alipayRequest.setReturnUrl("http://"+conf.get("mq_server_remote")+"/shared/zfb_return.jsp");
		alipayRequest.setNotifyUrl("http://"+conf.get("mq_server_remote")+"/shared/zfb_notify.jsp");//�ڹ������������û�����֪ͨ��ַ
		
		alipayRequest.setBizContent(AccountUtil.getOrderInfoJson("��ֵ��", "0.01").toString());
		String form="";
		try {
			form = alipayClient.pageExecute(alipayRequest).getBody();
		} catch (AlipayApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //����SDK���ɱ�
		httpResponse.setContentType("text/html;charset=utf-8");
	    httpResponse.getWriter().write(form);//ֱ�ӽ������ı�html�����ҳ��
	    httpResponse.getWriter().flush();
	    */
	}
	
	
}
