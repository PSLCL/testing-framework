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
package com.pslcl.dtf.resource.aws.instance.person;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;
import com.pslcl.dtf.resource.aws.provider.person.PersonReservedResource;

@SuppressWarnings("javadoc")
public class PersonInstanceFuture implements Callable<PersonInstance>
{
    public final PersonReservedResource reservedResource;
    private final ProgressiveDelayData pdelayData;

    public PersonInstanceFuture(PersonReservedResource reservedResource, ProgressiveDelayData pdelayData)
    {
        this.reservedResource = reservedResource;
        this.pdelayData = pdelayData;
    }

    @Override
    public PersonInstance call() throws FatalResourceException
    {
        try
        {
            checkFutureCanceled();
            reservedResource.pdelayData.preFixMostName = reservedResource.pconfig.resoucePrefixName;
            AwsPersonInstance personInstance = new AwsPersonInstance(reservedResource, pdelayData.provider.config);
            pdelayData.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Ok));
            ((AwsPersonProvider) pdelayData.provider).addBoundInstance(pdelayData.coord.resourceId, personInstance);
            checkFutureCanceled();
            return personInstance;
        }
//        catch (FatalResourceException e)
//        {
//            throw e;
//        } 
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
