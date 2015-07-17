package com.pslcl.qa.runner.resource;

import java.util.List;

public class ResourceQueryResult {
	private List<ResourceWithAttributes> availableResources;
	private List<ResourceWithAttributes> unavailableResources;
	private List<ResourceWithAttributes> invalidResources;
	private List<ReservedResourceWithAttributes> reservedResources;
	
	public ResourceQueryResult(List<ReservedResourceWithAttributes> reservedResources, List<ResourceWithAttributes> availableResources, 
			List<ResourceWithAttributes> unavailableResources, List<ResourceWithAttributes> invalidResources){
		this.reservedResources = reservedResources;
		this.availableResources = availableResources;
		this.unavailableResources = unavailableResources;
		this.invalidResources = invalidResources;		
	}
	
	/**
	 * Get a list of reserved resources.
	 * 
	 * @return A list of reserved resources.
	 */
	public List<ReservedResourceWithAttributes> getReservedResources(){
		return reservedResources;
	}
	
	/**
	 * Get the list of available resources. These resources are not bound and may become unavailable after this object is created.
	 * 
	 * @return A list of resources available at the time this object was created.
	 */
	public List<ResourceWithAttributes> getAvailableResources() {
		return availableResources;
	}
	
	/**
	 * Get the list of unavailable resources. These resources may become available after this object is created.
	 * 
	 * @return A list of resources unavailable at the time this object was created.
	 */
	public List<ResourceWithAttributes> getUnavailableResources() {
		return unavailableResources;
	}
	
	/**
	 * Get the list of requested resources not understood by the resource provider.
	 * 
	 * @return A list of requested resources not understood by the resource provider.
	 */
	public List<ResourceWithAttributes> getInvalidResources() {
		return invalidResources;
	}
	
	
}
