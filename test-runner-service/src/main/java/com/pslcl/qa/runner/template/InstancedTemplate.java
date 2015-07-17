package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.qa.runner.resource.ResourceInstance;

public class InstancedTemplate {

    //private SortedSet ss;
    private final List<ResourceInstance> refToResource; // can use references.set(idx, String);
    
    InstancedTemplate() {
        refToResource = new ArrayList<ResourceInstance>();
    }
}
