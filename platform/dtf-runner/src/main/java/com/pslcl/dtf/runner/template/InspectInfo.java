package com.pslcl.dtf.runner.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class InspectInfo {
	private ResourceInstance resourceInstance;
    private File fileArchiveTopDirectory;
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
		this.fileArchiveTopDirectory = null;
		this.strInstructionsHash = strInstructionsHash;
		this.artifacts = artifacts;
		this.instructions = null;
		this.contentStream = null;
		this.future = null;
	}
	
    /**
     * 
     * @param indexNextInspectInfo
     * @throws IOException
     */
	void setupArchiveTopDirectory(int indexNextInspectInfo) throws IOException {
        if (this.fileArchiveTopDirectory == null) {
            // place new empty temporary directory for this particular inspectInfo, with delete if needed, to hold all specified artifacts to inspect
            this.fileArchiveTopDirectory = new File(InspectHandler.tempArtifactDirectory + indexNextInspectInfo);
            FileUtils.deleteDirectory(this.fileArchiveTopDirectory); // whether directory is present, or not, this operates without exception
            this.fileArchiveTopDirectory.mkdirs();
        }
    }

    /**
     * 
     * @return
     */
	File getFileArchiveTopDirectory() {
    	return this.fileArchiveTopDirectory;
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