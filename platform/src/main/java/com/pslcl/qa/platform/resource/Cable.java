package com.pslcl.qa.platform.resource;

/**
 * Represents a Cable Resource instance
 */
public interface Cable extends Resource {
	
	/**
	 * The IP address associated with this cable.
	 * 
	 * @return
	 */
	public String getIPAddress();
}
