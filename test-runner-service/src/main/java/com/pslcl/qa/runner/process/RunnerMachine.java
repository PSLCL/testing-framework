package com.pslcl.qa.runner.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.pslcl.qa.runner.RunnerService;


/**
 * RunnerMachine knows how to do everything with templates, test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.  
 */
public class RunnerMachine {

    // class members

    final RunnerService runnerService;
    final ExecutorService templateExecutorService; // supplies threads for individual template tasks

    
    // sub classes
    
    /**
     * For threads created by templateExecutorService, specify thread name and sets daemon true
     */
    private class TemplateThreadFactory implements ThreadFactory {
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("threadFromTemplateExecutor");
            return t;
        }

    }

    /** Constructor
     * 
     * @param runnerService
     */
    public RunnerMachine(RunnerService runnerService) {
        this.runnerService = runnerService;
        templateExecutorService = Executors.newCachedThreadPool(new TemplateThreadFactory());
    }
    
    /**
     * Accept new template to eventually become a test run
     * 
     * @note runnerService.ackQueueStoreEntry() must eventually be called; it may quickly be called in this method, or eventually from a distant thread  
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Exception
     */
    public void initiateProcessing(long tNum, Object message) {
        try {
            TemplateState tState = new TemplateState(tNum, message);
            Action action = tState.getAction();  // Action.INITIALIZE_INSTANCE
            Action nextAction = action.act(tState, null, runnerService);            
            // for anything returned except ACTION.DISCARDED: the new tState is stored in ActionStore
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling template number " + tNum + ": " + e + ". Message remains in the QueueStore.");
        }
    }
    
    /**
     * @param tNum
     */
    void addNewTemplate(long tNum, TemplateState tState) {
        // TODO. May choose to not process this new template; would be because this RunnerService node is overloaded
        // boolean doProcess = determineDoProcess(tNum, iState);
        // if (doProcess)
        {
            TemplateState tStateOld = runnerService.actionStore.put(tNum, tState);
            // TODO: For tStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that tStateOld shows has been allocated or started or whatever?
        }
        
        try {
            new TemplateTask(this, tNum);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // launch independent thread
    }

    /**
     * 
     * @param tNum
     */
    void removeTemplate(long tNum) {
        TemplateState iStateOld = runnerService.actionStore.remove(tNum);
        // TODO: Shall we cleanup whatever it is that tStateOld shows has been allocated or started or whatever?
    }
    
}