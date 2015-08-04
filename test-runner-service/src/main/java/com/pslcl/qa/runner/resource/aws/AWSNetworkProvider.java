package com.pslcl.qa.runner.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.resource.NetworkInstance;
import com.pslcl.qa.runner.resource.NetworkProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class AWSNetworkProvider implements NetworkProvider {

	@Override
	public void setResource(String resourceHash, String resourceDescription) {
		// TODO Auto-generated method stub

	}

	@Override
	public void release(ResourceInstance resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<NetworkInstance> bind(List<? extends ResourceWithAttributes> resources) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void releaseReservedResource(ReservedResourceWithAttributes resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAvailable(ResourceWithAttributes resource)
			throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResourceQueryResult queryResourceAvailability(
			List<ResourceWithAttributes> resources) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds) {
        // temporary, to allow progress: return empty rqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(),
                new ArrayList<ResourceWithAttributes>(),
                new ArrayList<ResourceWithAttributes>(),
                new ArrayList<ResourceWithAttributes>());
        if (resources.size() > 0) {
            // temporary, to allow progress: return an artificially unavailable resource
            ResourceWithAttributes artificialUnavailableResource = resources.get(0);
            retRqr.getUnavailableResources().add(artificialUnavailableResource);
        }
        return retRqr;
	}

	@Override
	public NetworkInstance bind(ResourceWithAttributes resource)
			throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public List<String> getHashes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String hash) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription(String hash) {
        // TODO Auto-generated method stub
        return null;
    }

}
