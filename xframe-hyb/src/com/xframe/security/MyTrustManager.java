package com.xframe.security;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


public class MyTrustManager implements X509TrustManager{
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(MyTrustManager.class);
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            // TODO Auto-generated method stub
    	log.info("checkClientTrusted");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            // TODO Auto-generated method stub
    	log.info("checkServerTrusted");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
            // TODO Auto-generated method stub
    	log.info("getAcceptedIssuers");
        return null;
    }        
}