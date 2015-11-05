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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.PersonProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.ResourcesController;

public class AwsPersonProvider extends AwsResourceProvider implements PersonProvider
{
    private final ResourcesController controller;

    public AwsPersonProvider(ResourcesController controller)
    {
        this.controller = controller;
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        super.init(config);
    }

    @Override
    public void destroy()
    {
    }
    
    
    @Override
    public void updateArtifact(String component, String version, String platform, String name, String artifactHash)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidateArtifacts(String component, String version)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> resources)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void releaseReservedResource(ReservedResource resource)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceDescription> resources)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceDescription> resources, int timeoutSeconds)
    {
        // temporary, to allow progress: return empty rqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResource>(), new ArrayList<ResourceDescription>(), new ArrayList<ResourceDescription>(), new ArrayList<ResourceDescription>());
        if (resources.size() > 0)
        {
            // temporary, to allow progress: return an artificially unavailable resource
            ResourceDescription artificialUnavailableResource = resources.get(0);
            retRqr.getUnavailableResources().add(artificialUnavailableResource);
        }
        return retRqr;
    }

    @Override
    public Future<PersonInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
        // TODO Auto-generated method stub

    }
    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
