package com.pslcl.qa.runner.process;

public class DescribedTemplateState {
    
    private final long dtNum;
    private Object message;
    private Action action;
    
    /**
     * Constructor
     * @param dtNum
     * @param message Opaque object used eventually to ack the templateNumber held in the QueueStore
     */
    public DescribedTemplateState(long dtNum, Object message) {
        this.dtNum = dtNum;
        this.message = message;
        this.action = Action.INITIALIZE;
    }

    public long getDescribedTemplateNumber() {
        return dtNum;
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
