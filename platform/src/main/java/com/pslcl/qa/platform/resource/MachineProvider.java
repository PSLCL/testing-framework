package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;

/**
 * 
 * 
 *
 */
public interface MachineProvider extends ResourceProvider {
    
    /**
     * 
     * @param machineDescription
     * @return
     */
    Machine getMachine(String machineDescription);
    
}
