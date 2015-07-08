package com.pslcl.qa.runner.resource;

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
