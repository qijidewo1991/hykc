package com.xframe.security;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.json.JSONException;

import com.hyb.utils.AppUtil;
import com.hyb.utils.RedisClient;

public class LoginFilter implements Filter {
	public static final String USER_TOKEN="LOGINID";
	public static final String DEST_URL="DESTURL";
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(LoginFilter.class);
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
		java.util.ArrayList<String> areas=(java.util.ArrayList<String>)request.getServletContext().getAttribute("protectedAreas");
		
		if(areas==null || areas.size()==0){
			log.info("login-config.xml配置信息无效");
			chain.doFilter(req, rep);
			return;
		}
		
		
		boolean requestCheck=false;
		String strUri=request.getRequestURI();
		String strUrl=request.getRequestURL().toString();
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
		
		//debug(strUrl,"\trequst("+requestCheck+"("+request.getSession().getId()+")),url="+request.getRequestURL()+",uri="+request.getRequestURI()+",path="+request.getContextPath()+",pathInfo="+request.getPathInfo());
		
		Enumeration<String> names= req.getParameterNames();
		StringBuffer sb=new StringBuffer();
		while(names.hasMoreElements()){
			String name=names.nextElement().toString();
			String val=req.getParameter(name);
			sb.append("|"+name+"="+val);
		}
		
		String loginId=AppUtil.getCookieByName(request.getCookies(),LoginFilter.USER_TOKEN);
		String strObj=null;
		if(loginId!=null)
			strObj=RedisClient.getInstance().getFromHash("sessions", loginId);
		if(loginId==null){
			AppUtil.setCookie(rep, LoginFilter.DEST_URL, request.getRequestURI()+"?t="+System.nanoTime());			
			RequestDispatcher dispatcher=request.getRequestDispatcher(LoginCheck.LOGIN_URL);
			dispatcher.forward(req, rep);
			return;
		}
		
		AuthedUser user=(AuthedUser)request.getSession().getAttribute("authedUser");
		if(user==null){
			String strUserObj=RedisClient.getInstance(null).getFromHash("sessions", loginId);
			try {
				user=AuthedUser.fromJson(new org.json.JSONObject(strUserObj));
				request.getSession().setAttribute("authedUser",user);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		/*
		//log.info("目标URL="+request.getSession().getAttribute("requestUri"));
		debug(strUrl,"\t\tauthedUser="+(user!=null?user.toString():"NULL!"));
		if(user==null || user.getName()==null){
			String prevSessionId=request.getHeader("cookiess");
			if(prevSessionId!=null){
				HashSet<HttpSession> sessions = (HashSet<HttpSession>) request.getServletContext().getAttribute("sessions");
				for(HttpSession session:sessions){
					if(session.getId().equals(prevSessionId)){
						AuthedUser utmp=(AuthedUser)session.getAttribute("authedUser");
						request.getSession().setAttribute("authedUser", utmp);
						chain.doFilter(req, rep);
						return;
					}
				}
				
			}
			//headers
			//response.sendError(900);
			RequestDispatcher dispatcher=request.getRequestDispatcher(LoginCheck.LOGIN_URL);
			dispatcher.forward(req, rep);
			return;
		}
		*/
		
		chain.doFilter(req, rep);
	}
	
	private boolean shouldCheck(String url){
		return url.endsWith(".jsp") || url.toLowerCase().endsWith(".html") || url.toLowerCase().endsWith(".js") || url.indexOf("/do/")>=0;
	}
	
	@SuppressWarnings("unused")
	private void debug(String url,String msg){
		if(url.endsWith(".jsp") || url.toLowerCase().endsWith(".html") || url.indexOf("/do/")>=0){
			//log.info(msg);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		//System.out.println("\t%%%parameter="+filterConfig.getInitParameter("initParameter"));
		this.filterConfig=filterConfig;
	}

}
