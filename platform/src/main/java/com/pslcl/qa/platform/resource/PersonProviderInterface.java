package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceProviderInterface;

/**
 * 
 * 
 *
 */
public interface PersonProviderInterface extends ResourceProviderInterface {
    
    /**
     * 
     * @param personDescription
     * @return
     */
    Hash getMachine(String personDescription);

}
