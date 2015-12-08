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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;

@SuppressWarnings("javadoc")
public class MachineAvailabilityFuture implements Callable<ResourceQueryResult>
{
    private final Logger log;
    private final AwsMachineProvider provider;
    private final List<ResourceDescription> resources;
    
    public MachineAvailabilityFuture(AwsMachineProvider provider, List<ResourceDescription> resources)
    {
        this.provider = provider;
        this.resources = resources;
        log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public ResourceQueryResult call() throws Exception
    {
        MachineQueryResult result = new MachineQueryResult();
        List<ReservedResource> reservedResources = new ArrayList<ReservedResource>();
        List<ResourceDescription> availableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> unavailableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> invalidResources = new ArrayList<ResourceDescription>();
        ResourceQueryResult resourceQueryResult = new ResourceQueryResult(reservedResources, availableResources, unavailableResources, invalidResources);
        for (ResourceDescription resource : resources)
        {
            try
            {
                if (provider.internalIsAvailable(resource, result))
                    availableResources.add(resource);
                else
                    unavailableResources.add(resource);
            } catch (Exception e)
            {
                invalidResources.add(resource);
                log.debug(getClass().getSimpleName() + ".queryResourceAvailable failed: " + resource.toString(), e);
            }
        }
        return resourceQueryResult;
    }
}
