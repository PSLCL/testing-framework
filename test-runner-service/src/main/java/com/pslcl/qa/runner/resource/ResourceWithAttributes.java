package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A ResourceQuery is a resource hash and a map of attributes.
 */
public class ResourceWithAttributes {
	private String hash;
	private Map<String, String> attributes;
	private int reference;
	
	/**
	 * Construct a ResourceWithAttributes object.
	 * 
	 * @param hash A hash string uniquely identifying the desired resource.
	 * @param attributes A map of required attribute values.
	 * @param reference A reference which may identify a specific ResourceInstance. In the context of a Template, this reference may be 
	 * the line number on which the resource is bound.
	 */
	public ResourceWithAttributes(String hash, Map<String, String> attributes, int reference){
		this.hash = hash;
		this.attributes = attributes;
		this.reference = reference;
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
	
	/**
	 * 
	 * @returns A reference which may identify a specific ResourceInstance. In the context of a Template, this reference may be 
	 * the line number on which the resource is bound.
	 */
	public int getReference(){
		return reference;
	}

}
