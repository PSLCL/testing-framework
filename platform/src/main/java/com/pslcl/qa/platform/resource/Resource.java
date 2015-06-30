package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;

/**
 * Represents a Resource Instance.
 */
public interface Resource {

	/**
	 * Get the hash of this resource.
	 * 
	 * @return The hash of the resource.
	 */
	Hash getHash();
	
	/**
	 * Get the description of this resource.
	 * @return the description of the resource.
	 */
	String getDescription();
	
}
