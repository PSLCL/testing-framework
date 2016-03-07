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
package com.pslcl.dtf.core.util.executor;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.util.StrH;


@SuppressWarnings("javadoc")
public class BlockingExecutorConfiguration
{
    public static final String CorePoolSizeKey = "pslcl.service.util.executor.core-size";
    public static final String MaximumQueueSizeKey = "pslcl.service.util.executor.max-queue-size";
    public static final String MaxBlockingTimeKey = "pslcl.service.util.executor.max-blocking-time";
    public static final String ThreadNamePrefixKey = "pslcl.service.util.executor.thread-name";
    public static final String KeepAliveDelayKey = "pslcl.service.util.executor.keep-alive-delay";
    public static final String AllowCoreThreadTimeoutKey = "pslcl.service.util.executor.core-timeout";
    public static final String StatusNameKey = "pslcl.service.util.executor.status-name";

    public static final String CorePoolSizeDefault = "128";
    public static final String MaximumQueueSizeDefault = "128";
    public static final String MaxBlockingTimeDefault = "120000";
    public static final String ThreadNamePrefixDefault = "PslclBlockingExecutor";
    public static final String KeepAliveDelayDefault = "120000";
    public static final String AllowCoreThreadTimeoutDefault = "true";
    public static final String StatusNameDefault = "PslclBlockingExecutor";
    
    public static ExecutorConfig propertiesToConfig(RunnerConfig config) throws Exception
    {
        String msg ="ok";
        try
        {
            String value = config.properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            value = StrH.trim(value);
            config.initsb.ttl(CorePoolSizeKey, "=", value);
            msg = "invalid corePoolSize value";
            int corePoolSize = Integer.parseInt(value);
            value = config.properties.getProperty(MaximumQueueSizeKey, MaximumQueueSizeDefault);
            value = StrH.trim(value);
            config.initsb.ttl(MaximumQueueSizeKey, "=", value);
            msg = "invalid maxQueueSize value";
            int maxQueueSize = Integer.parseInt(value);
            value = config.properties.getProperty(MaxBlockingTimeKey, MaxBlockingTimeDefault);
            value = StrH.trim(value);
            config.initsb.ttl(MaxBlockingTimeKey, "=", value);
            msg = "invalid maxBlockingTime value";
            int maxBlockingTime = Integer.parseInt(value);
            String threadName = config.properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            threadName = StrH.trim(threadName);
            config.initsb.ttl(ThreadNamePrefixKey, "=", value);
            value = config.properties.getProperty(KeepAliveDelayKey, KeepAliveDelayDefault);
            value = StrH.trim(value);
            config.initsb.ttl(KeepAliveDelayKey, "=", value);
            msg = "invalid keepAliveDelay value";
            int keepAliveDelay = Integer.parseInt(value);
            value = config.properties.getProperty(AllowCoreThreadTimeoutKey, AllowCoreThreadTimeoutDefault);
            value = StrH.trim(value);
            config.initsb.ttl(AllowCoreThreadTimeoutKey, "=", value);
            msg = "invalid allowCoreThreadTimeout value";
            boolean allowCoreThreadTimeout = Boolean.parseBoolean(value);
            
            String statusName = config.properties.getProperty(StatusNameKey, StatusNameDefault);
            statusName = StrH.trim(statusName);
            config.initsb.ttl(StatusNameKey, "=", statusName);

            config.properties.remove(CorePoolSizeKey);
            config.properties.remove(MaximumQueueSizeKey);
            config.properties.remove(MaxBlockingTimeKey);
            config.properties.remove(ThreadNamePrefixKey);
            config.properties.remove(KeepAliveDelayKey);
            config.properties.remove(AllowCoreThreadTimeoutKey);
            config.properties.remove(StatusNameKey);
            
            msg = "EmitBlockingExecutor builder failed";
            return new ExecutorConfig(config.statusTracker, corePoolSize, maxQueueSize, maxBlockingTime, threadName, keepAliveDelay, allowCoreThreadTimeout, statusName);
        }catch(Exception e)
        {
            config.initsb.ttl(msg);
            throw e;
        }
    }
    
    public static class ExecutorConfig implements BlockingExecutorConfig
    {    
        public final StatusTracker statusTracker;
        public final int corePoolSize;
        public final int maxQueueSize;
        public final int maxBlockingTime;
        public final String threadNamePrefix;
        public final int keepAliveDelay;
        public final boolean allowCoreThreadTimeout;
        public final String statusName;

        public ExecutorConfig(
                        StatusTracker statusTracker,
                        int corePoolSize,
                        int maxQueueSize,
                        int maxBlockingTime,
                        String threadNamePrefix,
                        int keepAliveDelay,
                        boolean allowCoreThreadTimeout,
                        String statusName)
        {
            this.statusTracker = statusTracker;
            this.corePoolSize = corePoolSize;
            this.maxQueueSize = maxQueueSize;
            this.maxBlockingTime = maxBlockingTime;
            this.threadNamePrefix = threadNamePrefix;
            this.keepAliveDelay = keepAliveDelay;
            this.allowCoreThreadTimeout = allowCoreThreadTimeout;
            this.statusName = statusName;
        }

        @Override
        public int getCorePoolSize()
        {
            return corePoolSize;
        }

        @Override
        public String getThreadNamePrefix()
        {
            return threadNamePrefix;
        }

        @Override
        public StatusTracker getStatusTracker()
        {
            return statusTracker;
        }

        @Override
        public String getStatusSubsystemName()
        {
            return statusName;
        }

        @Override
        public int getMaximumPoolSize()
        {
            return maxQueueSize;
        }

        @Override
        public boolean isAllowCoreThreadTimeout()
        {
            return allowCoreThreadTimeout;
        }

        @Override
        public int getKeepAliveTime()
        {
            return keepAliveDelay;
        }

        @Override
        public int getMaximumBlockingTime()
        {
            return maxBlockingTime;
        }
    }
}
