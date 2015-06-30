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
    Network getNetwork(String networkDescription);

}
