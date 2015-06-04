package com.pslcl.qa.platform;

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
     */
    void bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;

    /** Place an artifact on a machine.
     *
     * @param machineRef
     * @param componentName
     * @param artifactName
     * @param artifactHash
     */
    void deploy( int machineRef, String componentName, String artifactName, String artifactHash )
                 throws ResourceNotFoundException, ArtifactNotFoundException;

    /** Ask a person to follow instructions to inspect an artifact.
     *
     * @param personRef
     * @param instructionsHash
     * @param componentName
     * @param artifactName
     * @param artifactHash
     */
    void inspect(int personRef, String instructionsHash, String componentName, String artifactName, String artifactHash)
                 throws ResourceNotFoundException, ArtifactNotFoundException;

    /** Connect a machine to a network.
     *
     * @param machineRef
     * @param networkRef
     */
    void connect( int machineRef, String networkRef )  throws ResourceNotFoundException;

    /** Run a program artifact on a machine.
     *
     * @param machineRef
     * @param componentName
     * @param artifactName
     * @param artifactHash
     * @param params
     * @return
     */
    boolean run( int machineRef, String componentName, String artifactName, String artifactHash, String params )
                 throws ResourceNotFoundException, ArtifactNotFoundException;

    /** Cancel resource requests associated with this runner instance  */
    void cancel();
}