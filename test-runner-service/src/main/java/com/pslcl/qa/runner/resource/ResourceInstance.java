package com.pslcl.qa.runner.resource;


/**
 * Represents a Resource Instance.
 */
public interface ResourceInstance extends ResourceWithAttributes {

	/**
     * Get the description of the resource.
     * @return the description of the resource.
     */
    String getDescription();
	
    /**
     * Get the ResourceProvider that bound the resource.
     * @return the ResourceProvider.
     */
    ResourceProvider getResourceProvider();
    
    /**
     * Get the number of seconds for which this resource may be bound, measured from now.
     * @return The number of seconds.
     */
    int getTimeoutSeconds();

}