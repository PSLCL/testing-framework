package com.pslcl.qa.runner.resource;

import java.util.Map;

/**
 * A resource with attributes that has been reserved by the resource provider. After a timeout period
 * specified by timeoutSeconds has passed, or if the resource is bound, the resource is no longer reserved.
 */
public class ReservedResourceWithAttributes implements ResourceWithAttributes {
    private String hash;
    private Map<String, String> attributes;
    private int reference;
	private ResourceProvider resourceProvider;
    private int timeoutSeconds;

	/**
	 * constructor
	 * @param resourceWithAttributes
	 * @param resourceProvider
	 * @param timeoutSeconds
	 */
    public ReservedResourceWithAttributes(ResourceWithAttributes resourceWithAttributes,
	                                      ResourceProvider resourceProvider, int timeoutSeconds) {
		this(resourceWithAttributes.getHash(), resourceWithAttributes.getAttributes(),
		     resourceWithAttributes.getReference(), resourceProvider, timeoutSeconds);
	}

	/**
	 * constructor
	 * @param hash
	 * @param attributes
	 * @param reference
	 * @param resourceProvider
	 * @param timeoutSeconds
	 */
    public ReservedResourceWithAttributes(String hash, Map<String, String> attributes, int reference,
	                                      ResourceProvider resourceProvider, int timeoutSeconds) {
	    this.hash = hash;
	    this.attributes = attributes;
	    this.reference = reference;	    
		this.resourceProvider = resourceProvider;
		this.timeoutSeconds = timeoutSeconds;
	}

	
	// implement ResourceWithAttributes interface
	
    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public int getReference() {
        return reference;
    }
	
	
	// instance methods

    /**
     * Get the ResourceProvider that reserved the resource.
     * @return the ResourceProvider
     */
	public ResourceProvider getResourceProvider() {
	    return resourceProvider;
	}
	
	/**
	 * Get the number of seconds for which this resource is reserved, measured from now.
	 * @return The number of seconds.
	 */
	public int getTimeoutSeconds(){
		return timeoutSeconds;
	}

}