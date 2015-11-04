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

import java.util.List;
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
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.resource.aws.instance.AwsMachineInstance.AwsInstanceState;
import com.pslcl.dtf.resource.aws.instance.AwsMachineInstance.WaitForInstanceState;
import com.pslcl.dtf.resource.aws.provider.AwsMachineProvider.MachineReservedResource;

public class MachineInstanceFuture implements Callable<MachineInstance>
{
    public final MachineReservedResource reservedResource;
    private final AmazonEC2Client ec2Client;
    private final Logger log;
    private final RunnerConfig config;

    public MachineInstanceFuture(MachineReservedResource reservedResource, AmazonEC2Client ec2Client, RunnerConfig config)
    {
        this.reservedResource = reservedResource;
        this.ec2Client = ec2Client;
        this.config = config;
        log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public MachineInstance call() throws Exception
    {
        int pollDelay = 1000;
        int timeout = 10000;
        try
        {
            listSecurityGroups();
            Vpc vpc = createVpc();
            GroupIdentifier groupId = createSecurityGroup(vpc, true);
            createKeyPair("dtf-uniqueName-keypair");
            createInstance(pollDelay, timeout);
            MachineInstance retMachineInstance = new AwsMachineInstance(ReservedResource.class.cast(reservedResource.resource));
            return retMachineInstance;
        } catch (Exception e)
        {
            log.error("run instance failed", e);
            throw e;
        }
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
        // subnetting, first and last ip not useable
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

    private GroupIdentifier createSecurityGroup(Vpc vpc, boolean windows) throws InterruptedException
    {
        //@formatter:off
        CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest()
            .withGroupName("dtf-uniqueName")    //TODO: maybe config prefix?
            .withDescription("dtf created secure group")
            .withVpcId(vpc.getVpcId());
        //@formatter:on
        
        CreateSecurityGroupResult sgResult = ec2Client.createSecurityGroup(csgr);
        GroupIdentifier groupId = new GroupIdentifier().withGroupId(sgResult.getGroupId()).withGroupName("dft-uniqueName");
        createNameTag("dtf-uniqueName", groupId.getGroupId());
        
        IpPermission perm = new IpPermission().withIpProtocol("tcp").withIpRanges("0.0.0.0/0"); //TODO: config
        if(windows)
            perm .withFromPort(3389).withToPort(3389);
        else
            perm.withFromPort(22).withToPort(22);

        AuthorizeSecurityGroupIngressRequest asgir = new AuthorizeSecurityGroupIngressRequest();
        asgir.withIpPermissions(perm).withGroupId(sgResult.getGroupId());
        ec2Client.authorizeSecurityGroupIngress(asgir);
        
//TODO: not sure this is needed        
        boolean found = false;
        do
        {
            Thread.sleep(100);
            DescribeSecurityGroupsRequest dsgr = new DescribeSecurityGroupsRequest();
            List<SecurityGroup> sgList = ec2Client.describeSecurityGroups().getSecurityGroups();
            for (SecurityGroup group : sgList)
            {
                log.info(group.toString());
                if (sgResult.getGroupId().equals(group.getGroupId()))
                {
                    found = true;
                    break;
                }
            }
        } while (!found);
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
        //@formatter:off
        CreateTagsRequest ctr = new CreateTagsRequest()
            .withTags(new Tag("Name", name))
            .withResources(resourceId);
        //@formatter:on
        ec2Client.createTags(ctr);
    }
    
}
