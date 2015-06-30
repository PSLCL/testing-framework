package com.pslcl.qa.platform;

import com.pslcl.qa.platform.resource.MachineResource;

/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider
 * is to instantiate resources. However, all interactions between the platform and the resource is brokered through
 * the provider.
 *
 * The test platform supports multiple runners, each of which may use a common set of resource providers
 * To distinguish runners, a runner reference is part of this API. Implementing class may set this reference.
 */
public interface ResourceProviderInterface {

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

    /** A runner is identified with this number by the resource manager.
     *
     * @param runnerRef
     */
    void setRunnerRef( int runnerRef );
    int getRunnerRef();

    /** Set a resource definition.
     *
     * @param resourceHash
     * @param resourceDescription
     */
    void setResource( String resourceHash, String resourceDescription);

    /** Acquire a resource.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return MachineResourceInterface, used to control the bound resource
     */
    MachineResource bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;

    /** Cancel resource requests associated with this runner instance  */
    void cancel();
}