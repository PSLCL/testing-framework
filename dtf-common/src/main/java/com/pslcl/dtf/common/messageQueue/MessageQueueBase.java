package com.pslcl.dtf.common.messageQueue;

import javax.jms.JMSException;
import javax.jms.Message;

import com.pslcl.dtf.common.config.RunnerConfig;

/**
 * This DAO layer knows about JMS. Using this DAO enforces JMS compatibility.
 * 
 *
 */
public abstract class MessageQueueBase implements MessageQueue
{
    private volatile RunnerConfig config;

    public MessageQueueBase()
    {
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        this.config = config;
    }

    /**
     * @note if return true, must eventually call ack. TODO: how to call ack through this abstract class? Or is that appropriate, since we have an interface?
     * @param strQueueStoreEntryNumber
     * @param message
     * @return
     */
    public boolean submitQueueStoreNumber(String strQueueStoreEntryNumber, Message message) throws Exception
    {
        try
        {
            config.runnerService.submitQueueStoreNumber(strQueueStoreEntryNumber, message);
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public void ackQueueStoreEntry(Message message) throws JMSException
    {
        // this call does the actual work
        message.acknowledge();
    }
}
