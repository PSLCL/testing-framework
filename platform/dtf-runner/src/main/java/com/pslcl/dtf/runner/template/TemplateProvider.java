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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusEvent;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusListener;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

public class TemplateProvider implements ResourceStatusListener {
    
    private final Map<byte[],InstancedTemplate> availableInstancedTemplates; // note: this populates in the destroy method
    private final ResourceProviders resourceProviders;
    private volatile StatusTracker statusTracker;
    
    public TemplateProvider() 
    {
        availableInstancedTemplates = new HashMap<byte[],InstancedTemplate>();
        resourceProviders = new ResourceProviders();
    }

    public ResourceProviders getResourceProviders()
    {
        return resourceProviders;
    }
    
    public void init(RunnerConfig config) throws Exception
    {
        statusTracker = config.statusTracker;
        statusTracker.registerResourceStatusListener(this);
        resourceProviders.init(config);
    }
    
    public void destroy() 
    {
        resourceProviders.destroy();
        statusTracker.deregisterResourceStatusListener(this);
    }
    
    public void destroy(byte [] template_hash, InstancedTemplate iT) {
        // Can also choose to destroy iT. This is early impl with no smarts.
        availableInstancedTemplates.put(template_hash, iT);
    }
    
    public void releaseTemplate(InstancedTemplate iT) {
    	
    }

    /**
     * Get an instanced template. An instanced template has executed all its steps, and has recorded enough information along the way to be reused.
     *  
     * Instance a template
     *     by instantiating a new template and running its steps for the first time, or
     *     by reusing a template and running its steps again.
     *         On reuse, the steps will be executed again, but includes, binds, and start will be found in place already.
     *         Deploys and connects are also found in place, unless new target info can be specified during reuse. 
     *         Run steps will happen again at each reuse.
     * 
     * @note re-entrant
     * @param nestedStepOffset TODO
     * @param dbdt
     * @return
     */
    public InstancedTemplate getInstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
    	return getInstancedTemplate(-1, reCore, dbTemplate, runnerMachine);
    }
        
    /**
     * 
     * @param nestedStepReference
     * @param reCore
     * @param dbTemplate
     * @param runnerMachine
     * @return
     * @throws Exception
     */
    public InstancedTemplate getInstancedTemplate(int nestedStepReference, RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
        // first check our tracking- is a matching reusable template available?
        InstancedTemplate iT = availableInstancedTemplates.get(dbTemplate.hash);
        // At any one time, there can be multiple instanced templates of the same .template_hash. They differ at least in which test run (or parent template) had used each one.

        if (iT != null) {
            // we will now use iT
            availableInstancedTemplates.remove(dbTemplate.hash);
            // Note: This is early impl with no smarts to optimize anything. At this line, they asked for the instantiated template, they get it, and now it is not available to another user  
            iT.runSteps(); // resets internal StepsParser object and uses it to run steps
        } else {
        	iT = InstancedTemplate.createInstancedTemplate(reCore, dbTemplate, runnerMachine); // sets internal StepsParser object and uses it to run steps
        }
        return iT;
    }

    @Override
    public void resourceStatusChanged(ResourceStatusEvent status)
    {
//        int templateStepNumber = StepsParser.resourceToLineMap.get(status.coordinate);  
        LoggerFactory.getLogger(getClass()).debug("\n" + getClass().getSimpleName() + ".resourceStatusChanged hit: " + status.toString());
        switch(status.status)
        {
            case Error:
                break;
            case Human:
                break;
            case Ok:
                break;
            case Warn:
                break;
            default:
                break;
        }
    }

}    
    
//            // old parsing technique: temporarily here as a resource for future code --------------------------------------------------------------
    
//    /**
//     * 
//     * @param iT
//     * @param stepsParser
//     * @param stepsReference
//     */
//    private void processPostBindSteps(InstancedTemplate iT, StepsParser stepsParser, int stepsReference) {
//        // parse steps after bind
//        while (true) {
//            String step = stepsParser.getNextStep();
//            if (step == null)
//                break;
//            // process step with this rule: step command is the first set of characters, leading up to the first space char
//            int offset = 0;
//            String spaceTermSubString = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//            if (spaceTermSubString != null) {
//                offset += spaceTermSubString.length();
//                if (++offset > step.length())
//                    offset = -1; // done parsing
//            
//                switch (spaceTermSubString) {
//                case "run":
//                    // machineRef strArtifactName strOptions
//                    System.out.println("TemplateProvider.getInstancedTemplate() finds run as reference " + stepsReference);
//  
//                    // This is what actually comes in - it is missing some spaces - this is bug 7066 under app-test-platform
////                  run[]0emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE-providerMode
//                    // is this intended?
////                  run 0 emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE -providerMode
//
////                  From an email                    
////                    The ordering of a command is:
////                        1.  The program action (run, start, etc.)
////                        2.  A program declaration (resource declaration, empty for run, since the reference is not used)
////                        3.  A machine reference
////                        4.  An artifact reference.
////                        5.  A possibly empty space-separated URL-encoded set of parameters.
//                    
//
//                    // this approximates what is needed
//                    strArtifactName = null;
//                    String strRunParams = null;
//                    machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                    if (machineRef != null) {
//                        offset += machineRef.length();
//                        if (++offset > step.length())
//                            offset = -1; // done
//
//                        // gather strArtifactName
//                        strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                        if (strArtifactName != null) {
//                            offset += strArtifactName.length();
//                            if (++offset > step.length())
//                                offset = -1; // done
//                            }
//                        }
//                        
//                        strRunParams = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                        // we do not further extract from step
////                        if (strOptions != null) {
////                            offset += strOptions.length();
////                            if (++offset > step.length())
////                                offset = -1; // done
////                        }
//                        
//                    if (strArtifactName != null) {
//                        // strOptions is available; it may be null
//                        int stepRef = Integer.parseInt(machineRef);
//                        ResourceInfo resourceInfo = iT.getResourceInfo(stepRef);
//                        boolean success = false;
//                        if (resourceInfo != null) {
//                            ResourceInstance resourceInstance = resourceInfo.getResourceInstance();
//                            if (MachineInstance.class.isInstance(resourceInstance)) {
//                                // run program on machineInstance
//                                MachineInstance machineInstance = MachineInstance.class.cast(resourceInstance);
////                              try {
//                                    String command = strArtifactName + (strRunParams!=null ? (" " +strRunParams): ""); // command is an executable command, including arguments, to be run on the machine
//                                    machineInstance.run(command);
//                                    ArtifactInfo artifactInfo = new ArtifactInfo(strArtifactName, null); // null because we do not know its value
//                                    resourceInfo.setArtifactInfo(artifactInfo);
//                                    resourceInfo.setRunParams(strRunParams);
//                                    
//                                    // TODO: set iT with more gathered info and instantiations and similar
//
//                                    success = true;
//                                    System.out.println("TemplateProvider.getInstancedTemplate() runs to bound resource " + stepRef + ": artifactInfo: " + strArtifactName);
////                              }
//                            }
//                        }
//                    } else {
//                        System.out.println("TemplateProvider.getInstancedTemplate() finds run to be incompletely specified");
//                    }
//                    break;
//                default:
//                    System.out.println("TemplateProvider.getInstancedTemplate() finds something unexpected: " + spaceTermSubString + " as reference " + stepsReference);
//                    break;
//                } // end switch()
//                stepsReference++;
//            } // end if()
//        } // end while()	
//    }