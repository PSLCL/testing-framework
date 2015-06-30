package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceNotFoundException;

/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider
 * is to instantiate resources. However, all interactions between the platform and the resource is brokered through
 * the provider.
 *
 * The test platform supports multiple runners, each of which may use a common set of resource providers
 * To distinguish runners, a runner reference is part of this API. Implementing class may set this reference.
 */
public interface ResourceProvider {

    /**
     *
     * @param component
     * @param version
     * @param platform
     * @param name
     * @param hash
     */
    void updateArtifact( String component, String version, String platform, String name, Hash hash );

    /**
     * @param component
     * @param version
     * @param platform
     * @param name
     */
    void removeArtifact( String component, String version, String platform, String name );

    /**
     * @param component
     * @param version
     */
    void invalidateArtifacts( String component, String version );

    /** Set a resource definition.
     *
     * @param resourceHash
     * @param resourceDescription
     */
    void setResource( String resourceHash, String resourceDescription);

    /** 
     * Acquire a resource.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Resource object which represents the Resource Instance.
     */
    Resource bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
    
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
    boolean isAvailable( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;

    /** Cancel resource requests associated with this runner instance  */
    void cancel();
}