package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.resource.ResourceInstance;

public class ResourceInfo implements Comparable<ResourceInfo> {
    private ResourceInstance resourceInstance;
    private ArtifactInfo artifactInfo = null;
    private String instructiondHash = null;
    private String network = null;
    private String runParams = null;

    private int getReference() {
        return resourceInstance.getReference();
    }

    
    // implement Comparable interface
    @Override
    public int compareTo(ResourceInfo ori) {
        return this.resourceInstance.getReference() - ori.getReference();
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
        this.instructiondHash = instructionsHash;
    }
    
    public void setNetwork(String network) {
        this.network = network;
    }
    
    public void setRunParams(String runParams) {
        this.runParams = runParams;
    }
}