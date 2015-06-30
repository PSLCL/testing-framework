package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceProviderInterface;

/**
 * 
 * 
 *
 */
public interface NetworkProvider extends ResourceProviderInterface {
    
    /**
     * 
     * @param networkDescription
     * @return
     */
    Hash getMachine(String networkDescription);

}
