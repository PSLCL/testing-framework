package com.pslcl.qa.platform.process;

public class InstanceState {
    
    private final long instanceNumber;
    private Object message;
    private Action instanceAction;
    
    /**
     * Constructor
     * @param instanceNumber
     * @param message Opaque object used eventually to ack the instanceNumber held in the InstanceStore
     */
    public InstanceState(long instanceNumber, Object message) {
        this.instanceNumber = instanceNumber;
        this.message = message;
        this.instanceAction = Action.INITIALIZE_INSTANCE;
    }

    public long getInstanceNumber() {
        return instanceNumber;
    }
    
    public Object getMessage() {
        return message;
    }

    public Action getInstanceAction() {
        return instanceAction;
    }
    
    public void setInstanceAction(Action action) {
        instanceAction = action;
    }

}
