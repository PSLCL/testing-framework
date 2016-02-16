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
		if (this.running == true) {
			this.running = false;
			log.debug(this.simpleName + ".close() called to halt CancelTask for reNum " + this.reCore.getRENum());
			synchronized (this.syncObject) {
				this.syncObject.notifyAll();
			}
		}
	}
	
	@Override
	public void run() {
		while (running) {
			// check run table for our reNum; all cancel action is handled in this call
			this.reCore.checkForRunCancel();
			
			synchronized (this.syncObject) {
				try {
					if (running)
						syncObject.wait(CancelTask.SLEEPTIME);
				} catch (InterruptedException e) {
					log.debug(this.simpleName + ".run() exits as test run concludes, with exception msg: " + e.getMessage());
					break;
				}
			} // end synchronized()
			
//			try {
//				Thread.sleep(CancelTask.SLEEPTIME);
//			} catch (InterruptedException e) {
//				log.debug(this.simpleName + ".run() sleep exits with exception msg: " + e.getMessage());
//				break;
//			}
		}
		log.debug(this.simpleName + "TERMINATES run-cancel checking for reNum " + this.reCore.getRENum());
	}

}
