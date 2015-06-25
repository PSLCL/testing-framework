package com.pslcl.qa.platform;

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
