package com.pslcl.qa.platform;

/**
 * 
 * 
 *
 */
public interface NetworkProviderInterface extends ResourceProviderInterface {
    
    /**
     * 
     * @param networkDescription
     * @return
     */
    Hash getMachine(String networkDescription);

}
