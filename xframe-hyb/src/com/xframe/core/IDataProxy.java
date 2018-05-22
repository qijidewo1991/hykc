package com.xframe.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IDataProxy {
	public static final String EXEC_OK="#$success@#";
	public String doProxy(HttpServletRequest req, HttpServletResponse rep,java.util.HashMap<String, String> params)
			throws Exception;
}
