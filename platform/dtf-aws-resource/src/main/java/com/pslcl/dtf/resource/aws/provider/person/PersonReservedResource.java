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

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.person.PersonConfigData;

@SuppressWarnings("javadoc")
public class PersonReservedResource extends ReservedResource  implements Runnable
{
    public final ResourceDescription resource;
    public final PersonConfigData pconfig;
    public final String email;
    public final ProgressiveDelayData pdelayData;
    public final AtomicBoolean bindFutureCanceled;
    
    private ScheduledFuture<?> timerFuture;
    private Future<PersonInstance> instanceFuture;
    
    PersonReservedResource(AwsPersonProvider provider, ResourceDescription resource, int timeoutSeconds, PersonConfigData pconfig, String email)
    {
        super(resource.getCoordinates(), resource.getAttributes(), timeoutSeconds);
        this.resource = resource;
        this.pconfig = pconfig;
        pdelayData = new ProgressiveDelayData(provider, resource.getCoordinates());
        this.email = email;
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

    synchronized void setInstanceFuture(Future<PersonInstance> future)
    {
        instanceFuture = future;
    }

    public synchronized Future<PersonInstance> getInstanceFuture()
    {
        return instanceFuture;
    }

    @Override
    public void run()
    {
        HashMap<Long, PersonReservedResource> map = ((AwsPersonProvider)pdelayData.provider).getReservedPeople();
        synchronized (map)
        {
            map.remove(resource.getCoordinates().resourceId);
        }
        LoggerFactory.getLogger(getClass()).info(resource.toString() + " reserve timed out");
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
        format.ttl("email = ", email);
        format.level.decrementAndGet();
        resource.getCoordinates().toString(format);
        return format;
    }
}




