package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;

public class RunHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
    private List<RunState> futuresOfRunState = null;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
	
	/**
	 * Constructor: Identify consecutive run steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public RunHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
		this.iT = iT;
		this.setSteps = setSteps;
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("run"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}

	public List<ProgramInfo> computeRunRequests() throws Exception {
    	List<ProgramInfo> retList = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
                String runStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(runStep); // setID run 0-based-machine-ref program-name [param param ...]
                                                              // 11 start 0 executableName -providerMode -verbose
            	System.out.println("RunHandler.computeRunRequests() finds run in stepSet " + parsedSetStep.getSetID() + ": " + runStep);
            	
            	ResourceInstance resourceInstance = null;
            	String strProgramName = null;
            	List<String> parameters = new ArrayList<String>();
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					resourceInstance = iT.getResourceInstance(machineReference);
					if (resourceInstance == null) {
	            		strProgramName = parsedSetStep.getParameter(1);
	            		for (int j=0; j<(parsedSetStep.getParameterCount()-2); j++)
	            			parameters.add(parsedSetStep.getParameter(i));
	                	retList.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("RunHandler.computeRunRequests() finds non-bound machine at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
                    LoggerFactory.getLogger(getClass()).debug(RunHandler.class.getSimpleName() + " run step does not specify machine reference or program name");
					throw e;
				}
            }
		}
		return retList;
	}
	
	void initiateRun(List<ProgramInfo> programInfos) throws Exception {
        // start multiple asynch runs
		this.futuresOfRunState = new ArrayList<RunState>();
		RunState.setAllProgramsRan(true); // initialize this to be useful while we process steps of our current setID
		for (ProgramInfo programInfo : programInfos) {
			ResourceInstance resourceInstance = programInfo.getResourceInstance();
			// We know that resourceInstance is a MachineInstance, because a run step always directs its work to MachineInstance.
			//     Still, check that condition to avoid problems that arise when template steps are improper. 
			if (!resourceInstance.getClass().isAssignableFrom(MachineInstance.class)) {
				RunState.setAllProgramsRan(false);
				throw new Exception("Specified program target is not a MachineInstance");
			}
			MachineInstance mi = MachineInstance.class.cast(resourceInstance);
			String runProgramCommandLine = programInfo.getProgramName();
			List<String> params = programInfo.getParameters();
			for (String param: params) {
				runProgramCommandLine += ' ';
				runProgramCommandLine += param;
			}
			Future<RunnableProgram> future = mi.run(runProgramCommandLine);
			// future:
			//     can be a null (run failed while in the act of creating a Future), or
			//     can be a Future<RunnableProgram>, for which an eventual call to future.get():
			//        returns a RunnableProgram from run completion (the RunnableProgram refers to a program that was run and has now completed running and has returned a result), or
			//        throws an exception from run error-out (e.g. the program did not actually run or did not properly run)
			
			futuresOfRunState.add(new RunState(future, mi));
		}
    }	

	/**
	 * thread blocks
	 */
	public List<RunState> waitComplete() throws Exception {
		// At this moment, this.futuresOfRunState is filled. Its embedded Future's each give us a RunnableProgram object. Our API is with each of these extracted RunnableProgram's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full run success, and otherwise we back out and clean up whatever other run(s) had been put in place, along the way.
		
		// Gather all results from futuresOfRunState, a list of objects with embedded Future's. This thread yields, in waiting for each of the multiple asynch start calls to complete.
		Exception exception = null;
		List<RunState> retList = new ArrayList<>();
        for (RunState runState : this.futuresOfRunState) {
        	if (runState != null) {
                try {
					RunnableProgram runnableProgram = runState.getFutureRunnableProgram().get(); // blocks until asynch answer comes, or exception, or timeout
					runState.setRunnableProgram(runnableProgram);
					retList.add(runState);
				} catch (InterruptedException ee) {
					RunState.setAllProgramsRan(false);
					exception = ee;
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                    LoggerFactory.getLogger(getClass()).info(RunHandler.class.getSimpleName() + ".waitComplete(), run errored out: " + msg, ee);
				} catch (ExecutionException e) {
					RunState.setAllProgramsRan(false);
					exception = e;
                    LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
				}
        	} else {
				RunState.setAllProgramsRan(false);
        	}
        }
        // each entry in retList is a run program that actually ran and returned a result
        // retList contains information, for each run program that ran: the RunnableProgram object and the MachineInstance that ran the start program
        
        // TODO: check, for each run program that did not run, that we have cleaned up things that were put in place 
        
        return retList;
	}
	
}