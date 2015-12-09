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

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;
import com.pslcl.dtf.resource.aws.provider.machine.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class MachineInstanceFuture implements Callable<MachineInstance>
{
    public static final String Ec2MidStr = "ec2";
    public static final String KeyPairMidStr = "key";

    public final MachineReservedResource reservedResource;
    private final AmazonEC2Client ec2Client;
    private final ProgressiveDelayData pdelayData;
    private volatile MachineConfigData config; 

    public MachineInstanceFuture(MachineReservedResource reservedResource, ProgressiveDelayData pdelayData)
    {
        this.reservedResource = reservedResource;
        this.pdelayData = pdelayData;
        ec2Client = pdelayData.provider.manager.ec2Client;
    }

    @Override
    public MachineInstance call() throws FatalResourceException
    {
        try
        {
            TabToLevel format = new TabToLevel();
            format.ttl("\nEc2 Instance:");
            format.level.incrementAndGet();
            format.ttl(pdelayData.coord.toString(format));
            config = MachineConfigData.init(pdelayData, reservedResource.resource, format, pdelayData.provider.manager.machineProvider.defaultMachineConfigData);
            pdelayData.preFixMostName = config.resoucePrefixName;
            reservedResource.vpc = pdelayData.provider.manager.subnetManager.getVpc(pdelayData, config.subnetConfigData);
            reservedResource.subnet = pdelayData.provider.manager.subnetManager.getSubnet(pdelayData, config.subnetConfigData);
            //            reservedResource.groupIdentifier = createSecurityGroup();
//             = createVpc();
//            reservedResource.subnet = createSubnet(reservedResource.vpc.getVpcId());
            //            reservedResource.net = createNetworkInterface(reservedResource.subnet.getSubnetId(), reservedResource.groupId.getGroupId());
            reservedResource.ec2Instance = createInstance(reservedResource.subnet.getSubnetId());
            //            reservedResource.ec2Instance = createInstance("test", reservedResource.net);
            AwsMachineInstance retMachineInstance = new AwsMachineInstance(reservedResource);
            pdelayData.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Ok));
            //      createKeyPair("dtf-uniqueName-keypair");
            //      String netId = createNetworkInterface(subnet.getSubnetId(), groupId.getGroupId());
            //        listSecurityGroups();
            ((AwsMachineProvider) pdelayData.provider).addBoundInstance(pdelayData.coord.resourceId, retMachineInstance);
            return retMachineInstance;
        } catch (FatalResourceException e)
        {
            //TODO: as you figure out forceCleanup and optimization of normal releaseFuture cleanup, need to to do possible cleanup on these exceptions
            throw e;
        } catch (Throwable t)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + " call method threw a non-FatalResourceException", t);
            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), t);
        }
    }

    private Instance createInstance(String subnetId) throws FatalResourceException
    {
        //http://stackoverflow.com/questions/22365470/launching-instance-vpc-security-groups-may-not-be-used-for-a-non-vpc-launch
        if (config.keyName == null)
            config.keyName = createKeyPair();

        //@formatter:off
        Placement placement = new Placement().withAvailabilityZone(config.subnetConfigData.availabilityZone);
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(reservedResource.imageId)
            .withInstanceType(reservedResource.instanceType)
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(config.keyName)
            .withSubnetId(subnetId)
//            .withSecurityGroupIds(sgGroupId)
            .withPlacement(placement);
        //@formatter:off

        if(config.iamArn != null && config.iamName != null)
        {
            IamInstanceProfileSpecification profile = new IamInstanceProfileSpecification().withArn(config.iamArn).withName(config.iamName);
            runInstancesRequest.setIamInstanceProfile(profile);
        }
        
        RunInstancesResult runResult = null;
        pdelayData.maxDelay = config.ec2MaxDelay;
        pdelayData.maxRetries = config.ec2MaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(Ec2MidStr, "runInstances");
        do
        {
            try
            {
                runResult = ec2Client.runInstances(runInstancesRequest);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
        
        @SuppressWarnings("null")
        Instance instance  = runResult.getReservation().getInstances().get(0);
        pdelayData.provider.manager.createNameTag(pdelayData, pdelayData.getHumanName(Ec2MidStr, null), instance.getInstanceId());
        List<GroupIdentifier> sgs = instance.getSecurityGroups();
        reservedResource.groupId = sgs.get(0).getGroupId();
        pdelayData.provider.manager.createNameTag(pdelayData, pdelayData.getHumanName(SubnetManager.SgMidStr, null), reservedResource.groupId);
//        setSgPermissions(reservedResource.groupId);
        DescribeInstancesRequest diRequest = new DescribeInstancesRequest().withInstanceIds(instance.getInstanceId());
        pdelay.reset();
        msg = pdelayData.getHumanName(Ec2MidStr, "describeInstances");
        do
        {
            try
            {
                DescribeInstancesResult diResult = ec2Client.describeInstances(diRequest);
                Instance inst = diResult.getReservations().get(0).getInstances().get(0);
                if (AwsInstanceState.getState(inst.getState().getName()) == AwsInstanceState.Running)
                    break;
                pdelay.retry(pdelayData.getHumanName(Ec2MidStr, "describeInstances"));
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if(fre instanceof FatalException)
                    throw fre;
            }
        }while(true);
        return instance;
    }
  
    private String createKeyPair()
    {
        synchronized (pdelayData.provider)
        {
            String name = pdelayData.getFullTemplateIdName(KeyPairMidStr, null);
            DescribeKeyPairsRequest dkpr = new DescribeKeyPairsRequest().withKeyNames(name);
            try
            {
                DescribeKeyPairsResult keyPairsResult = ec2Client.describeKeyPairs(dkpr);
                for (KeyPairInfo pair : keyPairsResult.getKeyPairs())
                {
                    name.equals(pair.getKeyName());
                    return name;
                }
            } catch (Exception e)
            {
            }
            CreateKeyPairRequest ckpr = new CreateKeyPairRequest().withKeyName(name);
            CreateKeyPairResult keypairResult = ec2Client.createKeyPair(ckpr);
            KeyPair keypair = keypairResult.getKeyPair();
            keypair.getKeyName();
            return keypair.getKeyName();
        }
    }
}
