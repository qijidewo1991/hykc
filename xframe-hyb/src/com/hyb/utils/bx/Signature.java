package com.hyb.utils.bx;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import org.json.JSONObject;


public class Signature {
	
	private static String byte2hex(byte[] b){
		StringBuilder strDes = new StringBuilder();
		String temp = null;
		for (int i = 0; i < b.length; i++) {
			temp = (Integer.toHexString(b[i] & 0xFF));
			if(temp.length() == 1){
				strDes.append("0");
			}
			strDes.append(temp);
		}
		return strDes.toString();
	}
	
	private String encrypt(String strSrc) {  
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String strDes = null;  
        byte[] bt = strSrc.getBytes();  
        digest.update(bt);  
        strDes = byte2hex(digest.digest());  
        return strDes;  
    }  
	
	
	/** 
     *  
     * �������̣� 
     * 1. ��token��timestamp��nonce�������������ֵ������� 
     * 2. �����������ַ���ƴ�ӳ�һ���ַ�������sha1���� 
     * @param toekn 
     * @param timestamp 
     * @param nonce 
     * @return 
     */  
    public String getSign(String token, String timestamp, String nonce){  
        //1. ��token��timestamp��nonce�������������ֵ�������  
        String[] arrTmp = { token, timestamp, nonce };  
        Arrays.sort(arrTmp);  
        StringBuffer sb = new StringBuffer();  
        //2.�����������ַ���ƴ�ӳ�һ���ַ�������sha1����  
        for (int i = 0; i < arrTmp.length; i++) {  
            sb.append(arrTmp[i]);  
        }  
        String expectedSignature = encrypt(sb.toString());  
        return expectedSignature;
    }  
	
    /**
	 * ����ַ���
	 * 
	 * @return
	 */
	private static String create_nonce_str() {
		return UUID.randomUUID().toString();
	}

	/**
	 * ʱ���
	 * 
	 * @return
	 */
	private static String create_timestamp() {
		return Long.toString(System.currentTimeMillis() / 1000);
	}
	
	
	public static void main(String[] args) {  
        
		String timestamp=create_nonce_str();//ʱ���  
        String nonce=create_timestamp();//�����  
        String token = "7caefe35d6bc19a67e6e8d0e564";//�찲�Ṱ��token
        Signature signature = new Signature();
        String sign = signature.getSign("kn1jZn35TviDxTaWT5tTjg6qnjSTF4nSN6eVa6N9","1486541081","c19aacdd-f2a4-48df-a78a-994949d16556");
        System.out.println(sign); 
    }  
    
	public static org.json.JSONObject getSignature(String token){
		org.json.JSONObject sig=new org.json.JSONObject();
		try{
			String timestamp=create_timestamp();//ʱ���  
	        String nonce=create_nonce_str();//�����  
	        Signature signature = new Signature();
	        String sign = signature.getSign(token,timestamp,nonce);
	        
	        sig.put("timestamp", timestamp);
	        sig.put("nonce", nonce);
	        sig.put("sign", sign);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return sig;
	}
}

