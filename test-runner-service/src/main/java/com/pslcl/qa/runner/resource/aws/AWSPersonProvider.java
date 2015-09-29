package com.pslcl.qa.runner.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.PersonInstance;
import com.pslcl.qa.runner.resource.PersonProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceWithAttributesInstance;

public class AWSPersonProvider implements PersonProvider {

	@Override
	public void updateArtifact(String component, String version, String platform, String name, String artifactHash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeArtifact(String component, String version, String platform, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void invalidateArtifacts(String component, String version) {
		// TODO Auto-generated method stub

	}

	@Override
	public void release(ResourceInstance resource) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<PersonInstance> bind(List<ReservedResourceWithAttributes> resources) {
		// TODO Auto-generated method stub
		return null;
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
		// temporary, to allow progress: return empty rqr
		ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>(),
				new ArrayList<ResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>());
		if (resources.size() > 0) {
			// temporary, to allow progress: return an artificially unavailable resource
			ResourceWithAttributes artificialUnavailableResource = resources.get(0);
			retRqr.getUnavailableResources().add(artificialUnavailableResource);
		}
		return retRqr;
	}

	@Override
	public PersonInstance bind(ReservedResourceWithAttributes resource) throws BindResourceFailedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAttributes(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getNames() {
		// TODO Auto-generated method stub
		return null;
	}
}
