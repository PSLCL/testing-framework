package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A ResourceQuery is a resource hash and a map of attributes.
 */
public class ResourceWithAttributes {
	private String hash;
	private Map<String, String> attributes;
	
	public ResourceWithAttributes(String hash, Map<String, String> attributes){
		this.hash = hash;
		this.attributes = attributes;
	}
	
	/**
	 * Get the hash of the requested resource.
	 * @return The hash of the requested resource.
	 */
	public String getHash(){
		return hash;
	}
	
	/**
	 * Get the map of attributes for the requested resource.
	 * @return A map of attributes for the requested resource.
	 */
	public Map<String, String> getAttributes(){
		return attributes;
	}

}
