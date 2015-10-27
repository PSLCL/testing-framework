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

import com.pslcl.dtf.runner.RunnerService;
import com.pslcl.dtf.runner.template.TemplateProvider;

/**
 * 
 * 
 * @note This is static code, with no variables
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
        Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();
            Action retAction = reState.getAction();
            if (retAction == INITIALIZE) {
                reState.setAction(ANALYZE);
                // put new reState object to ActionStore as kvp reNum/reState
                runnerService.runnerMachine.engageNewRunEntry(reNum, reState);
            } else {
                System.out.println("Internal Error. Action.INITIALIZE() rejects reState for reNum " + reNum + ". Action wrongly shown to be " + retAction);
            }
            
            // This return value for INITIALIZE is only for form; the separate ActionPipe mechanism can be moving reState forward right now, even dramatically. 
            return retAction;
        }
    },
    
    ANALYZE {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();
            
            // DECISION: We could check and prove that pk_run=reNum of table run has null for its result field. But we instead require that this be handled in whatever process placed reNum into the message queue. A human could manually place an reNum, and we will make a test run out of it.
            
            // TODO. Determine a priority
            // int priority = determineAndStorePriority(reNum, reState);
            // TODO: assess other things, feed this run entry to the prioritizer. The prioritizer could be a new state between ANAYZE and DO.
            reState.setAction(DO);
            return reState.getAction();
        }
    },
    
//  PRIORITIZE,    // useful here?    
    
    DO {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();
            // this is a new run entry: before returning, initiate a test run, process it to completion, gather result if available, and store the result
            boolean result = false;
            try {
                result = reCore.testRun(new Long(reNum), tp);
            } catch (Exception e) {
                System.out.println("Action.DO() see testRun() exception: " + e);
            }
            
            // TODO: store the result, perhaps with new enum value STORE_RESULT
            
            reState.setAction(REMOVE);
            return reState.getAction();
        }
    },
    
//  STORE_RESULT,   // useful here?
    
    REMOVE {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();
            System.out.println("Action.REMOVE() removes reNum " + reNum);
            runnerService.actionStore.remove(reNum);
            reState.setAction(DISCARDED);
            return reState.getAction();
        }
    },

    // if called, remove tState from actionStore; try to code in a way that "DISCARDED" is never called
    DISCARDED {
        @Override
        Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService) {
            long reNum = reState.getRunEntryNumber();runnerService.actionStore.remove(reNum);
            return reState.getAction();
        }
    };

    abstract Action act(RunEntryState reState, RunEntryCore reCore, TemplateProvider tp, RunnerService runnerService);
    
}