package com.pslcl.qa.platform.resource;

/**
 * Represents a Cable Resource instance
 */
public interface CableInstance extends ResourceInstance {
	
	/**
	 * The IP address associated with this cable.
	 * 
	 * @return
	 */
	public String getIPAddress();
}
