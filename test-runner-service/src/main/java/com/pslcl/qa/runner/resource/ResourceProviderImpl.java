package com.pslcl.qa.runner.resource;

import java.util.ArrayList;
import java.util.List;

public class ResourceProviderImpl implements ResourceProvider {

    @Override
    public void setResource(String resourceHash, String resourceDescription) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ResourceInstance bind(ResourceWithAttributes resourceWithAttributes) throws ResourceNotFoundException {
        System.out.println("ResourceProviderImpl.bind() called with resourceHash/resourceAttributes: " + resourceWithAttributes.getHash() + " / " + resourceWithAttributes.getAttributes());
        
        // TODO: From hash and attributes, figure out correct concrete resource provider to call, and call it.
        //       Note that this final call must fill not only the return ResourceInstance with something like a MachineImpl or a PersonImpl, but it must fill reference, which comes from param resourceWithAttributes  
        
        return null;
    }

    @Override
    public List<? extends ResourceInstance> bind(List<? extends ResourceWithAttributes> resources) {
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
        // TODO: From hash and attributes for each list element, figure out correct concrete resource provider to call, and call it.

        return null;
    }

}
