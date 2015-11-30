package com.pslcl.dtf.runner.template;

import java.util.Map;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class InspectInfo {
	private ResourceInstance resourceInstance;
	private String strInstructionsHash;
	private Map<String, String> artifacts;
	
	public InspectInfo(ResourceInstance resourceInstance, String strInstructionsHash, Map<String, String> artifacts) {
		this.resourceInstance = resourceInstance;
		this.strInstructionsHash = strInstructionsHash;
		this.artifacts = artifacts;
	}
	
	String getInstructionsHash() {
		return strInstructionsHash;
	}

	Map<String, String> getArtifacts() {
		return artifacts;
	}
	
	ResourceInstance getResourceInstance() {
		return resourceInstance;
	}

}