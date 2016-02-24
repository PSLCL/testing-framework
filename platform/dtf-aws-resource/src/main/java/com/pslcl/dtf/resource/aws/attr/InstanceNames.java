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
package com.pslcl.dtf.resource.aws.attr;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("javadoc")
public class InstanceNames
{
    public static final String InstanceKeyBase = ProviderNames.AwsKeyBase + ".ec2instance";
    
//    public static final String Ec2WindowsKey = InstanceKeyBase + ".windows";
    public static final String Ec2LinuxUserDataKey = InstanceKeyBase + ".linux-user-data";
    public static final String Ec2WinUserDataKey = InstanceKeyBase + ".win-user-data";
    
    public static final String Ec2LinuxUserDataDefault = "#!/bin/bash\n/usr/local/staf/startSTAFProc.sh";
    public static final String Ec2WinUserDataDefault = "<script>\\STAF\\startSTAFProc.bat</script>";
//    public static final String Ec2WindowsDefault = "false";
    
    /* ****************************************************************************
     * AWS Machine Provider bind declarations
     * see com.pslcl.dtf.resource.aws.instance.AwsMachineInstance     
     * see com.pslcl.dtf.resource.aws.instance.MachineInstanceFuture     
     * 
     * EC2 Instance declarations    
    ******************************************************************************/    
    
    /* ****************************************************************************
     * VPC declarations
    ******************************************************************************/    
    public static final String VpcKeyBase = InstanceKeyBase + ".vpc";
    
    public static final String VpcNameKey = VpcKeyBase + ".name";
    public static final String VpcCidrKey = VpcKeyBase + ".cidr";
    public static final String VpcTenancyKey = VpcKeyBase + ".tenancy";
    public static final String VpcMaxDelayKey = VpcKeyBase + ".max-delay";       
    public static final String VpcMaxRetriesKey = VpcKeyBase + ".max-retries";  

    public static final String VpcCidrDefault = "10.0.0.0/16";
    public static final String VpcNameAwsDefault = "aws-vpc-default";   
    public static final String VpcNameDefault = null;   // installation/site default
    public static final String VpcTenancyDefault = "default";
    public static final String VpcMaxDelayDefault = ProviderNames.ShortMaxDelayDefault; 
    public static final String VpcMaxRetriesDefault = ProviderNames.ShortMaxRetriesDefault;

    /* ****************************************************************************
     * Subnet declarations
    ******************************************************************************/    
    public static final String SubnetKeyBase = InstanceKeyBase + ".subnet";
    public static final String SubnetSizeKey = SubnetKeyBase + ".size";
    public static final String SubnetNameKey = SubnetKeyBase + ".name";
    public static final String SubnetCidrKey = SubnetKeyBase + ".cidr";
    public static final String SubnetVpcIdKey = SubnetKeyBase + ".vpc-name";

    public static final String SubnetCidrDefault = "10.0.0.0/28";
    public static final String SubnetNameDefault = ProviderNames.ResourcePrefixNameDefault + "-" + SubnetCidrDefault;
    public static final String SubnetSizeDefault = "16";  // nibble /28 cidr
    public static final String SubnetVpcNameDefault = VpcNameDefault;
    
    /* ****************************************************************************
     * Security Group declarations
    ******************************************************************************/    
    public static final String SgKeyBase = InstanceKeyBase + ".sg";
    
    public static final String SgNameKey = SgKeyBase + ".group-name";
    public static final String SgIdKey = SgKeyBase + ".group-id";
    public static final String SgMaxDelayKey = SgKeyBase + ".max-delay";       
    public static final String SgMaxRetriesKey = SgKeyBase + ".max-retries";   
    
    public static final String SgNameDefault = ProviderNames.ResourcePrefixNameDefault;
    public static final String SgIdDefault = null;
    public static final String SgMaxDelayDefault = ProviderNames.ShortMaxDelayDefault;      
    public static final String SgMaxRetriesDefault = ProviderNames.ShortMaxRetriesDefault;  // about 1 min timeout
    
    /* ****************************************************************************
     * VPC Permissions
    ******************************************************************************/    
    public static final String PermKeyBase = InstanceKeyBase + ".perm";
    
    // note that the following are base keys and can be numbered from 0 on up, to add as many permissions as desired
    // these are optional, but if one is given, they are must be given with matching numbers.
    public static final String PermProtocolKey = PermKeyBase + ".protocol";
    public static final String PermIpRangeKey = PermKeyBase + ".ip-range";
    public static final String PermPortKey = PermKeyBase + ".port";

    public static final String PermProtocolDefault = "tcp";
    public static final String PermIpRangeDefault = "0.0.0.0/0";
    public static final String PermPortDefault = "22";
    
    /* ****************************************************************************
     * ec2 instance declarations
    ******************************************************************************/    
    public static final String Ec2MaxDelayKey = InstanceKeyBase + ".max-delay";
    public static final String Ec2MaxRetriesKey = InstanceKeyBase + ".max-retries";
    public static final String Ec2IamArnKey = InstanceKeyBase + ".iam-arn";
    public static final String Ec2IamNameKey = InstanceKeyBase + ".iam-name";
    public static final String Ec2KeyPairNameKey = InstanceKeyBase + ".keypair-name";
    public static final String Ec2StallReleaseKey = InstanceKeyBase + ".stall-release";     // in minutes
    public static final String Ec2StallReleaseMarginKey = InstanceKeyBase + ".stall-release-margin"; // in minutes
    
    public static final String Ec2MaxDelayDefault = ProviderNames.LongMaxDelayDefault;
    public static final String Ec2MaxRetriesDefault = ProviderNames.LongMaxRetriesDefault;
    public static final String Ec2StallReleaseDefault = "50"; // in minutes
    
    public static List<String> getInstanceKeys()
    {
       List<String> keys = new ArrayList<String>();
       keys.add(Ec2LinuxUserDataKey);
       keys.add(Ec2WinUserDataKey);       
       keys.add(Ec2MaxDelayKey);
       keys.add(Ec2MaxRetriesKey);
       keys.add(Ec2IamArnKey);
       keys.add(Ec2IamNameKey);
       keys.add(Ec2KeyPairNameKey);
       keys.add(Ec2StallReleaseKey);
       keys.add(Ec2StallReleaseMarginKey);
       return keys;
    }
}