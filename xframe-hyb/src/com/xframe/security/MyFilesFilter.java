package com.xframe.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import com.hyb.utils.AppUtil;
import com.hyb.utils.RedisClient;
import com.xframe.utils.XHelper;

public class MyFilesFilter implements Filter {
	JSONObject jb=new JSONObject();
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(MyFilesFilter.class);
	@SuppressWarnings("unused")
	private FilterConfig filterConfig=null;
	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest req, ServletResponse rep,
			FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		
		javax.servlet.http.HttpServletRequest request=(javax.servlet.http.HttpServletRequest)req;
		//javax.servlet.http.HttpServletResponse response=(javax.servlet.http.HttpServletResponse)rep;
		java.util.ArrayList<String> areas=new java.util.ArrayList<String>(); //(java.util.ArrayList<String>)request.getSession().getServletContext().getAttribute("protectedAreas");
		areas.add("files/");
		areas.add("ds/");
		if(areas==null || areas.size()==0){
			log.info("login-config.xml配置信息无效");
			chain.doFilter(req, rep);
			return;
		}
		
		request.getSession().setAttribute("ec", "900");
		String loginId=AppUtil.getCookieByName(request.getCookies(),LoginFilter.USER_TOKEN);
		
		AuthedUser user=(AuthedUser)request.getSession().getAttribute("authedUser");
		MobileUser mUser=(MobileUser)request.getSession().getAttribute("mobiledUser");
		boolean requestCheck=false;
		String strUri=request.getRequestURI();
		String strUrl=request.getRequestURL().toString();
		//log.info("request==="+strUri);
		for(int i=0;i<areas.size();i++){
			/*
			Pattern pattern=Pattern.compile(areas.get(i));  
			Matcher m=pattern.matcher(strUri);
			System.out.println("ur=["+strUri+"],area["+areas.get(i)+"],result="+m.matches());
			*/
			if(strUri.indexOf(areas.get(i))>=0){
				requestCheck=true;
				break;
			}
		}
		

		if(!requestCheck){
			chain.doFilter(req, rep);
			return;
		}
		
		if(!shouldCheck(strUrl)){
			chain.doFilter(req, rep);
			return;
		}
		
		Enumeration<String> names= req.getParameterNames();
		StringBuffer sb=new StringBuffer();
		
		while(names.hasMoreElements()){
			String name=names.nextElement().toString();
			String val=req.getParameter(name);
			sb.append("|"+name+"="+val);
			try {
				jb.put(name, val);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("-----------------name="+name+"----------------val="+val);
		}
		
		//log.info("request url:"+strUrl);
		
		//文件上传不检查
		if(strUrl.indexOf("image_upload.jsp")>0){
			chain.doFilter(req, rep);
			return;
		}
		
		if(strUrl.indexOf("opers.execLogin")>0 || strUrl.indexOf("opers.execResetPwd")>0 || strUrl.indexOf("opers.getSms")>0 || strUrl.indexOf("/opers.regMobileUser")>0){ //请求登录不验证
			chain.doFilter(req, rep);
			return;
		}
		
		
		
		String app = "";
		String mobile="";
		String token="";
		
		try {
			mobile=jb.getString("mobile");
			token=jb.getString("token");
			app = jb.getString("app");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String strToken=null;
		if(mobile!=null){
			strToken=RedisClient.getInstance().getStr(mobile);
		}
		
		//if(user!=null || mUser!=null){ //web登录用户和已经登录的手机用户不再验证
		if(loginId!=null || mUser!=null || (strToken!=null && token!=null && strToken.equals(token))){
			chain.doFilter(req, rep);
			return;
		}
		
		
		
		
	
		
		
		try{
			org.json.JSONObject row=null;
			if(mobile.indexOf("cp-")!=-1){
				row=XHelper.loadRow("USER-"+mobile+"-"+app, "cp_users", true);
				log.info("mobile包含cp---------------"+mobile);
			}else{
				row=XHelper.loadRow("USER-"+mobile+"-"+app, "mobile_users", true);
				log.info("mobile不包含cp---------------"+mobile);
			}
			
		
			
			if(row!=null && token!=null){
				if(row.has("data:token") && row.getString("data:token").equals(token)){
					MobileUser tUser=new MobileUser();
					tUser.setApp(app);
					tUser.setMobile(mobile);
					tUser.setToken(token);
					request.getSession().setAttribute("mobiledUser",tUser);
					chain.doFilter(req, rep);
					//String uuid=UUID.randomUUID().toString();
					//AppUtil.setCookie(rep, LoginFilter.USER_TOKEN, uuid);
					RedisClient.getInstance().setStr(mobile,token);
					return;
				}
			}
		}catch(Exception ex){
			System.out.println(ex);
			ex.printStackTrace();
		}
		log.warn(" 无效请求:"+ request.getRequestURI()+",参数"+sb.toString());
		
		
	}
	
	private boolean shouldCheck(String url){
		//log.info("##url="+url+","+url.indexOf("ds/opers"));
		return url.endsWith(".jsp") || url.toLowerCase().endsWith(".html") 
				|| url.toLowerCase().endsWith(".js") 
				|| url.toLowerCase().indexOf("opers.")>0 
				|| url.indexOf("/do/")>=0 ;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		//System.out.println("\t%%%parameter="+filterConfig.getInitParameter("initParameter"));
		this.filterConfig=filterConfig;
	}

}
