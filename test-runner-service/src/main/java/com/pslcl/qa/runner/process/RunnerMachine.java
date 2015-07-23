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
    private class DescribedTemplateThreadFactory implements ThreadFactory {
        
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
        templateExecutorService = Executors.newCachedThreadPool(new DescribedTemplateThreadFactory());
        tp = new TemplateProvider();
    }
    
    /**
     * Accept new template to eventually become a test run
     * 
     * @note runnerService.ackQueueStoreEntry() must eventually be called; it may quickly be called in this method, or eventually from a distant thread  
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Exception
     */
    public void initiateProcessing(long dtNum, Object message) {
        try {
            DescribedTemplateState dtState = new DescribedTemplateState(dtNum, message);
            Action action = dtState.getAction();  // Action.INITIALIZE_INSTANCE
            Action nextAction = action.act(dtState, null, this.tp, runnerService);            
            // for anything returned except ACTION.DISCARDED: the new dtState is stored in ActionStore
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling dtNum " + dtNum + ": " + e + ". Message remains in the QueueStore.");
        }
    }
    
    /**
     * @param dtNum
     */
    void addNewTemplate(long dtNum, DescribedTemplateState dtState) {
        // TODO. May choose to not process this new template; would be because this RunnerService node is overloaded
        // boolean doProcess = determineDoProcess(tNum, iState);
        // if (doProcess)
        {
            DescribedTemplateState dtStateOld = runnerService.actionStore.put(dtNum, dtState);
            // TODO: For dtStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that tStateOld shows has been allocated or started or whatever?
        }
        
        try {
            new DescribedTemplateTask(this, dtNum);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // launch independent thread
    }

    /**
     * 
     * @param dtNum
     */
    void removeTemplate(long dtNum) {
        DescribedTemplateState dtStateOld = runnerService.actionStore.remove(dtNum);
        // TODO: Shall we cleanup whatever it is that dtStateOld shows has been allocated or started or whatever?
    }
    
}