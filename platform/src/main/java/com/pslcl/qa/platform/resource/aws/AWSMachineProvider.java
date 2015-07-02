package com.pslcl.qa.platform.resource.aws;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.resource.MachineInstance;
import com.pslcl.qa.platform.resource.MachineProvider;
import com.pslcl.qa.platform.resource.ResourceInstance;
import com.pslcl.qa.platform.resource.ResourceNotFoundException;

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
	public void updateArtifact(String component, String version, String platform, String name, Hash hash) {
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
	public MachineInstance bind(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAvailable(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void release(ResourceInstance resource) {
		// TODO Auto-generated method stub
		
	}

}
