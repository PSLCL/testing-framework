package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
	
	// class instance members, internal use only  
	
	private InstancedTemplate iT;
	private List<String> setSteps;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
	private boolean reserveInProgress;
	private final ResourceProviders resourceProviders;
	private List<ResourceProvider> listResourceProviders;
	private int nextIndexResourceProvider;
    private ResourceProvider currentRP;
	private Future<List<ResourceReserveDisposition>> currentRPFutureListOfRRD;
    private List<ReservedResource> reservedResources;
    private List<Future<? extends ResourceInstance>> futuresOfResourceInstances;

	private final Logger log;
    private final String simpleName;

    
    // class instance members, accessed externally by getter
    
    private List<ResourceInstance> resourceInstances; // internal use only
    private List<ResourceDescription> reserveResourceRequests;
    private boolean done;
    
	/**
	 * Constructor: Identify consecutive bind steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public BindHandler(InstancedTemplate iT, List<String> setSteps) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.reserveResourceRequests = null;
		this.reserveInProgress = true;
		this.done = false;
		this.resourceProviders = this.iT.getResourceProviders();
		this.listResourceProviders = this.resourceProviders.getProviders();
		this.nextIndexResourceProvider = 0;
		this.currentRP = null;
		this.currentRPFutureListOfRRD = null;
		this.reservedResources = new ArrayList<>();
		this.futuresOfResourceInstances = null;
		this.resourceInstances = new ArrayList<>();
		
		int iTempFinalSetOffset = 0;
		int iSetOffset = 0;
		while (true) {
			SetStep setStep = new SetStep(setSteps.get(iSetOffset));
			if (!setStep.getCommand().equals("bind"))
				break;
			this.iBeginSetOffset = 0;
			this.iFinalSetOffset = iTempFinalSetOffset;
			if (++iTempFinalSetOffset >= setSteps.size())
				break;
			iSetOffset = iTempFinalSetOffset; // there is another step in this set
		}
	}

	/**
	 * 
	 * @return
	 */
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
    int computeReserveRequests(int currentStepReference, String templateId, long runId) throws Exception {
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
			    	
			    	long resourceid = ResourceDescription.resourceIdMaster.incrementAndGet();
			    	
			    	if (false) // true: add randomness, temporarily
			    		resourceid += (new Random().nextInt() & 0xff);
			    	
			        ResourceCoordinates coord = new ResourceCoordinates(templateId, resourceid, runId);
			        this.iT.markStepReference(coord, bindStepReference);
			        ResourceDescription rd = new ResourceDescImpl(resourceName, // resourceName comes from the bind step, as string "machine", "person" or "network"
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
        return this.reserveResourceRequests.size();
    }
    
    /**
     * Proceed to reserve and then bind resources, as far as possible, then return. Set done only when binds complete or error out.
     * 
     * @note First, reserve resources by sequentially calling independent ResourceProvider's, with yielding. Last, make a single call to bind all reserved resources, in parallel.  
     * 
     * @throws Exception
     */
    void proceed() throws Exception {
    	try {
    		while (!done) {
	    		if (this.reserveInProgress) { // once cleared, .reserveInProgress is not set again
        			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
	    			//     reserve loop
	    			if (!this.reserveResourceRequests.isEmpty()) {
	    				if (this.currentRPFutureListOfRRD == null) {
		    				// ask next-in-line ResourceProvider to reserve the resources in reserveResourceRequests
		    				if (this.nextIndexResourceProvider >= this.listResourceProviders.size()) {
			    		        // Note: As an initial working step, proceed() is coded to a safe algorithm:
			    		        //       We expect full reserved success and resultant full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
		    					log.debug(simpleName + this.listResourceProviders.size() + " resource providers reserved " + this.reservedResources.size() +
			                            " resources, but cannot reserve " + this.reserveResourceRequests.size() + " resources");
		    					throw new Exception("resource providers cannot reserve all resources required by template bind steps");
		    				}
		    				this.currentRP = this.listResourceProviders.get(this.nextIndexResourceProvider++);
		    				
		    				// note: each element of reserveReseourceRequests is a ResourceDescriptionImpl
		    				// initiate reserving of each resource specified by each ResourceDescription, with 6 minute timeout for each reservation
		    				this.currentRPFutureListOfRRD = this.currentRP.reserve(this.reserveResourceRequests, 60 * 6);
                			return;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
	    				} else {
	    					try {
		    					// for currentRP, obtain our resolved future and process its result list
								List<ResourceReserveDisposition> rrds = this.currentRPFutureListOfRRD.get();
								this.currentRPFutureListOfRRD = null; // for next resource provider to fill
								for (ResourceReserveDisposition rrd : rrds) {
				    				if (rrd.isInvalidResource())
				    					throw new Exception("invalid resource request");
				    				// for unavailable: other resource providers will have their chance to reserve rrd's internally held inputResourceDescription (a resource reserve request) 
				    				if (!rrd.isUnavailableResource()) {
			    		            	ResourceCoordinates rc = rrd.getInputResourceDescription().getCoordinates();
			    		            	if (rc==null || rc.getManager()==null || rc.getProvider()==null)
			    		            		throw new Exception("proceed() finds one reserved resource with no templateCleanup info");
				    					
				    					// We have a reserved resource in rrd.getReservedResource(). Before using this reserved resource, check that requested resource type was answered by a resource provider of the same type.
				    					//   For example, I have seen that, when a person resource is requested to be reserved, AWSMachineProvider wrongly returns a reserved entry (visible in Eclipse as future.outcome.reserved).
				    					// Note: these next four getters return "machine" or "person" or "network"
				    					String currentRPName = this.currentRP.getName(); // .currentRP here is the same as .currentRP, just above, that filled this.currentFutureListofRRD in call this.currentRP.reserve()
				    					String rrRDName =   rrd.getInputResourceDescription().getName();
				    					String rrName =   rrd.getReservedResource().getName();
				    					String rrRPName = rrd.getReservedResource().getResourceProvider().getName();
			    		            	if (!currentRPName.equals(rrRDName) || !currentRPName.equals(rrName) || !currentRPName.equals(rrRPName)) {
			    		            		log.debug(simpleName + "proceed() finds mismatched rpName " + currentRPName + ", rrRDName " + rrRDName + ", rrName " + rrName + ", rrRPName " + rrRPName);
			    		            		
			    		            		// Notify iT that it should inform the Resource Provider system that this template is closing. InstancedTemplate iT may have past templateCleanup info, but just in case, supply it with what we have here.
			    		            		iT.informResourceProviders(rc);
			    		            		
			    		            		if (true) { // false: temporarily allow a not-requested resource type to pass through, in order to let deploys and inspects see it and error out		    		            		
			    		            			throw new Exception("proceed() finds mismatched ReservedResource.provider name and ResourceProvider names");
			    		            		}
			    		            	}
			    		            	
			    		            	// Successful reserve: set templateCleanup info
			    		            	// Note: Every successful reserve produces the same templateCleanup info (available in a ResourceCoordinates object).
			    		            	//       On eventual template destroy, a single call is used to inform the ResourceProvider system that this template no longer needs its reserved and bound resources.
			    		            	//       This block overwrites past template destroy info, so only the last encountered template destroy info remains visible to InstancedTemplate; it is sufficient to notify the ResourceProvider system.
			    		            	iT.setTemplateCleanupInfo(rc);
			    		            	
			    		            	// record this newly reserved resource to a list of ReservedResource's
				    					ResourceDescription inputRD = rrd.getInputResourceDescription();
				    					ReservedResource rr = new ReservedResource(inputRD.getCoordinates(), inputRD.getAttributes(), 1000*60 * 1); // 1 minute timeout; TODO: this needs to come from ResourceProvider
					    				this.reservedResources.add(rr);
					    				// successful reservation: remove input resource description from this.reserveResourceRequests
					    				this.reserveResourceRequests.remove(inputRD);
				    				}									
								}
				    			continue; // initiate more reservations or possibly initiate the follow-on parallel binds
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
	    				
            			return; // allow time for futures to resolve
	    			}
	    		} // end if (reserveInProgress)
	    		
                // complete work by blocking until all the futures complete
				this.waitComplete();
				done = true;
    		} //end while()
		} catch (Exception e) {
			done = true; // as a just in case, set done on exception, even though throwing e is thought to close out the entire test run

            // We will cleanup our template. The resource providers will then cleanup the resources that this template has successfully requested.
			throw e;
		}
    }

	/**
	 * thread blocks
	 */
	private void waitComplete() throws Exception {
		// At this moment, this.futuresOfResourceInstances is filled. It's Future's each give us a bound ResourceInstance. Our API is to return a list of each of these extracted ResourceInstance's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).
		
		// if we are called, this.futuresOfResourceInstances should have one or more entries
		if (this.futuresOfResourceInstances.isEmpty())
			throw new Exception("BindHandler.waitComplete() called with empty ResourceInstance futures list");

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full bind success, and otherwise we back out and release whatever reservations and binds had succeeded along the way.
		
		// Gather all the bound resource instances from futuresOfResourceInstances, a list of Futures. This thread yields, in waiting for each of the multiple asynch bind calls to complete.
		Exception exception = null;
        boolean allBindsSucceeded = true;

        for (Future<? extends ResourceInstance> future : this.futuresOfResourceInstances) {
            if (future != null) { // null: bind failed early so move along; do not add to this.resourceInstances
                try {
                    ResourceInstance resourceInstance = future.get(); // blocks until asynch answer comes, or exception, at which time the target thread has completed and is gone
                    this.resourceInstances.add(resourceInstance);
                    // We could, but don't need to, retrieve a ResourceProvider from this new bound resourceInstance. We don't check anything here, - we don't yet know what this ResourceInstance will be used for (we learn that in a follow-on step).

                    // We do not cleanup individual reserved resources. We do not cleanup individual bound resource instances. So, for this successful bind, we don't need to remove an entry from this.reservedResources 
                    
                    // retrieve this resource's step reference, then use it to mark resourceInstance, in hash map referenceToResourceInstance, held in iT
                    int stepReference = iT.getStepReference(resourceInstance.getCoordinates());
                    iT.markResourceInstance(stepReference, resourceInstance);
                    
                    log.debug(this.simpleName + "1 bind completes, with attributes of " + resourceInstance.getAttributes());
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
        	if (true) { // false; temporarily allow code to proceed without an actual resource
                // We will cleanup our template. The resource providers will cleanup the resources that we allocated for this template.
                if (exception != null)
                    throw new Exception(exception);
                throw new Exception("bind attempt could not create and return a Future");
        	}    
        }
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