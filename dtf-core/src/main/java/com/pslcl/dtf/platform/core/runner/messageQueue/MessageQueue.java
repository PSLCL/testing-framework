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
package com.pslcl.dtf.platform.core.runner.messageQueue;

import javax.jms.JMSException;
import javax.jms.Message;

import com.pslcl.dtf.platform.core.runner.config.RunnerConfig;

public interface MessageQueue
{
    /**
     * 
     *  @throws Exception
     */
    void init(RunnerConfig config) throws Exception;

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
