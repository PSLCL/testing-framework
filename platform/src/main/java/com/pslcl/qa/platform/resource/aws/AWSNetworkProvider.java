package com.pslcl.qa.platform.resource.aws;

import com.pslcl.qa.platform.resource.Network;
import com.pslcl.qa.platform.resource.NetworkProvider;
import com.pslcl.qa.platform.resource.Resource;
import com.pslcl.qa.platform.resource.ResourceNotFoundException;

public class AWSNetworkProvider implements NetworkProvider {

	@Override
	public void setResource(String resourceHash, String resourceDescription) {
		// TODO Auto-generated method stub

	}

	@Override
	public Network bind(String resourceHash, String resourceAttributes)
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
