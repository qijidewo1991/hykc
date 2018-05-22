package com.xframe.core;

public class FileItem {
	private String path;
	private long lastModified;
	private String script;
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	public String getScript() {
		return script;
	}
	public void setScript(String script) {
		this.script = script;
	}
	
	public FileItem(String path,String script,long date){
		this.path=path;
		this.script=script;
		this.lastModified=date;
	}

}
