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
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.InstanceAttribute;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
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
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("DisconnectFuture");
        
        //@formatter:off
        DescribeInstanceAttributeRequest diar = new 
                        DescribeInstanceAttributeRequest()
                            .withInstanceId(machineInstance.ec2Instance.getInstanceId())
                            .withAttribute("groupSet");
        //@formatter:on
        
        pdelayData.maxDelay = machineInstance.mconfig.subnetConfigData.sgMaxDelay;
        pdelayData.maxRetries = machineInstance.mconfig.subnetConfigData.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(EniMidStr, "describeInstanceAttribute");
        List<GroupIdentifier>currentList = null;
        do
        {
            try
            {
                pdelayData.provider.manager.awsThrottle();
                DescribeInstanceAttributeResult result = ec2Client.describeInstanceAttribute(diar);
                InstanceAttribute attr = result.getInstanceAttribute();
                currentList = attr.getGroups();
                break;
            } catch (Exception e)
            {
                Thread.currentThread().setName(tname);
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        
        List<String> sgroups = new ArrayList<String>();
        if(networkInstance == null)
        {
            // use the security group set at ec2 instantiation
            currentList = machineInstance.ec2Instance.getSecurityGroups();
            for (GroupIdentifier gid : currentList)  // should only be one
                sgroups.add(gid.getGroupId());
        }else
        {
            for (GroupIdentifier gid : currentList)
            {
                if(!gid.getGroupId().equals(networkInstance.groupIdentifier.getGroupId()))
                    sgroups.add(gid.getGroupId());
            }
        }
        //@formatter:off
        ModifyInstanceAttributeRequest miar = new ModifyInstanceAttributeRequest()
            .withInstanceId(machineInstance.ec2Instance.getInstanceId())
            .withGroups(sgroups);
        //@formatter:on

        pdelay.reset();
        msg = pdelayData.getHumanName(EniMidStr, "modifyInstanceAttribute");
        do
        {
            try
            {
                pdelayData.provider.manager.awsThrottle();
                ec2Client.modifyInstanceAttribute(miar);
                break;
            } catch (Exception e)
            {
                Thread.currentThread().setName(tname);
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        Thread.currentThread().setName(tname);
        return null;
    }
}
