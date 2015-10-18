package com.pslcl.qa.runner.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.daemon.DaemonInitException;

import com.pslcl.qa.runner.config.RunnerServiceConfig;

/**
 * This DAO layer knows about JMS. Using this DAO enforces JMS compatibility.
 * 
 *
 */
public abstract class MessageQueueDaoAbstract implements QueueStoreDao {
    private volatile RunnerServiceConfig config;
    
    public MessageQueueDaoAbstract() {
    }
    
    @Override
    public void init(RunnerServiceConfig config) throws Exception
    {
        this.config = config;
    }
    
    /**
     * @note if return true, must eventually call ack. TODO: how to call ack through this abstract class? Or is that appropriate, since we have an interface?
     * @param strQueueStoreEntryNumber
     * @param message
     * @return
     */
    public boolean submitQueueStoreNumber(String strQueueStoreEntryNumber, Message message) throws Exception {
        try {
            config.runnerService.submitQueueStoreNumber(strQueueStoreEntryNumber, message);
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
