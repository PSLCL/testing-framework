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
package com.pslcl.dtf.resource.aws.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Vpc;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerException;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.provider.network.NetworkReservedResource;

@SuppressWarnings("javadoc")
public class SubnetManager
{
    public static final int MaxSecurityGroups = 100;
    public static final String SubnetMidStr = "subnet";
    public static final String VpcMidStr = "vpc";
    public static final String SgMidStr = "sg";

    private final Logger log;
    private final HashMap<Long, NetworkReservedResource> reservedResources;
    private final HashMap<String, Vpc> vpcMap;
    private final HashMap<String, List<Subnet>> subnetMap;
    private final HashMap<Long, GroupIdentifier> sgMap; // delete me
    public final AtomicInteger availableSgs;
    private final AwsResourcesManager manager;
    public volatile SubnetConfigData defaultSubnetConfigData;
    private volatile Vpc defaultVpc;
    private volatile Subnet defaultSubnet;

    public SubnetManager(AwsResourcesManager manager)
    {
        log = LoggerFactory.getLogger(getClass());
        reservedResources = new HashMap<Long, NetworkReservedResource>();
        this.manager = manager;
        vpcMap = new HashMap<String, Vpc>();
        subnetMap = new HashMap<String, List<Subnet>>();
        sgMap = new HashMap<Long, GroupIdentifier>();
        availableSgs = new AtomicInteger(MaxSecurityGroups);
    }

    public void releaseSubnet(long resourceId)
    {
        synchronized (reservedResources)
        {
            reservedResources.remove(resourceId);
        }   
    }
    
    public void releaseSecurityGroup(ProgressiveDelayData pdelayData) throws FatalResourceException
    {
        GroupIdentifier groupId = null;
        synchronized (sgMap)
        {
            groupId = sgMap.remove(pdelayData.coord.resourceId);
            if(groupId == null)
                return;
        }
        releaseSecurityGroup(pdelayData, groupId.getGroupId());
        availableSgs.incrementAndGet();
    }

    @SuppressWarnings("null")
    public Vpc getVpc(ProgressiveDelayData pdelayData, SubnetConfigData config) throws FatalResourceException
    {
        if (vpcMap.size() == 0) // not been called yet 
        {
            discoverExistingVpcs(pdelayData, config);
            discoverExistingSubnets(pdelayData, config);
            cleanupExistingSecureGroups(pdelayData, config);
        }

        if (config.vpcName == null)
            return defaultVpc;

        Vpc vpc = vpcMap.get(config.vpcName);
        if (vpc == null)
        {
            CreateVpcRequest cvpcr = new CreateVpcRequest().withCidrBlock(config.vpcCidr).withInstanceTenancy(config.vpcTenancy);
            pdelayData.maxDelay = config.vpcMaxDelay;
            pdelayData.maxRetries = config.vpcMaxRetries;
            ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
            String msg = pdelayData.getHumanName(VpcMidStr, "createVpc");
            do
            {
                try
                {
                    vpc = manager.ec2Client.createVpc(cvpcr).getVpc();
                    break;
                } catch (Exception e)
                {
                    FatalResourceException fre = pdelay.handleException(msg, e);
                    if (fre instanceof FatalException)
                        throw fre;
                }
            } while (true);
            manager.createNameTag(pdelayData, pdelayData.getHumanName(VpcMidStr, null), vpc.getVpcId());
            synchronized (vpcMap)
            {
                vpcMap.put(config.vpcName, vpc);
            }
        }
        return vpc;
    }

    public Subnet getSubnet(ProgressiveDelayData pdelayData, SubnetConfigData config) throws FatalResourceException
    {
        String vpcId = null;
        synchronized (vpcMap)
        {
            String key = config.vpcName;
            if (key == null)
                key = InstanceNames.VpcNameAwsDefault;
            vpcId = vpcMap.get(key).getVpcId();
        }

        synchronized (subnetMap)
        {
            List<Subnet> list = subnetMap.get(vpcId);
            for (Subnet subnet : list)
            {
                if (subnet.getAvailabilityZone().equals(config.availabilityZone))
                    return subnet;
            }
        }

        CreateSubnetRequest request = new CreateSubnetRequest()
            .withVpcId(vpcId).withCidrBlock(config.vpcCidr)
            .withAvailabilityZone(config.availabilityZone);
        
        pdelayData.maxDelay = config.sgMaxDelay;
        pdelayData.maxRetries = config.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        CreateSubnetResult csnResult = null;
        String msg = pdelayData.getHumanName(SubnetMidStr, "createSubnet");
        do
        {
            try
            {
                csnResult = manager.ec2Client.createSubnet(request);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        @SuppressWarnings("null")
        Subnet subnet = csnResult.getSubnet();
        subnet.setMapPublicIpOnLaunch(true);
        manager.createNameTag(pdelayData, pdelayData.getHumanName(SubnetMidStr, null), subnet.getSubnetId());
        synchronized (subnetMap)
        {
            String key = subnet.getVpcId();
            List<Subnet> list = subnetMap.get(key);
            if (list == null)
            {
                list = new ArrayList<Subnet>();
                subnetMap.put(key, list);
            }
            list.add(subnet);
        }
        return subnet;
    }

    @SuppressWarnings("null")
    public GroupIdentifier getSecurityGroup(ProgressiveDelayData pdelayData, List<IpPermission> permissions, SubnetConfigData config) throws FatalResourceException
    {
        String vpcId = null;
        synchronized (vpcMap)
        {
            if(config.vpcName == null)
                vpcId = defaultVpc.getVpcId();
            else
                vpcId = vpcMap.get(config.vpcName).getVpcId();
        }
        
        //@formatter:off
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest()
            .withGroupName(pdelayData.getFullResourceIdName(SgMidStr, null))
            .withDescription(pdelayData.getFullTemplateIdName(SgMidStr, " templateId: " + pdelayData.coord.templateId))
            .withVpcId(vpcId);
        //@formatter:on

        GroupIdentifier groupId = null;
        pdelayData.maxDelay = config.sgMaxDelay;
        pdelayData.maxRetries = config.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        CreateSecurityGroupResult sgResult = null;
        String msg = pdelayData.getHumanName(SgMidStr, "createSecurityGroup");
        do
        {
            try
            {
                sgResult = manager.ec2Client.createSecurityGroup(request);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        groupId = new GroupIdentifier().withGroupId(sgResult.getGroupId()).withGroupName(pdelayData.getFullResourceIdName(SgMidStr, null));

        getSecureGroup(pdelayData, groupId.getGroupId());

        manager.createNameTag(pdelayData, pdelayData.getHumanName(SgMidStr, null), groupId.getGroupId());

        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest().withIpPermissions(permissions).withGroupId(groupId.getGroupId());

        pdelay.reset();
        msg = pdelayData.getHumanName(SgMidStr, "authorizeSecurityGroupIngress" + groupId.getGroupId());
        do
        {
            try
            {
                manager.ec2Client.authorizeSecurityGroupIngress(ingressRequest);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        
        synchronized (sgMap)
        {
            sgMap.put(pdelayData.coord.resourceId, groupId);
            availableSgs.decrementAndGet();
        }
        return groupId;
    }
    
    @SuppressWarnings("null")
    // vps will never be null
    private void discoverExistingVpcs(ProgressiveDelayData pdelayData, SubnetConfigData config) throws FatalResourceException
    {
        DescribeVpcsRequest dvpcr = new DescribeVpcsRequest();
        List<Vpc> vpcs = null;
        pdelayData.maxDelay = config.vpcMaxDelay;
        pdelayData.maxRetries = config.vpcMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(VpcMidStr, "describeVpcs");
        do
        {
            try
            {
                vpcs = manager.ec2Client.describeVpcs(dvpcr).getVpcs();
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);

        synchronized (vpcMap)
        {
            for (Vpc vpc : vpcs)
            {
                String name = AwsResourcesManager.getTagValue(vpc.getTags(), AwsResourcesManager.TagNameKey);
                if (InstanceNames.VpcNameAwsDefault.equals(name))
                    defaultVpc = vpc;
                if (AwsResourcesManager.isDtfObject(vpc.getTags()))
                    vpcMap.put(AwsResourcesManager.getTagValue(vpc.getTags(), AwsResourcesManager.TagNameKey), vpc);
            }
        }

        if (defaultVpc == null)
        {
            if (vpcs.size() > 1)
            {
                log.warn("Can not determine the Default VPC.  More than one, non DTF created vpc exists");
                AwsResourcesManager.handleStatusTracker(pdelayData, StatusTracker.Status.Alert);
                throw new FatalServerException(pdelayData.coord, "can not determine default vpc");
            }
            defaultVpc = vpcs.get(0);
            pdelayData.provider.manager.createNameTag(pdelayData, InstanceNames.VpcNameAwsDefault, defaultVpc.getVpcId());
        }
    }

    private void discoverExistingSubnets(ProgressiveDelayData pdelayData, SubnetConfigData config) throws FatalResourceException
    {
        DescribeSubnetsRequest dvpcr = new DescribeSubnetsRequest();
        List<Subnet> subnets = null;
        pdelayData.maxDelay = config.vpcMaxDelay;
        pdelayData.maxRetries = config.vpcMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SubnetMidStr, "describeSubnets");
        do
        {
            try
            {
                subnets = manager.ec2Client.describeSubnets(dvpcr).getSubnets();
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);

        if (subnets != null)
        {
            synchronized (subnetMap)
            {
                for (Subnet subnet : subnets)
                {
                    String key = subnet.getVpcId();
                    List<Subnet> list = subnetMap.get(key);
                    if (list == null)
                    {
                        list = new ArrayList<Subnet>();
                        subnetMap.put(key, list);
                    }
                    list.add(subnet);
                }
            }
            return;
        }
        log.warn("Can not determine the Subnet");
        AwsResourcesManager.handleStatusTracker(pdelayData, StatusTracker.Status.Alert);
        throw new FatalServerException(pdelayData.coord, "can not determine the subnet");
    }

    private void cleanupExistingSecureGroups(ProgressiveDelayData pdelayData, SubnetConfigData config) throws FatalResourceException
    {
        List<SecurityGroup> sgs = null;
        pdelayData.maxDelay = config.sgMaxDelay;
        pdelayData.maxRetries = config.sgMaxRetries;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SgMidStr, "describeSecurityGroups");
        do
        {
            try
            {
                sgs = manager.ec2Client.describeSecurityGroups().getSecurityGroups();
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);

        if (sgs != null)
        {
            for (SecurityGroup sg: sgs)
            {
                if (AwsResourcesManager.isDtfObject(sg.getTags()))
                {
                    releaseSecurityGroup(pdelayData, sg.getGroupId());
                }else
                    availableSgs.decrementAndGet();
            }
        }
    }
    
    
    private void releaseSecurityGroup(ProgressiveDelayData pdelayData, String groupId)
    {
        DeleteSecurityGroupRequest dsgr = new DeleteSecurityGroupRequest().withGroupId(groupId);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SgMidStr, "deleteSecurityGroup");
        do
        {
            try
            {
                manager.ec2Client.deleteSecurityGroup(dsgr);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                {
                    break;  // best try
//                    throw fre;
                }
            }
        } while (true);
    }

//    public void setSgPermissions(ProgressiveDelayData pdelayData, String groupId, List<IpPermission> permissions) throws FatalResourceException
//    {
//        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest().withIpPermissions(permissions).withGroupId(groupId);
//
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        String msg = pdelayData.getHumanName(SgMidStr, "authorizeSecurityGroupIngress" + groupId);
//        do
//        {
//            try
//            {
//                manager.ec2Client.authorizeSecurityGroupIngress(ingressRequest);
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//    }
//
    public SecurityGroup getSecureGroup(ProgressiveDelayData pdelayData, String groupId) throws FatalResourceException
    {
        DescribeSecurityGroupsRequest dsgRequest = new DescribeSecurityGroupsRequest().withGroupIds(groupId);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SgMidStr, "describeSecurityGroups " + groupId);
        SecurityGroup rvalue = null;
        do
        {
            try
            {
                List<SecurityGroup> sgList = manager.ec2Client.describeSecurityGroups(dsgRequest).getSecurityGroups();
                for (SecurityGroup group : sgList)
                {
                    if (groupId.equals(group.getGroupId()))
                    {
                        rvalue = group;
                        break;
                    }
                }
                if (rvalue != null)
                    break;
                pdelay.retry(msg);
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
        return rvalue;
    }

    public void init(RunnerConfig config) throws Exception
    {
        defaultSubnetConfigData = SubnetConfigData.init(config);
        manager.setTagtimeout(defaultSubnetConfigData.sgMaxDelay, defaultSubnetConfigData.sgMaxRetries);
    }
}