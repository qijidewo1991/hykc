package com.xframe.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PYUtility {
		/**
	    * ºº×Ö×ª»»Î»ººÓïÆ´ÒôÊ××ÖÄ¸£¬Ó¢ÎÄ×Ö·û²»±ä
	    * @param chines ºº×Ö
	    * @return Æ´Òô
	    */
	    public static String converterToFirstSpell(String chines){    	 
	        String pinyinName = "";
	        char[] nameChar = chines.toCharArray();
	        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
	        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
	        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
	        for (int i = 0; i < nameChar.length; i++) {
	            if (nameChar[i] > 128) {
	                try {
	                    pinyinName += PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat)[0].charAt(0);
	                } catch (BadHanyuPinyinOutputFormatCombination e) {
	                    e.printStackTrace();
	                }
	            }else{
	            	pinyinName += nameChar[i];
	            }
	        }
	        return pinyinName;
	    }
	    
	    /**
	     * ºº×Ö×ª»»Î»ººÓïÆ´Òô£¬Ó¢ÎÄ×Ö·û²»±ä
	     * @param chines ºº×Ö
	     * @return Æ´Òô
	     */
	     public static String converterToSpell(String chines){    	 
	         String pinyinName = "";
	         char[] nameChar = chines.toCharArray();
	         HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
	         defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
	         defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
	         for (int i = 0; i < nameChar.length; i++) {
	             if (nameChar[i] > 128) {
	                 try {
	                     pinyinName += PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat)[0];
	                 } catch (BadHanyuPinyinOutputFormatCombination e) {
	                     e.printStackTrace();
	                 }
	             }else{
	             	pinyinName += nameChar[i];
	             }
	         }
	         return pinyinName;
	     }
	     
	     public static void main(String[] args) {
	 		System.out.println(converterToFirstSpell("s»¶Ó­À´µ½×î°ôµÄJavaÖÐÎÄÉçÇø"));
	 		System.out.println(converterToSpell("ÐÝÏÐÓéÀÖ_point.shx"));
	     }

}
