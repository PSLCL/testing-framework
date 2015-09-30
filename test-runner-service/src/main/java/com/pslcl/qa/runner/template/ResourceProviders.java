package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotAvailableException;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceProvider;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceWithAttributesInstance;
import com.pslcl.qa.runner.resource.aws.AWSMachineProvider;
import com.pslcl.qa.runner.resource.aws.AWSNetworkProvider;
import com.pslcl.qa.runner.resource.aws.AWSPersonProvider;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders implements ResourceProvider {

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
        // Note: Do not include ResourceProviders in this list
        
        // TODO: programmatically determine this list and instantiate each one, as needed
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
            // but 1st, to avoid multiple reservations: for any success, strip param reserveResourceRequests of that entry
            for (ReservedResourceWithAttributes rrwa : retRqr.getReservedResources()) {
                for (ResourceWithAttributes rwa : reserveResourceRequests) {
                    ResourceWithAttributesInstance rwai = ResourceWithAttributesInstance.class.cast(rwa);
                    if (rwai.matches(rrwa)) {
                        reserveResourceRequests.remove(rwa);
                        break; // leaving loop avoids the trouble caused by altering reserveResourceRequests 
                    }
                }                
            }
            if (reserveResourceRequests.isEmpty())
                break; // the act of emptying reserveResourceRequests means all requested resources are reserved; retRqr reflects that

            // this rp reserves each resource in param resources, as it can and as it will 
            ResourceQueryResult localRqr = rp.reserveIfAvailable(reserveResourceRequests, timeoutSeconds);
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }
    
    @Override
    public ResourceInstance bind(ResourceWithAttributes resourceWithAttributes) throws ResourceNotFoundException {
        // note: param resourceWithAttributes is potentially a ReservedResourceWithAttributes, indicating to the target resource provider that it has already reserved this resource
        // subnote: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        System.out.println("ResourceProviders.bind() called with resourceHash/resourceAttributes: " + resourceWithAttributes.getHash() + " / " + resourceWithAttributes.getAttributes());

        ResourceInstance retRI = null;
        if (ReservedResourceWithAttributes.class.isInstance(resourceWithAttributes)) {
            // preferred path- call resource provider directly- it reserved the resource and will directly bind it
            ReservedResourceWithAttributes reservedResourceWithAttributes = ReservedResourceWithAttributes.class.cast(resourceWithAttributes);
            try {
                // note that this bind call  must fill the return ResourceInstance with not only something like an implementation of MachineInstance or a PersonInstance, but it must fill reference, which comes from param resourceWithAttributes
                retRI = reservedResourceWithAttributes.getResourceProvider().bind(resourceWithAttributes);
            } catch (ResourceNotAvailableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            // attempt bind from scratch- if the proper provider is not known, this needs to call "all the resource providers" to find out which one can perform the bind of param resourceWithAttributes
            
            // TODO: as necessary, optimize by taking advantage of knowledge of each resource provider's hash and attributes. 

        }

        
        return retRI;
    }

    @Override
    public List<? extends ResourceInstance> bind(List<? extends ResourceWithAttributes> resources) {
        // note: param resources is potentially a list of ReservedResourceWithAttributes, indicating to the target resource provider that it has already reserved resources within the list
        // subnote: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        List<ResourceInstance> retRiList = new ArrayList<>();
        for(int i=0; i<resources.size(); i++) {
            try {
                retRiList.add(this.bind(resources.get(i)));
            } catch (ResourceNotFoundException e) {
                retRiList.add(null);
                System.out.println("ResourceProviders.bind(List<> resources) stores null entry");
            }
        }
        return retRiList;
    }    
    
    @Override
    public void setResource(String resourceHash, String resourceDescription) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void release(ResourceInstance resource) {
        // TODO Auto-generated method stub
        
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
    public List<String> getHashes() {
        // ResourceProviders does not supply hashes, attributes or descriptions.
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String hash) {
        // ResourceProviders does not supply hashes, attributes or descriptions.
        return null;
    }

    @Override
    public String getDescription(String hash) {
        // ResourceProviders does not supply hashes, attributes or descriptions.
        return null;
    }
    
}