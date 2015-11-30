package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class BindHandler {
	private InstancedTemplate iT;
	private ResourceProviders resourceProviders;
    private List<Future<? extends ResourceInstance>> futuresOfResourceInstances;

	/**
	 * 
	 * @param iT
	 */
	public BindHandler(InstancedTemplate iT) {
		this.iT = iT;
		this.resourceProviders = this.iT.getResourceProviders();
		this.futuresOfResourceInstances = new ArrayList<Future<? extends ResourceInstance>>();
	}
	
	/**
	 * 
	 * @param reserveResourceRequests
	 */
	public void reserveAndInitiateBind(List<ResourceDescription> reserveResourceRequests) {
        // reserve the resource specified by each ResourceDescription, with 360 second timeout for each reservation		
        ResourceQueryResult rqr = resourceProviders.reserveIfAvailable(reserveResourceRequests, 360);
        
        // analyze the success/failure of each reserved resource, one resource for each bind step
    	// rqr.getAvailableResources() can be called, but it is irrelevant to our .reserveIfAvailable() call
        List<ResourceDescription> invalidResources = rqr.getInvalidResources(); // list is not in order
        if (invalidResources!=null && !invalidResources.isEmpty()) {
            System.out.println("ResourceReserveHandler.reserve() finds " + invalidResources.size() + " reports of invalid resource reserve requests");
        }
        List<ResourceDescription> unavailableResources = rqr.getUnavailableResources(); // list is not in order
        if (unavailableResources!=null && !unavailableResources.isEmpty()) {
            System.out.println("ResourceReserveHandler.reserve() finds " + unavailableResources.size() + " reports of unavailable resources for the given reserve requests");
        }
        List<ReservedResource> reservedResources = rqr.getReservedResources(); // list is not in order, but each element of returned list has its stepReference stored
        if (reservedResources!=null) {
            System.out.println("ResourceReserveHandler.reserve() finds " + reservedResources.size() + " successful ReservedResource requests" + (reservedResources.size() <= 0 ? "" : "; they are now reserved"));
        }
        
        // Note: The ultimate rule of this work: an entry in the reservedResource list means that the resource is reserved,
        //           independent of what the other ResourceProviders may have placed in the alternate lists.
        //       Background: For any one resource provider, coding is intended that any reservedResource has no entries in the other three lists.
        //                   However, rqr may be filled with entries from multiple resource providers, each answering their status of reserved, unavailable, or invalid.
        //                   The number of reservedResources (i.e. reservedResources.size()) is our primary interest.

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full reserved success and resultant full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
        if (
        	false && // TEMP: bypass actual bind for a while, until bind works better; allows follow on steps parsing to be exercised
       		reservedResources.size() == reserveResourceRequests.size()) {
            // Start multiple asynch binds. Each bind is performed by one specific resource provider, the same one that reserved the resource in the first place. 
			this.futuresOfResourceInstances = resourceProviders.bind(reservedResources);
			//  Each list element of futuresOfResourceInstances:
			//      can be a null (bind failed while in the act of creating a Future), or
			//      can be a Future, for which future.get():
			//          returns a ResourceInstance on bind success
			//          throws an exception on bind failure
        } else {
      	    // release all the reserved resources
        	for (ReservedResource rr : reservedResources) {
                ResourceCoordinates rc = rr.getCoordinates();
                ResourcesManager rm = rc.getManager();
                
                // TEMP: bypass release: not yet implemented
                //rm.release(rc.templateId, rc.resourceId, false);
        	}
        }
    }

	/**
	 * thread blocks
	 */
	public List<ResourceInstance> waitComplete() throws Exception {
		// At this moment, this.futuresOfResourceInstances is filled. It's Future's each give us a bound ResourceInstance. Our API is to return a list of each of these extracted ResourceInstance's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
		
		// Gather all the bound resource instances from futuresOfResourceInstances, a list of Futures. This thread yields, in waiting for each of the multiple asynch bind calls to complete.
		Exception exception = null;
        List<ResourceInstance> retList = new ArrayList<>();
        boolean allBindsSucceeded = true;
        for (Future<? extends ResourceInstance> future : this.futuresOfResourceInstances) {
        	if (future != null) { // null: bind failed early so move along; do not add to retList
                try {
                	
//                	// temp kluge just to make a failing bind look like it succeeds- this avoids the future; this requires bringing dtf-aws-resource into dtf-runner's dependencies; temporary
//                	AwsMachineProvider awsMP = new AwsMachineProvider(null);
//                	MachineReservedResource reservedResource = awsMP.new MachineReservedResource(null, null, null);
//                	ResourceInstance resourceInstance = new AwsMachineInstance(reservedResource);
                	
					ResourceInstance resourceInstance = future.get(); // blocks until asynch answer comes, or exception, or timeout
					retList.add(resourceInstance);
					
		        	// retrieve this resource's step reference, then use it to establish a lookup to resourceInstance, in hash map referenceToResourceInstance, held in iT
		        	int stepReference = iT.getStepReference(resourceInstance.getCoordinates());
		        	iT.markResourceInstance(stepReference, resourceInstance);
				} catch (InterruptedException ee) {
	        		allBindsSucceeded = false;
					exception = ee;
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                    LoggerFactory.getLogger(getClass()).info(BindHandler.class.getSimpleName() + ".waitComplete(), bind failed: " + msg, ee);
				} catch (ExecutionException e) {
	        		allBindsSucceeded = false;
					exception = e;
                    LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
				} finally {
				}
        	} else {
        		allBindsSucceeded = false;
        	}
        }

        if (!allBindsSucceeded) {
        	// because of this error, retList is not returned; for expected cleanup, caller can find all the ResourceInstances in iT.mapStepReferenceToResourceInstance
        	if (exception != null)
        		throw new Exception(exception);
        	throw new Exception("bind attempt could not create and return a Future");
        }
        
        return retList;
        
//        boolean allBound = (retList.size() == this.futuresOfResourceInstances.size()); // this catches the case of future==null
//        if (!allBound) {
//        	// release all resources that did bind
//        	for (ResourceInstance ri: retList) {
//                ResourceCoordinates rc = ri.getCoordinates();
//                ResourcesManager rm = rc.getManager();
//                rm.release(rc.templateId, rc.resourceId, false);
//        	}
//        	throw new Exception(exception);
//        }
//        
//        for (ResourceInstance resourceInstance: retList) {
//        	//iT.markResourceInstance(0, null);
//        	
//        	// retrieve this resource's step reference, then use it to establish a lookup to resourceInstance, in hash map referenceToResourceInstance, held in iT
//        	int stepReference = iT.getStepReference(resourceInstance.getCoordinates());
//        	iT.markResourceInstance(stepReference, resourceInstance);
//        }
//        
//        return retList;
	}
	
}