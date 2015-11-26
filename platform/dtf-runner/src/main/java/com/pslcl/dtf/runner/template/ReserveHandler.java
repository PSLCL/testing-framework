package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;

/**
 * Identifies consecutive bind steps and processes them in parallel
 */
public class ReserveHandler {
	private InstancedTemplate iT;
	private List<String> setSteps;
	private int iBeginSetOffset = -1;
	private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin...

	/**
	 * Constructor: Identify consecutive bind steps in a set of steps
	 * @param iT
	 * @param setSteps
	 */
	public ReserveHandler(InstancedTemplate iT, List<String> setSteps, int iBeginSetOffset) throws NumberFormatException {
		this.iT = iT;
		this.setSteps = setSteps;
		
		for (int i=iBeginSetOffset; i<setSteps.size(); i++) {
			SetStep setStep = new SetStep(setSteps.get(i));
			if (!setStep.getCommand().equals("bind"))
				break;
			if (this.iBeginSetOffset == -1)
				this.iBeginSetOffset = iBeginSetOffset;
			this.iFinalSetOffset = i;
		}
	}
	
    /**
     * Parse consecutive bind steps to form a list of ResourceDescription's.
     * Adjusts internal offset to match.
     * 
     * @return
     */
    List<ResourceDescription> computeReserveRequests(int currentStepReference, String templateId, long runId) throws Exception {
        List<ResourceDescription> retList = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
            	String bindStep = setSteps.get(i);
            	int bindStepReference = currentStepReference + i;
            	SetStep parsedSetStep = new SetStep(bindStep); // setID bind resourceName attributeKVPs 
            	                                               // 6 bind machine amzn-size=m3medium&jre=1.7 
            	System.out.println("BindHandler.computeResourceQuery() finds bind step reference " + bindStepReference + " in stepSet " + parsedSetStep.getSetID() + ": " + bindStep);
            	
            	String resourceName = null;
            	String strAttributeKVPs = null;
            	try {
            		resourceName = parsedSetStep.getParameter(0);
					strAttributeKVPs = parsedSetStep.getParameter(1);
				} catch (IndexOutOfBoundsException e) {
					if (resourceName == null) {
                        LoggerFactory.getLogger(getClass()).debug(ReserveHandler.class.getSimpleName() + " bind step does not specify a resource");
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
            	
                ResourceCoordinates coord = new ResourceCoordinates(templateId, ResourceDescription.resourceIdMaster.incrementAndGet(), runId);
                this.iT.markStepReference(coord, bindStepReference);
                ResourceDescription rd = new ResourceDescImpl(resourceName, coord, attributeMap);
                retList.add(rd); // or retList.add(i, rd);            	
            }
        	
        }
        return retList;
    }

}