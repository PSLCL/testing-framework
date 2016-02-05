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
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.ec2.model.IpPermission;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.attr.InstanceNames;

@SuppressWarnings("javadoc")
public class SubnetConfigData
{
//    public volatile String availabilityZone;
    public volatile String vpcName;
    public volatile String vpcCidr;
    public volatile String vpcTenancy;
    public volatile int vpcMaxDelay;
    public volatile int vpcMaxRetries;

    public volatile String subnetCidr;
    public volatile String subnetName;
    public volatile int subnetSize;
    public volatile String subnetVpcId;

    public final List<IpPermission> permissions;
    public volatile String sgGroupName;
    public volatile String sgGroupId;
    public volatile int sgMaxDelay;
    public volatile int sgMaxRetries;
    
    public volatile String resoucePrefixName;

    private SubnetConfigData()
    {
        permissions = new ArrayList<IpPermission>();
    }

    public static SubnetConfigData init(ResourceDescription resource, TabToLevel format, SubnetConfigData defaultData) throws Exception
    {
        if(format == null)
            format = new TabToLevel();
        SubnetConfigData data = new SubnetConfigData();
        format.ttl(SubnetConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();
        format.ttl("VPC/Subnet: ");
        format.level.incrementAndGet();
//        data.availabilityZone = getAttribute(InstanceNames.AvailabilityZoneKey, defaultData.availabilityZone, resource, format);
        data.vpcName = getAttribute(InstanceNames.VpcNameKey, defaultData.vpcName, resource, format);
        data.vpcCidr = getAttribute(InstanceNames.VpcCidrKey, defaultData.vpcCidr, resource, format);
        data.vpcTenancy = getAttribute(InstanceNames.VpcTenancyKey, defaultData.vpcTenancy, resource, format);
        data.vpcMaxDelay = Integer.parseInt(getAttribute(InstanceNames.VpcMaxDelayKey, "" + defaultData.vpcMaxDelay, resource, format));
        data.vpcMaxRetries = Integer.parseInt(getAttribute(InstanceNames.VpcMaxRetriesKey, "" + defaultData.vpcMaxRetries, resource, format));
        data.subnetCidr = getAttribute(InstanceNames.SubnetCidrKey, defaultData.subnetCidr, resource, format);
        data.subnetName = getAttribute(InstanceNames.SubnetNameKey, defaultData.subnetName, resource, format);
        data.subnetSize = Integer.parseInt(getAttribute(InstanceNames.SubnetSizeKey, "" + defaultData.subnetSize, resource, format));
        data.subnetVpcId = getAttribute(InstanceNames.SubnetVpcIdKey, defaultData.subnetVpcId, resource, format);
        format.level.decrementAndGet();

        format.ttl("SecurityGroup:");
        format.level.incrementAndGet();
        data.sgGroupName = getAttribute(InstanceNames.SgNameKey, defaultData.sgGroupName, resource, format);
        data.sgGroupId = getAttribute(InstanceNames.SgIdKey, defaultData.sgGroupId, resource, format);
        data.sgMaxDelay = Integer.parseInt(getAttribute(InstanceNames.SgMaxDelayKey, "" + defaultData.sgMaxDelay, resource, format));
        data.sgMaxRetries = Integer.parseInt(getAttribute(InstanceNames.SgMaxRetriesKey, "" + defaultData.sgMaxRetries, resource, format));
        format.level.decrementAndGet();
        
        format.ttl("SecurityGroup Permissions:");
        format.level.incrementAndGet();
        addPermissions(null, resource, format, data.permissions, defaultData);
        format.level.decrementAndGet();
        
        format.ttl("Resource name prefix:");
        format.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(ResourceNames.ResourceShortNameKey, ResourceNames.ResourceShortNameDefault, resource, format);
        return data;
    }

    public static SubnetConfigData init(RunnerConfig config) throws Exception
    {
        SubnetConfigData data = new SubnetConfigData();
        config.initsb.ttl(SubnetManager.class.getSimpleName() + " init:");
        config.initsb.level.incrementAndGet();
        config.initsb.ttl("VPC/Subnet defaults init: ");
        config.initsb.level.incrementAndGet();
        data.vpcName = getAttribute(config, InstanceNames.VpcNameKey, InstanceNames.VpcNameDefault);
        data.vpcCidr = getAttribute(config, InstanceNames.VpcCidrKey, InstanceNames.VpcCidrDefault);
        data.vpcTenancy = getAttribute(config, InstanceNames.VpcTenancyKey, InstanceNames.VpcTenancyDefault);
        data.vpcMaxDelay = Integer.parseInt(getAttribute(config, InstanceNames.VpcMaxDelayKey, InstanceNames.VpcMaxDelayDefault));
        data.vpcMaxRetries = Integer.parseInt(getAttribute(config, InstanceNames.VpcMaxRetriesKey, InstanceNames.VpcMaxRetriesDefault));
        data.subnetCidr = getAttribute(config, InstanceNames.SubnetCidrKey, InstanceNames.SubnetCidrDefault);
        data.subnetName = getAttribute(config, InstanceNames.SubnetNameKey, InstanceNames.SubnetNameDefault);
        data.subnetSize = Integer.parseInt(getAttribute(config, InstanceNames.SubnetSizeKey, InstanceNames.SubnetSizeDefault));
//        data.subnetVpcId = getAttribute(config, InstanceNames.AvailabilityZoneKey, InstanceNames.AvailabilityZoneDefault);
        config.initsb.level.decrementAndGet();

        config.initsb.ttl("SecurityGroup defaults init:");
        config.initsb.level.incrementAndGet();
        data.sgGroupName = getAttribute(config, InstanceNames.SgNameKey, InstanceNames.SgNameDefault);
        data.sgGroupId = getAttribute(config, InstanceNames.SgIdKey, InstanceNames.SgIdDefault);
        data.sgMaxDelay = Integer.parseInt(getAttribute(config, InstanceNames.SgMaxDelayKey, InstanceNames.SgMaxDelayDefault));
        data.sgMaxRetries = Integer.parseInt(getAttribute(config, InstanceNames.SgMaxRetriesKey, InstanceNames.SgMaxRetriesDefault));
        config.initsb.level.decrementAndGet();

        config.initsb.ttl("Permissions:");
        config.initsb.level.incrementAndGet();
        addPermissions(config, null, null, data.permissions, null);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl("Resource name prefix:");
        config.initsb.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(config, ResourceNames.ResourceShortNameKey, ResourceNames.ResourceShortNameDefault);
        return data;
    }

    private static String getAttribute(RunnerConfig config, String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
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

    private static void addPermissions(
                    RunnerConfig config, ResourceDescription resource, TabToLevel format, 
                    List<IpPermission> permissions, SubnetConfigData defaultData) throws Exception
    {
        String[] protocols = null;
        String[] ipRanges = null;
        String[] ports = null;

        Map<String, String> attrs;
        if (config != null)
        {
            format = config.initsb;
            attrs = new HashMap<String, String>();
            for (Entry<Object, Object> entry : config.properties.entrySet())
            {
                if(entry.getValue() instanceof String)
                    attrs.put((String) entry.getKey(), (String) entry.getValue());
            }
        } else
            attrs = resource.getAttributes();

        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermProtocolKey, attrs);
        int size = list.size();
        if (size == 0)
        {
            if (config != null) // setting up defaultData
            {
                //@formatter:off
                IpPermission perm = new IpPermission()
                    .withIpProtocol(InstanceNames.PermProtocolDefault)
                    .withIpRanges(InstanceNames.PermIpRangeDefault)
                    .withFromPort(Integer.parseInt(InstanceNames.PermPortDefault))
                    .withToPort(Integer.parseInt(InstanceNames.PermPortDefault));
                //@formatter:on
                permissions.add(perm);
                format.ttl(InstanceNames.PermProtocolKey, "0", " = ", perm.getIpProtocol());
                format.ttl(InstanceNames.PermIpRangeKey, "0", " = ", perm.getIpRanges());
                format.ttl(InstanceNames.PermPortKey, "0", " = ", perm.getToPort());
            } else
            { // actual call
                int count = 0;
                for (IpPermission value : defaultData.permissions)
                {
                    permissions.add(value);
                    format.ttl(InstanceNames.PermProtocolKey + count, " = ", value.getIpProtocol(), " (default injected)");
                    format.ttl(InstanceNames.PermIpRangeKey + count, " = ", value.getIpRanges(), " (default injected)");
                    format.ttl(InstanceNames.PermPortKey + count, " = ", value.getToPort(), " (default injected)");
                    ++count;
                }
            }
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

        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermIpRangeKey, attrs);
        if (list.size() != size)
            throw new Exception("Permissions, number of IpRanges does not match number of Protocols, exp: " + size + " rec: " + list.size());
        for (int i = 0; i < size; i++)
        {
            Entry<String, String> entry = list.get(i);
            ipRanges[i] = entry.getValue();
        }

        list = PropertiesFile.getPropertiesForBaseKey(InstanceNames.PermPortKey, attrs);
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
}