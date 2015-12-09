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

import java.util.concurrent.Callable;

import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.person.AwsPersonInstance;

@SuppressWarnings("javadoc")
public class ReleasePersonFuture implements Callable<Void>
{
    private final AwsPersonProvider provider;
    private final AwsPersonInstance instance;
    private final ProgressiveDelayData pdelayData;

    public ReleasePersonFuture(AwsPersonProvider provider, AwsPersonInstance instance, ProgressiveDelayData pdelayData)
    {
        this.provider = provider;
        this.instance = instance;
        this.pdelayData = pdelayData;
    }

    @Override
    public Void call() throws Exception
    {
//        LoggerFactory.getLogger(getClass()).debug("Releasing resource start: " + instance.getCoordinates().toString());
//        pdelayData.provider.manager.subnetManager.releaseSecurityGroup(pdelayData);
//        provider.getConfig().statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Down));
//        LoggerFactory.getLogger(getClass()).debug("Releasing resource complete: " + instance.getCoordinates().toString());
        return null;
    }
}
