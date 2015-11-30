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
    void instanceNestedTemplates(RunEntryCore reCore) throws Exception {
        StepsParser stepsParser = iT.getStepsParser();
        int localStepReference = stepsParser.getStepReference(); // 0, because template includes are always first
        // instance them in parallel, each in their own thread
        List<String> includeSteps = stepsParser.getNextSteps("include "); // trailing space is required
        List<Future<? extends InstancedTemplate>> futures = new ArrayList<>();
        for (String includeStep: includeSteps) {
            String includeText = StepsParser.peekNextSpaceTerminatedSubString(includeStep, 0); // get "include" substring
            includeText += " "; // append the trailing space
            String templateHashText = StepsParser.peekNextSpaceTerminatedSubString(includeStep, (includeText.length()));
            DBTemplate nestedDBTemplate = new DBTemplate(null); // null: a nested template has no reNum (run entry number)
            // use reCore to fill nestedDBTemplate. todo: nestedDBTemplate can be truncated

            // db code



            // start up instantiation of this nested template
            NestedTemplateTask task = new NestedTemplateTask(localStepReference, reCore, nestedDBTemplate, runnerMachine);
            Future<InstancedTemplate> future = runnerMachine.getConfig().blockingExecutor.submit(task);
            futures.add(future);
            ++localStepReference;
        }

        // collect resultant InstancedTemplates from n nested templates
        List<InstancedTemplate> localListNestedIT = new ArrayList<>();
        for (Future<? extends InstancedTemplate> future : futures) {
            // future can be null (submit task failed early); ; or can be an InstancedTemplate
            boolean fail = true;
            try {
                InstancedTemplate localIT = future.get();
                if (localIT != null) {
                    localListNestedIT.add(localIT);
                    fail = false;
                }
            } catch (InterruptedException ee) {
                Throwable t = ee.getCause();
                String msg = ee.getLocalizedMessage();
                if(t != null)
                    msg = t.getLocalizedMessage();
                //LoggerFactory.getLogger(getClass()).info(ResourceInstance.class.getSimpleName() + " bind failed: " + msg, ee);
            } catch (ExecutionException e) {
                LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
            }

            if (fail) {
                // TODO: cleanup each InstancedTemplate from listNestedIT
                // TODO: throw Exception to the caller
            }
            
            iT.setListNestedIT(localListNestedIT);
        }
        // We discard our information about each future (i.e. Future<? extends InstancedTemplate>). Our API is with each InstancedTemplate in listNestedIT.

        // on success exit, every template instance is running
    }

}