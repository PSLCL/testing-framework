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
    /* ****************************************************************************
     * Global declarations    
    ******************************************************************************/
    public static final String InstanceKeyBase = ClientNames.AwsKeyBase + ".ec2instance";
    public static final String AvailabilityZoneKey = InstanceKeyBase + ".avail-zone";
    public static final String AvailabilityZoneDefault = "us-west-2a";
    
    public static final String Ec2UserDataKey = InstanceKeyBase + ".user-data";
//    public static final String Ec2UserDataDefault = "#!/bin/bash\n/usr/local/staf/startSTAFProc.sh $(curl http://169.254.169.254/latest/meta-data/instance-id)";
    public static final String Ec2UserDataDefault = "#!/bin/bash\n/usr/local/staf/startSTAFProc.sh";
    
    /* ****************************************************************************
     * AWS Machine Provider bind declarations
     * see com.pslcl.dtf.resource.aws.instance.AwsMachineInstance     
     * see com.pslcl.dtf.resource.aws.instance.MachineInstanceFuture     
     * 
     * EC2 Instance declarations    
    ******************************************************************************/    
    
    /*
        Note on max-delay and max-retry counts
        
                                   delay        total       if delay then max count = timeout
        count: 0 delay: 100                     100ms
        count: 1 delay: 200                     300ms
        count: 2 delay: 400                     700ms
        count: 3 delay: 800         0.8 sec     1.5sec      20 = 15+ sec    39 = 30s    76 = 1min   150 = 2min
        count: 4 delay: 1600        1.6 sec     3.1sec      21 = 30+ sec    40 = 1min   77 = 2min   150 = 4min
        count: 5 delay: 3200        3.2 sec     6.3sec      22 = 1+ min
        count: 6 delay: 6400        6.4 sec     12.7sec     23 = 2+ min     42 = 2min   79 = 8min   152 = 16 min
        count: 7 delay: 12800       12.8 sec    25.5sec     24 = 4+ min
        count: 8 delay: 25600       25.6 sec    0.85min     25 = 8+ min     43 = 16min  81 = 32min  153 = 64min
        count: 9 delay: 51200       51.2 sec    1.7min      26 = 16+ min    45 = 32min  83 = 64min  155 = 2.1hrs
        count: 10 delay: 102400     1.7 min     3.4min      27 = 32+ min
        count: 11 delay: 204800     3.4 min     6.8min      28 = 64+ min
        count: 12 delay: 409600     6.8 min     13.6min     29 = 2.1 hrs
        count: 13 delay: 819200     13.6 min    27.3min     30 = 4.2 hrs    49 = 8.4hrs 86 = 16.8h  160 = 33.6hrs
                                                                            19,56,130
*/
    public static final String shortMaxDelay = "800";  // 0.8 seconds
    public static final String shortMaxRetries = "76"; // roughly 1 minute
    public static final String longMaxDelay = "6400";  // roughly 6.4 seconds
    public static final String longMaxRetries = "152"; // rouchly 16 minutes
    
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
    public static final String VpcMaxDelayDefault = shortMaxDelay; 
    public static final String VpcMaxRetriesDefault = shortMaxRetries;

    /* ****************************************************************************
     * Subnet declarations
    ******************************************************************************/    
    public static final String SubnetKeyBase = InstanceKeyBase + ".subnet";
    public static final String SubnetSizeKey = SubnetKeyBase + ".size";
    public static final String SubnetNameKey = SubnetKeyBase + ".name";
    public static final String SubnetCidrKey = SubnetKeyBase + ".cidr";
    public static final String SubnetVpcIdKey = SubnetKeyBase + ".vpc-name";

    public static final String SubnetCidrDefault = "10.0.0.0/28";
    public static final String SubnetNameDefault = ClientNames.TestShortNameDefault + "-" + SubnetCidrDefault;
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
    
    public static final String SgNameDefault = ClientNames.TestShortNameDefault;
    public static final String SgIdDefault = null;
    public static final String SgMaxDelayDefault = shortMaxDelay;      
    public static final String SgMaxRetriesDefault = shortMaxRetries;  // about 1 min timeout
    
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
    public static final String Ec2IamNameKey = InstanceKeyBase + ".max-retries";
    public static final String Ec2KeyPairNameKey = InstanceKeyBase + ".keypair-name";
    
    public static final String Ec2MaxDelayDefault = longMaxDelay;
    public static final String Ec2MaxRetriesDefault = longMaxRetries;
    
    public static List<String> getKeys()
    {
       List<String> keys = new ArrayList<String>();
       keys.add(VpcCidrKey);
       keys.add(VpcTenancyKey);
       keys.add(VpcMaxDelayKey);       
       keys.add(VpcMaxRetriesKey);  
       keys.add(SgNameKey);
       keys.add(SgIdKey);
       keys.add(SgMaxDelayKey);       
       keys.add(SgMaxRetriesKey);   
       keys.add(PermProtocolKey);
       keys.add(PermIpRangeKey);
       keys.add(PermPortKey);
       keys.add(Ec2MaxDelayKey);
       keys.add(Ec2MaxRetriesKey);
       keys.add(Ec2IamArnKey);
       keys.add(Ec2IamNameKey);
       keys.add(Ec2KeyPairNameKey);
       keys.add(Ec2UserDataKey);
       keys.add(ClientNames.TestShortNameKey);
       return keys;
    }
}