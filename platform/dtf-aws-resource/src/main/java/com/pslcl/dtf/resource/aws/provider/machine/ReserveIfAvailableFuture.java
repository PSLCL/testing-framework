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

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;

@SuppressWarnings("javadoc")
public class ReserveIfAvailableFuture implements Callable<ResourceQueryResult>
{
    private final AwsMachineProvider provider;
    private final  List<ResourceDescription> resources;
    private final int timeoutSeconds;
    
    public ReserveIfAvailableFuture(AwsMachineProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public ResourceQueryResult call() throws Exception
    {
            MachineQueryResult queryResult = new MachineQueryResult();
            ResourceQueryResult resouceQueryResult = provider.internalQueryResourceAvailability(resources, queryResult);

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
                        requested.getCoordinates().setManager(provider.getManager());
                        requested.getCoordinates().setProvider(provider);
                        reservedResources.add(new ReservedResource(requested.getCoordinates(), avail.getAttributes(), timeoutSeconds));
                        MachineReservedResource rresource = new MachineReservedResource(provider, avail, requested.getCoordinates(), queryResult);
                        ScheduledFuture<?> future = provider.getConfig().scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                        rresource.setTimerFuture(future);
                        provider.getReservedMachines().put(requested.getCoordinates().resourceId, rresource);
                    }
                }
            }
            return result;
    }
}
