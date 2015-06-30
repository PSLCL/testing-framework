package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.ResourceNotFoundException;


/**
 * A Resource provider which allows binding of Cable resource types.
 */
public interface CableProvider extends ResourceProvider {
	/** 
     * Acquire a Cable.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Cable object which represents the Cable Resource Instance.
     */
	@Override
	public Cable bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
