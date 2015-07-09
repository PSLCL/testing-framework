package com.pslcl.qa.runner.process;

public class TemplateState {
    
    private final long templateNumber;
    private Object message;
    private Action action;
    
    /**
     * Constructor
     * @param templateNumber
     * @param message Opaque object used eventually to ack the templateNumber held in the QueueStore
     */
    public TemplateState(long templateNumber, Object message) {
        this.templateNumber = templateNumber;
        this.message = message;
        this.action = Action.INITIALIZE;
    }

    public long getTemplateNumber() {
        return templateNumber;
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
