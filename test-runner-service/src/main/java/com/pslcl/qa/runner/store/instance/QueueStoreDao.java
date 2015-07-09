package com.pslcl.qa.runner.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

public interface QueueStoreDao {
    
    /**
     * 
     *  @throws JMSException
     */
    void connect() throws JMSException;
    
    /**
     * 
     *  @throws JMSException
     */
    boolean queueStoreExists() throws JMSException;
    
    /**
     *  @note Either initQueueStoreSet or initQueueStoreGet must be implemented.
     *  @note Cleanup accomplished with cleanupQueueStoreAccess.   
     *  @throws JMSException
     */
    void initQueueStoreSet() throws JMSException;

    /**
     *  @note associated with initQueueStoreSet  
     *  @throws JMSException
     */
    void setQueueStoreEntry(long queueStoreEntryNumber) throws JMSException;
    
    /**
     *  @note Either initQueueStoreSet or initQueueStoreGet must be implemented.   
     *  @note Cleanup accomplished with cleanupQueueStoreAccess.   
     *  @throws JMSException
     */
    void initQueueStoreGet() throws JMSException;
    
    /**
     *  @note Either ackQueueStoreEntry(String) or ackQueueStoreEntry(Message) must be implemented
     *  @note associated with initQueueStoreGet
     *  @throws JMSException
     */
    void ackQueueStoreEntry(String jmsMessageID) throws JMSException;

    /**
     *  @note Either ackQueueStoreEntry(String) or ackQueueStoreEntry(Message) must be implemented
     *  @note associated with initQueueStoreGet
     *  @throws JMSException
     */
    void ackQueueStoreEntry(Message message) throws JMSException;
    
    /**
     * 
     *  @throws JMSException
     */
    void cleanupQueueStoreAccess() throws JMSException;
    
}
