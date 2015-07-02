package com.pslcl.qa.platform.resource;



/**
 *  A Resource provider which allows binding of Person resource types.
 */
public interface PersonProvider extends ResourceProvider, ArtifactConsumer {
    
	/** 
     * Acquire a Person.
     *
     * @param resourceHash
     * @param resourceAttributes
     * @return Person object which represents the Person Resource Instance.
     */
	@Override
	public PersonInstance bind( String resourceHash, String resourceAttributes ) throws ResourceNotFoundException;
}
