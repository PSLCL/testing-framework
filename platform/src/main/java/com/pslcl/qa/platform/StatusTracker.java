package com.pslcl.qa.platform;

import java.util.HashMap;

public class StatusTracker {
	public static final short OK = 0;
	public static final short WARN = 1;
	public static final short ERROR = 2;
	
    private final HashMap<String, Short> status = new HashMap<String, Short>(); // Stores status values for arbitrary strings. There are three status values: OK (0), WARN (1), and ERROR (2).
    private volatile short consolidatedStatus = OK;
    
    public StatusTracker()
    {
    }
    
    public void setStatus(String name, short status) {
    	synchronized (this.status) {
    		this.status.put(name, status);
    		if (status > this.consolidatedStatus) {
    			this.consolidatedStatus = status;
    		} else {
    			update();
    		}
    	}
    }

    public void removeStatus(String name) {
    	synchronized (this.status) {
    		this.status.remove(name);
    		update();
    	}
    }
    public short getStatus(String name) {
    	return this.status.get(name);
    }
    
    public short getStatus() {
    	return this.consolidatedStatus;
    }
    
	public void close() {
	}
	
	private void update() {
		short status;
		status = OK;
		for (String iter : this.status.keySet()) {
			short iterStatus = this.status.get(iter);
			if (iterStatus > status)
				status = iterStatus;
		}
		if (status != consolidatedStatus) {
			consolidatedStatus = status;
		}
	}
    
}
