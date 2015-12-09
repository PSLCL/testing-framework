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

import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveResult;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.provider.MachineProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance;
import com.pslcl.dtf.resource.aws.instance.machine.MachineConfigData;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;
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
    public volatile MachineConfigData defaultMachineConfigData;

    public AwsMachineProvider(AwsResourcesManager manager)
    {
        super(manager);
        reservedMachines = new HashMap<Long, MachineReservedResource>();
        boundInstances = new HashMap<Long, AwsMachineInstance>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
    }

    InstanceFinder getInstanceFinder()
    {
        return instanceFinder;
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
        synchronized(reservedMachines)
        {
            for(Entry<Long, MachineReservedResource> entry : reservedMachines.entrySet())
            {
                ResourceCoordinates coord = entry.getValue().resource.getCoordinates();
                if(coord.templateId.equals(templateId))
                    coord.setRunId(runId);
            }
        }
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
                    reservedMachines.remove(key);
                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, config.statusTracker, instance.getCoordinates());
                    futures.add(config.blockingExecutor.submit(new ReleaseMachineFuture(this, instance, null, /*reservedResource.vpc.getVpcId() reservedResource.subnet.getSubnetId()*/ null, pdelayData)));
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
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, config.statusTracker, coordinates);
            pdelayData.preFixMostName = config.properties.getProperty(ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
            String name = pdelayData.getFullTemplateIdName(MachineInstanceFuture.KeyPairMidStr, null);
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
        config.initsb.indentedOk();
        defaultMachineConfigData = MachineConfigData.init(config);
        config.initsb.level.decrementAndGet();
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public Future<MachineInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, config.statusTracker, resource.getCoordinates());
        config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Warn));

        synchronized (reservedMachines)
        {
            MachineReservedResource reservedResource = reservedMachines.get(resource.getCoordinates().resourceId);
            if (reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getCoordinates().resourceId + ") is not reserved");
            reservedResource.getTimerFuture().cancel(true);
            Future<MachineInstance> future = config.blockingExecutor.submit(new MachineInstanceFuture(reservedResource, pdelayData));
            reservedResource.setInstanceFuture(future);
            //            reservedResource.timerFuture.cancel(true);
            return future;
        }
    }

    @Override
    public List<Future<MachineInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        List<Future<MachineInstance>> list = new ArrayList<Future<MachineInstance>>();
        for (ReservedResource resource : resources)
        {
            list.add(bind(resource));
        }
        return list;
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
    public Future<ResourceReserveResult> reserveIfAvailable(List<ResourceDescription> resources, int timeoutSeconds)
    {
        return config.blockingExecutor.submit(new MachineReserveFuture(this, resources, timeoutSeconds));
    }

    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        return ProviderNames.getAllMachineProviderKeys();
    }
}
