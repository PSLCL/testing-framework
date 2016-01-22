package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

public class ConfigureHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
	private List<ProgramInfo> configureInfos = null;
    private List<ProgramState> futuresOfProgramState;
    private boolean done = false;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
    private final Logger log;
    private final String simpleName;

	/**
	 * Constructor: Identify consecutive configure steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public ConfigureHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.futuresOfProgramState = new ArrayList<ProgramState>();
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("configure"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}
	
    /**
     * 
     * @return
     */
	public boolean isDone() {
		return done;
	}
	
    /**
     * 
     * @return
     * @throws Exception
     */
    int getConfigureRequestCount() throws Exception {
        if (this.configureInfos != null) {
            int retCount = this.configureInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("ConfigureHandler unexpectedly finds no configure requests");
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    int computeConfigureRequests() throws Exception {
    	this.configureInfos = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
            	String configureStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(configureStep); // setID configure 0-based-machine-ref program-name [param param ...]
                                                                    // 10 configure 0 executableName -providerMode -verbose
            	log.debug(simpleName + "computeConfigureRequests() finds configure in stepSet " + parsedSetStep.getSetID() + ": " + configureStep);
            	
            	ResourceInstance resourceInstance = null;
            	String strProgramName = null;
            	List<String> parameters = new ArrayList<String>();
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					resourceInstance = iT.getResourceInstance(machineReference);
					if (resourceInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for configure, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("ConfigureHandler processing asked to run a configure program on a non 'machine' resource");	
	            		strProgramName = parsedSetStep.getParameter(1);
	            		for (int j=2; j<(parsedSetStep.getParameterCount()); j++)
	            			parameters.add(parsedSetStep.getParameter(j));
	                	this.configureInfos.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("ConfigureHandler.computeConfigureRequests() finds null ResourceInstance at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
                	log.error(simpleName + "configure step does not specify machine reference or program name");
					throw e;
				}
            }
		}
        return this.configureInfos.size();
	}
	
    /**
     * Proceed to apply this.configureInfos to bound machines, then return. Set done when configures complete or error out.
     *
     * @throws Exception
     */
    List<ProgramState> proceed() throws Exception {
        if (this.configureInfos==null || this.configureInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("ConfigureHandler processing has no configureInfo");
        }
        
        List<ProgramState> retList = new ArrayList<ProgramState>();
        try {
            while (!done) {
				if (this.futuresOfProgramState.isEmpty()) {
	    			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
					//     initiate multiple asynch configures, this must add to this.futuresOfProgramState: it will because this.configureInfos is NOT empty
					for (ProgramInfo configureInfo : configureInfos) {
						try {
							MachineInstance mi = configureInfo.computeProgramRunInformation();
							String configureProgramCommandLine = configureInfo.getComputedCommandLine();
							log.debug(this.simpleName + "proceed() submits program command line: " + configureProgramCommandLine);
							Future<Integer> future = mi.configure(configureProgramCommandLine);
							futuresOfProgramState.add(new ProgramState(future, mi));
						} catch (Exception e) {
				            log.warn(simpleName + "proceed(), configure failed with exception: " + e.getMessage());
							throw e;
						}
					}
					// this return is for the 1st caller, who can ignore this empty list
        			return retList;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
				} else {
					// For each list element of futuresOfProgramState, .getFuture():
					//     can be null (configure failed while in the act of creating a Future), or
					//     can be a Future<Integer>, for which future.get():
					//        returns an Integer from configure completion (the Integer is the run result of the configuring program, or
					//        throws an exception from configure error-out (e.g. the configuring program did not properly run)
			    	
                    // complete work by blocking until all the futures complete
			        retList = this.waitComplete();
			        this.done = true;				
				}
            } // end while(!done)
        } catch (Exception e) {
			this.done = true;
			throw e;
        }
        return retList;
    }
    
	/**
	 * thread blocks
	 */
	public List<ProgramState> waitComplete() throws Exception {
		// At this moment, this.futuresOfProgramState is filled. Its embedded Future's each give us an Integer. Our API is with each of these extracted Integer's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full configure success, and otherwise we back out (but we do not clean up whatever other configures had been put in place, because Configure's are deemed to pollute its machine).
		
		// Gather all results from futuresOfProgramState, a list of objects with embedded Future's. This thread blocks, in waiting for each of the multiple asynch configure calls to complete.
		List<ProgramState> retList = new ArrayList<>();
        for (ProgramState programState : this.futuresOfProgramState) {
        	if (programState!=null && programState.getFutureProgramRunResult()!=null) {
                try {
					Integer runResult = programState.getFutureProgramRunResult().get(); // blocks until asynch answer comes, or exception, or timeout
					programState.setProgramRunResult(runResult); // pass this Integer result back to caller
					retList.add(programState);
				} catch (InterruptedException | ExecutionException ioreE) {
		            Throwable t = ioreE.getCause();
		            String msg = ioreE.getLocalizedMessage();
		            if(t != null)
		                msg = t.getLocalizedMessage();
		            log.warn(simpleName + "waitComplete(), configure program errored out: " + msg + "; " + ioreE.getMessage());
		        	throw ioreE;
				}                

        	} else {
	            log.warn(simpleName + "waitComplete(), configure program errored out with a failed future");
        		throw new Exception("Future.get() failed");
        	}
        }
        
        // each entry in retList is a config program that actually ran; it may have returned zero or non-zero, but it ran
        // retList contains information, for each config program that ran: the run result and the MachineInstance that ran the config program
        return retList;
	}

}