package com.pslcl.dtf.runner.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CancelTask implements Runnable {
    private static final int SLEEPTIME = 1000 * 8; // 8 seconds
    
    private final Logger log;
    private final String simpleName;
    private final RunEntryCore reCore;
    private boolean running = true;
    private Object syncObject;
    
    CancelTask(RunEntryCore reCore, RunnerMachine runnerMachine) {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.reCore = reCore;
    
        this.syncObject = new Object();
        
        try {
            runnerMachine.getConfig().blockingExecutor.execute(this); // schedules call to this.run(), the actual cancel task
        } catch (Exception e) {
            log.error(simpleName + "constructor failed for reNum " + this.reCore.getRENum() + ", with Exception " + e);
            throw e;
        }       
    }
    
    void close() {
        synchronized (this.syncObject) {
            if (this.running) {
                this.running = false;
                this.syncObject.notifyAll();
            }
        } // end synchronized()
    }
    
    @Override
    public void run() {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("CancelTask");
        while (true) {
            synchronized (this.syncObject) {
                if (!this.running)
                    break; // java ok to break out of synchronized block this way
            
                // check run table for our reNum; all cancel action is set up in this call, or at least scheduled
                this.reCore.checkRunCancel();
            
                try {
                    if (this.running)
                        syncObject.wait(CancelTask.SLEEPTIME);
                } catch (InterruptedException e) {
                    log.debug(this.simpleName + ".run() exits with exception msg: " + e.getMessage());
                    break;
                }
            } // end synchronized()
        }
        log.debug(this.simpleName + "TERMINATES run-cancel checking for reNum " + this.reCore.getRENum());
        Thread.currentThread().setName(tname); // overwrite "CancelTask"
    }

}