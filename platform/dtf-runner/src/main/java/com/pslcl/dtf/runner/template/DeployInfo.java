package com.pslcl.dtf.runner.template;

import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class DeployInfo {
	
	// static behavior
	
	static private boolean allDeployedSuccess = false;
	
	static public void markAllDeployedSuccess_true() {
		DeployInfo.allDeployedSuccess = true;
	}
	
	static public boolean getAllDeployedSuccess() {
		return allDeployedSuccess;
	}


	// class instance behavior
	
	private final ResourceInstance resourceInstance;
	private final String filename;
	private final String artifactHash;
	private Future<Void> future = null;
	private boolean deployFailed = false;
	
	DeployInfo(ResourceInstance resourceInstance, String filename, String artifactHash) {
		this.resourceInstance = resourceInstance;
		this.filename = filename;
		this.artifactHash = artifactHash;
	}

	ResourceInstance getResourceInstance() {
		return this.resourceInstance;
	}

	String getFilename() {
		return this.filename;
	}

	String getArtifactHash() {
		return this.artifactHash;
	}
	
	void setFuture(Future<Void> future) {
		this.future = future;
	}
	
	Future<Void> getFuture() {
		return this.future;
	}
	
	void markDeployFailed() {
		this.deployFailed = true;
		DeployInfo.allDeployedSuccess = false;
	}
	
	boolean getDeployFailed() {
		return this.deployFailed;
	}
	
}