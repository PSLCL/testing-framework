package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.ResourceNotFoundException;


/**
 * 
 * 
 *
 */
public interface MachineProvider extends ResourceProvider {
    
	/** 
     * Acquire a Machine.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Machine object which represents the Machine Resource Instance.
     */
	@Override
	public Machine bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
