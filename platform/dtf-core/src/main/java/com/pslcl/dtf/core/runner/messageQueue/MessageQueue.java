/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.core.runner.messageQueue;

import javax.jms.JMSException;
import javax.jms.Message;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

public interface MessageQueue
{
    /**
     * 
     *  @param config The RunnerConfig 
     *  @throws Exception on error
     */
    void init(RunnerConfig config) throws Exception;

    /**
     * 
     */
    void destroy();

    /**
     * 
     *  @return true for exists; false otherwise
     *  @throws JMSException on message queue related error
     */
    boolean queueStoreExists() throws JMSException;

    /**
     *  Note: Either initQueueStoreSet or initQueueStoreGet must be implemented.
     *  Note: Cleanup accomplished with cleanupQueueStoreAccess.   
     *  @throws JMSException  on message queue related error
     */
    void initQueueStoreSet() throws JMSException;

    /**
     *  Note: Associated with initQueueStoreSet
     *  @param queueStoreEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).   
     *  @throws JMSException on message queue related error
     */
    void setQueueStoreEntry(long queueStoreEntryNumber) throws JMSException;

    /**
     *  Note: Either initQueueStoreSet or initQueueStoreGet must be implemented.   
     *  Note: Cleanup accomplished with cleanupQueueStoreAccess.   
     *  @throws JMSException on message queue related error
     */
    void initQueueStoreGet() throws JMSException;

    /**
     *  Note: Either ackQueueStoreEntry(String) or ackQueueStoreEntry(Message) must be implemented
     *  Note: Associated with initQueueStoreGet
     *  @param jmsMessageID The messageID of the JMS message to ack
     *  @throws JMSException on message queue related error
     */
    void ackQueueStoreEntry(String jmsMessageID) throws JMSException;

    /**
     *  Note: Either ackQueueStoreEntry(String) or ackQueueStoreEntry(Message) must be implemented
     *  Note: associated with initQueueStoreGet
     *  @param message The JMS message to ack
     *  @throws JMSException on message queue related error
     */
    void ackQueueStoreEntry(Message message) throws JMSException;

    /**
     * 
     *  @throws JMSException on message queue related error
     */
    void cleanupQueueStoreAccess() throws JMSException;

}