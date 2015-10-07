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
package com.pslcl.qa.runner.resource.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.PersonInstance;
import com.pslcl.qa.runner.resource.PersonProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceStatusCallback;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class AwsPersonProvider extends AwsResourceProvider implements PersonProvider
{
    public AwsPersonProvider()
    {
    }

    @Override
    public void init(RunnerServiceConfig config) throws Exception
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
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds)
    {
        // temporary, to allow progress: return empty rqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>(), new ArrayList<ResourceWithAttributes>());
        if (resources.size() > 0)
        {
            // temporary, to allow progress: return an artificially unavailable resource
            ResourceWithAttributes artificialUnavailableResource = resources.get(0);
            retRqr.getUnavailableResources().add(artificialUnavailableResource);
        }
        return retRqr;
    }

    @Override
    public Future<PersonInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
        // TODO Auto-generated method stub

    }
}
