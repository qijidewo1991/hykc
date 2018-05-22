package com.xframe.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.*;

public class ExcelTools {
	private org.apache.log4j.Logger log=Logger.getLogger(ExcelTools.class);
	private java.util.HashMap<String, CellStyle> styles=new java.util.HashMap<String, CellStyle>();
	//private java.util.HashMap<Integer, java.text.NumberFormat> fmtMap=new java.util.HashMap<Integer, java.text.NumberFormat>();
	private java.util.HashMap<Integer, String> typesMap=new java.util.HashMap<Integer, String>();
	
	
	/**
     * create a library of cell styles
     */
    @SuppressWarnings("unused")
	private static Map<String, CellStyle> createStyles(Workbook wb){
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
        DataFormat df = wb.createDataFormat();

        CellStyle style;
        Font headerFont = wb.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(headerFont);
        styles.put("header", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(headerFont);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("header_date", style);

        Font font1 = wb.createFont();
        font1.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(font1);
        styles.put("cell_b", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font1);
        styles.put("cell_b_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font1);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_b_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_g", style);

        Font font2 = wb.createFont();
        font2.setColor(IndexedColors.BLUE.getIndex());
        font2.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(font2);
        styles.put("cell_bb", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_bg", style);

        Font font3 = wb.createFont();
        font3.setFontHeightInPoints((short)14);
        font3.setColor(IndexedColors.DARK_BLUE.getIndex());
        font3.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setFont(font3);
        style.setWrapText(true);
        styles.put("cell_h", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setWrapText(true);
        styles.put("cell_normal", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setWrapText(true);
        styles.put("cell_normal_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setWrapText(true);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_normal_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_LEFT);
        style.setIndention((short)1);
        style.setWrapText(true);
        styles.put("cell_indented", style);

        style = createBorderedStyle(wb);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        styles.put("cell_blue", style);

        return styles;
    }

    private static CellStyle createBorderedStyle(Workbook wb){
        CellStyle style = wb.createCellStyle();
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        return style;
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExcelTools et=new ExcelTools();
		/*
		JSONObject jo=new JSONObject();
		JSONArray cols=new JSONArray();
		XSSFWorkbook wb=null;
		try{
			jo.put("reportTitle", "测试Excel");
			JSONArray datas=new JSONArray();
			jo.put("args", datas);
			for(int i=0;i<10;i++){
				JSONObject r=new JSONObject();
				datas.put(r);
				for(int j=0;j<3;j++){
					r.put("col"+j, i+""+j);
				}
				//r.put("col", "col"+i);
				//r.put("value", "我的测试"+i);
			}
			
			for(int i=0;i<3;i++){
				JSONObject c=new JSONObject();
				cols.put(c);
				c.put("header", "标题1");
				c.put("type", "string");
				c.put("width", String.valueOf((i+1)*60));
				c.put("align", "left");
				c.put("name", "col"+i);
			}
			
			wb=et.createWorkbook(jo,cols);

			Map<String, CellStyle> styles=createStyles(wb);
			Row rtmp=wb.getSheetAt(0).getRow(5);
			Cell ctmp=rtmp.getCell(3);
			//CellStyle cstyle=ctmp.getCellStyle();
			CellStyle cstyle = wb.createCellStyle();
			XSSFFont ft=wb.createFont();
			ft.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
			ft.setFontHeightInPoints((short)20);
			cstyle.setFont(ft);
			
			CellStyle cs=createBorderedStyle(wb);
			cs.setFont(ft);
			ctmp.setCellStyle(cs);
			
			
			
			FileOutputStream fileOut = new FileOutputStream("d:\\temp\\poitest\\datas.xlsx");
			wb.write(fileOut);
			fileOut.close();
			
			java.io.FileInputStream fin=new java.io.FileInputStream("d:\\temp\\poitest\\hb1.xlsx");
			XSSFWorkbook wb1=new XSSFWorkbook(fin);
			fin.close();
			
			String sb=et.parseWorkbook(wb1,1500);
			org.mbc.util.Tools.writeTextFile("d:\\temp\\poitest\\datas.html", sb, "gbk");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		*/
		
		//*
		java.util.HashMap<String, Object> params=new java.util.HashMap<String, Object>();
		params.put("date", "2013年5月1日");
		params.put("time", "11:00:01");
		params.put("zdr", "冉军超");
		params.put("c0", "hello,c0,测试0!");
		params.put("c1", "hello,c1");
		params.put("c1-1", "hello,c1-1");
		params.put("title", "test c2,欢迎使用!");
		
		
		java.util.ArrayList<java.util.ArrayList<String>> al=new java.util.ArrayList<java.util.ArrayList<String>>();
		for(int i=0;i<10;i++){
			java.util.ArrayList<String> t=new java.util.ArrayList<String>();
			for(int j=0;j<14;j++){
				if(j==5)
					t.add(String.valueOf((i+1)*2.54));
				else
					t.add(String.valueOf(i));
			}
			al.add(t);
		}
		params.put("table1", al);
		try{
			XSSFWorkbook wb=et.createSimpleDocument("F:\\apache-tomcat-6.0.36_touch\\webapps\\shopwork\\WEB-INF\\xls\\purchase_temp.xml",params,0);	
			
			//org.mbc.util.Tools.writeTextFile("def.html", et.convertToHtml(wb),"gbk");
			FileOutputStream fileOut = new FileOutputStream("d:\\temp\\rjca1.xlsx");
			wb.write(fileOut);
			fileOut.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}//*/
	}
	
	@SuppressWarnings("unchecked")
	public XSSFWorkbook createWorkbook(JSONObject jo,JSONArray cols) throws Exception{
		XSSFWorkbook wb=new XSSFWorkbook();
		String repportTitle=!jo.has("reportTitle")?"无标题":jo.getString("reportTitle");
		java.util.Iterator<String> it=jo.keys();
		JSONArray datas=null;
		while(it.hasNext()){
			String k=it.next();
			if(jo.get(k) instanceof JSONArray){
				datas=jo.getJSONArray(k);
				break;
			}
		}
		
		int offsetx=1;
		int offsety=1;
		
		Sheet sheet=wb.createSheet();
		sheet.setDisplayGridlines(false);
		
		CellStyle style;
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short)18);
        titleFont.setFontName("黑体");
        style = wb.createCellStyle();
        style.setFont(titleFont);
        style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        style.setVerticalAlignment(XSSFCellStyle.ALIGN_CENTER);
        
        
        Row title=sheet.createRow(offsety+0);
        Row header=sheet.createRow(offsety+1);
        //DataFormat format = wb.createDataFormat();
        
        for(int i=0;i<cols.length();i++){
        	JSONObject def=cols.getJSONObject(i);
        	String name=def.getString("name");
        	String align=def.getString("align");
        	
        	CellStyle headerStyle=wb.createCellStyle();
        	Font headerFont = wb.createFont();
        	headerFont.setFontHeightInPoints((short)11);
        	headerFont.setFontName("宋体");
        	headerStyle.setFont(headerFont);
        	headerStyle.setAlignment(align.equals("left")?CellStyle.ALIGN_LEFT:(align.equals("center")?CellStyle.ALIGN_CENTER:CellStyle.ALIGN_RIGHT));
        	
        	headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
        	headerStyle.setBorderTop(CellStyle.BORDER_THIN);
        	headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
        	headerStyle.setBorderRight(CellStyle.BORDER_THIN);
        	
        	styles.put(name, headerStyle);
        	
        	Cell headerCell=header.createCell(offsetx+i);
        	headerCell.setCellValue(def.getString("header"));
        	headerCell.setCellStyle(headerStyle);
        	sheet.setColumnWidth(offsetx+i, def.getInt("width")*30);
        	
        	
        	
        }
        
		
		
		Cell titleCell=title.createCell(offsetx+0);
		titleCell.setCellValue(repportTitle);
		sheet.addMergedRegion(new CellRangeAddress(offsety+0,offsetx+0,offsety+0,offsety+cols.length()));
		titleCell.setCellStyle(style);
		int cidx=0;
		for(int i=0;i<datas.length();i++){
			JSONObject o=datas.getJSONObject(i);
			Row _row=sheet.createRow(offsety+i+2);
        	
			it=o.keys();
			
			for(int j=0;j<cols.length();j++){
				JSONObject def=cols.getJSONObject(j);
	        	String name=def.getString("name");
	        	String v="";
	        	//System.out.println("name="+name+",align==="+align+",alignment="+headerStyle.getAlignment());
				if(name.equals("autoid"))
					v=String.valueOf(++cidx);
				else
					v=o.getString(name);
				Cell cell=_row.createCell(offsetx+j);
				//cell.setCellValue(v);
				//cell.setCellType(Cell.CELL_TYPE_STRING);
				cell.setCellStyle(styles.get(name));
				setCellValue(cell,v,styles.get(name));
				// CellFormat cf = CellFormat.getInstance(
	            //         style.getDataFormatString());
	            // CellFormatResult result = cf.apply(cell);
			}
		}
		
		return wb;
	}
	
	public XSSFWorkbook createSimpleDocument(String templateFile,java.util.HashMap<String, Object> params,int sheetIndex) throws Exception{
		return createSimpleDocument(templateFile,params,sheetIndex,null);
	}
	
	@SuppressWarnings("unchecked")
	public XSSFWorkbook createSimpleDocument(String templateFile,java.util.HashMap<String, Object> params,int sheetIndex,java.util.HashMap<String,java.util.HashMap<String, String>> customStyles) throws Exception{
		org.dom4j.Document doc=org.dom4j.DocumentHelper.parseText(org.mbc.util.Tools.loadTextFile(templateFile, "gbk"));
		org.dom4j.Element root=doc.getRootElement();
		String excelT=root.attributeValue("excel");
		java.io.File file=new java.io.File(templateFile);
		java.io.FileInputStream fin=new java.io.FileInputStream(file.getParent()+File.separator+excelT);
		XSSFWorkbook wb=new XSSFWorkbook(fin);
		fin.close();
		
		Sheet sheet = wb.getSheetAt(sheetIndex);
		//System.out.println("rows fist=="+sheet.getFirstRowNum()+",last="+sheet.getLastRowNum());
		
		
		java.util.List<org.dom4j.Element> typeslist=root.selectNodes("//types/column");
		for(int i=0;i<typeslist.size();i++){
			org.dom4j.Element e=typeslist.get(i);
			String type=e.attributeValue("type");
			int idx=Integer.parseInt(e.attributeValue("index"));
			typesMap.put(idx, type);
		}
		
		java.util.List<org.dom4j.Element> list=root.selectNodes("//variables/cell");
		for(int i=0;i<list.size();i++){
			org.dom4j.Element e=list.get(i);
			String name=e.attributeValue("name");
			int col=Integer.parseInt(e.attributeValue("col"));
			int row=Integer.parseInt(e.attributeValue("row"));
			int colspan=Integer.parseInt(e.attribute("colspan")==null?"1":e.attributeValue("colspan"));
			int rowspan=Integer.parseInt(e.attribute("rowspan")==null?"1":e.attributeValue("rowspan"));
			
			if(params.containsKey(name)){
				if(row>sheet.getLastRowNum() || row<sheet.getFirstRowNum())
					sheet.createRow(row);
					//throw new Exception("变量的行超出模板中的行数(name="+name+",row="+row+",col="+col+")!");
				//System.out.println("cell fist=="+sheet.getRow(row).getFirstCellNum()+",last="+sheet.getRow(row).getLastCellNum());
				//if(col>sheet.getRow(row).getLastCellNum() || col<sheet.getRow(row).getFirstCellNum())
				//	throw new Exception("变量的列超出模板中的列数(name="+name+",row="+row+",col="+col+")!");
				try{
					if(sheet.getRow(row)==null)
						sheet.createRow(row);
					if(sheet.getRow(row).getCell(col)==null)
						sheet.getRow(row).createCell(col);
					sheet.getRow(row).getCell(col).setCellValue(params.get(name).toString());
					if(rowspan>1 || colspan>1)
						sheet.addMergedRegion(new CellRangeAddress(row, row+(rowspan-1),col, col+(colspan-1)));
				}catch(Exception ex){
					ex.printStackTrace();
				}
			}
		}
		
		CellStyle style;
        Font titleFont = wb.createFont();
        titleFont.setFontHeightInPoints((short)14);
        titleFont.setFontName("Trebuchet MS");
        style = wb.createCellStyle();
        //style.setFont(titleFont);
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        
		
		list=root.selectNodes("//tables/table");
		Row tRow=null;
		
		for(int i=0;i<list.size();i++){
			org.dom4j.Element t=list.get(i);
			int col=Integer.parseInt(t.attributeValue("col"));
			int row=Integer.parseInt(t.attributeValue("row"));
			int colscount=Integer.parseInt(t.attribute("columnCount")==null?"0":t.attributeValue("columnCount"));
			String name=t.attributeValue("name");
			//System.out.println("name="+name);
			java.util.HashMap<String,CellStyle> styleMap=new java.util.HashMap<String, CellStyle>();
			if(tRow==null){
				tRow=sheet.getRow(row);
				for(int j=0;j<colscount;j++){
					Cell tCell=tRow.getCell(col+j);
					styleMap.put(String.valueOf(col+j), tCell.getCellStyle());
				}
				
			}
			if(params.containsKey(name) && (params.get(name) instanceof java.util.ArrayList<?>)){
				//System.out.println("params=="+params);
				java.util.ArrayList<java.util.ArrayList<String>> al=(java.util.ArrayList<java.util.ArrayList<String>>)params.get(name);
				if(al.size()>1)
					sheet.shiftRows(row, sheet.getLastRowNum(), al.size()-1,true,false);
				//System.out.println("1111111111111");
				for(int x=0;x<al.size();x++){
					Row _row=sheet.createRow(row+x);
					java.util.ArrayList<String> rv=al.get(x);
					//System.out.println("rv["+x+"]=="+rv);
					for(int j=0;j<colscount;j++){
						Cell cell=_row.createCell(col+j);
						if(styleMap.get(String.valueOf(col+j))!=null){
							//customStyles;
							CellStyle stylex=styleMap.get(String.valueOf(col+j));
							
							if(customStyles!=null && customStyles.containsKey(x+","+j)){
								short align=stylex.getAlignment();
								short valign=stylex.getVerticalAlignment();
								java.util.HashMap<String, String> mstyles=customStyles.get(x+","+j);
								//System.out.println("key=="+x+","+j+",msytles="+mstyles+",value=="+String.valueOf(rv.get(j)));
								Font fo= cell.getSheet().getWorkbook().getFontAt(stylex.getFontIndex());
								
								stylex=ExcelTools.createBorderedStyle(wb);
								stylex.setAlignment(align);
								stylex.setVerticalAlignment(valign);
								Font font = wb.createFont();
							    font.setFontHeightInPoints(fo.getFontHeightInPoints());
							    font.setFontName(fo.getFontName());
								if(font!=null){
									if(mstyles.containsKey("fontWeight")){
										String fw=mstyles.get("fontWeight");
										if(fw.equals("bold"))
											font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
										else
											font.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
									}
									
									if(mstyles.containsKey("textDecoration")){
										String fw=mstyles.get("textDecoration");
										if(fw.equals("italic"))
											font.setItalic(true);
									}
									
									if(mstyles.containsKey("textAlign")){
										String fw=mstyles.get("textAlign");
										if(fw.equals("left"))
											stylex.setAlignment(CellStyle.ALIGN_LEFT);
										else if(fw.equals("center"))
											stylex.setAlignment(CellStyle.ALIGN_CENTER);
										else if(fw.equals("right"))
											stylex.setAlignment(CellStyle.ALIGN_RIGHT);
									}
										
								}
								stylex.setFont(font);
							}
							cell.setCellStyle(stylex);
						}
						if(j<rv.size()){
							//setCellValue(cell,String.valueOf(rv.get(j)),styleMap.containsKey(String.valueOf(col+j))?styleMap.get(String.valueOf(col+j)):null);
							String v=String.valueOf(rv.get(j));
							if(typesMap.containsKey(j)){
								//System.out.println("type==="+typesMap.get(j)+",v=="+v);
								if(typesMap.get(j).equals("number")){
									try{
										cell.setCellValue(Double.parseDouble(v));
									}catch(Exception ex){
										cell.setCellValue(v);
									}
								}else if(typesMap.get(j).equals("int")){
									try{
										cell.setCellValue(Integer.parseInt(v));
									}catch(Exception ex){
										cell.setCellValue(v);
									}
								}else if(typesMap.get(j).equals("currency")){
									try{
										double d=Double.parseDouble(v);
										cell.setCellValue(d);
									}catch(Exception ex){
										ex.printStackTrace();
										cell.setCellValue(v);
									}
								}
							}else
								cell.setCellValue(v);
						}
							
					}
				}
				
			}
		}
		
		
		
		//System.out.println("size=="+list.size()+",sheet size=");
		return wb;
	}
	
	private static int ultimateCellType(Cell c) {
        int type = c.getCellType();
        if (type == Cell.CELL_TYPE_FORMULA)
            type = c.getCachedFormulaResultType();
        return type;
    }
	
	private String getCellStyle(Cell c){
		try{
		StringBuffer sb=new StringBuffer();
		CellStyle cstyle = c.getCellStyle();
		
		//font begin
		Font font = c.getSheet().getWorkbook().getFontAt(cstyle.getFontIndex());
		if (font.getBoldweight() > HSSFFont.BOLDWEIGHT_NORMAL)
            sb.append("font-weight: bold;");
        if (font.getItalic())
        	sb.append("font-style: italic;");
        
        sb.append("font-family: "+font.getFontName()+";");

        int fontheight = font.getFontHeightInPoints();
        if (fontheight == 9) {
            //fix for stupid ol Windows
            fontheight = 10;
        }
        sb.append("font-size: "+fontheight+"pt;");
        //font end
        
        
        //align begin
        if (cstyle.getAlignment() == CellStyle.ALIGN_CENTER) {
           sb.append("text-align:center;");
        }else  if (cstyle.getAlignment() == CellStyle.ALIGN_LEFT) {
            sb.append("text-align:left;");
        }else  if (cstyle.getAlignment() == CellStyle.ALIGN_RIGHT) {
            sb.append("text-align:right;");
        }
        
        if(cstyle.getVerticalAlignment()==CellStyle.VERTICAL_TOP){
        	sb.append("vertical-align:top;");
        }else if(cstyle.getVerticalAlignment()==CellStyle.VERTICAL_CENTER){
        	sb.append("vertical-align:middle;");
        }else if(cstyle.getVerticalAlignment()==CellStyle.VERTICAL_BOTTOM){
        	sb.append("vertical-align:bottom;");
        }
        //align end
        
		
        //borders begin
        sb.append("border-left: solid "+cstyle.getBorderLeft()+"px;");
        sb.append("border-right: solid "+cstyle.getBorderRight()+"px;");
        sb.append("border-bottom: solid "+cstyle.getBorderBottom()+"px;");
        sb.append("border-top: solid "+cstyle.getBorderTop()+"px;");
        //borders end;
        
		return sb.toString();
		}catch(Exception ex){
			log.error("cell("+c.getRowIndex()+","+c.getColumnIndex()+")"+ex.toString());
			ex.printStackTrace();
			return "";
		}
	}
	
	private void setCellValue(Cell c,String v,CellStyle style){
		System.out.println("celltype=="+ultimateCellType(c)+",value="+v);
		try{
		//int a=	HSSFCell.CELL_TYPE_BLANK;
		switch (ultimateCellType(c)) {
		case HSSFCell.CELL_TYPE_BLANK:
        case HSSFCell.CELL_TYPE_STRING:
            c.setCellValue(v);
            break;
        case HSSFCell.CELL_TYPE_BOOLEAN:
        	c.setCellValue(Boolean.parseBoolean(v));
        	break;
        case HSSFCell.CELL_TYPE_ERROR:
        case HSSFCell.CELL_TYPE_NUMERIC:
       	 	c.setCellValue(Double.parseDouble(v));
       	 	break;
        default:
           break;
        }
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private String getCellValue(Cell c){
		 switch (ultimateCellType(c)) {
         case HSSFCell.CELL_TYPE_STRING:
             return c.getStringCellValue();
         case HSSFCell.CELL_TYPE_BOOLEAN:
        	 return String.valueOf(c.getBooleanCellValue());
         case HSSFCell.CELL_TYPE_ERROR:
             return "";
         case HSSFCell.CELL_TYPE_NUMERIC:
         {
        	
        	 //System.out.println("format=="+c.getCellStyle().getDataFormatString());
        	 //System.out.println("\tcolumnIdx="+c.getColumnIndex()+",type=["+typesMap.get(c.getColumnIndex())+"]");
        	 if(typesMap.containsKey(c.getColumnIndex()) && typesMap.get(c.getColumnIndex()).equals("int")){
        		 java.text.NumberFormat nf=java.text.NumberFormat.getInstance();
        		 nf.setMaximumFractionDigits(0);
        		 //System.out.println("column=="+c.getColumnIndex()+",value="+Math.ceil(c.getNumericCellValue()));
        		 return nf.format(c.getNumericCellValue());
        	 }
        	 return String.valueOf(c.getNumericCellValue());
         }
         default:
             // "right" is the default
             break;
         }
		return "";
	}
	
	public String parseWorkbook(XSSFWorkbook wb){
		return parseWorkbook(wb,0);
	}
	
	public String parseWorkbook(XSSFWorkbook wb,int width){
		StringBuffer sb=new StringBuffer();
		Sheet sheet = wb.getSheetAt(0);
		Iterator<Row> iter = sheet.rowIterator();
        int firstColumn = (iter.hasNext() ? Integer.MAX_VALUE : 0);
        int endColumn = 0;
        while (iter.hasNext()) {
            Row row = iter.next();
            short firstCell = row.getFirstCellNum();
            if (firstCell >= 0) {
                firstColumn = Math.min(firstColumn, firstCell);
                endColumn = Math.max(endColumn, row.getLastCellNum());
            }
        }
        
        java.util.HashMap<String, CellRangeAddress> addrMap=new java.util.HashMap<String, CellRangeAddress>();
        
		System.out.println("first="+firstColumn+",end="+endColumn);
		String k="";
	    for(int i=0;i<sheet.getNumMergedRegions();i++){
	       	CellRangeAddress addr=sheet.getMergedRegion(i);
	       	k=addr.getFirstRow()+","+addr.getFirstColumn();
	       	//System.out.println("region::"+k+",last="+addr.getLastRow()+","+addr.getLastColumn()+",value="+sheet.getRow(addr.getFirstRow()).getCell(addr.getFirstColumn()).getStringCellValue());
	       	addrMap.put(k, addr);
	    }
	    java.util.HashMap<String, Integer> rowSpan=new java.util.HashMap<String, Integer>();	
	    CellRangeAddress addr=null;
		sb.append("<div style='padding:10px;'>"); 
	    sb.append("<table border='0' style='"+(width>0?"width:"+width+"px;":"")+"font-size:12px;' cellspacing='0' cellpadding='3' style='border-collapse:collapse;border-color: #000000;'>\n");
		 Iterator<Row> rows = sheet.rowIterator();
		 while(rows.hasNext()){
			 Row row = rows.next();
			 addr=null;
			 //System.out.println("\nrow==="+row.getRowNum()+",cells=="+row.getPhysicalNumberOfCells()+",height="+row.getHeightInPoints()+",width=");
			 if(row.getHeightInPoints()<1)continue;
			 sb.append("\t<tr>\n");
			 for(int i=row.getFirstCellNum();i<=row.getLastCellNum();i++){
				 //System.out.print(" "+i);
				 if(i>=0){
					 Cell c=row.getCell(i);
					 
					 if(c!=null){
						 String key=String.valueOf(i);
						 if(rowSpan.containsKey(key) && rowSpan.get(key)>0){
							 int v=rowSpan.get(key);
							 v--;
							 if(v==0)
								 rowSpan.remove(key);
							 else
								 rowSpan.put(key, v);
							 continue;
						 }
						 //System.out.print("#");
						 //c.setCellType(Cell.CELL_TYPE_STRING);
						 String sv=getCellValue(c);
						 if(sv==null || sv.trim().length()==0)
							 sv="&nbsp;";
						 if(addrMap.containsKey(row.getRowNum()+","+i)){
							 addr=addrMap.get(row.getRowNum()+","+i);
							 int colspan=addr.getLastColumn()-addr.getFirstColumn()+1;
							 int spans=1;
							 if(addr.getLastRow()!=addr.getFirstRow()){
								 spans=addr.getLastRow()-addr.getFirstRow()+1;
								 //System.out.print("["+spans+"]");
								 for(int j=addr.getFirstColumn();j<=addr.getLastColumn();j++){
									 rowSpan.put(String.valueOf(j), spans-1);
								 }
							 }
							 i=i+colspan-1;
							 sb.append("<td style='"+getCellStyle(c)+"' colspan='"+colspan+"' "+(spans>1?"rowspan='"+spans+"'":"")+">"+sv+"</td>");
						 }else
							 sb.append("<td style='"+getCellStyle(c)+"'>"+sv+"</td>");
					 }
				 }
			 }
			 //System.out.println((row.getRowNum())+"==="+row.getPhysicalNumberOfCells());
			 sb.append("\n\t</tr>\n");
		 }
		 
		 sb.append("</table>\n");
		 sb.append("</div>");
		return sb.toString();
	}
}
