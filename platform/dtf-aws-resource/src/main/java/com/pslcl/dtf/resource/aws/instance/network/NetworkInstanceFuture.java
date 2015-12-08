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
package com.pslcl.dtf.resource.aws.instance.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.IpPermission;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.provider.network.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.network.NetworkReservedResource;

@SuppressWarnings("javadoc")
public class NetworkInstanceFuture implements Callable<NetworkInstance>
{
    public static final String TagNameKey = "Name";
    public static final String TagRunIdKey = "runId";
    public static final String TagTemplateIdKey = "templateId";
    public static final String TagResourceIdKey = "resourceId";

    public static final String Ec2MidStr = "ec2";
    public static final String SgMidStr = "sg";
    public static final String SubnetMidStr = "subnet";
    public static final String NetMidStr = "eni";
    public static final String VpcMidStr = "vpc";
    public static final String KeyPairMidStr = "key";
    public static final String TstIdMidStr = "norunid"; // temporary value set, you know the templateProvider has not called ResourcesManager.setRunId()
    public static final String StatusPrefixStr = "resource-";

    public final NetworkReservedResource reservedResource;
    private final AmazonEC2Client ec2Client;
    private final Logger log;
    private final ProgressiveDelayData pdelayData;

    private volatile String availabilityZone;
    private volatile String vpcCidr;
    private volatile String vpcTenancy;
    private volatile int vpcMaxDelay;
    private volatile int vpcMaxRetries;
    
    private volatile String vpcName;
    private volatile String subnetCidr;
    private volatile String subnetName;
    private volatile int subnetSize;
    private volatile String subnetVpcName;

    private volatile String sgGroupName;
    private volatile String sgGroupId;
    private volatile int sgMaxDelay;
    private volatile int sgMaxRetries;

    private volatile String iamArn;
    private volatile String iamName;
    private volatile String keyName;
    private volatile int ec2MaxDelay;
    private volatile int ec2MaxRetries;

    private final List<IpPermission> permissions;

    public NetworkInstanceFuture(NetworkReservedResource reservedResource, AmazonEC2Client ec2Client, ProgressiveDelayData pdelayData)
    {
        log = LoggerFactory.getLogger(getClass());
        this.reservedResource = reservedResource;
        this.ec2Client = ec2Client;
        this.pdelayData = pdelayData;
        //        pdelayData.coord.runId = pdelayData.getHumanName(TstIdMidStr, null);
        permissions = new ArrayList<IpPermission>();
    }

    @Override
    public NetworkInstance call() throws FatalResourceException
    {
//        try
//        {
//            init();
//            //            reservedResource.groupIdentifier = createSecurityGroup();
//            reservedResource.vpc = createVpc();
//            reservedResource.subnet = createSubnet(reservedResource.vpc.getVpcId());
//            //            reservedResource.net = createNetworkInterface(reservedResource.subnet.getSubnetId(), reservedResource.groupId.getGroupId());
//            reservedResource.ec2Instance = createInstance(reservedResource.groupId, reservedResource.subnet.getSubnetId());
//            //            reservedResource.ec2Instance = createInstance("test", reservedResource.net);
            AwsNetworkInstance retMachineInstance = new AwsNetworkInstance(reservedResource);
            pdelayData.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Ok));
            //      createKeyPair("dtf-uniqueName-keypair");
            //      String netId = createNetworkInterface(subnet.getSubnetId(), groupId.getGroupId());
            //        listSecurityGroups();
            ((AwsNetworkProvider) pdelayData.provider).addBoundNetwork(pdelayData.coord.resourceId, retMachineInstance);
            return retMachineInstance;
//        } catch (FatalResourceException e)
//        {
//            //TODO: as you figure out forceCleanup and optimization of normal releaseFuture cleanup, need to to do possible cleanup on these exceptions
//            throw e;
//        } catch (Throwable t)
//        {
//            log.error(getClass().getSimpleName() + " call method threw a non-FatalResourceException", t);
//            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "call"), t);
//        }
    }
}
