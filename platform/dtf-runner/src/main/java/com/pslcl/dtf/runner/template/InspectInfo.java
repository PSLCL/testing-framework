package com.pslcl.dtf.runner.template;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class InspectInfo {
	private ResourceInstance resourceInstance;
	private String strInstructionsHash;    // from template include step: the hash to access a file that holds instructions
	private Map<String, String> artifacts; // from template include step: {filename, hash}; hash to access content of a file
	private String instructions;
	private TarArchiveInputStream contentStream;
	
	public InspectInfo(ResourceInstance resourceInstance, String strInstructionsHash, Map<String, String> artifacts) {
		this.resourceInstance = resourceInstance;
		this.strInstructionsHash = strInstructionsHash;
		this.artifacts = artifacts;
		this.instructions = null;
		this.contentStream = null;
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
	
	String getInstructions() {
		return this.instructions;
	}
	void setInstruction(String instructions) {
		this.instructions = instructions;
	}
	
	TarArchiveInputStream getContentStream() {
		return this.contentStream;
	}
	void setContentStream(TarArchiveInputStream contentStream) {
		this.contentStream = contentStream;
	}

}