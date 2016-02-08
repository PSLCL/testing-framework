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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.StrH.DoubleRange;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

@SuppressWarnings("javadoc")
public class InstanceFinder
{
//    private final Map<String, AtomicInteger> limits;
    private volatile String instanceType;
    private volatile Map<String, GlobalAttrMapData> globalAttrMapData;

    public InstanceFinder()
    {
//        limits = new HashMap<String, AtomicInteger>();
    }

    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.ttl("AWS Instance:");
        config.initsb.level.incrementAndGet();
        instanceType = config.properties.getProperty(ProviderNames.InstanceTypeKey, ProviderNames.InstanceTypeDefault);
        instanceType = StrH.trim(instanceType);
        config.initsb.ttl(ProviderNames.InstanceTypeKey, " = ", instanceType);
        config.initsb.level.decrementAndGet();
        config.initsb.ttl("AWS Instance Type Limits:");
        config.initsb.level.incrementAndGet();
//        for (int i = 0; i < ProviderNames.instanceTypes.length; i++)
//        {
//            String key = ProviderNames.InstanceTypeKeyBase + "." + ProviderNames.instanceTypes[i].toString() + ProviderNames.InstanceTypeLimit;
//            String value = config.properties.getProperty(key, "0");
//            value = StrH.trim(value);
//            config.initsb.ttl(key, " = ", value);
//            int limit = Integer.parseInt(value);
//            limits.put(ProviderNames.instanceTypes[i].toString(), new AtomicInteger(limit));
//        }
        config.initsb.level.decrementAndGet();
        config.initsb.ttl("Global to AWS mapping configuration:");
        config.initsb.level.incrementAndGet();
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(ProviderNames.InstanceGlobalMapKey, config.properties);
        if (list.size() > 0)
            globalAttrMapData = new HashMap<String, GlobalAttrMapData>();
        for (Entry<String, String> entry : list)
        {
            // =cores memory-range disk-range awsInstanceType
            // pslcl.dtf.aws.instance.map0=1 0.5-1.0 1.0 t2.micro
            String value = entry.getValue();
            value = StrH.trim(value);
            String[] attrs = value.split(" ");
            try
            {
                int cores = Integer.parseInt(attrs[0]);
                globalAttrMapData.put(attrs[3], new GlobalAttrMapData(cores, attrs[1], attrs[2], attrs[3], attrs[4]));
            } catch (Exception e)
            {
                throw new Exception("invalid " + ProviderNames.InstanceGlobalMapKey + " format: " + value + " : " + e.getMessage());
            }
        }
//        instanceType = config.properties.getProperty(ProviderNames.InstanceTypeKey, ProviderNames.InstanceTypeDefault);
        config.initsb.level.decrementAndGet();
    }

    public InstanceType findInstance(ResourceDescription resource) throws ResourceNotFoundException
    {
        Map<String, String> attrs = resource.getAttributes();
        TabToLevel format = new TabToLevel();
        format.ttl("\n", getClass().getSimpleName() + ".findInstance:");
        format.level.incrementAndGet();
        String type = attrs.get(ProviderNames.InstanceTypeKey); // globals have priority over aws specific
        format.ttl(ProviderNames.InstanceTypeKey, " = ", type);
        String coresStr = attrs.get(ResourceNames.MachineCoresKey);
        format.ttl(ResourceNames.MachineCoresKey, " = ", coresStr);
        String memoryRange = attrs.get(ResourceNames.MachineMemoryKey);
        format.ttl(ResourceNames.MachineMemoryKey, " = ", memoryRange);
        String diskRange = attrs.get(ResourceNames.MachineDiskKey);
        format.ttl(ResourceNames.MachineDiskKey, " = ", diskRange);
        InstanceType itype = null;
        if(coresStr != null || memoryRange != null || diskRange != null)
        {
            String msg = "Test script specified cores, memory or disk but no AWS mappings have been configured";
            if(globalAttrMapData == null)
            {
                format.ttl(msg);
                LoggerFactory.getLogger(getClass()).warn(format.toString());
                throw new ResourceNotFoundException(msg);
            }
            boolean found = false;
            boolean jumpedUp = false;
            int firstHit = -1;
            int i = -1;
            for(Entry<String, GlobalAttrMapData> entry : globalAttrMapData.entrySet())
            {
                ++i;
                GlobalAttrMapData data = entry.getValue();
                if(!data.isHit(coresStr, memoryRange, diskRange))
                    continue;
                itype = data.instanceType;
                if(!jumpedUp)
                    firstHit = i;
                if(checkLimits(itype))
                {
                    found = true;
                    type = itype.toString();
                    format.ttl("instanceType = " + type.toString());
                    break;
                }
                jumpedUp = true;
            }
            format.ttl("found = " + found);
            format.ttl("firstHit = " + firstHit);
            format.ttl("jumpedUp = " + jumpedUp);
            if(!found)
            {
                format.ttl(msg);
                LoggerFactory.getLogger(getClass()).warn(format.toString());
                throw new ResourceNotFoundException(msg);
            }
            if(jumpedUp)
            {
                format.ttl("jumped past smaller fits because their limits hit: smallest hit index: " + firstHit);
                LoggerFactory.getLogger(getClass()).info(format.toString());
            }else
                LoggerFactory.getLogger(getClass()).debug(format.toString());
        }
        if(type == null)
        {
            type = instanceType;
            resource.addAttribute(ProviderNames.InstanceTypeKey, type);
        }
        itype = getInstanceType(type);
        return itype;
    }

    public boolean checkLimits(InstanceType instanceType)
    {
        int count = globalAttrMapData.get(instanceType.toString()).limit.get();
        if (count == -1)
            return true;
        if (count > 0)
            return true;
        return false;
    }

    public boolean reserveInstance(InstanceType instanceType)
    {
        AtomicInteger count = globalAttrMapData.get(instanceType.toString()).limit;
        if (count.get() == -1)
            return true;
        if (count.decrementAndGet() >= 0)
            return true;
        return false;
    }

    public void releaseInstance(InstanceType instanceType)
    {
        AtomicInteger count = globalAttrMapData.get(instanceType.toString()).limit;
        if (count.get() == -1)
            return;
        count.incrementAndGet();
    }

    private static InstanceType getInstanceType(String type) throws ResourceNotFoundException
    {
        for (int i = 0; i < ProviderNames.instanceTypes.length; i++)
        {
            if (ProviderNames.instanceTypes[i].toString().equals(type))
                return ProviderNames.instanceTypes[i];
        }
        throw new ResourceNotFoundException("Invalid AWS Instance Type: " + type);
    }

    private class GlobalAttrMapData
    {
        private final AtomicInteger limit;
        private final InstanceType instanceType;
        private final int cores;
        private final DoubleRange memory;
        private final DoubleRange diskSpace;

        private GlobalAttrMapData(int cores, String memoryRange, String diskRange, String instanceType, String limit) throws Exception
        {
            this.cores = cores;
            memory = new DoubleRange(memoryRange);
            diskSpace = new DoubleRange(diskRange);
            this.instanceType = getInstanceType(instanceType);
            this.limit = new AtomicInteger(Integer.parseInt(limit));
        }
        
        private boolean isHit(String cores, String memoryRange, String diskRange)
        {
            if(cores != null)
            {
                int c = Integer.parseInt(cores);
                if(this.cores < c)
                    return false;
            }
            DoubleRange range = null;
            if(memoryRange != null)
            {
                range = new DoubleRange(memoryRange);
                if(!memory.inRange(range))
                    return false;
            }
            if(diskRange != null)
            {
                range = new DoubleRange(diskRange);
                if(!diskSpace.inRange(range))
                return false;
            }
            return true;
        }
    }
}