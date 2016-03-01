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
package com.pslcl.dtf.test.resource.aws.provider.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;

@SuppressWarnings("javadoc")
public class MachineReserveFuture implements Callable<List<ResourceReserveDisposition>>
{
    private final AwsMachineProvider provider;
    private final List<ResourceDescription> resources;
    private final int timeoutSeconds;

    public MachineReserveFuture(AwsMachineProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public List<ResourceReserveDisposition> call() throws Exception
    {
        MachineQueryResult result = new MachineQueryResult();
        List<ResourceReserveDisposition> list = new ArrayList<ResourceReserveDisposition>();
        for (ResourceDescription resource : resources)
        {
            try
            {
                if (provider.internalIsAvailable(resource, result))
                {
                    resource.getCoordinates().setManager(provider.manager);
                    resource.getCoordinates().setProvider(provider);
                    //@formatter:off
                    list.add(new ResourceReserveDisposition(
                                    resource, 
                                    new ReservedResource(
                                                    resource.getCoordinates(), 
                                                    resource.getAttributes(), 
                                                    timeoutSeconds)));                    
                    //@formatter:on
                    MachineReservedResource rresource = new MachineReservedResource(provider, resource, resource.getCoordinates(), result);
                    ScheduledFuture<?> future = provider.config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                    rresource.setTimerFuture(future, timeoutSeconds);
                    provider.addReservedMachine(resource.getCoordinates().resourceId, rresource);
                    LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".reserved" + rresource.format.toString());
                } else
                    list.add(new ResourceReserveDisposition(resource));
            } catch (Exception e)
            {
                ResourceReserveDisposition disposition = new ResourceReserveDisposition(resource);
                disposition.setInvalidResource();
                list.add(disposition);
                LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".queryResourceAvailable failed: " + resource.toString(), e);
            }
        }
        return list;
    }
}
