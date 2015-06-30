package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;

/**
 * 
 * 
 *
 */
public interface PersonProvider extends ResourceProvider {
    
    /**
     * 
     * @param personDescription
     * @return
     */
    Person getPerson(String personDescription);

}
