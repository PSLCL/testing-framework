package com.pslcl.qa.runner.resource;


/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider
 * is to instantiate resources. However, all interactions between the platform and the resource is brokered through
 * the provider.
 */
public interface ResourceProvider {

    /** Set a resource description.
     *
     * @note resourceHash is used in the other api calls. If this method is called prior, an associated description is then available.
     * @param resourceHash
     * @param resourceDescription
     */
    public void setResource( String resourceHash, String resourceDescription);

    /** 
     * Acquire a resource.
     * 
     * The resource must be released once it is no longer needed.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Resource object which represents the Resource Instance.
     */
    public ResourceInstance bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
    
    /**
     * Release a resource.
     * 
     * @param resource The resource to release.
     */
    public void release(ResourceInstance resource);
    
    /**
     * Check whether the specified resource is available.
     * 
     * @param resourceHash The hash of the resource.
     * @param resourceAttributes The resource attributes.
     * 
     * @return True if the specified resource is available. False otherwise.
     * 
     * @throws ResourceNotFoundException
     */
    public boolean isAvailable( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}