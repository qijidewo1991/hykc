package com.xframe.security;

public interface IAuthMethod {
	public AuthedUser authenticate(String userName, String password,String authCode,String answer) throws Exception;
}
