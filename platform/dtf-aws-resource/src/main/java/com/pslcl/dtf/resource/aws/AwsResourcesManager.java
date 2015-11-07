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

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.provider.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.AwsPersonProvider;

@SuppressWarnings("javadoc")
public class AwsResourcesManager implements ResourcesManager
{
    private final List<ResourceProvider> resourceProviders;
    private final AwsMachineProvider machineProvider;
    private final AwsNetworkProvider networkProvider;
    private final AwsPersonProvider personProvider;

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

    @Override
    public List<ResourceProvider> getResourceProviders()
    {
        return new ArrayList<ResourceProvider>(resourceProviders);
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        
        config.initsb.ttl(AwsMachineProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        machineProvider.init(config);
        config.initsb.level.decrementAndGet();

        config.initsb.ttl(AwsPersonProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        personProvider.init(config);
        config.initsb.level.decrementAndGet();

        config.initsb.ttl(AwsNetworkProvider.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        networkProvider.init(config);
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
