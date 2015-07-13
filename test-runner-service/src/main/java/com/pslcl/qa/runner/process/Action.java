package com.pslcl.qa.runner.process;

import com.pslcl.qa.runner.RunnerService;

/**
 * 
 * 
 * @note This is static code, with no variables
 */
public enum Action implements Actions {
    // note: enums are static
    
    // See at bottom: abstract Action act(TemplateState ts);
  
//    UNKNOWN {
//        Action act(TemplateState tState) {
//            long tNum = tState.getTemplateNumber();
//            return this;
//        }
//    },

    INITIALIZE {
        Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService) {
            long dtNum = dtState.getDescribedTemplateNumber();

            Action retAction = dtState.getAction();
            if (retAction == INITIALIZE) {
                dtState.setAction(ANALYZE);
                // put new tState object to ActionStore as kvp tNum/tState
                runnerService.runnerMachine.addNewTemplate(dtNum, dtState);
            } else {
                System.out.println("Internal Error. Action.INITIALIZE() rejects dtState for dtNum " + dtNum + ". Action wrongly shown to be " + retAction);
            }
            
            // This return value for INITIALIZE is only for form; the separate ActionPipe mechanism can be moving tState forward right now, even dramatically. 
            return retAction;
        }
    },
    
    ANALYZE {
        Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService) {
            long dtNum = dtState.getDescribedTemplateNumber();
            
            // TODO. Determine a priority
            // int priority = determineAndStorePriority(dtNum, dtState);
            // TODO: assess other things, also
            dtState.setAction(DO);
            return dtState.getAction();
        }
    },
    
    DO {
        Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService) {
            long dtNum = dtState.getDescribedTemplateNumber();
            dtCore.processTemplate(dtNum, dtCore);
            dtState.setAction(REMOVE);
            return dtState.getAction();
        }
    },
    
//  STORE_RESULT,
    
    REMOVE {
        Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService) {
            long dtNum = dtState.getDescribedTemplateNumber();
            runnerService.actionStore.remove(dtNum);
            dtState.setAction(DISCARDED);
            return dtState.getAction();
        }
    },

    // if called, remove tState from actionStore; try to code in a way that "DISCARDED" is never called
    DISCARDED {
        Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService) {
            long dtNum = dtState.getDescribedTemplateNumber();
            runnerService.actionStore.remove(dtNum);
            return dtState.getAction();
        }
    };

    abstract Action act(DescribedTemplateState dtState, DescribedTemplateCore dtCore, RunnerService runnerService);
    
}