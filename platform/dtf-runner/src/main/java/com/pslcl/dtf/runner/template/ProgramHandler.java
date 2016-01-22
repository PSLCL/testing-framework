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

public class ProgramHandler {
	private enum RunType {
		CONFIGURE(0), RUN(1), START(2);
		private int value;
		
		private RunType(int value) {
			this.value = value;
		}
	}
	
	private InstancedTemplate iT;
	private List<String> setSteps;
    private RunType runType;
	private List<ProgramInfo> programInfos = null;
    private List<ProgramState> futuresOfProgramState;
    private boolean done = false;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
    private final Logger log;
    private final String simpleName;

	/**
	 * Constructor: Identify consecutive program steps [configure or run or start] in a set of steps
	 * 
	 * @param iT
	 * @param setSteps List of steps to process. Must not be null. Must not be empty. First step must be [configure or run or start] 
	 */
	public ProgramHandler(InstancedTemplate iT, List<String> setSteps) throws IllegalArgumentException, NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		this.iT = iT;
		this.setSteps = setSteps;
		this.futuresOfProgramState = new ArrayList<ProgramState>();

		// get program name from 0'th step of the set: configure or run or start
		SetStep setStep = new SetStep(setSteps.get(0));
		String programString = setStep.getCommand();
		if (programString.equals("configure"))
			this.runType = RunType.CONFIGURE;
		else if (programString.equals("run"))
			this.runType = RunType.RUN;
		else if (programString.equals("start"))
			this.runType = RunType.START;
		else
		   throw new IllegalArgumentException();
		
		int iTempFinalSetOffset = 0;
		while (true) {
			if (!setStep.getCommand().equals(programString))
				break;
			this.iBeginSetOffset = 0;
			this.iFinalSetOffset = iTempFinalSetOffset++;
			if (this.iBeginSetOffset >= setSteps.size()-1)
				break;
			setStep = new SetStep(setSteps.get(++this.iBeginSetOffset));
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
    int getConsecutiveSameStepCount() throws Exception {
        if (this.programInfos != null) {
            int retCount = this.programInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("ProgramHandler unexpectedly finds no program steps [configure or run or start]");
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    int computeProgramRequests() throws Exception {
    	this.programInfos = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
            	String programStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(programStep);   // setID [configure | run | start] 0-based-machine-ref program-name [param param ...]
                                                                    // 10 [configure | run | start] 0 executableName -providerMode -verbose
            	log.debug(simpleName + "computeProgramRequests() finds program step in stepSet " + parsedSetStep.getSetID() + ": " + programStep);
            	
            	ResourceInstance resourceInstance = null;
            	String strProgramName = null;
            	List<String> parameters = new ArrayList<String>();
        		try {
					String strMachineReference = parsedSetStep.getParameter(0);
					int machineReference = Integer.valueOf(strMachineReference).intValue();
					resourceInstance = iT.getResourceInstance(machineReference);
					if (resourceInstance != null) {
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for [configure | run | start], check resourceInstance for required type: machine
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.MachineName)
	            			throw new Exception("ProgramHandler processing asked to run a program on a non 'machine' resource");	
	            		strProgramName = parsedSetStep.getParameter(1);
	            		for (int j=2; j<(parsedSetStep.getParameterCount()); j++)
	            			parameters.add(parsedSetStep.getParameter(j));
	                	this.programInfos.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("ProgramHandler.computeProgramRequests() finds null ResourceInstance at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
                	log.error(simpleName + "program step does not specify machine reference or program name");
					throw e;
				}
            }
		}
        return this.programInfos.size();
	}
	
    /**
     * Proceed to apply this.programInfos to bound machines, then return. Set done when the programs, of this set, complete or error out.
     *
     * @throws Exception
     */
    List<ProgramState> proceed() throws Exception {
        if (this.programInfos==null || this.programInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("ProgramHandler processing has no programInfo");
        }
        
        List<ProgramState> retList = new ArrayList<ProgramState>();
        try {
            while (!done) {
				if (this.futuresOfProgramState.isEmpty()) {
	    			// The pattern is that this first work, accomplished at the firstopst .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
					//     initiate multiple asynch [configure | run | start]'s, this must add to this.futuresOfProgramState: it will because this.programInfos is NOT empty
					for (ProgramInfo programInfo : programInfos) {
						try {
							MachineInstance mi = programInfo.computeProgramRunInformation();
							String programCommandLine = programInfo.getComputedCommandLine();
							log.debug(this.simpleName + "for run type " + this.runType + ", proceed() submits program command line: " + programCommandLine);
							
							if (this.runType == RunType.CONFIGURE) {
								Future<Integer> future = mi.configure(programCommandLine);
								futuresOfProgramState.add(new ProgramState(future, mi));
							} else if (this.runType == RunType.RUN)	{
								Future<RunnableProgram> future = mi.run(programCommandLine);
								futuresOfProgramState.add(new ProgramState(future, mi, 0));
							} else if (this.runType == RunType.START) {
								Future<RunnableProgram> future = mi.start(programCommandLine);
								futuresOfProgramState.add(new ProgramState(future, mi, 0));
							} else {
								// impossible to be here, but log it anyway
								log.error(this.simpleName + "run type not [configure or run or start]");
							}
						} catch (Exception e) {
				            log.warn(simpleName + "proceed(), program failed with exception: " + e.getMessage());
							throw e;
						}
					}
					// this return is for the 1st caller, who can ignore this empty list
        			return retList;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
				} else {
					// For each list element of futuresOfProgramState, .getFuture():
					//     can be null ([configure | run | start] failed while in the act of creating a Future), or
					//     can be a Future<Integer>, for which future.get():
					//        returns an Integer from configure completion (the Integer is the run result of the configuring program, or
					//        throws an exception from configure error-out (e.g. the configuring program did not properly run)
					//     can be a Future<RunnableProgram>, for which future.get():
					//        returns a RunnableProgram from run or start completion, or
					//        throws an exception from run or start error-out (e.g. the program did not properly run)
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
		// At this moment, this.futuresOfProgramState is filled. Its embedded Future's each give us an Integer, or a RunnableProgram. Our API is with these extracted objects.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full program success, and otherwise we back out.
		//       At the time that the governing template is eventually reused, the runs on other machines will be cleaned up.
		
		// Gather all results from futuresOfProgramState, a list of objects with embedded Future's. This thread blocks, in waiting for each of the multiple asynch [configure | run | start] calls to complete.
		List<ProgramState> retList = new ArrayList<>();
        for (ProgramState programState : this.futuresOfProgramState) {
        	if (programState!=null && programState.getFutureProgramRunResult()!=null) {
                try {
                	if (this.runType == RunType.CONFIGURE) {
						Integer runResult = programState.getFutureProgramRunResult().get(); // blocks until asynch answer comes, or exception, or timeout
						programState.setProgramRunResult(runResult); // pass this Integer result back to caller
                	} else {
                		RunnableProgram runnableProgram = programState.getFutureRunnableProgram().get();
                		programState.setRunnableProgram(runnableProgram);
                	}
					retList.add(programState);
				} catch (InterruptedException | ExecutionException ioreE) {
		            Throwable t = ioreE.getCause();
		            String msg = ioreE.getLocalizedMessage();
		            if(t != null)
		                msg = t.getLocalizedMessage();
		            log.warn(simpleName + "waitComplete(), " + this.runType + " program errored out: " + msg + "; " + ioreE.getMessage());
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