package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.qa.runner.resource.ResourceInstance;

/**
 * InstancedTemplate is all information about a template to allow it to be used, for the first time or to be re-used
 *
 */
public class InstancedTemplate {

    //private SortedSet ss;
    private final List<ResourceInstance> refToResource; // can use references.set(idx, String);
    
    InstancedTemplate() {
        refToResource = new ArrayList<ResourceInstance>();
    }
}
