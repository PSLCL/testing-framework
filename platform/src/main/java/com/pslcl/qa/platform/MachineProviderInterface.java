package com.pslcl.qa.platform;

/**
 * 
 * 
 *
 */
public interface MachineProviderInterface extends ResourceProviderInterface {
    
    /**
     * 
     * @param machineDescription
     * @return
     */
    Hash getMachine(String machineDescription);
    
}
