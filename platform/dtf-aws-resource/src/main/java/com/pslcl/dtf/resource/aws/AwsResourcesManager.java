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
package com.pslcl.dtf.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.AwsClientConfig;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.ClientType;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;
import com.pslcl.dtf.resource.aws.provider.machine.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.network.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;

@SuppressWarnings("javadoc")
public class AwsResourcesManager implements ResourcesManager
{
    public static final String StatusPrefixStr = "resource-";
    
    private final List<ResourceProvider> resourceProviders;
    public final AwsMachineProvider machineProvider;
    public final AwsNetworkProvider networkProvider;
    public final AwsPersonProvider personProvider;
    private volatile RunnerConfig config;
    public volatile SubnetManager subnetManager;
    public volatile AmazonEC2Client ec2Client;
    public volatile AwsClientConfig ec2cconfig;
    public volatile AmazonSimpleEmailServiceClient sesClient;
    public volatile String systemId;
    
    public AwsResourcesManager()
    {
        resourceProviders = new ArrayList<ResourceProvider>();
        machineProvider = new AwsMachineProvider(this);
        networkProvider = new AwsNetworkProvider(this);
        personProvider = new AwsPersonProvider(this);
        resourceProviders.add(machineProvider);
        resourceProviders.add(networkProvider);
        resourceProviders.add(personProvider);
    }

    private volatile int maxDelay;
    private volatile int maxRetries;
    
    public void setTagtimeout(int maxDelay, int maxRetries)
    {
        this.maxDelay = maxDelay;
        this.maxRetries = maxRetries;
    }


    public static String getTagValue(List<Tag> tags, String key)
    {
        for(Tag tag : tags)
        {
            if(tag.getKey().equals(key))
                return tag.getValue();
        }
        return null;
    }
    
    public static boolean isDtfObject(List<Tag> tags, String systemId)
    {
        boolean[] expected = new boolean[5];
        for(Tag tag : tags)
        {
            if(tag.getKey().equals(TagNameKey))
                expected[0] = true;
            else if(tag.getKey().equals(TagTemplateIdKey))
                expected[1] = true;
            else if(tag.getKey().equals(TagResourceIdKey))
                expected[2] = true;
            else if(tag.getKey().equals(TagRunIdKey))
                expected[3] = true;
            else if(tag.getKey().equals(SystemIdKey))
            {
                if(tag.getValue().equals(systemId))
                    expected[4] = true;
            }
        }
        boolean dtfResource = true;
        for(int i=0; i < expected.length; i++)
        {
            if(!expected[i])
            {
                dtfResource = false;
                break;
            }
        }
        return dtfResource;
    }
    
    public static void handleStatusTracker(ProgressiveDelayData pdelayData, StatusTracker.Status status)
    {
        pdelayData.statusTracker.setStatus(StatusPrefixStr+pdelayData.coord.resourceId, status);
        pdelayData.statusTracker.fireResourceStatusChanged(
                        pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, status));
        pdelayData.statusTracker.removeStatus("0x"+Long.toHexString(pdelayData.coord.templateInstanceId));
    }
    
    
    public static final String TagNameKey = "Name";
    
    public static final String SystemIdKey = "dtfSystemId";
    public static final String TagRunIdKey = "runId";
    public static final String TagTemplateIdKey = "templateId";
    public static final String TagTemplateInstanceIdKey = "templateInstanceId";
    public static final String TagResourceIdKey = "resourceId";
    
    public void createNameTag(ProgressiveDelayData pdelayData, String name, String resourceId) throws FatalResourceException
    {
        pdelayData.maxDelay = maxDelay;
        pdelayData.maxRetries = maxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        //@formatter:off
        CreateTagsRequest ctr = new 
                        CreateTagsRequest().withTags(
                                        new Tag(TagNameKey, name),
                                        new Tag(SystemIdKey, systemId),
                                        new Tag(TagRunIdKey, Long.toString(pdelayData.coord.getRunId())),
                                        new Tag(TagTemplateIdKey, pdelayData.coord.templateIdToHexString()),
                                        new Tag(TagTemplateInstanceIdKey, "0x"+Long.toHexString(pdelayData.coord.templateInstanceId)),
                                        new Tag(TagResourceIdKey, "0x"+Long.toHexString(pdelayData.coord.resourceId)))
                                        .withResources(resourceId);
        //@formatter:on
        do
        {
            try
            {
                ec2Client.createTags(ctr);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(name + " createTags", e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
    }
    
    public void createIdleNameTag(ProgressiveDelayData pdelayData, String name, String resourceId) throws FatalResourceException
    {
        pdelayData.maxDelay = maxDelay;
        pdelayData.maxRetries = maxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        //@formatter:off
        DeleteTagsRequest dtr = new 
                        DeleteTagsRequest().withTags(
                                        new Tag(TagRunIdKey),
                                        new Tag(TagTemplateIdKey),
                                        new Tag(TagTemplateInstanceIdKey),
                                        new Tag(TagResourceIdKey))
                                        .withResources(resourceId);
        CreateTagsRequest ctr = new 
                        CreateTagsRequest().withTags(
                                        new Tag(TagNameKey, name))
                                        .withResources(resourceId);
        //@formatter:on
        do
        {
            try
            {
                ec2Client.createTags(ctr);
                ec2Client.deleteTags(dtr);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(name + " createTags", e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
    }
    
    @Override
    public List<ResourceProvider> getResourceProviders()
    {
        return new ArrayList<ResourceProvider>(resourceProviders);
    }

    public AwsMachineProvider getMachineProvider()
    {
        return machineProvider;
    }
    
    public AwsNetworkProvider getNetworkProvider()
    {
        return networkProvider;
    }
    
    public AwsPersonProvider getPersonProvider()
    {
        return personProvider;
    }
    
    @Override
    public void init(RunnerConfig config) throws Exception
    {
        this.config = config;
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        
        String value = config.properties.getProperty(ResourceNames.SystemIdKey, ResourceNames.SystemIdDefault);
        value = StrH.trim(value);
        systemId = value;
        config.initsb.ttl("ec2Client:");
        config.initsb.level.incrementAndGet();
        ec2cconfig = AwsClientConfiguration.getClientConfiguration(config, ClientType.Ec2);
        ec2Client = new AmazonEC2Client(ec2cconfig.clientConfig);
        ec2Client.setEndpoint(ec2cconfig.endpoint);
        config.initsb.ttl("obtained ec2Client");
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl("sesClient:");
        config.initsb.level.incrementAndGet();
        AwsClientConfig sescconfig = AwsClientConfiguration.getClientConfiguration(config, ClientType.Ses);
        sesClient = new AmazonSimpleEmailServiceClient(sescconfig.clientConfig);
        sesClient.setRegion(sescconfig.region);
//        sesClient.setEndpoint(cconfig.endpoint); do not set this on the ses client, it will not work
        config.initsb.ttl("obtained sesClient");
        config.initsb.level.decrementAndGet();
        
        subnetManager = new SubnetManager(this);
        subnetManager.init(config);
        
        machineProvider.init(config);
        personProvider.init(config);
        networkProvider.init(config);
        value = config.properties.getProperty(ResourceNames.StafLocalPingKey, ResourceNames.StafLocalPingDefault);
        value = StrH.trim(value);
        if(Boolean.parseBoolean(value))
            StafSupport.ping("local");
        
        config.initsb.level.decrementAndGet();
    }

    @Override
    public void destroy()
    {
        try
        {
            StafSupport.destroy();
            int size = resourceProviders.size();
            for (int i = 0; i < size; i++)
                resourceProviders.get(i).destroy();
            resourceProviders.clear();
            ec2Client.shutdown();
            sesClient.shutdown();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".destroy failed", e);
        }
    }

    @Override
    public void setRunId(long templateInstanceId, long runId)
    {
        machineProvider.setRunId(templateInstanceId, runId);
    }

    @Override
    public void forceCleanup()
    {
        machineProvider.forceCleanup();
    }

    @Override
    public void release(long templateInstanceId, boolean isReusable)
    {
        config.blockingExecutor.submit(new ReleaseFuture(templateInstanceId, isReusable));
    }

    @Override
    public void release(long templateInstanceId, long resourceId, boolean isReusable)
    {
        throw new RuntimeException("not implemented");
    }
    
    private class ReleaseFuture implements Callable<Void>
    {
        private final long templateInstanceId;
        private final boolean isReusable;
        
        private ReleaseFuture(long templateInstanceId, boolean isReusable)
        {
            this.templateInstanceId = templateInstanceId;
            this.isReusable = isReusable;
        }
        
        @Override
        public Void call() throws Exception
        {
            String tname = Thread.currentThread().getName();
            Thread.currentThread().setName("AwsReleaseFuture");
            try
            {
                personProvider.release(templateInstanceId, isReusable);
            }catch(Exception e)
            {
                LoggerFactory.getLogger(getClass()).warn("personProvider.release failed", e);
            }
            try
            {
                networkProvider.release(templateInstanceId, isReusable);
            }catch(Exception e)
            {
                LoggerFactory.getLogger(getClass()).warn("networkProvider.release failed", e);
            }
            try
            {
                machineProvider.release(templateInstanceId, isReusable);
            }catch(Exception e)
            {
                LoggerFactory.getLogger(getClass()).warn("machineProvider.release failed", e);
            }
            Thread.currentThread().setName(tname);
            return null;
        }
    }
}
