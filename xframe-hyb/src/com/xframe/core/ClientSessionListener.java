package com.xframe.core;

import java.util.HashSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


public class ClientSessionListener implements HttpSessionListener {
	@SuppressWarnings("unused")
	private static final org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(ClientSessionListener.class);
	@Override
	public void sessionCreated(HttpSessionEvent event) {
		// TODO Auto-generated method stub
		HttpSession session = event.getSession();
		ServletContext application = session.getServletContext();
		@SuppressWarnings("unchecked")
		HashSet<HttpSession> sessions = (HashSet<HttpSession>) application.getAttribute("sessions");
        if (sessions == null) {
               sessions = new HashSet<HttpSession>();
               application.setAttribute("sessions", sessions);
        }
       //log.info("$$$$$$$SESSION CREATED:"+session.getId());
        // �´�����session����ӵ�HashSet����
        sessions.add(session);

        
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		// TODO Auto-generated method stub
		 HttpSession session = event.getSession();
		 //log.info("$$$$$$$SESSION DESTROYED!:"+session.getId());
         ServletContext application = session.getServletContext();
         @SuppressWarnings("unchecked")
         HashSet<HttpSession> sessions = (HashSet<HttpSession>) application.getAttribute("sessions");
         
         // ���ٵ�session����HashSet�����Ƴ�
         sessions.remove(session);
	}

}
