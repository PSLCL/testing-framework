package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.ResourceNotFoundException;


/**
 * 
 * 
 *
 */
public interface PersonProvider extends ResourceProvider {
    
	/** 
     * Acquire a Person.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Person object which represents the Person Resource Instance.
     */
	@Override
	public Person bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
