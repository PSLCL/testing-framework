package com.pslcl.qa.runner.process;


public class RunEntryTask implements Runnable {
    private RunnerMachine runnerMachine;
    private RunEntryCore reCore;
    private long reNum;
    private String runInstanceThreadName;

    /**
     * Constructor: From a given run entry number, initiate execution of its test run.
     * 
     * @note Step 1 Check to see if first time setup is accomplished; if not, setup our testrunExecutor.
     * @note Step 2 Submit this test instance to the testRunExecutorService for future execution.
     * @note Every thread or task created by this class closes itself automatically, whether seconds or days from instantiation.
     * @param core The database and business logic class
     * @param testInstanceNumber identifies test instance to execute
     */
    public RunEntryTask(RunnerMachine runnerMachine, long reNum) throws Exception {
        this.runnerMachine = runnerMachine;
        this.reNum = reNum;
        this.runInstanceThreadName = new String("runEntry " + reNum);
        this.reCore = new RunEntryCore(reNum);
        
        try {
            runnerMachine.getConfig().getBlockingExecutor().execute(this); // schedules call to this.run(); this is the full execution of the specified test run
        } catch (Exception e) {
            System.out.println("RunEntryTask constructor failed for reNum " + reNum + ", with Exception " + e);
            throw e;
        }
    }

    /**
     * Execute one test run
     * 
     * @note Efforts to exit this thread early require cooperation of whatever code this method calls.
     */
    @Override
    public void run() {
        // this thread can block for possibly days at a time while waiting for the test run to return a test result and otherwise complete itself
        System.out.println( "RunEntryTask.run() opens reNum " + reNum);
        
        while(true) {
            RunEntryState reState = runnerMachine.getService().actionStore.get(reNum);
            Action action = reState.getAction();
            Action nextAction = action.act(reState, reCore, runnerMachine.getTemplateProvider(), runnerMachine.getService());
            System.out.println("RunEntryTask.run() ran Action " + action.toString() + " for reNum " + reNum + ", finds next action " + nextAction.toString());
            if (nextAction == Action.DISCARDED)
                break; // close thread
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        System.out.println( "RunEntryTask.run() closes reNum " + reNum);
    }
    
}
