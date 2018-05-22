package com.hyb.utils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.geo.GeoRadiusParam;

public class RedisClient {
	private static RedisClient instance = null;
	private JedisCluster _cluster = null;
	private JedisPool _pool = null;
	private static final int PORT = 7000;

	public RedisClient(java.util.Set<HostAndPort> hostSets) {
		_cluster = new JedisCluster(hostSets);
	}

	public RedisClient(String host, int port, int timeout) {
		_pool = new JedisPool(new GenericObjectPoolConfig(), host, port, timeout);
	}

	public RedisClient(String host, int port) {
		this(host, port, 2000);
	}

	public static RedisClient getInstance() {
		return getInstance(null);
	}

	public static RedisClient getInstance(String strHosts) {
		// System.out.println("\t=====HOSTRS::"+strHosts+",instance="+instance);
		if (instance == null) {
			java.util.HashSet<HostAndPort> sets = new java.util.HashSet<HostAndPort>();
			if (strHosts != null) {
				String[] hosts = strHosts.split(",");
				if (hosts.length > 0) {
					for (int i = 0; i < hosts.length; i++) {
						String[] items = hosts[i].split(":");
						String name = items[0];
						int port = items.length > 1 ? Integer.parseInt(items[1]) : PORT;
						HostAndPort p = new HostAndPort(name, port);
						sets.add(p);
					}
				}
			}

			if (sets.size() > 1) {
				instance = new RedisClient(sets);
			} else {
				if (sets.size() == 0) {
					instance = new RedisClient("127.0.0.1", PORT);
				} else {
					HostAndPort p = sets.iterator().next();
					instance = new RedisClient(p.getHost(), p.getPort());
				}
			}
			System.out.println("\tSETS::" + sets + ",instance=" + instance.getCommand());
			instance.setStr("StartTime", "time:" + System.currentTimeMillis());

		}
		return instance;
	}

	public JedisCommands getCommand() {
		if (_cluster != null)
			return _cluster;
		return _pool.getResource();
	}

	public void setStr(String key, String val) {
		JedisCommands cmd = this.getCommand();
		cmd.set(key, val);
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public String getStr(String key) {
		String val = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			val = cmd.get(key);
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return val;
	}

	public boolean exists(String key) {
		JedisCommands cmd = this.getCommand();
		boolean bRet = cmd.exists(key);
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return bRet;
	}

	public void delStr(String key) {
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			cmd.del(key);
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public long getListSize(String key) {
		long count = 0;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			count = cmd.llen(key);
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return count;
	}

	public String getLastFromList(String key) {
		String val = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			val = cmd.lindex(key, cmd.llen(key) - 1);
		}

		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return val;
	}

	public String getAtList(String key, long pos) {
		String val = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			if (pos >= 0 && pos < cmd.llen(key)) {
				val = cmd.lindex(key, pos);
			}
		}

		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return val;
	}

	public void addToList(String key, String... value) {
		JedisCommands cmd = this.getCommand();
		cmd.rpush(key, value);
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public String popFromList(String key) {
		String val = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			if (cmd.llen(key) > 0) {
				val = cmd.lpop(key);
			}
		}

		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return val;
	}

	/**
	 * 添加地理坐标到redis
	 * @param key,货物的key=cp_goods,车辆的key=cp_cars  可以理解为分组
	 * @param longitude
	 * @param latitude
	 * @param member 名称/备注
	 */
	public void addPoint(String key, double longitude, double latitude, String member) {
		Jedis jedis = this._pool.getResource();
		jedis.geoadd(key, longitude, latitude, member);
		if (null != jedis) {
			jedis.close();
		}
	}

	/**
	 * 获得坐标附近的点信息
	 * @param key,货物的key=cp_goods,车辆的key=cp_cars  可以理解为分组
	 * @param longitude
	 * @param latitude
	 * @param distance 距离  km
	 * @return
	 */
	public JSONArray getPointNear(String key, double longitude, double latitude, double distance) {
		Jedis jedis = this._pool.getResource();
		List<GeoRadiusResponse> list = jedis.georadius(key, longitude, latitude, distance, GeoUnit.KM, GeoRadiusParam.geoRadiusParam().withDist().withCoord());
		JSONArray jary = new JSONArray();
		for (int x = 0; x < list.size(); x++) {
			JSONObject json = new JSONObject();
			double lon = list.get(x).getCoordinate().getLongitude();
			double lat = list.get(x).getCoordinate().getLatitude();
			String dis = new BigDecimal(""+list.get(x).getDistance()).toPlainString();
			String mem = list.get(x).getMemberByString();
			try {
				json.put("member", mem);
				json.put("lon", lon);
				json.put("lat", lat);
				json.put("distance", dis);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			jary.put(json);
		}
		if (null != jedis) {
			jedis.close();
		}
		return jary;
	}

	/**
	 * 获取某个点的坐标
	 * @param key,货物的key=cp_goods,车辆的key=cp_cars  可以理解为分组
	 * @param member 名称/备注
	 */
	public JSONObject getPointGps(String key, String member) {
		Jedis jedis = this._pool.getResource();
		List<GeoCoordinate> list=jedis.geopos(key, member);
		try {
			JSONObject json = new JSONObject();
			double lon = list.get(0).getLongitude();
			double lat = list.get(0).getLatitude();
			try {
				json.put("lon", lon);
				json.put("lat", lat);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if (null != jedis) {
				jedis.close();
			}
			return json;
		} catch (Exception e) {//未处理错误 jedis报错空指针
			return null;
		}
		
	}

	/**
	 * 删除某个点
	 * @param key,货物的key=cp_goods,车辆的key=cp_cars  可以理解为分组
	 * @param member 名称/备注
	 */
	public void delPoint(String key, String member) {
		Jedis jedis = this._pool.getResource();
		jedis.zrem(key, member);	
		if (null != jedis) {
			jedis.close();
		}
	}
	
	public void setExipired(String key, int expired) {
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(key)) {
			cmd.expire(key, expired);
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public void addToHash(String hashName, String key, String val) {
		JedisCommands cmd = this.getCommand();
		cmd.hset(hashName, key, val);
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public String getFromHash(String hashName, String key) {
		// System.out.println("getFromHash->"+hashName+",key="+key);
		String val = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(hashName)) {
			if (cmd.hexists(hashName, key))
				val = cmd.hget(hashName, key);
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return val;
	}

	public java.util.Set<String> getAllHashKeys(String hashName) {
		java.util.Set<String> set = null;
		JedisCommands cmd = this.getCommand();
		if (cmd.exists(hashName)) {
			try {
				set = cmd.hkeys(hashName);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return set == null ? new java.util.HashSet<String>() : set;
	}

	public void removeFromHash(String hashName, String key) {
		JedisCommands cmd = this.getCommand();
		cmd.hdel(hashName, key);
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
	}

	public long getHashSize(String hashName) {
		long cnt = 0;
		JedisCommands cmd = this.getCommand();
		cnt = cmd.hlen(hashName).longValue();
		if (cmd instanceof Jedis)
			((Jedis) cmd).close();
		return cnt;
	}
}
