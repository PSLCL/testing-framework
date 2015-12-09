package com.pslcl.dtf.core.runner.resource;

/**
 * ResourceReserveDisposition identifies the outcome of a resource reserve request.
 * 
 *
 */
public class ResourceReserveDisposition {
	private ResourceDescription inputResourceDescription;
	private ReservedResource reservedResource;
	private boolean invalidResource = false;
	private boolean unavailableResource = false;
	
	/**
	 * 
	 * @param inputResourceDescription
	 * @param reservedResource Must not be null
	 */
	public ResourceReserveDisposition(ResourceDescription inputResourceDescription, ReservedResource reservedResource) {
		this.inputResourceDescription = inputResourceDescription;
		this.reservedResource = reservedResource;
	}
	
	/**
	 * @note This establishes that resource is unavailable; follow this with .setInvalidResource() to establish invalid resource, instead. 
	 * @param inputResourceDescription
	 */
	public ResourceReserveDisposition(ResourceDescription inputResourceDescription) {
		this.inputResourceDescription = inputResourceDescription;
		this.reservedResource = null;
		this.unavailableResource = true;
	}
	
	/**
	 * 
	 */
	public void setInvalidResource() {
		this.invalidResource = true;
		this.unavailableResource = false;
		this.reservedResource = null;
	}

	/**
	 * 
	 * @return "input" ResourceDescription, will not be null
	 */
	public ResourceDescription getInputResourceDescription() {
		return this.inputResourceDescription;
	}
	
	/**
	 * 
	 * @return "output" reservedResource, will be null for invalid or unavailable resource
	 */
	public ReservedResource getReservedResource() {
		return this.reservedResource;
	}
	
	/**
	 * 
	 * @return true for invalid resource, otherwise false
	 */
	public boolean isInvalidResource() {
		return this.invalidResource;
	}
	
	/**
	 * 
	 * @return true for unavailable resource, otherwise false
	 */
	public boolean isUnavailableResource() {
		return this.unavailableResource;
	}

}