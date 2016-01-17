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
package com.pslcl.dtf.core.runner.resource.staf;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class StafRunnableProgram implements RunnableProgram
{
    private final Logger log;
    private Integer ccode;
    private boolean running;
    private final Object context;
    private final ProcessResult result;
    
    public StafRunnableProgram(STAFResult result, Object context) throws Exception
    {
        this(result, false, context);
    }
    
    public StafRunnableProgram(STAFResult result, boolean running, Object context) throws Exception
    {
        log = LoggerFactory.getLogger(getClass());
        this.ccode = ccode;
        this.running = running;
        this.context = context;
        this.result = new ProcessResult(result);
        if(!running)
            ccode = result.rc;
    }

    public Object getContext()
    {
        return context;
    }
    
    public synchronized void setRunResult(int ccode)
    {
        this.ccode = ccode;
    }
    
    public synchronized void setRunning(boolean running)
    {
        this.running = running;
    }
    
    
    @Override
    public Future<Integer> kill()
    {
        return null;
    }

    @Override
    public synchronized boolean isRunning()
    {
        return running;
    }

    @Override
    public synchronized Integer getRunResult()
    {
        return ccode;
    }
    
    public synchronized String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }
    
    public synchronized TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName() + ":");
        format.level.incrementAndGet();
        format.ttl("ccode = ", (ccode == null ? "unknown" : ccode.toString()));
        format.ttl("running = ", ""+running);
        format.level.incrementAndGet();
        result.toString(format);
        format.level.decrementAndGet();
        format.level.decrementAndGet();
        return format;
    }
}
