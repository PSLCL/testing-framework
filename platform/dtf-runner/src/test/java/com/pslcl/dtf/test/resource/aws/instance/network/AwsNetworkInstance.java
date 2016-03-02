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
package com.pslcl.dtf.test.resource.aws.instance.network;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.test.resource.aws.provider.network.NetworkReservedResource;

@SuppressWarnings("javadoc")
public class AwsNetworkInstance implements NetworkInstance
{
    public final NetworkReservedResource reservedResource;
    public final ResourceDescription resource;
    public final RunnerConfig runnerConfig;
    public final GroupIdentifier groupIdentifier;

    public AwsNetworkInstance(NetworkReservedResource reservedResource, GroupIdentifier groupIdentifier, RunnerConfig runnerConfig)
    {
        this.reservedResource = reservedResource;
        resource = reservedResource.resource;
        this.groupIdentifier = groupIdentifier;
        this.runnerConfig = runnerConfig;
    }

    @Override
    public String getName()
    {
        return resource.getName();
    }

    @Override
    public Map<String, String> getAttributes()
    {
        Map<String, String> map = resource.getAttributes();
        synchronized (map)
        {
            return new HashMap<String, String>(map);
        }
    }

    @Override
    public void addAttribute(String key, String value)
    {
        Map<String, String> map = resource.getAttributes();
        synchronized (map)
        {
            map.put(key, value);
        }
    }
    
    @Override
    public ResourceProvider getResourceProvider()
    {
        return resource.getCoordinates().getProvider();
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return resource.getCoordinates();
    }
}
