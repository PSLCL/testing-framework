package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A resource with attributes that has been reserved by the resource provider. After a timeout period
 * specified by timeoutSeconds has passed, or if the resource is bound, the resource is no longer reserved.
 *
 */
public class ReservedResourceWithAttributes extends ResourceWithAttributes {
	private int timeoutSeconds;

	public ReservedResourceWithAttributes(ResourceWithAttributes resourceWithAttributes, int timeoutSeconds) {
		super(resourceWithAttributes.getHash(), resourceWithAttributes.getAttributes());
		this.timeoutSeconds = timeoutSeconds;
	}

	public ReservedResourceWithAttributes(String hash, Map<String, String> attributes, int timeoutSeconds) {
		super(hash, attributes);
		this.timeoutSeconds = timeoutSeconds;
	}
	
	/**
	 * Get the number of seconds that this resource is reserved for.
	 * @return The number of seconds that this resource is reserved for.
	 */
	public int getTimeoutSeconds(){
		return timeoutSeconds;
	}

}
