package com.pslcl.qa.runner.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.MachineProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class AWSMachineProvider implements MachineProvider {
	private final AmazonEC2Client ec2Client;
	private final AWSResourceProviderProperties properties;
	private final List<AWSMachineInstance> instances = new ArrayList<AWSMachineInstance>();
	
	public AWSMachineProvider(AWSResourceProviderProperties properties){
		this.properties = properties;
		DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
		ec2Client = new AmazonEC2Client(providerChain);		
	}

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
	public void setResource(String resourceHash, String resourceDescription) {
		// TODO Auto-generated method stub

	}

	@Override
	public void release(ResourceInstance resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<MachineInstance> bind(List<? extends ResourceWithAttributes> resources) {
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
	public MachineInstance bind(ResourceWithAttributes resource)
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
