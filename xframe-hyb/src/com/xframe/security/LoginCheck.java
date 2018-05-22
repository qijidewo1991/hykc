package com.xframe.security;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hyb.utils.AppUtil;
import com.hyb.utils.RedisClient;

import nl.captcha.Captcha;

public class LoginCheck extends HttpServlet {
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(LoginCheck.class);
	private IAuthMethod authMethod=null;
	private java.util.ArrayList<String> areas=new java.util.ArrayList<String>();
	private String defaultUrl="";
	public static final String LOGIN_URL="/login.jsp";
	/**
	 * 
	 */
	private static final long serialVersionUID = 2990149067935181122L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse rep)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		//System.out.println("\n\tLOGINCHECK###param map="+req.getRequestURI());
		defaultUrl="/apps/main.jsp";
		javax.servlet.http.HttpServletRequest request=(javax.servlet.http.HttpServletRequest)req;
		String strUri=AppUtil.getCookieByName(request.getCookies(), LoginFilter.DEST_URL);
		
		//log.info("==>"+defaultUrl+",strUri="+strUri);
		if(this.authMethod==null){
			req.getRequestDispatcher(LOGIN_URL).forward(req, rep);
			return;
		}
		
		if(this.areas.size()==0){ //未设置保护区域，继续
			req.getSession().setAttribute("authedUser",new AuthedUser());
			rep.sendRedirect(strUri);
			return;
		}
		
		//log.info("==>222"+defaultUrl+",strUri="+strUri);
		
		String j_deptid=req.getParameter("j_deptid");
		String j_username=req.getParameter("j_username");
		String j_password=req.getParameter("j_password");
		String j_authcode=req.getParameter("j_authcode");
		
		Captcha captcha=(Captcha)req.getSession().getAttribute("simpleCaptcha");
		String answer=captcha!=null?captcha.getAnswer():""+j_authcode;
		log.info("name:"+j_username+",pwd:"+j_password+",code:"+j_authcode+",answer="+(answer));
		try {
			AuthedUser user=authMethod.authenticate(j_deptid+"-"+j_username, j_password, j_authcode.toLowerCase(), answer.toLowerCase());
			if(user.getName()!=null){
				log.info("验证OK:default="+defaultUrl+"\ndest="+strUri);
				String uuid=UUID.randomUUID().toString();
				AppUtil.setCookie(rep, LoginFilter.USER_TOKEN, uuid);
				RedisClient.getInstance().addToHash("sessions", uuid, user.toJsonObject().toString());
				if(strUri!=null)
					rep.sendRedirect(strUri);
				else
					rep.sendRedirect(defaultUrl);
				
				
				/*
				org.hibernate.Session hSession=Configuration.sessionFactory.openSession();
				business.FM_USERS u=(business.FM_USERS)hSession.createQuery("from business.FM_USERS t where t.login_name='"+user.getName()+"'").uniqueResult();
				if(u!=null)
					user.setUserName(u.getName());
				hSession.close();
				*/
				req.getSession().setAttribute("authedUser",user);
			}else{
				log.info("验证失败...");
				//String url=getLoginUrl(request,strUri);
				//req.getRequestDispatcher(url).forward(req, rep);
				req.getRequestDispatcher(LOGIN_URL).forward(req, rep);
			}
			
		 } catch (Exception e) {
			// TODO Auto-generated catch block
			 log.error("异常:"+e.toString());
			 e.printStackTrace();
			 req.getRequestDispatcher(LOGIN_URL).forward(req, rep);
		 }
		
	}
	
	/*
	private String getLoginUrl(javax.servlet.http.HttpServletRequest req, String strUri){
		String currentUrl=req.getRequestURI();
		String strLogin="/login.jsp";
		if(strUri==null)
			return strLogin;
		String[] items=strUri.split("/");
		String[] citems=currentUrl.split("/");
		String prefix="";
		for(int i=0;i<items.length-citems.length;i++){
			prefix="../"+prefix;
		}
		req.getSession().setAttribute("prefix",prefix);
		return strLogin;
	}*/

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		String cfgFile=this.getInitParameter("config");
		try{
			org.dom4j.Document doc=org.dom4j.DocumentHelper.parseText(org.mbc.util.Tools.loadTextFile(this.getServletContext().getRealPath(cfgFile)));
			org.dom4j.Element root=doc.getRootElement();
			defaultUrl=root.attributeValue("default-url");
			if(defaultUrl==null)
				defaultUrl="/";
			org.dom4j.Element protectedArea=root.element("protected-area");
			
			java.util.List<org.dom4j.Element> list=protectedArea.elements("url-pattern");
			for(int i=0;i<list.size();i++){
				areas.add(list.get(i).getText());
			}
			org.dom4j.Element methodNode=root.element("method");
			String clsName=methodNode.attributeValue("className");
			Class ObjCls=Class.forName(clsName);
			Object o=ObjCls.newInstance();
			java.util.List<org.dom4j.Attribute> atts=methodNode.attributes();
			
			org.springframework.beans.BeanWrapper wrapper=new org.springframework.beans.BeanWrapperImpl(o);
			for(int i=0;i<atts.size();i++){
				wrapper.setPropertyValue(atts.get(i).getName(), atts.get(i).getValue());
			}
			this.authMethod=(IAuthMethod)o;
			
			this.getServletContext().setAttribute("protectedAreas", areas);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	
}
