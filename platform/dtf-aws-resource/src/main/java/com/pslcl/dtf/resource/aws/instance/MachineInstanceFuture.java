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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Vpc;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.instance.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.instance.AwsMachineInstance.WaitForInstanceState;
import com.pslcl.dtf.resource.aws.provider.AwsMachineProvider.MachineReservedResource;

public class MachineInstanceFuture implements Callable<MachineInstance>
{
    public static final String ErrorPrefix = "awsMachine-";

    public final MachineReservedResource reservedResource;
    private final AmazonEC2Client ec2Client;
    private final Logger log;
    private final RunnerConfig config;

    private volatile String vpcCidr;
    private volatile String vpcTenancy;
    private volatile int vpcMaxDelay;
    private volatile int vpcMaxRetries;
    private volatile String sgGroupName;
    private volatile String sgGroupId;
    private volatile int sgMaxDelay;
    private volatile int sgMaxRetries;

    private volatile int ec2MaxDelay;
    private volatile int ec2MaxRetries;
    
    private volatile String tstShortName;
    private volatile String tstLongName;

    private final List<IpPermission> permissions;

    public MachineInstanceFuture(MachineReservedResource reservedResource, AmazonEC2Client ec2Client, RunnerConfig config)
    {
        log = LoggerFactory.getLogger(getClass());
        this.reservedResource = reservedResource;
        this.ec2Client = ec2Client;
        this.config = config;
        permissions = new ArrayList<IpPermission>();
    }

    @Override
    public MachineInstance call() throws Exception
    {
        init();
        listSecurityGroups();
        Vpc vpc = createVpc();
        GroupIdentifier groupId = createSecurityGroup(vpc, true);
        createKeyPair("dtf-uniqueName-keypair");
        createInstance(ec2MaxDelay, ec2MaxRetries);
        MachineInstance retMachineInstance = new AwsMachineInstance(ReservedResource.class.cast(reservedResource.resource));
        return retMachineInstance;
    }

    private Instance createInstance(int pollDelay, int timeout) throws Exception
    {
        //http://stackoverflow.com/questions/22365470/launching-instance-vpc-security-groups-may-not-be-used-for-a-non-vpc-launch   
        //@formatter:off
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(reservedResource.imageId)
            .withInstanceType(reservedResource.instanceType)
            .withMinCount(1)
            .withMaxCount(1);
//            .withSecurityGroups(defaultSg.getGroupName())
//            .withKeyName(keypairName);
//            .withSecurityGroups(sgResult.getGroupId());
        //@formatter:off
        RunInstancesResult runResult = ec2Client.runInstances(runInstancesRequest);
        Instance instance  = runResult.getReservation().getInstances().get(0);
        createNameTag("dtf-ec2-something", instance.getInstanceId());
        
        WaitForInstanceState wfis = new WaitForInstanceState(instance, AwsInstanceState.Running, config, pollDelay, timeout);
        wfis.call();       // don't waste a thread on this one
        return instance;
    }
    
    private Vpc createVpc()
    {
        // note that in subnetting, first and last ip not useable
        DescribeVpcsResult vpcsResults = ec2Client.describeVpcs();
        for (Vpc vpc : vpcsResults.getVpcs())
        {
            if (vpc.getCidrBlock().equals("10.0.0.0/16"))
            {
                DeleteVpcRequest dvpcr = new DeleteVpcRequest(vpc.getVpcId());
                ec2Client.deleteVpc(dvpcr);
            }
            log.info(vpc.toString());
        }

        //@formatter:off
        CreateVpcRequest cvpcr = new CreateVpcRequest()
            .withCidrBlock("11.0.0.0/16") 
            .withInstanceTenancy("default");
            //@formatter:on
        Vpc vpc = ec2Client.createVpc(cvpcr).getVpc();
        createNameTag("dtf-vpc-something", vpc.getVpcId());
        return vpc;
    }

    private GroupIdentifier createSecurityGroup(Vpc vpc, boolean windows) throws Exception
    {
        String name = tstShortName + "-sg-" + sgGroupName;
        String description = tstLongName + " security group " + sgGroupName;
        //@formatter:off
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest()
            .withGroupName(name)
            .withDescription(description)
            .withVpcId(vpc.getVpcId());
        //@formatter:on
        
        GroupIdentifier groupId = null;
        ProgressiveDelay pdelay = new ProgressiveDelay(sgMaxDelay, sgMaxRetries, config.statusTracker, tstLongName);
        do
        {
            try
            {
                CreateSecurityGroupResult sgResult = ec2Client.createSecurityGroup(request);
                groupId = new GroupIdentifier().withGroupId(sgResult.getGroupId()).withGroupName(name);
                break;
            } catch (Exception e)
            {
                pdelay.handleException(ErrorPrefix + "createSecurityGroup", e);
            }
        }while(true);
        
        
        DescribeSecurityGroupsRequest dsgRequest = new DescribeSecurityGroupsRequest().withGroupIds(groupId.getGroupId());
        boolean found = false;
        pdelay.reset();
        do
        {
            try
            {
                List<SecurityGroup> sgList = ec2Client.describeSecurityGroups().getSecurityGroups();
                for (SecurityGroup group : sgList)
                {
                    if (groupId.getGroupId().equals(group.getGroupId()))
                    {
                        found = true;
                        break;
                    }
                }
                break;
            } catch (Exception e)
            {
                pdelay.handleException(ErrorPrefix + "describeSecurityGroups: " + groupId.getGroupId(), e);
            }
        }while(true);
        
        createNameTag(name, groupId.getGroupId());

        AuthorizeSecurityGroupIngressRequest ingressRequest = 
                        new AuthorizeSecurityGroupIngressRequest().withIpPermissions(permissions).withGroupId(groupId.getGroupId());

        pdelay.reset();
        do
        {
            try
            {
                ec2Client.authorizeSecurityGroupIngress(ingressRequest);
                break;
            } catch (Exception e)
            {
                pdelay.handleException(ErrorPrefix + "createSecurityGroup", e);
            }
        }while(true);
        return groupId;
    }

    private KeyPair createKeyPair(String keypairName)
    {
        //TODO: allow a configured name?
        DescribeKeyPairsRequest dkpr = new DescribeKeyPairsRequest();
        DescribeKeyPairsResult keyPairsResult = ec2Client.describeKeyPairs(dkpr);
        KeyPairInfo grabAKeyPair = null;
        for (KeyPairInfo pair : keyPairsResult.getKeyPairs())
        {
            log.info(pair.toString());
            grabAKeyPair = pair;
        }

        CreateKeyPairRequest ckpr = new CreateKeyPairRequest();
        ckpr.withKeyName(keypairName); //TODO config?
        CreateKeyPairResult keypairResult = ec2Client.createKeyPair(ckpr);
        KeyPair keypair = keypairResult.getKeyPair();
        String pemContent = keypair.getKeyMaterial(); // write this to file
        keypair.getKeyName();
        return keypair;
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

    private void createNameTag(String name, String resourceId)
    {
        ProgressiveDelay pdelay = new ProgressiveDelay(sgMaxDelay, sgMaxRetries, config.statusTracker, tstLongName);
        CreateTagsRequest ctr = new CreateTagsRequest().withTags(new Tag("Name", name)).withResources(resourceId);
        do
        {
            try
            {
                ec2Client.createTags(ctr);
                break;
            } catch (Exception e)
            {
                pdelay.handleException(ErrorPrefix + "createNameTag", e);
            }
        }while(true);
    }

    private void addPermissions(Map<String, String> map, TabToLevel format) throws Exception
    {
        String[] protocols = null;
        String[] ipRanges = null;
        String[] ports = null;

        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermProtocolKey, map);
        int size = list.size();
        if (size == 0)
        {
            //@formatter:off
                IpPermission perm = new IpPermission()
                    .withIpProtocol(InstanceNames.PermProtocolDefault)
                    .withIpRanges(InstanceNames.PermIpRangeDefault)
                    .withFromPort(Integer.parseInt(InstanceNames.PermPortDefault))
                    .withToPort(Integer.parseInt(InstanceNames.PermPortDefault));
                //@formatter:on
            permissions.add(perm);
            format.ttl("default");
            format.level.incrementAndGet();
            format.ttl(InstanceNames.PermProtocolKey, " = ", InstanceNames.PermProtocolDefault);
            format.ttl(InstanceNames.PermIpRangeKey, " = ", InstanceNames.PermIpRangeDefault);
            format.ttl(InstanceNames.PermPortKey, " = ", InstanceNames.PermPortDefault);
            format.level.decrementAndGet();
            return;
        }
        protocols = new String[size];
        ipRanges = new String[size];
        ports = new String[size];

        for (int i = 0; i < size; i++)
        {
            Entry<String, String> entry = list.get(i);
            protocols[i] = entry.getValue();
        }

        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermIpRangeKey, map);
        if (list.size() != size)
            throw new Exception("Permissions, number of IpRanges does not match number of Protocols, exp: " + size + " rec: " + list.size());
        for (int i = 0; i < size; i++)
        {
            Entry<String, String> entry = list.get(i);
            ipRanges[i] = entry.getValue();
        }

        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermPortKey, map);
        if (list.size() != size)
            throw new Exception("Permissions, number of ports does not match number of Protocols, exp: " + size + " rec: " + list.size());
        for (int i = 0; i < size; i++)
        {
            Entry<String, String> entry = list.get(i);
            ports[i] = entry.getValue();
        }

        for (int i = 0; i < size; i++)
        {
            //@formatter:off
                IpPermission perm = new IpPermission()
                    .withIpProtocol(protocols[i])
                    .withIpRanges(ipRanges[i])
                    .withFromPort(Integer.parseInt(ports[i]))
                    .withToPort(Integer.parseInt(ports[i]));
                //@formatter:on
            permissions.add(perm);
            format.ttl("perm-" + i);
            format.level.incrementAndGet();
            format.ttl(InstanceNames.PermProtocolKey, " = ", protocols[i]);
            format.ttl(InstanceNames.PermIpRangeKey, " = ", ipRanges[i]);
            format.ttl(InstanceNames.PermPortKey, " = ", ports[i]);
            format.level.decrementAndGet();
        }
    }

    private void init() throws FatalResourceException
    {
        try
        {
            TabToLevel format = new TabToLevel(null);
            format.ttl("\nEc2 Instance:");
            format.level.incrementAndGet();
            format.ttl("Vpc:");
            format.level.incrementAndGet();
            Map<String, String> map = reservedResource.resource.getAttributes();
            vpcCidr = StrH.getAttribute(map, InstanceNames.VpcCidrKey, InstanceNames.VpcCidrDefault);
            format.ttl(InstanceNames.VpcCidrKey, " = ", vpcCidr);
            vpcTenancy = StrH.getAttribute(map, InstanceNames.VpcTenancyKey, InstanceNames.VpcTenancyDefault);
            format.ttl(InstanceNames.VpcTenancyKey, " = ", vpcTenancy);
            vpcMaxDelay = StrH.getIntAttribute(map, InstanceNames.VpcMaxDelayKey, InstanceNames.VpcMaxDelayDefault);
            format.ttl(InstanceNames.VpcMaxDelayKey, " = ", vpcMaxDelay);
            vpcMaxRetries = StrH.getIntAttribute(map, InstanceNames.VpcMaxRetriesKey, InstanceNames.VpcMaxRetriesDefault);
            format.ttl(InstanceNames.VpcMaxRetriesKey, " = ", vpcMaxRetries);
            format.level.decrementAndGet();

            format.ttl("SecurityGroup:");
            format.level.incrementAndGet();
            sgGroupName = StrH.getAttribute(map, InstanceNames.SgNameKey, InstanceNames.SgNameDefault);
            format.ttl(InstanceNames.SgNameKey, " = ", sgGroupName);
            sgGroupId = StrH.getAttribute(map, InstanceNames.SgIdKey, InstanceNames.SgIdDefault);
            format.ttl(InstanceNames.SgIdKey, " = ", sgGroupId);
            sgMaxDelay = StrH.getIntAttribute(map, InstanceNames.SgMaxDelayKey, InstanceNames.SgMaxDelayDefault);
            format.ttl(InstanceNames.SgMaxDelayKey, " = ", sgMaxDelay);
            sgMaxRetries = StrH.getIntAttribute(map, InstanceNames.SgMaxRetriesKey, InstanceNames.SgMaxRetriesDefault);
            format.ttl(InstanceNames.SgMaxRetriesKey, " = ", sgMaxRetries);
            format.level.decrementAndGet();

            format.ttl("Permissions:");
            format.level.incrementAndGet();
            addPermissions(map, format);
            format.level.decrementAndGet();

            format.ttl("ec2 instance:");
            format.level.incrementAndGet();
            ec2MaxDelay = StrH.getIntAttribute(map, InstanceNames.Ec2MaxDelayKey, InstanceNames.Ec2MaxDelayDefault);
            format.ttl(InstanceNames.Ec2MaxDelayKey, " = ", ec2MaxDelay);
            ec2MaxRetries = StrH.getIntAttribute(map, InstanceNames.Ec2MaxRetriesKey, InstanceNames.Ec2MaxRetriesDefault);
            format.ttl(InstanceNames.Ec2MaxRetriesKey, " = ", ec2MaxRetries);
            format.level.decrementAndGet();
            
            format.ttl("Test names:");
            format.level.incrementAndGet();
            tstShortName = StrH.getAttribute(map, ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
            format.ttl(ClientNames.TestShortNameKey, " = ", tstShortName);
            tstLongName = StrH.getAttribute(map, ClientNames.TestLongNameKey, ClientNames.TestLongNameDefault);
            format.ttl(ClientNames.TestLongNameKey, " = ", tstLongName);
            log.debug(format.sb.toString());
        } catch (Exception e)
        {
            throw new ProgressiveDelay(0, 0, config.statusTracker, tstLongName).handleException(ErrorPrefix + "init", e);
        }
    }
}
