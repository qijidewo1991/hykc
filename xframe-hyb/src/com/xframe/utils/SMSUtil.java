package com.xframe.utils;

import java.util.List;
import java.util.Random;

import com.shcm.bean.BalanceResultBean;
import com.shcm.bean.ReplyBean;
import com.shcm.bean.SendResultBean;
import com.shcm.bean.SendStateBean;
import com.shcm.send.DataApi;
import com.shcm.send.OpenApi;

public class SMSUtil {
	private static String sOpenUrl = "http://smsapi.c123.cn/OpenPlatform/OpenApi";
	private static String sDataUrl = "http://smsapi.c123.cn/DataPlatform/DataApi";
	
	// 接口帐号
	private static final String account = "1001@501038520002";
	
	// 接口密钥
	private static final String authkey = "B55B389DA3806ABCED04E7735D7D42EA";
	
	// 通道组编号
	private static final int cgid = 184;
	
	// 默认使用的签名编号(未指定签名编号时传此值到服务器)
	private static final int csid = 0;
	
	public static List<SendResultBean> sendOnce(String mobile, String content) throws Exception
	{
		
		// 发送短信
		return OpenApi.sendOnce(mobile, content, 0, 0, null);
	}
	
	public static void sendSms2(String mobile,String msg) throws Exception{
		System.out.println("发送信息::"+mobile+",msg="+msg);
	}
	
	public static boolean sendSms(String mobile,String msg) throws Exception
	{
		System.out.println("发送信息::"+mobile+",msg="+msg);
		// 发送参数
		OpenApi.initialzeAccount(sOpenUrl, account, authkey, cgid, csid);
		
		// 状态及回复参数
		DataApi.initialzeAccount(sDataUrl, account, authkey);
		
		// 取帐户余额
		BalanceResultBean br = OpenApi.getBalance();
		if(br == null)
		{
			System.out.println("获取可用余额时发生异常!");
			return false;
		}
		
		if(br.getResult() < 1)
		{
			System.out.println("获取可用余额失败: " + br.getErrMsg());
			return false;
		}
		System.out.println("可用条数: " + br.getRemain());
		
		
		List<SendResultBean> listItem = sendOnce(mobile, msg);
		if(listItem != null)
		{
			for(SendResultBean t:listItem)
			{
				if(t.getResult() < 1)
				{
					System.out.println("发送提交失败: " + t.getErrMsg());
				}else{
					System.out.println("发送成功: 消息编号<" + t.getMsgId() + "> 总数<" + t.getTotal() + "> 余额<" + t.getRemain() + ">");
					return true;
				}
				
				
			}
		}
		
		return false;
			
	}
	
	public static void receiveSms() throws Exception
	{
		// 发送参数
		OpenApi.initialzeAccount(sOpenUrl, account, authkey, cgid, csid);
		
		// 状态及回复参数
		DataApi.initialzeAccount(sDataUrl, account, authkey);
		
		List<SendStateBean> listSendState = DataApi.getSendState();
		if(listSendState != null)
		{
			for(SendStateBean t:listSendState)
			{
				System.out.println("状态报告 => 序号<" + t.getId() + "> 消息编号<" + t.getMsgId() + "> 手机号码<" + t.getMobile() + "> 结果<" + (t.getStatus() > 1 ? "发送成功" : t.getStatus() > 0 ? "发送失败" : "未知状态") + "> 运营商返回<" + t.getDetail() + ">");
			}
		}
		
		// 取回复
		List<ReplyBean> listReply = DataApi.getReply();
		if(listReply != null)
		{
			for(ReplyBean t:listReply)
			{
				System.out.println("回复信息 => 序号<" + t.getId() + "> 消息编号<" + t.getMsgId() + "> 回复时间<" + t.getReplyTime() + "> 手机号码<" + t.getMobile() + "> 回复内容<" + t.getContent() + ">");
			}
		}
	}
	
	public static void main(String[] args){
		java.util.Random random=new java.util.Random();
		System.out.println("int=="+random.nextInt(10000));
		try{
			sendSms("18638100868","我的测试INXS,欢迎使用债务宝.. ok?。。。");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
