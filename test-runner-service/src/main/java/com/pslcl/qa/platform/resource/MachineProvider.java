package com.pslcl.qa.platform.resource;



/**
 * A Resource provider which allows binding of Machine resource types.
 */
public interface MachineProvider extends ResourceProvider, ArtifactConsumer {
    
	/** 
     * Acquire a Machine.
     * 
     * This resource must be released once it is no longer needed.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return MachineInstance object which represents the Machine as a ResourceInstance.
     * @note This MachineInstance is more explicitly defined than the ResourceInstance returned by ResourceProvider.bind().
     * @see com.pslcl.qa.platform.resource#ResourceProvider
     */
	@Override
	public MachineInstance bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
