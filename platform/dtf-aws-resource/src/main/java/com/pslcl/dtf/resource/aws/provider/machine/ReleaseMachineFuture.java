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

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;

@SuppressWarnings("javadoc")
public class ReleaseMachineFuture implements Callable<Void>
{
    private final AwsMachineProvider provider;
    private final ResourceCoordinates coordinates;
    private final Instance instance;
    private final String vpcId;
    private final String subnetId;
    private final ProgressiveDelayData pdelayData;

    public ReleaseMachineFuture(AwsMachineProvider provider, ResourceCoordinates coord, Instance instance, String vpcId, String subnetId, ProgressiveDelayData pdelayData)
    {
        this.vpcId = vpcId;
        this.subnetId = subnetId;
        this.instance = instance;
        this.provider = provider;
        coordinates = coord;
        this.pdelayData = pdelayData;
    }

    @Override
    public Void call() throws Exception
    {
        LoggerFactory.getLogger(getClass()).debug("Releasing resource start: " + coordinates.toString());
        terminateEc2Instance();
        deleteSubnet();
        deleteVpc();
        
        provider.config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Down));
        LoggerFactory.getLogger(getClass()).debug("Releasing resource complete: " + coordinates.toString());
        return null;
    }
    
    private void terminateEc2Instance() throws FatalResourceException
    {
        String instanceId = instance.getInstanceId();
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "terminateInstances:" + instanceId);
        do
        {
            try
            {
                TerminateInstancesRequest trequest = new TerminateInstancesRequest().withInstanceIds(instanceId);
                provider.manager.ec2Client.terminateInstances(trequest);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
        DescribeInstancesRequest diRequest = new DescribeInstancesRequest().withInstanceIds(instance.getInstanceId());
        pdelay.reset();
        msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "describeInstances: " + instanceId);
        do
        {
            try
            {
                DescribeInstancesResult diResult = provider.manager.ec2Client.describeInstances(diRequest);
                Instance inst = diResult.getReservations().get(0).getInstances().get(0);
                if (AwsInstanceState.getState(inst.getState().getName()) == AwsInstanceState.Terminated)
                    break;
                pdelay.retry(pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "describeInstances: " + instanceId));
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
    }
    
    private void deleteVpc() throws FatalResourceException
    {
        if(vpcId == null)
            return;
        DeleteVpcRequest drequest = new DeleteVpcRequest().withVpcId(vpcId);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "deleteVpc:" + vpcId);
        do
        {
            try
            {
                provider.manager.ec2Client.deleteVpc(drequest);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
    }
    
    private void deleteSubnet() throws FatalResourceException
    {
        if(subnetId == null)
            return;
        DeleteSubnetRequest request = new DeleteSubnetRequest().withSubnetId(subnetId);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "deleteSubnet:" + subnetId);
        do
        {
            try
            {
                provider.manager.ec2Client.deleteSubnet(request);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
    }
}
