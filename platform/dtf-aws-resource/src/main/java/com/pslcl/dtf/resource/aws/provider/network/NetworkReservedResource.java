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

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusEvent;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class NetworkReservedResource implements Runnable
{
    public final ResourceDescription resource;
    public volatile GroupIdentifier groupIdentifier;
    public volatile String groupId;
    public volatile Vpc vpc;
    public volatile Subnet subnet;
    public volatile String net;
    public volatile Instance ec2Instance;
    public volatile long runId;

    private ScheduledFuture<?> timerFuture;
    private Future<NetworkInstance> instanceFuture;
    private final ProgressiveDelayData pdelayData; 

    NetworkReservedResource(ProgressiveDelayData pdelayData, ResourceDescription resource, GroupIdentifier groupId)
    {
        this.pdelayData = pdelayData;
        this.resource = resource;
        this.groupIdentifier = groupId;
        ResourceCoordinates newCoord = pdelayData.coord;
        resource.getCoordinates().setManager(newCoord.getManager());
        resource.getCoordinates().setProvider(newCoord.getProvider());
        resource.getCoordinates().setRunId(newCoord.getRunId());
    }

    synchronized void setTimerFuture(ScheduledFuture<?> future)
    {
        timerFuture = future;
    }

    synchronized ScheduledFuture<?> getTimerFuture()
    {
        return timerFuture;
    }

    synchronized void setInstanceFuture(Future<NetworkInstance> future)
    {
        instanceFuture = future;
    }

    synchronized Future<NetworkInstance> getInstanceFuture()
    {
        return instanceFuture;
    }

    @Override
    public void run()
    {
        HashMap<Long, NetworkReservedResource> map = ((AwsNetworkProvider)pdelayData.provider).getReservedNetworks();
        synchronized (map)
        {
            map.remove(resource.getCoordinates().resourceId);
            try
            {
                pdelayData.manager.subnetManager.releaseSecurityGroup(pdelayData);
            } catch (FatalResourceException e)
            {
                LoggerFactory.getLogger(getClass()).warn("failed to cleanup securityGroup: " + groupIdentifier.getGroupId());
            }
        }
        LoggerFactory.getLogger(getClass()).info(resource.toString() + " reserve timed out");
    }
}
