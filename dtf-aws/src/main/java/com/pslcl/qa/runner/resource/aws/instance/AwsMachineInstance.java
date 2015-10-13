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
package com.pslcl.qa.runner.resource.aws.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.resource.ReservedResource;
import com.pslcl.qa.runner.resource.exception.IncompatibleResourceException;
import com.pslcl.qa.runner.resource.instance.CableInstance;
import com.pslcl.qa.runner.resource.instance.MachineInstance;
import com.pslcl.qa.runner.resource.instance.NetworkInstance;
import com.pslcl.qa.runner.resource.instance.StartProgram;
import com.pslcl.qa.runner.resource.provider.ResourceProvider;

public class AwsMachineInstance implements MachineInstance
{
    private String name;
    private Map<String, String> attributes;
    private String description;
    private ResourceProvider resourceProvider;
    private int timeoutSeconds;
    private int reference;

    /**
     * constructor for the use case where resource was previously reserved
     * 
     * @param resource
     */
    public AwsMachineInstance(ReservedResource reservedResourceWithAttributes)
    {
        name = reservedResourceWithAttributes.getName();
        attributes = reservedResourceWithAttributes.getAttributes();
        // description = ;
        resourceProvider = reservedResourceWithAttributes.getResourceProvider();
        reference = reservedResourceWithAttributes.getReference();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return new HashMap<String, String>(attributes);
    }

    @Override
    public ResourceProvider getResourceProvider()
    {
        return resourceProvider;
    }

    @Override
    public int getReference()
    {
        return reference;
    }

    @Override
    public Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException
    {
        return null;
    }

    @Override
    public Future<Integer> run(String command)
    {
        return null;
    }

    @Override
    public Future<Integer> configure(String command)
    {
        return null;
    }

    @Override
    public Future<StartProgram> start(String command)
    {
        return null;
    }

    @Override
    public Future<Void> deploy(String filename, String artifactHash)
    {
        return null;
    }

    @Override
    public Future<Void> delete(String filename)
    {
        return null;
    }

    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        return null;
    }
}
