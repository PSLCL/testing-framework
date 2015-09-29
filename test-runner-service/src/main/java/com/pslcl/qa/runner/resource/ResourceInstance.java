package com.pslcl.qa.runner.resource;


/**
 * Represents a Resource Instance.
 */
public interface ResourceInstance extends ResourceWithAttributes {
	
    /**
     * Get the ResourceProvider that bound the resource.
     * @return the ResourceProvider.
     */
    ResourceProvider getResourceProvider();
}