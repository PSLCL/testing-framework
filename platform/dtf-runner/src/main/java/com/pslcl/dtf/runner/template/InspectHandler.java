package com.pslcl.dtf.runner.template;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.runner.QAPortalAccess;

public class InspectHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
    private List<Future<? extends Void>> futuresOfInspects;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...
    private final Logger log;
    private final String simpleName;

	/**
	 * Constructor: Identify consecutive inspect steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public InspectHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
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
            	log.debug(simpleName + "computeInspectRequests() finds inspect in stepSet " + parsedSetStep.getSetID() + ": " + inspectStep);
            	int parsedSetStepParameterCount = parsedSetStep.getParameterCount();
            	if (parsedSetStepParameterCount < 4) // after setID and command, must have 0-based-person-ref, instructionsHash, and at least this couple "strArtifactName strArtifactHash", each of which adds 2 to parameter count
              		throw new IllegalArgumentException("inspect step did not specify all needed person reference, instructionsHash, artifact name, and artifact hash");
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
	            		// Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
	            		//       Now that we know it is used for inspect, check resourceInstance for required type: person
	            		// riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
	            		ResourceProvider riRP = resourceInstance.getResourceProvider();
	            		String resourceType = ResourceProvider.getTypeName(riRP);
	            		if (resourceType==null || resourceType!=ResourceProvider.PersonName)
	            			throw new Exception("InspectHandler processing asked to deploy to non 'person' resource");
						strInstructionsHash = parsedSetStep.getParameter(1);
						for (int j=2; j<parsedSetStepParameterCount; j+=2)
							artifacts.put(parsedSetStep.getParameter(j), parsedSetStep.getParameter(j+1));
		            	retList.add(new InspectInfo(resourceInstance, strInstructionsHash, artifacts));
					} else {
	            		throw new Exception("InspectHandler.computeInspectRequests() finds null bound ResourceInstance at reference " + strPersonReference);
					}
				} catch (Exception e) {
                    log.debug(simpleName + "inspect step processing error, msg: " + e);
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
		QAPortalAccess qapa = this.iT.getQAPortalAccess();

        // start multiple asynch inspects
		for (InspectInfo inspectInfo : inspectInfos) {
			ResourceInstance resourceInstance = inspectInfo.getResourceInstance();
			// We know that resourceInstance is a PersonInstance, because an inspect step must always direct its work to a PersonInstance.
			//     Still, check that condition to avoid problems that arise when template steps are improper. 
			if (!PersonInstance.class.isAssignableFrom(resourceInstance.getClass()))
				throw new Exception("Specified inspect target is not a PersonInstance");
			PersonInstance pi = PersonInstance.class.cast(resourceInstance);
			
			String instructionsHash = inspectInfo.getInstructionsHash();
			String instructions = qapa.getContent("content", instructionsHash); // TODO: this needs to be asynchronous and gathering moved to .waitComplete()
			if (false) // true: temporarily, a cheap substitution to overcome QAPortal access problem
				instructions = new String("this is instructions");
			
			String contentHash = inspectInfo.getInstructionsHash();
			String strContent = qapa.getContent("content", contentHash); // TODO: this needs to be asynchronous and gathering moved to .waitComplete()
			if (false) // true: temporarily, a cheap substitution to overcome QAPortal access problem
				strContent = "this is content";
			byte [] arrayContent = strContent.getBytes();
			
			InputStream is = new ByteArrayInputStream(arrayContent); // ByteArrayInputStream extends InputStream
			// we own is; TODO: Does .waitComplete() clean it up?
			String archiveFilename = new String("attachments.tar.gz"); // hard coded per the design docs for PersonInstance
			Future<? extends Void> future = pi.inspect(instructions, is, archiveFilename);
			// TODO: close this Stream when the Future<Void> comes back; will have to track stream is against each future 

			futuresOfInspects.add(future);
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
                    log.debug(simpleName + "waitComplete(), inspect failed: " + msg, ee);
                    allInspects = false;
				} catch (ExecutionException e) {
                    log.info(simpleName + "Executor pool shutdown"); // TODO: need new msg
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