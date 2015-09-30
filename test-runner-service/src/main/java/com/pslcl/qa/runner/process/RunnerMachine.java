package com.pslcl.qa.runner.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.pslcl.qa.runner.RunnerService;
import com.pslcl.qa.runner.template.TemplateProvider;


/**
 * RunnerMachine knows how to do everything with templates, test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.  
 */
public class RunnerMachine {

    // class members

    final RunnerService runnerService;
    final ExecutorService templateExecutorService; // supplies threads for individual template tasks
    public final TemplateProvider tp;


    
    // sub classes
    
    /**
     * For threads created by templateExecutorService, specify thread name and sets daemon true
     */
    private class TemplateRunThreadFactory implements ThreadFactory {
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("thread from templateExecutorService");
            return t;
        }

    }

    /** Constructor
     * 
     * @param runnerService
     */
    public RunnerMachine(RunnerService runnerService) {
        this.runnerService = runnerService;
        templateExecutorService = Executors.newCachedThreadPool(new TemplateRunThreadFactory());
        tp = new TemplateProvider(templateExecutorService);
    }
    
    /**
     * Accept new template to become a test run, with an entry in table run
     * 
     * @note runnerService.ackQueueStoreEntry() must eventually be called; it may quickly be called in this method, or eventually from a distant thread
     * @oaram reNum
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Exception
     */
    public void initiateProcessing(long reNum, Object message) {
        try {
            RunEntryState reState = new RunEntryState(reNum, message);
            Action action = reState.getAction();  // Action.INITIALIZE
            Action nextAction = action.act(reState, null, this.tp, runnerService);            
            // for anything returned except ACTION.DISCARDED: the new reState is stored in ActionStore
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling reNum " + reNum + ": " + e + ". Message remains in the QueueStore.");
        }
    }
    
    /**
     * @note can drop reNum, the given run entry
     * @param reNum
     */
    void engageNewRunEntry(long reNum, RunEntryState reState) {
        // TODO. May choose to not process this new template; would be because this RunnerService node is overloaded. Or because this run entry already has a result stored
        // boolean doProcess = determineDoProcess(reNum, reState);
        // if (doProcess)
        {
            RunEntryState reStateOld = runnerService.actionStore.put(reNum, reState);
            // TODO: For reStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
        }
        
        try {
            new RunEntryTask(this, reNum);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // launch independent thread
    }

    /**
     * 
     * @param reNum
     */
    void removeTemplateRun(long reNum) {
        RunEntryState reStateOld = runnerService.actionStore.remove(reNum);
        // TODO: Shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
    }
    
}