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
package com.pslcl.dtf.resource.aws;

import java.net.InetAddress;

import javax.jms.JMSException;

import org.apache.commons.daemon.DaemonInitException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.retry.RetryPolicy;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.resource.aws.attr.ClientNames;

@SuppressWarnings("javadoc")
public class AwsClientConfiguration
{
    private AwsClientConfiguration()
    {
    }

    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * @throws DaemonInitException 
     * 
     * @throws JMSException 
     */
    public static AwsClientConfig getClientConfiguration(RunnerConfig config) throws Exception
    {
        AwsClientConfig awsClientConfig = (AwsClientConfig) config.properties.get(ClientNames.ConfiKey);
        if (awsClientConfig != null)
            return awsClientConfig;

        config.initsb.ttl("AWS Client Configuration:");
        config.initsb.level.incrementAndGet();

        String value = config.properties.getProperty(ClientNames.RegionKey, ClientNames.RegionDefault);
        config.initsb.ttl(ClientNames.RegionKey, " = ", value);
        Region region = RegionUtils.getRegion(value);
        
        value = config.properties.getProperty(ClientNames.EndpointKey, ClientNames.EndpointDefault);
        config.initsb.ttl(ClientNames.EndpointKey, " = ", value);
        String endpoint = value;
        
        value = config.properties.getProperty(ClientNames.GroupIdKey, ClientNames.GroupIdDefault);
        config.initsb.ttl(ClientNames.GroupIdKey, " = ", value);
        String groupId = value;
        
        
        value = config.properties.getProperty(ClientNames.ConnectionTimeoutKey, ClientNames.ConnectionTimeoutDefault);
        config.initsb.ttl(ClientNames.ConnectionTimeoutKey, " = ", value);
        int connectionTimeout = Integer.parseInt(value);
        
        value = config.properties.getProperty(ClientNames.SocketTimeoutKey, ClientNames.SocketTimeoutDefault);
        config.initsb.ttl(ClientNames.SocketTimeoutKey, " = ", value);
        int socketTimeout = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.MaxConnectionsKey, ClientNames.MaxConnectionsDefault);
        config.initsb.ttl(ClientNames.MaxConnectionsKey, " = ", value);
        int maxConnections = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.MaxErrorRetryKey, ClientNames.MaxErrorRetryDefault);
        config.initsb.ttl(ClientNames.MaxErrorRetryKey, " = ", value);
        int maxErrorRetry = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.RetryPolicyKey, ClientNames.RetryPolicyDefault);
        config.initsb.ttl(ClientNames.RetryPolicyKey, " = ", value);
        String retryPolicy = value;

        value = config.properties.getProperty(ClientNames.UseReaperKey, ClientNames.UseReaperDefault);
        config.initsb.ttl(ClientNames.UseReaperKey, " = ", value);
        boolean useReaper = Boolean.parseBoolean(value);

        value = config.properties.getProperty(ClientNames.UseGzipKey, ClientNames.UseGzipDefault);
        config.initsb.ttl(ClientNames.UseGzipKey, " = ", value);
        boolean useGzip = Boolean.parseBoolean(value);

        value = config.properties.getProperty(ClientNames.LocalAddressKey, ClientNames.LocalAddressDefault);
        config.initsb.ttl(ClientNames.LocalAddressKey, " = ", value);
        String localAddress = value;

        value = config.properties.getProperty(ClientNames.ProtocolKey, ClientNames.ProtocolDefault);
        config.initsb.ttl(ClientNames.ProtocolKey, " = ", value);
        String protocol = value;
        
        value = config.properties.getProperty(ClientNames.UserAgentKey, ClientNames.UserAgentDefault);
        config.initsb.ttl(ClientNames.UserAgentKey, " = ", value);
        String userAgent = value;

        value = config.properties.getProperty(ClientNames.ReceiveBuffSizeHintKey, ClientNames.ReceiveBuffSizeHintDefault);
        config.initsb.ttl(ClientNames.ReceiveBuffSizeHintKey, " = ", value);
        int receiveBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.SendBuffSizeHintKey, ClientNames.SendBuffSizeHintDefault);
        config.initsb.ttl(ClientNames.SendBuffSizeHintKey, " = ", value);
        int sendBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.SignerOverrideKey, ClientNames.SignerOverrideDefault);
        config.initsb.ttl(ClientNames.SignerOverrideKey, " = ", value);
        String signerOverride = value;

        value = config.properties.getProperty(ClientNames.ConnectionTtlKey, ClientNames.ConnectionTtlDefault);
        config.initsb.ttl(ClientNames.ConnectionTtlKey, " = ", value);
        long connectionTtl = Long.parseLong(value);
        
        config.initsb.ttl("AWS Client Proxy Configuration:");
        config.initsb.level.incrementAndGet();
        value = config.properties.getProperty(ClientNames.ProxyDomainKey, ClientNames.ProxyDomainDefault);
        config.initsb.ttl(ClientNames.ProxyDomainKey, " = ", value);
        String proxyDomain = value;

        value = config.properties.getProperty(ClientNames.ProxyHostKey, ClientNames.ProxyHostDefault);
        config.initsb.ttl(ClientNames.ProxyHostKey, " = ", value);
        String proxyHost = value;

        value = config.properties.getProperty(ClientNames.ProxyPasswordKey, ClientNames.ProxyPasswordDefault);
        config.initsb.ttl(ClientNames.ProxyPasswordKey, " = ", value);
        String proxyPassword = value;

        value = config.properties.getProperty(ClientNames.ProxyPortKey, ClientNames.ProxyPortDefault);
        config.initsb.ttl(ClientNames.ProxyPortKey, " = ", value);
        int proxyPort = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.ProxyUserNameKey, ClientNames.ProxyUserNameDefault);
        config.initsb.ttl(ClientNames.ProxyUserNameKey, " = ", value);
        String proxyUserName = value;

        value = config.properties.getProperty(ClientNames.ProxyWorkstationKey, ClientNames.ProxyWorkstationDefault);
        config.initsb.ttl(ClientNames.ProxyWorkstationKey, " = ", value);
        String proxyWorkstation = value;

        value = config.properties.getProperty(ClientNames.ProxyPreemptiveAuthKey, ClientNames.ProxyPreemptiveAuthDefault);
        config.initsb.ttl(ClientNames.ProxyPreemptiveAuthKey, " = ", value);
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
        awsClientConfig = new AwsClientConfig(clientConfig, providerChain, region, endpoint, groupId);
        config.properties.put(ClientNames.ConfiKey, awsClientConfig);
        return awsClientConfig;
    }

    public static class AwsClientConfig
    {
        public final ClientConfiguration clientConfig;
        public final DefaultAWSCredentialsProviderChain providerChain;
        public final Region region;
        public final String endpoint;
        public final String groupId;

        //@formatter:off
        public AwsClientConfig(
                        ClientConfiguration clientConfig, 
                        DefaultAWSCredentialsProviderChain providerChain, 
                        Region region,
                        String endpoint, 
                        String groupId)
        //@formatter:on
        {
            this.clientConfig = clientConfig;
            this.providerChain = providerChain;
            this.region = region;
            this.endpoint = endpoint;
            this.groupId = groupId;
        }
    }
}