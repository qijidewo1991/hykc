package com.xframe.utils;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.*;

public class CacheService {
	public static final String CACHE_DEFAULT="eway_cache";
	public static final String CACHE_TEMP="eway_temp";
	private static Logger log=Logger.getLogger(CacheService.class);
	private static CacheManager manager=null;
	
	private static CacheService instance=null;
	
	public static CacheService getInstance(){
		if(instance==null)
			instance=new CacheService();
		
		return instance;
	}
	
	private CacheService(){
	}
	
	public void destroyCache() {
		// TODO Auto-generated method stub
		CacheManager.getInstance().shutdown();
		
		log.warn("CACHE DESTROIED!!");
	}

	public void flushCache(String cacheName) {
		// TODO Auto-generated method stub
		Cache cache=manager.getCache(cacheName);
		if(cache==null)
			return;
		cache.removeAll();
	}

	public Object getFromCache(String key, String cacheName) {
		// TODO Auto-generated method stub
		Cache cache=null;
		if(cacheName==null)
			cache=manager.getCache(CACHE_DEFAULT);
		else
			cache=manager.getCache(cacheName);
		
		if(cache==null)
			return null;
		Element element=cache.get(key);
		if(element==null)
			return null;
		
		return element.getObjectValue();
	}

	public Object getFromCache(String key) {
		// TODO Auto-generated method stub
		return this.getFromCache(key,null);
	}

	public void initCache(String xmlfile) {
		// TODO Auto-generated method stub
		
		manager = new CacheManager(xmlfile);
		
		log.info("≥ı ºªØª∫¥Ê........."+manager);
	}

	public void putInCache(String key, Object obj, String cacheName) {
		// TODO Auto-generated method stub
		Cache cache=null;
		if(cacheName==null)
			cache=manager.getCache(CACHE_DEFAULT);
		else
			cache=manager.getCache(cacheName);
		if(cache==null)
			return;
		Element element = new Element(key, obj);
		cache.put(element);
		
		//log.info("[CACHE::]"+cache);
	}

	public void putInCache(String key, Object obj) {
		// TODO Auto-generated method stub
		this.putInCache(key, obj, null);
	}

	public void removeEntry(String key, String cacheName) {
		// TODO Auto-generated method stub
		Cache cache=null;
		if(cacheName==null)
			cache=manager.getCache(CACHE_DEFAULT);
		else
			cache=manager.getCache(cacheName);
		
		if(cache.getKeys().contains(key))
    		cache.remove(key);
	}
	
	public boolean containsKey(String key,String cacheName){
		Cache cache=null;
		if(cacheName==null)
			cache=manager.getCache(CACHE_DEFAULT);
		else
			cache=manager.getCache(cacheName);
		return cache.getKeys().contains(key);
	}
	
	public boolean containsKey(String key){
		return containsKey(key,null);
	}

	public void removeEntry(String key) {
		// TODO Auto-generated method stub
		this.removeEntry(key, null);
	}


	public void destroy() {
		// TODO Auto-generated method stub
		this.destroyCache();
	}

	public boolean initialize(javax.servlet.ServletContext appContext) {
		// TODO Auto-generated method stub
		
		String ehcache=appContext.getRealPath("/WEB-INF/ehcache.xml");
		this.initCache(ehcache);
		return true;
	}
}
