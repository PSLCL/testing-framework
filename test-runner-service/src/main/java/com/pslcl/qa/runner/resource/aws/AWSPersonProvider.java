package com.pslcl.qa.runner.resource.aws;

import com.pslcl.qa.runner.resource.PersonInstance;
import com.pslcl.qa.runner.resource.PersonProvider;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;

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
	public PersonInstance bind(String resourceHash, String resourceAttributes)
			throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAvailable(String resourceHash, String resourceAttributes)
			throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void release(ResourceInstance resource) {
		// TODO Auto-generated method stub
		
	}

}
