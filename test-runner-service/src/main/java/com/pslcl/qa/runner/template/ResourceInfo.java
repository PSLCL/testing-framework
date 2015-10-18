package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.resource.instance.ResourceInstance;

public class ResourceInfo implements Comparable<ResourceInfo> {
    private ResourceInstance resourceInstance;
    private ArtifactInfo artifactInfo = null;
    private String instructionsHash = null;
    private String network = null;
    private String runParams = null;

    private long getReference() {
        return resourceInstance.getReference();
    }
    
    // implement Comparable interface
    @Override
    public int compareTo(ResourceInfo ori) 
    {
        long delta = resourceInstance.getReference() - ori.resourceInstance.getReference(); 
        
        if(delta < 0)
            return -1;
        if(delta > 0)
            return 1;
        return 0;
    }

   
    // public methods
    
    /**
     * Constructor
     * @param resourceInstance
     */
    public ResourceInfo(ResourceInstance resourceInstance) {
        this.resourceInstance = resourceInstance;
    }

    public ResourceInstance getResourceInstance() {
        return resourceInstance;
    }
    
    public void setArtifactInfo(ArtifactInfo artifactInfo) {
        this.artifactInfo = artifactInfo;
    }
    
    public void setInstructionsHash(String instructionsHash) {
        this.instructionsHash = instructionsHash;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }
    
    public void setRunParams(String runParams) {
        this.runParams = runParams;
    }
}