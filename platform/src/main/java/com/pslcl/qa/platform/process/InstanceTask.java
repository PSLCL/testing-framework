package com.pslcl.qa.platform.process;

import com.pslcl.qa.platform.Core;

public class InstanceTask implements Runnable {
    private RunnerMachine runnerMachine;
    private Core core;
    private InstanceCore iCore;
    private long iNum;
    private String instanceThreadName;

    /**
     * Constructor: From a given test instance number, initiate execution of the corresponding test instance (aka test run).
     * 
     * @note Step 1 Check to see if first time setup is accomplished; if not, setup our testrunExecutor.
     * @note Step 2 Submit this test instance to the testRunExecutorService for future execution.
     * @note Every thread or task created by this class closes itself automatically, whether seconds or days from instantiation.
     * @param core The database and business logic class
     * @param testInstanceNumber identifies test instance to execute
     */
    public InstanceTask(RunnerMachine runnerMachine, long iNum) throws Exception {
        this.runnerMachine = runnerMachine;
        this.iNum = iNum;
        this.instanceThreadName = new String("instance " + iNum);
        
//        // HACK: fixed test number for initial testing purposes; test instance number is what came out of the InstanceStore
//        this.core = new Core(71);
        this.iCore = new InstanceCore(iNum);
        
        try {
            runnerMachine.instanceExecutorService.execute(this); // schedules call to this.run(); this is the full execution of the specified test instance
        } catch (Exception e) {
            System.out.println("InstanceTask constructor failed for instance number " + iNum + ", with Exception " + e);
            throw e;
        }
    }

    /**
     * Execute one test instance
     * 
     * @note Efforts to exit this thread early require cooperation of whatever code this method calls.
     */
    @Override
    public void run() {
        // this thread can block for possibly days at a time while waiting for the test run to return a test result and otherwise complete itself
        System.out.println( "InstanceTask.run() opens instance number " + iNum);
        
        while(true) {
            InstanceState iState = runnerMachine.runnerService.actionStore.get(iNum);
            Action action = iState.getInstanceAction();
            Action nextAction = action.act(iState, iCore, runnerMachine.runnerService);
            System.out.println("InstanceTask.run() ran Action " + action.toString() + " for instance number " + iNum + ", finds next action to be " + nextAction.toString());
            if (nextAction == Action.DISCARDED)
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        System.out.println( "InstanceTask.run() closes instance number " + iNum);
    }
    
}
