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

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class MachineReservedResource implements Runnable
{
    public final ResourceDescription resource;
    public final InstanceType instanceType;
    public final String imageId;
    public final AtomicBoolean bindFutureCanceled;
    public volatile GroupIdentifier groupIdentifier;
    public volatile String groupId;
    public volatile Vpc vpc;
    public volatile Subnet subnet;
    public volatile String net;
    public volatile Instance ec2Instance;
    public volatile long runId;
    
    private ScheduledFuture<?> timerFuture;
    private Future<MachineInstance> instanceFuture;
    public final AwsMachineProvider provider;

    MachineReservedResource(AwsMachineProvider provider, ResourceDescription resource, ResourceCoordinates newCoord, MachineQueryResult result)
    {
        this.provider = provider;
        this.resource = resource;
        resource.getCoordinates().setManager(newCoord.getManager());
        resource.getCoordinates().setProvider(newCoord.getProvider());
        resource.getCoordinates().setRunId(newCoord.getRunId());
        instanceType = result.getInstanceType();
        imageId = result.getImageId();
        bindFutureCanceled = new AtomicBoolean(false);
    }

    synchronized void setTimerFuture(ScheduledFuture<?> future)
    {
        timerFuture = future;
    }

    synchronized ScheduledFuture<?> getTimerFuture()
    {
        return timerFuture;
    }

    synchronized void setInstanceFuture(Future<MachineInstance> future)
    {
        instanceFuture = future;
    }

    synchronized Future<MachineInstance> getInstanceFuture()
    {
        return instanceFuture;
    }

    @Override
    public void run()
    {
        HashMap<Long, MachineReservedResource> map = provider.getReservedMachines();
        synchronized (map)
        {
            map.remove(resource.getCoordinates().resourceId);
            provider.getInstanceFinder().releaseInstance(instanceType);
            LoggerFactory.getLogger(getClass()).info(resource.toString() + " reserve timed out");
        }
    }
    
    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        return toString(format).toString(); 
    }
    
    public TabToLevel toString(TabToLevel format)
    {
        format.sb.append(getClass().getSimpleName() + ":\n");
        format.level.incrementAndGet();
        format.ttl("instanceType = ", instanceType);
        format.ttl("imageId = ", imageId);
        format.ttl("groupId = ", groupId);
        format.level.decrementAndGet();
        resource.getCoordinates().toString(format);
        return format;
    }
}
