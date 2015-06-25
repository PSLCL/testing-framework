package com.pslcl.qa.platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class RunnerInstance implements Runnable {
   
    /**
     * Instantiate testrunExecutorService.
     * 
     * @note run() is called only once and blocks and does not exit until all test runner activity has completed
     *
     */
    private static class TestRunExecutorTask implements Runnable {

        /**
         * Specifies thread name and sets daemon true for the thread that instantiates testrunExecutorService
         */
        private class RunnerThreadFactory implements ThreadFactory {
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("testrunExecutorService");
                return t;
            }

        }
        
        @Override
        public void run() {
            synchronized (testrunExecutorSynchObj) {            
                System.out.println( "RunnerInstance.RunExecutorTask.run() initializes testrunExecutorService");
                ThreadFactory tFactory = new RunnerThreadFactory();
                testrunExecutorService = Executors.newCachedThreadPool(tFactory);
                testrunExecutorSynchObj.notifyAll(); // unblock constructor
                System.out.println( "RunnerInstance.RunExecutorTask.run() initialized testrunExecutorService, waits now for notification that testrunExecutorService is shutdown.");
                
                while (!allTasksComplete) {
                    try {
                        testrunExecutorSynchObj.wait(); // wait for notification from ExecutorShutdownTask
                    } catch (InterruptedException e) {
                        // exit anyway
                    }
                }
            } // end synchronized()
            System.out.println( "RunnerInstance.RunExecutorTask.run() learns testrunExecutorService has shutdown, and exits");
        }
        
    }
    
    /**
     * Shutdown task for the testrunExecutorService.
     * 
     * @note run() is called only once and blocks and does not exit until all every scheduled test run has completed
     */
    private static class ExecutorShutdownTask implements Runnable {
        
        @Override
        public void run() {
            // blocks until all tasks have completed execution
            System.out.println( "RunnerInstance.ExecutorShutdownTask.run() called to monitor completion of test runs and to setup future testrunExecutorService cleanup");
            if (testrunExecutorService != null) {
                testrunExecutorService.shutdown(); // allows existing tasks to run to completion, but accepts no new tasks
                try {
                    testrunExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // may be here for seconds or even days
                    // testrunExecutorService is now fully shutdown
                } catch (InterruptedException e) {
                    // fall through for orderly cleanup
                }
                System.out.println("RunnerInstance.ExecutorShutdownTask.run() sees all test runs tasks completed: " + testrunExecutorService.isTerminated());
                System.out.println("RunnerInstance.ExecutorShutdownTask.run() sees testrunExecutorService is shutdown: " + testrunExecutorService.isShutdown());
                
                // tell TestRunExecutorTask to exit
                allTasksComplete = true;
                synchronized (testrunExecutorSynchObj) {
                    testrunExecutorSynchObj.notifyAll();
                }
            } else {
                System.out.println( "RunnerInstance.ExecutorShutdownTask.run() finds testrunExecutorService not instantiated");
            }
            System.out.println( "RunnerInstance.ExecutorShutdownTask.run() exits. Manual runner activity has completed");
        }

    }
    
    /**
     * Setup detecting that every test run has completed, and setup final thread cleanup.
     * 
     * @note this call does not block
     * @throws InterruptedException
     */
    protected static void setupAutoThreadCleanup() throws InterruptedException {
        // this guard not needed if caller guarantees not to call more than once
        if (testrunExecutorTaskShutdownRequested == false) {
            testrunExecutorTaskShutdownRequested = true;
            ExecutorShutdownTask est = new ExecutorShutdownTask();
            Thread t = new Thread(est);
            t.start();
        }
    }

    private static final Object testrunExecutorSynchObj = new Object();
    private static ExecutorService testrunExecutorService = null;
    private static TestRunExecutorTask testrunExecutorTask = null;
    private static boolean testrunExecutorTaskShutdownRequested = false;
    private static boolean allTasksComplete = false;

    private final Core core;
    private final long testInstanceNumber;
    private String testrunThreadName;
    
    /**
     * Get name of this thread that executes the test instance of the given testInstanceNumber
     */
    public String getThreadtName() {
        return testrunThreadName;
    }

    /**
     * Constructor: From a given test instance number, initiate execution of the corresponding test instance (aka test run).
     * 
     * @note Step 1 Check to see if first time setup is accomplished; if not, setup our testrunExecutor.
     * @note Step 2 Submit this test instance to the testRunExecutorService for future execution.
     * @note Every thread or task created by this class closes itself automatically, whether seconds or days from instantiation.
     * @param core The database and business logic class
     * @param testInstanceNumber identifies test instance to execute
     */
    public RunnerInstance(Core core, long testInstanceNumber) throws Exception {

        // note: testrunExecutorSynchObj not required here if constructor is guaranteed to be called from only one thread
        synchronized (testrunExecutorSynchObj) {
            this.core = core;
            this.testInstanceNumber = testInstanceNumber;
            if (testrunExecutorTask == null) {
                // first time setup and brief delay; blocks follow on constructor calls
                // Init testrunExecutorTask to instantiate and hold testrunExecutorService; keeps testrunExecutorService alive until every test instance completes execution.
                testrunExecutorTask = new TestRunExecutorTask();
                Thread t = new Thread(testrunExecutorTask);
                t.start();
                while (testrunExecutorService == null) {
                    testrunExecutorSynchObj.wait();
                }
            }

            this.testrunThreadName = new String("testrunTaskInstanceNumber" + testInstanceNumber);
            try {
                testrunExecutorService.execute(this); // schedules call to this.run(); this is the full execution of the specified test instance
            } catch (Exception e) {
                System.out.println("RunnerInstance failed for testInstanceNumber " + testInstanceNumber + ", with Exception " + e);
                throw e;
            }
        } // end synchronized()
    }

    /**
     * Execute one test instance
     * 
     * @note Efforts to exit this thread early require cooperation of code core.executeTestInstance(), which this method calls.
     */
    @Override
    public void run() {
        // this thread can block for possibly days at a time while waiting for the test run to return a test result and otherwise complete itself
        System.out.println( "RunnerInstance.run() executes test instance number " + testInstanceNumber);
        core.executeTestInstance(testInstanceNumber);
        System.out.println( "RunnerInstance.run() exits for test instance number " + testInstanceNumber);
    }
    
}