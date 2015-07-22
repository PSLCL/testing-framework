package com.pslcl.qa.runner.resource.aws;

import com.pslcl.qa.runner.ArtifactNotFoundException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;

public class AWSMachineInstance implements MachineInstance {

	@Override
	public String getHash() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deploy(String componentName, String artifactName,
			String artifactHash) throws ResourceNotFoundException,
			ArtifactNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void inspect(String instructionsHash, String componentName,
			String artifactName, String artifactHash)
			throws ResourceNotFoundException, ArtifactNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void connect(String networkRef) throws ResourceNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean run(String componentName, String artifactName,
			String artifactHash, String params)
			throws ResourceNotFoundException, ArtifactNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getReference() {
		// TODO Auto-generated method stub
		return 0;
	}

}
