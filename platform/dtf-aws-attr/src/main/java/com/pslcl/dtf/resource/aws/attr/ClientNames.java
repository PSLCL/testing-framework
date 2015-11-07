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


@SuppressWarnings("javadoc")
public class ClientNames
{
    /* ****************************************************************************
     * Global declarations    
    ******************************************************************************/    
    public static final String AwsKeyBase = "amazonaws";
    public static final String GroupIdKey = AwsKeyBase + ".group-id";
    public static final String PslclKeyBase = "pslcl.dtf";
    public static final String TestKeyBase = PslclKeyBase + ".test";
    
    public static final String TestShortNameKey = PslclKeyBase + ".short-name";
    
    public static final String TestShortNameDefault = "dtf";
    
    /* ****************************************************************************
     * AWS Client Configuration declarations    
     * see com.pslcl.dtf.resource.aws.AwsClientConfiguration     
    ******************************************************************************/    
    public static final String ClientKeyBase = AwsKeyBase + ".client";
    
    public static final String ConfiKey = ClientKeyBase + ".client-config"; // only used internally to cache the AWSClientConfiguration
    public static final String EndpointKey = ClientKeyBase + ".endpoint";

    public static final String ConnectionTimeoutKey = ClientKeyBase + ".connection-timeout";
    public static final String MaxConnectionsKey = ClientKeyBase + ".max-connections";
    public static final String MaxErrorRetryKey = ClientKeyBase + ".max-error-retry";
    public static final String RetryPolicyKey = ClientKeyBase + ".retry-policy";
    public static final String LocalAddressKey = ClientKeyBase + ".local-address";
    public static final String ProtocolKey = ClientKeyBase + ".protocol";
    public static final String ProxyDomainKey = ClientKeyBase + ".proxy-domain";
    public static final String ProxyHostKey = ClientKeyBase + ".proxy-host";
    public static final String ProxyPasswordKey = ClientKeyBase + ".proxy-password";
    public static final String ProxyPortKey = ClientKeyBase + ".proxy-port";
    public static final String ProxyUserNameKey = ClientKeyBase + ".proxy-user-name";
    public static final String ProxyWorkstationKey = ClientKeyBase + ".proxy-workstation";
    public static final String ProxyPreemptiveAuthKey = ClientKeyBase + ".preemptive-proxy-auth";
    public static final String SocketTimeoutKey = ClientKeyBase + ".socket-timeout";
    public static final String UserAgentKey = ClientKeyBase + ".user-agent";
    public static final String UseReaperKey = ClientKeyBase + ".use-reaper";
    public static final String UseGzipKey = ClientKeyBase + ".use-gzip";
    public static final String ReceiveBuffSizeHintKey = ClientKeyBase + ".socket-receive-buff-size-hint";
    public static final String SendBuffSizeHintKey = ClientKeyBase + ".socket-send-buff-size-hint";
    public static final String SignerOverrideKey = ClientKeyBase + ".signer-override";
    public static final String ConnectionTtlKey = ClientKeyBase + ".connection-ttl";
    
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
}