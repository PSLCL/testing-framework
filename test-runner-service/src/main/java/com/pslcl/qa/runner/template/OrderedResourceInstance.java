package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.resource.ResourceInstance;

public class OrderedResourceInstance implements Comparable<OrderedResourceInstance> {
    private ResourceInstance resourceInstance;
    
    public OrderedResourceInstance(ResourceInstance resourceInstance) {
        this.resourceInstance = resourceInstance;
    }
    
    private int getReference() {
        return resourceInstance.getReference();
    }

    @Override
    public int compareTo(OrderedResourceInstance ori) {
        return this.resourceInstance.getReference() - ori.getReference();
    }

}
