package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.resource.ResourceInstance;

public class OrderedResourceInfo implements Comparable<OrderedResourceInfo> {
    private ResourceInstance resourceInstance;
    private ArtifactInfo artifactInfo = null;
    
    public OrderedResourceInfo(ResourceInstance resourceInstance) {
        this.resourceInstance = resourceInstance;
    }
    
    private int getReference() {
        return resourceInstance.getReference();
    }

    @Override
    public int compareTo(OrderedResourceInfo ori) {
        return this.resourceInstance.getReference() - ori.getReference();
    }
    
    public void setArtifactInfo(ArtifactInfo artifactInfo) {
        this.artifactInfo = artifactInfo;
    }

}
