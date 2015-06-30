package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceProviderInterface;

/**
 * 
 * 
 *
 */
public interface CableProviderInterface extends ResourceProviderInterface {

    /**
     * 
     * @param cableDescription
     * @return
     */
    Hash getMachine(String cableDescription);
    
}
