package com.xframe.data;

import java.io.BufferedInputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import com.xframe.utils.FilterListBuilder;
import com.xframe.utils.HBaseUtils;

public class TransTools {
	private static org.apache.log4j.Logger log=org.apache.log4j.Logger.getLogger(TransTools.class);
	private RecordObject loadSingleRow(HConnection connection,String tableName,String rowid) throws Exception{
		RecordObject recObj=new RecordObject();
		recObj.setName(tableName);
		HTableInterface userTable=connection.getTable(recObj.getName());
		try{
			Get get=new Get(Bytes.toBytes(rowid));
			ColumnObject[] columns=null;
			Result result=userTable.get(get);
			if(!result.isEmpty()){
				recObj.setRowid(result.getRow());
				Cell[] cells=result.rawCells();
				int idx=0;
				columns=new ColumnObject[cells.length];
				for(Cell cell:cells){
					byte[] qual=new byte[cell.getQualifierLength()];
					byte[] faml=new byte[cell.getFamilyLength()];
					System.arraycopy(cell.getQualifierArray(), cell.getQualifierOffset(), qual, 0, qual.length);
					System.arraycopy(cell.getFamilyArray(), cell.getFamilyOffset(), faml, 0, faml.length);
					byte[] val=result.getValue(faml, qual);
					columns[idx++]=new ColumnObject(faml,qual,val);
				}
				recObj.setColumns(columns);
			}
		}catch(Exception ex){
			log.error("loadSingleRow:"+ex.toString());
		}finally{
			if(userTable!=null)
				userTable.close();
		}
		
		return recObj;
	}
	
	private RecordObject[] loadObjects(HConnection connection,String khh) throws Exception{
		RecordObject[] objs=null;
		HTableInterface userTable=connection.getTable("syscfg"); 
		try{
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.add("data:khbh","=",khh);
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","objects");
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = userTable.getScanner(scan);
			java.util.ArrayList<RecordObject> list=new java.util.ArrayList<RecordObject>();
			
			for (Result scannerResult:scanner) {
				RecordObject rec=new RecordObject();
				rec.setName("syscfg");
				rec.setRowid(scannerResult.getRow());
				if(rec.getRowid()!=null){
					Cell[] cells=scannerResult.rawCells();
					int idx=0;
					ColumnObject[] columns=new ColumnObject[cells.length];
					for(Cell cell:cells){
						byte[] qual=new byte[cell.getQualifierLength()];
						byte[] faml=new byte[cell.getFamilyLength()];
						System.arraycopy(cell.getQualifierArray(), cell.getQualifierOffset(), qual, 0, qual.length);
						System.arraycopy(cell.getFamilyArray(), cell.getFamilyOffset(), faml, 0, faml.length);
						byte[] val=scannerResult.getValue(faml, qual);
						columns[idx++]=new ColumnObject(faml,qual,val);
					}
					rec.setColumns(columns);
					list.add(rec);
				}
			}
			objs=list.toArray(new RecordObject[list.size()]);
		}catch(Exception ex){
			log.error("loadObjects:"+ex.toString());
			ex.printStackTrace();
		}finally{
			if(userTable!=null)
				userTable.close();
		}
		return objs;
	}
	
	private RecordObject[] loadModules(HConnection connection) throws Exception{
		RecordObject[] objs=null;
		HTableInterface userTable=connection.getTable("syscfg"); 
		try{
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","modules");
			long total=HBaseUtils.getRowCount("syscfg",flistBuilder.getFilterStr()); 
			log.info("total modules:"+total);
			scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = userTable.getScanner(scan);
			java.util.ArrayList<RecordObject> list=new java.util.ArrayList<RecordObject>();
			
			for (Result scannerResult:scanner) {
				RecordObject rec=new RecordObject();
				rec.setName("syscfg");
				rec.setRowid(scannerResult.getRow());
				int idx=0;
				if(rec.getRowid()!=null){
					Cell[] cells=scannerResult.rawCells();
					ColumnObject[] columns=new ColumnObject[cells.length];
					for(Cell cell:cells){
						byte[] qual=new byte[cell.getQualifierLength()];
						byte[] faml=new byte[cell.getFamilyLength()];
						System.arraycopy(cell.getQualifierArray(), cell.getQualifierOffset(), qual, 0, qual.length);
						System.arraycopy(cell.getFamilyArray(), cell.getFamilyOffset(), faml, 0, faml.length);
						byte[] val=scannerResult.getValue(faml, qual);
						columns[idx++]=new ColumnObject(faml,qual,val);
					}
					rec.setColumns(columns);
					list.add(rec);
				}
			}
			objs=list.toArray(new RecordObject[list.size()]);
			log.info("modules::"+objs);
		}catch(Exception ex){
			log.error("loadModules:"+ex.toString());
			ex.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(userTable!=null)
				userTable.close();
		}
		return objs;
	}
	
	private String createOutFile(String dir,String khh,byte[] data){
		String path=dir+java.io.File.separator+khh+".dpp";
		try{
			java.io.FileOutputStream fileOut=new java.io.FileOutputStream(new java.io.File(path));
			ZipOutputStream out = new ZipOutputStream(fileOut);
			//khxx
			out.putNextEntry(new ZipEntry(khh));
			out.write(data);
			out.closeEntry();
			out.close();
			fileOut.flush();
			fileOut.close();
		}catch(Exception ex){
			log.error("createOutFile:"+ex.toString());
		}
		return path;
	}
	
	public String exportModules(String dir){
		return exportKhxx(dir,"1001001");
	}
	
	public String exportKhxx(String dir,String khh){
		HConnection connection=HBaseUtils.getHConnection();
		java.io.ByteArrayOutputStream bout=new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream oo=null;
		try{
			oo=new java.io.ObjectOutputStream(bout);
			RecordObject khxx=loadSingleRow(connection,"syscfg","khxx"+khh);
			RecordObject adminUser=loadSingleRow(connection,"syscfg","users"+khh+"-"+(khh.equals("1001001")?"admin":"1001"));
			RecordObject[] objs=loadObjects(connection,khh);
			ClientData client=new ClientData();
			client.setKhh(khh);
			client.setKhxx(khxx);
			client.setUserObject(adminUser);
			if(khh.equals("1001001")){
				client.setRecords(loadModules(connection));
			}else{
				client.setRecords(objs);
			}
			oo.writeObject(client);
			byte[] raw=bout.toByteArray();
			return createOutFile(dir,khh,raw);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(oo!=null){
				try{
					oo.close();
				}catch(Exception ex){	
				}
				
			}
			HBaseUtils.closeConnection(connection);
		}
		return "";
	}

	public boolean importKhxx(String file){
		try{
			java.util.zip.ZipInputStream zin=new java.util.zip.ZipInputStream(new java.io.FileInputStream(file));
			ZipEntry entry=zin.getNextEntry();
			if(entry==null)
				return false;
			BufferedInputStream bin=new BufferedInputStream(zin);  
			java.io.ObjectInputStream oin=new java.io.ObjectInputStream(bin);
			ClientData data=(ClientData)oin.readObject();
			oin.close();
			bin.close();
			zin.close();
			importClientData(data);
		}catch(Exception ex){
			log.error("importKhxx:("+file+")"+ex.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void importClientData(ClientData data) throws Exception{
		RecordObject khxx=data.getKhxx();
		RecordObject user=data.getUserObject();
		RecordObject[] objs=data.getRecords();
		if(khxx==null && !data.getKhh().equals("1001001"))
			throw new Exception("客户信息主记录无效");
		
		dump(data.getKhxx());
		System.out.println("USERS");
		dump(data.getUserObject());
		System.out.println("RECORDS:"+data.getRecords());
		if(data.getRecords()!=null){
			for(int i=0;i<data.getRecords().length;i++){
				dump(data.getRecords()[i]);
			}
		}
		
		
		System.out.println("BEGIN iMPORT==============");
		HConnection connection=HBaseUtils.getHConnection();
		try{
			boolean bRet=importRow(connection,khxx);
			
			bRet=importRow(connection,user);
			bRet=importObjs(connection,objs);
			log.info("导入结果:"+bRet);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			HBaseUtils.closeConnection(connection);
		}
		
	}
	
	private boolean importObjs(HConnection connection,RecordObject[] records){
		if(records==null)
			return false;
		boolean bRet=true;
		for(RecordObject rec:records){
			bRet &=importRow(connection,rec);
		}
		return bRet;
	}
	
	private boolean importRow(HConnection connection,RecordObject record){
		if(record==null)
			return false;
		String name=record.getName();
		HTableInterface userTable=null;
		try{
			userTable=connection.getTable(name);
			Put put=new Put(record.getRowid());
			if(record.getColumns()==null)
				return false;
			for(ColumnObject col:record.getColumns()){
				put.add(col.getFamily(), col.getQualifier(), col.getData());
			}
			userTable.put(put);
		}catch(Exception ex){
			log.error("importRow:["+name+":"+"]=>"+ex.toString());
		}finally{
			try{
				userTable.close();
			}catch(Exception ex){
			}
		}
		return true;
	}
	
	public static void main(String[] args){
		org.apache.log4j.BasicConfigurator.configure();
		try{
			java.util.zip.ZipInputStream zin=new java.util.zip.ZipInputStream(new java.io.FileInputStream("d:\\temp\\4101006.dpp"));
			ZipEntry entry=zin.getNextEntry();
			if(entry==null)
				return;
			
			log.info("("+entry.getName()+")size=="+entry.getSize()+",extra="+entry.getExtra());
			BufferedInputStream bin=new BufferedInputStream(zin);  
			java.io.ObjectInputStream oin=new java.io.ObjectInputStream(bin);
			ClientData data=(ClientData)oin.readObject();
			System.out.println("data="+data.getKhh()+",=="+data.getKhxx()+",rec="+data.getRecords());
			System.out.println("KHXX");
			dump(data.getKhxx());
			System.out.println("USERS");
			dump(data.getUserObject());
			System.out.println("RECORDS:"+data.getRecords());
			if(data.getRecords()!=null){
				for(int i=0;i<data.getRecords().length;i++){
					dump(data.getRecords()[i]);
				}
			}
			oin.close();
			bin.close();
			zin.close();
		}catch(Exception ex){
			log.error("importKhxx:"+ex.toString());
			ex.printStackTrace();
			return;
		}
	}
	
	private static void dump(RecordObject rec){
		if(rec==null){
			System.out.println("=====>NULL");
			return;
		}
		
		System.out.println("name="+rec.getName()+",rowid="+rec.getRowid());
		if(rec.getColumns()!=null){
			for(int i=0;i<rec.getColumns().length;i++){
				ColumnObject c=rec.getColumns()[i];
				try {
					System.out.println("\tcol="+new String(c.getFamily())+","+new String(c.getQualifier())+",data="+new String(c.getData(),"utf-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private RecordObject[] loadTableRows(HConnection connection,String tableName) throws Exception{
		RecordObject[] objs=null;
		HTableInterface userTable=connection.getTable(tableName); 
		try{
			Scan scan=new Scan();
			FilterListBuilder flistBuilder=new FilterListBuilder();
			//flistBuilder.add("data:khbh","=",khh);
			//flistBuilder.addRowFilter(FilterListBuilder.SubstrCompare,"=","objects");
			//scan.setFilter(flistBuilder.getFilterList());
			ResultScanner scanner = userTable.getScanner(scan);
			java.util.ArrayList<RecordObject> list=new java.util.ArrayList<RecordObject>();
			
			for (Result scannerResult:scanner) {
				RecordObject rec=new RecordObject();
				rec.setName(tableName);
				rec.setRowid(scannerResult.getRow());
				if(rec.getRowid()!=null){
					Cell[] cells=scannerResult.rawCells();
					int idx=0;
					ColumnObject[] columns=new ColumnObject[cells.length];
					for(Cell cell:cells){
						byte[] qual=new byte[cell.getQualifierLength()];
						byte[] faml=new byte[cell.getFamilyLength()];
						System.arraycopy(cell.getQualifierArray(), cell.getQualifierOffset(), qual, 0, qual.length);
						System.arraycopy(cell.getFamilyArray(), cell.getFamilyOffset(), faml, 0, faml.length);
						byte[] val=scannerResult.getValue(faml, qual);
						columns[idx++]=new ColumnObject(faml,qual,val);
					}
					rec.setColumns(columns);
					list.add(rec);
				}
			}
			objs=list.toArray(new RecordObject[list.size()]);
		}catch(Exception ex){
			log.error("loadTableRows:"+ex.toString());
			ex.printStackTrace();
		}finally{
			if(userTable!=null)
				userTable.close();
		}
		return objs;
	}
	
	public String exportTable(String dir,String tableName){
		HConnection connection=HBaseUtils.getHConnection();
		java.io.ByteArrayOutputStream bout=new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream oo=null;
		try{
			oo=new java.io.ObjectOutputStream(bout);
			RecordObject[] objs=loadTableRows(connection,tableName);
			log.info("表["+tableName+"]记录行数:"+objs.length);
			TableData client=new TableData();
			client.setTableName(tableName);
			client.setRecords(objs);
			oo.writeObject(client);
			byte[] raw=bout.toByteArray();
			return createOutFile(dir,tableName,raw);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(oo!=null){
				try{
					oo.close();
				}catch(Exception ex){	
				}
				
			}
			HBaseUtils.closeConnection(connection);
		}
		return "";
	}
	
	
	public boolean importTable(String file,String destTable){
		try{
			java.util.zip.ZipInputStream zin=new java.util.zip.ZipInputStream(new java.io.FileInputStream(file));
			ZipEntry entry=zin.getNextEntry();
			if(entry==null)
				return false;
			BufferedInputStream bin=new BufferedInputStream(zin);  
			java.io.ObjectInputStream oin=new java.io.ObjectInputStream(bin);
			TableData data=(TableData)oin.readObject();
			oin.close();
			bin.close();
			zin.close();
			importTableData(data,destTable);
		}catch(Exception ex){
			log.error("importKhxx:("+file+")"+ex.toString());
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void importTableData(TableData data,String destTable) throws Exception{
		RecordObject[] objs=data.getRecords();
		if(data.getRecords()!=null){
			for(int i=0;i<data.getRecords().length;i++){
				dump(data.getRecords()[i]);
			}
		}
		
		
		System.out.println("BEGIN iMPORT==============");
		HConnection connection=HBaseUtils.getHConnection();
		try{
			boolean bRet=importTableRows(data.getTableName(), connection,objs,destTable);
			log.info("导入结果:"+bRet);
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			HBaseUtils.closeConnection(connection);
		}
		
	}
	
	private boolean importTableRows(String name,HConnection connection,RecordObject[] records,String destTable){
		if(records==null)
			return false;
		boolean bRet=true;
		HTableInterface userTable=null;
		java.util.ArrayList<Put> list=new java.util.ArrayList<Put>();
		try{
			userTable=connection.getTable(destTable);
			
			for(RecordObject record:records){
				Put put=new Put(record.getRowid());
				if(record.getColumns()==null)
					continue;
				for(ColumnObject col:record.getColumns()){
					put.add(col.getFamily(), col.getQualifier(), col.getData());
				}
				list.add(put);
			}
			
			userTable.put(list);
		}catch(Exception ex){
			log.error("importTableRows:["+name+"=>"+""+destTable+"]"+ex.toString());
		}finally{
			try{
				userTable.close();
			}catch(Exception ex){
			}
		}
		
		log.info("importTableRows:["+name+"=>"+""+destTable+"],导入行数"+list.size());
		return bRet;
	}
}
