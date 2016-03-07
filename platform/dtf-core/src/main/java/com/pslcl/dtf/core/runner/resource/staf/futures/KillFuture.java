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
package com.pslcl.dtf.core.runner.resource.staf.futures;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.runner.resource.staf.StopResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class KillFuture implements Callable<Integer>
{
    private final Logger log;
    private final StafRunnableProgram runnableProgram;

    public KillFuture(StafRunnableProgram runnableProgram)
    {
        this.runnableProgram = runnableProgram;
        log = LoggerFactory.getLogger(getClass());
        if (log.isDebugEnabled())
        {
            TabToLevel format = new TabToLevel();
            format.ttl(getClass().getSimpleName());
            format.level.incrementAndGet();
            runnableProgram.toString(format);
            LoggerFactory.getLogger(getClass()).debug(format.toString());
        }
    }

    @Override
    public Integer call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("KillFuture");
        StopResult result = StafSupport.processStop(runnableProgram);
        runnableProgram.setStopped(result);
        Thread.currentThread().setName(tname);
        return result.ccode;
    }
}