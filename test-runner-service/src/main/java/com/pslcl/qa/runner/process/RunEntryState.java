package com.pslcl.qa.runner.process;

public class RunEntryState {
    
    private final long reNum;
    private Object message;
    private Action action;
    
    /**
     * Constructor
     * @param reNum
     * @param message Opaque object used eventually to ack the templateNumber held in the QueueStore
     */
    public RunEntryState(long reNum, Object message) {
        this.reNum = reNum;
        this.message = message;
        this.action = Action.INITIALIZE;
    }

    public long getRunEntryNumber() {
        return reNum;
    }
    
    public Object getMessage() {
        return message;
    }

    public Action getAction() {
        return action;
    }
    
    public void setAction(Action action) {
        this.action = action;
    }

}
