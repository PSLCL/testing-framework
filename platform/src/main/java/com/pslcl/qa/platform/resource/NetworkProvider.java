package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.ResourceNotFoundException;


/**
 * 
 * 
 *
 */
public interface NetworkProvider extends ResourceProvider {
    
	/** 
     * Acquire a Network.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Network object which represents the Network Resource Instance.
     */
	@Override
	public Network bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;

}
