package com.pslcl.qa.platform.resource.aws;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.ResourceNotFoundException;
import com.pslcl.qa.platform.resource.Network;
import com.pslcl.qa.platform.resource.NetworkProvider;

public class AWSNetworkProvider implements NetworkProvider {

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
	public void setRunnerRef(int runnerRef) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getRunnerRef() {
		// TODO Auto-generated method stub
		return 0;
	}

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
	public void cancel() {
		// TODO Auto-generated method stub

	}

}
