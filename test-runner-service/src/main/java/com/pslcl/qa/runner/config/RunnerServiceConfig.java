package com.pslcl.qa.runner.config;


public class RunnerServiceConfig
{
    private final StatusTracker statusTracker;
    private final BlockingExecutor blockingExecutor;
    private final ScheduledExecutor scheduledExecutor;
    
    public RunnerServiceConfig()
    {
        statusTracker = new StatusTrackerProvider();
        blockingExecutor = new BlockingExecutor();
        scheduledExecutor = new ScheduledExecutor();
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
}