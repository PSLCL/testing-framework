package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;

/**
 * 
 * 
 *
 */
public interface NetworkProvider extends ResourceProvider {
    
    /**
     * 
     * @param networkDescription
     * @return
     */
    Hash getMachine(String networkDescription);

}
