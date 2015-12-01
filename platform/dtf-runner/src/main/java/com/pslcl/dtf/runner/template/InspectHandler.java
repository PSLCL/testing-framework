package com.pslcl.dtf.runner.template;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

public class InspectHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
    private List<Future<? extends Void>> futuresOfInspects;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...

	/**
	 * Constructor: Identify consecutive inspect steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public InspectHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
		this.iT = iT;
		this.setSteps = setSteps;
		this.futuresOfInspects = new ArrayList<Future<? extends Void>>();
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("inspect"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}
	
	public List<InspectInfo> computeInspectRequests() throws Exception { // setID inspect 0-based-person-ref instructionsHash [strArtifactName strArtifactHash] ...
    	List<InspectInfo> retList = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
            	String inspectStep = setSteps.get(i);
            	SetStep parsedSetStep = new SetStep(inspectStep); // setID inspect 0-based-person-ref instructionsHash [strArtifactName strArtifactHash] ...
                                                                  // 8 inspect 0 A4E1FBEBC0F8EF188E444F4C62A1265E1CCACAD5E0B826581A5F1E4FA5FE919C 
            	System.out.println("InspectHandler.computeInspectRequests() finds inspect in stepSet " + parsedSetStep.getSetID() + ": " + inspectStep);
            	int parsedSetStepParameterCount = parsedSetStep.getParameterCount();
            	if (parsedSetStepParameterCount < 4) // after setID and command, must have 0-based-person-ref, instructionsHash, and at least 1 of this couple "strArtifactName strArtifactHash"
              		throw new IllegalArgumentException("inspect step does not specify person reference, artifact name, or artifact hash");
            	if (parsedSetStepParameterCount%2 != 0) { // odd parameter count means a strArtifactName is missing its associated strArtifactHash
            		throw new IllegalArgumentException("InspectHandler.computeInspectRequests() finds its final artifact name parameter is missing its required artifact hash paramenter");
            	}
            	
            	ResourceInstance resourceInstance = null;
            	String strInstructionsHash = null;
            	Map<String, String> artifacts = new HashMap<>();
        		try {
					String strPersonReference = parsedSetStep.getParameter(0);
					int personReference = Integer.valueOf(strPersonReference).intValue();
					resourceInstance = iT.getResourceInstance(personReference);
					if (resourceInstance != null)
					{
						strInstructionsHash = parsedSetStep.getParameter(1);
						for (int j=2; j<parsedSetStepParameterCount; j+=2)
							artifacts.put(parsedSetStep.getParameter(j), parsedSetStep.getParameter(j+1));
		            	retList.add(new InspectInfo(resourceInstance, strInstructionsHash, artifacts));
					} else {
	            		throw new Exception("InspectHandler.computeInspectRequests() finds non-bound person at reference " + strPersonReference);
					}
				} catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).debug(InspectHandler.class.getSimpleName() + " inspect step does not specify person reference, instruction hash, artifact name, or artifact hash");
					throw e;
				}
            }
		}
		return retList;		
	}
	
	/**
	 * 
	 * @param inspectInfos
	 * @throws Exception 
	 */
	void initiateInspect(List<InspectInfo> inspectInfos) throws Exception {
		
        // start multiple asynch inspects
		for (InspectInfo inspectInfo : inspectInfos) {
			ResourceInstance resourceInstance = inspectInfo.getResourceInstance();
			// We know that resourceInstance is a PersonInstance, because an inspect step must always direct its work to a PersonInstance.
			//     Still, check that condition to avoid problems that arise when template steps are improper. 
			if (!resourceInstance.getClass().isAssignableFrom(PersonInstance.class))
				throw new Exception("Specified inspect target is not a PersonInstance");
			PersonInstance pi = PersonInstance.class.cast(resourceInstance);
			String instructions = new String("instructions"); // TODO: use inspectInfos.getInstructionsHash(), and asynchronously go get the instructions file and fill this local variable
			
			String strContent = "content"; // TODO: use inspectInfos.getArtifacts(), and asynchronously go get the several files and fill this local variable, or directly fill variable arrayContent
			byte [] arrayContent = strContent.getBytes();
			ByteArrayInputStream bais = new ByteArrayInputStream(arrayContent);

			String archiveFilename = new String("attachments.tar.gz");

			futuresOfInspects.add(pi.inspect(instructions, bais, archiveFilename));			
		}
		// Each list element of futuresOfInspects:
		//     can be a null (inspect failed while in the act of creating a Future), or
		//     can be a Future<Void>, for which future.get():
		//        returns a Void on inspect success, or
		//        throws an exception on inspect failure
	}
	
	/**
	 * thread blocks
	 */
	public void waitComplete() throws Exception {
		// At this moment, this.futuresOfInspects is filled. It's Future's each give us a Void.
		//     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full inspect success, and otherwise we back out and release whatever other activity had been put in place, along the way.
		
		// Gather all results from futuresOfInspects, a list of Futures. This thread yields, in waiting for each of the multiple asynch inspect calls to complete.
		boolean allInspects = true;
        for (Future<? extends Void> future : this.futuresOfInspects) {
            if (future != null) {
                try {
					future.get(); // blocks until asynch answer comes, or exception, or timeout
				} catch (InterruptedException ee) {
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                    LoggerFactory.getLogger(getClass()).info(InspectHandler.class.getSimpleName() + ".waitComplete(), inspect failed: " + msg, ee);
                    allInspects = false;
				} catch (ExecutionException e) {
                    LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
                    allInspects = false;
				}
        	} else {
                allInspects = false;
        	}        	
        }
        
        if (!allInspects) {
        	throw new Exception("InspectHandler.waitComplete() finds one or more inspect steps failed");
        }  
	}
	
}