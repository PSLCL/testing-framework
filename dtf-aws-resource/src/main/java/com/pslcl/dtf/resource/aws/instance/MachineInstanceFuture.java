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
package com.pslcl.dtf.resource.aws.instance;

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.pslcl.dtf.common.resource.ReservedResource;
import com.pslcl.dtf.common.resource.instance.MachineInstance;
import com.pslcl.dtf.resource.aws.provider.AwsMachineProvider.MachineReservedResource;

public class MachineInstanceFuture implements Callable<MachineInstance>
{
    public final MachineReservedResource reservedResource;

    public MachineInstanceFuture(MachineReservedResource reservedResource)
    {
        this.reservedResource = reservedResource;
    }

    @Override
    public MachineInstance call() throws Exception
    {
        try
        {
                RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
                runInstancesRequest
                    .withImageId(reservedResource.imageId)
                    .withInstanceType(reservedResource.instanceType)
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName(reservedResource.resource.getName());
        //            .withSecurityGroups("my-security-group");    
                MachineInstance retMachineInstance = new AwsMachineInstance(ReservedResource.class.cast(reservedResource));
                return retMachineInstance;
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error("create instance failed", e);
            throw e;
        }
    }
}
