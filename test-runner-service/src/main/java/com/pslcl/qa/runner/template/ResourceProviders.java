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
        resourceProviders = new ArrayList<>(); // list of class objects that implement the ResourceProvider interface

        // TEMPORARY: hard code all known resourceProviders and instantiate each one
        resourceProviders.add(new AWSMachineProvider(null));
        resourceProviders.add(new AWSPersonProvider());
        resourceProviders.add(new AWSNetworkProvider());
        // Note: Do not include ResourceProviderImpl in this list
        
        // TODO: programmatically determine this list and instantiate each one, as needed
    }
    
    /**
     * 
     * @param resources
     * @param timeoutSeconds
     * @return
     */
    ResourceQueryResult reserve(List<ResourceWithAttributes> resources, int timeoutSeconds) {
        // start retRqr with empty lists; afterward merge into these lists
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>());
        // invite every ResourceProvider to reserve each resource in param resources, as it can and as it will
        for (ResourceProvider rp : resourceProviders) {
            // to avoid multiple reservations: for any most recent success, strip param resources of that entry
            for (ReservedResourceWithAttributes rrwa : retRqr.getReservedResources()) {
                for (ResourceWithAttributes rwa : resources) {
                    ResourceWithAttributesImpl rwai = ResourceWithAttributesImpl.class.cast(rwa);
                    if (rwai.matches(rrwa)) {
                        resources.remove(rwa);
                        break; // note that resources is altered, but there is no need to check it again 
                    }
                }                
            }
            if (resources.isEmpty())
                break; // done: all requested resources are reserved, retRqr reflects that

            // this rp reserves each resource in param resources, as it can and as it will 
            ResourceQueryResult localRqr = rp.reserveIfAvailable(resources, timeoutSeconds);
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

}