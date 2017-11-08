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

import com.pslcl.dtf.runner.RunnerService;

public class RunEntryTask implements Runnable {

    // instance declarations

    private RunnerMachine runnerMachine;
    private RunEntryCore reCore;
    private long reNum;
    //private String runInstanceThreadName;
    private final Logger log;
    private final String simpleName;


    // public class methods

    /**
     * Constructor: From a given run entry number, initiate execution of its test run.
     *
     * Note: Submit reNum to the testRun executor service for future execution.
     * Note: Every thread or task created by this class closes itself automatically, whether seconds or days from instantiation.
     * @param runnerMachine The RunnerMachine
     * @param reNum The run entry number
     * @throws Exception on any error
     */
    public RunEntryTask(RunnerMachine runnerMachine, long reNum) throws Exception {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.runnerMachine = runnerMachine;
        this.reNum = reNum;
        //this.runInstanceThreadName = new String("runEntry " + reNum);
        this.reCore = new RunEntryCore(this.runnerMachine.getDBConnPool(), reNum);
        // the above fills in column ready_time of our db run table row, unless ready_time was already filled

        try {
            // schedule call to .run(), which initiates .testRun(); this is the full execution of the specified test run
            runnerMachine.getConfig().blockingExecutor.execute(this);
        } catch (Exception e) {
            log.error(simpleName + "constructor failed for reNum " + reNum + ", with Exception " + e);
            throw e;
        }
    }

    /**
     * Execute one test run
     *
     * Note: Efforts to exit this thread early require cooperation of whatever code this method calls.
     */
    @Override
    public void run() {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("RunEntryTask");
        // this thread blocks while waiting for the test run to return a test result and otherwise complete itself (if needed, block could be for days at a time)
        log.debug(simpleName + "run() opens reNum " + reNum);
        RunnerService rs = this.runnerMachine.getService();
        RunEntryStateStore ress = rs.runEntryStateStore;
        RunEntryState reState = ress.get(reNum);

        while(true) {
            try {
                Action newAction = reState.getNewAction(); // blocks until action changes to something new
                // goal is to get to Action.TESTRUN, which calls RunEntryCore.testRun()
                Action nextAction = newAction.act(reState, reCore, runnerMachine.getService());
                log.debug(simpleName + "run() ran Action " + newAction.toString() + " for reNum " + reNum +
                                       ", finds next action " + nextAction.toString());
                if (nextAction == Action.DISCARDED)
                    break; // close thread
            } catch (Throwable t) {
                log.warn(this.simpleName + "run() sees exception " + t.getMessage());
                break; // close thread
            }
        }

        log.debug(simpleName + "run() closes reNum " + reNum);
        Thread.currentThread().setName(tname);
    }

}