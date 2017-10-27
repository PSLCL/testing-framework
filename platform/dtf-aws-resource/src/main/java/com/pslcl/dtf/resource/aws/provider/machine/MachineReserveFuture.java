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

import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;

@SuppressWarnings("javadoc")
public class MachineReserveFuture implements Callable<List<ResourceReserveDisposition>>
{
    private final Logger log;
    private final AwsMachineProvider provider;
    private final List<ResourceDescription> resources;
    private final int timeoutSeconds;

    public MachineReserveFuture(AwsMachineProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        log = LoggerFactory.getLogger(getClass());
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public List<ResourceReserveDisposition> call() throws Exception
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n"+getClass().getSimpleName() + ".call");
        format.inc();
        format.ttl("resources requested: ", resources.size());
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("MachineReserveFuture");
        MachineQueryResult result = new MachineQueryResult();
        List<ResourceReserveDisposition> list = new ArrayList<ResourceReserveDisposition>();
        for (ResourceDescription resource : resources)
        {
            resource.getCoordinates().toString(format);
            format.inc();
            try
            {
                boolean available = false;
                if (provider.internalIsAvailable(resource, result, format))
                {
                    format.ttl("image and instance type available");
                    resource.getCoordinates().setManager(provider.manager);
                    resource.getCoordinates().setProvider(provider);
                    MachineReservedResource rresource = new MachineReservedResource(provider, resource, resource.getCoordinates(), result);
                    AwsMachineInstance machineInstance = ((AwsMachineProvider) provider).checkForReuse(rresource, true, format);
                    if(machineInstance == null)
                    {
                        format.ttl("no reuse instances available");
                        available = provider.checkInstanceTypeLimits(result.getInstanceType(), true, format);
                        format.ttl("typeLimits available: ", available);
                    }
                    else
                    {
                        format.ttl("reuse instance is available");
                        available = true;
                    }
                    if(available)
                    {

                        //@formatter:off
                        list.add(new ResourceReserveDisposition(
                                        resource,
                                        new ReservedResource(
                                                        resource.getCoordinates(),
                                                        resource.getAttributes(),
                                                        timeoutSeconds)));
                        //@formatter:on
                        ScheduledFuture<?> future = provider.config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
                        rresource.setTimerFuture(future, timeoutSeconds);
                        provider.addReservedMachine(resource.getCoordinates().resourceId, rresource);
                        format.ttl("reserved and reserved timer started");
                    }else
                        format.ttl("instance type limit hit, not available");
                }else
                    format.ttl("image and/or instance type was not available");

                if(!available)
                    list.add(new ResourceReserveDisposition(resource));
            } catch (Exception e)
            {
                Thread.currentThread().setName(tname);
                ResourceReserveDisposition disposition = new ResourceReserveDisposition(resource);
                disposition.setInvalidResource();
                list.add(disposition);
                String msg = "unexpected exception, reserve failed";
                format.ttl(msg);
                log.debug(msg, e);
            }
            format.dec();
        }
        log.debug(format.toString());
        Thread.currentThread().setName(tname);
        return list;
    }
}
