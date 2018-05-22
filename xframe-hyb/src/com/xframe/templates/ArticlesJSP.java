package com.xframe.templates;

import org.json.*;

public class ArticlesJSP
{
  protected static String nl;
  public static synchronized ArticlesJSP create(String lineSeparator)
  {
    nl = lineSeparator;
    ArticlesJSP result = new ArticlesJSP();
    nl = null;
    return result;
  }

  public final String NL = nl == null ? (System.getProperties().getProperty("line.separator")) : nl;
  protected final String TEXT_1 = "<%@ page contentType=\"text/html; charset=utf-8\"%>" + NL + "<!doctype html>" + NL + "<html>" + NL + "\t<head>" + NL + "\t\t<title>Articles</title> " + NL + "\t\t<meta charset=\"utf-8\">" + NL + "\t\t<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\"> " + NL + "\t\t<META HTTP-EQUIV=\"Cache-Control\" CONTENT=\"no-cache\"> " + NL + "\t\t<META HTTP-EQUIV=\"Expires\" CONTENT=\"0\"> " + NL + "\t\t<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=0\">" + NL + "\t\t<link id=\"bs-css2\" href=\"../charisma/css/bootstrap-cerulean.min.css\" rel=\"stylesheet\">" + NL + "\t\t<link href=\"../charisma/css/charisma-app.css\" rel=\"stylesheet\">" + NL + "\t\t<script src=\"../charisma/bower_components/jquery/jquery.min.js\"></script>" + NL + "\t\t<link href=\"list.css?t=3\" rel=\"stylesheet\">" + NL + "\t\t<script lang=\"javascript\">" + NL + "\t\t\tfunction showDetail(strTitle,strId){" + NL + "\t\t\t\tvar strurl=\"/files/content.jsp?rowid=\"+strId;" + NL + "\t\t\t\tjsObj.onShowWeb(strurl,strTitle,2);" + NL + "\t\t\t}" + NL + "\t\t</script>" + NL + "\t</head>" + NL + "\t<body style=\"background-color:#f1f1f1;\">" + NL + "\t\t";
  protected final String TEXT_2 = NL + "\t\t<ul class=\"list-group\">\t" + NL + "\t\t\t\t<div style=\"text-align:center;margin-top:15px;\" class=\"news-time\">" + NL + "\t\t\t\t\t<span style=\"background-color:#999999;color:#ffffff;font-size:12px;padding:3px;\">";
  protected final String TEXT_3 = "</span>" + NL + "\t\t\t\t</div>" + NL + "\t\t\t\t" + NL + "\t\t\t\t<a href=\"javascript:showDetail('";
  protected final String TEXT_4 = "','";
  protected final String TEXT_5 = "')\" class=\"list-group-item header-item\">" + NL + "\t\t\t\t\t<div class=\"news-body\">" + NL + "\t\t\t\t\t\t<img src=\"";
  protected final String TEXT_6 = "\" class=\"news-img\"/>" + NL + "\t\t\t\t\t\t<div class=\"news-title\">";
  protected final String TEXT_7 = "</div>" + NL + "\t\t\t\t\t</div>" + NL + "\t\t\t\t</a>" + NL + "\t\t</ul>\t" + NL + "\t\t\t";
  protected final String TEXT_8 = NL + "\t</body>" + NL + "</html>\t";

  public String generate(Object argument)
  {
    final StringBuffer stringBuffer = new StringBuffer();
    
	
	java.util.ArrayList<JSONObject> list=(java.util.ArrayList<JSONObject>)argument;

    stringBuffer.append(TEXT_1);
    
				try{
					for(int i=0;i<list.size();i++){
						JSONObject row=list.get(i);
						String rowid=row.getString("rowid");
						String title=row.getString("data:title");
						String pubTime=row.getString("data:pub_time");
						String fileDir=row.getString("data:file_dir");
						String thumb=row.getString("data:thumb");
						String prefix="../files/";
						thumb=thumb.substring(thumb.indexOf(prefix)+prefix.length());
			
    stringBuffer.append(TEXT_2);
    stringBuffer.append(pubTime);
    stringBuffer.append(TEXT_3);
    stringBuffer.append(title);
    stringBuffer.append(TEXT_4);
    stringBuffer.append(rowid);
    stringBuffer.append(TEXT_5);
    stringBuffer.append(thumb);
    stringBuffer.append(TEXT_6);
    stringBuffer.append(title);
    stringBuffer.append(TEXT_7);
    			
					}
				}catch(Exception ex){
					ex.printStackTrace();
				}
		
    stringBuffer.append(TEXT_8);
    return stringBuffer.toString();
  }
}
