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

import java.util.Map;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.test.resource.aws.instance.machine.AwsMachineInstance;
import com.pslcl.dtf.test.resource.aws.instance.network.AwsNetworkInstance;

@SuppressWarnings("javadoc")
public class AwsCableInstance implements CableInstance
{
    private final String ipAddress;
    private final MachineInstance machineInstance;
    private final NetworkInstance networkInstance;
    
    public AwsCableInstance(AwsMachineInstance machineInstance, AwsNetworkInstance networkInstance)
    {
        this.machineInstance = machineInstance;
        this.networkInstance = networkInstance;
        ipAddress = machineInstance.ec2Instance.getPrivateIpAddress();
    }

    @Override
    public ResourceProvider getResourceProvider()
    {
        return machineInstance.getResourceProvider();
    }

    @Override
    public String getName()
    {
        return machineInstance.getName();
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return machineInstance.getAttributes();
    }

    @Override
    public void addAttribute(String key, String value)
    {
        machineInstance.addAttribute(key, value);
    }
    
    @Override
    public ResourceCoordinates getCoordinates()
    {
        return machineInstance.getCoordinates();
    }

    @Override
    public String getIPAddress()
    {
        return ipAddress;
    }

    @Override
    public MachineInstance getMachineInstance()
    {
        return machineInstance;
    }

    @Override
    public NetworkInstance getNetworkInstance()
    {
        return networkInstance;
    }
}
