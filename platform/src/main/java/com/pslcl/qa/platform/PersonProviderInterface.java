package com.pslcl.qa.platform;

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
