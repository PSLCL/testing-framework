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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Instance;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.provider.network.NetworkReservedResource;

@SuppressWarnings("javadoc")
public class AwsNetworkInstance implements NetworkInstance
{
    private final Logger log;
    private final String name;
    private final Map<String, String> attributes;
    private String description;
    private int timeoutSeconds;
    private final ResourceCoordinates coordinates;
    public final Instance ec2Instance;

    public AwsNetworkInstance(NetworkReservedResource reservedResource)
    {
        log = LoggerFactory.getLogger(getClass());
        name = reservedResource.resource.getName();
        attributes = reservedResource.resource.getAttributes();
        coordinates = reservedResource.resource.getCoordinates();
        ec2Instance = reservedResource.ec2Instance;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        synchronized (attributes)
        {
            return new HashMap<String, String>(attributes);
        }
    }

    @Override
    public void addAttribute(String key, String value)
    {
        synchronized (attributes)
        {
            attributes.put(key, value);
        }
    }
    
    @Override
    public ResourceProvider getResourceProvider()
    {
        return coordinates.getProvider();
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return coordinates;
    }
}
