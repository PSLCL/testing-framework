package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

public class NestedTemplateHandler {

    private InstancedTemplate iT;
    private RunnerMachine runnerMachine;

    NestedTemplateHandler(InstancedTemplate iT, RunnerMachine runnerMachine) {
        this.iT = iT;
        this.runnerMachine = runnerMachine;
    }

    /**
     *
     * @throws Exception
     */
    void instanceNestedTemplates(RunEntryCore reCore, StepsParser stepsParser) throws Exception {
        int localStepReference = stepsParser.getStepReference(); // 0, because template includes are always first
        // instance them in parallel, each in their own thread
        List<String> includeSteps = stepsParser.getNextSteps("include "); // trailing space is required
        List<Future<? extends ReferencedNestedTemplate>> futures = new ArrayList<>();
        for (String includeStep: includeSteps) {
        	String includeText = StepsParser.peekNextSubString(includeStep, 0) + " "; // move past include hash parameter, past "include" substring and its trailing space
            String templateHashText = StepsParser.peekNextSubString(includeStep, (includeText.length()));
            DBTemplate nestedDBTemplate = new DBTemplate(null); // null: a nested template has no reNum (run entry number)
            // use reCore to fill nestedDBTemplate. TODO: nestedDBTemplate can be truncated

            // TODO: db code


            if (nestedDBTemplate.checkValidTemplateInfo()) {
                // start up instantiation of this nested template; work takes place in its own thread, as task.call()
                NestedTemplateTask task = new NestedTemplateTask(localStepReference, reCore, nestedDBTemplate, runnerMachine);
                Future<ReferencedNestedTemplate> future = runnerMachine.getConfig().blockingExecutor.submit(task);
                futures.add(future);
            } else {
            	LoggerFactory.getLogger(getClass()).warn("NestedTemplateHandler.instanceNestedTemplates() include step template reference " + futures.size() + " has inadequate template info");
            	throw new Exception("inadequate template info");
            }
            ++localStepReference;
        }

        // collect resultant InstancedTemplates from n nested templates
        for (Future<? extends ReferencedNestedTemplate> future : futures) {
            // future can be null (submit task failed early); ; or can be an InstancedTemplate
            boolean fail = true;
            try {
                ReferencedNestedTemplate rnt= future.get();
                if (rnt != null) {
                	this.iT.markNestedTemplate(rnt.nestedStepReference, rnt.instancedTemplate);
                    fail = false;
                }
			} catch (InterruptedException | ExecutionException ioreE) {
                Throwable t = ioreE.getCause();
                String msg = ioreE.getLocalizedMessage();
                if(t != null)
                    msg = t.getLocalizedMessage();
                LoggerFactory.getLogger(getClass()).warn("waitComplete(), nested template failed: " + msg, ioreE);
			} 

            if (fail) {
                // cleanup
            	this.iT.destroyNestedTemplate();
            	throw new Exception("nested template failure");
            }
            
        }
        // We discard our information about each future (i.e. Future<? extends InstancedTemplate>). Our API is with each InstancedTemplate in listNestedIT.

        // on success exit, every template instance is running
    }

}