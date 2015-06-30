package com.pslcl.qa.platform.resource;


/**
 * 
 * 
 *
 */
public interface CableProvider extends ResourceProvider {

    /**
     * 
     * @param cableDescription
     * @return
     */
    Machine getMachine(String cableDescription);
    
}
