package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.qa.runner.resource.ResourceInstance;

/**
 * InstancedTemplate is all information about a template to allow it to be used, for the first time or to be re-used
 *
 */
public class InstancedTemplate {

    private final List<ResourceInstance> refToResource; // contains step reference number
    private String descriptionHash = null;
    private List<OrderedResourceInfo> orderedResourceInfos = null;
    
    InstancedTemplate(String descriptionHash) {
        this.descriptionHash = descriptionHash;
        refToResource = new ArrayList<ResourceInstance>();
    }
    
    public void setOrderedResourceInfos(List<OrderedResourceInfo> orderedResourceInfos) {
        this.orderedResourceInfos = orderedResourceInfos;
    }
    
    public OrderedResourceInfo getOrderedResourceInfo(int stepReference) {
        return (orderedResourceInfos != null) ? orderedResourceInfos.get(stepReference) : null;
    }
}
