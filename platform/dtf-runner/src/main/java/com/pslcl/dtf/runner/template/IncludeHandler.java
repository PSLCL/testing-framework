package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * 
 * 
 *
 */
public class IncludeHandler {
	private InstancedTemplate iT;
	private List<String> includeSteps;
	private List<IncludeInfo> includeInfos;
	private RunnerMachine runnerMachine;
    private final Logger log;
    private final String simpleName;

	
	IncludeHandler(InstancedTemplate iT, List<String> includeSteps, RunnerMachine runnerMachine) {
		this.iT = iT;
		this.includeSteps = includeSteps;
		this.includeInfos = null;
		this.runnerMachine = runnerMachine;
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
	}
	
    /**
     * 
     * @note This method does not cause actions that need to be cleaned up
     * @return
     * @throws Exception
     */
	int computeIncludeRequests() throws Exception {
    	this.includeInfos = new ArrayList<IncludeInfo>();
    	
    	// collect template hash(es)
    	for (String includeStep : this.includeSteps) {
    		String[] parsedStep = includeStep.split(" "); // [command, templateHash]
    		if (parsedStep.length != 2) {
    			log.warn(this.simpleName + "includeStep malformed: " + includeStep);
                throw new IllegalArgumentException("include step malformed");
    		}
    		
    		String includeTemplateHash = parsedStep[1];
    		IncludeInfo includeInfo = new IncludeInfo(includeTemplateHash);
    		this.includeInfos.add(includeInfo);
    	}
    	
    	// collect template info(s)
    	for (IncludeInfo includeInfo : this.includeInfos)
    		includeInfo.setDBTemplate(this.iT.getRunEntryCore().getTemplateInfo(includeInfo.getTemplateHash()));
    	
    	return this.includeInfos.size();
    }
    		
    /**
     * 
     */
    void instanceTemplates() throws Exception {
    	// launch instantiation tasks
        List<Future<ReferencedNestedTemplate>> futures = new ArrayList<>();
        int stepReference = 0; // the first include statement is always line 0 of steps
    	for (IncludeInfo includeInfo : this.includeInfos) { // this.includeInfos is an ArrayList across which iteration retrieves in the same order as insertion (because we are done inserting)
    		try {
				NestedTemplateTask ntt = new NestedTemplateTask(stepReference, this.iT.getRunEntryCore(), includeInfo.getDBTemplate(), this.runnerMachine);
				log.debug(this.simpleName + ".instanceTemplates() launches template instantiation for nested template " + includeInfo.getTemplateHash() + ", as reference " + stepReference);
				Future<ReferencedNestedTemplate> future = this.runnerMachine.getConfig().blockingExecutor.submit(ntt);
				futures.add(future);
				++stepReference;
			} catch (Exception e) {
				log.debug(this.simpleName + ".instanceTemplates(), sees exception while launching template instantiation tasks, msg: " + e.getMessage());
				// TODO: cleanup whatever exists in futures, i.e. future.get() for each, to get an iT; then iT.cleanup 
				
				throw e;
			}
    	}
    	
    	// collect task results
    	boolean allIncludes = true;
    	for (Future<ReferencedNestedTemplate> future : futures) {
    		ReferencedNestedTemplate rnt = null;
    		try {
				rnt = future.get();
	    		log.debug(this.simpleName + ".instanceTemplates() sees successful template instantation for nested template " + rnt.iT.getTemplateID());
            	this.iT.markNestedTemplate(rnt.nestedStepReference, rnt.iT);
            } catch (InterruptedException | ExecutionException ioreE) {
                String msg = ioreE.getLocalizedMessage();
                Throwable t = ioreE.getCause();
                if(t != null)
                    msg = t.getLocalizedMessage();
                log.warn(simpleName + ".instanceTemplates(), include failed: " + msg);
                allIncludes = false;
                break;
            }
    	}
        if (!allIncludes) {
			log.debug(this.simpleName + ".instanceTemplates(), sees include failure while collecting template instantiation results; now cleans up successful nested templates");
        	
			// cleanup whatever nested templates have been instantiated
        	this.iT.destroyNestedTemplates();
            throw new Exception("IncludeHandler() finds one or more include steps failed");
        }
    }

}