package com.pslcl.qa.runner.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.daemon.DaemonInitException;

import com.pslcl.qa.runner.config.RunnerServiceConfig;

public interface QueueStoreDao {
    
    /**
     * 
     *  @throws Exception
     */
    void init(RunnerServiceConfig config) throws Exception;
    
    /**
     * 
     */
    void destroy();
    /**
     * 
     *  @throws JMSException
     */
    //TODO: don't need anymore?
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
