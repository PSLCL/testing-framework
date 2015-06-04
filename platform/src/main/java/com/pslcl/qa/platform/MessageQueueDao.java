package com.pslcl.qa.platform;

import javax.jms.JMSException;

public interface MessageQueueDao {
    
    void connect() throws JMSException;
    boolean queueExists() throws JMSException;
    void engage() throws JMSException;
    void setInstanceEntry(long instanceNumber) throws JMSException;
    void ackInstanceEntry(long instanceNumber) throws JMSException;

}
