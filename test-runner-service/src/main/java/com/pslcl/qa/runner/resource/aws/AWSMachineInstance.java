package com.pslcl.qa.runner.resource.aws;

import java.util.Map;

import com.pslcl.qa.runner.ArtifactNotFoundException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceProvider;

public class AWSMachineInstance implements MachineInstance {

	@Override
	public String getHash() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public Map<String, String> getAttributes() {
        // TODO Auto-generated method stub
        return null;
    }	
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
    @Override
    public ResourceProvider getResourceProvider() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public int getTimeoutSeconds() {
        // TODO Auto-generated method stub
        return 0;
    }
    
	@Override
	public void deploy(String componentName, String artifactName,
			String artifactHash) throws ResourceNotFoundException,
			ArtifactNotFoundException {
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
