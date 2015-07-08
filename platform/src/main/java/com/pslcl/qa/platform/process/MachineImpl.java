package com.pslcl.qa.platform.process;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.resource.MachineInstance;
import com.pslcl.qa.platform.resource.MachineProvider;
import com.pslcl.qa.platform.resource.ResourceInstance;
import com.pslcl.qa.platform.resource.ResourceNotFoundException;

public class MachineImpl implements MachineProvider {

//  private String resourceHash = null;
//  private String resourceDescription;
//  private Map<String,String> mapDescription = new HashMap>?;
    
    // MachineResource interface

    @Override
    public MachineInstance bind(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    
    // ResourceProvider interface

    @Override
    public void release(ResourceInstance resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setResource(String resourceHash, String resourceDescription) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isAvailable(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    
    // ArtifactConsumer interface
    
    @Override
    public void updateArtifact(String component, String version, String platform, String name, Hash hash) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void invalidateArtifacts(String component, String version) {
        // TODO Auto-generated method stub
        
    }

}
