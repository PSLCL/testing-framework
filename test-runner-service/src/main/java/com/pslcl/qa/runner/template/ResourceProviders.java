package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceProvider;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceWithAttributesImpl;
import com.pslcl.qa.runner.resource.aws.AWSMachineProvider;
import com.pslcl.qa.runner.resource.aws.AWSNetworkProvider;
import com.pslcl.qa.runner.resource.aws.AWSPersonProvider;

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
        
        // Note: Do not include ResourceProviderImpl in this list
        resourceProviders.add(new AWSMachineProvider(null));
        resourceProviders.add(new AWSPersonProvider());
        resourceProviders.add(new AWSNetworkProvider());
    }
    
    ResourceQueryResult reserve(List<ResourceWithAttributes> resources, int timeoutSeconds) {
        // these accumulate
        List<ReservedResourceWithAttributes> reservedResources = new ArrayList<>();
        List<ResourceWithAttributes> availableResources = new ArrayList<>();
        List<ResourceWithAttributes> unavailableResources = new ArrayList<>();
        List<ResourceWithAttributes> invalidResources = new ArrayList<>();
        
        // start with empty lists
        ResourceQueryResult retRqr = new ResourceQueryResult(reservedResources, availableResources, unavailableResources, invalidResources);
        
        for (ResourceProvider rp : resourceProviders) {
            // to avoid multiple reservations: for any most recent success, strip resources of that entry
            for (ReservedResourceWithAttributes rrwa : retRqr.getReservedResources()) {
                for (ResourceWithAttributes rwa : resources) {
                    ResourceWithAttributesImpl rwai = ResourceWithAttributesImpl.class.cast(rwa);
                    if (rwai.match(rrwa)) {
                        resources.remove(rwa);
                        break; // note that resources is altered, but there is no need to check it again 
                    }
                }                
            }
            if (resources.isEmpty())
                break; // done: all requested resources are reserved, retRqr reflects that
            
            ResourceQueryResult localRqr = rp.reserveIfAvailable(resources, timeoutSeconds);
            retRqr.resolve(localRqr); // modifies retRqr
        }
        return retRqr;
    }

}