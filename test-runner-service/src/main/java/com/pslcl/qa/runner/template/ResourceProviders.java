package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceProvider;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceStatusCallback;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceWithAttributesInstance;
import com.pslcl.qa.runner.resource.aws.AWSMachineProvider;
import com.pslcl.qa.runner.resource.aws.AWSNetworkProvider;
import com.pslcl.qa.runner.resource.aws.AWSPersonProvider;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders implements ResourceProvider {

    private final List<ResourceProvider> resourceProviders;

    /**
     * constructor
     */
    public ResourceProviders() {
        resourceProviders = new ArrayList<>(); // list of class objects that implement the ResourceProvider interface

        // TEMPORARY: hard code all known resourceProviders and instantiate each one
        resourceProviders.add(new AWSMachineProvider(null)); // TODO: replace null
        resourceProviders.add(new AWSPersonProvider());
        resourceProviders.add(new AWSNetworkProvider());
        // Note: Do not include ResourceProviders in this list
        
        // TODO: programmatically determine this list and instantiate each one, as needed
    }
    
    public void init(RunnerServiceConfig config) throws Exception
    {
    }
    
    public void destroy() 
    {
    }
    

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> reserveResourceRequests, int timeoutSeconds) {
        
        // Identify each ResourceProvider (like AWSMachineProvider) that understands specific requirements of individual elements of param ResourceWithAttributes and can reserve the corresponding resource.
        //    This class has a list of all types of ResourceProvider (like AWSMachineProvider).

        // Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes?

        // start retRqr with empty lists; afterward merge reservation results into retRqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>(),
                                                             new ArrayList<ResourceWithAttributes>());

        // invite every ResourceProvider to reserve each resource in param reserveResourceRequests, as it can and as it will
        for (ResourceProvider rp : resourceProviders) {
            // but 1st, to avoid multiple reservations: for any past success in filling any of the several rrwa S in retRqr, strip param reserveResourceRequests of that entry
            for (ReservedResourceWithAttributes rrwa : retRqr.getReservedResources()) {
                for (ResourceWithAttributes rwa : reserveResourceRequests) {
                    ResourceWithAttributesInstance rwai = ResourceWithAttributesInstance.class.cast(rwa);
                    if (rwai.matches(rrwa)) {
                        reserveResourceRequests.remove(rwa);
                        break; // done with this rwa; as we leave the for loop, we also avoid the trouble caused by altering reserveResourceRequests 
                    }
                }                
            }
            if (reserveResourceRequests.isEmpty()) {
                // The act of finding reserveResourceRequests to be empty means all requested resources are reserved.
                //     retRqr reflects that case: it holds all original reservation requests. 
                //     We do not not need to check further- not for this rp and not for any remaining rp that have not been checked yet.
                break;
            }

            // this rp reserves each resource in param resources, as it can and as it will 
            ResourceQueryResult localRqr = rp.reserveIfAvailable(reserveResourceRequests, timeoutSeconds);
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

    @Override
    public Future<? extends ResourceInstance> bind(ReservedResourceWithAttributes reservedResourceWithAttributes, ResourceStatusCallback statusCallback) throws BindResourceFailedException {
        // Note: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        System.out.println("ResourceProviders.bind() called with resourceName/resourceAttributes: " + reservedResourceWithAttributes.getName() + " / " + reservedResourceWithAttributes.getAttributes());

        Future<? extends ResourceInstance> retRI = null;
        try {
            // note that this bind call  must fill the return ResourceInstance with not only something like an implementation of MachineInstance or a PersonInstance, but it must fill reference, which comes from param resourceWithAttributes
            retRI = reservedResourceWithAttributes.getResourceProvider().bind(reservedResourceWithAttributes, null);
        } catch (BindResourceFailedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retRI;
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback) throws BindResourceFailedException {
        // Note: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        List<Future<? extends ResourceInstance>> retRiList = new ArrayList<>();
        for(int i=0; i<resources.size(); i++) {
            try {
                retRiList.add(this.bind(resources.get(i), null));
            } catch (BindResourceFailedException e) {
                retRiList.add(null);
                System.out.println("ResourceProviders.bind(List<> resources) stores null entry");
            }
        }
        return retRiList;
    }  

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String hash) {
        // ResourceProviders does not supply hashes, attributes or descriptions.
        return null;
    }

    @Override
    public List<String> getNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable) {
        // TODO Auto-generated method stub
        
    }
    
}