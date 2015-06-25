package com.pslcl.qa.platform.process;

import com.pslcl.qa.platform.Core;
import com.pslcl.qa.platform.RunnerService;

/**
 * 
 * 
 * @note This is static code, with no variables
 */
public enum Action implements Actions {
    // note: enums are static
    
    // See at bottom: abstract Action act(InstanceState is);
  
//    UNKNOWN {
//        Action act(InstanceState iState) {
//            long iNum = iState.getInstanceNumber();
//            return this;
//        }
//    },

    INITIALIZE_INSTANCE {
        Action act(InstanceState iState, Core core, RunnerService runnerService) {
            long iNum = iState.getInstanceNumber();

            Action retAction = iState.getInstanceAction();
            if (retAction == INITIALIZE_INSTANCE) {
                iState.setInstanceAction(ANALYZE);
                // put new iState object to ActionStore as kvp iNum/iState
                runnerService.runnerMachine.addNewInstance(iNum, iState);
            } else {
                System.out.println("Internal Error. Action.INITIALIZE_INSTANCE() rejects iState for instance number " + iNum + ". Action wrongly shown to be " + retAction);
            }
            
            // This return value for INITIALIZE_INSTANCE is only for form; the separate ActionPipe mechanism can be moving iState forward right now, even dramatically. 
            return retAction;
        }
    },
    
    ANALYZE {
        Action act(InstanceState iState, Core core, RunnerService runnerService) {
            long iNum = iState.getInstanceNumber();
            
            // TODO. Determine a priority
            // int priority = determineAndStorePriority(iNum, iState);
            iState.setInstanceAction(DO);
            return iState.getInstanceAction();
        }
    },
    
    DO {
        Action act(InstanceState iState, Core core, RunnerService runnerService) {
            long iNum = iState.getInstanceNumber();
            core.executeTestInstance(iNum);
            iState.setInstanceAction(REMOVE);
            return iState.getInstanceAction();
        }
    },
    
//  STORE_RESULT,
    
    REMOVE {
        Action act(InstanceState iState, Core core, RunnerService runnerService) {
            long iNum = iState.getInstanceNumber();
            runnerService.actionStore.remove(iNum);
            iState.setInstanceAction(DISCARDED);
            return iState.getInstanceAction();
        }
    },

    // if called, remove iState from actionStore; try to code in a way that "DISCARDED" is never called
    DISCARDED {
        Action act(InstanceState iState, Core core, RunnerService runnerService) {
            long iNum = iState.getInstanceNumber();
            runnerService.actionStore.remove(iNum);
            return iState.getInstanceAction();
        }
    };

    abstract Action act(InstanceState is, Core core, RunnerService runnerService);
    
}