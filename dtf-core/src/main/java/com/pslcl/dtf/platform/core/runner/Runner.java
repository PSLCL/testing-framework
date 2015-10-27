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
package com.pslcl.dtf.platform.core.runner;

import java.lang.Thread.UncaughtExceptionHandler;

import javax.jms.Message;

import org.apache.commons.daemon.Daemon;

import com.pslcl.dtf.platform.core.runner.config.RunnerConfig;

public interface Runner extends Daemon, UncaughtExceptionHandler
{
    public RunnerConfig getConfig();

    /**
     * 
     * @param strRunEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).
     * @param message JMS message associated with reNum, used for eventual message ack
     * @throws Exception
     */
    public void submitQueueStoreNumber(String strRunEntryNumber, Message message) throws Exception;

    /**
     * Test pipeline backdoor
     * @return a RunnerMachine object;
     */
    public Object getRunnerMachine();
}