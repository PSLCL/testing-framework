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
package com.pslcl.qa.runner.resource.aws;

import java.net.InetAddress;

import javax.jms.JMSException;

import org.apache.commons.daemon.DaemonInitException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.retry.RetryPolicy;
import com.pslcl.qa.runner.config.RunnerServiceConfig;

/**
 * Access AWS SQS via JMS
 * 
 */
public class AwsClientConfiguration
{
    public static final String AwsClientConfiKey = "com.pslcl.qa.aws.client-config";
    public static final String EndpointKey = "pslcl.qa.runner.resource.aws.endpoint";
    public static final String GroupIdKey = "pslcl.qa.platform.resource.aws.group-id";

    public static final String ConnectionTimeoutKey = "com.amazonaws.client.connection-timeout";
    public static final String MaxConnectionsKey = "com.amazonaws.client.max-connections";
    public static final String MaxErrorRetryKey = "com.amazonaws.client.max-error-retry";
    public static final String RetryPolicyKey = "com.amazonaws.client.retry-policy";
    public static final String LocalAddressKey = "com.amazonaws.client.local-address";
    public static final String ProtocolKey = "com.amazonaws.client.protocol";
    public static final String ProxyDomainKey = "com.amazonaws.client.proxy-domain";
    public static final String ProxyHostKey = "com.amazonaws.client.proxy-host";
    public static final String ProxyPasswordKey = "com.amazonaws.client.proxy-password";
    public static final String ProxyPortKey = "com.amazonaws.client.proxy-port";
    public static final String ProxyUserNameKey = "com.amazonaws.client.proxy-user-name";
    public static final String ProxyWorkstationKey = "com.amazonaws.client.proxy-workstation";
    public static final String ProxyPreemptiveAuthKey = "com.amazonaws.client.preemptive-proxy-auth";
    public static final String SocketTimeoutKey = "com.amazonaws.client.socket-timeout";
    public static final String UserAgentKey = "com.amazonaws.client.user-agent";
    public static final String UseReaperKey = "com.amazonaws.client.use-reaper";
    public static final String UseGzipKey = "com.amazonaws.client.use-gzip";
    public static final String ReceiveBuffSizeHintKey = "com.amazonaws.client.socket-receive-buff-size-hint";
    public static final String SendBuffSizeHintKey = "com.amazonaws.client.socket-send-buff-size-hint";
    public static final String SignerOverrideKey = "com.amazonaws.client.signer-override";
    public static final String ConnectionTtlKey = "com.amazonaws.client.connection-ttl";

    
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

    private AwsClientConfiguration()
    {
    }

    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * @throws DaemonInitException 
     * 
     * @throws JMSException 
     */
    public static AwsClientConfig getClientConfiguration(RunnerServiceConfig config) throws Exception
    {
        AwsClientConfig awsClientConfig = (AwsClientConfig) config.properties.get(AwsClientConfiKey);
        if (awsClientConfig != null)
            return awsClientConfig;

        config.initsb.ttl("AWS Client Configuration:");
        config.initsb.level.incrementAndGet();

        String value = config.properties.getProperty(EndpointKey, EndpointDefault);
        config.initsb.ttl(EndpointKey, " = ", value);
        String endpoint = value;
        
        value = config.properties.getProperty(GroupIdKey, GroupIdDefault);
        config.initsb.ttl(GroupIdKey, " = ", value);
        String groupId = value;
        
        
        value = config.properties.getProperty(ConnectionTimeoutKey, ConnectionTimeoutDefault);
        config.initsb.ttl(ConnectionTimeoutKey, " = ", value);
        int connectionTimeout = Integer.parseInt(value);
        
        value = config.properties.getProperty(SocketTimeoutKey, SocketTimeoutDefault);
        config.initsb.ttl(SocketTimeoutKey, " = ", value);
        int socketTimeout = Integer.parseInt(value);

        value = config.properties.getProperty(MaxConnectionsKey, MaxConnectionsDefault);
        config.initsb.ttl(MaxConnectionsKey, " = ", value);
        int maxConnections = Integer.parseInt(value);

        value = config.properties.getProperty(MaxErrorRetryKey, MaxErrorRetryDefault);
        config.initsb.ttl(MaxErrorRetryKey, " = ", value);
        int maxErrorRetry = Integer.parseInt(value);

        value = config.properties.getProperty(RetryPolicyKey, RetryPolicyDefault);
        config.initsb.ttl(RetryPolicyKey, " = ", value);
        String retryPolicy = value;

        value = config.properties.getProperty(UseReaperKey, UseReaperDefault);
        config.initsb.ttl(UseReaperKey, " = ", value);
        boolean useReaper = Boolean.parseBoolean(value);

        value = config.properties.getProperty(UseGzipKey, UseGzipDefault);
        config.initsb.ttl(UseGzipKey, " = ", value);
        boolean useGzip = Boolean.parseBoolean(value);

        value = config.properties.getProperty(LocalAddressKey, LocalAddressDefault);
        config.initsb.ttl(LocalAddressKey, " = ", value);
        String localAddress = value;

        value = config.properties.getProperty(ProtocolKey, ProtocolDefault);
        config.initsb.ttl(ProtocolKey, " = ", value);
        String protocol = value;
        
        value = config.properties.getProperty(UserAgentKey, UserAgentDefault);
        config.initsb.ttl(UserAgentKey, " = ", value);
        String userAgent = value;

        value = config.properties.getProperty(ReceiveBuffSizeHintKey, ReceiveBuffSizeHintDefault);
        config.initsb.ttl(ReceiveBuffSizeHintKey, " = ", value);
        int receiveBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(SendBuffSizeHintKey, SendBuffSizeHintDefault);
        config.initsb.ttl(SendBuffSizeHintKey, " = ", value);
        int sendBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(SignerOverrideKey, SignerOverrideDefault);
        config.initsb.ttl(SignerOverrideKey, " = ", value);
        String signerOverride = value;

        value = config.properties.getProperty(ConnectionTtlKey, ConnectionTtlDefault);
        config.initsb.ttl(ConnectionTtlKey, " = ", value);
        long connectionTtl = Long.parseLong(value);
        
        config.initsb.ttl("AWS Client Proxy Configuration:");
        config.initsb.level.incrementAndGet();
        value = config.properties.getProperty(ProxyDomainKey, ProxyDomainDefault);
        config.initsb.ttl(ProxyDomainKey, " = ", value);
        String proxyDomain = value;

        value = config.properties.getProperty(ProxyHostKey, ProxyHostDefault);
        config.initsb.ttl(ProxyHostKey, " = ", value);
        String proxyHost = value;

        value = config.properties.getProperty(ProxyPasswordKey, ProxyPasswordDefault);
        config.initsb.ttl(ProxyPasswordKey, " = ", value);
        String proxyPassword = value;

        value = config.properties.getProperty(ProxyPortKey, ProxyPortDefault);
        config.initsb.ttl(ProxyPortKey, " = ", value);
        int proxyPort = Integer.parseInt(value);

        value = config.properties.getProperty(ProxyUserNameKey, ProxyUserNameDefault);
        config.initsb.ttl(ProxyUserNameKey, " = ", value);
        String proxyUserName = value;

        value = config.properties.getProperty(ProxyWorkstationKey, ProxyWorkstationDefault);
        config.initsb.ttl(ProxyWorkstationKey, " = ", value);
        String proxyWorkstation = value;

        value = config.properties.getProperty(ProxyPreemptiveAuthKey, ProxyPreemptiveAuthDefault);
        config.initsb.ttl(ProxyPreemptiveAuthKey, " = ", value);
        boolean proxyPreemptiveAuth = Boolean.parseBoolean(value);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        
        // @formatter:off
        ClientConfiguration clientConfig = new ClientConfiguration()
            .withMaxConnections(maxConnections)
            .withUserAgent(userAgent)
            .withProxyHost(proxyHost)
            .withProxyPort(proxyPort)
            .withProxyUsername(proxyUserName)
            .withProxyPassword(proxyPassword)
            .withProxyDomain(proxyDomain)
            .withProxyWorkstation(proxyWorkstation)
            .withSocketTimeout(socketTimeout)
            .withConnectionTimeout(connectionTimeout)
            .withReaper(useReaper)
            .withGzip(useGzip)
            .withSocketBufferSizeHints(sendBuffSizeHint, receiveBuffSizeHint)
            .withSignerOverride(signerOverride)
            .withConnectionTTL(connectionTtl)
            .withPreemptiveBasicProxyAuth(proxyPreemptiveAuth);
        // @formatter:on

        if(maxErrorRetry != -1)
            clientConfig.setMaxErrorRetry(maxErrorRetry);
        if(localAddress != null)
            clientConfig.setLocalAddress(InetAddress.getByName(value));
        if(Protocol.HTTPS.toString().equals(protocol))
            clientConfig.setProtocol(Protocol.HTTPS);
        else
            if(Protocol.HTTP.toString().equals(protocol))
                clientConfig.setProtocol(Protocol.HTTP);
        if(retryPolicy != null)
        {
            RetryPolicy rpolicy = (RetryPolicy) Class.forName(retryPolicy).newInstance();
            clientConfig.setRetryPolicy(rpolicy);
        }
        DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain(); // finds available aws creds
        awsClientConfig = new AwsClientConfig(clientConfig, providerChain, endpoint, groupId);
        config.properties.put(AwsClientConfiKey, awsClientConfig);
        return awsClientConfig;
    }

    public static class AwsClientConfig
    {
        public final ClientConfiguration clientConfig;
        public final DefaultAWSCredentialsProviderChain providerChain;
        public final String endpoint;
        public final String groupId;

        public AwsClientConfig(ClientConfiguration clientConfig, DefaultAWSCredentialsProviderChain providerChain, String endpoint, String groupId)
        {
            this.clientConfig = clientConfig;
            this.providerChain = providerChain;
            this.endpoint = endpoint;
            this.groupId = groupId;
        }
    }
}