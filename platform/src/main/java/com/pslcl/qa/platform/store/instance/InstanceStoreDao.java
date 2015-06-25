package com.pslcl.qa.platform.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;

public interface InstanceStoreDao {
    
    /**
     * 
     *  @throws JMSException
     */
    void connect() throws JMSException;
    
    /**
     * 
     *  @throws JMSException
     */
    boolean instanceStoreExists() throws JMSException;
    
    /**
     *  @note Either initInstanceStoreSet or initInstanceStoreGet must be implemented.
     *  @note Cleanup accomplished with cleanupInstanceStoreAccess.   
     *  @throws JMSException
     */
    void initInstanceSet() throws JMSException;

    /**
     *  @note associated with initInstanceSet  
     *  @throws JMSException
     */
    void setInstanceEntry(long instanceNumber) throws JMSException;
    
    /**
     *  @note Either initInstanceStoreSet or initInstanceStoreGet must be implemented.   
     *  @note Cleanup accomplished with cleanupInstanceStoreAccess.   
     *  @throws JMSException
     */
    void initInstanceStoreGet() throws JMSException;
    
    /**
     *  @note Either ackInstanceEntry(String) or ackInstanceEntry(Message) must be implemented
     *  @note associated with initInstanceGet
     *  @throws JMSException
     */
    void ackInstanceEntry(String jmsMessageID) throws JMSException;

    /**
     *  @note Either ackInstanceEntry(String) or ackInstanceEntry(Message) must be implemented
     *  @note associated with initInstanceGet
     *  @throws JMSException
     */
    void ackInstanceEntry(Message message) throws JMSException;
    
    /**
     * 
     *  @throws JMSException
     */
    void cleanupInstanceStoreAccess() throws JMSException;
    
}
