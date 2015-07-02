package com.pslcl.qa.platform.resource;


/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider
 * is to instantiate resources. However, all interactions between the platform and the resource is brokered through
 * the provider.
 *
 * The test platform supports multiple runners, each of which may use a common set of resource providers
 * To distinguish runners, a runner reference is part of this API. Implementing class may set this reference.
 */
public interface ResourceProvider {

    /** Set a resource definition.
     *
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