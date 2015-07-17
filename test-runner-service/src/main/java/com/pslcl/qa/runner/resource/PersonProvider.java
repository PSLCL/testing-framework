package com.pslcl.qa.runner.resource;

import java.util.List;



/**
 *  A Resource provider which allows binding of Person resource types.
 */
public interface PersonProvider extends ResourceProvider, ArtifactConsumer {
    
	/** 
     * Acquire a Person.
     *
     * @param resource
     * @return Person object which represents the Person Resource Instance.
     */
	@Override
	public PersonInstance bind( ResourceWithAttributes resource ) throws ResourceNotFoundException;
}
