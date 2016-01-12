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
package com.pslcl.dtf.resource.aws.instance.network;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.SubnetConfigData;
import com.pslcl.dtf.resource.aws.provider.network.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.network.NetworkReservedResource;

@SuppressWarnings("javadoc")
public class NetworkInstanceFuture implements Callable<NetworkInstance>
{
    public final NetworkReservedResource reservedResource;
    private final ProgressiveDelayData pdelayData;
    private final GroupIdentifier groupIdentifier;

    public NetworkInstanceFuture(NetworkReservedResource reservedResource, GroupIdentifier groupIdentifier, ProgressiveDelayData pdelayData)
    {
        this.reservedResource = reservedResource;
        this.groupIdentifier = groupIdentifier;
        this.pdelayData = pdelayData;
    }

    @Override
    public NetworkInstance call() throws FatalResourceException
    {
        try
        {
            checkFutureCanceled();
            reservedResource.subnetConfig = SubnetConfigData.init(reservedResource.resource, null, ((AwsNetworkProvider)pdelayData.provider).defaultSubnetConfigData);
            checkFutureCanceled();
            reservedResource.vpc = pdelayData.provider.manager.subnetManager.getVpc(pdelayData, reservedResource.subnetConfig);
            checkFutureCanceled();
            reservedResource.subnet = pdelayData.provider.manager.subnetManager.getSubnet(pdelayData, reservedResource.subnetConfig);
            checkFutureCanceled();
            AwsNetworkInstance networkInstance = new AwsNetworkInstance(reservedResource, groupIdentifier, pdelayData.provider.config);
            pdelayData.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Ok));
            ((AwsNetworkProvider) pdelayData.provider).addBoundInstance(pdelayData.coord.resourceId, networkInstance);
            return networkInstance;
        } catch (FatalResourceException e)
        {
            //TODO: as you figure out forceCleanup and optimization of normal releaseFuture cleanup, need to to do possible cleanup on these exceptions
            throw e;
        }
        catch(CancellationException ie)
        {
            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), ie);
        }
        catch (Throwable t)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + " call method threw a non-FatalResourceException", t);
            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), t);
        }
    }
    
    private void checkFutureCanceled()
    {
        if(reservedResource.bindFutureCanceled.get())
            throw new CancellationException();
    }

}
