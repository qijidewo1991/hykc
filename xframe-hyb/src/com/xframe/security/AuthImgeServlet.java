package com.xframe.security;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.captcha.Captcha;
import nl.captcha.servlet.CaptchaServletUtil;

public class AuthImgeServlet extends HttpServlet
{
  private static final long serialVersionUID = 40913456229L;
  private static int _width = 200;
  private static int _height = 50;

  private static long _ttl = 600000L;

  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    if (getInitParameter("captcha-height") != null) {
      _height = Integer.valueOf(getInitParameter("captcha-height")).intValue();
    }

    if (getInitParameter("captcha-width") != null) {
      _width = Integer.valueOf(getInitParameter("captcha-width")).intValue();
    }

    if (getInitParameter("ttl") != null)
      _ttl = Long.valueOf(getInitParameter("ttl")).longValue();
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
  {
    HttpSession session = req.getSession();
    
    if(req.getParameter("t")!=null){
    	if(session.getAttribute("simpleCaptcha") != null){
    		session.removeAttribute("simpleCaptcha");
    	}
    }

    if (session.getAttribute("simpleCaptcha") == null) {
      Captcha captcha = buildAndSetCaptcha(session);
      CaptchaServletUtil.writeImage(resp, captcha.getImage());
      return;
    }

    Captcha captcha = (Captcha)session.getAttribute("simpleCaptcha");
    if (shouldExpire(captcha)) {
      captcha = buildAndSetCaptcha(session);
      CaptchaServletUtil.writeImage(resp, captcha.getImage());
      return;
    }

    CaptchaServletUtil.writeImage(resp, captcha.getImage());
  }

  private Captcha buildAndSetCaptcha(HttpSession session) {
    Captcha captcha = new Captcha.Builder(_width, _height)
      .addText()
      .gimp()
      .addBorder()
      .addNoise()
      .addBackground()
      .build();

    session.setAttribute("simpleCaptcha", captcha);
    return captcha;
  }

  static void setTtl(long ttl)
  {
    if (ttl < 0L) {
      ttl = 0L;
    }

    _ttl = ttl;
  }

  static long getTtl()
  {
    return _ttl;
  }

  static boolean shouldExpire(Captcha captcha)
  {
    long ts = captcha.getTimeStamp().getTime();
    long now = new Date().getTime();
    long diff = now - ts;

    return diff >= _ttl;
  }
}
