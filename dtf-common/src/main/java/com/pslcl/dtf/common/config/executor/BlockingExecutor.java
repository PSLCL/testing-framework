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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.common.config.status.StatusTracker;

/**
 * A <code>ThreadPoolExecutor</code> that allows for blocking submits.
 * </p>
 * It is desirable that where possible, EMIT applications do not block OAL callback threads.
 * Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 * create an instance of this class as a singleton object to the whole application and include its reference
 * in the <code>Service<T></code>'s configuration object, which in turn is typically made available to all 
 * subsystems of the application, where they can utilize this thread pool to avoid blocking OAL callbacks as
 * well as for other custom worker task requirements.
 * </p>
 * Java's <code>ThreadPoolExecutor</code> implementation throws an exception
 * if the number of outstanding threads equals the core pool size.  Instead of failing hard and losing input data
 * if the queue becomes full, this implementation has modified the default behavior and will block the submission
 * of new requests instead of immediately throwing an exception.
 * </p> 
 * The maximum amount of time to allow for blocking can be specified.  This allows for the application to throttle 
 * the input source and try to catch up before giving up and throwing an exception.
 */
//@ThreadSafe
public class BlockingExecutor extends ThreadPoolExecutor
{
    private static final int initialCorePoolSize = 3; 
    private static final int initialMaximumPoolSize = 10; 
    private static final int initialKeepAliveTime = 1000 * 120; // keep burst thread creations around for two minutes if demand quiets down
    private volatile BlockThenRunPolicy rejectPolicy;

    /**
     * The default constructor.
     * </p>
     * This call will initialize a usable ThreadPoolExecutor with a core size of 3 threads and a 
     * maximum pool size of 10 threads.  However, to enable the blocking functionality, thread pool
     * factory and status tracking the user must call the <code>init</code> method before utilizing 
     * this thread pool.
     * @see #init(BlockingExecutorConfig) 
     *  
     */
    public BlockingExecutor()
    {
        super(initialCorePoolSize, initialMaximumPoolSize, initialKeepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(Math.max(initialCorePoolSize, initialMaximumPoolSize), true));
    }

    /**
     * Initialize our custom extended functionality of the ThreadPoolExecutor.
     * </p>
     * This will initialize the thread pool with our blocking queue submission functionality,
     * thread pool factory functionality, and status tracking functionality.
     * @param config the configuration parameters used to configure the extended queue functionality.
     */
    public void init(BlockingExecutorConfig config)
    {
        setCorePoolSize(config.getCorePoolSize());
        setMaximumPoolSize(config.getMaximumPoolSize());
        setKeepAliveTime(config.getKeepAliveTime(), TimeUnit.MILLISECONDS);
        allowCoreThreadTimeOut(config.isAllowCoreThreadTimeout());
        rejectPolicy = new BlockThenRunPolicy(config.getMaximumBlockingTime(), config.getStatusTracker(), config.getStatusSubsystemName());
        super.setRejectedExecutionHandler(rejectPolicy);
        rejectPolicy.setMaxBlockingTime(config.getMaximumBlockingTime());
        config.getStatusTracker().setStatus(config.getStatusSubsystemName(), StatusTracker.Status.Ok);
        setThreadFactory(new BlockingExecutorThreadFactory(config.getThreadNamePrefix(), config.getStatusTracker(), config.getStatusSubsystemName()));
    }
    
//    /**
//     * Set the maximum blocking time for offers to the queue.
//     * @param maxOfferTime the maximum offer time.
//     */
//    public void setMaxBlockingTime(int maxOfferTime)
//    {
//        rejectPolicy.setMaxBlockingTime(maxOfferTime);
//    }

    @Override
    public void setRejectedExecutionHandler(RejectedExecutionHandler h)
    {
        throw new UnsupportedOperationException("setRejectedExecutionHandler is not allowed on this class.");
    }

    //    @ThreadSafe
    private static class BlockingExecutorThreadFactory implements ThreadFactory, UncaughtExceptionHandler
    {
        private final String namePrefix;
        private final AtomicInteger counter;
        private final StatusTracker statusTracker;
        private final String statusName;

        private BlockingExecutorThreadFactory(String namePrefix, StatusTracker statusTracker, String statusName)
        {
            this.namePrefix = namePrefix + "-";
            counter = new AtomicInteger(-1);
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
            String msg = "Application Executor thread uncaught exception in thread " + thread;
            LoggerFactory.getLogger(getClass()).error(msg, e);
            statusTracker.setStatus(statusName, StatusTracker.Status.Error);
        }
    }

    //    @ThreadSafe
    private static class BlockThenRunPolicy implements RejectedExecutionHandler
    {
        private final AtomicInteger maxOfferTime;
        private final StatusTracker statusTracker;
        private final String statusName;

        private BlockThenRunPolicy(int maxBlockingTime, StatusTracker statusTracker, String statusName)
        {
            maxOfferTime = new AtomicInteger(maxBlockingTime);
            this.statusTracker = statusTracker;
            this.statusName = statusName;
        }

        private void setMaxBlockingTime(int maxOfferTime)
        {
            this.maxOfferTime.set(maxOfferTime);
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            if (executor.isShutdown())
            {
                statusTracker.setStatus(statusName, StatusTracker.Status.Error);
                throw new RejectedExecutionException("ThreadPoolExecutor has shutdown while attempting to offer a new task.");
            }
            LoggerFactory.getLogger(getClass()).warn("rejectedExecution, Executor queue is full, blocking producer");
            statusTracker.setStatus(statusName, StatusTracker.Status.Warn);
            BlockingQueue<Runnable> queue = executor.getQueue();
            try
            {
                // offer the task to the queue, for a blocking-timeout
                if (!queue.offer(task, maxOfferTime.get(), TimeUnit.MILLISECONDS))
                {
                    String msg = "rejectedExecution callback thread timed out offering task to queue";
                    LoggerFactory.getLogger(getClass()).warn(msg); // likely called by an EMIT callback thread, which my get swallowed without logging, so log it warn here
                    throw new RejectedExecutionException(msg);
                }
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                String msg = "rejectedExecution callback thread was interrupted, cancelling immediately";
                LoggerFactory.getLogger(getClass()).debug(msg, e);
                statusTracker.setStatus(statusName, StatusTracker.Status.Error);
                throw new RejectedExecutionException(msg, e);
            }
        }
    }
}