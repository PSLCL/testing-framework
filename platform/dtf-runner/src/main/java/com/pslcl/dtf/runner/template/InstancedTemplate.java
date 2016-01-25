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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.runner.QAPortalAccess;
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
    	InstancedTemplate iT;
		iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine);
		iT.runSteps();
    	return iT;
    }
	
    private final Logger log;
    private final String simpleName;
	private RunEntryCore reCore;   // holds runID of our top-level template (might be us or we might be nested down from it)
	private DBTemplate dbTemplate; // holds templateID as a hash
	private RunnerMachine runnerMachine;
	private ResourceCoordinates templateCleanupInfo;
	private long uniqueMark;
	
	private StepsParser stepsParser;
    private Map<Integer, InstancedTemplate> mapStepReferenceToNestedTemplate;
    private Map<ResourceCoordinates, Integer> mapResourceCoordinatesToStepReference;
    private Map<Integer, ResourceInstance> mapStepReferenceToResourceInstance;
    
    /** the purpose of holding these accumulating lists is, at least, for cleanup when this (a template) is destroyed */
    private List<ResourceInstance> boundResourceInstances;
    private List<DeployInfo> deployedInfos;
	private List<CableInstance> cableInstances;
//	private List<StartState> startProgramStates;
//	private List<RunState> runProgramStates;

    private List<ResourceInfo> orderedResourceInfos = null;
    
    InstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
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
        this.uniqueMark = this.runnerMachine.getTemplateProvider().addToReleaseMap(this);
    }
    
    long getUniqueMark() {
    	return this.uniqueMark;
    }
    
    public ResourceProviders getResourceProviders() {
    	return this.runnerMachine.getTemplateProvider().getResourceProviders();
    }
    
    public QAPortalAccess getQAPortalAccess() {
    	return this.runnerMachine.getService().getQAPortalAccess();
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
    	
    	try {
			// first, instance nested templates
			NestedTemplateHandler nth = new NestedTemplateHandler(this, runnerMachine);
			try {
				nth.instanceNestedTemplates(reCore); // blocking call
			} catch (Exception e) {
				log.debug(simpleName + "runSteps() exception in processing a nested template, msg: " + e);
				throw e;
			}
			int currentStepReference = this.mapStepReferenceToNestedTemplate.size();

			// second, process each step set, in sequence (starting at setID 0, with no missing setID)
			//     do this by fully processing steps of the same set, before moving on to the next set
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

			    // algorithm for each setID: initiate step processing in sequence, as supplied by the template, while also allowing parallel processing; within a setID, order of step completion is uncertain
				BindHandler bindHandler = null;
				ProgramHandler configureHandler = null;
				ConnectHandler connectHandler = null;
				DeployHandler deployHandler = null;
				InspectHandler inspectHandler = null;
				ProgramHandler runHandler = null;
				ProgramHandler startHandler = null;

				List<ProgramInfo> startInfos = null;
				
				// this for() loop: initiate all step commands for same setID
			    for (int setStepCount=0; setStepCount<stepsOfSet.size(); setStepCount++) {
			    	// from the first step of a possible group of same-command steps, get its command and act on it
			    	String commandString = null;
			    	SetStep firstLikeStep = new SetStep(stepsOfSet.get(setStepCount));
			    	try {
						commandString = firstLikeStep.getCommand();
						switch(commandString) {
							// NOTE: In a pattern, none of these cases accomplish their final work. That work is accomplished in the do/while loop that follows our for loop.
						case "bind":
							if (bindHandler == null) {
								bindHandler = new BindHandler(this, stepsOfSet);
								int consumedStepReferences = bindHandler.computeReserveRequests(currentStepReference, this.getTemplateID(), this.getRunID());
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block. 
								bindHandler.proceed();
								// bind processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "bind steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "configure":
							if (configureHandler == null) {
								configureHandler = new ProgramHandler(this, stepsOfSet);
							    int consumedStepReferences = configureHandler.computeProgramRequests(); // setID configure 0-based-machine-ref program-name [param param ...]
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								configureHandler.proceed(); // ignore the empty return list for this first time caller
								// configure processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "configure steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "connect":
							// We track CableInstance's of each connect, even though (probably) only needed for cleaning up the case where a parent template causes connects in a nested template
							if (connectHandler == null) {
								connectHandler = new ConnectHandler(this, stepsOfSet);
								int consumedStepReferences = connectHandler.computeConnectRequests(); // setID connect 0-based-machine-ref 0-based-network-reference
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;

								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								connectHandler.proceed(); // ignore the empty return list for this first time caller
								// connect processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "connect steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "deploy":
							// We track MachineInstance's of each deploy, even though (probably) only needed for cleaning up the case where a parent template deploys to the machine of a nested template
							if (deployHandler == null) {
								deployHandler = new DeployHandler(this, stepsOfSet);
								int consumedStepReferences = deployHandler.computeDeployRequests(); // setID deploy 0-based-machine-ref artifact-name artifactHash
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								deployHandler.proceed();
								// deploy processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "deploy steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "inspect":
							if (inspectHandler == null) {
								inspectHandler = new InspectHandler(this, stepsOfSet);
								int consumedStepReferences = inspectHandler.computeInspectRequests(); // setID inspect 0-based-person-ref instructionsHash strArtifactName strArtifactHash [strArtifactName strArtifactHash] ...
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								inspectHandler.proceed();
								// inspect processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "inspect steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "run":
							// We track MachineInstance's of each run, even though (probably) only needed for cleaning up the case where a parent template "runs" to the machine instance of a nested template
							if (runHandler == null) {
								runHandler = new ProgramHandler(this, stepsOfSet);
							    int consumedStepReferences = runHandler.computeProgramRequests(); // setID run 0-based-machine-ref program-name [param param ...]
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								runHandler.proceed(); // ignore the empty return list for this first time caller
								// run processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "run steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
							}
							break;
						case "start":
							// We track MachineInstance's of each start, even though (probably) only needed for cleaning up the case where a parent template "starts" to the machine instance of a nested template
							if (startHandler == null) {
								// A generator is encouraged to divide templates into nested templates, such that a single start step appears in a template, and only in a nested template.
								// Even so, we process whatever start step arrangement comes to us.
								startHandler = new ProgramHandler(this, stepsOfSet);
							    int consumedStepReferences = startHandler.computeProgramRequests(); // setID start 0-based-machine-ref program-name [param param ...]
								setStepCount += consumedStepReferences;
								currentStepReference += consumedStepReferences;
								
								// Pattern for all .proceed() calls. Call it (first) in this for() loop that initiates all step commands for same setID. This first call will not block.
								startHandler.proceed(); // ignore the empty return list for this first time caller
								// start processing has advanced and may require waiting for Future(s); notification of current progress comes to us below, after we have initiated the processing of other steps of this step set
							} else {
								log.debug(this.simpleName + "start steps not all contiguous, for step set " + setStepCount);
								throw new Exception("Improper step order");
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
					    log.debug(simpleName + "runSteps(): " + commandString + " executing step fails: " + e.getMessage());
						throw e;
					}
			    } // end for(): initiate all step commands for same setID


			    // TODO: move things like this to here
				List<ProgramInfo> runInfos = null;
//				List<ProgramInfo> startInfos = null;

			    
				// this do/while loop: process all step commands for same setID
			    boolean allStepsCompleteForThisStepSet;
			    do {
		    		allStepsCompleteForThisStepSet = true;
			    	
			        // These individual .proceed() calls may potentially each block for a while, then return. At each return, they mark themselves as done, or not.
			    	//     Eventually, by cycling through this loop multiple times, all steps of this step set will be found to have concluded their processing.
			    	//     The loop exits at that time.
			    	if (bindHandler!=null && !bindHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;
			    		bindHandler.proceed();
			    		if (bindHandler.isDone()) {
			    			List<ResourceInstance> localRI = bindHandler.getResourceInstances();
			    			this.boundResourceInstances.addAll(localRI);
			    			log.debug(simpleName + "bindHandler() completes " + localRI.size() + " bind(s) for setID " + setID);
			    		}
			    	}
			        if (configureHandler!=null && !configureHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;
			    		
			        	List<ProgramState> localProgramStates = configureHandler.proceed();
			    		if (configureHandler.isDone()) {
			    			boolean fail = true;
			    			if (localProgramStates.size() == configureHandler.getConsecutiveSameStepCount()) {
					        	boolean configStepErroredOut= false;
					        	for (ProgramState ps : localProgramStates) {
					        		Integer programRunResult = ps.getProgramRunResult(); 
					        		if (programRunResult==null || programRunResult!=0) {
					        			configStepErroredOut = true;
					        			log.debug(this.simpleName + "A configure program returned non-zero, or failed to run at all");
					        			break;
					        		}
					        	}
					        	if (!configStepErroredOut)
					        		fail = false;
			    			} else {
			    				log.debug(this.simpleName + "Configure program results are missing");
			    			}
			    			if (fail)
				        		throw new Exception("Configure step(s) errored out");
			    			log.debug(simpleName + "configureHandler() completes " + configureHandler.getConsecutiveSameStepCount() + " configure program(s) for setID " + setID);
			    		}
			        }
			        if (connectHandler!=null && !connectHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;

						// we track and record CableInstance's of each connect, even though (probably) only needed for cleaning up the case where a parent template causes connects in a nested template
			        	List<CableInstance> localCableInstances = connectHandler.proceed();
			    		if (connectHandler.isDone()) {
							this.cableInstances.addAll(localCableInstances);
							if (ConnectInfo.getAllConnectedSuccess() && localCableInstances.size()==connectHandler.getConnectRequestCount()) {
				    			log.debug(simpleName + "connectHandler() completes " + connectHandler.getConnectRequestCount() + " connect(s) for setID " + setID);
							} else {
								// one or more connect steps errored out; this template is errored out
								throw new Exception("InstancedTemplate.runSteps() connect handling has incomplete successful connect list, for setID " + setID);
								// initiate cleanup/destroy of this template run; this.cableInstances holds all information
							}
			    		}
			        }
			        if (deployHandler!=null && !deployHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;

			    		// we track and record MachineInstance's of each deploy, even though (probably) only needed for cleaning up the case where a parent template deploys to a nested template
			    		deployHandler.proceed();
			    		if (deployHandler.isDone()) {
			    			List<DeployInfo> localDeployInfos = deployHandler.getDeployInfos();
							this.deployedInfos.addAll(localDeployInfos);
							if (DeployInfo.getAllDeployedSuccess() && localDeployInfos.size()==deployHandler.getDeployRequestCount()) {
				    			log.debug(simpleName + "deployHandler() completes " + deployHandler.getDeployRequestCount() + " deploys(s) for setID " + setID);
							} else {
								// one or more deploy steps errored out; this template is errored out
								throw new Exception("InstancedTemplate.runSteps() deploy handling has incomplete successful deployed list, for setID " + setID);
								// initiate cleanup/destroy of this template run; this.deployedInfos holds all information
							}
			    		}
			        }
			        if (inspectHandler!=null && !inspectHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;

			        	inspectHandler.proceed();
			    		if (inspectHandler.isDone()) {
			    			log.debug(simpleName + "inspectHandler() completes " + inspectHandler.getInspectRequestCount() + " inspect(s) for setID " + setID);
			    		}
			        }
			        if (runHandler!=null && !runHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;
			    		
			        	List<ProgramState> localProgramStates = runHandler.proceed();
			    		if (runHandler.isDone()) {
			    			boolean fail = true;
			    			if (localProgramStates.size() == runHandler.getConsecutiveSameStepCount()) {
					        	boolean runStepErroredOut= false;
					        	for (ProgramState ps : localProgramStates) {
					        		RunnableProgram runnableProgram = ps.getRunnableProgram(); 
					        		if (runnableProgram == null) {
					        			runStepErroredOut = true;
					        			log.debug(this.simpleName + "A run program returned null RunnableProgram");
					        			break;
					        		}
					        		
					        		Integer programRunResult = runnableProgram.getRunResult();
					        		if (programRunResult==null || programRunResult!=0) {
					        			runStepErroredOut = true;
					        			log.debug(this.simpleName + "A program run returned non-zero, or failed to run at all");
					        			break;
					        		}
					        	}
					        	if (!runStepErroredOut)
					        		fail = false;
			    			} else {
			    				log.debug(this.simpleName + "Run program results are missing");
			    			}
			    			if (fail)
				        		throw new Exception("Run step(s) errored out");
			    			log.debug(simpleName + "runHandler() completes " + runHandler.getConsecutiveSameStepCount() + " run program(s) for setID " + setID);
			    		}
			        }
			        
			        if (startHandler!=null && !startHandler.isDone()) {
			    		allStepsCompleteForThisStepSet = false;
			    		
			        	List<ProgramState> localProgramStates = startHandler.proceed();
			    		if (startHandler.isDone()) {
			    			boolean fail = true;
			    			if (localProgramStates.size() == startHandler.getConsecutiveSameStepCount()) {
					        	boolean startStepErroredOut= false;
					        	for (ProgramState ps : localProgramStates) {
					        		RunnableProgram runnableProgram = ps.getRunnableProgram(); 
					        		if (runnableProgram == null) {
					        			startStepErroredOut = true;
					        			log.debug(this.simpleName + "A program start returned null RunnableProgram");
					        			break;
					        		}
					        		
					        		Integer programStartResult = runnableProgram.getRunResult();
					        		if (programStartResult==null || programStartResult!=0) {
					        			startStepErroredOut = true;
					        			log.debug(this.simpleName + "A program start returned non-zero, or failed to run at all");
					        			break;
					        		}
					        	}
					        	if (!startStepErroredOut)
					        		fail = false;
			    			} else {
			    				log.debug(this.simpleName + "Start program results are missing");
			    			}
			    			if (fail)
				        		throw new Exception("Start step(s) errored out");
			    			log.debug(simpleName + "startHandler() completes " + startHandler.getConsecutiveSameStepCount() + " start program(s) for setID " + setID);
			    		}
			        }
			    } while (!allStepsCompleteForThisStepSet); // end do/while loop: process all step commands for same setID
			} // end for(): process each step set, in sequence
    	} catch (Exception e) {
			log.debug(this.simpleName + "runSteps() errors out for runID " + this.getRunID() + ", templateID " + this.getTemplateID() + ", uniqueMark " + this.getUniqueMark());
			this.runnerMachine.getTemplateProvider().releaseTemplate(this); // removes mark, cleans up by iT by informing resource providers
			throw e;
		}
    	
    	log.debug(this.simpleName + "runSteps() completes without error");
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
	    	
	    	log.debug(simpleName + "informResourceProviders() about to inform the resource provider system that template " + (templateID!=null ? templateID : "null") + " no longer needs its reserved or bound resources");
	    	rm.release(templateID, false);
	    	log.debug(simpleName + "informResourceProviders()        informed the resource provider system that template " + (templateID!=null ? templateID : "null") + " no longer needs its reserved or bound resources");
	    	
    	} else
    		log.debug(simpleName + "because there was no cleanup info, informResourceProviders() did NOT inform the resource provider system that template " + (templateID!=null ? templateID : "null") + " no longer needs its reserved or bound resources");
    }
    
    public void destroy() {
    	// this is intended to be called only by TemplateProvider.releaseTemplate(iT)
    	this.informResourceProviders();
    	// maybe other cleanup related to iT only, but not .releaseTemplate() again - it called us
    }
    	
}