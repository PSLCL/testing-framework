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

/**
 *
 *
 * Note: This is static code, with no variables
 */
public enum Action implements Actions {
    // note: enums are static

    // See at bottom: abstract Action act(TemplateState ts);

//    UNKNOWN {
//        Action act(RunEntryState reState, . . .) {
//            long reNum = reState.getRunEntryNumber();
//            return this;
//        }
//    },

    INITIALIZE {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) throws Exception {
            long reNum = reState.getRunEntryNumber();
            Action retAction = reState.getAction();
            if (retAction == INITIALIZE) {
                try {
                    // put new reState object to ActionStore as kvp reNum/reState
                    runnerService.runnerMachine.engageRunEntry(reNum, reState);
                    reState.setAction(ANALYZE);
                } catch (Exception e) {
                    log.warn("Action.INITIALIZE() sees exception message " + e, e);
                    throw e;
                }
            } else {
                System.out.println("Internal Error. Action.INITIALIZE() rejects reState for reNum " + reNum + ". Action wrongly shown to be " + retAction);
            }

            // This return value for INITIALIZE is only for form; the separate ActionPipe mechanism can be moving reState forward right now, even dramatically.
            return retAction;
        }
    },

    ANALYZE {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) {
            /*long reNum =*/ reState.getRunEntryNumber();

            // TODO. Determine a priority
            // int priority = determineAndStorePriority(reNum, reState);
            // TODO: assess other things, feed this run entry to the prioritizer. The prioritizer could be a new state between ANAYZE and DO.
            reState.setAction(TESTRUN);
            return reState.getAction();
        }
    },

//  PRIORITIZE,    // useful here?

    TESTRUN {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) {
            try {
                // initiate a test run, then block until the test run completes, its result is gathered, and its result is stored
                reCore.testRun(runnerService.runnerMachine);
            } catch (Throwable t) {
                log.warn("Action.TESTRUN() sees testRun() throwable: " + t, t);
            }
            // throwable or not, we remove reNum from active consideration- its test result is stored, UNLESS:
            //    - the test run is canceled. or
            //    - the test run is postponed, or
            //    - the throwable is from the database storage call tring to write the result.
            reState.setAction(REMOVE);
            return reState.getAction();
        }
    },

    REMOVE {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) {
            // Remove reNum from RunEntryStateStore, our map of run actions. Our message queue handling is separate.
            //    The presence or absence of any entry in our input message queue has nothing to do with this "REMOVE".
            // Note: Postponing a test run (only happens when a resource cannot currently be reserved) relies
            //       on the msg queue to present reNum again. We will ignore the re-presented reNum, however,
            //       until that reNum has been removed from our list of active run entries. Action.REMOVE does that.
            long reNum = reState.getRunEntryNumber();
            log.debug("Action.REMOVE() removes reNum " + reNum);

            // temporarily, comment out this line, to allow reNum to remain in the RunEntryState map, so next message queue pull, of the same reNum, will be bypassed
            runnerService.getRunnerMachine().disengageRunEntry(reNum);

            reState.setAction(DISCARDED);
            return reState.getAction();
        }
    },

    // if called, remove reState from actionStore; try to code in a way that "DISCARDED" is never called
    DISCARDED {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();

            // temporarily, comment out this line, to allow reNum to remain in the RunEntryState map, so next message queue pull, of the same reNum, will be bypassed
            runnerService.getRunnerMachine().disengageRunEntry(reNum);

            return reState.getAction();
        }
    };

    abstract Action act(RunEntryState reState, RunEntryCore reCore, RunnerService runnerService) throws Exception;


    private static Logger log = LoggerFactory.getLogger(Action.class);

}