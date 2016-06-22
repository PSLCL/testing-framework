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

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;

@SuppressWarnings("javadoc")
public class SubnetConfigData
{
//    public volatile String availabilityZone;
    public volatile String vpcId;
    public volatile int vpcMaxDelay;
    public volatile int vpcMaxRetries;

    public volatile String subnetCidr;
    public volatile String subnetName;
    public volatile int subnetSize;
    public volatile String subnetVpcId;

    public volatile String sgDefaultVpcOverrideId;
    public volatile int sgMaxDelay;
    public volatile int sgMaxRetries;
    
    public volatile String resoucePrefixName;

    private SubnetConfigData()
    {
    }

    public static SubnetConfigData init(ResourceDescription resource, TabToLevel format, SubnetConfigData defaultData) throws Exception
    {
        if(format == null)
            format = new TabToLevel();
        SubnetConfigData data = new SubnetConfigData();
        format.ttl(SubnetConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();
        
        format.ttl("Resource name prefix:");
        format.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(ProviderNames.ResourcePrefixNameKey, defaultData.resoucePrefixName, resource, format);
        format.level.decrementAndGet();
        
        format.ttl("VPC/Subnet: ");
        format.level.incrementAndGet();
//        data.availabilityZone = getAttribute(InstanceNames.AvailabilityZoneKey, defaultData.availabilityZone, resource, format);
        data.vpcId = getAttribute(InstanceNames.VpcIdKey, defaultData.vpcId, resource, format);
        data.vpcMaxDelay = Integer.parseInt(getAttribute(InstanceNames.VpcMaxDelayKey, "" + defaultData.vpcMaxDelay, resource, format));
        data.vpcMaxRetries = Integer.parseInt(getAttribute(InstanceNames.VpcMaxRetriesKey, "" + defaultData.vpcMaxRetries, resource, format));
        data.subnetCidr = getAttribute(InstanceNames.SubnetCidrKey, defaultData.subnetCidr, resource, format);
        data.subnetName = getAttribute(InstanceNames.SubnetNameKey, defaultData.subnetName, resource, format);
        data.subnetSize = Integer.parseInt(getAttribute(InstanceNames.SubnetSizeKey, "" + defaultData.subnetSize, resource, format));
        data.subnetVpcId = getAttribute(InstanceNames.SubnetVpcIdKey, defaultData.subnetVpcId, resource, format);
        format.level.decrementAndGet();

        format.ttl("SecurityGroup:");
        format.level.incrementAndGet();
        data.sgDefaultVpcOverrideId = getAttribute(InstanceNames.SgGroupIdKey, defaultData.sgDefaultVpcOverrideId, resource, format);
        data.sgMaxDelay = Integer.parseInt(getAttribute(InstanceNames.SgMaxDelayKey, "" + defaultData.sgMaxDelay, resource, format));
        data.sgMaxRetries = Integer.parseInt(getAttribute(InstanceNames.SgMaxRetriesKey, "" + defaultData.sgMaxRetries, resource, format));
        format.level.decrementAndGet();
        
        format.ttl("SecurityGroup Permissions:");
        format.level.incrementAndGet();
        format.level.decrementAndGet();
        return data;
    }

    public static SubnetConfigData init(RunnerConfig config) throws Exception
    {
        SubnetConfigData data = new SubnetConfigData();
        config.initsb.ttl(SubnetManager.class.getSimpleName() + " init:");
        config.initsb.level.incrementAndGet();
        config.initsb.ttl("VPC/Subnet defaults init: ");
        config.initsb.level.incrementAndGet();
        data.vpcId = getAttribute(config, InstanceNames.VpcIdKey, InstanceNames.VpcNameDefault);
        data.vpcMaxDelay = Integer.parseInt(getAttribute(config, InstanceNames.VpcMaxDelayKey, InstanceNames.VpcMaxDelayDefault));
        data.vpcMaxRetries = Integer.parseInt(getAttribute(config, InstanceNames.VpcMaxRetriesKey, InstanceNames.VpcMaxRetriesDefault));
        data.subnetCidr = getAttribute(config, InstanceNames.SubnetCidrKey, InstanceNames.SubnetCidrDefault);
        data.subnetName = getAttribute(config, InstanceNames.SubnetNameKey, InstanceNames.SubnetNameDefault);
        data.subnetSize = Integer.parseInt(getAttribute(config, InstanceNames.SubnetSizeKey, InstanceNames.SubnetSizeDefault));
//        data.subnetVpcId = getAttribute(config, InstanceNames.AvailabilityZoneKey, InstanceNames.AvailabilityZoneDefault);
        config.initsb.level.decrementAndGet();

        config.initsb.ttl("SecurityGroup defaults init:");
        config.initsb.level.incrementAndGet();
        data.sgDefaultVpcOverrideId = getAttribute(config, InstanceNames.SgGroupIdKey, InstanceNames.SgIdDefault);
        data.sgMaxDelay = Integer.parseInt(getAttribute(config, InstanceNames.SgMaxDelayKey, InstanceNames.SgMaxDelayDefault));
        data.sgMaxRetries = Integer.parseInt(getAttribute(config, InstanceNames.SgMaxRetriesKey, InstanceNames.SgMaxRetriesDefault));
        config.initsb.level.decrementAndGet();

        config.initsb.ttl("Permissions:");
        config.initsb.level.incrementAndGet();
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl("Resource name prefix:");
        config.initsb.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(config, ProviderNames.ResourcePrefixNameKey, ProviderNames.ResourcePrefixNameDefault);
        return data;
    }

    private static String getAttribute(RunnerConfig config, String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        value = StrH.trim(value);
        config.initsb.ttl(key, " = ", value);
        return value;
    }
    
    private static String getAttribute(String key, String defaultValue, ResourceDescription resource, TabToLevel format)
    {
        String value = resource.getAttributes().get(key);
        if (value == null)
        {
            value = defaultValue;
            resource.addAttribute(key, value);
            format.ttl(key, " = ", value, " (default injected)");
        } else
            format.ttl(key, " = ", value);
        return value;
    }
}