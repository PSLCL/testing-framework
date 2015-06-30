package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceProviderInterface;

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
