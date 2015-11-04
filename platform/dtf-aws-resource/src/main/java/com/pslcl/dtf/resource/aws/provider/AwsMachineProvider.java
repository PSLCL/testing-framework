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
package com.pslcl.dtf.resource.aws.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.MachineProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.instance.MachineInstanceFuture;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */
public class AwsMachineProvider extends AwsResourceProvider implements MachineProvider
{
    private final HashMap<Long, MachineReservedResource> reservedResources;
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;

    public AwsMachineProvider()
    {
        reservedResources = new HashMap<Long, MachineReservedResource>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
    }

    @Override
    public void init(RunnerConfig config) throws Exception
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
    public Future<MachineInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        synchronized(reservedResources)
        {
            MachineReservedResource reservedResource = reservedResources.get(resource.getReference());
            if(reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getReference() +") is not reserved");
            Future<MachineInstance> future = config.blockingExecutor.submit(new MachineInstanceFuture(reservedResource, ec2Client, config)); 
            reservedResource.setInstanceFuture(future);
//            reservedResource.timerFuture.cancel(true);
            return future;
        }
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        for(ReservedResource resource : resources)
        {
            bind(resource);
        }
        synchronized (reservedResources)
        {
            return null;
        }
    }

    @Override
    public void releaseReservedResource(ReservedResource resource)
    {
    }

    @Override
    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        return internalIsAvailable(resource, new MachineQueryResult());
    }
    
    private boolean internalIsAvailable(ResourceDescription resource, MachineQueryResult result) throws ResourceNotFoundException
    {
        InstanceType instanceType = instanceFinder.findInstance(resource);
        if (!instanceFinder.checkLimits(instanceType))
            return false;
        result.setInstanceType(instanceType);
        result.setImageId(imageFinder.findImage(ec2Client, resource));
        return true;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceDescription> resources)
    {
        return internalQueryResourceAvailability(resources, new MachineQueryResult());
    }
    
    private ResourceQueryResult internalQueryResourceAvailability(List<ResourceDescription> resources, MachineQueryResult result)
    {
        List<ReservedResource> reservedResources = new ArrayList<ReservedResource>();
        List<ResourceDescription> availableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> unavailableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> invalidResources = new ArrayList<ResourceDescription>();
        ResourceQueryResult resourceQueryResult = new ResourceQueryResult(reservedResources, availableResources, unavailableResources, invalidResources);
        for (ResourceDescription resource : resources)
        {
            try
            {
                if (internalIsAvailable(resource, result))
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
    public ResourceQueryResult reserveIfAvailable(List<ResourceDescription> resources, int timeoutSeconds)
    {
        MachineQueryResult queryResult = new MachineQueryResult();
        ResourceQueryResult resouceQueryResult = internalQueryResourceAvailability(resources, queryResult);

        List<ReservedResource> reservedResources = new ArrayList<ReservedResource>();
        List<ResourceDescription> availableResources = new ArrayList<ResourceDescription>();
        //@formatter:off
        ResourceQueryResult result = new ResourceQueryResult(
                                            reservedResources, 
                                            availableResources, 
                                            resouceQueryResult.getUnavailableResources(), 
                                            resouceQueryResult.getInvalidResources());
        //@formatter:on

        for (ResourceDescription requested : resources)
        {
            for (ResourceDescription avail : resouceQueryResult.getAvailableResources())
            {
                if (avail.getName().equals(requested.getName()))
                {
                    reservedResources.add(new ReservedResource(avail, this, timeoutSeconds));
                    MachineReservedResource rresource = new MachineReservedResource(avail, queryResult);
                    ScheduledFuture<?> future = config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                    rresource.setTimerFuture(future);
                    this.reservedResources.put(rresource.resource.getReference(), rresource);
                }
            }
        }
        return result;
    }

    // implement ArtifactConsumer interface

    @Override
    public void updateArtifact(String component, String version, String platform, String name, String artifactHash)
    {
    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name)
    {
    }

    @Override
    public void invalidateArtifacts(String component, String version)
    {
    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
    }

    private class MachineQueryResult
    {
        private InstanceType instanceType;
        private String imageId; 
        
        public MachineQueryResult()
        {
        }
        
        private synchronized void setInstanceType(InstanceType type)
        {
            instanceType = type;
        }
        
        private synchronized InstanceType getInstanceType()
        {
            return instanceType;
        }
        
        private synchronized void setImageId(String imageId)
        {
            this.imageId = imageId;
        }
        
        private synchronized String getImageId()
        {
            return imageId;
        }
    }

    
    
    public class MachineReservedResource implements Runnable
    {
        public final ResourceDescription resource;
        public final InstanceType instanceType;
        public final String imageId;
        private ScheduledFuture<?> timerFuture;
        private Future<MachineInstance> instanceFuture;

        private MachineReservedResource(ResourceDescription resource, MachineQueryResult result)
        {
            this.resource = resource;
            instanceType = result.instanceType;
            imageId = result.imageId;
        }

        public synchronized void setTimerFuture(ScheduledFuture<?> future)
        {
            timerFuture = future;
        }
        
        public synchronized ScheduledFuture<?> getTimerFuture()
        {
            return timerFuture;
        }
        
        public synchronized void setInstanceFuture(Future<MachineInstance> future)
        {
            instanceFuture = future;
        }
        
        public synchronized Future<MachineInstance> getInstanceFuture()
        {
            return instanceFuture;
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

    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
