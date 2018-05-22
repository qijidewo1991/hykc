package com.xframe.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hyb.utils.AppUtil;
import com.hyb.utils.Configuration;
import com.hyb.utils.RedisClient;
import com.hyb.utils.VehPosition;
import com.xframe.utils.AppHelper;
import com.xframe.utils.CacheService;
import com.xframe.utils.FilterListBuilder;
import com.xframe.utils.HBaseUtils;
import com.zhiyun.config.ZhiyunConfig;

public class MqttMessageProcessor implements MessageListener {
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MqttMessageProcessor.class);
	private java.util.HashMap<String, String> sysconf = AppHelper.getSystemConf();
	public static java.util.concurrent.ConcurrentHashMap<String, java.util.ArrayList<String>> msgQueue = new java.util.concurrent.ConcurrentHashMap<String, java.util.ArrayList<String>>();
	public static java.util.concurrent.ConcurrentHashMap<String, java.util.HashSet<String>> topics = new java.util.concurrent.ConcurrentHashMap<String, java.util.HashSet<String>>();
	public static java.util.concurrent.ConcurrentLinkedQueue<String> broadcastQueue = new java.util.concurrent.ConcurrentLinkedQueue<String>();
	public static RedisClient redis = RedisClient.getInstance(""+ZhiyunConfig.redisurl);//"10.1.1.3:7000,10.1.1.4:7000,10.1.1.38:7000"

	public MqttMessageProcessor() {
	}

	@Override
	public void onMessageArrived(String topic, String strmsg) {
		// TODO Auto-generated method stub
		if (strmsg.equals("keepalive"))
			return;
		if (topic.equals(MqttManagerV3.TOPIC_GPS)) {
			// log.info("GPS:"+strmsg);
			processGpsMessage(strmsg);
		} else if (topic.equals(MqttManagerV3.TOPIC_LOCAL)) {
			// log.info("GPS:"+strmsg);
			processLocalMessage(strmsg);
		} else if (topic.equals(MqttManagerV3.TOPIC_BROADCAST)) {

		}
	}

	/**
	 * 处理Local消息 消息组成: HEADER+0x02+FROM+0x02+TO+0x02+BODY
	 * 
	 * @param msg
	 */
	private void processLocalMessage(String msg) {
		String[] items = msg.split("" + (char) 0x02);
		final String type = items[0]; // 消息类型
		final String from = items[1]; // 发送者ID
		final String to_user = items[2];// 目标用户ID
		final String body = items[3]; // 消息内容
		java.util.Date now = new java.util.Date();
		final String strNow = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", now);
		log.info(type + "===>" + from + "->" + to_user + "=" + body);
		if (type.equals("CHAT") || type.equals("SOURCE") || type.equals("NOTIFY")) { // 聊天类型消息
			new Thread() {
				public void run() {
					String[] ret = addChatLogs(to_user, from, type, body, strNow);
					String rowid = ret[0];
					String destUser = ret[1];
					String msgToSend = rowid + (char) 0x02 + type + (char) 0x02 + strNow + (char) 0x02 + body;
					if (destUser.indexOf("@") > 0) { // WEB登录用户
						java.util.ArrayList<String> queue = null;
						if (msgQueue.containsKey(destUser)) {
							queue = msgQueue.get(destUser);
						} else {
							queue = new java.util.ArrayList<String>();
							msgQueue.put(destUser, queue);
						}
						queue.add(msgToSend);
					} else {
						MqttManagerV3.getInstance().sendWithThread(msgToSend, destUser);
					}

				}
			}.start();
		} else if (type.equals("BROADCAST")) {
			broadcastQueue.add(body);
		} else if (type.equals("REPLY")) {
			log.info("GET A REPLY===>" + from + "->" + to_user + "=" + body);
			new Thread() {
				public void run() {
					Connection connection = HBaseUtils.getConnection();
					Table traceTable = null;
					try {
						traceTable = connection.getTable(TableName.valueOf("chat_his"));
						String rowid = body;
						Put put = new Put(Bytes.toBytes(rowid));
						put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("read"), Bytes.toBytes("1"));
						traceTable.put(put);
					} catch (Exception ex) {
						log.error("processGpsMessage_thread:" + ex.toString());
					} finally {
						try {
							if (traceTable != null)
								traceTable.close();
						} catch (Exception ex) {
						}
						HBaseUtils.closeConnection2(connection);
					}
				}
			}.start();
		} else if (type.equals("PUB")) {
			new Thread() {
				public void run() {
					Connection connection = HBaseUtils.getConnection();
					Table traceTable = null;
					try {
						traceTable = connection.getTable(TableName.valueOf("custom_msg"));
						String[] contentItems = body.split((char) 0x03 + "");
						String rowid = contentItems[2].split("\\|")[0];

						log.info("PUBLISHE MESSAGE:" + body);
						Put put = new Put(Bytes.toBytes(rowid));
						put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("zt"), Bytes.toBytes("1"));
						put.addColumn(Bytes.toBytes("data"), Bytes.toBytes("pub_time"), Bytes.toBytes(strNow));
						traceTable.put(put);

						java.util.Calendar c = java.util.Calendar.getInstance();
						c.add(java.util.Calendar.DAY_OF_MONTH, -15);
						Scan scan = new Scan();
						FilterListBuilder flistBuilder = new FilterListBuilder();
						flistBuilder.add("data:zt", "=", "1");
						flistBuilder.add("data:pub_time", ">=", org.mbc.util.Tools.formatDate("yyyy-MM-dd", c.getTime()));
						System.out.println("\tFILTER::" + flistBuilder.getFilterStr());
						scan.setFilter(flistBuilder.getFilterList());
						ResultScanner scanner = traceTable.getScanner(scan);
						java.util.ArrayList<org.json.JSONObject> rows = new java.util.ArrayList<org.json.JSONObject>();
						for (Result scannerResult : scanner) {
							String tRowId = new String(scannerResult.getRow(), "utf-8");
							if (tRowId != null) {
								org.json.JSONObject item = new org.json.JSONObject();
								item.put("rowid", rowid);
								Cell[] cells = scannerResult.rawCells();
								for (Cell cell : cells) {
									String qual = new String(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength(), "utf-8");
									String faml = new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength(), "utf-8");
									String val = new String(scannerResult.getValue(Bytes.toBytes(faml), Bytes.toBytes(qual)), "utf-8");
									item.put(faml + ":" + qual, val);
								}
								rows.add(item);
							}
						}
						scanner.close();

						// log.info("ROWS:"+rows);
						AppUtil.sortJsonList("data:pub_time", false, rows);
						com.xframe.templates.ArticlesJSP jsp = new com.xframe.templates.ArticlesJSP();
						String str = jsp.generate(rows);
						org.mbc.util.Tools.writeTextFile(CoreServlet.ARTICLES_JSP, str, "utf-8");
						MqttManagerV3.getInstance().sendWithThread("-" + (char) 0x02 + type + (char) 0x02 + strNow + (char) 0x02 + body, to_user);
					} catch (Exception ex) {
						log.error("processLocalPubMessage:" + ex.toString());
						ex.printStackTrace();
					} finally {
						try {
							if (traceTable != null)
								traceTable.close();
						} catch (Exception ex) {
						}
						HBaseUtils.closeConnection2(connection);
					}
				}
			}.start();
		} else if (type.equals("STATUS")) {
			if (body.equals("online")) {
				AppHelper.updateOnlineUser(from, body);
			} else {
				AppHelper.removeOnlineUser(from);
			}
		} else if (type.equals("LOGIN")) {
			log.info("###LOGIN::" + from);
			new Thread() {
				public void run() {
					String strContent = "{}";
					try {
						strContent = AppHelper.loadContacts(from).toString();
						log.info("\t contacts==" + strContent);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					MqttManagerV3.getInstance().sendWithThread("-" + (char) 0x02 + type + (char) 0x02 + strNow + (char) 0x02 + strContent, MqttManagerV3.TOPIC_PREFIX + from);
				}
			}.start();
		}
	}

	private String[] addChatLogs(String to_user, String from, String type, String body, String strNow) {
		String rowid = "";
		String destUser = "";
		HConnection connection = HBaseUtils.getHConnection();
		HTableInterface traceTable = null;
		try {
			traceTable = connection.getTable("chat_his");
			String topicHeader = MqttManagerV3.TOPIC_PREFIX;
			org.json.JSONObject jsonFrom = Configuration.loadUserDataByClientId(connection, from);
			destUser = to_user;
			if (destUser.indexOf("@") > 0 && destUser.endsWith("@")) { // 手机用户发给客服的消息
				String deptid = destUser.substring(topicHeader.length());
				deptid = deptid.substring(0, deptid.indexOf("@"));
				String chatUser = Configuration.chooseChatUser(jsonFrom, deptid);
				log.info("\t++++++++++jsonFrom=" + jsonFrom +"-----deptid="+deptid+ ",-----user=" + chatUser);
				destUser = topicHeader + deptid + "@" + chatUser;
			}

			String strTo = destUser;
			if (strTo.startsWith(topicHeader))
				strTo = strTo.substring(topicHeader.length());
			log.info("++++++++++jsonTo=" + strTo);
			org.json.JSONObject jsonTo = Configuration.loadUserDataByClientId(connection, strTo);
			if (type.equals("CHAT")) { // 添加联系人记录
				HTableInterface contactsTable = connection.getTable("recent_contacts");
				Put put0 = new Put(Bytes.toBytes(from + "|" + strTo));
				log.info("++++++++++jsonTo=" + jsonTo);
				if (jsonTo.getString("entity:type").startsWith("DLXX")) {
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("to_name"), Bytes.toBytes(jsonTo.getString("data:username")));
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("deptid"), Bytes.toBytes(jsonTo.getString("data:deptid")));
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("deptname"), Bytes.toBytes(jsonTo.getString("data:deptname")));
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("head"), Bytes.toBytes("files/default_app_head.png"));
				} else {
					// log.info("FROM("+from+")="+jsonFrom);
					// log.info("\tTO("+strTo+")="+jsonTo);
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("to_name"), Bytes.toBytes(jsonTo.getString("data:username")));
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("deptid"), Bytes.toBytes("-"));
					put0.add(Bytes.toBytes("data"), Bytes.toBytes("deptname"), Bytes.toBytes("-"));
					if (jsonTo.has("data:head"))
						put0.add(Bytes.toBytes("data"), Bytes.toBytes("head"), Bytes.toBytes(jsonTo.getString("data:head")));
					else
						put0.add(Bytes.toBytes("data"), Bytes.toBytes("head"), Bytes.toBytes("files/default_mobile_head.png"));
				}
				put0.add(Bytes.toBytes("data"), Bytes.toBytes("to"), Bytes.toBytes(strTo));
				put0.add(Bytes.toBytes("data"), Bytes.toBytes("time"), Bytes.toBytes(strNow));
				put0.add(Bytes.toBytes("data"), Bytes.toBytes("body"), Bytes.toBytes(body));
				contactsTable.put(put0);
				contactsTable.close();
			}

			rowid = type + "-" + strNow + "-" + from + "|" + strTo + "-" + strTo + "|" + from;
			Put put = new Put(Bytes.toBytes(rowid));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("from"), Bytes.toBytes(from));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("to"), Bytes.toBytes(strTo));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("time"), Bytes.toBytes(strNow));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("type"), Bytes.toBytes(type));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("body"), Bytes.toBytes(body));
			put.add(Bytes.toBytes("data"), Bytes.toBytes("read"), Bytes.toBytes("0"));
			traceTable.put(put);

			// MqttManagerV3.getInstance().sendWithThread(rowid+(char)0x02+body,
			// to);
		} catch (Exception ex) {
			log.error("processLocalMessage:" + ex.toString());
			ex.printStackTrace();
		} finally {
			try {
				if (traceTable != null)
					traceTable.close();
			} catch (Exception ex) {
			}
			HBaseUtils.closeConnection(connection);
		}

		return new String[] { rowid, destUser };
	}

	private void processGpsMessage(String msg) {
		try {
			String[] items = msg.split(",");
			// log.info("=======>"+msg);
			if (items.length < 5)
				return;
			final java.util.HashMap<String, String> map = new java.util.HashMap<String, String>();
			for (int i = 0; i < items.length; i++) {
				String item = items[i];
				String name = item.substring(0, item.indexOf(":"));
				String val = item.substring(item.indexOf(":") + 1);
				map.put(name, val);
			}

			java.util.concurrent.ConcurrentHashMap<String, VehPosition> pointsMap = AppUtil.getCachedPoints();
			final String ip = map.get("Ip");
			VehPosition vp = null;
			if (pointsMap.containsKey(ip)) {
				vp = pointsMap.get(ip);
			} else {
				vp = new VehPosition();
				vp.setId(ip);
				pointsMap.put(ip, vp);
			}
			vp.setLastUpdated(System.nanoTime());
			vp.setData(map);

			new Thread() {
				@SuppressWarnings("unchecked")
				public void run() {
					String key = ip + "list";
					java.util.ArrayList<String> vehPoints = null;
					CacheService cache = CacheService.getInstance();
					if (cache.containsKey(key))
						vehPoints = (java.util.ArrayList<String>) cache.getFromCache(key);
					else {
						vehPoints = new java.util.ArrayList<String>();
						cache.putInCache(key, vehPoints);
					}
					// log.info("==key="+key+",points="+vehPoints);
					try {
						vehPoints.add(map.get("Lon") + "," + map.get("Lat") + "," + map.get("Time").split(" ")[1]);
						int SIZE = 20;

						if (vehPoints.size() > SIZE) {
							StringBuffer sb = new StringBuffer();
							for (int i = 0; i < SIZE; i++) {
								String strpoint = vehPoints.get(0);
								sb.append("|" + strpoint);
								vehPoints.remove(0);
							}

							String traceDir = sysconf.get("trace_dir");
							String destDir = traceDir + java.io.File.separator + org.mbc.util.Tools.formatDate("yyyyMMdd", new java.util.Date());
							java.io.File curDir = new java.io.File(destDir);
							if (!curDir.exists())
								curDir.mkdir();

							try {
								java.io.File destFile = new java.io.File(curDir.getAbsolutePath() + java.io.File.separator + ip);
								java.io.RandomAccessFile rf = new java.io.RandomAccessFile(destFile, "rw");
								long len = rf.length();
								// System.out.println("len=="+len);
								// String
								// str=org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss",
								// new java.util.Date());

								byte[] raw = sb.toString().getBytes();
								rf.skipBytes((int) len);
								rf.write(raw, 0, raw.length);
								rf.close();
							} catch (Exception ex) {
								log.error("异常:" + ex.getMessage());
							}

						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					/*
					 * HConnection connection=HBaseUtils.getHConnection();
					 * HTableInterface traceTable=null; try{
					 * traceTable=connection.getTable("veh_trace"); String
					 * rowid=map.get("Ip")+"-"+map.get("Time"); Put put=new
					 * Put(Bytes.toBytes(rowid)); for(String k:map.keySet()){
					 * put
					 * .add(Bytes.toBytes("data"),Bytes.toBytes(k),Bytes.toBytes
					 * (map.get(k))); } traceTable.put(put); }catch(Exception
					 * ex){
					 * log.error("processGpsMessage_thread:"+ex.toString());
					 * }finally{ try{ if(traceTable!=null) traceTable.close();
					 * }catch(Exception ex){ }
					 * HBaseUtils.closeConnection(connection); }
					 */
				}
			}.start();
		} catch (Exception ex) {
			log.error("processGpsMessage异常:" + ex.toString() + ",msg=" + msg);
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static void handleVehicleReg(final java.util.HashMap<String, String> params) {
		new Thread() {
			public void run() {
				Connection connection = HBaseUtils.getConnection();
				Table traceTable = null;
				try {
					traceTable = connection.getTable(TableName.valueOf("vehicles"));
					String rowid = params.get("dept_id") + "-" + params.get("plate");
					Put put = new Put(Bytes.toBytes(rowid));
					for (String k : params.keySet()) {
						put.addColumn(Bytes.toBytes("data"), Bytes.toBytes(k), Bytes.toBytes(params.get(k)));
					}
					traceTable.put(put);
				} catch (Exception ex) {
					log.error("handleVehicleReg:" + ex.toString());
				} finally {
					try {
						if (traceTable != null)
							traceTable.close();
					} catch (Exception ex) {
					}
					HBaseUtils.closeConnection2(connection);
				}
			}
		}.start();

	}

	@SuppressWarnings({ "unchecked", "unused" })
	private static void handleVehiclePos(final java.util.HashMap<String, String> params) {
		final String ip = params.get("plate");
		String key = ip + "list";
		java.util.ArrayList<String> vehPoints = null;
		CacheService cache = CacheService.getInstance();
		if (cache.containsKey(key))
			vehPoints = (java.util.ArrayList<String>) cache.getFromCache(key);
		else {
			vehPoints = new java.util.ArrayList<String>();
			cache.putInCache(key, vehPoints);
		}

		// 更新缓存位置信息begin
		java.util.concurrent.ConcurrentHashMap<String, VehPosition> pointsMap = AppUtil.getCachedPoints();
		VehPosition vp = null;
		if (pointsMap.containsKey(ip)) {
			vp = pointsMap.get(ip);
			if (vp.getData() != null && vp.getData().containsKey("time") && vp.getData().get("time").equals(params.get("time")))
				return;
		} else {
			vp = new VehPosition();
			vp.setId(ip);
			pointsMap.put(ip, vp);

			// 添加临时车辆注册信息
			/*
			 * java.util.HashMap<String, String> hm=new
			 * java.util.HashMap<String, String>(); hm.put("plate", ip);
			 * hm.put("dept_id", params.get("dept")); hm.put("terminal_id", ip);
			 * hm.put("color", "2"); handleVehicleReg(hm);
			 */
		}
		vp.setLastUpdated(System.nanoTime());
		vp.setData(params);
		// 更新缓存位置信息end

		String time = org.mbc.util.Tools.formatDate("yyyy-MM-dd HH:mm:ss", new java.util.Date());// params.get("time");
		String[] times = time.split(" ");
		String date = times[0];
		String hour = times[1].substring(0, times[1].indexOf(":"));
		String minute = times[1].substring(times[1].indexOf(":") + 1);

		StringBuffer sb1 = new StringBuffer();
		sb1.append(params.get("lon"));
		sb1.append(",");
		sb1.append(params.get("lat"));
		sb1.append(",");
		sb1.append(minute);
		sb1.append(",");
		sb1.append(params.get("vel"));
		sb1.append(",");
		sb1.append(params.get("direct"));
		int SIZE = 20;

		vehPoints.add(sb1.toString());
		if (vehPoints.size() > SIZE) {
			StringBuffer tb = new StringBuffer();

			// 更新至数据库begin
			String rowid = date + "_" + params.get("plate");
			Connection connection = null;
			Table positionTable = null;
			try {
				if (connection == null) {
					connection = HBaseUtils.getConnection();
					positionTable = connection.getTable(TableName.valueOf("vehicle_position"));
				}

				Get get = new Get(Bytes.toBytes(rowid));
				Result result = positionTable.get(get);

				byte[] faml = Bytes.toBytes("data");
				byte[] qual = Bytes.toBytes("h" + hour);
				if (!result.isEmpty()) {
					if (result.containsColumn(faml, qual)) {
						byte[] raw = result.getValue(faml, qual);
						if (raw != null)
							tb.append(new String(raw, "utf-8"));
					}
				}

				for (int i = 0; vehPoints.size() > 0 && i < SIZE; i++) {
					String strpoint = vehPoints.get(0);
					tb.append("|" + strpoint);
					vehPoints.remove(0);
				}

				Put put = new Put(Bytes.toBytes(rowid));
				put.addColumn(faml, qual, Bytes.toBytes(tb.toString()));
				positionTable.put(put);

			} catch (Exception ex) {
				ex.printStackTrace();

			} finally {
				if (positionTable != null) {
					try {
						positionTable.close();
					} catch (Exception e1) {
					}
				}

				if (connection != null) {
					HBaseUtils.closeConnection2(connection);
					connection = null;
				}
			}
			// 更新至数据库end
		}

	}

	public static void onVehicleRequest(String plateno) throws Exception {
		

		JSONArray jary = new JSONArray();
		JSONObject json = new JSONObject();
		long leng_red = redis.getListSize(plateno);
		for (int i = 0; i < leng_red-1; i++) {
			String point = redis.popFromList(plateno);
			if (point == null) {
				continue;
			}
			String[] pointarray = point.split(",");
			String plate = pointarray[0];// 车牌号
			String time = pointarray[1];
			String[] times = time.split(" ");
			String time1 = times[0];// 日
			String time2 = times[1];// 時刻
			String[] time3 = time2.split(":");
			String hour = time3[0];
			String mini = time3[1] + ":" + time3[2];
			String lat = pointarray[2];// lat
			String lon = pointarray[3];// lon
			String deptid = pointarray[4];// 机构号
			String bus_direct = pointarray[5];
			String direct = pointarray[6];
			String status = pointarray[7];
			String vid = pointarray[8];
			String vel = pointarray[9];
			StringBuffer sb1 = new StringBuffer();
			sb1.append(lon);
			sb1.append(",");
			sb1.append(lat);
			sb1.append(",");
			sb1.append(mini);
			sb1.append(",");
			sb1.append(vel);
			sb1.append(",");
			sb1.append(direct);
			try {
				if (json.has(time1 + "%" + hour + "%" + plate)) {
					String json_value = json.getString(time1 + "%" + hour + "%" + plate);
					json.put(time1 + "%" + hour + "%" + plate, json_value + "|" + sb1.toString());
				} else {
					json.put(time1 + "%" + hour + "%" + plate, sb1.toString());
				}

				if (i == leng_red - 2) {

					Iterator iterator = json.keys();
					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						String value = "";
						try {
							value = json.getString(key);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						JSONObject jsonobj = new JSONObject();
						jsonobj.put("time", key);
						jsonobj.put("info", value);
						jary.put(jsonobj);
					}
				}

			} catch (JSONException e1) {
				System.out.println("" + e1);
				e1.printStackTrace();
			}

		}

		String syplo = "";
		String syh = "";
		String sypday = "";
		String syptime = "";
		// 写入数据库
		Connection connection = null;
		Table positionTable = null;
		try {

			if (connection == null) {
				connection = HBaseUtils.getConnection();
				positionTable = connection.getTable(TableName.valueOf("vehicle_position"));
			}
			StringBuffer sbf = new StringBuffer();
			List<Put> ap = new ArrayList<Put>();
			if (jary.length() > 1) {

				for (int x = 0; x < jary.length(); x++) {
					String day_final = "";
					String plate_final = "";
					String hour_final = "";
					String info_final = "";
					String time_value = jary.getJSONObject(x).getString("time");
					info_final = jary.getJSONObject(x).getString("info");
					String[] ttt = time_value.split("%");
					day_final = ttt[0];
					hour_final = ttt[1];
					plate_final = ttt[2];
					String rowid = day_final + "_" + plate_final;
					Get get = new Get(Bytes.toBytes(rowid));
					Result result = positionTable.get(get);
					byte[] faml = Bytes.toBytes("data");
					byte[] qual = Bytes.toBytes("h" + hour_final);

					syplo = plate_final;
					sypday = day_final;
					syh = hour_final;
					syptime = time_value;

					if (!result.isEmpty()) {
						if (result.containsColumn(faml, qual)) {
							byte[] raw = result.getValue(faml, qual);
							if (raw != null)
								sbf.append(new String(raw, "utf-8"));
						}
					}
					sbf.append("|" + info_final);
					Put put = new Put(Bytes.toBytes(rowid));
					put.addColumn(faml, qual, Bytes.toBytes(sbf.toString()));
					ap.add(put);
					//System.out.println("" + syplo + "      " + sypday + "      " + syh + "     " + syptime);
				}
			} else {
				if (jary.length() == 1) {

					String day_final = "";
					String plate_final = "";
					String hour_final = "";
					String info_final = "";
					String time_value = jary.getJSONObject(0).getString("time");
					info_final = jary.getJSONObject(0).getString("info");
					String[] ttt = time_value.split("%");
					day_final = ttt[0];
					hour_final = ttt[1];
					plate_final = ttt[2];
					String rowid = day_final + "_" + plate_final;
					Get get = new Get(Bytes.toBytes(rowid));
					Result result = positionTable.get(get);
					syplo = plate_final;
					sypday = day_final;
					syh = hour_final;
					syptime = time_value;
					byte[] faml = Bytes.toBytes("data");
					byte[] qual = Bytes.toBytes("h" + hour_final);
					if (!result.isEmpty()) {
						if (result.containsColumn(faml, qual)) {
							byte[] raw = result.getValue(faml, qual);
							if (raw != null)
								sbf.append(new String(raw, "utf-8"));
						}
					}
					sbf.append("|" + info_final);
					Put put = new Put(Bytes.toBytes(rowid));
					put.addColumn(faml, qual, Bytes.toBytes(sbf.toString()));
					ap.add(put);
					//System.out.println("" + syplo + "      " + sypday + "      " + syh + "     " + syptime);

				}
			}
			positionTable.put(ap);
		} catch (Exception e) {
			System.out.println("" + e);
		} finally {
			if (connection != null) {
				HBaseUtils.closeConnection2(connection);
				connection = null;
			}
		}
	}

	@Override
	public void onConnectionEvent(String evtName) {
		// TODO Auto-generated method stub
		log.info("ConnectionEvent:" + evtName);
	}

}
