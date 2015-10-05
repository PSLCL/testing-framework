package com.pslcl.qa.runner.resource.aws;

import com.pslcl.qa.runner.config.RunnerServiceConfig;

public abstract class AwsResourceProvider
{
    public static final String GroupIdKey = "pslcl.qa.platform.resource.aws.group-id";
    public static final String GroupIdDefault = "AWSTestResource";
    
    /**
     * Resource Group ID - Used to tag AWS resources so that they are identifiable by the resource provider.
     */
    protected volatile RunnerServiceConfig config;
    public volatile String groupId;
    
    public void init(RunnerServiceConfig config)
    {
        this.config = config;
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        String value = config.properties.getProperty(GroupIdKey, GroupIdDefault);
        config.initsb.ttl(GroupIdKey, " = ", value);
        groupId = value;
        config.initsb.level.decrementAndGet();
    }
    
    public String getGroupId()
    {
        return groupId;
    }
}
