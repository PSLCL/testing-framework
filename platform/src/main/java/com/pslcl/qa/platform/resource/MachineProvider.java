package com.pslcl.qa.platform.resource;



/**
 * A Resource provider which allows binding of Machine resource types.
 */
public interface MachineProvider extends ResourceProvider, ArtifactConsumer {
    
	/** 
     * Acquire a Machine.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Machine object which represents the Machine Resource Instance.
     */
	@Override
	public MachineInstance bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
