package com.pslcl.qa.runner.resource;



/**
 * A Resource provider which allows binding of Cable resource types.
 */
public interface CableProvider extends ResourceProvider {
	/** 
     * Acquire a Cable.
     *
     * @param resource
     * @return Cable object which represents the Cable Resource Instance.
     */
	@Override
	public CableInstance bind( ReservedResourceWithAttributes resource ) throws ResourceNotFoundException;
}
