package com.pslcl.qa.runner.resource;

import java.util.List;

/**
 * Hold and resolve results
 */
public class ResourceQueryResult {
    private List<ReservedResourceWithAttributes> reservedResources;
    private List<ResourceWithAttributes> availableResources;
	private List<ResourceWithAttributes> unavailableResources;
	private List<ResourceWithAttributes> invalidResources;
	
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
	
	/**
	 * Merge a given ResourceQueryResult to this object
	 * 
     * @note Once the reservation is made (a reservedResource entry is placed into the reserved list), it will not be removed
	 * @param localRqr Caller is responsible to ensure that localRqr does not contain a ReservedResourceWithAttributes entry that is already stored in the reservedResources entry of this object.
	 */
	public void merge(ResourceQueryResult localRqr) {
	    for (ReservedResourceWithAttributes rrwa : localRqr.getReservedResources()) {
	        // record successful reservation and remove corresponding entry that others may have placed from the three "fail" lists 
	        this.reservedResources.add(rrwa);
	        this.availableResources.remove(rrwa);
            this.unavailableResources.remove(rrwa);
	        this.invalidResources.remove(rrwa);
	        // caller sees only the reserved entry
	    }
	    
	    // keep in mind: once a successful rwa is entered into this object, the caller will not submit it to us in follow on calls 
	    for (ResourceWithAttributes rwa : localRqr.getAvailableResources()) {
	        // rwa is not found in incoming reservedResources, unavailableResources or invalidResources
	        this.availableResources.add(rwa); // might add to an entry from a previous call
	    }
	    for (ResourceWithAttributes rwa : localRqr.getUnavailableResources()) {
	        
	        this.unavailableResources.add(rwa); // might add to an entry from a previous call
	    }
	    for (ResourceWithAttributes rwa : localRqr.getInvalidResources()) {
	        this.invalidResources.add(rwa); // might add to an entry from a previous call
	    }
	}
	
}
