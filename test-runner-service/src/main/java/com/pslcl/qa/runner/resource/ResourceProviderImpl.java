package com.pslcl.qa.runner.resource;

import java.util.List;

public class ResourceProviderImpl implements ResourceProvider {

    @Override
    public void setResource(String resourceHash, String resourceDescription) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ResourceInstance bind(ResourceWithAttributes resourceWithAttributes) throws ResourceNotFoundException {
        System.out.println("ResourceProviderImpl.bind() called with resourceHash/resourceAttributes: " + resourceWithAttributes.getHash() + " / " + resourceWithAttributes.getAttributes());
        
        return null;
    }

    @Override
    public List<ResourceInstance> bind(List<ResourceWithAttributes> resources) {
        // TODO Auto-generated method stub
        return null;
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
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds) {
        // TODO Auto-generated method stub
        return null;
    }

//    @Override
//    public void setResource(String resourceHash, String resourceDescription) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public MachineInstance bind(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
//        System.out.println("MachineImpl.bind() called with resourceHash/resourceAttributes: " + resourceHash + " / " + resourceAttributes);
//        
//        // TODO Auto-generated method stub
//        return null;
//    }
//    
//    @Override
//    public void release(ResourceInstance resource) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public boolean isAvailable(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
//        // TODO Auto-generated method stub
//        return false;
//    }
//
//    @Override
//    public void updateArtifact(String component, String version, String platform, String name, String artifactHash) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void removeArtifact(String component, String version, String platform, String name) {
//        // TODO Auto-generated method stub
//        
//    }
//
//    @Override
//    public void invalidateArtifacts(String component, String version) {
//        // TODO Auto-generated method stub
//        
//    }

}
