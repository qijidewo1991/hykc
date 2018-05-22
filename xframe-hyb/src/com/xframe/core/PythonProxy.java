package com.xframe.core;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.python.util.PythonInterpreter;

public class PythonProxy implements IDataProxy {
	private static Logger log=Logger.getLogger(PythonProxy.class);
	private static java.util.HashMap<String, FileItem> fileItems=
			new java.util.HashMap<String, FileItem>();
	@Override
	public String doProxy(HttpServletRequest req, HttpServletResponse rep,
			HashMap<String, String> params) throws Exception {
		// TODO Auto-generated method stub
		if(req.getParameter("debug")!=null)
			log.info("params=="+params);
		JSONObject json=new JSONObject();
		json.put("success", "true");
		json.put("message", "");
		
		
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.set("request", req);
		interp.set("response", rep);
		interp.set("json", json);
		interp.set("params", params);		
		
		String src=params.get("script");
		String baseDir=params.get("baseDir");
		String proxyName=params.get("proxyName");
		
		params.remove("script");
		params.remove("proxyName");
		params.remove("baseDir");
		
		
		
		//向json添加所有request参数
		try{
			//String strVars="";
	        for(String k:params.keySet()){
	        	String val=params.get(k);
	        	if(val==null)
	        		continue;
	        	//strVars+=name+"=\""+new String(val.getBytes("iso-8859-1"),"gbk")+"\"\r\n";
	        	//interp.exec(name+"='"+params.get(name)+"'");
	        	//json.put(k, new String(val.getBytes("iso-8859-1"),"utf-8"));
	        	json.put(k, val);
	        }
	        //interp.exec(strVars);		
		}catch(Exception ex){
			ex.printStackTrace();
		}
				
		
		String strScript="";
		String path=baseDir+java.io.File.separatorChar+src;
		java.io.File file=new java.io.File(path);
		if(!file.exists())
			throw new Exception("["+proxyName+"]"+"#文件"+src+"不存在!");
		
		
		strScript=this.loadScript(file);
		
		if(strScript.length()==0)
			throw new Exception("["+proxyName+"]"+"#文件"+src+"不存在!");
		try{
			interp.exec(strScript);
			//log.info("Python return:"+(json.has("retstr")?json.get("retstr"):""));
			if(req.getParameter("result_filter")!=null){
				String resultFilter=req.getParameter("result_filter");
				java.io.File filterFile=new java.io.File(req.getServletContext().getRealPath("WEB-INF/proxy_filters/"+resultFilter+".py"));
				if(filterFile.exists()){
					strScript=this.loadScript(filterFile);
					interp.exec(strScript);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			throw new Exception("["+proxyName+"]"+src+"异常:"+ex.toString());
		}
		interp.cleanup();
		return (json.has("handled")?json.toString():IDataProxy.EXEC_OK) ;
		
	}
	
	private String loadScript(java.io.File file){
		String strScript="";
		String path=file.getAbsolutePath();
		if(fileItems.containsKey(path)){
			FileItem item=fileItems.get(path);
			if(file.lastModified()>item.getLastModified()){
				strScript=org.mbc.util.Tools.loadTextFile(path,"utf-8");
				item.setLastModified(file.lastModified());
				item.setScript(strScript);
			}else{
				strScript=item.getScript();
			}
		}else{
			strScript=org.mbc.util.Tools.loadTextFile(path,"utf-8");
			fileItems.put(path, new FileItem(path,strScript,file.lastModified()));
		}
		
		return strScript;
	}

}
