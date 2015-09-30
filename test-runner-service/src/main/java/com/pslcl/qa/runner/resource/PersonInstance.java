package com.pslcl.qa.runner.resource;

import java.util.Map;

import com.pslcl.qa.runner.ArtifactNotFoundException;

/**
 * Represents a Person Resource instance.
 */
public interface PersonInstance extends ResourceInstance {

	/**
	 * Ask a person to follow a set of instructions. Included artifacts will be sent to the person as an archive named
	 * attachments.tar.gz.
	 * 
	 * This method is equivalent to a template Inspect command.
	 *
	 * @param instructions
	 *            An HTML-formatted list of instructions to be sent to the person.
	 * @param artifacts
	 *            A map of artifact filenames(key) and hashes(value).
	 *   
	 * @throws ArtifactNotFoundException if one of the listed artifacts is unknown.
	 */
	void inspect(String instructions, Map<String, String> artifacts) throws ArtifactNotFoundException;

}