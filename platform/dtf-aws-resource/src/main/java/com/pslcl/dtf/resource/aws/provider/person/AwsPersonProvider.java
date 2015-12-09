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

import java.util.List;
import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveResult;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.PersonProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;

@SuppressWarnings("javadoc")
public class AwsPersonProvider extends AwsResourceProvider implements PersonProvider
{
    public AwsPersonProvider(AwsResourcesManager manager)
    {
        super(manager);
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
    public List<Future<PersonInstance>> bind(List<ReservedResource> resources)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void releaseReservedResource(ReservedResource resource)
    {
        // TODO Auto-generated method stub

    }

    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Future<ResourceReserveResult> reserveIfAvailable(List<ResourceDescription> resources, int timeoutSeconds)
    {
        return null;
    }

    @Override
    public Future<PersonInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        // TODO Auto-generated method stub
        return null;
    }

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
