package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

public class RunHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
	private boolean done;
	private List<ProgramInfo> runInfos = null;
    private List<ProgramState> futuresOfProgramState = null;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
    private final Logger log;
    private final String simpleName;
	
	/**
	 * Constructor: Identify consecutive run steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public RunHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
		this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.futuresOfProgramState = new ArrayList<ProgramState>();
		this.done = false;
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("run"))
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
	boolean isDone() {
		return done;
	}
	
    /**
     * 
     * @return
     * @throws Exception
     */
    int getRunRequestCount() throws Exception {
        if (this.runInfos != null) {
            int retCount = this.runInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("RunHandler unexpectedly finds no run requests");
    }
	
	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	int computeRunRequests() throws Exception {
    	this.runInfos = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
                String runStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(runStep); // setID run 0-based-machine-ref program-name [param param ...]
                                                              // 11 start 0 executableName -providerMode -verbose
                log.debug(simpleName + "computeRunRequests() finds run in stepSet " + parsedSetStep.getSetID() + ": " + runStep);
            	
            	ResourceInstance resourceInstance = null;
            	String strProgramName = null;
            	List<String> parameters = new ArrayList<String>();
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					resourceInstance = iT.getResourceInstance(machineReference);
					if (resourceInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for run, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("RunHandler processing asked to run a program on a non 'machine' resource");
	            		strProgramName = parsedSetStep.getParameter(1);
	            		for (int j=2; j<(parsedSetStep.getParameterCount()); j++)
	            			parameters.add(parsedSetStep.getParameter(j));
	                	this.runInfos.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("RunHandler.computeRunRequests() finds null ResourceInstance at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
                    log.debug(simpleName + "run step does not specify machine reference or program name");
					throw e;
				}
            }
		}
		return this.runInfos.size();
	}
	
    /**
     * Proceed to apply this.runInfos to bound machines, then return. Set done when the runs complete or error out.
     *
     * @throws Exception
     */
    List<ProgramState> proceed() throws Exception {
        if (this.runInfos==null || this.runInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("ConfigureHandler processing has no configureInfo");
        }
        
        List<ProgramState> retList = new ArrayList<ProgramState>();
        try {
            while (!done) {
				if (this.futuresOfProgramState.isEmpty()) {
	    			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
					//     initiate multiple asynch runs, this must add to this.futuresOfProgramState: it will because this.runInfos is NOT empty
					for (ProgramInfo runInfo : runInfos) {
						try {
							MachineInstance mi = runInfo.computeProgramRunInformation();
							String runProgramCommandLine = runInfo.getComputedCommandLine();
							log.debug(this.simpleName + "proceed() submits program command line: " + runProgramCommandLine);
							Future<RunnableProgram> future = mi.run(runProgramCommandLine);
							futuresOfProgramState.add(new ProgramState(future, mi, 0));
						} catch (Exception e) {
				            log.warn(simpleName + "proceed(), run failed with exception: " + e.getMessage());
							throw e;
						}
					}
					// this return is for the 1st caller, who can ignore this empty list
        			return retList;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
				} else {
					// For each list element of futuresOfProgramState, .getFuture():
					//     can be null (run failed while in the act of creating a Future), or
					//     can be a Future<RunnableProgram>, for which future.get():
					//        returns a RunnableProgram object from run completion (which contains an Integer, as the run result of the running program, or
					//        throws an exception from run error-out (e.g. the run program did not properly run)
			    	
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
		// At this moment, this.futuresOfProgramState is filled. Its embedded Future's each give us a RunnableProgram object. Our API is with each of these extracted RunnableProgram's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full run success, and otherwise we back out. At the time that the governing template is eventually reused, the runs on other machines will be cleaned up.
		
		// Gather all results from futuresOfProgramState, a list of objects with embedded Future's. This thread yields, in waiting for each of the multiple asynch run calls to complete.
		List<ProgramState> retList = new ArrayList<>();
        for (ProgramState programState : this.futuresOfProgramState) {
        	if (programState!=null && programState.getFutureRunnableProgram()!=null) {
                try {
					RunnableProgram runnableProgram = programState.getFutureRunnableProgram().get(); // blocks until asynch answer comes, or exception, or timeout
					programState.setRunnableProgram(runnableProgram); // pass the result back to caller
					retList.add(programState);
				} catch (InterruptedException | ExecutionException ioreE) {
		            Throwable t = ioreE.getCause();
		            String msg = ioreE.getLocalizedMessage();
		            if(t != null)
		                msg = t.getLocalizedMessage();
		            log.warn(simpleName + "waitComplete(), run program errored out: " + msg + "; " + ioreE.getMessage());
		        	throw ioreE;
				}                

        	} else {
	            log.warn(simpleName + "waitComplete(), run progra errored out with a failed future");
        		throw new Exception("Future.get() failed");
        	}
        }
        
        // each entry in retList is a run program that actually ran; it may have returned zero or non-zero, but it ran
        // retList contains information, for each run program that ran: the run result and the MachineInstance that ran the run program
        return retList;
	}
	
}