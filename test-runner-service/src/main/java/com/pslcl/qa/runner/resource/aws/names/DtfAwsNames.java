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
package com.pslcl.qa.runner.resource.aws.names;

public class DtfAwsNames
{
    public static final String AwsKeyBase = "com.amazonaws";
    public static final String AwsClientKeyBase = AwsKeyBase + ".client";
    public static final String AwsInstanceKeyBase = AwsKeyBase + ".instance";
    public static final String AwsInstanceTypeKeyBase = AwsInstanceKeyBase + ".type";
    public static final String AwsInstanceTypeLimit = "-limit";
    
    // see com.pslcl.qa.runner.resource.aws.AwsClientConfiguration     
    public static final String AwsClientConfiKey = AwsClientKeyBase + ".client-config"; // only used internally to cache the AWSClientConfiguration
    public static final String EndpointKey = AwsClientKeyBase + ".endpoint";
    public static final String GroupIdKey = AwsKeyBase + "group-id";

    public static final String ConnectionTimeoutKey = AwsClientKeyBase + ".connection-timeout";
    public static final String MaxConnectionsKey = AwsClientKeyBase + ".max-connections";
    public static final String MaxErrorRetryKey = AwsClientKeyBase + ".max-error-retry";
    public static final String RetryPolicyKey = AwsClientKeyBase + ".retry-policy";
    public static final String LocalAddressKey = AwsClientKeyBase + ".local-address";
    public static final String ProtocolKey = AwsClientKeyBase + ".protocol";
    public static final String ProxyDomainKey = AwsClientKeyBase + ".proxy-domain";
    public static final String ProxyHostKey = AwsClientKeyBase + ".proxy-host";
    public static final String ProxyPasswordKey = AwsClientKeyBase + ".proxy-password";
    public static final String ProxyPortKey = AwsClientKeyBase + ".proxy-port";
    public static final String ProxyUserNameKey = AwsClientKeyBase + ".proxy-user-name";
    public static final String ProxyWorkstationKey = AwsClientKeyBase + ".proxy-workstation";
    public static final String ProxyPreemptiveAuthKey = AwsClientKeyBase + ".preemptive-proxy-auth";
    public static final String SocketTimeoutKey = AwsClientKeyBase + ".socket-timeout";
    public static final String UserAgentKey = AwsClientKeyBase + ".user-agent";
    public static final String UseReaperKey = AwsClientKeyBase + ".use-reaper";
    public static final String UseGzipKey = AwsClientKeyBase + ".use-gzip";
    public static final String ReceiveBuffSizeHintKey = AwsClientKeyBase + ".socket-receive-buff-size-hint";
    public static final String SendBuffSizeHintKey = AwsClientKeyBase + ".socket-send-buff-size-hint";
    public static final String SignerOverrideKey = AwsClientKeyBase + ".signer-override";
    public static final String ConnectionTtlKey = AwsClientKeyBase + ".connection-ttl";
    
    public static final String GroupIdDefault = "AwsTestResource";
    public static final String EndpointDefault = "ec2.us-west-2.amazonaws.com";
    public static final String ConnectionTimeoutDefault = "50000";
    public static final String MaxConnectionsDefault = "50";
    public static final String MaxErrorRetryDefault = "-1";
    public static final String RetryPolicyDefault = null;
    public static final String LocalAddressDefault = null;
    public static final String ProtocolDefault = "https";
    public static final String ProxyDomainDefault = null;
    public static final String ProxyHostDefault = null;
    public static final String ProxyPasswordDefault = null;
    public static final String ProxyPortDefault = "-1";
    public static final String ProxyUserNameDefault = null;
    public static final String ProxyWorkstationDefault = null;
    public static final String ProxyPreemptiveAuthDefault = "false";
    public static final String SocketTimeoutDefault = "50000";
    public static final String UserAgentDefault = "aws-sdk-java";
    public static final String UseReaperDefault = "true";
    public static final String UseGzipDefault = "false";
    public static final String ReceiveBuffSizeHintDefault = "0";
    public static final String SendBuffSizeHintDefault = "0";
    public static final String SignerOverrideDefault = null;
    public static final String ConnectionTtlDefault = "-1";
    
    // see com.pslcl.qa.runner.resource.aws.AwsMachineProvider     
    public static final String AwsAmiIdKey = AwsInstanceKeyBase + ".ami-id";
    public static final String AwsInstanceTypeKey = AwsInstanceKeyBase + ".type";
    public static final String AwsInstanceKeyNameKey = AwsInstanceKeyBase + ".key-name";
    public static final String AwsInstanceSecureGroupKey = AwsInstanceKeyBase + ".secure-group";
    
    public static final String AwsAmiIdDefault = "ami-e3106686"; // Amazon Linux AMI 2015.09 (HVM), SSD Volume Type
    public static final String AwsInstanceDefault = InstanceType.T2Medium.value;
    
    
    public enum InstanceType
    {
        // @formatter:off
        T2Micro("t2-micro"), T2Small("t2-small"), T2Medium("t2-medium"), T2Large("t2-large"), 
        M4Large("m4-large"), M4XLarge("m4-xlarge"), M42XLarge("m4-2xlarge"), M44XLarge("m4-4xlarge"), M410XLarge("m4-10xlarge"),
        M3Medium("m3-medium"), M3Large("m3-large"), M3Xlarge("m3-xlarge"), M32XLarge("m3-2xlarge"),
        C4Large("c4-large"), C4XLarge("c4-xlarge"), C42XLarge("c4-2xlarge"), C44XLarge("c4-4xlarge"), C48XLarge("c4-8xlarge"), 
        C3Large("c3-large"), C3XLarge("c3-xlarge"), C32XLarge("c3-2xlarge"), C34XLarge("c3-4xlarge"), C38XLarge("c3-8xlarge"), 
        G22XLarge("g2-2xlarge"), G28XLarge("g2-8xlarge"),
        R3Large("r3-large"), R3XLarge("r3-xlarge"), R32XLarge("r3-2xlarge"), R34XLarge("r3-4xlarge"), R38XLarge("r3-8xlarge"), 
        D2Large("d2-large"), D2XLarge("d2-xlarge"), D22XLarge("d2-2xlarge"), D24XLarge("d2-4xlarge"), D28XLarge("d2-8xlarge"), 
        I2Large("i2-large"), I2XLarge("i2-xlarge"), I22XLarge("i2-2xlarge"), I24XLarge("i2-4xlarge"), I28XLarge("i2-8xlarge"); 
        // @formatter:on
        
        private InstanceType(String value)
        {
            this.value = value;
        }
        public String value;
    }
}