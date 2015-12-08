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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveResult;

@SuppressWarnings("javadoc")
public class MachineReserveFuture implements Callable<ResourceReserveResult>
{
    private final AwsMachineProvider provider;
    private final  List<ResourceDescription> resources;
    private final int timeoutSeconds;
    
    public MachineReserveFuture(AwsMachineProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public ResourceReserveResult call() throws Exception
    {
        MachineQueryResult result = new MachineQueryResult();
        List<ReservedResource> reservedResources = new ArrayList<ReservedResource>();
        List<ResourceDescription> unavailableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> invalidResources = new ArrayList<ResourceDescription>();
        ResourceReserveResult resourceQueryResult = new ResourceReserveResult(reservedResources, unavailableResources, invalidResources);
        for (ResourceDescription resource : resources)
        {
            try
            {
                if (provider.internalIsAvailable(resource, result))
                {
                    resource.getCoordinates().setManager(provider.getManager());
                    resource.getCoordinates().setProvider(provider);
                    reservedResources.add(new ReservedResource(resource.getCoordinates(), resource.getAttributes(), timeoutSeconds));
                    MachineReservedResource rresource = new MachineReservedResource(provider, resource, resource.getCoordinates(), result);
                    ScheduledFuture<?> future = provider.getConfig().scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                    rresource.setTimerFuture(future);
                    provider.addReservedMachine(resource.getCoordinates().resourceId, rresource);
                }
                else
                    unavailableResources.add(resource);
            } catch (Exception e)
            {
                invalidResources.add(resource);
                LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".queryResourceAvailable failed: " + resource.toString(), e);
            }
        }
        return resourceQueryResult;
    }
}
