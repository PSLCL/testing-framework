package com.pslcl.qa.runner.resource.aws;

import java.util.List;

import com.pslcl.qa.runner.resource.PersonInstance;
import com.pslcl.qa.runner.resource.PersonProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class AWSPersonProvider implements PersonProvider {

	@Override
	public void updateArtifact(String component, String version,
			String platform, String name, String artifactHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeArtifact(String component, String version,
			String platform, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invalidateArtifacts(String component, String version) {
		// TODO Auto-generated method stub

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
	public List<ResourceInstance> bind(List<ResourceWithAttributes> resources) {
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
	public ResourceQueryResult reserveIfAvailable(
			List<ResourceWithAttributes> resources, int timeoutSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PersonInstance bind(ResourceWithAttributes resource)
			throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
