package com.hyb.utils;
import org.apache.log4j.*;
import java.sql.*;
public class DBHandler {
	private static Logger log=Logger.getLogger(DBHandler.class);
	public static Connection getConnection(String jdbcUrl){
		Connection con=null;
		try{
			javax.naming.Context ctx=new javax.naming.InitialContext();
			javax.sql.DataSource ds=(javax.sql.DataSource)ctx.lookup(jdbcUrl);
			//javax.sql.DataSource ds=(javax.sql.DataSource)Configuration.appContext.getBean(jdbcUrl);
			con=ds.getConnection();
		}catch(Exception ex){
			log.error(ex.toString());
			return null;
		}
		
		return con;
	}
	
	public static void closeConnection(Connection con){
		if(con==null)
			return;
		try{
			con.close();
		}catch(Exception ex){
			
		}
	}
	
	public static void closeStatement(Statement stmt){
		if(stmt==null)
			return;
		try{
			stmt.close();
		}catch(Exception ex){
			
		}
	}
	
	public static void closeStatement(PreparedStatement stmt){
		if(stmt==null)
			return;
		try{
			stmt.close();
		}catch(Exception ex){
			
		}
	}
	
	public static void closeResultSet(ResultSet rs){
		if(rs==null)
			return;
		try{
			rs.close();
		}catch(Exception ex){
		}
	}
}

