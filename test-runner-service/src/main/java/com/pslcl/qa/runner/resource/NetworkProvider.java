package com.pslcl.qa.runner.resource;



/**
 *  A Resource provider which allows binding of Network resource types.
 */
public interface NetworkProvider extends ResourceProvider {
    
	/** 
     * Acquire a Network.
     *
     * @param resource
     * @return Network object which represents the Network Resource Instance.
     */
	@Override
	public NetworkInstance bind( ResourceWithAttributes resource ) throws ResourceNotFoundException;

}
