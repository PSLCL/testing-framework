/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.runner.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunEntryTask implements Runnable {

	// instance declarations 

	private RunnerMachine runnerMachine;
    private RunEntryCore reCore;
    private long reNum;
    private String runInstanceThreadName;
    private final Logger log;
    private final String simpleName;
        
    
    // public class methods

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
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.runnerMachine = runnerMachine;
        this.reNum = reNum;
        this.runInstanceThreadName = new String("runEntry " + reNum);
        this.reCore = new RunEntryCore(new Long(reNum));
        
        try {
            runnerMachine.getConfig().blockingExecutor.execute(this); // schedules call to this.run(); this is the full execution of the specified test run
        } catch (Exception e) {
            log.error(simpleName + "constructor failed for reNum " + reNum + ", with Exception " + e);
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
        // this thread blocks while waiting for the test run to return a test result and otherwise complete itself (if needed, block could be for days at a time)
        log.debug(simpleName + "run() opens reNum " + reNum);
        
        while(true) {
            RunEntryState reState = runnerMachine.getService().runEntryStateStore.get(reNum);
            Action action = reState.getAction();
            Action nextAction;
			try {
				nextAction = action.act(reState, reCore, runnerMachine.getService());
                log.debug(simpleName + "run() ran Action " + action.toString() + " for reNum " + reNum + ", finds next action " + nextAction.toString());
	            if (nextAction == Action.DISCARDED)
	                break; // close thread
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
 
        log.debug(simpleName + "run() closes reNum " + reNum);
    }
    
}