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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.provider.SubnetConfigData;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;
import com.pslcl.dtf.resource.aws.provider.machine.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class MachineInstanceFuture implements Callable<MachineInstance>
{
    public static final String Ec2MidStr = "ec2";
    public static final String NetMidStr = "eni";
    public static final String KeyPairMidStr = "key";
    public static final String TstIdMidStr = "norunid"; // temporary value set, you know the templateProvider has not called ResourcesManager.setRunId()
    public static final String StatusPrefixStr = "resource-";

    public final MachineReservedResource reservedResource;
    private final AmazonEC2Client ec2Client;
    private final Logger log;
    private final ProgressiveDelayData pdelayData;
    private volatile SubnetConfigData subnetConfigData; 
    private volatile MachineConfigData config; 

    public MachineInstanceFuture(MachineReservedResource reservedResource, AmazonEC2Client ec2Client, ProgressiveDelayData pdelayData)
    {
        log = LoggerFactory.getLogger(getClass());
        this.reservedResource = reservedResource;
        this.ec2Client = ec2Client;
        this.pdelayData = pdelayData;
        //        pdelayData.coord.runId = pdelayData.getHumanName(TstIdMidStr, null);
//        permissions = new ArrayList<IpPermission>();
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
            config = MachineConfigData.init(pdelayData, reservedResource.resource, format, pdelayData.manager.machineProvider.defaultMachineConfigData);
            pdelayData.preFixMostName = config.resoucePrefixName;
            reservedResource.vpc = pdelayData.manager.subnetManager.getVpc(pdelayData, config.subnetConfigData);
            reservedResource.subnet = pdelayData.manager.subnetManager.getSubnet(pdelayData, config.subnetConfigData);
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
            log.error(getClass().getSimpleName() + " call method threw a non-FatalResourceException", t);
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
        pdelayData.manager.createNameTag(pdelayData, pdelayData.getHumanName(Ec2MidStr, null), instance.getInstanceId());
        List<GroupIdentifier> sgs = instance.getSecurityGroups();
        reservedResource.groupId = sgs.get(0).getGroupId();
        pdelayData.manager.createNameTag(pdelayData, pdelayData.getHumanName(SubnetManager.SgMidStr, null), reservedResource.groupId);
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
  
//    private String createNetworkInterface(String subnetId, String ... groupId) throws FatalResourceException
//    {
//        CreateNetworkInterfaceRequest request = new CreateNetworkInterfaceRequest()
//            .withDescription("description")
//            .withGroups(groupId)
//            .withSubnetId(subnetId);
//        pdelayData.maxDelay = sgMaxDelay;
//        pdelayData.maxRetries = sgMaxRetries;
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        CreateNetworkInterfaceResult csniResult = null;
//        String msg = pdelayData.getHumanName(NetMidStr, "createNetworkInterface");
//        do
//        {
//            try
//            {
//                csniResult = ec2Client.createNetworkInterface(request);
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if(fre instanceof FatalException)
//                    throw fre;
//            }
//        }while(true);
//        @SuppressWarnings("null")
//        String netId = csniResult.getNetworkInterface().getNetworkInterfaceId(); 
//        createNameTag(pdelayData.getHumanName(NetMidStr, null), netId);
//        return netId;
//    }
//
//    private Subnet createSubnet(String vpcId) throws FatalResourceException
//    {
//        CreateSubnetRequest request = new CreateSubnetRequest().withVpcId(vpcId).withCidrBlock(vpcCidr).withAvailabilityZone(availabilityZone);
//        pdelayData.maxDelay = sgMaxDelay;
//        pdelayData.maxRetries = sgMaxRetries;
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        CreateSubnetResult csnResult = null;
//        String msg = pdelayData.getHumanName(SubnetMidStr, "createSubnet");
//        do
//        {
//            try
//            {
//                csnResult = ec2Client.createSubnet(request);
//                break;
//            }
//            catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if(fre instanceof FatalException)
//                    throw fre;
//            }
//        }while(true);
//        @SuppressWarnings("null")
//        Subnet subnet = csnResult.getSubnet(); 
//        subnet.setMapPublicIpOnLaunch(true);
//        createNameTag(pdelayData.getHumanName(SubnetMidStr, null), subnet.getSubnetId());
//        return subnet;
//    }
//    
//    @SuppressWarnings("null")
//    private Vpc createVpc() throws FatalResourceException
//    {
//        CreateVpcRequest cvpcr = new CreateVpcRequest().withCidrBlock(vpcCidr).withInstanceTenancy(vpcTenancy);
//        Vpc vpc = null;
//        pdelayData.maxDelay = vpcMaxDelay;
//        pdelayData.maxRetries = vpcMaxRetries;
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        String msg = pdelayData.getHumanName(VpcMidStr, "createVpc");
//        do
//        {
//            try
//            {
//                vpc = ec2Client.createVpc(cvpcr).getVpc();
//                break;
//            }
//            catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if(fre instanceof FatalException)
//                    throw fre;
//            }
//        }while(true);
//        createNameTag(pdelayData.getHumanName(VpcMidStr, null), vpc.getVpcId());
//        return vpc;
//    }
//
//    @SuppressWarnings("null")
//    private GroupIdentifier createSecurityGroup() throws FatalResourceException
//    {
//        //@formatter:off
//        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest()
//            .withGroupName(pdelayData.getFullName(SgMidStr, null))
//            .withDescription(pdelayData.getFullName(SgMidStr, " resourceId: " + pdelayData.coord.resourceId));
////            .withVpcId(vpc.getVpcId());
//        //@formatter:on
//
//        GroupIdentifier groupId = null;
//        pdelayData.maxDelay = sgMaxDelay;
//        pdelayData.maxRetries = sgMaxRetries;
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        CreateSecurityGroupResult sgResult = null;
//        String msg = pdelayData.getHumanName(SgMidStr, "createSecurityGroup");
//        do
//        {
//            try
//            {
//                sgResult = ec2Client.createSecurityGroup(request);
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//        groupId = new GroupIdentifier().withGroupId(sgResult.getGroupId()).withGroupName(pdelayData.getFullName(SgMidStr, null));
//
//        getSecureGroup(groupId.getGroupId());
//
//        createNameTag(pdelayData.getHumanName(SgMidStr, null), groupId.getGroupId());
//
//        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest().withIpPermissions(permissions).withGroupId(groupId.getGroupId());
//
//        pdelay.reset();
//        msg = pdelayData.getHumanName(SgMidStr, "authorizeSecurityGroupIngress" + groupId.getGroupId());
//        do
//        {
//            try
//            {
//                ec2Client.authorizeSecurityGroupIngress(ingressRequest);
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//        return groupId;
//    }
//
//    private void setSgPermissions(String groupId) throws FatalResourceException
//    {
//        AuthorizeSecurityGroupIngressRequest ingressRequest = new AuthorizeSecurityGroupIngressRequest().withIpPermissions(permissions).withGroupId(groupId);
//
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        String msg = pdelayData.getHumanName(SgMidStr, "authorizeSecurityGroupIngress" + groupId);
//        do
//        {
//            try
//            {
//                ec2Client.authorizeSecurityGroupIngress(ingressRequest);
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
//    private SecurityGroup getSecureGroup(String groupId) throws FatalResourceException
//    {
//        DescribeSecurityGroupsRequest dsgRequest = new DescribeSecurityGroupsRequest().withGroupIds(groupId);
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        String msg = pdelayData.getHumanName(SgMidStr, "describeSecurityGroups " + groupId);
//        SecurityGroup rvalue = null;
//        do
//        {
//            try
//            {
//                List<SecurityGroup> sgList = ec2Client.describeSecurityGroups(dsgRequest).getSecurityGroups();
//                for (SecurityGroup group : sgList)
//                {
//                    if (groupId.equals(group.getGroupId()))
//                    {
//                        rvalue = group;
//                        break;
//                    }
//                }
//                if (rvalue != null)
//                    break;
//                pdelay.retry(msg);
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//        return rvalue;
//    }

    private String createKeyPair()
    {
        String name = pdelayData.getFullName(KeyPairMidStr, null);
        DescribeKeyPairsRequest dkpr = new DescribeKeyPairsRequest().withKeyNames(name);
        try
        {
            DescribeKeyPairsResult keyPairsResult = ec2Client.describeKeyPairs(dkpr);
            KeyPairInfo grabAKeyPair = null;
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
        String pemContent = keypair.getKeyMaterial(); // write this to file
        keypair.getKeyName();
        return keypair.getKeyName();
    }

    private void listSecurityGroups()
    {
        boolean ok = true;
        SecurityGroup defaultSg = null;
        //TODO: config a name of an existing group to use
        DescribeSecurityGroupsRequest dsgr = new DescribeSecurityGroupsRequest();
        List<SecurityGroup> sgList = ec2Client.describeSecurityGroups().getSecurityGroups();
        for (SecurityGroup group : sgList)
        {
            log.info(group.toString());
            if (group.getGroupName().equals("default"))
            {
                defaultSg = group;
                DeleteSecurityGroupRequest delsgr = new DeleteSecurityGroupRequest().withGroupId(group.getGroupId());
                try
                {
                    ec2Client.deleteSecurityGroup(delsgr);
                } catch (Exception e)
                {
                    log.error("could not delete sg: " + group.toString());
                }
            }
        }
    }

//    private void createNameTag(String name, String resourceId) throws FatalResourceException
//    {
//        pdelayData.maxDelay = sgMaxDelay;
//        pdelayData.maxRetries = sgMaxRetries;
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        //@formatter:off
//        CreateTagsRequest ctr = new 
//                        CreateTagsRequest().withTags(
//                                        new Tag(TagNameKey, name),
//                                        new Tag(TagRunIdKey, Long.toHexString(pdelayData.coord.getRunId())),
//                                        new Tag(TagTemplateIdKey, pdelayData.coord.templateId),
//                                        new Tag(TagResourceIdKey, Long.toHexString(pdelayData.coord.resourceId)))
//                                        .withResources(resourceId);
//        //@formatter:on
//        do
//        {
//            try
//            {
//                ec2Client.createTags(ctr);
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(name + " createTags", e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//    }

//    private void addPermissions(ResourceDescription resource, TabToLevel format) throws Exception
//    {
//        String[] protocols = null;
//        String[] ipRanges = null;
//        String[] ports = null;
//
//        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermProtocolKey, resource.getAttributes());
//        int size = list.size();
//        if (size == 0)
//        {
//            //@formatter:off
//                IpPermission perm = new IpPermission()
//                    .withIpProtocol(InstanceNames.PermProtocolDefault)
//                    .withIpRanges(InstanceNames.PermIpRangeDefault)
//                    .withFromPort(Integer.parseInt(InstanceNames.PermPortDefault))
//                    .withToPort(Integer.parseInt(InstanceNames.PermPortDefault));
//                //@formatter:on
//            permissions.add(perm);
//            format.ttl("default");
//            format.level.incrementAndGet();
//            format.ttl(InstanceNames.PermProtocolKey, " = ", InstanceNames.PermProtocolDefault);
//            format.ttl(InstanceNames.PermIpRangeKey, " = ", InstanceNames.PermIpRangeDefault);
//            format.ttl(InstanceNames.PermPortKey, " = ", InstanceNames.PermPortDefault);
//            format.level.decrementAndGet();
//            return;
//        }
//        protocols = new String[size];
//        ipRanges = new String[size];
//        ports = new String[size];
//
//        for (int i = 0; i < size; i++)
//        {
//            Entry<String, String> entry = list.get(i);
//            protocols[i] = entry.getValue();
//        }
//
//        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermIpRangeKey, resource.getAttributes());
//        if (list.size() != size)
//            throw new Exception("Permissions, number of IpRanges does not match number of Protocols, exp: " + size + " rec: " + list.size());
//        for (int i = 0; i < size; i++)
//        {
//            Entry<String, String> entry = list.get(i);
//            ipRanges[i] = entry.getValue();
//        }
//
//        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermPortKey, resource.getAttributes());
//        if (list.size() != size)
//            throw new Exception("Permissions, number of ports does not match number of Protocols, exp: " + size + " rec: " + list.size());
//        for (int i = 0; i < size; i++)
//        {
//            Entry<String, String> entry = list.get(i);
//            ports[i] = entry.getValue();
//        }
//
//        for (int i = 0; i < size; i++)
//        {
//            //@formatter:off
//                IpPermission perm = new IpPermission()
//                    .withIpProtocol(protocols[i])
//                    .withIpRanges(ipRanges[i])
//                    .withFromPort(Integer.parseInt(ports[i]))
//                    .withToPort(Integer.parseInt(ports[i]));
//                //@formatter:on
//            permissions.add(perm);
//            format.ttl("perm-" + i);
//            format.level.incrementAndGet();
//            format.ttl(InstanceNames.PermProtocolKey, " = ", protocols[i]);
//            format.ttl(InstanceNames.PermIpRangeKey, " = ", ipRanges[i]);
//            format.ttl(InstanceNames.PermPortKey, " = ", ports[i]);
//            format.level.decrementAndGet();
//        }
//    }

//    private void init(ProgressiveDelayData pdelayData) throws FatalResourceException
//    {
//        try
//        {
//            TabToLevel format = new TabToLevel();
//            format.ttl("\nEc2 Instance:");
//            format.level.incrementAndGet();
//            format.ttl(pdelayData.coord.toString(format));
//
//            SubnetConfigData.init(reservedResource, format, pdelayData.manager.subnetManager.defaultData);
//            
//            format.ttl("Vpc:");
//            format.level.incrementAndGet();
//            vpcName = getAttribute(InstanceNames.VpcNameKey, InstanceNames.VpcNameDefault, reservedResource.resource, format);
//            vpcCidr = getAttribute(InstanceNames.VpcCidrKey, InstanceNames.VpcCidrDefault, reservedResource.resource, format);
//            vpcTenancy = getAttribute(InstanceNames.VpcTenancyKey, InstanceNames.VpcTenancyDefault, reservedResource.resource, format);
//            vpcMaxDelay = Integer.parseInt(getAttribute(InstanceNames.VpcMaxDelayKey, InstanceNames.VpcMaxDelayDefault, reservedResource.resource, format));
//            vpcMaxRetries = Integer.parseInt(getAttribute(InstanceNames.VpcMaxRetriesKey, InstanceNames.VpcMaxRetriesDefault, reservedResource.resource, format));
//            format.level.decrementAndGet();
//            
//            format.ttl("Subnet:");
//            format.level.incrementAndGet();
//            subnetName = getAttribute(InstanceNames.SubnetNameKey, InstanceNames.SubnetNameDefault, reservedResource.resource, format);
//            subnetSize = Integer.parseInt(getAttribute(InstanceNames.SubnetSizeKey, InstanceNames.SubnetSizeDefault, reservedResource.resource, format));
//            subnetCidr = getAttribute(InstanceNames.SubnetCidrKey, InstanceNames.SubnetCidrDefault, reservedResource.resource, format);
//            subnetVpcName = getAttribute(InstanceNames.SubnetVpcNameKey, InstanceNames.SubnetVpcNameDefault, reservedResource.resource, format);
//            format.level.decrementAndGet();
//
//            format.ttl("SecurityGroup:");
//            format.level.incrementAndGet();
//            sgGroupName = getAttribute(InstanceNames.SgNameKey, InstanceNames.SgNameDefault, reservedResource.resource, format);
//            sgGroupId = getAttribute(InstanceNames.SgIdKey, InstanceNames.SgIdDefault, reservedResource.resource, format);
//            sgMaxDelay = Integer.parseInt(getAttribute(InstanceNames.SgMaxDelayKey, InstanceNames.SgMaxDelayDefault, reservedResource.resource, format));
//            sgMaxRetries = Integer.parseInt(getAttribute(InstanceNames.SgMaxRetriesKey, InstanceNames.SgMaxRetriesDefault, reservedResource.resource, format));
//            format.level.decrementAndGet();
//
//            format.ttl("Permissions:");
//            format.level.incrementAndGet();
//            addPermissions(reservedResource.resource, format);
//            format.level.decrementAndGet();
//
//            format.ttl("ec2 instance:");
//            format.level.incrementAndGet();
//            availabilityZone = getAttribute(InstanceNames.AvailabilityZoneKey, InstanceNames.AvailabilityZoneDefault, reservedResource.resource, format);
//            ec2MaxDelay = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxDelayKey, InstanceNames.Ec2MaxDelayDefault, reservedResource.resource, format));
//            ec2MaxRetries = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxRetriesKey, InstanceNames.Ec2MaxRetriesDefault, reservedResource.resource, format));
//            iamArn = getAttribute(InstanceNames.Ec2IamArnKey, null, reservedResource.resource, format);
//            iamName = getAttribute(InstanceNames.Ec2IamNameKey, null, reservedResource.resource, format);
//            keyName = getAttribute(InstanceNames.Ec2KeyPairNameKey, null, reservedResource.resource, format);
//            format.level.decrementAndGet();
//
//            format.ttl("Test names:");
//            format.level.incrementAndGet();
//            pdelayData.preFixMostName = getAttribute(ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault, reservedResource.resource, format);
//            log.debug(format.sb.toString());
//        } catch (Exception e)
//        {
//            throw new ProgressiveDelay(pdelayData).handleException(pdelayData.getHumanName("dtf", "init"), e);
//        }
//    }
//    
//    private String getAttribute(String key, String defaultValue, ResourceDescription resource, TabToLevel format)
//    {
//        String value = resource.getAttributes().get(key);
//        if(value == null)
//        {
//            value = defaultValue;
//            resource.addAttribute(key, value);
//            format.ttl(key, " = ", value, " (default injected)");
//        }else
//            format.ttl(key, " = ", value);
//        return value;
//    }
}