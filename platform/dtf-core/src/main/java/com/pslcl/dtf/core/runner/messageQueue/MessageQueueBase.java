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