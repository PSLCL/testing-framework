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
package com.pslcl.dtf.resource.aws.provider.person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.provider.PersonProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;
import com.pslcl.dtf.resource.aws.instance.person.AwsPersonInstance;
import com.pslcl.dtf.resource.aws.instance.person.PersonConfigData;
import com.pslcl.dtf.resource.aws.instance.person.PersonInstanceFuture;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;

@SuppressWarnings("javadoc")
public class AwsPersonProvider extends AwsResourceProvider implements PersonProvider
{
    private final HashMap<Long, AwsPersonInstance> boundPeople; // key is templateId
    private final HashMap<Long, PersonReservedResource> reservedPeople; // key is resourceId
    public volatile PersonConfigData defaultPersonConfigData;

    public AwsPersonProvider(AwsResourcesManager manager)
    {
        super(manager);
        reservedPeople = new HashMap<Long, PersonReservedResource>();
        boundPeople = new HashMap<Long, AwsPersonInstance>();
    }

    RunnerConfig getConfig()
    {
        return config;
    }

    HashMap<Long, PersonReservedResource> getReservedPeople()
    {
        return reservedPeople;
    }

    public void addBoundInstance(long resourceId, AwsPersonInstance instance)
    {
        synchronized (boundPeople)
        {
            boundPeople.put(resourceId, instance);
        }
    }

    public void setRunId(String templateId, long runId)
    {
        synchronized (reservedPeople)
        {
            for (Entry<Long, PersonReservedResource> entry : reservedPeople.entrySet())
            {
                ResourceCoordinates coord = entry.getValue().resource.getCoordinates();
                if (coord.templateId.equals(templateId))
                    coord.setRunId(runId);
            }
        }
    }

    public void forceCleanup()
    {
    }

    public void release(String templateId, boolean isReusable)
    {
        if(true)
            return;
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        ResourceCoordinates coordinates = null;
        synchronized (boundPeople)
        {
            synchronized (reservedPeople)
            {
                List<Long> releaseList = new ArrayList<Long>();
                for (Entry<Long, AwsPersonInstance> entry : boundPeople.entrySet())
                {
                    coordinates = entry.getValue().getCoordinates();
                    if (coordinates.templateId.equals(templateId))
                        releaseList.add(entry.getKey());
                }
                for (Long key : releaseList)
                {
                    AwsPersonInstance instance = boundPeople.remove(key);
                    PersonReservedResource reservedResource = reservedPeople.remove(key);
                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, instance.getCoordinates());
                    futures.add(config.blockingExecutor.submit(new ReleasePersonFuture(this, instance, pdelayData)));
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
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, coordinates);
            pdelayData.preFixMostName = config.properties.getProperty(ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
            String name = pdelayData.getFullTemplateIdName(MachineInstanceFuture.KeyPairMidStr, null);
            DeleteKeyPairRequest request = new DeleteKeyPairRequest().withKeyName(name);
            ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
            String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "deleteVpc:" + name);
            do
            {
                try
                {
                    manager.ec2Client.deleteKeyPair(request);
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
        defaultPersonConfigData = PersonConfigData.init(config);
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public Future<PersonInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, resource.getCoordinates());
        config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Warn));

        synchronized (reservedPeople)
        {
            PersonReservedResource reservedResource = reservedPeople.get(resource.getCoordinates().resourceId);
            if (reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getCoordinates().resourceId + ") is not reserved");
            reservedResource.getTimerFuture().cancel(true);
            Future<PersonInstance> future = config.blockingExecutor.submit(new PersonInstanceFuture(reservedResource, reservedResource.groupIdentifier, pdelayData));
            reservedResource.setInstanceFuture(future);
            return future;
        }
    }

    @Override
    public List<Future<PersonInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        List<Future<PersonInstance>> list = new ArrayList<Future<PersonInstance>>();
        for (ReservedResource resource : resources)
            list.add(bind(resource));
        return list;
    }

    @Override
    public Future<List<ResourceReserveDisposition>> reserve(List<ResourceDescription> resources, int timeoutSeconds)
    {
        return config.blockingExecutor.submit(new PersonReserveFuture(this, resources, timeoutSeconds));
    }

    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        return ProviderNames.getAllPersonProviderKeys();
    }
}
