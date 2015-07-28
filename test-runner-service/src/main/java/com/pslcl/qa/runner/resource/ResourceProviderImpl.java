package com.pslcl.qa.runner.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.template.ResourceProviders;

public class ResourceProviderImpl implements ResourceProvider {

    private ResourceProviders resourceProviders;
    
    /**
     * constructor
     */
    public ResourceProviderImpl() {
        resourceProviders = new ResourceProviders();
    }
    
    @Override
    public void setResource(String resourceHash, String resourceDescription) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ResourceInstance bind(ResourceWithAttributes resourceWithAttributes) throws ResourceNotFoundException {
        // note: param resourceWithAttributes is potentially a ReservedResourceWithAttributes, indicating to the target resource provider that it has already reserved this resource
        // subnote: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        System.out.println("ResourceProviderImpl.bind() called with resourceHash/resourceAttributes: " + resourceWithAttributes.getHash() + " / " + resourceWithAttributes.getAttributes());

        // Identify that instance of ResourceProvider that responds to a bind call with the given ResourceWithAttributes and returns a bound resource.
        //       Note that this final call must fill not only the return ResourceInstance with something like a MachineImpl or a PersonImpl, but it must fill reference, which comes from param resourceWithAttributes
        // TODO: as necessary, optimize by taking advantage of knowledge of each resource provider's hash and attributes. 
        
        return null;
    }

    @Override
    public List<? extends ResourceInstance> bind(List<? extends ResourceWithAttributes> resources) {
        // note: param resources is potentially a list of ReservedResourceWithAttributes, indicating to the target resource provider that it has already reserved resources within the list
        // subnote: Current implementation assumes only one instance is running of test-runner-service; so a resource reserved by a provider automatically matches this test-runner-service
        List<ResourceInstance> retRi = new ArrayList<>();
        for(int i=0; i<resources.size(); i++) {
            try {
                retRi.add(this.bind(resources.get(i)));
            } catch (ResourceNotFoundException e) {
                retRi.add(null);
                System.out.println("ResourceProviderImpl.bind(List<> resources) stores null entry");
            }
        }
        return retRi;
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
        // TODO: From hash and attributes, figure out correct concrete resource provider to call, and call it.

        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources) {
        // TODO: From hash and attributes for each list element, figure out correct concrete resource provider to call, and call it.
        
        return null;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds) {
        // Identify that instance of ResourceProvider that responds to this call with the given ResourceWithAttributes and returns reserved resources.
        // TODO: optimize by taking advantage of knowledge of each resource provider's hash and attributes. 
        return null;
    }

    @Override
    public List<String> getHashes() {
        // ResourceProviderImpl does not supply hashes, attributes or descriptions.
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String hash) {
        // ResourceProviderImpl does not supply hashes, attributes or descriptions.
        return null;
    }

    @Override
    public String getDescription(String hash) {
        // ResourceProviderImpl does not supply hashes, attributes or descriptions.
        return null;
    }

}
