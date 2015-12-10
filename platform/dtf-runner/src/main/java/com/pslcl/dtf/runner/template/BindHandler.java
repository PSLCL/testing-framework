package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

/**
 * Handle Resource Reserve and Resource Bind activities for multiple resources, in parallel.
 */
public class BindHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
	private boolean reserveInProgress;
	private final ResourceProviders resourceProviders;
	private int nextIndexResourceProvider;
	private Future<List<ResourceReserveDisposition>> currentFutureListOfRRD;
    private List<ReservedResource> currentReservedResources;
    private List<ReservedResource> reservedResources;
    private List<Future<? extends ResourceInstance>> futuresOfResourceInstances;
    List<ResourceInstance> resourceInstances;
    private boolean done;

	private final Logger log;
    private final String simpleName;

    private List<ResourceDescription> reserveResourceRequests;
    
	/**
	 * Constructor: Identify consecutive bind steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public BindHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.reserveResourceRequests = null;
		this.reserveInProgress = true;
		this.done = false;
		this.resourceProviders = this.iT.getResourceProviders();
		this.nextIndexResourceProvider = 0;
		this.currentFutureListOfRRD = null;
		this.currentReservedResources = new ArrayList<>();
		this.reservedResources = new ArrayList<>();
		//this.mapReservedResources = new HashMap<>();
		this.resourceInstances = new ArrayList<>();
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("bind"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}
	
    int getReserveResourceRequestCount() {
    	return reserveResourceRequests!=null ? reserveResourceRequests.size() : 0;
    }
    
    List<ResourceInstance> getResourceInstances() {
    	return this.resourceInstances;
    }
	
	boolean isDone() {
		return done;
	}
	
    /**
     * Parse consecutive bind steps to form a list of ResourceDescription's.
     * Adjusts internal set offset to match.
     * 
     * @return
     */
    void computeReserveRequests(int currentStepReference, String templateId, long runId) throws Exception {
        try {
			reserveResourceRequests = new ArrayList<>();
			if (this.iBeginSetOffset != -1) {
			    for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
			    	String bindStep = setSteps.get(i);
			    	int bindStepReference = currentStepReference + i;
			    	SetStep parsedSetStep = new SetStep(bindStep); // setID bind resourceName attributeKVPs 
			    	                                               // 6 bind machine amzn-size=m3medium&jre=1.7
					log.debug(simpleName + "computeResourceQuery() finds bind step reference " + bindStepReference + " in stepSet " + parsedSetStep.getSetID() + ": " + bindStep);
			    	
			    	String resourceName = null;
			    	String strAttributeKVPs = null;
			    	try {
			    		resourceName = parsedSetStep.getParameter(0); // resourceName comes from the template step, as string "machine", "person" or "network"
						strAttributeKVPs = parsedSetStep.getParameter(1);
					} catch (IndexOutOfBoundsException e) {
						if (resourceName == null) {
							log.debug(simpleName + " bind step does not specify a resource");
							throw e;
						}
						// no attributes are specified in this bind step; legal; swallow exception
					}
			    	
			    	// fill attribute map with n KVPs
			    	Map<String, String> attributeMap = new HashMap<>();
			    	if (strAttributeKVPs != null) {
			        	String[] attributeElements = strAttributeKVPs.split("=|&"); // regex: split on =, and also on &
			        	if (attributeElements.length%2 != 0)
			        		throw new Exception("malformed attribute list");
			    		for (int j=0; j<(attributeElements.length-1); ) // depends on strAttributeKVPs being complete: K=V&K=V&K=V...., such that attributeElements has even number of elements
			    			attributeMap.put(attributeElements[j++], attributeElements[j++]);
			    	}
			    	
			        ResourceCoordinates coord = new ResourceCoordinates(templateId, ResourceDescription.resourceIdMaster.incrementAndGet(), runId);
			        this.iT.markStepReference(coord, bindStepReference);
			        ResourceDescription rd = new ResourceDescImpl(resourceName, // resourceName comes from the template step, as string "machine", "person" or "network"
			        		                                      coord, attributeMap);
			        reserveResourceRequests.add(rd);
					// Note: Each element of the returned this list has had its associated step reference number stored. This number is the line number in a set of template steps, 0 based.  
					//       This stored number is retrieved in this manner: long referenceNum = iT.getReference(listRD.get(0).getCoordinates());
					//       Explanation: Each ResourceDescription contains a ResourceCoordinates object. It gives added value for general Resource use.
					//           It is also used as a key by which to lookup the step reference number (step line number) of its associated bound Resource.
					//           To retrieve a bind step's reference number, call iT.getReference(resourceDescription.getCoordinates())
			    }
			}
		} catch (Exception e) {
			done = true; // as a just in case, we set this even on exception, even though throwing e is thought to close out the entire test run
			throw e;
		}
    }
    
    /**
     * Proceed as far as possible, then return. Set done only when binds complete or error out.
     * 
     * @note First, sequentially call independent ResourceProvider's, with yielding, to gather reserved resources from them. Last, make a single call to bind all reserved resources, in parallel.  
     * 
     * @throws Exception
     */
    void proceed() throws Exception {
    	try {
    		while (!done) {
	    		if (this.reserveInProgress) { // once cleared, .reserveInProgress is not set again
	    			// reserve loop
	    			if (!this.reserveResourceRequests.isEmpty()) {
	    				if (this.currentFutureListOfRRD == null) {
		    				// ask next-in-line ResourceProvider to reserve the resources in reserveResourceRequests
	    					List<ResourceProvider> rps = this.resourceProviders.getProviders();
		    				if (this.nextIndexResourceProvider >= rps.size()) {
			    		        // Note: As an initial working step, proceed() is coded to a safe algorithm:
			    		        //       We expect full reserved success and resultant full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
		    					log.debug(simpleName + rps.size() + " resource providers reserved " + this.reservedResources.size() +
			                            " resources, but cannot reserve " + this.reserveResourceRequests.size() + " resources");
		    					throw new Exception("resource providers cannot reserve all resources required by template bind steps");
		    				}
		    				ResourceProvider rp = rps.get(this.nextIndexResourceProvider++);
		    				
		    				// note: each element of reserveReseourceRequests is a ResourceDescriptionImpl
		    				// initiate reserving of each resource specified by each ResourceDescription, with 6 minute timeout for each reservation
		    				this.currentFutureListOfRRD = rp.reserve(this.reserveResourceRequests, 1000*60 * 6);
		    				return; // come back later to check this future
	    				} else {
	    					try {
		    					// obtain and process the result list from our resolved future
								List<ResourceReserveDisposition> rrds = this.currentFutureListOfRRD.get();
								this.currentFutureListOfRRD = null;
								for (ResourceReserveDisposition rrd : rrds) {
				    				if (rrd.isInvalidResource())
				    					throw new Exception("invalid resource request");
				    				if (!rrd.isUnavailableResource()) {
				    					ResourceDescription inputRD = rrd.getInputResourceDescription();
				    					ReservedResource rr = new ReservedResource(inputRD.getCoordinates(), inputRD.getAttributes(), 1000*60 * 1); // 1 minute timeout; TODO: this needs to come from ResourceProvider
					    				this.currentReservedResources.add(rr);
					    				// successful reservation: remove input resource description from this.reserveResourceRequests
					    				this.reserveResourceRequests.remove(inputRD);
				    				}									
								}
				    			this.reservedResources.addAll(this.currentReservedResources); // accumulating linear list will be submitted to a final bind call
				    			continue; // initiate more reservations or possibly initiate the follow on binds
							} catch (Exception e) {
								log.debug(simpleName + "proceed() fails to reserve with exception " + e);
								throw e;
							}
	    				}
	    			} else {
	    				// move from reserving resources to binding them
	    				this.reserveInProgress = false;
	    				log.debug(simpleName + "has reserved " + this.reservedResources.size() + " resource(s)");
	    				
	    	    		// Initiate multiple parallel binds. Each individual bind is performed by one specific resource provider, the same one that reserved the resource in the first place. 
	    				this.futuresOfResourceInstances = resourceProviders.bind(this.reservedResources);
	    				//  Each list element of futuresOfResourceInstances:
	    				//      can be a null (bind failed while in the act of creating a Future), or
	    				//      can be a Future, for which future.get():
	    				//          returns a ResourceInstance on bind success
	    				//          throws an exception on bind failure
	    				return; // come back later to check this future
	    			}
	    		}
	    		
				this.resourceInstances = this.waitComplete();
				done = true;
    		} //end while()
		} catch (Exception e) {
			done = true; // as a just in case, we set this on exception, even though throwing e is thought to close out the entire test run
			throw e;
		}
    }

	/**
	 * thread blocks
	 */
	public List<ResourceInstance> waitComplete() throws Exception {
		// At this moment, this.futuresOfResourceInstances is filled. It's Future's each give us a bound ResourceInstance. Our API is to return a list of each of these extracted ResourceInstance's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).
		
		// if we are called, this.futuresOfResourceInstances should have one or more entries
		if (this.futuresOfResourceInstances.isEmpty())
			throw new Exception("BindHandler.waitComplete() called with empty ResourceInstance futures list");

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
		
		// Gather all the bound resource instances from futuresOfResourceInstances, a list of Futures. This thread yields, in waiting for each of the multiple asynch bind calls to complete.
		Exception exception = null;
        List<ResourceInstance> retList = new ArrayList<>();
        boolean allBindsSucceeded = true;

        for (Future<? extends ResourceInstance> future : this.futuresOfResourceInstances) {
            if (future != null) { // null: bind failed early so move along; do not add to retList
                try {
                    ResourceInstance resourceInstance = future.get(); // blocks until asynch answer comes, or exception, at which time the target thread has completed and is gone
                    retList.add(resourceInstance);
                    
                    // retrieve this resource's step reference, then use it to establish a lookup to resourceInstance, in hash map referenceToResourceInstance, held in iT
                    int stepReference = iT.getStepReference(resourceInstance.getCoordinates());
                    iT.markResourceInstance(stepReference, resourceInstance);
                } catch (InterruptedException ie) {
                    allBindsSucceeded = false;
                    exception = ie;
                    log.warn(simpleName + "waitComplete(), bind failed: ", ie);
                } catch (ExecutionException ee) {
                    allBindsSucceeded = false;
                    exception = ee; // I have seen FatalServerException
                    String msg = ee.getLocalizedMessage();
                    Throwable t = ee.getCause();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                    log.warn(simpleName + "waitComplete(), bind failed: " + msg, ee);
                } catch (Exception e) {
                    // can happen with things like null pointer exception
                    exception = e;
                    allBindsSucceeded = false;
                }
            } else {
                allBindsSucceeded = false;
            }
        }

        if (!allBindsSucceeded) {
            // because of this error, retList is not returned; for expected cleanup, caller can find all the ResourceInstances in iT.mapStepReferenceToResourceInstance

        if (true) { // false; temporarily allow code to proceed without an actual resource
                if (exception != null)
                    throw new Exception(exception);
                throw new Exception("bind attempt could not create and return a Future");
        	}    
        }
        
        return retList;
        // We return a list of ResourceInstance's, from which we could determine resource type for each element.
        // But at this point, we don't know what each list element will be used for, and so we cannot check that it has the right type.
        // We check this later, when each ResourceInstance is used as the object of an action- deploy, inspect, etc.
	}
	
}

//  boolean allBound = (retList.size() == this.futuresOfResourceInstances.size()); // this catches the case of future==null
//  if (!allBound) {
//  	// release all resources that did bind
//  	for (ResourceInstance ri: retList) {
//          ResourceCoordinates rc = ri.getCoordinates();
//          ResourcesManager rm = rc.getManager();
//          rm.release(rc.templateId, rc.resourceId, false);
//  	}
//  	throw new Exception(exception);
//  }