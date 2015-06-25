package com.pslcl.qa.platform.process;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.pslcl.qa.platform.Core;
import com.pslcl.qa.platform.RunnerService;


/**
 * RunnerMachine knows how to do everything with test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.  
 */
public class RunnerMachine {

    // class members

    final RunnerService runnerService;
    final ExecutorService instanceExecutorService; // supplies threads for individual InstanceTasks

    
    // sub classes
    
    /**
     * For threads created by testRunExecutorService, specify thread name and sets daemon true
     */
    private class InstanceThreadFactory implements ThreadFactory {
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("threadFromInstanceExecutor");
            return t;
        }

    }

    /** Constructor
     * 
     * @param runnerService
     */
    public RunnerMachine(RunnerService runnerService, Core core) {
        this.runnerService = runnerService;
        instanceExecutorService = Executors.newCachedThreadPool(new InstanceThreadFactory());
    }
    
    /**
     * Accept new test instance to eventually become a test run
     * 
     * @note runnerService.ackInstanceEntry() must eventually be called; it may quickly be called in this method, or eventually from a distant thread  
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Exception
     */
    public void initiateProcessing(long iNum, Object message) {
        try {
            InstanceState iState = new InstanceState(iNum, message);
            Action action = iState.getInstanceAction();  // Action.INITIALIZE_INSTANCE
            Action nextAction = action.act(iState, null, runnerService);            
            // for anything returned except ACTION.DISCARDED: the new iState is stored in ActionStore
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling instanceNumber " + iNum + ": " + e + ". Message remains in the InstanceStore.");
        }
    }
    
    /**
     * @param iNum
     */
    void addNewInstance(long iNum, InstanceState iState) {
        // TODO. May choose to not process this new instance; would be because this RunnerService node is overloaded
        // boolean doProcess = determineDoProcess(iNum, iState);
        // if (doProcess)
        {
            InstanceState iStateOld = runnerService.actionStore.put(iNum, iState);
            // TODO: For iStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that iStateOld shows has been allocated or started or whatever?
        }
        
        try {
            new InstanceTask(this, iNum);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // launch independent thread
    }

    /**
     * 
     * @param iNum
     */
    void removeInstance(long iNum) {
        InstanceState iStateOld = runnerService.actionStore.remove(iNum);
        // TODO: Shall we cleanup whatever it is that iStateOld shows has been allocated or started or whatever?
    }
    
}