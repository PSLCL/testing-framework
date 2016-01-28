package com.pslcl.dtf.runner.template;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class InspectInfo {
	private ResourceInstance resourceInstance;
	private String strInstructionsHash;    // from template include step: the hash to access a file that holds instructions
	private Map<String, String> artifacts; // from template include step: {filename, hash}; hash to access content of a file
	private String instructions;
	private InputStream contentStream;
	private Future<? extends Void> future;
	
	/**
	 * 
	 * @param resourceInstance
	 * @param strInstructionsHash
	 * @param artifacts
	 */
	public InspectInfo(ResourceInstance resourceInstance, String strInstructionsHash, Map<String, String> artifacts) {
		this.resourceInstance = resourceInstance;
		this.strInstructionsHash = strInstructionsHash;
		this.artifacts = artifacts;
		this.instructions = null;
		this.contentStream = null;
		this.future = null;
	}
	
	/**
	 * 
	 * @return
	 */
	String getInstructionsHash() {
		return strInstructionsHash;
	}

	/**
	 * 
	 * @return
	 */
	Map<String, String> getArtifacts() {
		return artifacts;
	}
	
	/**
	 * 
	 * @return
	 */
	ResourceInstance getResourceInstance() {
		return resourceInstance;
	}
	
	/**
	 * 
	 * @return
	 */
	String getInstructions() {
		return this.instructions;
	}
	void setInstruction(String instructions) {
		this.instructions = instructions;
	}

	/**
	 * 
	 * @return
	 */
	InputStream getContentStream() {
		return this.contentStream;
	}
	void setContentStream(InputStream contentStream) {
		this.contentStream = contentStream;
	}
	
	/**
	 * 
	 * @return
	 */
	Future<? extends Void> getInspectFuture() {
		return this.future;
	}
	void setInspectFuture(Future<? extends Void> future) {
		this.future = future;
	}
	
}