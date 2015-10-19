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
package com.pslcl.dtf.common.config.executor;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.common.config.status.StatusTracker;

/**
 * An EMIT application friendly <code>ScheduledThreadPoolExecutor</code>.
 * </p>
 * Java's <code>java.util.Timer</code> has fallen in disfavor based on badly behaving timer tasks
 * being able to take down all tasks in the timer.  It is now suggested that a <code>ScheduledThreadPoolExecutor</code>
 * with at least two threads is used in the place of old timer.
 * </p>
 * Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 * create an instance of this class as a singleton object to the whole application and include its reference
 * in the <code>Service<T></code>'s configuration object, which in turn is typically made available to all 
 * subsystems of the application, where they can utilize this timer as needed.
 */
//@ThreadSafe
public class ScheduledExecutor extends ScheduledThreadPoolExecutor
{
    private static final int initialCorePoolSize = 2; 
    
    /**
     * The default constructor.
     * </p>
     * This call will initialize a usable ScheduledThreadPoolExecutor with a core size of 2 threads.
     * However, to enable the custom thread pool factory and status tracking extensions to 
     * the queue the user must call the <code>init</code> method before utilizing 
     * this thread pool.
     * @see #init(ExecutorQueueConfig) 
     *  
     */
    public ScheduledExecutor()
    {
        super(initialCorePoolSize);
    }

    /**
     * Initialize our custom extended functionality of the SceduledThreadPoolExecutor.
     * </p>
     * This will initialize the thread pool with our thread pool factory functionality, and 
     * status tracking functionality.
     * @param config the configuration parameters used to configure the extended queue functionality.
     */
    public void init(ExecutorQueueConfig config)
    {
        setCorePoolSize(config.getCorePoolSize());
        setThreadFactory(new TimerExecutorThreadFactory(config.getThreadNamePrefix(), config.getStatusTracker(), config.getStatusSubsystemName()));
        config.getStatusTracker().setStatus(config.getStatusSubsystemName(), StatusTracker.Status.Ok);
    }
    
    private static class TimerExecutorThreadFactory implements ThreadFactory, UncaughtExceptionHandler
    {
        private final String namePrefix;
        private final AtomicInteger counter;
        private final StatusTracker statusTracker;
        private final String statusName;

        private TimerExecutorThreadFactory(String namePrefix, StatusTracker statusTracker, String statusName)
        {
            this.namePrefix = namePrefix + "-";
            this.counter = new AtomicInteger(-1);
            this.statusTracker = statusTracker;
            this.statusName = statusName;
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(r, namePrefix + counter.incrementAndGet());
            t.setUncaughtExceptionHandler(this);
            return t;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable e)
        {
            String msg = "Application Timer Executor thread uncaught exception in thread " + thread;
            LoggerFactory.getLogger(getClass()).error(msg, e);
            statusTracker.setStatus(statusName, StatusTracker.Status.Error);
        }
    }
}