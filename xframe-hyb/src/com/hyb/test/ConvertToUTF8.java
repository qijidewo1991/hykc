package com.hyb.test;

import java.io.UnsupportedEncodingException;

public class ConvertToUTF8 {
	private static java.util.HashMap<String, String> map=new java.util.HashMap<String, String>();
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String baseDir="D:\\StudioProjects\\huoyunbao-driver";
		map.put("WalletActivity.java", "1");
		map.put("Helper.java", "1");
		map.put("FFmpegPreviewActivity.java", "1");
		map.put("NewFFmpegFrameRecorder.java", "1");
		map.put("Util.java", "1");
		travseFiles(new java.io.File(baseDir));
		
	}
	
	public static void travseFiles(java.io.File parentDir){
		for(java.io.File file:parentDir.listFiles()){
			if(file.isDirectory()){
				System.out.println("###"+file.getName());
				travseFiles(file);
			}else{
				String strPath=file.getAbsolutePath();
				// || (strPath.indexOf("\\assets\\") && strPath.endsWith(".data"))
				if((strPath.indexOf("\\src\\")>0 && strPath.endsWith(".java")) || ((strPath.indexOf("\\assets\\")>0 && strPath.endsWith(".data")))){
					System.out.println("==>"+file.getAbsolutePath());
					String strText=org.mbc.util.Tools.loadTextFile(strPath,"gbk");
					
					System.out.println("\t"+file.getName());
					if(!map.containsKey(file.getName())){
						//org.mbc.util.Tools.writeTextFile(strPath, strText, "utf-8");
					}
					/*
					try {
						strText=new String(strText.getBytes("gbk"),"utf-8");
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					*/
				}
			}
		}
	}

}
