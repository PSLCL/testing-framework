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
package com.pslcl.dtf.resource.aws.instance.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.ModifyNetworkInterfaceAttributeRequest;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;

@SuppressWarnings("javadoc")
public class DisconnectFuture implements Callable<Void>
{
    public static final String EniMidStr = "eni";

    private final AwsMachineInstance machineInstance;
    private final AwsNetworkInstance networkInstance;

    private final ProgressiveDelayData pdelayData;
    private final AmazonEC2Client ec2Client;

    public DisconnectFuture(AwsMachineInstance machineInstance, AwsNetworkInstance networkInstance, ProgressiveDelayData pdelayData)
    {
        this.machineInstance = machineInstance;
        this.networkInstance = networkInstance;
        this.pdelayData = pdelayData;
        ec2Client = pdelayData.provider.manager.ec2Client;
    }

    @Override
    public Void call() throws FatalResourceException
    {
        List<String> sgroups = new ArrayList<String>();
        List<GroupIdentifier> existingGroups = machineInstance.ec2Instance.getSecurityGroups();
        for (GroupIdentifier gid : existingGroups)
        {
            if(gid.getGroupName().equals("default"))
                sgroups.add(gid.getGroupId());
        }
        String netInterId = machineInstance.ec2Instance.getNetworkInterfaces().get(0).getNetworkInterfaceId();
        //@formatter:off
        ModifyNetworkInterfaceAttributeRequest mniar = new ModifyNetworkInterfaceAttributeRequest()
            .withNetworkInterfaceId(netInterId)
            .withGroups(sgroups);
        //@formatter:on

        pdelayData.maxDelay = networkInstance.reservedResource.subnetConfig.sgMaxDelay;
        pdelayData.maxRetries = networkInstance.reservedResource.subnetConfig.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(EniMidStr, "modifyNetworkInterfaceAttribute");
        do
        {
            try
            {
                ec2Client.modifyNetworkInterfaceAttribute(mniar);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        return null;
    }
}
