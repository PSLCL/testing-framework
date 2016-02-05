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
    public volatile boolean windows;
    public volatile String linuxUserData;
    public volatile String winUserData;
    public volatile String linuxSandboxPath;
    public volatile String winSandboxPath;
    public volatile int stallReleaseMinutes;   
    
    private MachineConfigData()
    {
    }

    public static MachineConfigData init(ProgressiveDelayData pdelayData, ResourceDescription resource, TabToLevel format, MachineConfigData defaultData) throws Exception
    {
        MachineConfigData data = new MachineConfigData();
        format.ttl(MachineConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();

        format.ttl("Test name prefix:");
        format.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(ResourceNames.ResourceShortNameKey, defaultData.resoucePrefixName, resource, format);
        LoggerFactory.getLogger(MachineConfigData.class).debug(format.sb.toString());
        format.level.decrementAndGet();
        
        format.ttl("\nEc2 Instance:");
        format.level.incrementAndGet();
        format.ttl(pdelayData.coord.toString(format));
        data.ec2MaxDelay = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxDelayKey, ""+defaultData.ec2MaxDelay, resource, format));
        data.ec2MaxRetries = Integer.parseInt(getAttribute(InstanceNames.Ec2MaxRetriesKey, ""+defaultData.ec2MaxRetries, resource, format));
        data.stallReleaseMinutes = Integer.parseInt(getAttribute(InstanceNames.Ec2StallReleaseKey, ""+defaultData.stallReleaseMinutes, resource, format));
        data.iamArn = getAttribute(InstanceNames.Ec2IamArnKey, defaultData.iamArn, resource, format);
        data.iamName = getAttribute(InstanceNames.Ec2IamNameKey, defaultData.iamName, resource, format);
        data.keyName = getAttribute(InstanceNames.Ec2KeyPairNameKey, defaultData.keyName, resource, format);
        data.windows = Boolean.parseBoolean(getAttribute(InstanceNames.Ec2WindowsKey, ""+defaultData.windows, resource, format));
        data.linuxUserData = getAttribute(InstanceNames.Ec2LinuxUserDataKey, defaultData.linuxUserData, resource, format);
        data.winUserData = getAttribute(InstanceNames.Ec2WinUserDataKey, defaultData.winUserData, resource, format);
        data.linuxSandboxPath = getAttribute(ResourceNames.DeployLinuxSandboxKey, defaultData.linuxSandboxPath, resource, format);
        data.winSandboxPath = getAttribute(ResourceNames.DeployWinSandboxKey, defaultData.winSandboxPath, resource, format);
        format.level.decrementAndGet();

        data.subnetConfigData = SubnetConfigData.init(resource, format, pdelayData.provider.manager.subnetManager.defaultSubnetConfigData);
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
        data.stallReleaseMinutes = Integer.parseInt(getAttribute(config, InstanceNames.Ec2StallReleaseKey, InstanceNames.Ec2StallReleaseDefault));
        data.iamArn = getAttribute(config, InstanceNames.Ec2IamArnKey, null);
        data.iamName = getAttribute(config, InstanceNames.Ec2IamNameKey, null);
        data.keyName = getAttribute(config, InstanceNames.Ec2KeyPairNameKey, null);
        data.windows = Boolean.parseBoolean(getAttribute(config, InstanceNames.Ec2WindowsKey, InstanceNames.Ec2WindowsDefault));
        data.linuxUserData = getAttribute(config, InstanceNames.Ec2LinuxUserDataKey, InstanceNames.Ec2LinuxUserDataDefault);
        data.winUserData = getAttribute(config, InstanceNames.Ec2WinUserDataKey, InstanceNames.Ec2WinUserDataDefault);
        data.linuxSandboxPath = getAttribute(config, ResourceNames.DeployLinuxSandboxKey, ResourceNames.DeployLinuxSandboxDefault);
        data.winSandboxPath = getAttribute(config, ResourceNames.DeployWinSandboxKey, ResourceNames.DeployWinSandboxDefault);
        
        config.initsb.level.decrementAndGet();

        data.subnetConfigData = SubnetConfigData.init(config);
        
        config.initsb.ttl("Test name prefix:");
        config.initsb.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(config, ResourceNames.ResourceShortNameKey, ResourceNames.ResourceShortNameDefault);
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