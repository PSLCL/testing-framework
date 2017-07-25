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

import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.exception.FatalClientException;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerTimeoutException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.futures.PingFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;
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
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("MachineInstanceFuture");
        try
        {
            reservedResource.format.ttl("preFixMostName = ", pdelayData.preFixMostName);
            checkFutureCanceled();
            config = MachineConfigData.init(pdelayData, reservedResource.resource, reservedResource.format, pdelayData.provider.manager.machineProvider.defaultMachineConfigData);
            checkFutureCanceled();
            AwsMachineInstance machineInstance = ((AwsMachineProvider) pdelayData.provider).checkForReuse(reservedResource);
            if (machineInstance == null)
            {
                pdelayData.preFixMostName = config.resourcePrefixName;
                reservedResource.vpc = pdelayData.provider.manager.subnetManager.getVpc(pdelayData, config.subnetConfigData);
                checkFutureCanceled();
                reservedResource.subnet = pdelayData.provider.manager.subnetManager.getSubnet(pdelayData, config.subnetConfigData);
                checkFutureCanceled();
                String sgDefaultVpcOverrideId = config.subnetConfigData.sgDefaultVpcOverrideId;
                if(sgDefaultVpcOverrideId != null)
                {
                    pdelayData.provider.manager.subnetManager.getSecureGroup(pdelayData, sgDefaultVpcOverrideId);
                    checkFutureCanceled();
                }
                createInstance(reservedResource.subnet.getSubnetId(), sgDefaultVpcOverrideId);
                machineInstance = new AwsMachineInstance(reservedResource, config, pdelayData.provider.config);
                pdelayData.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Ok));
            }
            ((AwsMachineProvider) pdelayData.provider).addBoundInstance(pdelayData.coord.resourceId, machineInstance);
            LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + "- bound: " + reservedResource.format.toString());
            Thread.currentThread().setName(tname);
            return machineInstance;
        } catch (CancellationException ie)
        {
            Thread.currentThread().setName(tname);
            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), ie);
        } catch (FatalResourceException e)
        {
            Thread.currentThread().setName(tname);
            throw e;
        } catch (Throwable t)
        {
            Thread.currentThread().setName(tname);
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + " call method threw a non-FatalResourceException", t);
            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), t);
        }
    }

    private void checkFutureCanceled()
    {
        if (reservedResource.bindFutureCanceled.get())
            throw new CancellationException();
    }

    private void createInstance(String subnetId, String sgDefaultVpcOverrideId) throws FatalResourceException
    {
        //http://stackoverflow.com/questions/22365470/launching-instance-vpc-security-groups-may-not-be-used-for-a-non-vpc-launch
        if (config.keyName == null)
            config.keyName = createKeyPair();

        Base64.Encoder encoder = Base64.getMimeEncoder();
        String userData = encoder.encodeToString(config.linuxUserData.getBytes());
        if (config.windows)
            userData = encoder.encodeToString(config.winUserData.getBytes());
        //@formatter:off
        Placement placement = new Placement().withAvailabilityZone(pdelayData.provider.manager.ec2cconfig.availabilityZone);
        
        pdelayData.maxDelay = config.subnetConfigData.sgMaxDelay;
        pdelayData.maxRetries = config.subnetConfigData.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        RootBlockDeviceInfo rootBlockDeviceInfo = getImageDiskInfo(reservedResource.imageId, pdelay);
        int volumeSize = (int)config.rootDiskSize;
        if(config.rootDiskSize - volumeSize != 0.0)
            ++volumeSize;
        if(rootBlockDeviceInfo.minDeviceSize > volumeSize)
            volumeSize = rootBlockDeviceInfo.minDeviceSize;
        EbsBlockDevice ebsBlockDevice = new EbsBlockDevice().withVolumeSize(volumeSize);
        BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping().withDeviceName(rootBlockDeviceInfo.deviceName).withEbs(ebsBlockDevice);
        
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(reservedResource.imageId)
            .withInstanceType(reservedResource.instanceType)
            .withMinCount(1)
            .withMaxCount(1)
            .withKeyName(config.keyName)
            .withSubnetId(subnetId)
            .withUserData(userData)
            .withBlockDeviceMappings(blockDeviceMapping)
            .withPlacement(placement);
        //@formatter:on
        if(sgDefaultVpcOverrideId != null)
            runInstancesRequest.withSecurityGroupIds(sgDefaultVpcOverrideId);

        if (config.iamArn != null)// && config.iamName != null)
        {
            IamInstanceProfileSpecification profile = new IamInstanceProfileSpecification().withArn(config.iamArn); //.withName(config.iamName);
            runInstancesRequest.setIamInstanceProfile(profile);
        }

        RunInstancesResult runResult = null;
        pdelayData.maxDelay = config.ec2MaxDelay;
        pdelayData.maxRetries = config.ec2MaxRetries;
        pdelay.reset();
        String msg = pdelayData.getHumanName(Ec2MidStr, "runInstances");
        synchronized (reservedResource)
        {
            // possibility of template release being called before all bind requests for that template have completed.
            // this synch is required to guarantee that all ec2Client.runInstances success calls are seen by the 
            // AwsMachineProvider.release code.
            checkFutureCanceled();
            do
            {
                try
                {
                    pdelayData.provider.manager.awsThrottle();
                    runResult = ec2Client.runInstances(runInstancesRequest);
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                    {
                        LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + "- bind failed: " + reservedResource.format.toString());
                        throw fre;
                    }
                }
            } while (true);

            if (runResult != null) // get rid of possible null warning
                reservedResource.ec2Instance = runResult.getReservation().getInstances().get(0);
        }
        waitForState(pdelay, AwsInstanceState.Running);
        pdelayData.provider.manager.createNameTag(pdelayData, pdelayData.getHumanName(Ec2MidStr, null), reservedResource.ec2Instance.getInstanceId());
        reservedResource.resource.getAttributes().put(ResourceNames.DnsHostKey, reservedResource.ec2Instance.getPublicDnsName());
//        waitForStaf(pdelay);
    }

    private void waitForState(ProgressiveDelay pdelay, AwsInstanceState... states) throws FatalResourceException
    {
        DescribeInstancesRequest diRequest = new DescribeInstancesRequest().withInstanceIds(reservedResource.ec2Instance.getInstanceId());
        pdelayData.maxDelay = config.ec2MaxDelay;
        pdelayData.maxRetries = config.ec2MaxRetries;
        pdelay.reset();
        String msg = pdelayData.getHumanName(Ec2MidStr, "describeInstances");
        int count = 0;
        do
        {
            checkFutureCanceled();
            try
            {
                pdelayData.provider.manager.awsThrottle();
                DescribeInstancesResult diResult = ec2Client.describeInstances(diRequest);
                Instance inst = diResult.getReservations().get(0).getInstances().get(0);
                if (inst != null)
                {
                    boolean found = false;
                    for (int i = 0; i < states.length; i++)
                    {
                        if (AwsInstanceState.getState(inst.getState().getName()) == states[i])
                        {
                            found = true;
                            break;
                        }
                    }
                    if (found)
                    {
                        synchronized (reservedResource)
                        {
                            reservedResource.ec2Instance = inst; // this picks up the running information i.e. public ip addresses
                            break;
                        }
                    }
                }
                pdelay.retry(pdelayData.getHumanName(Ec2MidStr, "describeInstances"));
            } catch (Exception e)
            {
                if(count++ < 3)
                {
                    pdelay.retry(pdelayData.getHumanName(Ec2MidStr, "describeInstances"));
                }else
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                        throw fre;
                }
            }
        } while (true);
    }
    
    private void waitForStaf(ProgressiveDelay pdelay) throws FatalResourceException
    {
        ProcessCommandData cmdData = new ProcessCommandData(null, null, null, false, false, null, null, null, false);
        StafRunnableProgram runnableProgram;
        try
        {
            runnableProgram = new StafRunnableProgram(cmdData);
        } catch (Exception e1)
        {
            throw new FatalResourceException(reservedResource.resource.getCoordinates(), "failed to obtain StafRunnableProgram", e1);
        }
        String address = reservedResource.ec2Instance.getPublicIpAddress();
        if(address == null || address.length() < 1)
            throw new FatalClientException(reservedResource.resource.getCoordinates(), " No public IP Address on new EC2 instance, check default SG or configured SG rules.");
        cmdData.setHost(address);
        cmdData.setWait(true);

        //@formatter:off
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(
                        (AwsResourceProvider)reservedResource.resource.getCoordinates().getProvider(), 
                        reservedResource.resource.getCoordinates());
        //@formatter:on

        pdelayData.maxDelay = config.ec2MaxDelay;
        pdelayData.maxRetries = config.ec2MaxRetries;
        pdelay.reset();
        String msg = pdelayData.getHumanName(Ec2MidStr, "stafPing");
        do
        {
            if (reservedResource.bindFutureCanceled.get())
                throw new CancellationException();
            try
            {
                PingFuture pingFuture = new PingFuture(runnableProgram);
                Future<Integer> future = reservedResource.provider.config.blockingExecutor.submit(pingFuture); 
                Integer rc = future.get();
                if (rc != null && rc == 0)
                    break;
                pdelay.retry(msg);
            } catch (FatalServerTimeoutException fstoe)
            {
                throw fstoe;
            } catch (Exception e)
            {
                try
                {
                    pdelay.retry(msg);
                } catch (FatalServerTimeoutException fstoe)
                {
                    throw fstoe;
                }
            }
        } while (true);
    }

    private RootBlockDeviceInfo getImageDiskInfo(String amiId, ProgressiveDelay pdelay) throws FatalResourceException
    {
        DescribeImagesRequest diRequest = new DescribeImagesRequest().withImageIds(amiId);
        String msg = pdelayData.getHumanName(Ec2MidStr, "describeImage");
        do
        {
            checkFutureCanceled();
            try
            {
                pdelayData.provider.manager.awsThrottle();
                DescribeImagesResult diResult = ec2Client.describeImages(diRequest);
                Image image = diResult.getImages().get(0);
                List<BlockDeviceMapping> blockDevices = image.getBlockDeviceMappings();
                BlockDeviceMapping blockDevice = blockDevices.get(0);
                String deviceName = blockDevice.getDeviceName();
                EbsBlockDevice ebs = blockDevice.getEbs();
                int size = ebs.getVolumeSize();
                return new RootBlockDeviceInfo(deviceName, size);
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
    }

    private String createKeyPair() throws FatalResourceException
    {
        synchronized (pdelayData.provider)
        {
            String name = pdelayData.getKeyPairName(KeyPairMidStr);
            ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
            String msg = pdelayData.getHumanName(KeyPairMidStr, "describeKeyPairs");
            DescribeKeyPairsRequest dkpr = new DescribeKeyPairsRequest();//.withKeyNames(name);
            DescribeKeyPairsResult keyPairsResult = null;
            do
            {
                try
                {
                    pdelayData.provider.manager.awsThrottle();
                    keyPairsResult = ec2Client.describeKeyPairs(dkpr);
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                        throw fre;
                }
            } while (true);

            for (KeyPairInfo pair : keyPairsResult.getKeyPairs())
            {
                if (name.equals(pair.getKeyName()))
                    return name;
            }

            CreateKeyPairRequest ckpr = new CreateKeyPairRequest().withKeyName(name);
            CreateKeyPairResult keypairResult = null;
            pdelay.reset();
            msg = pdelayData.getHumanName(KeyPairMidStr, "createKeyPair");
            do
            {
                try
                {
                    pdelayData.provider.manager.awsThrottle();
                    keypairResult = ec2Client.createKeyPair(ckpr);
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                        throw fre;
                }
            } while (true);
            KeyPair keypair = keypairResult.getKeyPair();

            return keypair.getKeyName();
        }
    }

    private class RootBlockDeviceInfo
    {
        private final String deviceName;
        private final int minDeviceSize;

        private RootBlockDeviceInfo(String deviceName, int minDeviceSize)
        {
            this.deviceName = deviceName;
            this.minDeviceSize = minDeviceSize;
        }
    }
}
