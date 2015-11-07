/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;

import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * InstancedTemplate is all information about a previously instantiated template to allow it to be reused.
 *
 */
public class InstancedTemplate {

    /**
     * 
     * @param dbTemplate
     * @return
     */
	static InstancedTemplate createInstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) {
    	InstancedTemplate iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine); // sets internal StepsParser object and uses it to run steps
    	iT.runSteps();
    	return iT;
    }
	
	private RunEntryCore reCore;
	private DBTemplate dbTemplate;
	private RunnerMachine runnerMachine;
	
	private StepsParser stepsParser;
    private List<InstancedTemplate> listNestedIT;
    private final List<ResourceInstance> refToResource; // contains step reference number
    private List<ResourceInfo> orderedResourceInfos = null;
    
    InstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) {
    	this.reCore = reCore;
    	this.dbTemplate = dbTemplate;
    	this.runnerMachine = runnerMachine;
        this.listNestedIT = new ArrayList<InstancedTemplate>();
        this.refToResource = new ArrayList<ResourceInstance>();
    }
    
    public StepsParser getStepsParser() {
    	return stepsParser;
    }
    
    public void setOrderedResourceInfos(List<ResourceInfo> orderedResourceInfos) {
        this.orderedResourceInfos = orderedResourceInfos;
    }
    
    public ResourceInfo getResourceInfo(int stepReference) {
        return (orderedResourceInfos != null) ? orderedResourceInfos.get(stepReference) : null;
    }
    
    
    public void setListNestedIT(List<InstancedTemplate> listNestedIT) {
    	this.listNestedIT = listNestedIT;
    }

    
    void runSteps() {
    	// resets internal StepsParser object and uses it to run steps
    	this.stepsParser = new StepsParser(dbTemplate.steps); // tracks steps offset internally
    	
    	// instance nested templates
        NestedTemplateHandler nth = new NestedTemplateHandler(this, runnerMachine);
        try {
			nth.instanceNestedTemplates(reCore); // blocking call
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // step sets begin; process each step set in sequential order.
        for (int setID = 0; ; setID++) {
        	String strSetID = Integer.toString(setID) + ' ';
            List<String> steps = stepsParser.getNextSteps(strSetID);
            if (steps.isEmpty())
        	    break;
            
            // current algorithm for each setID:
            //     process as supplied, in sequence; check for consecutive same steps
            for (int stepOffset=0; stepOffset<steps.size();) {
                String examineStep = steps.get(stepOffset);
                
                // or can we ask BindHandler to get past the setID?
                // get past the setID and its following space
                StepsParser.offsetToSpace(examineStep, 0);     // What can stepsParser do to advance the offset here?
                
                
                switch(examineStep) {
                case "bind":
                	BindHandler bindHandler = new BindHandler(this, stepsParser, stepOffset);
                	bindHandler.process();
                	
                	break;
                case "deploy":
                	
                	break;
                case "inspect":
                	
                	break;
                case "connect":
                	
                	break;
                case "configure":
                	
                	break;
                case "start":
                	
                	break;
                case "run":
                	
                	break;
                default:
                	// TODO: log message; note: include here must be rejected
                	break;
                }
            	
            }
            //stepsParser.getNextSpaceTerminatedSubString(step, offset);
        }    	
    }
    
}