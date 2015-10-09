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
package com.pslcl.qa.runner.resource.aws.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.MachineProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */
public class AwsMachineProvider extends AwsResourceProvider implements MachineProvider
{
    private final HashMap<Integer, ReservedResource> reservedResources;
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;

    public AwsMachineProvider()
    {
        reservedResources = new HashMap<Integer, ReservedResource>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
    }

    @Override
    public void init(RunnerServiceConfig config) throws Exception
    {
        super.init(config);
        instanceFinder.init(config);
        imageFinder.init(config);
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public Future<MachineInstance> bind(ReservedResourceWithAttributes resource) throws BindResourceFailedException
    {
        synchronized(reservedResources)
        {
            ReservedResource reservedResource = reservedResources.get(resource.getReference());
            if(reservedResource == null)
                throw new BindResourceFailedException(resource.getName() + "(" + resource.getReference() +") is not reserved");
            reservedResource.future.cancel(true);
            return config.blockingExecutor.submit(new AwsMachineInstanceFuture(resource));
        }
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources)
    {
        synchronized (reservedResources)
        {
            return null;
        }
    }

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource)
    {
    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException
    {
        InstanceType instanceType = instanceFinder.findInstance(resource);
        if (!instanceFinder.checkLimits(instanceType))
            return false;
        imageFinder.findImage(ec2Client, resource);
        return true;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources)
    {
        List<ReservedResourceWithAttributes> reservedResources = new ArrayList<ReservedResourceWithAttributes>();
        List<ResourceWithAttributes> availableResources = new ArrayList<ResourceWithAttributes>();
        List<ResourceWithAttributes> unavailableResources = new ArrayList<ResourceWithAttributes>();
        List<ResourceWithAttributes> invalidResources = new ArrayList<ResourceWithAttributes>();
        ResourceQueryResult resourceQueryResult = new ResourceQueryResult(reservedResources, availableResources, unavailableResources, invalidResources);
        for (ResourceWithAttributes resource : resources)
        {
            try
            {
                if (isAvailable(resource))
                    availableResources.add(resource);
                else
                    unavailableResources.add(resource);
            } catch (Exception e)
            {
                invalidResources.add(resource);
                log.debug(getClass().getSimpleName() + ".queryResourceAvailable failed: " + resource.toString(), e);
            }
        }
        return resourceQueryResult;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds)
    {
        ResourceQueryResult queryResult = queryResourceAvailability(resources);

        List<ReservedResourceWithAttributes> reservedResources = new ArrayList<ReservedResourceWithAttributes>();
        List<ResourceWithAttributes> availableResources = new ArrayList<ResourceWithAttributes>();
        ResourceQueryResult result = new ResourceQueryResult(reservedResources, availableResources, queryResult.getUnavailableResources(), queryResult.getInvalidResources());

        for (ResourceWithAttributes requested : resources)
        {
            for (ResourceWithAttributes avail : queryResult.getAvailableResources())
            {
                if (avail.getName().equals(requested.getName()))
                {
                    synchronized (reservedResources)
                    {
                        // final limit check/reserve could have lost it to thread switch
                        // this is a light weight check, so redundancy not a concern
                        InstanceType instanceType = null;
                        // this call was already made so we know it will not fail
                        try
                        {
                            instanceType = instanceFinder.findInstance(avail);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        if (!instanceFinder.reserveInstance(instanceType))
                        {
                            // it was taken by a thread switch, change to unavailable.
                            queryResult.getUnavailableResources().add(avail);
                            continue;
                        }
                        reservedResources.add(new ReservedResourceWithAttributes(avail, this, timeoutSeconds));
                        ReservedResource rresource = new ReservedResource(avail, instanceType);
                        ScheduledFuture<?> future = config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                        rresource.setFuture(future);
                    }
                }
            }
        }
        return result;
    }

    // implement ArtifactConsumer interface

    @Override
    public void updateArtifact(String component, String version, String platform, String name, String artifactHash)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidateArtifacts(String component, String version)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
        // TODO Auto-generated method stub

    }

    private class ReservedResource implements Runnable
    {
        public final ResourceWithAttributes resource;
        public final InstanceType instanceType;
        public volatile ScheduledFuture<?> future;

        private ReservedResource(ResourceWithAttributes resource, InstanceType instanceType)
        {
            this.resource = resource;
            this.instanceType = instanceType;
        }

        private void setFuture(ScheduledFuture<?> future)
        {
            this.future = future;
        }

        @Override
        public void run()
        {
            synchronized(reservedResources)
            {
                reservedResources.remove(resource.getReference());
                instanceFinder.releaseInstance(instanceType);
                log.info(resource.toString() + " reserve timed out");
            }
        }
    }
}
