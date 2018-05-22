package com.xframe.core;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

public class DBProxy extends javax.servlet.http.HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2807714282474275070L;
	
	private static final Logger log=Logger.getLogger(DBProxy.class);
	
	private String baseDirectory;
	
	private java.util.HashMap<String, org.dom4j.Document> proxyMap=new java.util.HashMap<String, Document>();
	
	private java.util.HashMap<String, Long> proxyDate=new java.util.HashMap<String, Long>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void service(HttpServletRequest req, HttpServletResponse rep)
			throws ServletException, IOException {
		long lStart=System.currentTimeMillis();
		// TODO Auto-generated method stub
		String reqObj=req.getRequestURI().substring(req.getRequestURI().lastIndexOf("/")+1);
		if(reqObj.indexOf(".")<0){
			completeRequest("Error request format["+req.getRequestURI()+"]!",rep);
			return;
		}
		
		//rep.setCharacterEncoding(Configuration.defaultEncoding);
		rep.setContentType("text/html");
		String pak=reqObj.substring(0,reqObj.indexOf("."))+".xml";
		String name=reqObj.substring(reqObj.indexOf(".")+1);
		java.io.File file=new java.io.File(this.getServletContext().getRealPath(baseDirectory)+"/"+pak);
		if(!proxyMap.containsKey(pak) && !file.exists()){
			completeRequest("Error request package["+pak+"]!",rep);
			return;
		}
		
		org.dom4j.Document doc=null;
		if(!proxyMap.containsKey(pak) || file.lastModified()>proxyDate.get(pak)){
			log.info("package["+pak+"] modified,reloading...");
			try{
				doc=org.dom4j.DocumentHelper.parseText(org.mbc.util.Tools.loadTextFile(file.getAbsolutePath()));
				proxyMap.put(pak, doc);
				proxyDate.put(pak, file.lastModified());
			}catch(Exception ex){
				ex.printStackTrace();
				completeRequest("Faile to reloading package["+pak+"]!",rep);
				return;
			}
		}else
			doc=proxyMap.get(pak);
		
		Element target=null;
		java.util.List<Element> plist=doc.getRootElement().elements("proxy");
		for(int i=0;i<plist.size();i++){
			if(plist.get(i).attributeValue("name")!=null && plist.get(i).attributeValue("name").equals(name)){
				target=plist.get(i);
				break;
			}
		}
		if(target==null){
			completeRequest("Target proxy["+name+"] not found!",rep);
			return;
		}
		String type=target.attributeValue("type");
		try{
			String encoding=target.attributeValue("encode");
			if(encoding!=null)
				req.setCharacterEncoding(encoding);
			java.util.HashMap<String, String> params=new java.util.HashMap<String, String>();
			//java.util.List<Element> list=target.elements("param");
			//log.debug("paramsize:::"+list.size());
			
			
	 		java.util.Enumeration<String> names=req.getParameterNames();
	 		while(names.hasMoreElements()){
	 			String pname=names.nextElement();
	 			String value=req.getParameter(pname);
	 			//System.out.println("\t["+pname+"]"+value);
	 			params.put(pname, new String(value.getBytes("iso-8859-1"),req.getCharacterEncoding()==null?"gbk":req.getCharacterEncoding()));
	 		}
			
	 		/*
			for(int i=0;i<list.size();i++){
				String pname=list.get(i).getText().trim();
				String val=req.getParameter(pname);
				params.put(pname, val);
			}*/
			
			params.put("encoding", req.getCharacterEncoding());
			params.put("__method", reqObj);
			params.put("baseDir", this.getServletContext().getRealPath(baseDirectory));
			//params.put("__dbname", "default");
			params.put("script", target.attributeValue("src"));
			params.put("proxyName", name);
			
			log.info("\tCALL DB PROXY["+name+"]");
			
			rep.setCharacterEncoding("utf-8");
			
			Class cls=type.equals("java")?Class.forName(target.attributeValue("src")):Class.forName("com.xframe.core.PythonProxy");
			
			IDataProxy provider=(IDataProxy)cls.newInstance();
			try{
				String strReturn=provider.doProxy(req, rep, params);
				if(!strReturn.equals(IDataProxy.EXEC_OK)){
					rep.getWriter().write(strReturn);
				}
			}catch(Exception ex){
				log.error("\n*****************["+name+"]Òì³£*****************");
				ex.printStackTrace();
			}
			log.info("ClientProxy("+name+") spent "+(System.currentTimeMillis()-lStart)+"ms");
		}catch(Exception ex){
			ex.printStackTrace();
			completeRequest("Create proxy["+name+"] failed,ex="+ex.toString()+"!",rep);
			return;
		}
	}

	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		baseDirectory=this.getInitParameter("base-dir");
		try{
			java.io.File dir=new java.io.File(this.getServletContext().getRealPath(baseDirectory));
			java.io.File[] files=dir.listFiles();
			for(int i=0;i<files.length;i++){
				if(files[i].isDirectory())
					continue;
				try{
					org.dom4j.Document doc=org.dom4j.DocumentHelper.parseText(org.mbc.util.Tools.loadTextFile(files[i].getAbsolutePath()));
					proxyMap.put(files[i].getName(), doc);
					proxyDate.put(files[i].getName(), files[i].lastModified());
				}catch(Exception ex){
					log.warn("proxy["+files[i].getName()+"] load failed!");
				}
			}
		}catch(Exception ex){
			log.error("initialize failed,exception:"+ex.toString());
		}
	}
	
	public static void completeRequest(String msg,HttpServletResponse rep){
		log.info(msg);
		try{
			rep.getWriter().println("{success:false,message:'"+msg+"'}");
		}catch(Exception ex){
		}
	}
}
