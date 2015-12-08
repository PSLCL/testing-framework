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
    private List<ConfigureState> futuresOfConfigureState = null;
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
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("configure"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}

	public List<ProgramInfo> computeConfigureRequests() throws Exception {
    	List<ProgramInfo> retList = new ArrayList<>();
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
	            		for (int j=0; j<(parsedSetStep.getParameterCount()-2); j++)
	            			parameters.add(parsedSetStep.getParameter(i));
	                	retList.add(new ProgramInfo(resourceInstance, strProgramName, parameters));
					} else {
	            		throw new Exception("ConfigureHandler.computeConfigureRequests() finds null ResourceInstance at reference " + strMachineReference);
					}
    			} catch (IndexOutOfBoundsException e) {
                	log.error(simpleName + "configure step does not specify machine reference or program name");
					throw e;
				}
            }
		}
		return retList;
	}
	
	/**
	 * 
	 * @param configureInfos
	 * @throws Exception
	 */
	void initiateConfigure(List<ProgramInfo> configureInfos) throws Exception {
        // start multiple asynch configures
		this.futuresOfConfigureState = new ArrayList<ConfigureState>();
		ConfigureState.setAllProgramsRan(true); // initialize this to be useful while we process every step of our current setID
		for (ProgramInfo configureInfo : configureInfos) {
			try {
				MachineInstance mi = configureInfo.computeProgramRunInformation();
				String configureProgramCommandLine = configureInfo.getComputedCommandLine();
				Future<Integer> future = mi.configure(configureProgramCommandLine);
				// future:
				//     can be a null (configure failed while in the act of creating a Future), or
				//     can be a Future<Integer>, for which an eventual call to future.get():
				//        returns an Integer from configure completion (the Integer is the run result of the configuring program, or
				//        throws an exception from configure error-out (e.g. the configuring program did not properly run)
				
				futuresOfConfigureState.add(new ConfigureState(future, mi));
			} catch (Exception e) {
				ConfigureState.setAllProgramsRan(false);
				throw e;
			}
		}
	}
	
	/**
	 * thread blocks
	 */
	public List<ConfigureState> waitComplete() throws Exception {
		// At this moment, this.futuresOfConfigureState is filled. Its embedded Future's each give us an Integer. Our API is with each of these extracted Integer's.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full configure success, and otherwise we back out (but we do not clean up whatever other configures had been put in place, because Configure's are deemed to pollute its machine).
		
		// Gather all results from futuresOfConfigureState, a list of objects with embedded Future's. This thread yields, in waiting for each of the multiple asynch configure calls to complete.
		Exception exception = null;
		List<ConfigureState> retList = new ArrayList<>();
        for (ConfigureState configureState : this.futuresOfConfigureState) {
        	if (configureState!=null && configureState.getFutureProgramRunResult()!=null) {
                try {
					Integer runResult = configureState.getFutureProgramRunResult().get(); // blocks until asynch answer comes, or exception, or timeout
					configureState.setProgramRunResult(runResult); // pass this Integer result back to caller
					retList.add(configureState);
				} catch (InterruptedException ee) {
					ConfigureState.setAllProgramsRan(false);
					exception = ee;
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                	log.debug(simpleName + "waitComplete(), configure errored out: " + msg, ee);
				} catch (ExecutionException e) {
					ConfigureState.setAllProgramsRan(false);
					exception = e;
                	log.debug(simpleName + "waitComplete() Executor pool shutdown"); // TODO: new msg
				}
        	} else {
				ConfigureState.setAllProgramsRan(false);
        	}
        }
        // each entry in retList is a config program that actually ran; it may have returned zero or non-zero, but it ran
        // retList contains information, for each config program that ran: the run result and the MachineInstance that ran the config program
        
        // TODO: check, for each config program that did not run, that we have cleaned up things that were put in place 
        
        return retList;
	}

}