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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.QueryResult;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class RunFuture implements Callable<RunnableProgram>
{
    private final Logger log;
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final ExecutorService executor;
    private final boolean windows;
    private final long runId;
    private final Object context;
    
    public RunFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, boolean windows, long runId, Object context)
    {
        this(host, linuxSandbox, winSandbox, partialDestPath, null, windows, runId, context);
    }
    
    public RunFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, ExecutorService executor, boolean windows, long runId, Object context)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.executor = executor;
        this.windows = windows;
        this.runId = runId;
        this.context = context;
        log = LoggerFactory.getLogger(getClass());
        if (log.isDebugEnabled())
        {
            TabToLevel format = new TabToLevel();
            format.ttl(getClass().getSimpleName());
            format.level.incrementAndGet();
            format.ttl("host = ", host);
            format.ttl("linuxSandboxPath = ", linuxSandbox);
            format.ttl("winSandboxPath = ", winSandbox);
            format.ttl("partialDestPath = ", partialDestPath);
            format.ttl("executor = ", (executor == null ? "null" : executor.getClass().getName()));
            format.ttl("windows = ", windows);
            LoggerFactory.getLogger(getClass()).debug(format.toString());
        }
    }

    @Override
    public RunnableProgram call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("RunFuture");
        ProcessCommandData cmdData = DeployFuture.getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows, runId);
        cmdData.setHost(host);
        cmdData.setWait(executor == null);
        cmdData.setContext(context);
        cmdData.setUseWorkingDir(true);
        if (windows)
        {
            cmdData.setCommand(cmdData.getFileName());
            StafRunnableProgram runnableProgram = StafSupport.issueProcessShellRequest(cmdData);
            runnableProgram.setExecutorService(executor);
            if (executor != null)
                return runnableProgram;
            Thread.currentThread().setName(tname);
            return runnableProgram;
        }
        // linux
        cmdData.setCommand("./" + cmdData.getFileName());
        StafRunnableProgram runnableProgram = StafSupport.issueProcessShellRequest(cmdData);
        runnableProgram.setExecutorService(executor);
        Thread.currentThread().setName(tname);
        if (executor != null)
            return runnableProgram;
        //TODO: if we decide to support timeout
//        waitForComplete(runnableProgram, 20, 800);
        return runnableProgram;
    }

    @SuppressWarnings("unused")
    private void waitForComplete(StafRunnableProgram runnableProgram, int maxRetries, int maxDelay) throws Exception
    {
        int count = 0;
        int totalTime = 0;
        do
        {
            ++count;
            long delay = ((long) Math.pow(2, count) * 100L);
            delay = Math.min(delay, maxDelay);
            log.debug("count: " + count + " delay: " + delay + " totalwait: " + StrH.scaleMilliSeconds(totalTime));
            try
            {
                QueryResult result = StafSupport.processQuery(runnableProgram);
                if (!result.isRunning())
                    break;
                Thread.sleep(delay);
                totalTime += delay;
            } catch (InterruptedException e)
            {
                throw new Exception("Thread canceled", e);
            }
            if (count >= maxRetries - 1)
                throw new Exception("timed out");
        } while (true);
    }

    public static class TimeoutData
    {
        public final long totalTime;
        public final int maxRetries;
        public final int maxDelay;

        public TimeoutData(int maxRetries, int maxDelay, long totalTime)
        {
            this.maxRetries = maxRetries;
            this.maxDelay = maxDelay;
            this.totalTime = totalTime;
        }
        
        /**
         * Assumes a minimum of 3 retries.
         * <p>starting delay is 200ms, thus any sourceValue given under 1400ms will return count=3, max delay=1ms.
         * Similar errors will be seen through out the range with totalTimes exceeding the target by a fair amount.
         * i.e. 5 minutes will be 6.8 minutes  
         * @param value
         * @param targetUnit
         * @param targetMaxDelay
         * @param maxDelayUnit
         * @return
         */
        public static TimeoutData getTimeoutData(long targetValue, TimeUnit targetUnit, int targetMaxDelay, TimeUnit targetMaxDelayUnit, long minMs)
        {
            long targetMs = TimeUnit.MILLISECONDS.convert(targetValue, targetUnit);
            if (targetMs < 1401)
                return new TimeoutData(3, 1, 1400);
            long targetMaxDelayMs = TimeUnit.MILLISECONDS.convert(targetMaxDelay, targetMaxDelayUnit);
            long totalTime = 0;
            int count = -1;
            boolean done = false;
            do
            {
                ++count;
                long delay = targetMaxDelayMs;
                long nextDelay = 0;
                if(!done)
                {
                    delay = ((long) Math.pow(2, count) * minMs);
                    nextDelay = ((long) Math.pow(2, count + 1) * minMs);
                    if(delay > targetMaxDelayMs)
                    {
                        delay = targetMaxDelayMs;
                        done = true;
                    }
                }
//                long nextDelay = ((long) Math.pow(2, count + 1) * 100L);
//                delay = Math.min(delay, targetMaxDelayMs);
                totalTime += delay;
                long nextTotalTime = totalTime + nextDelay; 
                if(nextTotalTime >= targetMs -1)
                {
                    if(delay < targetMaxDelayMs)
                        targetMaxDelayMs = (int)delay;
                }
                if (totalTime >= targetMs - 1)
                    return new TimeoutData(count, (int) delay, totalTime);
            } while (true);
        }

        public String toString()
        {
            TabToLevel format = new TabToLevel();
            format.ttl("\n",getClass().getSimpleName(), ":");
            format.level.incrementAndGet();
            format.ttl("maxRetries: ", maxRetries);
            format.ttl("maxDelay: ", maxDelay, " (", StrH.scaleMilliSeconds(maxDelay), ")");
            format.ttl("totalTime: ", totalTime, " (",StrH.scaleMilliSeconds(totalTime), ")");
            return format.toString();
        }
    }
}