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

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.AwsClientConfig;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;
import com.pslcl.dtf.resource.aws.provider.machine.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.network.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;

@SuppressWarnings("javadoc")
public class AwsResourcesManager implements ResourcesManager
{
    private final List<ResourceProvider> resourceProviders;
    public final AwsMachineProvider machineProvider;
    public final AwsNetworkProvider networkProvider;
    public final AwsPersonProvider personProvider;
    public volatile SubnetManager subnetManager;
    public volatile AmazonEC2Client ec2Client;

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
    
    public static boolean isDtfObject(List<Tag> tags)
    {
        boolean[] expected = new boolean[4];
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
        pdelayData.statusTracker.setStatus(MachineInstanceFuture.StatusPrefixStr+pdelayData.coord.resourceId, status);
        pdelayData.statusTracker.fireResourceStatusChanged(
                        pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, status));
        pdelayData.statusTracker.removeStatus(pdelayData.coord.templateId);
    }
    
    
    public static final String TagNameKey = "Name";
    public static final String TagRunIdKey = "runId";
    public static final String TagTemplateIdKey = "templateId";
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
                                        new Tag(TagRunIdKey, Long.toHexString(pdelayData.coord.getRunId())),
                                        new Tag(TagTemplateIdKey, pdelayData.coord.templateId),
                                        new Tag(TagResourceIdKey, Long.toHexString(pdelayData.coord.resourceId)))
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
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        
        AwsClientConfig cconfig = AwsClientConfiguration.getClientConfiguration(config);
        ec2Client = new AmazonEC2Client(cconfig.clientConfig);
        ec2Client.setEndpoint(cconfig.endpoint);
        config.initsb.ttl("obtained ec2Client");
        subnetManager = new SubnetManager(this);
        subnetManager.init(config);
        
        machineProvider.init(config);
        machineProvider.setEc2Client(ec2Client);
        personProvider.init(config);
        personProvider.setEc2Client(ec2Client);
        networkProvider.init(config);
        networkProvider.setEc2Client(ec2Client);
        
        config.initsb.level.decrementAndGet();
    }

    @Override
    public void destroy()
    {
        try
        {
            int size = resourceProviders.size();
            for (int i = 0; i < size; i++)
                resourceProviders.get(i).destroy();
            resourceProviders.clear();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".destroy failed", e);
        }
    }

    @Override
    public void setRunId(String templateId, long runId)
    {
        machineProvider.setRunId(templateId, runId);
    }

    @Override
    public void forceCleanup()
    {
        machineProvider.forceCleanup();
    }

    @Override
    public void release(String templateId, boolean isReusable)
    {
        machineProvider.release(templateId, isReusable);
    }

    @Override
    public void release(String templateId, long resourceId, boolean isReusable)
    {
        throw new RuntimeException("not implemented");
    }
}
