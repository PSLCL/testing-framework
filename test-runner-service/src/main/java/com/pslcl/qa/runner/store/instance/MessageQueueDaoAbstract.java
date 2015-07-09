package com.pslcl.qa.runner.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

import com.pslcl.qa.runner.RunnerService;

/**
 * This DAO layer knows about JMS. Using this DAO enforces JMS compatibility.
 * 
 *
 */
public abstract class MessageQueueDaoAbstract implements QueueStoreDao {
    private final RunnerService runnerService;
    
    public MessageQueueDaoAbstract(RunnerService runnerService) {
        this.runnerService = runnerService;
    }
    
    /**
     * @note if return true, must eventually call ack. TODO: how to call ack through this abstract class? Or is that appropriate, since we have an interface?
     * @param strQueueStoreEntryNumber
     * @param message
     * @return
     */
    boolean submitQueueStoreNumber(String strQueueStoreEntryNumber, Message message) throws Exception {
        try {
            runnerService.submitQueueStoreNumber(strQueueStoreEntryNumber, message);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void ackQueueStoreEntry(Message message) throws JMSException {
        // this call does the actual work
        message.acknowledge();
    }
}
