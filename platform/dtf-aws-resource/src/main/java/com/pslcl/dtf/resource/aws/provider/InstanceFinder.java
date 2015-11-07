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
package com.pslcl.dtf.resource.aws.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

@SuppressWarnings("javadoc")
public class InstanceFinder
{
    private final Map<String, AtomicInteger> limits;
    private volatile String instanceType;

    public InstanceFinder()
    {
        limits = new HashMap<String, AtomicInteger>();
    }

    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.ttl("AWS Instance:");
        config.initsb.level.incrementAndGet();
        instanceType = config.properties.getProperty(ProviderNames.InstanceTypeKey, ProviderNames.InstanceTypeDefault);
        config.initsb.ttl(ProviderNames.InstanceTypeKey, " = ", instanceType);
        config.initsb.level.decrementAndGet();
        config.initsb.ttl("AWS Instance Type Limits:");
        config.initsb.level.incrementAndGet();
        for(int i=0; i < ProviderNames.instanceTypes.length; i++)
        {
            String key = ProviderNames.InstanceTypeKeyBase + "." + ProviderNames.instanceTypes[i].toString() + ProviderNames.InstanceTypeLimit;
            String value = config.properties.getProperty(key, "0");
            config.initsb.ttl(key," = ", value);
            int limit = Integer.parseInt(value);
            limits.put(ProviderNames.instanceTypes[i].toString(), new AtomicInteger(limit));
        }
        config.initsb.level.decrementAndGet();
    }        

    public InstanceType findInstance(ResourceDescription resource) throws ResourceNotFoundException
    {
        Map<String, String> attrs = resource.getAttributes();
        String type = attrs.get(ProviderNames.InstanceTypeKey);
        if(type == null)
            type = instanceType;
        InstanceType itype = null;
        for(int i=0; i < ProviderNames.instanceTypes.length; i++)
        {
            if(ProviderNames.instanceTypes[i].toString().equals(type))
            {
                itype = ProviderNames.instanceTypes[i];
                break;
            }
        }
        if(itype == null)
            throw new ResourceNotFoundException(ProviderNames.InstanceTypeKey + "=" + type + " is not a valid AWS instance type");
        return itype;
    }    
    
    public boolean checkLimits(InstanceType instanceType)
    {
        int count = limits.get(instanceType.toString()).get();
        if(count == -1)
            return true;
        if(count > 0)
            return true;
        return false;
    }
    
    public boolean reserveInstance(InstanceType instanceType)
    {
        AtomicInteger count = limits.get(instanceType.toString());
        if(count.get() == -1)
            return true;
        if(count.decrementAndGet() >= 0)
            return true;
        return false;
    }
    
    public void releaseInstance(InstanceType instanceType)
    {
        AtomicInteger count = limits.get(instanceType.toString());
        if(count.get() == -1)
            return;
        count.incrementAndGet();
    }
}