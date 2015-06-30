package com.pslcl.qa.platform.resource.aws;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceNotFoundException;
import com.pslcl.qa.platform.resource.Person;
import com.pslcl.qa.platform.resource.PersonProvider;
import com.pslcl.qa.platform.resource.Resource;

public class AWSPersonProvider implements PersonProvider {

	@Override
	public void updateArtifact(String component, String version,
			String platform, String name, Hash hash) {
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
	public Person bind(String resourceHash, String resourceAttributes)
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
	public void release(Resource resource) {
		// TODO Auto-generated method stub
		
	}

}
