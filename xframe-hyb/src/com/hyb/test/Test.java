package com.hyb.test;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import com.xframe.utils.CacheService;
import com.xframe.utils.Tools;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		java.util.Calendar c=java.util.Calendar.getInstance();
		c.add(java.util.Calendar.DAY_OF_YEAR, -15);
		System.out.println("c=="+c.getTime());
		System.out.println("======"+isMobileNumber("18400001111"));
		
		java.util.Date d=Tools.parseDate("yyyy-MM", "2016-12");
		System.out.println("date="+d);
		c.setTime(d);
		c.add(java.util.Calendar.MONTH, 1);
		c.add(java.util.Calendar.DAY_OF_MONTH,-1);
		System.out.println("date1="+c.getTime());
		
		java.util.Calendar c1=java.util.Calendar.getInstance();
		for(int i=0;i<10;i++){
			try{
				Thread.sleep(1000);
				System.out.println(org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", c1.getTime()));
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
	public static boolean isMobileNumber(String strIn){
		Pattern p = Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$");

		Matcher m = p.matcher(strIn);
		
		return m.matches();

	}
	
	public static void test1(String order_no){
		long lStart=System.nanoTime();
		org.json.JSONObject ret=new org.json.JSONObject();
		try {
			ret.put("success", "true");
			while(true){
				try{
					Thread.sleep(300);
				}catch(Exception ex){
				}
				
				if(CacheService.getInstance().containsKey(order_no)){
					CacheService.getInstance().removeEntry(order_no);
					
					break;
				}
				
				if(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime()-lStart)>30){
					ret.put("success", "false");
					ret.put("message", "付款超时,请查询账单是否付款成功.");
					break;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
