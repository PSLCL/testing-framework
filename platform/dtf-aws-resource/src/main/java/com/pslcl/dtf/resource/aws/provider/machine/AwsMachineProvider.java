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
package com.pslcl.dtf.resource.aws.provider.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.MachineProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.instance.AwsMachineInstance;
import com.pslcl.dtf.resource.aws.instance.MachineInstanceFuture;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */

@SuppressWarnings("javadoc")
public class AwsMachineProvider extends AwsResourceProvider implements MachineProvider
{
    private final HashMap<Long, AwsMachineInstance> boundInstances; // key is templateId
    private final HashMap<Long, MachineReservedResource> reservedMachines; // key is resourceId
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;
    private final AwsResourcesManager manager;

    public AwsMachineProvider(AwsResourcesManager controller)
    {
        this.manager = controller;
        reservedMachines = new HashMap<Long, MachineReservedResource>();
        boundInstances = new HashMap<Long, AwsMachineInstance>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
    }

    RunnerConfig getConfig()
    {
        return config;
    }

    AmazonEC2Client getEc2Client()
    {
        return ec2Client;
    }

    InstanceFinder getInstanceFinder()
    {
        return instanceFinder;
    }

    public AwsResourcesManager getManager()
    {
        return manager;
    }

    HashMap<Long, AwsMachineInstance> getBoundInstances()
    {
        return boundInstances;
    }

    HashMap<Long, MachineReservedResource> getReservedMachines()
    {
        return reservedMachines;
    }

    public void addBoundInstance(long resourceId, AwsMachineInstance instance)
    {
        synchronized (boundInstances)
        {
            boundInstances.put(resourceId, instance);
        }
    }

    public void addReservedMachine(long resourceId, MachineReservedResource reservedResource)
    {
        synchronized (reservedMachines)
        {
            reservedMachines.put(resourceId, reservedResource);
        }
    }

    public void setRunId(String templateId, long runId)
    {
    }

    public void forceCleanup()
    {
    }

    public void release(String templateId, boolean isReusable)
    {
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        ResourceCoordinates coordinates = null;
        synchronized (boundInstances)
        {
            synchronized (reservedMachines)
            {
                List<Long> releaseList = new ArrayList<Long>();
                for (Entry<Long, AwsMachineInstance> entry : boundInstances.entrySet())
                {
                    coordinates = entry.getValue().getCoordinates();
                    if (coordinates.templateId.equals(templateId))
                        releaseList.add(entry.getKey());
                }
                for (Long key : releaseList)
                {
                    AwsMachineInstance instance = boundInstances.remove(key);
                    MachineReservedResource reservedResource = reservedMachines.remove(key);
                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(manager, this, config.statusTracker, instance.getCoordinates());
                    futures.add(config.blockingExecutor.submit(new ReleaseFuture(this, instance, reservedResource.vpc.getVpcId(), reservedResource.subnet.getSubnetId(), pdelayData)));
                }
            }
        }
        // make sure they are all complete before deleting the key-pair
        for (Future<Void> future : futures)
        {
            try
            {
                future.get();
            } catch (Exception e)
            {
            }
        }

        if (coordinates != null)
        {
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(
                            manager, this, config.statusTracker, coordinates);
            pdelayData.preFixMostName = config.properties.getProperty(ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
            String name = pdelayData.getFullName(MachineInstanceFuture.KeyPairMidStr, null);
            DeleteKeyPairRequest request = new DeleteKeyPairRequest().withKeyName(name);
            ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
            String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "deleteVpc:" + name);
            do
            {
                try
                {
                    ec2Client.deleteKeyPair(request);
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                        break;
                }
            } while (true);
        }
    }

    public void releaseReservedResource(String templateId)
    {
        release(templateId, false);
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
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(manager, this, config.statusTracker, resource.getCoordinates());
        config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Warn));

        synchronized (reservedMachines)
        {
            MachineReservedResource reservedResource = reservedMachines.get(resource.getCoordinates().resourceId);
            if (reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getCoordinates().resourceId + ") is not reserved");
            reservedResource.getTimerFuture().cancel(true);
            Future<MachineInstance> future = config.blockingExecutor.submit(new MachineInstanceFuture(reservedResource, ec2Client, pdelayData));
            reservedResource.setInstanceFuture(future);
            //            reservedResource.timerFuture.cancel(true);
            return future;
        }
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        for (ReservedResource resource : resources)
        {
            bind(resource);
        }
        synchronized (reservedMachines)
        {
            return null;
        }
    }

    public void releaseReservedResource(ReservedResource resource)
    {
    }

    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        return internalIsAvailable(resource, new MachineQueryResult());
    }

    boolean internalIsAvailable(ResourceDescription resource, MachineQueryResult result) throws ResourceNotFoundException
    {
        InstanceType instanceType = instanceFinder.findInstance(resource);
        if (!instanceFinder.checkLimits(instanceType))
            return false;
        result.setInstanceType(instanceType);
        result.setImageId(imageFinder.findImage(ec2Client, resource));
        return true;
    }

    @Override
    public Future<ResourceQueryResult> queryResourceAvailability(List<ResourceDescription> resources)
    {
        return config.blockingExecutor.submit(new QueryResourceAvailabilityFuture(this, resources));
    }

    ResourceQueryResult internalQueryResourceAvailability(List<ResourceDescription> resources, MachineQueryResult result)
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
    public Future<ResourceQueryResult> reserveIfAvailable(List<ResourceDescription> resources, int timeoutSeconds)
    {
        return config.blockingExecutor.submit(new ReserveIfAvailableFuture(this, resources, timeoutSeconds));
    }

    public void release(ResourceInstance resource, boolean isReusable)
    {
    }

    //    private class MachineQueryResult
    //    {
    //        private InstanceType instanceType;
    //        private String imageId; 
    //        
    //        public MachineQueryResult()
    //        {
    //        }
    //        
    //        private synchronized void setInstanceType(InstanceType type)
    //        {
    //            instanceType = type;
    //        }
    //        
    //        private synchronized InstanceType getInstanceType()
    //        {
    //            return instanceType;
    //        }
    //        
    //        private synchronized void setImageId(String imageId)
    //        {
    //            this.imageId = imageId;
    //        }
    //        
    //        private synchronized String getImageId()
    //        {
    //            return imageId;
    //        }
    //    }
    //    
    //    public class MachineReservedResource implements Runnable
    //    {
    //        public final ResourceDescription resource;
    //        public final InstanceType instanceType;
    //        public final String imageId;
    //        public volatile GroupIdentifier groupIdentifier;
    //        public volatile String groupId;
    //        public volatile Vpc vpc;
    //        public volatile Subnet subnet;
    //        public volatile String net;
    //        public volatile Instance ec2Instance;
    //        public volatile long runId;
    //        
    //        private ScheduledFuture<?> timerFuture;
    //        private Future<MachineInstance> instanceFuture;
    //
    //        private MachineReservedResource(ResourceDescription resource, ResourceCoordinates newCoord, MachineQueryResult result)
    //        {
    //            this.resource = resource;
    //            instanceType = result.instanceType;
    //            imageId = result.imageId;
    //        }
    //
    //        public synchronized void setTimerFuture(ScheduledFuture<?> future)
    //        {
    //            timerFuture = future;
    //        }
    //        
    //        public synchronized ScheduledFuture<?> getTimerFuture()
    //        {
    //            return timerFuture;
    //        }
    //        
    //        public synchronized void setInstanceFuture(Future<MachineInstance> future)
    //        {
    //            instanceFuture = future;
    //        }
    //        
    //        public synchronized Future<MachineInstance> getInstanceFuture()
    //        {
    //            return instanceFuture;
    //        }
    //
    //        @Override
    //        public void run()
    //        {
    //            synchronized(reservedMachines)
    //            {
    //                reservedMachines.remove(resource.getCoordinates().resourceId);
    //                instanceFinder.releaseInstance(instanceType);
    //                log.info(resource.toString() + " reserve timed out");
    //            }
    //        }
    //    }

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
