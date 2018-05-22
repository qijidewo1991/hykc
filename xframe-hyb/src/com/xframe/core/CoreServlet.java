package com.xframe.core;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.hyb.utils.AccountProcessThread;
import com.hyb.utils.Configuration;
import com.hyb.utils.RedisClient;
import com.hyb.utils.VehCheckThread;
import com.hyb.utils.CityDistributionThread;
import com.xframe.utils.AppHelper;
import com.xframe.utils.CacheService;

public class CoreServlet extends HttpServlet {
	
	private static final long serialVersionUID = -2792906202835542795L;
	
	public static Logger log=Logger.getLogger(CoreServlet.class);
	
	private INetManager manager=null;
	
	public static String ARTICLES_JSP="files/articles.jsp";
	
	public static String CONF_ROOT="";
	
	//public static ApplicationContext appContext=null;
	
	public static org.hibernate.SessionFactory h2Factory=null;
	
	public static ZkMonitor zkMonitor=null;
	
	
	@Override
	public void init() throws ServletException {
		String logcfg=this.getInitParameter("log4j-config");
		if(logcfg!=null){
			String filename=this.getServletContext().getRealPath(logcfg);
			org.apache.log4j.xml.DOMConfigurator.configure(filename);
		}
		
		log.info("初始化XFRAME环境............");
		
		CONF_ROOT=this.getServletContext().getRealPath("WEB-INF");
		
		String strMaster=this.getInitParameter("HMaster");
		String strQuorum=this.getInitParameter("Quorum");
		String strClientPort=this.getInitParameter("ClientPort");
		boolean bRet=com.xframe.utils.HBaseUtils.initializeHBase(strMaster, strQuorum, strClientPort);
		if(!bRet){
			log.error("初始化HBASE失败!");
		}else{
			log.info("初始化HBASE成功!");
		}
		
		
		
		CacheService.getInstance().initialize(this.getServletContext());
		
		//初始化配置文件路径
		AppHelper.SYSCONF=this.getServletContext().getRealPath("WEB-INF/sysconf.properties");
		
		String serverId="TOMCAT"+System.currentTimeMillis();
		if(this.getInitParameter("ServerId")!=null)
			serverId=this.getInitParameter("ServerId");
		
		//初始化zkclient
		if(zkMonitor==null){
			zkMonitor=new ZkMonitor(serverId);
			zkMonitor.init(strQuorum);
		}
				
				
		
		startManager();
		
		String strHosts=this.getInitParameter("redisHosts")==null?"127.0.0.1:7000":this.getInitParameter("redisHosts");
		//System.out.println("###REDIS-CLIENT:"+strHosts);
		RedisClient.getInstance(strHosts);
		//载入系统定义的参数信息
		Configuration.loadConfiguration();
		//添加车辆监控线程
		new VehCheckThread().start();
		
		//账户处理线程
		new AccountProcessThread().start();
		
		//城配接货处理线程
//		new CityDistributionThread().start();
		
		ARTICLES_JSP=this.getServletContext().getRealPath(ARTICLES_JSP);
		log.info("初始化环境.............完成!");
		
		//初始化支付宝参数
		initPayParameters();
		
		try{
			org.hibernate.cfg.Configuration cfg = new org.hibernate.cfg.Configuration();
			h2Factory = cfg.configure().buildSessionFactory();

		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		/*
		java.io.File f=new java.io.File(getServletContext().getRealPath("/WEB-INF/hibernate/beans.xml"));
		log.info("initializing h2 engine...."+f.exists());
		String path=f.getAbsolutePath();
		try{
			if(System.getProperty("os.name").equals("Linux"))
				path="/"+path;
			appContext=new FileSystemXmlApplicationContext(path);
			log.info("appContext=="+appContext);
		}catch(Exception ex){
			ex.printStackTrace();
			log.error("异常:"+ex.toString()+",RELOAD FILE FROM /");
			appContext=new FileSystemXmlApplicationContext("/"+path);
		}
		
		h2Factory = (org.hibernate.SessionFactory)appContext.getBean("h2SessionFactory");
		log.info("H2Factory=="+h2Factory);
		//*/
	}
	
	private void startManager(){
		new Thread(){
			public void run(){
				//manager=SocketManager.getInstance();
				if(manager==null){
					manager=MqttManagerV3.getInstance();
					manager.setMsgListener(new MqttMessageProcessor());
					manager.startManager();
				}
			}
		}.start();
		
	}
	
	private void initPayParameters(){
		java.util.HashMap<String, String> conf=AppHelper.getSystemConf();
	}
}
