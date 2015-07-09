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
        Action act(TemplateState tState, TemplateCore tCore, RunnerService runnerService) {
            long tNum = tState.getTemplateNumber();

            Action retAction = tState.getAction();
            if (retAction == INITIALIZE) {
                tState.setAction(ANALYZE);
                // put new tState object to ActionStore as kvp tNum/tState
                runnerService.runnerMachine.addNewTemplate(tNum, tState);
            } else {
                System.out.println("Internal Error. Action.INITIALIZE() rejects tState for template number " + tNum + ". Action wrongly shown to be " + retAction);
            }
            
            // This return value for INITIALIZE is only for form; the separate ActionPipe mechanism can be moving tState forward right now, even dramatically. 
            return retAction;
        }
    },
    
    ANALYZE {
        Action act(TemplateState tState, TemplateCore tCore, RunnerService runnerService) {
            long tNum = tState.getTemplateNumber();
            
            // TODO. Determine a priority
            // int priority = determineAndStorePriority(iNum, tState);
            // TODO: assess other things, also
            tState.setAction(DO);
            return tState.getAction();
        }
    },
    
    DO {
        Action act(TemplateState tState, TemplateCore tCore, RunnerService runnerService) {
            long tNum = tState.getTemplateNumber();
            tCore.processTemplate(tNum,  tCore);
            tState.setAction(REMOVE);
            return tState.getAction();
        }
    },
    
//  STORE_RESULT,
    
    REMOVE {
        Action act(TemplateState tState, TemplateCore tCore, RunnerService runnerService) {
            long tNum = tState.getTemplateNumber();
            runnerService.actionStore.remove(tNum);
            tState.setAction(DISCARDED);
            return tState.getAction();
        }
    },

    // if called, remove tState from actionStore; try to code in a way that "DISCARDED" is never called
    DISCARDED {
        Action act(TemplateState tState, TemplateCore tCore, RunnerService runnerService) {
            long tNum = tState.getTemplateNumber();
            runnerService.actionStore.remove(tNum);
            return tState.getAction();
        }
    };

    abstract Action act(TemplateState ts, TemplateCore tCore, RunnerService runnerService);
    
}