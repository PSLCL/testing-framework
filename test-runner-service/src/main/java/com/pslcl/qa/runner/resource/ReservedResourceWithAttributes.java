package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A resource with attributes that has been reserved by the resource provider. After a timeout period
 * specified by timeoutSeconds has passed, or if the resource is bound, the resource is no longer reserved.
 *
 */
public class ReservedResourceWithAttributes extends ResourceWithAttributes {
	private ResourceProvider resourceProvider;
    private int timeoutSeconds;

	public ReservedResourceWithAttributes(ResourceWithAttributes resourceWithAttributes, ResourceProvider resourceProvider, int timeoutSeconds) {
		super(resourceWithAttributes.getHash(), resourceWithAttributes.getAttributes(), resourceWithAttributes.getReference());
		this.resourceProvider = resourceProvider;
		this.timeoutSeconds = timeoutSeconds;
	}

	public ReservedResourceWithAttributes(String hash, Map<String, String> attributes, ResourceProvider resourceProvider, int timeoutSeconds, int reference) {
		super(hash, attributes, reference);
		this.resourceProvider = resourceProvider;
		this.timeoutSeconds = timeoutSeconds;
	}
	
	/**
	 * Get the ResourceProvider that reserved the resource.
	 * @return the ResourceProvider
	 */
	public ResourceProvider getResourceProvider() {
	    return resourceProvider;
	}
	
	/**
	 * Get the number of seconds that this resource is reserved for.
	 * @return The number of seconds that this resource is reserved for.
	 */
	public int getTimeoutSeconds(){
		return timeoutSeconds;
	}

}
