package com.pslcl.qa.runner.resource.aws;

import com.pslcl.qa.runner.resource.NetworkInstance;
import com.pslcl.qa.runner.resource.NetworkProvider;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;

public class AWSNetworkProvider implements NetworkProvider {

	@Override
	public void setResource(String resourceHash, String resourceDescription) {
		// TODO Auto-generated method stub

	}

	@Override
	public NetworkInstance bind(String resourceHash, String resourceAttributes)
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
