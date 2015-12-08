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
package com.pslcl.dtf.resource.aws.provider.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
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
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.provider.NetworkProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;
import com.pslcl.dtf.resource.aws.instance.network.NetworkInstanceFuture;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;
import com.pslcl.dtf.resource.aws.provider.SubnetConfigData;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;

@SuppressWarnings("javadoc")
public class AwsNetworkProvider extends AwsResourceProvider implements NetworkProvider
{
    private final HashMap<Long, AwsNetworkInstance> boundNetworks; // key is resourceId
    private final HashMap<Long, NetworkReservedResource> reservedNetworks; // key is resourceId
    private final AwsResourcesManager manager;
    public volatile SubnetConfigData defaultSubnetConfigData;
    
    public AwsNetworkProvider(AwsResourcesManager manager)
    {
        this.manager = manager;
        reservedNetworks = new HashMap<Long, NetworkReservedResource>();
        boundNetworks = new HashMap<Long, AwsNetworkInstance>();
    }

    RunnerConfig getConfig()
    {
        return config;
    }

    AmazonEC2Client getEc2Client()
    {
        return ec2Client;
    }

    public SubnetManager getSubnetManager()
    {
        return manager.subnetManager;
    }
    
    public AwsResourcesManager getManager()
    {
        return manager;
    }

    HashMap<Long, AwsNetworkInstance> getBoundNetworks()
    {
        return boundNetworks;
    }

    HashMap<Long, NetworkReservedResource> getReservedNetworks()
    {
        return reservedNetworks;
    }

    public void addBoundNetwork(long resourceId, AwsNetworkInstance instance)
    {
        synchronized (boundNetworks)
        {
            boundNetworks.put(resourceId, instance);
        }
    }

    public void addReservedNetwork(long resourceId, NetworkReservedResource reservedResource)
    {
        synchronized (reservedNetworks)
        {
            reservedNetworks.put(resourceId, reservedResource);
        }
    }

    public void setRunId(String templateId, long runId)
    {
        synchronized(reservedNetworks)
        {
            for(Entry<Long, NetworkReservedResource> entry : reservedNetworks.entrySet())
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
        synchronized (boundNetworks)
        {
            synchronized (reservedNetworks)
            {
                List<Long> releaseList = new ArrayList<Long>();
                for (Entry<Long, AwsNetworkInstance> entry : boundNetworks.entrySet())
                {
                    coordinates = entry.getValue().getCoordinates();
                    if (coordinates.templateId.equals(templateId))
                        releaseList.add(entry.getKey());
                }
                for (Long key : releaseList)
                {
                    AwsNetworkInstance instance = boundNetworks.remove(key);
                    NetworkReservedResource reservedResource = reservedNetworks.remove(key);
                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(manager, this, config.statusTracker, instance.getCoordinates());
                    futures.add(config.blockingExecutor.submit(new ReleaseNetworkFuture(this, instance, reservedResource.vpc.getVpcId(), reservedResource.subnet.getSubnetId(), pdelayData)));
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
        defaultSubnetConfigData = SubnetConfigData.init(config);
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public Future<NetworkInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(manager, this, config.statusTracker, resource.getCoordinates());
        config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Warn));

        synchronized (reservedNetworks)
        {
            NetworkReservedResource reservedResource = reservedNetworks.get(resource.getCoordinates().resourceId);
            if (reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getCoordinates().resourceId + ") is not reserved");
            reservedResource.getTimerFuture().cancel(true);
            Future<NetworkInstance> future = config.blockingExecutor.submit(new NetworkInstanceFuture(reservedResource, ec2Client, pdelayData));
            reservedResource.setInstanceFuture(future);
            //            reservedResource.timerFuture.cancel(true);
            return future;
        }
    }

    @Override
    public List<Future<NetworkInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        List<Future<NetworkInstance>> list = new ArrayList<Future<NetworkInstance>>();
        for (ReservedResource resource : resources)
        {
            list.add(bind(resource));
        }
        return list;
    }

    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        return internalIsAvailable(resource, new NetworkQueryResult());
    }

    boolean internalIsAvailable(ResourceDescription resource, NetworkQueryResult result) throws ResourceNotFoundException
    {
//        InstanceType instanceType = instanceFinder.findInstance(resource);
//        if (!instanceFinder.checkLimits(instanceType))
//            return false;
//        result.setInstanceType(instanceType);
//        result.setImageId(imageFinder.findImage(ec2Client, resource));
        return true;
    }

    @Override
    public Future<ResourceQueryResult> queryResourceAvailability(List<ResourceDescription> resources)
    {
        return config.blockingExecutor.submit(new NetworkAvailabilityFuture(this, resources));
    }

    ResourceQueryResult internalQueryResourceAvailability(List<ResourceDescription> resources, NetworkQueryResult result)
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
        return config.blockingExecutor.submit(new NetworkReserveFuture(this, resources, timeoutSeconds));
    }

    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        return ProviderNames.getAllNetworkProviderKeys();
    }
}
