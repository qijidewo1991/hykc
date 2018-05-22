package com.xframe.utils;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncodingAttributes;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.ssl.internal.www.protocol.https.Handler;
import com.xframe.security.MyHostNameVerifier;
import com.xframe.security.MyTrustManager;


public class Tools {
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(Tools.class);
	public static void convertAmrToMp3(String srcFile,String destFile) throws Exception {
		String osName=System.getProperty("os.name").toLowerCase();
		log.info("convertAmrToMp3,os="+osName+",file="+srcFile);
		if(osName.indexOf("linux")>=0){
			convertAmrToMp3Ex(srcFile,true);
			return;
		}
		File source = new File(srcFile);
		File target = new File(destFile);
		AudioAttributes audio = new AudioAttributes();
		Encoder encoder = new Encoder();
		/*
		for(String ss : encoder.getSupportedEncodingFormats())
		{
			System.out.println(ss);
		}*/
		// pcm_s16le libmp3lame libvorbis libfaac
		audio.setCodec("libmp3lame");
		EncodingAttributes attrs = new EncodingAttributes();
		attrs.setFormat("mp3");
		attrs.setAudioAttributes(audio);

		encoder.encode(source, target, attrs);
	}
	
	public static String convertAudioToMp3(String fileName,boolean debug){
		try{
			
			java.io.File inFile=new java.io.File(fileName);
			if(!inFile.exists())
				return "";
			String outFileName=fileName.replace(".wav", ".mp3");
			Process curProcess=Runtime.getRuntime().exec("ffmpeg -i "+fileName+" "+outFileName);
			
			java.io.InputStream in=curProcess.getInputStream();
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			String strError=sb.toString();
			System.out.println(strError);
			java.io.File outFile=new java.io.File(outFileName);
			if(outFile.exists() && outFile.length()>0){
				if(!debug)
					inFile.delete();
				return outFileName;
			}
			
			return "";
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return "";
	}
	
	public static String convertAmrToMp3Ex(String fileName,boolean debug){
		try{
			
			java.io.File inFile=new java.io.File(fileName);
			if(!inFile.exists())
				return "";
			String outFileName=fileName.replace(".amr", ".mp3");
			Process curProcess=Runtime.getRuntime().exec("ffmpeg -i "+fileName+" "+outFileName);
			
			java.io.InputStream in=curProcess.getInputStream();
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			String strError=sb.toString();
			System.out.println(strError);
			java.io.File outFile=new java.io.File(outFileName);
			if(outFile.exists() && outFile.length()>0){
				if(!debug)
					inFile.delete();
				return outFileName;
			}
			
			return "";
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return "";
	}
	
	public static java.util.Date parseDate(String format,String val){
		java.text.DateFormat df=new java.text.SimpleDateFormat(format);
		try {
			java.util.Date date=df.parse(val);
			return date;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getSpendTime(java.util.Date d1,java.util.Date d2){
		long day=24*3600;
		long hour=3600;
		long minute=60;
		long ms=(d2.getTime()-d1.getTime())/1000;
		StringBuffer sb=new StringBuffer();
		if(ms>day){ //天
			long t1=ms;
			ms=ms%day;
			int days=(int)(t1/day);
			sb.append(days+"天");
		}
		
		if(ms>hour){
			long t1=ms;
			ms=ms%hour;
			int hours=(int)(t1/hour);
			sb.append(hours+"小时");
		}
		
		if(ms>minute){
			long t1=ms;
			ms=ms%minute;
			int minutes=(int)(t1/minute);
			sb.append(minutes+"分钟");
		}
		
		return sb.toString();
	}
	
	public static String post(String url,java.util.HashMap<String, String> params) throws Exception{
		return post(url,params,"UTF-8");
	}
	
	public static String post(String url) throws Exception{
		return post(url,null,"UTF-8");
	}
	
	public static String post(String url,java.util.HashMap<String, String> params,String strEncode) throws Exception{
		//System.out.println("url="+url+",params="+params);
		HttpClient httpclient=new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);
		List<BasicNameValuePair> formparams = new ArrayList<BasicNameValuePair>();
		UrlEncodedFormEntity uefEntity;
		if(params!=null){
			for(String k:params.keySet()){
				formparams.add(new BasicNameValuePair(k, params.get(k)));
			}
		}
		
		uefEntity = new UrlEncodedFormEntity(formparams, strEncode);
		httppost.setEntity(uefEntity);
		//System.out.println("executing request " + httppost.getURI());
		HttpResponse response = httpclient.execute(httppost);

		HttpEntity entity = response.getEntity();
		if (entity != null) {
			String strRet=EntityUtils.toString(entity, strEncode);
			return strRet;
		}
		
		return "";
	}
	
	public static String post(String url,String params){
		 try{
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new MyHostNameVerifier());
			URL myURL=new URL(url);
			java.net.URLConnection urlcon=myURL.openConnection();
			System.out.println("\tcon=="+urlcon);
			HttpURLConnection conn = (HttpURLConnection)urlcon;
			
			
			conn.setReadTimeout(30 * 1000);  
	        conn.setDoInput(true);// 允许输入  
	        conn.setDoOutput(true);// 允许输出  
	        conn.setUseCaches(false);  
	        conn.setRequestMethod("POST"); // Post方式  
	        conn.setRequestProperty("connection", "keep-alive");  
	        conn.setRequestProperty("Charsert", "UTF-8");
	        conn.setRequestProperty("Content-Type", "application/json");
	       
	        
	        DataOutputStream outStream = new DataOutputStream(  
	                conn.getOutputStream());  
	        // 首先组拼文本类型的参数  
	        if(params!=null){
	        	outStream.write(params.getBytes());  
		        outStream.flush();
	        }
	        
	        
	        //boolean success = conn.getResponseCode()==200;  
	        InputStream in = conn.getInputStream();  
	        InputStreamReader isReader = new InputStreamReader(in);  
	        BufferedReader bufReader = new BufferedReader(isReader);  
	        String line = null;  
	        String data = "";  
	        while ((line = bufReader.readLine()) != null)  
	            data += line;  
	        
	        outStream.close();  
	        conn.disconnect();  
	        return data;
	     }catch(Exception e){
	    	 e.printStackTrace();
	    	 
	     }
		return "";
	}
	
	/**因为有时候会出现超时，金蝶专用。
	 * @param url
	 * @param params
	 * @return
	 */
	public static String post2(String url,String params){
		 try{
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(new MyHostNameVerifier());
			URL myURL=new URL(url);
			java.net.URLConnection urlcon=myURL.openConnection();
			System.out.println("\tcon=="+urlcon);
			HttpURLConnection conn = (HttpURLConnection)urlcon;
			
			
			conn.setReadTimeout(30 * 1000);  
	        conn.setDoInput(true);// 允许输入  
	        conn.setDoOutput(true);// 允许输出  
	        conn.setUseCaches(false);  
	        conn.setRequestMethod("POST"); // Post方式  
	        conn.setRequestProperty("connection", "keep-alive");  
	        conn.setRequestProperty("Charsert", "UTF-8");
	        conn.setRequestProperty("Content-Type", "application/json");
	       
	        
	        DataOutputStream outStream = new DataOutputStream(  
	                conn.getOutputStream());  
	        // 首先组拼文本类型的参数  
	        if(params!=null){
	        	outStream.write(params.getBytes());  
		        outStream.flush();
	        }
	        
	        
	        //boolean success = conn.getResponseCode()==200;  
	        InputStream in = conn.getInputStream();  
	        InputStreamReader isReader = new InputStreamReader(in);  
	        BufferedReader bufReader = new BufferedReader(isReader);  
	        String line = null;  
	        String data = "";  
	        while ((line = bufReader.readLine()) != null)  
	            data += line;  
	        
	        outStream.close();  
	        conn.disconnect();  
	        return data;
	     }catch(Exception e){
	    	 e.printStackTrace();
	    	 JSONObject json=new JSONObject();
	    	 try {
				json.put("044", "post网络错误"+e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	    	 return json.toString();
	    	 
	     }
		
	}
	public static void checkNews(String rawDir){
		java.io.File rawFolder=new java.io.File(rawDir);
		if(!rawFolder.exists())
			return;
		java.io.File destFolder=new java.io.File(rawFolder.getParentFile().getAbsolutePath()+"\\current");
		long lastModified=0;
		java.io.File rawFile=null;
		for(java.io.File file:rawFolder.listFiles()){
			if(file.lastModified()>lastModified){
				lastModified=file.lastModified();
				rawFile=file;
			}
		}
		if(!destFolder.exists() || (destFolder.exists() && destFolder.lastModified()<lastModified)){
			if(destFolder.exists())
				org.mbc.util.Tools.deleteDir(destFolder);
			destFolder.mkdir();
			try {
				if(rawFile!=null)
					unzipNews(rawFile,destFolder.getAbsolutePath());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void unzipNews(java.io.File file,String outFile) throws Exception{
		java.util.zip.ZipInputStream jarIn=null;
		try{
			jarIn = new java.util.zip.ZipInputStream(new java.io.FileInputStream(file));
			java.util.zip.ZipEntry jarentry=null;
			java.io.File curDir=null;
			String rootDir=null;
			while((jarentry = jarIn.getNextEntry()) != null){
				if(jarentry.isDirectory()){
					if(rootDir==null){
						rootDir=jarentry.getName();
						curDir=new java.io.File(outFile);
						continue;
					}
					String fileDir=jarentry.getName();
					if(fileDir.indexOf(rootDir)==0)
						fileDir=fileDir.substring((int)rootDir.length());
					curDir=new java.io.File(outFile+java.io.File.separator+fileDir);
					if(!curDir.exists())
						curDir.mkdir();
            		continue;
            	}
				//System.out.println("File::"+jarentry.getName());
				byte[] abyte0=new byte[2048];
				java.io.ByteArrayOutputStream bOut=new java.io.ByteArrayOutputStream();
				int i;
                while((i = jarIn.read(abyte0, 0,  abyte0.length)) != -1){
                	bOut.write(abyte0, 0, i);
                }
                
                if(curDir.exists()){
                	String fileDir=jarentry.getName();
                	if(fileDir.indexOf(rootDir)==0)
						fileDir=fileDir.substring((int)rootDir.length());
                	
                	String destFileName=outFile+java.io.File.separator+fileDir;
                	if(fileDir.endsWith(".xml")){
                		String content=new String(bOut.toByteArray(),"utf-8");
                		content=content.replaceAll("/images/", "current/images/");
                		org.mbc.util.Tools.writeTextFile(destFileName, content, "utf-8");
                	}else{
                		org.mbc.util.Tools.writeBytesToFile(bOut.toByteArray(), destFileName);
                		try{
                			if(!destFileName.endsWith(".txt")){
                				/*
                				Size size=getImageSize(destFileName);
                				if(size.getWidth()>0 && size.getHeight()>0){
                					if(size.getHeight()/size.getWidth()>445/600)
                						Runtime.getRuntime().exec("convert "+destFileName+" -resize 600x425 "+destFileName);
                					else
                						Runtime.getRuntime().exec("convert "+destFileName+" "+destFileName);
                				}*/
                				Runtime.getRuntime().exec("convert "+destFileName+" -resize 600x425! "+destFileName);
                			}
            			}catch(Exception ex){
            			}
                	}
                }
				//files.put(jarentry.getName(), bOut.toByteArray());
				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static Size getImageSize(String strImg){
		Size size=new Size();
		try{
			Process curProcess=Runtime.getRuntime().exec("identify "+strImg);
			
			java.io.InputStream in=curProcess.getInputStream();
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			String strError=sb.toString();
			String[] items=strError.split(" ");
			//System.out.println("error=="+sb.toString());
			String[] sects=items[2].split("x");
			size.setWidth(Integer.parseInt(sects[0]));
			size.setHeight(Integer.parseInt(sects[1]));
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return size;
	}
	
	public static String convertMp4ToFlv(String fileName){
		return convertMp4ToFlv(fileName,false);
	}
	
	public static String convertMp4ToFlv(String fileName,boolean debug){
		try{
			int[] size=getVideoSize(fileName,debug);
			int width=size[0];
			int height=size[1];
			if(width==0 || height==0){
				width=1280;
				height=720;
			}
			
			if(width==0 || height==0){
				return "";
			}
			
			if(debug)
				log.info("convertMp4ToFlv::size=w:"+width+",h:"+height);
			
			java.io.File inFile=new java.io.File(fileName);
			if(!inFile.exists())
				return "";
			String outFileName=fileName.replace(".mp4", ".flv");
			Process curProcess=Runtime.getRuntime().exec("ffmpeg -i "+fileName+" -ab 56 -ar 22050 -b 500 -r 29.97 -s "+width+"x"+height+" "+outFileName);
			
			java.io.InputStream in=curProcess.getInputStream();
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			String strError=sb.toString();
			System.out.println(strError);
			java.io.File fout=new java.io.File(outFileName);
			if(fout.exists() && fout.length()>0){
				if(!debug){
					inFile.delete();
				}
			}else{
				return "";
			}
			
			return outFileName;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return "";
	}
	
	public static String getFrameFromVideo(String fileName){
		return getFrameFromVideo(fileName,true);
	}
	
	public static String getFrameFromVideo(String fileName,boolean debug){
		try{
			int[] size=getVideoSize(fileName,false);
			int width=size[0];
			int height=size[1];
			if(width==0 || height==0){
				width=1280;
				height=720;
			}
			
			if(width==0 || height==0){
				return "";
			}
			
			
			java.io.File inFile=new java.io.File(fileName);
			if(!inFile.exists())
				return "";
			String ext=fileName.substring(fileName.lastIndexOf("."));
			String outFileName=fileName.replace(ext, ".jpg");
			Process curProcess=Runtime.getRuntime().exec("ffmpeg -y -i "+fileName+" -vframes 1 -ss 0:0:0 -an -f  image2 "+outFileName);
			
			java.io.InputStream in=curProcess.getErrorStream();
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			if(debug){
				System.out.println("outFile===>"+outFileName);
				String strError=sb.toString();
				System.out.println(strError);
			}
			java.io.File fout=new java.io.File(outFileName);
			if(fout.exists() && fout.length()>0){
			}else{
				return "";
			}
			
			return outFileName;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return "";
	}
	
	public static String recordLiveStream(String url,String fileName){
		try{
			//Process curProcess=Runtime.getRuntime().exec("G:\\焦作项目资料\\视频方案\\rtmpdumphelper\\rtmpdump.exe -r "+url+" -m 60 -v -o "+fileName);
			Process curProcess=Runtime.getRuntime().exec("rtmpdump.exe -r "+url+" -m 60 -v -o "+fileName);
			final java.io.InputStream in=curProcess.getInputStream();
			final java.io.InputStream err=curProcess.getErrorStream();
			Thread tIn=new Thread(){
				public void run(){
					byte[] b=new byte[1024];
					@SuppressWarnings("unused")
					int count;
					try {
						while((count=in.read(b))>0){
							//System.out.println("==>"+new String(b,0,count));
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			tIn.start();
			
			Thread tErr=new Thread(){
				public void run(){
					byte[] b=new byte[1024];
					@SuppressWarnings("unused")
					int count;
					try {
						while((count=err.read(b))>0){
							//System.out.println("**>"+new String(b,0,count));
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			tErr.start();
			
			curProcess.waitFor();
			tIn.join();
			tErr.join();
			tIn=null;
			tErr=null;
			log.info("录制完成:"+fileName);
			in.close();
			err.close();
			
			return "";
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return "";
	}
	
	
	public static int[] getVideoSize(String fileName,boolean debug){
		int[] size=new int[2];
		try{
			
			if(debug)
				log.info("getVideoSize::VideoFile="+fileName);
			
			java.io.File inFile=new java.io.File(fileName);
			if(!inFile.exists())
				return size;
			Process curProcess=Runtime.getRuntime().exec("ffmpeg -i "+fileName);
			
			java.io.InputStream in=curProcess.getErrorStream();
			//java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(in));
			StringBuffer sb=new StringBuffer();
			byte[] b=new byte[1024];
			int count;
			while((count=in.read(b,0,b.length))>0){
				sb.append(new String(b,0,count));
			}
			in.close();
			String strError=sb.toString();
			//System.out.println(fileName);
			if(debug)
				log.info("getVideoSize::output="+strError);
			String tag="Stream #";
			int pos=strError.indexOf(tag);
			while(pos>0){
				strError=strError.substring(pos+tag.length());
				if(strError.indexOf("Video")>0){
					strError=strError.substring(strError.indexOf("Video")+6,strError.indexOf("\n"));
					String[] items=strError.split(",");
					String sizeStr=items[2].trim();
					if(sizeStr.indexOf(" ")>0){
						sizeStr=sizeStr.substring(0,sizeStr.indexOf(" "));
					}
					items=sizeStr.split("x");
					size[0]=Integer.parseInt(items[0]);
					size[1]=Integer.parseInt(items[1]);
					break;
				}
				pos=strError.indexOf(tag);	
			}
			
			if(debug)
				log.info("getVideoSize::size="+size[0]+","+size[1]);
			//System.out.println(strError);;
			return size;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return size;
	}
	
	public static void main(String[] args){
		//String url="rtmp://101.200.217.86/live/STREAM1458204649743";
		//recordLiveStream(url,"d:\\output\\abb.flv");
		/*
		java.io.File[] files=new java.io.File("G:\\media\\ffmpeg\\bin").listFiles();
		for(java.io.File f:files){
			//String file="G:\\media\\ffmpeg\\bin\\866696025682095_20160421185033.mp4";
			@SuppressWarnings("unused")
			String file="G:\\media\\ffmpeg\\bin\\866696025682095_20160421185033.mp4";
			//int[] size=getVideoSize(f.getAbsolutePath(),true);
			//System.out.println("widht="+size[0]+",height="+size[1]);
			
			//if(size.length>0)
			//	break;
		}
		
		String file="G:\\media\\ffmpeg\\bin\\866696025682095_20160421185033.mp4";
		String img=getFrameFromVideo(file);
		System.out.println("========>"+img);
		*/
		System.out.println("str="+org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(8));
		
	}
	
}
