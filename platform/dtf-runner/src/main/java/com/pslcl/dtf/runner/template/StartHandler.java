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

public class StartHandler {

	private InstancedTemplate iT;
	private List<String> setSteps;
    private List<StartState> futuresOfStartState = null;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
    private final Logger log;
    private final String simpleName;
	
	/**
	 * Constructor: Identify consecutive start steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public StartHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("start"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}

	public List<ProgramInfo> computeStartRequests() throws Exception {
    	List<ProgramInfo> retList = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
                String startStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(startStep); // setID start 0-based-machine-ref program-name [param param ...]
                                                                // 11 start 0 executableName -providerMode -verbose
				log.debug(simpleName + "computeStartRequests() finds start in stepSet " + parsedSetStep.getSetID() + ": " + startStep);
            	
            	ResourceInstance resourceInstance = null;
            	String strProgramName = null;
            	List<String> parameters = new ArrayList<String>();
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					resourceInstance = iT.getResourceInstance(machineReference);
					if (resourceInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for start, check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("StartHandler processing asked to start a program on a non 'machine' resource");							
	            		strProgramName = parsedSetStep.getParameter(1);
	            		for (int j=0; j<(parsedSetStep.getParameterCount()-2); j++)
	            			parameters.add(parsedSetStep.getParameter(i));
	                	retList.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("StartHandler.computeStartRequests() finds null ResourceInstance at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
    				log.debug(simpleName + "start step does not specify machine reference or program name");
					throw e;
				}
            }
		}
		return retList;
	}

	/**
	 * 
	 * @param startInfos
	 * @throws Exception
	 */
	void initiateStart(List<ProgramInfo> startInfos) throws Exception {
        // start multiple asynch starts (note: we expect only one start in any one step set or even in any one template, but we will process multiple starts, anyway)
		this.futuresOfStartState = new ArrayList<StartState>();
		StartState.setAllProgramsRan(true); // initialize this to be useful while we process every step of our current setID
		for (ProgramInfo startInfo : startInfos) {
			try {
				MachineInstance mi = startInfo.computeProgramRunInformation();
				String runProgramCommandLine = startInfo.getComputedCommandLine();
				Future<RunnableProgram> future = mi.start(runProgramCommandLine);
				// future:
				//     can be a null (start failed while in the act of creating a Future), or
				//     can be a Future<RunnableProgram>, for which an eventual call to future.get():
				//        returns a RunnableProgram from start completion (the RunnableProgram refers to a program that was started and now may or may not be running), or
				//        throws an exception from start error-out (e.g. the program did not properly start)
				
				futuresOfStartState.add(new StartState(future, mi));
			} catch (Exception e) {
				StartState.setAllProgramsRan(false);
				throw e;
			}			
		}
    }

	/**
	 * thread blocks
	 */
	public List<StartState> waitComplete() throws Exception {
		// At this moment, this.futuresOfStartState is filled. Its embedded Future's each give us a RunnableProgram object. Our API is with each of these extracted RunnableProgram's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full start success, and otherwise we back out and clean up whatever other start(s) had been put in place, along the way.
		
		// Gather all results from futuresOfStartState, a list of objects with embedded Future's. This thread yields, in waiting for each of the multiple asynch start calls to complete.
		Exception exception = null;
		List<StartState> retList = new ArrayList<>();
        for (StartState startState : this.futuresOfStartState) {
        	if (startState != null) {
                try {
					RunnableProgram runnableProgram = startState.getFutureRunnableProgram().get(); // blocks until asynch answer comes, or exception, or timeout
					startState.setRunnableProgram(runnableProgram);
					retList.add(startState);
				} catch (InterruptedException ee) {
					StartState.setAllProgramsRan(false);
					exception = ee;
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
    				log.debug(simpleName + "waitComplete(), start errored out: " + msg, ee);
				} catch (ExecutionException e) {
					StartState.setAllProgramsRan(false);
					exception = e;
    				log.debug(simpleName + "Executor pool shutdown"); // TODO: new msg
				}
        	} else {
				StartState.setAllProgramsRan(false);
        	}
        }
        // each entry in retList is a start program that actually ran (and may still be running)
        // retList contains information, for each start program that ran: the RunnableProgram object and the MachineInstance that ran the start program
        
        // TODO: check, for each start program that did not run, that we have cleaned up things that were put in place 
        
        return retList;
	}
	
}