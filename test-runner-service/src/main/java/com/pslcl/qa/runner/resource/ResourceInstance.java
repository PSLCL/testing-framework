package com.pslcl.qa.runner.resource;


/**
 * Represents a Resource Instance.
 */
public interface ResourceInstance {

	/**
	 * Get the hash of this resource.
	 * 
	 * @return A hex string representing the hash of the resource.
	 */
	String getHash();
	
	/**
	 * Get the description of this resource.
	 * @return the description of the resource.
	 */
	String getDescription();
	
	/**
	 * A reference matching this ResourceInstance with a specific {@link ResourceWithAttributes} request. 
	 * 
	 * @return The resource reference.
	 * 
	 * @see ResourceWithAttributes#getReference()
	 */
	int getReference();
	
}
