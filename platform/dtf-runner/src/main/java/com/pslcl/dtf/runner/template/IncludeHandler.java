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
	private List<NestedTemplateTask> ntt;
    private final Logger log;
    private final String simpleName;

	
	IncludeHandler(InstancedTemplate iT, List<String> includeSteps, RunnerMachine runnerMachine) {
		this.iT = iT;
		this.includeSteps = includeSteps;
		this.includeInfos = null;
		this.runnerMachine = runnerMachine;
		this.ntt = null;
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";

	}
	
    /**
     * 
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
    List<ReferencedNestedTemplate> instanceTemplates() throws Exception {
    	// launch instantiation tasks
        List<Future<ReferencedNestedTemplate>> futures = new ArrayList<>();
        int stepReference = 0; // the first include statement is always line 0 of steps
    	for (IncludeInfo includeInfo : this.includeInfos) { // this.includeInfos is an ArrayList across which iteration retrieves in the same order as insertion (because we are done inserting)
    		NestedTemplateTask ntt = new NestedTemplateTask(stepReference, this.iT.getRunEntryCore(), includeInfo.getDBTemplate(), this.runnerMachine);
    		log.debug(this.simpleName + ".instanceTemplates() launches template instantiation for nested template " + includeInfo.getTemplateHash() + ", as reference " + stepReference);
    		Future<ReferencedNestedTemplate> future = this.runnerMachine.getConfig().blockingExecutor.submit(ntt);
    		futures.add(future);
    		++stepReference;
    	}
    	
    	// collect task results
    	boolean allIncludes = true;
    	List<ReferencedNestedTemplate> retRnts = new ArrayList<>();
    	for (Future<ReferencedNestedTemplate> future : futures) {
    		ReferencedNestedTemplate rnt = null;
    		try {
				rnt = future.get();
	    		log.debug(this.simpleName + ".instanceTemplates() sees successful template instantation for nested template " + rnt.instancedTemplate.getTemplateID());
	    		retRnts.add(rnt);
            } catch (InterruptedException | ExecutionException ioreE) {
                Throwable t = ioreE.getCause();
                String msg = ioreE.getLocalizedMessage();
                if(t != null)
                    msg = t.getLocalizedMessage();
                log.warn(simpleName + ".instanceTemplates(), include failed: " + msg, ioreE);
                allIncludes = false;
            }
    	}
        if (!allIncludes)
            throw new Exception("IncludeHandler() finds one or more include steps failed");
        return retRnts;
    }

}