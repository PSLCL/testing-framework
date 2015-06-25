package com.pslcl.qa.platform.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

import com.pslcl.qa.platform.RunnerService;

/**
 * This DAO layer knows about JMS. Using this DAO enforces behavior to be JMS compatible.
 * 
 *
 */
public abstract class MessageQueueDaoAbstract implements InstanceStoreDao {
    private final RunnerService runnerService;
    
    public MessageQueueDaoAbstract(RunnerService runnerService) {
        this.runnerService = runnerService;
    }
    
    /**
     * @note if return true, must eventually call ack. TODO: how to call ack through this abstract class? Or is that appropriate, since we have an interface?
     * @param hexStrInstanceNumber
     * @param message
     * @return
     */
    boolean submitInstanceNumber_Store(String hexStrInstanceNumber, Message message) throws Exception {
        try {
            runnerService.submitInstanceNumber_Store(hexStrInstanceNumber, message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void ackInstanceEntry(Message message) throws JMSException {
        // this call does the actual work
        message.acknowledge();
    }   
}
