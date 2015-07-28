package com.pslcl.qa.runner.resource;

import java.util.Map;


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
     * Get the map of attributes supported by the resource provider.
     * @return The map of attributes.
     */
    public Map<String, String> getAttributes();
	
	/**
	 * Get the description of this resource.
	 * @return the description of the resource.
	 */
	String getDescription();
	
	/**
     * Get the ResourceProvider that bound the resource.
     * @return the ResourceProvider
     */
    public ResourceProvider getResourceProvider();
    
	/**
	 * A reference matching this ResourceInstance with a specific {@link ResourceWithAttributes} request. 
	 * 
	 * @return The resource reference.
	 * 
	 * @see ResourceWithAttributes#getReference()
	 */
	int getReference();
	
}
