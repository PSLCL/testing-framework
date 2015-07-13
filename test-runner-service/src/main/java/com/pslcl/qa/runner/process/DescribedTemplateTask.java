package com.pslcl.qa.runner.process;


public class DescribedTemplateTask implements Runnable {
    private RunnerMachine runnerMachine;
    private DescribedTemplateCore tCore;
    private long dtNum;
    private String instanceThreadName;

    /**
     * Constructor: From a given described template number, initiate execution of its corresponding test instance (aka test run).
     * 
     * @note Step 1 Check to see if first time setup is accomplished; if not, setup our testrunExecutor.
     * @note Step 2 Submit this test instance to the testRunExecutorService for future execution.
     * @note Every thread or task created by this class closes itself automatically, whether seconds or days from instantiation.
     * @param core The database and business logic class
     * @param testInstanceNumber identifies test instance to execute
     */
    public DescribedTemplateTask(RunnerMachine runnerMachine, long dtNum) throws Exception {
        this.runnerMachine = runnerMachine;
        this.dtNum = dtNum;
        this.instanceThreadName = new String("instance " + dtNum);
        this.tCore = new DescribedTemplateCore(dtNum);
        
        try {
            runnerMachine.templateExecutorService.execute(this); // schedules call to this.run(); this is the full execution of the specified test instance
        } catch (Exception e) {
            System.out.println("InstanceTask constructor failed for instance number " + dtNum + ", with Exception " + e);
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
        System.out.println( "InstanceTask.run() opens instance number " + dtNum);
        
        while(true) {
            DescribedTemplateState iState = runnerMachine.runnerService.actionStore.get(dtNum);
            Action action = iState.getAction();
            Action nextAction = action.act(iState, tCore, runnerMachine.runnerService);
            System.out.println("InstanceTask.run() ran Action " + action.toString() + " for instance number " + dtNum + ", finds next action to be " + nextAction.toString());
            if (nextAction == Action.DISCARDED)
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        System.out.println( "DescribedTemplateTask.run() closes instance number " + dtNum);
    }
    
}
