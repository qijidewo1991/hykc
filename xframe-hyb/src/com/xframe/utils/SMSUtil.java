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
	
	// �ӿ��ʺ�
	private static final String account = "1001@501038520002";
	
	// �ӿ���Կ
	private static final String authkey = "B55B389DA3806ABCED04E7735D7D42EA";
	
	// ͨ������
	private static final int cgid = 184;
	
	// Ĭ��ʹ�õ�ǩ�����(δָ��ǩ�����ʱ����ֵ��������)
	private static final int csid = 0;
	
	public static List<SendResultBean> sendOnce(String mobile, String content) throws Exception
	{
		
		// ���Ͷ���
		return OpenApi.sendOnce(mobile, content, 0, 0, null);
	}
	
	public static void sendSms2(String mobile,String msg) throws Exception{
		System.out.println("������Ϣ::"+mobile+",msg="+msg);
	}
	
	public static boolean sendSms(String mobile,String msg) throws Exception
	{
		System.out.println("������Ϣ::"+mobile+",msg="+msg);
		// ���Ͳ���
		OpenApi.initialzeAccount(sOpenUrl, account, authkey, cgid, csid);
		
		// ״̬���ظ�����
		DataApi.initialzeAccount(sDataUrl, account, authkey);
		
		// ȡ�ʻ����
		BalanceResultBean br = OpenApi.getBalance();
		if(br == null)
		{
			System.out.println("��ȡ�������ʱ�����쳣!");
			return false;
		}
		
		if(br.getResult() < 1)
		{
			System.out.println("��ȡ�������ʧ��: " + br.getErrMsg());
			return false;
		}
		System.out.println("��������: " + br.getRemain());
		
		
		List<SendResultBean> listItem = sendOnce(mobile, msg);
		if(listItem != null)
		{
			for(SendResultBean t:listItem)
			{
				if(t.getResult() < 1)
				{
					System.out.println("�����ύʧ��: " + t.getErrMsg());
				}else{
					System.out.println("���ͳɹ�: ��Ϣ���<" + t.getMsgId() + "> ����<" + t.getTotal() + "> ���<" + t.getRemain() + ">");
					return true;
				}
				
				
			}
		}
		
		return false;
			
	}
	
	public static void receiveSms() throws Exception
	{
		// ���Ͳ���
		OpenApi.initialzeAccount(sOpenUrl, account, authkey, cgid, csid);
		
		// ״̬���ظ�����
		DataApi.initialzeAccount(sDataUrl, account, authkey);
		
		List<SendStateBean> listSendState = DataApi.getSendState();
		if(listSendState != null)
		{
			for(SendStateBean t:listSendState)
			{
				System.out.println("״̬���� => ���<" + t.getId() + "> ��Ϣ���<" + t.getMsgId() + "> �ֻ�����<" + t.getMobile() + "> ���<" + (t.getStatus() > 1 ? "���ͳɹ�" : t.getStatus() > 0 ? "����ʧ��" : "δ֪״̬") + "> ��Ӫ�̷���<" + t.getDetail() + ">");
			}
		}
		
		// ȡ�ظ�
		List<ReplyBean> listReply = DataApi.getReply();
		if(listReply != null)
		{
			for(ReplyBean t:listReply)
			{
				System.out.println("�ظ���Ϣ => ���<" + t.getId() + "> ��Ϣ���<" + t.getMsgId() + "> �ظ�ʱ��<" + t.getReplyTime() + "> �ֻ�����<" + t.getMobile() + "> �ظ�����<" + t.getContent() + ">");
			}
		}
	}
	
	public static void main(String[] args){
		java.util.Random random=new java.util.Random();
		System.out.println("int=="+random.nextInt(10000));
		try{
			sendSms("18638100868","�ҵĲ���INXS,��ӭʹ��ծ��.. ok?������");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
