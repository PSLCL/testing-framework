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

import org.slf4j.Logger;

import com.amazonaws.services.ec2.model.ImageAttributeName;
import com.amazonaws.services.ec2.model.InstanceType;

@SuppressWarnings("javadoc")
public class ProviderNames
{
    // see dtf-core ResourceNames.PslclKeyBase, ResourceKeyBase and ResourceShortNameDefault.
    // Did not want to depend on dtf-core
    public static final String PslclKeyBase = "pslcl.dtf";  
    public static final String AwsKeyBase = PslclKeyBase + ".aws";
    public static final String ResourceKeyBase = AwsKeyBase + ".resource";
    
    public static final String ResourcePrefixNameKey = ResourceKeyBase + ".prefix-name";
    public static final String ResourcePrefixNameDefault = "dtf";
    
    public static final String ShortMaxDelayDefault = "5000"; // 5 seconds
    public static final String ShortMaxRetriesDefault = "17"; // roughly 1 minute
    public static final String LongMaxDelayDefault = "15000"; // 15 seconds
    public static final String LongMaxRetriesDefault = "67";  // roughly 15 minutes
    
    /* ****************************************************************************
     * AWS Machine Provider declarations
     * see com.pslcl.dtf.resource.aws.provider.AwsMachineProvider     
     * 
     * Instance type declarations    
    ******************************************************************************/    
    public static final String InstanceKeyBase = AwsKeyBase + ".instance";
    
    public static final String InstanceTypeKey = InstanceKeyBase + ".type";
    public static final String InstanceGlobalMapKey = InstanceKeyBase + ".map";

    public static final String InstanceTypeDefault = InstanceType.M3Medium.toString();
    public static final String InstanceGlobalMapDefault = null; // always pick InstanceTypeDefault
    
    // use the toString() method of the aws's InstanceType for the desired Attribute String.
    // for example attributes.put(AwsNames.AwsInstanceTypeKey, InstanceType.C1Medium.toString());
    // the following array is for convenience in referencing the needed AWS instance type enum from this class.   
    public static final InstanceType[] instanceTypes = InstanceType.values();

    /** 
     * Log current AWS API InstanceType values.
     * <p>As new AWS API dependency versions are moved to, the RunnerService's properties file keys may need to change.
     * Run this method to log the current state of instance types.  
     * @param log SLF4j Logger to use.
     */
    public static void logInstanceTypes(Logger log)
    {
        StringBuilder sb = new StringBuilder("\nInstance Types:\n");
        for(int i=0; i < instanceTypes.length; i++)
            sb.append("\t"+instanceTypes[i].name() + "(" + instanceTypes[i].toString() + ")\n");
        sb.append("\n");
        log.info(sb.toString());
    }
    
    /* ****************************************************************************
     * Image declarations    
    ******************************************************************************/  
    
    // These are not all the possible filters but the ones thought of possible interest to generators and building defaults
    public static final String ImageKeyBase = AwsKeyBase + ".image";
    
    public static final String ImageArchitectureKey = ImageKeyBase + ".architecture";
    public static final String ImageHypervisorKey = ImageKeyBase + ".hypervisor";
    public static final String ImageImageTypeKey = ImageKeyBase + ".image-type";
    public static final String ImageIsPublicKey = ImageKeyBase + ".public";
    public static final String ImageNameKey = ImageKeyBase + ".name";
    public static final String ImageOwnerKey = ImageKeyBase + ".owner";
    public static final String ImagePlatformKey = ImageKeyBase + ".platform";
    public static final String ImageRootDevTypeKey = ImageKeyBase + ".root-dev-type";
    public static final String ImageStateKey = ImageKeyBase + ".state";

    public static final String ImageArchitecture32bit = "i386";
    public static final String ImageArchitecture64bit = "x86_64";
    public static final String ImageArchitectureDefault = ImageArchitecture64bit;
    public static final String ImageArchitectureFilter = "architecture";
    
    public static final String ImageHypervisorOvm = "ovm";
    public static final String ImageHypervisorXen = "xen";
    public static final String ImageHypervisorDefault = ImageHypervisorXen;
    public static final String ImageHypervisorFilter = "hypervisor";
    
    public static final String ImageImageTypeMachine = "machine";
    public static final String ImageImageTypeKernel = "kernel";
    public static final String ImageImageTypeRamdisk = "ramdisk";
    public static final String ImageImageTypeDefault = ImageImageTypeMachine;
    public static final String ImageImageTypeFilter = "image-type";
    
    public static final String ImageRootDevTypeEbs = "ebs";
    public static final String ImageRootDevTypeInstaceStore = "instance-store";
    public static final String ImageRootDevTypeDefault = ImageRootDevTypeEbs;
    public static final String ImageRootDevTypeFilter = "root-device-type";
    
    public static final String ImageStateAvailable = "available";
    public static final String ImageStatePending = "pending";
    public static final String ImageStateFailed = "failed";
    public static final String ImageStateDefault = ImageStateAvailable;
    public static final String ImageStateFilter = "state";

    public static final String ImagePlatformNonWindows = null;
    public static final String ImagePlatformWindows = "windows"; 
    public static final String ImagePlatformDefault = ImagePlatformNonWindows;
    public static final String ImagePlatformFilter = "platform"; 

    public static final String ImageIsPublicDefault = "true";
    public static final String ImageIsPublicFilter = "is-public";
    public static final String ImageImageIdDefault = null;
    public static final String ImageImageIdFilter = "image-id";
    public static final String ImageNameDefault = null;
    public static final String ImageNameFilter = "name";
    public static final String ImageOwnerDefault = "amazon";
    public static final String ImageOwnerFilter = "owner-alias";

    // use the toString() method of the aws's ImageAttributeName for the desired Attribute String.
    // for example attributes.put(AwsNames.AwsImageDescriptionKey, ImageAttributeName.Description.toString());
    // the following array is for convenience in referencing the needed AWS image attribute name enum from this class.   
    public static final ImageAttributeName[] imageAttributeNames = ImageAttributeName.values();

    /** 
     * Log current AWS API Instance Attribute values.
     * <p>As new AWS API dependency versions are moved to, the RunnerService's properties file keys may need to change.
     * Run this method to log the current state of instance attributes.  
     * @param log SLF4j Logger to use.
     */
    public static void logImageAttributeNames(Logger log)
    {
        StringBuilder sb = new StringBuilder("\nImage attribute names:\n");
        for(int i=0; i < imageAttributeNames.length; i++)
            sb.append("\t"+imageAttributeNames[i].name() + "(" + imageAttributeNames[i].toString() + ")\n");
        sb.append("\n");
        log.info(sb.toString());
    }
    
    /* ****************************************************************************
     * Block Device - sub image declarations    
    ******************************************************************************/
    
    public static final String BlockingDeviceKeyBase = ImageKeyBase + ".block-device";
    
    public static final String BlockingDeviceVolumeSizeKey = BlockingDeviceKeyBase + ".volume-size";
    public static final String BlockingDeviceVolumeTypeKey = BlockingDeviceKeyBase + ".volume-type";
    public static final String BlockingDeviceDeleteOnTerminationKey = BlockingDeviceKeyBase + ".delete-on-termination";
    
    public static final String BlockingDeviceTypeGp2 = "gp2";
    public static final String BlockingDeviceTypeIo1 = "io1";
    public static final String BlockingDeviceTypeStandard = "standard";
    public static final String BlockingDeviceVolumeTypeDefault = BlockingDeviceTypeStandard;
    public static final String BlockingDeviceVolumeTypeFilter = "block-device-mapping.volume-type";
    
    // size in gig, 1 min, 1024 is max, the following were see on amazon only images
    
    public static final String BlockingDeviceVolumeSize2 = "2";     // 27
    public static final String BlockingDeviceVolumeSize8 = "8";     // 340
    public static final String BlockingDeviceVolumeSize10 = "10";   // 4
    public static final String BlockingDeviceVolumeSize30 = "30";   // 19
    public static final String BlockingDeviceVolumeSize35 = "35";  // none below here showed when standard is selected
    public static final String BlockingDeviceVolumeSize40 = "40";
    public static final String BlockingDeviceVolumeSize45 = "45";
    public static final String BlockingDeviceVolumeSize50 = "50";
    public static final String BlockingDeviceVolumeSize55 = "55";
    public static final String BlockingDeviceVolumeSize60 = "60";
    public static final String BlockingDeviceVolumeSizeDefault = BlockingDeviceVolumeSize8;
    
    public static final String BlockingDeviceVolumeSizeFilter = "block-device-mapping.volume-size"; 
    public static final String BlockingDeviceDeleteOnTerminationDefault = "false";
    public static final String BlockingDeviceDeleteOnTerminationFilter = "block-device-mapping.delete-on-termination";
    
    /* ****************************************************************************
     * Image.getLocation() parsing declarations    
    ******************************************************************************/
    public static final String LocationKeyBase = ImageKeyBase + ".location";
    
    public static final String LocationYearKey = LocationKeyBase + ".year";
    public static final String LocationMonthKey = LocationKeyBase + ".month";
    public static final String LocationDotKey = LocationKeyBase + ".dot";
    // note that feature is a base key and can be numbered from 0 on up, to add as many filters as desired
    public static final String LocationFeatureKey = LocationKeyBase + ".feature";
    
    public static final String Location64bit = "64bit";
    public static final String LocationBase = "base";
    public static final String LocationDocker = "docker";
    public static final String LocationDocker120 = "docker120";
    public static final String LocationEbs = "ebs";
    public static final String LocationEcs = "ecs";
    public static final String LocationGolang = "golang";
    public static final String LocationHvm = "hvm";         // Virtualization type: Hardware Virtual Machine
    public static final String LocationJava7 = "java7";
    public static final String LocationJava8 = "java8";
    public static final String LocationPhp54 = "php54";
    public static final String LocationPhp55 = "php55";
    public static final String LocationPhp56 = "php56";
    public static final String LocationPv = "pv";           // Virtualization type: Paravirtual
    public static final String LocationPython26 = "python26";
    public static final String LocationPython27 = "python27";
    public static final String LocationPython34 = "python34";
    public static final String LocationNodejs = "nodejs";
    public static final String LocationRuby = "ruby";
    public static final String LocationTomcat7Java6 = "tomcat7java6";
    public static final String LocationTomcat7Java7 = "tomcat7java7";
    public static final String LocationTomcat8Java8 = "tomcat8java8";
    
    public static final String LocationFeatureDefault0 = LocationBase;
    public static final String LocationFeatureDefault1 = LocationHvm;
    public static final String LocationYearDefault = null;
    public static final String LocationMonthDefault = null;
    public static final String LocationDotDefault = null;
    
    
    /* ****************************************************************************
     * SES Inspect    
    ******************************************************************************/    
    public static final String SesKeyBase = AwsKeyBase + ".ses";
    
    public static final String SesMaxDelayKey = SesKeyBase + ".max-delay";
    public static final String SesMaxRetriesKey = SesKeyBase + ".max-retries";

    /* ****************************************************************************
     * Various helper key lists    
    ******************************************************************************/
    public static List<String> getMachineKeys()
    {
       List<String> keys = new ArrayList<String>();
       keys.add(ResourcePrefixNameKey);
       keys.add(InstanceTypeKey);
       keys.add(ImageArchitectureKey);
       keys.add(ImageHypervisorKey);
       keys.add(ImageImageTypeKey);
       keys.add(ImageIsPublicKey);
       keys.add(ImageNameKey);
       keys.add(ImageOwnerKey);
       keys.add(ImagePlatformKey);
       keys.add(ImageRootDevTypeKey);
       keys.add(ImageStateKey);
       keys.add(BlockingDeviceVolumeSizeKey);
       keys.add(BlockingDeviceVolumeTypeKey);
       keys.add(BlockingDeviceDeleteOnTerminationKey);
       keys.add(LocationYearKey);
       keys.add(LocationMonthKey);
       keys.add(LocationDotKey);
       keys.add(LocationFeatureKey);
       return keys;
    }
    
//    public static List<String> getAllMachineProviderKeys()
//    {
//        List<String> keys = InstanceNames.getInstanceKeys();
//        keys.addAll(getMachineKeys());
//        return keys;
//    }
    
    public static List<String> getNetworkKeys()
    {
        List<String> keys = new ArrayList<String>();
        keys.add(InstanceNames.VpcIdKey);  
        keys.add(InstanceNames.VpcCidrKey);
        keys.add(InstanceNames.VpcTenancyKey);
        keys.add(InstanceNames.VpcMaxDelayKey);       
        keys.add(InstanceNames.VpcMaxRetriesKey);   
        keys.add(InstanceNames.SubnetSizeKey);
        keys.add(InstanceNames.SubnetNameKey);
        keys.add(InstanceNames.SubnetCidrKey);
        keys.add(InstanceNames.SubnetVpcIdKey);
        keys.add(InstanceNames.SgGroupIdKey);
        keys.add(InstanceNames.SgMaxDelayKey);
        keys.add(InstanceNames.SgMaxRetriesKey);
        keys.add(InstanceNames.PermProtocolKey);
        keys.add(InstanceNames.PermIpRangeKey);
        keys.add(InstanceNames.PermPortKey);
        return keys;
    }
    public static List<String> getSesKeys()
    {
        List<String> keys = new ArrayList<String>();
        keys.add(SesMaxDelayKey);  
        keys.add(SesMaxRetriesKey);  
        return keys;
    }
}