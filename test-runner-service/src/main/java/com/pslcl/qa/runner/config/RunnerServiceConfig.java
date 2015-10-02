package com.pslcl.qa.runner.config;

import java.util.Properties;

import org.apache.commons.daemon.DaemonContext;


public class RunnerServiceConfig
{
    private final static String PropertiesFileShort = "-c";
    private final static String PropertiesFileLong = "--config-path";
    
    private final StatusTracker statusTracker;
    private final BlockingExecutor blockingExecutor;
    private final ScheduledExecutor scheduledExecutor;
    private final Properties properties;
    
    public RunnerServiceConfig(DaemonContext daemonContext) throws Exception
    {
        statusTracker = new StatusTrackerProvider();
        blockingExecutor = new BlockingExecutor();
        BlockingExecutorConfig blockingExecutorConfig = new BlockingExecutorPropertiesConfig(statusTracker);
        blockingExecutor.init(blockingExecutorConfig);
        scheduledExecutor = new ScheduledExecutor();
        ExecutorQueuePropertiesConfig executorQueueConfig = new ExecutorQueuePropertiesConfig(statusTracker);
        scheduledExecutor.init(executorQueueConfig);
        properties = new Properties();
        String[] args = daemonContext.getArguments();
        if(args.length > 1 || args.length < 2)
            throw new Exception("Commandline switch " + PropertiesFileShort + " or " + PropertiesFileLong + " required");
        if(!args[0].equals(PropertiesFileShort) && !args[0].equals(PropertiesFileLong))
            throw new Exception("Commandline switch " + PropertiesFileShort + " or " + PropertiesFileLong + " required");
        PropertiesFile.load(properties, args[1]);
    }

    public Properties getProperties()
    {
        return properties;
    }
    
    public StatusTracker getStatusTracker()
    {
        return statusTracker;
    }

    public BlockingExecutor getBlockingExecutor()
    {
        return blockingExecutor;
    }

    public ScheduledExecutor getScheduledExecutor()
    {
        return scheduledExecutor;
    }
    
    public static class BlockingExecutorPropertiesConfig implements BlockingExecutorConfig
    {
        private static final String SubsystemNameDefault = "RunnerService-Executor"; 
        private static final String CorePoolSizeDefault = "2";
        private static final String MaxPoolSizeDefault = "10";
        private static final String CoreThreadTimeoutDefault = "false";
        private static final String KeepAliveTimeoutDefault = "15000";
        private static final String MaxBlockingTimeDefault = "5000";
        
        private final StatusTracker statusTracker;
        
        public BlockingExecutorPropertiesConfig(StatusTracker statusTracker)
        {
            this.statusTracker = statusTracker;
        }
        
        @Override
        public int getCorePoolSize()
        {
            return Integer.parseInt(CorePoolSizeDefault);
        }

        @Override
        public String getThreadNamePrefix()
        {
            return SubsystemNameDefault; 
        }

        @Override
        public StatusTracker getStatusTracker()
        {
            return statusTracker;
        }

        @Override
        public String getStatusSubsystemName()
        {
            return SubsystemNameDefault; 
        }

        @Override
        public int getMaximumPoolSize()
        {
            return Integer.parseInt(MaxPoolSizeDefault);
        }

        @Override
        public boolean isAllowCoreThreadTimeout()
        {
            return Boolean.parseBoolean(CoreThreadTimeoutDefault);
        }

        @Override
        public int getKeepAliveTime()
        {
            return Integer.parseInt(KeepAliveTimeoutDefault);
        }

        @Override
        public int getMaximumBlockingTime()
        {
            return Integer.parseInt(MaxBlockingTimeDefault);
        }
        
    }
    public static class ExecutorQueuePropertiesConfig implements ExecutorQueueConfig
    {
        private static final String SubsystemNameDefault = "RunnerService-ScheduledExecutor"; 
        private static final String CorePoolSizeDefault = "2";
        
        private final StatusTracker statusTracker;
        
        public ExecutorQueuePropertiesConfig(StatusTracker statusTracker)
        {
            this.statusTracker = statusTracker;
        }
        
        @Override
        public int getCorePoolSize()
        {
            return Integer.parseInt(CorePoolSizeDefault);
        }

        @Override
        public String getThreadNamePrefix()
        {
            return SubsystemNameDefault; 
        }

        @Override
        public StatusTracker getStatusTracker()
        {
            return statusTracker;
        }

        @Override
        public String getStatusSubsystemName()
        {
            return SubsystemNameDefault; 
        }
    }
}