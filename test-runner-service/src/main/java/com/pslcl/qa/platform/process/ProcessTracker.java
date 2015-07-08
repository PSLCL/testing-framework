package com.pslcl.qa.platform.process;

import java.util.concurrent.TimeoutException;

import com.pslcl.qa.platform.RunnerService;

public class ProcessTracker {
    
    private RunnerService runnerService = null;
    
    public ProcessTracker(RunnerService runnerService) {
        this.runnerService = runnerService;
    }
    
    /**
     * 
     * @note This is a static method, because there is only one database.
     * @note Timeout at five seconds by throwing TimeoutException
     * @param instanceNumber
     * @return
     * @throws Exception
     */
    public static boolean resultStored(long instanceNumber) throws TimeoutException, Exception {
        return false; // TODO
    }
    
    /**
     * 
     * @note Timeout at five seconds by throwing TimeoutException
     * @param instanceNumber
     * @return
     * @throws Exception
     */
    public boolean inProcess(long iNum) throws TimeoutException, Exception {
        // TODO: Expand this, perhaps. Shown is the basic truth: if result is not stored, then it is inProcess.
        //       But this simple truth ignores the case where progress is hopelessly blocked.
        //       So make a decision:
        //           Is there anything that can be added here that could fix up a blocked test instance, by replacing it with a new test instance?
        //           If not, the this single line of code is appropriate.
        return runnerService.actionStore.get(iNum) != null;
    }

}
