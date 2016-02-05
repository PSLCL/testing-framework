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
import java.util.concurrent.atomic.AtomicInteger;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.provider.PersonProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.RequestThrottle;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
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
    private final AtomicInteger roundRobinIndex;
    public final RequestThrottle inspectThrottle;

    public AwsPersonProvider(AwsResourcesManager manager)
    {
        super(manager);
        reservedPeople = new HashMap<Long, PersonReservedResource>();
        boundPeople = new HashMap<Long, AwsPersonInstance>();
        roundRobinIndex = new AtomicInteger(-1);
        inspectThrottle = new RequestThrottle(1);
    }

    int getNextInspectorIndex()
    {
        int index = roundRobinIndex.incrementAndGet();
        if(roundRobinIndex.get() >= defaultPersonConfigData.inspectors.size())
        {
            roundRobinIndex.set(0);
            index = 0;
        }
        return index;
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
                ResourceCoordinates coord = entry.getValue().getCoordinates();
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
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        ResourceCoordinates coordinates = null;
        synchronized (boundPeople)
        {
            synchronized (reservedPeople)
            {
                releasePossiblePendings(templateId, isReusable);
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
                    reservedPeople.remove(key);
                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, instance.getCoordinates());
                    futures.add(config.blockingExecutor.submit(new ReleasePersonFuture(this, coordinates, pdelayData)));
                }
            }
        }
        // make sure they are all complete before going further
        for (Future<Void> future : futures)
        {
            try
            {
                future.get();
            } catch (Exception e)
            {
            }
        }
    }

    private void releasePossiblePendings(String templateId, boolean isReusable)
    {
        // note this is being called already synchronized to reservedMachines
        
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        ResourceCoordinates coordinates = null;
        List<Long> releaseList = new ArrayList<Long>();
        for (Entry<Long, PersonReservedResource> entry : reservedPeople.entrySet())
        {
            PersonReservedResource rresource = entry.getValue(); 
            coordinates = rresource.resource.getCoordinates();
            if (coordinates.templateId.equals(templateId))
            {
                Future<PersonInstance> future = rresource.getInstanceFuture();
                boolean cancelResult = future.cancel(false);
                /*
                    This attempt will fail if the task has already completed, has already been cancelled,
                    or could not be cancelled for some other reason.
                    In the PersonInstanceFuture's case no aws side resources are being obtained.
                    Thus we could do the future.cancel(true) here but I wanted to maintain the same cancel policy as the
                    more complex Machine incase things may change in the future and for consistency.
                */
                rresource.bindFutureCanceled.set(true);
                if(cancelResult)
                {
                    try
                    {
                        future.get();
                    } catch (Exception e)
                    {
                        TabToLevel format = new TabToLevel();
                        format.ttl("\n", getClass().getSimpleName(), ".release cancel pending future handling");
                        log.debug(rresource.toString(format).toString());
                        releaseList.add(entry.getKey());
                        ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, coordinates);
                        futures.add(config.blockingExecutor.submit(new ReleasePersonFuture(this, coordinates, pdelayData)));
                    }
                }else
                {
                    try
                    {
                        future.get();
                    } catch (Exception e)
                    {
                        log.info("release machine code caught a somewhat unexpected exception during cancel cleanup", e);
                    }
                }
            }
        }
        for (Long key : releaseList)
            reservedPeople.remove(key);
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
            Future<PersonInstance> future = config.blockingExecutor.submit(new PersonInstanceFuture(reservedResource, pdelayData));
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
        return ResourceNames.getAllPersonProviderKeys();
    }
}
