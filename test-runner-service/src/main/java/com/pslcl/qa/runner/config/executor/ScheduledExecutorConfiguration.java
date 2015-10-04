/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.qa.runner.config.executor;

import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.config.status.StatusTracker;

@SuppressWarnings("javadoc")
public class ScheduledExecutorConfiguration
{
    public static final String CorePoolSizeKey = "pslcl.service.util.scheduled-executor.core-size";
    public static final String ThreadNamePrefixKey = "pslcl.service.util.scheduled-executor.thread-name";
    public static final String StatusNameKey = "pslcl.service.util.scheduled-executor.status-name";

    public static final String CorePoolSizeDefault = "2";
    public static final String ThreadNamePrefixDefault = "PslclScheduledExecutor";
    public static final String StatusNameDefault = "PslclScheduledExecutor";

    public static ScheduledExecutorConfig propertiesToConfig(RunnerServiceConfig config) throws Exception
    {
        String msg ="ok";
        try
        {
            String value = config.properties.getProperty(CorePoolSizeKey, CorePoolSizeDefault);
            config.initsb.ttl(CorePoolSizeKey, "=", value);
            msg = "invalid corePoolSize value";
            int corePoolSize = Integer.parseInt(value);
            String threadName = config.properties.getProperty(ThreadNamePrefixKey, ThreadNamePrefixDefault);
            config.initsb.ttl(ThreadNamePrefixKey, "=", value);
            String statusName = config.properties.getProperty(StatusNameKey, StatusNameDefault);
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
