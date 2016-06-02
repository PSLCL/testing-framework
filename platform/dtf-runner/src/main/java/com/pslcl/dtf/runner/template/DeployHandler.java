package com.pslcl.dtf.runner.template;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

public class DeployHandler {
	
	/**
	 * 
	 */
	static void delete(List<DeployInfo> deployInfos) {
    	for (DeployInfo di : deployInfos) {
    		MachineInstance mi = MachineInstance.class.cast(di.getResourceInstance());
    		String filename = di.getFilename();
   
    		// TODO: treat this as asynch, and handle all of them in parallel
    		Future<Void> future;
			try {
				future = mi.delete(filename);
				future.get();
			} catch (Exception e) {
    			LoggerFactory.getLogger("DeployHandler").warn(".delete() fails for filename " + filename + ", with msg: " + e.getMessage());
			}	
    	}
	}

	
	// instance members
	
	private InstancedTemplate iT;
	private List<String> setSteps;
    private List<DeployInfo> deployInfos = null;
    private List<DeployInfo> futuresOfDeploys;
    private StepSetOffsets stepSetOffsets; 
    private boolean done;
    private final Logger log;
    private final String simpleName;

    /**
     * Constructor: Identify consecutive deploy steps in a set of steps
     * @param iT The InstancedTemplate
     * @param setSteps List of steps in the step set
     * @param initialSetStepCount The offset of the first deploy step in the steps of @param setSteps
     */
	public DeployHandler(InstancedTemplate iT, List<String> setSteps, int initialSetStepCount) {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.futuresOfDeploys = new ArrayList<DeployInfo>();
		this.done = false;
        this.stepSetOffsets = new StepSetOffsets("deploy", setSteps, initialSetStepCount);
	}
	
	/**
	 * 
	 * @return
	 */
	List<DeployInfo> getDeployInfos() {
		return this.deployInfos;
	}
	
    /**
     * 
     * @return
     * @throws Exception
     */
    int getDeployRequestCount() throws Exception {
        if (this.deployInfos != null) {
            int retCount = this.deployInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("DeployHandler unexpectedly finds no deploy requests");
    }

    /**
     * 
     * @return true if done; false if not done
     */
	public boolean isDone() {
		return done;
	}
	
	/**
	 * 
	 * @return
	 */
	int computeDeployRequests() throws Exception {
    	this.deployInfos = new ArrayList<>();
        int beginSetOffset = this.stepSetOffsets.getBeginSetOffset();
        if (beginSetOffset >= 0) {
            for (int i=beginSetOffset; i<=this.stepSetOffsets.getFinalSetOffset(); i++) {
            	try {
                	String deployStep = setSteps.get(i);
                	SetStep parsedSetStep = new SetStep(deployStep); // setID deploy 0-based-machine-ref artifact-name artifactHash
                                                                     // 7 deploy 0 lib%2Fslf4j-api-1.7.6.jar A4E1FBEBC0F8EF188E444F4C62A1265E1CCACAD5E0B826581A5F1E4FA5FE919C
                    log.warn(simpleName + "computeDeployRequests() finds deploy in stepSet " + parsedSetStep.getSetID() + ": " + deployStep);
                    
                	ResourceInstance resourceInstance = null;
                	String strArtifactName = null;
                	String strArtifactHash = null;
                	
                	String strMachineReference = parsedSetStep.getParameter(0);
	            	resourceInstance = iT.getResourceInstance(strMachineReference);
	            	if (resourceInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for deploy, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("DeployHandler processing asked to deploy to non 'machine' resource");
	            		
	            		strArtifactName = parsedSetStep.getParameter(1);
	            		strArtifactHash = parsedSetStep.getParameter(2);
	                	this.deployInfos.add(new DeployInfo(resourceInstance, strArtifactName, strArtifactHash));
	            	} else {
	            		throw new Exception("DeployHandler.computeDeployRequests() finds null bound ResourceInstance at reference " + strMachineReference);
	            	}
				} catch (IndexOutOfBoundsException e) {
                    log.warn(simpleName + "deploy step does not specify machine reference, artifact name, or artifact hash");
                    done = true;
					throw e;
				}
            }
		}
        return this.deployInfos.size();
	}
	
    /**
     * Proceed to obtain inspect instructions and files to inspect, then issue inspect command(s), as far as possible, then return. Set done only when inspects complete or error out.
     *
     * @throws Exception
     */
    void proceed() throws Exception {
        if (this.deployInfos==null || this.deployInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("DeployHandler processing has no deployInfo");
        }
        
        try {
			while (!done) {
				if (this.futuresOfDeploys.isEmpty()) {
	    			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
					//     initiate multiple asynch deploys, this must add to this.futuresOfDeploys: it will because this.deployInfos is NOT empty
					for (DeployInfo deployInfo : this.deployInfos) {
						ResourceInstance resourceInstance = deployInfo.getResourceInstance();
						// We know resourceInstance is a MachineInstance, because a deploy step must never direct its work to anything except a MachineInstance.
						//     Still, check that condition to avoid problems that arise when template steps are improper. 
						if (!MachineInstance.class.isAssignableFrom(resourceInstance.getClass()))
							throw new Exception("Specified deploy target is not a MachineInstance"); // futuresOfDeploys may have entries filled; at this moment they are benign
						MachineInstance mi = MachineInstance.class.cast(resourceInstance);
						URL artifactURL = this.iT.getQAPortalAccess().formArtifactHashSpecifiedURL(deployInfo.getArtifactHash());
						Future<Void> future = mi.deploy(deployInfo.getFilename(), artifactURL.toString());
						deployInfo.setFuture(future);
						futuresOfDeploys.add(deployInfo);
					}
        			return;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
				} else {
					// For each list element of futuresOfDeploys, .getFuture():
					//     can be a null (deploy failed while in the act of creating a Future), or
					//     can be a Future<Void>, for which future.get():
					//        returns a Void on deploy success, or
					//        throws an exception on deploy failure
			    	
                    // complete work by blocking until all the futures complete
			        this.waitComplete();
			        this.done = true;
				}
			} // end while(!done)
		} catch (Exception e) {
			this.done = true;
			throw e;
		}
    }
	
	/**
	 * Wait for all deploys to complete, for the single setID that is being processed.
	 * Mark deploy errors in the returned DeployInfo list
	 * 
	 * Note: Thread blocks
	 * Note: No checked exceptions are thrown. The caller must catch Exception to handle unchecked exceptions
	 * @return The list of completed DeployInfo objects
	 * @throws Exception on any error
	 */
	public List<DeployInfo> waitComplete() throws Exception {
		try {
			// At this moment, this.futuresOfDeploys is filled. Each of its objects has a future member; each will give us a Void.
			//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

			// Note: As an initial plan, this block is coded to a safe algorithm:
			//       We expect full deploy success, and otherwise we back out and release whatever other activity had been put in place, along the way.
			
			// Gather all results from futuresOfDeploys, a list of DeployInfo, each holding a Future. This thread yields, in waiting for each of the multiple asynch deploy calls to complete.
			DeployInfo.markAllDeployedSuccess_true(); // this required setup allow negation by any pass through the loop 
			for (DeployInfo deployInfo: this.futuresOfDeploys) {
				Future<Void> future = deployInfo.getFuture();
			    if (future != null) {
			        try {
						future.get(); // blocks until asynch answer comes, or exception, or timeout
					} catch (InterruptedException | ExecutionException ioreE) {
			            Throwable t = ioreE.getCause();
			            String msg = ioreE.getLocalizedMessage();
			            if(t != null)
			                msg = t.getLocalizedMessage();
			            log.debug(simpleName + "waitComplete(), deploy failed future.get() with computed msg: " + msg + "; original msg: " + ioreE.getMessage());
			        	deployInfo.markDeployFailed(); // marks allDeployedSuccess as false, also
			        	throw ioreE;
	                } catch (Exception e) {
	                    // can happen with things like null pointer exception
			            log.debug(simpleName + "waitComplete(), deploy failed future.get() with msg: " + e.getMessage());
			        	deployInfo.markDeployFailed(); // marks allDeployedSuccess as false, also
			            throw e;
					}
			    } else {
		            log.debug(simpleName + "waitComplete(), deploy failed- future returned as null");
			    	deployInfo.markDeployFailed(); // marks allDeployedSuccess as false, also
			    	// stay in loop to gather other futures
			    }
			}
		} catch (Exception e) {
			throw e;
		}
        return this.futuresOfDeploys;
	}

}