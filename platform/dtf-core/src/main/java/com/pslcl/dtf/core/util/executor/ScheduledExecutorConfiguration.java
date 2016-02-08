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
public class ScheduledExecutorConfiguration
{
    public static final String CorePoolSizeKey = "pslcl.service.util.scheduled-executor.core-size";
    public static final String ThreadNamePrefixKey = "pslcl.service.util.scheduled-executor.thread-name";
    public static final String StatusNameKey = "pslcl.service.util.scheduled-executor.status-name";

    public static final String CorePoolSizeDefault = "6";
    public static final String ThreadNamePrefixDefault = "PslclScheduledExecutor";
    public static final String StatusNameDefault = "PslclScheduledExecutor";

    public static ScheduledExecutorConfig propertiesToConfig(RunnerConfig config) throws Exception
    {
        String msg ="ok";
        try
        {
            String value = config.properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            value = StrH.trim(value);
            config.initsb.ttl(CorePoolSizeKey, "=", value);
            msg = "invalid corePoolSize value";
            int corePoolSize = Integer.parseInt(value);
            String threadName = config.properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            threadName = StrH.trim(threadName);
            config.initsb.ttl(ThreadNamePrefixKey, "=", value);
            String statusName = config.properties.getProperty(StatusNameKey, StatusNameDefault);
            statusName = StrH.trim(statusName);
            config.initsb.ttl(StatusNameKey, "=", statusName);
            
            config.properties.remove(CorePoolSizeKey);
            config.properties.remove(ThreadNamePrefixKey);
            config.properties.remove(StatusNameKey);
            
            return new ScheduledExecutorConfig(config.statusTracker, corePoolSize, threadName, statusName);
        }catch(Exception e)
        {
            config.initsb.ttl(msg);
            throw e;
        }
    }
    
    public static class ScheduledExecutorConfig implements ExecutorQueueConfig
    {    
        public final int corePoolSize;
        public final String threadNamePrefix;
        public final String statusName;
        public final StatusTracker statusTracker;

        public ScheduledExecutorConfig(StatusTracker statusTracker, int corePoolSize, String threadNamePrefix, String statusName)
        {
            this.statusTracker = statusTracker;
            this.corePoolSize = corePoolSize;
            this.threadNamePrefix = threadNamePrefix;
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
    }
}
