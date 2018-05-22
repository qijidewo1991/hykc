package com.xframe.core;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Text2Image extends HttpServlet {
	private static final long serialVersionUID = -3183648675406339273L;
	private static String[] fontList=null;
	protected void service(HttpServletRequest req, HttpServletResponse rep)
			throws ServletException, IOException {
		
		
		byte[] data=getImageData(req,rep);
		if(data==null)
			return;
		rep.setContentType("image/png");
		rep.setContentLength(data.length);
		java.io.OutputStream out=rep.getOutputStream();
		try{
			out.write(data);
			rep.flushBuffer();
		}catch(Exception ex){
			System.out.println("exception:: "+ex.toString());
			//ex.printStackTrace();
		}finally{
			out.close();
		}
	}
	
	private static BufferedImage createBufferedImage(int width,int height,String text,String fontFamily,int fontSize,boolean bold,String color){
		if(fontList==null){
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            fontList = ge.getAvailableFontFamilyNames();
		}
		
		boolean isFound=false;
		String family=fontFamily;
		for(String f:fontList){
			System.out.println("\tFONT=="+f);
			if(f.equals(family)){
				isFound=true;
				break;
			}
		}
		
		if(!isFound){
			family="SimHei";
			
		}
		
		Font font=new Font(fontFamily,bold?Font.BOLD:Font.PLAIN,fontSize);
		FontMetrics fm = sun.font.FontDesignMetrics.getMetrics(font);
		int rh=fm.getHeight();
		int rw=fm.stringWidth(text);
		
		BufferedImage bi=new BufferedImage(rw,rh,BufferedImage.TRANSLUCENT);
		Graphics2D g2d=bi.createGraphics();
		g2d.setColor(parseColor(color));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2d.setFont(font);
		
		g2d.drawString(text, 0,rh-fm.getDescent()-fm.getLeading());
		//System.out.println("y==="+(height>fontSize?(height-(height-fontSize)/2):height));
		//g2d.drawString(text, 0,40);
		g2d.dispose();
		return bi;
	}
	
	private byte[] getImageData(HttpServletRequest req,HttpServletResponse rep){
		/*
		java.util.Enumeration<String> hnames=req.getHeaderNames();
		while(hnames.hasMoreElements()){
			String k=hnames.nextElement();
			String v=req.getHeader(k);
			//System.out.println("\tHEADER["+k+"]="+v);
		}*/
		//String agent=req.getHeader("user-agent");
		//System.out.println("anget="+agent+",req encodeing="+req.getCharacterEncoding()+",rep enc="+rep.getCharacterEncoding());
		
		String strEnc="gbk";//agent.indexOf("AppleWebKit")>0?"utf-8":"gbk";
		StringBuffer sb=new StringBuffer();
		for(String name:req.getParameterMap().keySet()){
			String v=req.getParameter(name);
			try {
				sb.append("&"+name+"="+new String(v.getBytes("iso-8859-1"),strEnc));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//System.out.println("Txt2Img:"+sb.toString());
		String imgid=org.mbc.util.MD5Tools.createMessageDigest(sb.toString());
		java.io.File file=new java.io.File(req.getServletContext().getRealPath("shared/temp/"+imgid+".png"));
		if(file.exists()){
			return org.mbc.util.Tools.readBytesFromFile(file.getAbsolutePath());
		}
		String font=req.getParameter("font");
		String size=req.getParameter("size");
		String bold=req.getParameter("bold");
		String text=req.getParameter("text");
		String color=req.getParameter("color")!=null?req.getParameter("color"):"000000";
		
		try {
			if(font!=null)
				font=new String(font.getBytes("iso-8859-1"),strEnc);
			if(text!=null)
				text=new String(text.getBytes("iso-8859-1"),strEnc);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("==========="+text+",font="+font);
		boolean isBold=bold!=null && bold.equals("true");
		int fontSize=16;
		int width=100;
		int height=30;
		try{
			fontSize=Integer.parseInt(size);
		}catch(Exception ex){
		}
		
		try{
			width=Integer.parseInt(req.getParameter("width"));
		}catch(Exception ex){
		}
		
		try{
			height=Integer.parseInt(req.getParameter("height"));
		}catch(Exception ex){
		}
		
		try {
			BufferedImage image=createBufferedImage(width,height,text,font,fontSize,isBold,color);
			java.io.ByteArrayOutputStream bout=new java.io.ByteArrayOutputStream();
			javax.imageio.ImageIO.write(image, "png",bout );
			byte[] raw=bout.toByteArray();
			org.mbc.util.Tools.writeBytesToFile(raw, file.getAbsolutePath());
			return raw;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static Color parseColor(String strColor){
		if(strColor.length()!=6)
			return Color.black;
		
		try{
			int[] sects=new int[3];
			String sectItem;
			for(int i=0;i<3;i++){
				sectItem=strColor.substring(i*2,i*2+2);
				sects[i]=Integer.parseInt(sectItem, 16);
				//System.out.println("item="+sectItem+",val="+sects[i]);
			}
			return new Color(sects[0],sects[1],sects[2]);
		}catch(Exception ex){
			
		}
		
		
		return Color.black;
	}
}
