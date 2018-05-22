package com.xframe.security;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class MyHostNameVerifier implements HostnameVerifier{
	
    @Override
    public boolean verify(String hostname, SSLSession session) {
            // TODO Auto-generated method stub
    	
            return true;
    }
}