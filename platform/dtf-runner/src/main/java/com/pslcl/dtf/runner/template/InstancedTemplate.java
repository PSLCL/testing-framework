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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * InstancedTemplate is all information about a previously instantiated template, to maintain its operation and to allow it to be reused.
 * 
 * @note The test runner should re-use templates, when possible, but should not attempt to re-use individual resources.
 *       Instead, when a template is no longer needed, all resources should be released to their resource provider.
 *       The resource provider is responsible for individual resource reuse.
 */
public class InstancedTemplate {

    /**
     * 
     * @param dbTemplate
     * @return
     */
	static InstancedTemplate createInstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
    	InstancedTemplate iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine); // sets internal StepsParser object and uses it to run steps
    	iT.runSteps();
    	return iT;
    }
	
    private final Logger log;
    private final String simpleName;
	private RunEntryCore reCore;   // holds runID of our top-level template (might be us or we might be nested down from it)
	private DBTemplate dbTemplate; // holds templateID as a hash
	private RunnerMachine runnerMachine;
	private ResourceCoordinates templateCleanupInfo;
	
	private StepsParser stepsParser;
    private Map<Integer, InstancedTemplate> mapStepReferenceToNestedTemplate;
    private Map<ResourceCoordinates, Integer> mapResourceCoordinatesToStepReference;
    private Map<Integer, ResourceInstance> mapStepReferenceToResourceInstance;
    
    /** the purpose of holding these accumulating lists is, at least, for cleanup when this (a template) is destroyed */
    private List<ResourceInstance> boundResourceInstances;
    private List<DeployInfo> deployedInfos;
	private List<CableInstance> cableInstances;
	private List<StartState> startProgramStates;
	private List<RunState> runProgramStates;

    private List<ResourceInfo> orderedResourceInfos = null;
    
    InstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
    	this.reCore = reCore;
    	this.dbTemplate = dbTemplate;
    	this.runnerMachine = runnerMachine;
    	this.templateCleanupInfo = null;
        mapStepReferenceToNestedTemplate = new HashMap<Integer, InstancedTemplate>(); 
        mapResourceCoordinatesToStepReference = new HashMap<ResourceCoordinates, Integer>();
        mapStepReferenceToResourceInstance = new HashMap<Integer, ResourceInstance>();
        this.boundResourceInstances = new ArrayList<>();
        this.deployedInfos = new ArrayList<>();
        this.cableInstances = new ArrayList<>();
    }
    
    public ResourceProviders getResourceProviders() {
    	return this.runnerMachine.getTemplateProvider().getResourceProviders();
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
    
	public String getTemplateID() {
		return dbTemplate.getTemplateId();
	}
	
	public long getRunID() {
		return reCore.getRENum();
	}
    
    
    /**
     * 
     * @param coord
     * @param stepReference
     */
    public void markStepReference(ResourceCoordinates coord, int stepReference) {
    	mapResourceCoordinatesToStepReference.put(coord, stepReference);
    }
    
    /**
     * 
     * @param coord
     * @return
     */
    public int getStepReference(ResourceCoordinates coord) {
    	return mapResourceCoordinatesToStepReference.get(coord).intValue();
    }

    /**
     *     
     * @param stepReference
     * @param resourceInstance
     */
    public void markResourceInstance(int stepReference, ResourceInstance resourceInstance) {
    	mapStepReferenceToResourceInstance.put(stepReference, resourceInstance);
    }

    /**
     * 
     * @param stepReference
     * @return
     */
    public ResourceInstance getResourceInstance(int stepReference) {
    	return mapStepReferenceToResourceInstance.get(stepReference);
    }

    /**
     * 
     * @param stepReference
     * @param instancedTemplate
     */
    public void markNestedTemplate(int stepReference, InstancedTemplate instancedTemplate) {
    	mapStepReferenceToNestedTemplate.put(stepReference, instancedTemplate);
    }

    /**
     * 
     * @param stepReference
     * @return
     */
    public InstancedTemplate getNestedTemplate(int stepReference) {
    	return mapStepReferenceToNestedTemplate.get(stepReference);
    }
    
    
    public void destroyNestedTemplate() {
    	for (InstancedTemplate instancedTemplate : mapStepReferenceToNestedTemplate.values()) {
        	// TODO: Do actual destroy of everything having to do with each nested template    		
    	}
    	mapStepReferenceToNestedTemplate.clear();
    }
    
    /**
     * templates.md.step-ordering says that "each set of commands is sorted in increasing alphanumeric order." Interpretations:
     *     - it is the duty of the generator to place these template steps in both its desired order and in the required order
     *     - since only include steps are stipulated to begin with an alpha character, they come first, both by original intent and by alphanumeric order (when considered as alphas before numerals)
     *     - within any step set, like steps are placed contiguous to each other: e.g. bind's are together, connect's are together, etc.
     *     - within any step set, the order of step type will be: bind, configure, connect, deploy, inspect, run, and start
     *     - where steps must process in a particular order, they must be organized into different step sets
     *     - within any step set, dtf-runner may process steps in any order
     *
     * templates.md.step-ordering also says "Template instantiation will fail if any steps are encountered which cannot be parsed," and "setIDs start with 0, each set incrementing by 1." Interpretations:
	 *     - Parsing violations and problems include:
     *         - a step does not terminate with a newline (newline is required in the doc, after all; detectable only on the last step)
     *         - after include steps, a step begins with anything but a number (the setID)
     *         - after include steps, the first setID is not 0 (0 was required in the doc, after all)
     *         - a change in setID is not 1 greater than before
     * 
     * @throws Exception
     */
    void runSteps() throws Exception {
    	// resets internal StepsParser object and uses it to run steps
        this.stepsParser = new StepsParser(dbTemplate.steps); // tracks steps offset internally
    	
    	// instance nested templates
        NestedTemplateHandler nth = new NestedTemplateHandler(this, runnerMachine);
        try {
			nth.instanceNestedTemplates(reCore); // blocking call
		} catch (Exception e) {
			log.debug(simpleName + "runSteps() exception in processing a nested template, msg: " + e);
			throw e;
		}
        int currentStepReference = this.mapStepReferenceToNestedTemplate.size();

        // step sets begin (steps in the same set); process each step set in sequential order (starting at setID 0, with no missing setID)
        // any error terminates step handling
        for (int setID=0; ; setID++) {
        	String strSetID = Integer.toString(setID) + ' '; // trailing space required for a legal setID
            List<String> stepsOfSet = stepsParser.getNextSteps(strSetID); // this call consumed all steps in the set; i.e. having the same setID 
            if (stepsOfSet.isEmpty()) {
            	// Our choice: We could check that any additional steps exist. If they exist, their setID is out of sequence, the template is in error, and we can fail the test run.
            	//             Or we can ignore this check. By failing to check, and by leaving the loop here, we terminate the template at the last successful step.
        	    // Decision: Exit step processing. If there exist follow-on steps, we don't know about them.
            	break; // step handling now complete
            }
            
    		BindHandler bindHandler = null;
    		DeployHandler deployHandler = null;
    		List<DeployInfo> deployInfos = null;
    		InspectHandler inspectHandler = null; // Inspect purposely has no overall list of InspectInfo
    		ConnectHandler connectHandler = null;
    		List<ConnectInfo> connectInfos = null;
    		ConfigureHandler configureHandler = null;
    		List<ProgramInfo> configureInfos = null;
    		StartHandler startHandler = null;
    		List<ProgramInfo> startInfos = null;
    		RunHandler runHandler = null;
    		List<ProgramInfo> runInfos = null;
            // algorithm for each setID: initiate step processing in sequence, as supplied, while also allowing parallel processing; order of step completion is uncertain
            for (int setStepCount=0; setStepCount<stepsOfSet.size(); setStepCount++) {
            	// from the first step of a possible group of same-command steps, get its command and act on it
            	String commandString = null;
            	SetStep firstLikeStep = new SetStep(stepsOfSet.get(setStepCount));
            	try {
					commandString = firstLikeStep.getCommand();
					switch(commandString) {
					case "bind":
						if (bindHandler == null) {
							bindHandler = new BindHandler(this, stepsOfSet, setStepCount);
							bindHandler.computeReserveRequests(currentStepReference, this.getTemplateID(), this.getRunID());
							bindHandler.proceed();
							// bind processing has advanced and requires waiting for Future(s); notification of current progress comes to us asynchronously, below, after we have initiated the processing of other steps of this step set

							int consumedStepReferencesA = bindHandler.getReserveResourceRequestCount(); // the number of step lines processed (won't be 0), regardless of the outcome of actual reserve and bind activity
							setStepCount += consumedStepReferencesA;
							currentStepReference += consumedStepReferencesA;
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "deploy":
						// We track MachineInstance's of each deploy, even though (probably) only needed for cleaning up the case where a parent template deploys to the machine of a nested template
						if (deployHandler == null) {
							deployHandler = new DeployHandler(this, stepsOfSet, setStepCount);
							deployInfos = deployHandler.computeDeployRequests(); // setID deploy 0-based-machine-ref artifact-name artifactHash
							deployHandler.initiateDeploy(deployInfos);
							int consumedStepReferences = deployInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// deploy(s) are being processed; notification comes to us asynchronously, below, where we receive deploy notification after we have initiated the processing of other steps of this step set
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "inspect":
						if (inspectHandler == null) {
							inspectHandler = new InspectHandler(this, stepsOfSet, setStepCount);
							List<InspectInfo> inspectInfos = inspectHandler.computeInspectRequests(); // setID inspect 0-based-person-ref instructionsHash [strArtifactName strArtifactHash] ...
							inspectHandler.initiateInspect(inspectInfos);
							int consumedStepReferences = inspectInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// inspect(s) are being processed; notification comes to us asynchronously, below, where we receive inspect notification after we have initiated the processing of other steps of this step set
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "connect":
						// We track CableInstance's of each connect, even though (probably) only needed for cleaning up the case where a parent template causes connects in a nested template
						if (connectHandler == null) {
							connectHandler = new ConnectHandler(this, stepsOfSet, setStepCount);
							connectInfos = connectHandler.computeConnectRequests(); // setID connect 0-based-machine-ref 0-based-network-reference
							connectHandler.initiateConnect(connectInfos);
							int consumedStepReferences = connectInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// connect(s) are being processed; notification comes to us asynchronously, below, where we receive connect notification after we have initiated the processing of other steps of this step set
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "configure":
						if (configureHandler == null) {
							configureHandler = new ConfigureHandler(this, stepsOfSet, setStepCount);
							configureInfos = configureHandler.computeConfigureRequests(); // setID configure 0-based-machine-ref program-name [param param ...]
							configureHandler.initiateConfigure(configureInfos);
							int consumedStepReferences = configureInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// configure(s) are being processed; notification comes to us asynchronously, below, where we receive configure notification after we have initiated the processing of other steps of this step set
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "start":
						// We track MachineInstance's of each start, even though (probably) only needed for cleaning up the case where a parent template "starts" to the machine instance of a nested template
						if (startHandler == null) {
							// A generator is encouraged to divide templates into nested templates, such that a single start step appears in a template, and only in a nested template.
							// Even so, we process whatever start step arrangement comes to us.
							startHandler = new StartHandler(this, stepsOfSet, setStepCount);
							startInfos = startHandler.computeStartRequests(); // setID start 0-based-machine-ref program-name [param param ...]
							startHandler.initiateStart(startInfos);
							int consumedStepReferences = configureInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// start(s) are being processed; notification comes to us asynchronously, below, where we receive start notification after we have initiated the processing of other steps of this step set							
						} else {
							// TODO: warn log to report improper ordering of steps within a step set
						}
						break;
					case "run":
						// We track MachineInstance's of each run, even though (probably) only needed for cleaning up the case where a parent template "runs" to the machine instance of a nested template
						if (runHandler == null) {
							runHandler = new RunHandler(this, stepsOfSet, setStepCount);
							runInfos = runHandler.computeRunRequests(); // setID start 0-based-machine-ref program-name [param param ...]
							runHandler.initiateRun(runInfos);
							int consumedStepReferences = configureInfos.size();
							setStepCount += consumedStepReferences;
							currentStepReference += consumedStepReferences;
							// run(s) are being processed; notification comes to us asynchronously, below, where we receive run notification after we have initiated the processing of other steps of this step set							
						}
						break;
					default:
					    log.debug(simpleName + "unhandled step command: " + commandString + (commandString.equals("include") ? ": must precede all other commands" : ""));
						break;
					}
            	} catch (ArrayIndexOutOfBoundsException ae) {
				    log.error(simpleName + "malformed step");
				    throw ae;
				} catch (Exception e) {
				    log.debug(simpleName + "runSteps(): " + commandString + " executing step fails; exception msg: " + e);
					throw e;
				}
            } // end for() same setID command initiating
            
            do {
                // block, in turn, until all steps of this set have concluded their processing
            	if (bindHandler!=null && !bindHandler.isDone()) {
            		bindHandler.proceed();
            		if (bindHandler.isDone()) {
            			List<ResourceInstance> localRI = bindHandler.getResourceInstances();
            			this.boundResourceInstances.addAll(localRI);
            			log.debug(simpleName + "bindHandler() completes " + localRI.size() + " bind(s) for one setID");
            		}
            	}
                if (deployHandler!=null && !deployHandler.isDone()) {
    				// we track and record MachineInstance's of each deploy, even though (probably) only needed for cleaning up the case where a parent template deploys to a nested template
                	boolean allDeploysSucceeded = false;
                    try {
    					List<DeployInfo> localDeployInfos = deployHandler.waitComplete();
    					deployedInfos.addAll(localDeployInfos);
    					if (DeployInfo.getAllDeployedSuccess() && localDeployInfos.size()==deployInfos.size()) // check success further
    						allDeploysSucceeded = true;
    					else {
    						// TODO: log and notify
    					}
    				} catch (Exception e) {
    					// e is an unchecked exception; respond by throwing a new exception to cause this template run to error out
    					// TODO: log and notify
    				}

                    if (!allDeploysSucceeded) {
        				// one or more deploy steps errored out; this template is errored out
        				throw new Exception("InstancedTemplate.runSteps() deploy handling has incomplete successful deployed list, for setID " + setID);
        				// initiate cleanup/destroy of this template run; deployedInfos holds all information
                    }
                }
                if (inspectHandler != null) {
                	try {
    					inspectHandler.waitComplete();
    				} catch (Exception e) {
        				// one or more inspect steps errored out; this template is errored out
        				throw new Exception("InstancedTemplate.runSteps() inspect handling finds failed inspect, for setID " + setID);
        				// initiate destroy of this template run; we do not cleanup past inspect steps
    				}
                }
                if (connectHandler != null) {
    				// we track and record CableInstance's of each connect, even though (probably) only needed for cleaning up the case where a parent template causes connects in a nested template
                	boolean allConnectsSucceeded = false;
    				try {
    	            	List<CableInstance> localCableInstances = connectHandler.waitComplete();
    					if (ConnectInfo.getAllConnectedSuccess() && localCableInstances.size()==connectInfos.size()) {
    						allConnectsSucceeded = true;
    						cableInstances.addAll(localCableInstances);
    					} else {
    						// TODO: log and notify
    					}					
    				} catch (Exception e) {
    					// e is an unchecked exception; respond by throwing a new exception to cause this template run to error out
    					// TODO: log and notify
    				}
                	
                    if (!allConnectsSucceeded) {
        				// one or more connect steps errored out; this template is errored out
        				throw new Exception("InstancedTemplate.runSteps() connect handling has incomplete successful connect list, for setID " + setID);
        				// initiate cleanup/destroy of this template run; cableInstances holds all information
                    }
                }
                if (configureHandler != null) {
                	// success is that no exception is thrown by .waitComplete() or the asynch configure process(es) behind it
                	List<ConfigureState> localConfigureStates = configureHandler.waitComplete();
                	boolean configStepErroredOut= false;
                	for (ConfigureState cs : localConfigureStates) {
                		// TODO: Because Configures do not need to be cleaned up, storing MachineInstance is not needed
                		// this.configuredProgramState.addAll(localConfigureStates);

                		if (cs.getProgramRunResult()==null || cs.getProgramRunResult()!=0)
                			configStepErroredOut = true;
                	}
                	if (configStepErroredOut || ConfigureState.getAllProgramsRan()==false || localConfigureStates.size()!=configureInfos.size())
                		throw new Exception("Configure step(s) errored out"); // cleanup/destroy this template run; this.mapStepReferenceToResourceInstance holds all bound resources
                }
                if (startHandler != null) {
                	// success is that no exception is thrown by .waitComplete() or the asynch start process(es) behind it
                	List<StartState> localStartStates = startHandler.waitComplete();
            	    this.startProgramStates.addAll(localStartStates);
                	boolean startStepErroredOut = false;
                	for (StartState ss : localStartStates) {
                		if (ss.getRunnableProgram() == null)
                			startStepErroredOut = true;
                	}
                	if (startStepErroredOut || StartState.getAllProgramsRan()==false || localStartStates.size()!=startInfos.size())
                		throw new Exception("Start step(s) errored out"); // cleanup/destroy this template run; this.mapStepReferenceToResourceInstance holds all bound resources
                }
                if (runHandler != null) {
                	// success is that no exception is thrown by .waitComplete() or the asynch run process(es) behind it
                	List<RunState> localRunStates = runHandler.waitComplete();
                	this.runProgramStates.addAll(localRunStates);
                	boolean runStepErroredOut = false;
                	for (RunState rs: localRunStates) {
                		if (rs.getProgramRunResult()==null || rs.getProgramRunResult()!=0) {
                			runStepErroredOut = true;
                		}
                    	if (runStepErroredOut || RunState.getAllProgramsRan()==false || localRunStates.size()!=runInfos.size())
                    		throw new Exception("Run step(s) errored out"); // cleanup/destroy this template run; this.mapStepReferenceToResourceInstance holds all bound resources
                	}
                }
            } while (bindHandler!=null  && !bindHandler.isDone() ||
            		 deployHandler!=null && !deployHandler.isDone());
        }
    }

    void setTemplateCleanupInfo(ResourceCoordinates resourceCoordinates) {
    	this.templateCleanupInfo = resourceCoordinates;
    }
    
    void informResourceProviders(ResourceCoordinates resourceCoordinates) {
    	if (this.templateCleanupInfo == null)
    		this.templateCleanupInfo = resourceCoordinates;
    }
    
    private void informResourceProviders() {
    	String templateID = null;
    	if (this.templateCleanupInfo != null) { // null: this template did not successfully reserve a resource
	    	// Tell the ResourcesManager (associated with every bind step of this specific instanced template), to release this template instance (the one associated with the relevant templateID). 
	    	ResourcesManager rm = this.templateCleanupInfo.getManager();
	    	templateID = this.templateCleanupInfo.templateId;
	    	rm.release(templateID, false);
    	}
    	log.debug(simpleName + "informResourceProviders() " + (this.templateCleanupInfo==null ? "does NOT inform" : "informs") + " the resource provider system that template " + (templateID!=null ? templateID : "") + " no longer needs its reserved or bound resources");
    }
    
    public void destroy() {
    	this.informResourceProviders();
    }
    	
}