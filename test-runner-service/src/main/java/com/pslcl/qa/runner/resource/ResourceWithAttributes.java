package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A ResourceQuery is a resource hash and a map of attributes.
 */
public interface ResourceWithAttributes {
	
	/**
	 * Get the name of the resource.
	 * @return The name.
	 */
	String getName();	
	
	/**
	 * Get the set of attributes for the resource.
	 * @return A map of strings representing the set of attributes for the resource.
	 */
	Map<String, String> getAttributes();
	
	/**
	 * Get a reference matching this object to the original resource request.
	 * In the context of a template, this reference may be the line number holding the original resource bind request.
	 * @returns The resource reference. 
	 */
	int getReference();

}