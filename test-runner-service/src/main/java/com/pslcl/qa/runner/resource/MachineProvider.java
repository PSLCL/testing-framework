package com.pslcl.qa.runner.resource;

import java.util.List;
import java.util.concurrent.Future;



/**
 * A Resource provider which allows reserving and binding of Machine resource types.
 */
public interface MachineProvider extends ResourceProvider, ArtifactConsumer {
    
	/** 
     * Acquire a Machine.
     * 
     * This resource must be released once it is no longer needed.
     *
     * @param resource
     * @return MachineInstance object which represents the Machine as a ResourceInstance.
     * @note This MachineInstance is more explicitly defined than the ResourceInstance returned by ResourceProvider.bind().
     * @see ResourceProvider
     */
	@Override
	public Future<MachineInstance> bind( ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException;
	
	/** 
     * Acquire a list of machines. Available machines will be bound and a list containing the resulting {@link MachineInstance} objects will be
     * returned.
     * 
     * The resources must be released once they are no longer needed.
     *
     * @param resources A list of resources with attributes.
     * @return A list of {@link MachineInstance} objects which each represent a Machine Instance.
     */
	@Override
	public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback);
}
