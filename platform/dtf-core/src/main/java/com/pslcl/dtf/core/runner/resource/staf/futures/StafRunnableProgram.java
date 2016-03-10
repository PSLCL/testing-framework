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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.ProcessResult;
import com.pslcl.dtf.core.runner.resource.staf.StopResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class StafRunnableProgram implements RunnableProgram
{
    public final static String ProcessQueryHandle = "query handle ";
    public final static String ProcessStopHandle = "stop handle ";
    public final static String ProcessFreeHandle = "free handle ";
    
    public final ProcessResult result;
    private final ProcessCommandData commandData;
    private ExecutorService executor;
    private boolean stopped;
    
    public StafRunnableProgram(ProcessCommandData commandData) throws Exception
    {
    	this(null, commandData);
    }
    
    public StafRunnableProgram(STAFResult result, ProcessCommandData commandData) throws Exception
    {
        this.commandData = commandData;
        if(result == null)
            this.result = null;
        else
            this.result = new ProcessResult(result, commandData.isWait());
    }
    
    public synchronized boolean isStopped()
    {
        return stopped;
    }

    public synchronized void setStopped(StopResult result)
    {
        this.stopped = true;
        this.result.setStopResult(result);
    }

    public synchronized void setExecutorService(ExecutorService executor)
    {
        this.executor = executor;
    }

    public synchronized ExecutorService getExecutorService()
    {
        return executor;
    }

    public synchronized String getProcessQueryCommand()
    {
        StringBuilder cmd = new StringBuilder(ProcessQueryHandle)
                        .append(" ")
                        .append(getProcessHandle());
        return cmd.toString();
    }
    
    public synchronized String getProcessStopCommand()
    {
        StringBuilder cmd = new StringBuilder(ProcessStopHandle)
                        .append(" ")
                        .append(getProcessHandle());
        return cmd.toString();
    }
    
    public synchronized String getProcessFreeCommand()
    {
        StringBuilder cmd = new StringBuilder(ProcessFreeHandle)
                        .append(" ")
                        .append(getProcessHandle());
        return cmd.toString();
    }
    
    public ProcessResult getResult()
    {
        return result;
    }


    public ProcessCommandData getCommandData()
    {
        return commandData;
    }


    @Override
    public Future<Integer> kill()
    {
        return executor.submit(new KillFuture(this));
    }

    @Override
    public synchronized boolean isRunning()
    {
        //TODO: poll or notify? needs added here
        return !commandData.isWait() && !stopped;
    }

    @Override
    public synchronized Integer getRunResult()
    {
        if(isRunning())
            return null;
        //TODO: when runforever with kill for stop but get "start" ccode ... this is broken here
        return result.getServiceCcode();
    }
    
    public synchronized String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }
    
    public String getProcessHandle()
    {
        if(commandData.isWait())
            return null;
        return result.getServiceHandle();
    }
    
    public synchronized TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName() + ":");
        format.level.incrementAndGet();
        format.ttl("running = ", isRunning());
        if(result != null)
            result.toString(format);
        commandData.toString(format);
        format.level.decrementAndGet();
        return format;
    }
}
