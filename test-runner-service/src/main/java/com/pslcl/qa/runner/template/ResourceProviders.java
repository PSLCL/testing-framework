package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.qa.runner.resource.ResourceProvider;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders {

    private List<ResourceProvider> resourceProviders;    

    /**
     * constructor
     */
    public ResourceProviders() {
        resourceProviders = new ArrayList<>();
        
        // TODO: programmatically obtain list of class objects that implement the ResourceProvider interface
        // TEMPORARY: hard code this list, for example:  resourceProviders.add(...);
        //            Note: Do not include ResourceProviderImpl in this list
    }

}