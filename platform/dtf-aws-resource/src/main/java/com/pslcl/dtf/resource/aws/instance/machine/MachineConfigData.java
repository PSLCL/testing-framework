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

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.provider.SubnetConfigData;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;

@SuppressWarnings("javadoc")
public class MachineConfigData
{
    public volatile SubnetConfigData subnetConfigData;
    public volatile String iamArn;
    public volatile String iamName;
    public volatile String keyName;
    public volatile int ec2MaxDelay;
    public volatile int ec2MaxRetries;
    public volatile String resoucePrefixName;
    public volatile String userData;
    public volatile String deploySandboxPath;
    
    private MachineConfigData()
    {
    }

    public static MachineConfigData init(ProgressiveDelayData pdelayData, ResourceDescription resource, TabToLevel format, MachineConfigData defaultData) throws Exception
    {
        MachineConfigData data = new MachineConfigData();
        format.ttl(MachineConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();
        
        format.ttl("\nEc2 Instance:");
        format.level.incrementAndGet();
        format.ttl(pdelayData.coord.toString(format));
        data.ec2MaxDelay = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxDelayKey, ""+defaultData.ec2MaxDelay, resource, format));
        data.ec2MaxRetries = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxRetriesKey, ""+defaultData.ec2MaxRetries, resource, format));
        data.iamArn = getAttribute(InstanceNames.Ec2IamArnKey, null, resource, format);
        data.iamName = getAttribute(InstanceNames.Ec2IamNameKey, null, resource, format);
        data.keyName = getAttribute(InstanceNames.Ec2KeyPairNameKey, defaultData.keyName, resource, format);
        data.userData = getAttribute(InstanceNames.Ec2UserDataKey, defaultData.userData, resource, format);
        data.deploySandboxPath = getAttribute(ResourceNames.DeployDestSandboxKey, defaultData.deploySandboxPath, resource, format);
        format.level.decrementAndGet();

        data.subnetConfigData = SubnetConfigData.init(resource, format, pdelayData.provider.manager.subnetManager.defaultSubnetConfigData);
        
        format.ttl("Test name prefix:");
        format.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(ClientNames.TestShortNameKey, defaultData.resoucePrefixName, resource, format);
        LoggerFactory.getLogger(MachineConfigData.class).debug(format.sb.toString());
        return data;
    }
    
    public static MachineConfigData init(RunnerConfig config) throws Exception
    {
        MachineConfigData data = new MachineConfigData();
        config.initsb.ttl(SubnetManager.class.getSimpleName() + " init:");
        config.initsb.level.incrementAndGet();
        
        config.initsb.ttl("\nEc2 defaults:");
        config.initsb.level.incrementAndGet();
        data.ec2MaxDelay = Integer.parseInt(getAttribute(config, InstanceNames.Ec2MaxDelayKey, InstanceNames.Ec2MaxDelayDefault));
        data.ec2MaxRetries = Integer.parseInt(getAttribute(config, InstanceNames.Ec2MaxRetriesKey, InstanceNames.Ec2MaxRetriesDefault));
        data.iamArn = getAttribute(config, InstanceNames.Ec2IamArnKey, null);
        data.iamName = getAttribute(config, InstanceNames.Ec2IamNameKey, null);
        data.keyName = getAttribute(config, InstanceNames.Ec2KeyPairNameKey, null);
        data.userData = getAttribute(config, InstanceNames.Ec2UserDataKey, InstanceNames.Ec2UserDataDefault);
        data.deploySandboxPath = getAttribute(config, ResourceNames.DeployDestSandboxKey, ResourceNames.DeployDestSandboxDefault);
        
        config.initsb.level.decrementAndGet();

        data.subnetConfigData = SubnetConfigData.init(config);
        
        config.initsb.ttl("Test name prefix:");
        config.initsb.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(config, ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        return data;
    }
    
    private static String getAttribute(String key, String defaultValue, ResourceDescription resource, TabToLevel format)
    {
        String value = resource.getAttributes().get(key);
        if(value == null)
        {
            value = defaultValue;
            resource.addAttribute(key, value);
            format.ttl(key, " = ", value, " (default injected)");
        }else
            format.ttl(key, " = ", value);
        return value;
    }
    
    private static String getAttribute(RunnerConfig config, String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        config.initsb.ttl(key, " = ", value);
        return value;
    }
}