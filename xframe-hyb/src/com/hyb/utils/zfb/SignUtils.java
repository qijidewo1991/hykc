package com.hyb.utils.zfb;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class SignUtils {

	private static final String ALGORITHM = "RSA";

	private static final String SIGN_ALGORITHMS = "SHA1WithRSA";

	private static final String DEFAULT_CHARSET = "UTF-8";

	public static String sign(String content, String privateKey) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(
					Base64.decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance(ALGORITHM);
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);

			java.security.Signature signature = java.security.Signature
					.getInstance(SIGN_ALGORITHMS);

			signature.initSign(priKey);
			signature.update(content.getBytes(DEFAULT_CHARSET));

			byte[] signed = signature.sign();

			return Base64.encode(signed);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static void main(String[] args){
		
		try{
			//byte[] key=Base64.decode("MIICdAIBADANBgkqhkiG9w0BAQEFAASCAl4wggJaAgEAAoGBAO+oybaovQaWBw8QE/meS5oAy57IQ7Db/cAipqhwao6+KH6AmYwoLSVPmyoBsjeREU4Mi7Rkw6LEeCwUB9AiGpeviz9/81VzAsj+fQ9+Th3B2/aeO7CWihGZ2jhMwwSMjo3/MPQBgleQx86DzKtro4g0DijPIhi/pBIG7pBfdh8ZAgMBAAECf3yCNEmxYIMLbp9kuvv0QVLBFwhnAsPfhvVLC0p3HOUL5f3S0fL+7HV15ibsuqojs9nYqrwNnZXyfHxp66U2Epm6KoQl+P2R+jTlGv9M008IAfFv+gYx/wR5ZApHV3UUirH8irCODwLtSBO1ZGq5GTtbSu7A3UmBwjPSy2hthPUCQQD9QaubO33WwF3irlMBLCA+kYzN9j72bMSLctJGW7pc5Ye+6vosV5NTu/7lO/V1uTy+w1bZEkzCWjklF6duIUwTAkEA8kFo8UA8lJ89KK440ipxRwM+nHSs9rEaGxd9wkzZvCxYAExfqWF++R0gewP1QephrgblHt+ZOstef64LKUp1owJBAMm9vGPmGjIt/xwJ3dk7O1xcOZwAItvOfSrQhqzBeU1zEpWFPVCBWr0DLuOQxdHHg5o6pT46E6dmk8r2csuJ/r0CQFL+PamIzDhOZXFuXmEB0VT2s5h+EFNjUQI/BgJuuZlRx8QyZgNtN0a4x8vdC0TNuzEXg58UEzalPXaYdZJrw+kCQDLM2h4i3fM1sQl4KHRBfbYhCCww9uJR++DypgdYR6ovmg1C8fRfqiSSgfuTNXa13PG0ZhrH4xp8uODRvhRB1QA=");
			//System.out.println("k=="+key);
			int t=new java.util.Random().nextInt(1000);
			java.text.NumberFormat numFormat=java.text.NumberFormat.getInstance();
			numFormat.setGroupingUsed(false);
			numFormat.setMinimumIntegerDigits(3);
			System.out.println("t="+numFormat.format(t));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

}
