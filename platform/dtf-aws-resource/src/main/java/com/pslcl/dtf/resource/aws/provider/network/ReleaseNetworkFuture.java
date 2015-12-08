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

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;

@SuppressWarnings("javadoc")
public class ReleaseNetworkFuture implements Callable<Void>
{
    private final AwsNetworkProvider provider;
    private final AwsNetworkInstance instance;
    private final String vpcId;
    private final String subnetId;
    private final ProgressiveDelayData pdelayData;

    public ReleaseNetworkFuture(AwsNetworkProvider provider, AwsNetworkInstance instance, String vpcId, String subnetId, ProgressiveDelayData pdelayData)
    {
        this.vpcId = vpcId;
        this.subnetId = subnetId;
        this.instance = instance;
        this.provider = provider;
        this.pdelayData = pdelayData;
    }

    @Override
    public Void call() throws Exception
    {
        LoggerFactory.getLogger(getClass()).debug("Releasing resource start: " + instance.getCoordinates().toString());
        
        provider.getConfig().statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Down));
        LoggerFactory.getLogger(getClass()).debug("Releasing resource complete: " + instance.getCoordinates().toString());
        return null;
    }
}
