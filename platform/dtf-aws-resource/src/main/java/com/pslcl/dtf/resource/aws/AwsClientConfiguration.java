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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.retry.RetryPolicy;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.resource.aws.attr.ClientNames;

@SuppressWarnings("javadoc")
public class AwsClientConfiguration
{
    private AwsClientConfiguration()
    {
    }

    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * @throws Exception if configuration can not be obtained. 
     */
    public static AwsClientConfig getClientConfiguration(RunnerConfig config, ClientType type) throws Exception
    {
        String locationKey = ClientNames.ClientKeyBase; 
        String availDefault = null;
        String regionDefault = null;
        String endpointDefault = null;
        switch(type)
        {
            case Ec2:
                locationKey = ClientNames.Ec2ClientKeyBase;
                availDefault = ClientNames.Ec2AvailabilityZoneDefault;
                regionDefault = ClientNames.Ec2RegionDefault;
                endpointDefault = ClientNames.Ec2EndpointDefault;
                break;
            case Ses:
                locationKey = ClientNames.SesClientKeyBase;
                availDefault = ClientNames.SesAvailabilityZoneDefault;
                regionDefault = ClientNames.SesRegionDefault;
                endpointDefault = ClientNames.SesEndpointDefault;
                break;
            case Sqs:
                locationKey = ClientNames.SqsClientKeyBase;
                availDefault = ClientNames.SqsAvailabilityZoneDefault;
                regionDefault = ClientNames.SqsRegionDefault;
                endpointDefault = ClientNames.SqsEndpointDefault;
                break;
            default:
                break;
        }
        
        String value = config.properties.getProperty(locationKey + ClientNames.RegionName, regionDefault);
        value = StrH.trim(value);
        config.initsb.ttl(locationKey+ ClientNames.RegionName, " = ", value);
        Region region = RegionUtils.getRegion(value);
        
        value = config.properties.getProperty(locationKey + ClientNames.EndpointName, endpointDefault);
        value = StrH.trim(value);
        config.initsb.ttl(locationKey + ClientNames.EndpointName, " = ", value);
        String endpoint = value;
        
        value = config.properties.getProperty(locationKey + ClientNames.AvailabilityZoneName, availDefault);
        value = StrH.trim(value);
        config.initsb.ttl(locationKey + ClientNames.AvailabilityZoneName, " = ", value);
        String availabilityZone = value;
        
        value = config.properties.getProperty(ClientNames.GroupIdKey, ClientNames.GroupIdDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.GroupIdKey, " = ", value);
        String groupId = value;
        
        value = config.properties.getProperty(ClientNames.ConnectionTimeoutKey, ClientNames.ConnectionTimeoutDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ConnectionTimeoutKey, " = ", value);
        int connectionTimeout = Integer.parseInt(value);
        
        value = config.properties.getProperty(ClientNames.SocketTimeoutKey, ClientNames.SocketTimeoutDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.SocketTimeoutKey, " = ", value);
        int socketTimeout = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.MaxConnectionsKey, ClientNames.MaxConnectionsDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.MaxConnectionsKey, " = ", value);
        int maxConnections = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.MaxErrorRetryKey, ClientNames.MaxErrorRetryDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.MaxErrorRetryKey, " = ", value);
        int maxErrorRetry = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.RetryPolicyKey, ClientNames.RetryPolicyDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.RetryPolicyKey, " = ", value);
        String retryPolicy = value;

        value = config.properties.getProperty(ClientNames.UseReaperKey, ClientNames.UseReaperDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.UseReaperKey, " = ", value);
        boolean useReaper = Boolean.parseBoolean(value);

        value = config.properties.getProperty(ClientNames.UseGzipKey, ClientNames.UseGzipDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.UseGzipKey, " = ", value);
        boolean useGzip = Boolean.parseBoolean(value);

        value = config.properties.getProperty(ClientNames.LocalAddressKey, ClientNames.LocalAddressDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.LocalAddressKey, " = ", value);
        String localAddress = value;

        value = config.properties.getProperty(ClientNames.ProtocolKey, ClientNames.ProtocolDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProtocolKey, " = ", value);
        String protocol = value;
        
        value = config.properties.getProperty(ClientNames.UserAgentKey, ClientNames.UserAgentDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.UserAgentKey, " = ", value);
        String userAgent = value;

        value = config.properties.getProperty(ClientNames.ReceiveBuffSizeHintKey, ClientNames.ReceiveBuffSizeHintDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ReceiveBuffSizeHintKey, " = ", value);
        int receiveBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.SendBuffSizeHintKey, ClientNames.SendBuffSizeHintDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.SendBuffSizeHintKey, " = ", value);
        int sendBuffSizeHint = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.SignerOverrideKey, ClientNames.SignerOverrideDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.SignerOverrideKey, " = ", value);
        String signerOverride = value;

        value = config.properties.getProperty(ClientNames.ConnectionTtlKey, ClientNames.ConnectionTtlDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ConnectionTtlKey, " = ", value);
        long connectionTtl = Long.parseLong(value);
        
        config.initsb.ttl("AWS Client Proxy Configuration:");
        config.initsb.level.incrementAndGet();
        value = config.properties.getProperty(ClientNames.ProxyDomainKey, ClientNames.ProxyDomainDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyDomainKey, " = ", value);
        String proxyDomain = value;

        value = config.properties.getProperty(ClientNames.ProxyHostKey, ClientNames.ProxyHostDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyHostKey, " = ", value);
        String proxyHost = value;

        value = config.properties.getProperty(ClientNames.ProxyPasswordKey, ClientNames.ProxyPasswordDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyPasswordKey, " = ", value);
        String proxyPassword = value;

        value = config.properties.getProperty(ClientNames.ProxyPortKey, ClientNames.ProxyPortDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyPortKey, " = ", value);
        int proxyPort = Integer.parseInt(value);

        value = config.properties.getProperty(ClientNames.ProxyUserNameKey, ClientNames.ProxyUserNameDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyUserNameKey, " = ", value);
        String proxyUserName = value;

        value = config.properties.getProperty(ClientNames.ProxyWorkstationKey, ClientNames.ProxyWorkstationDefault);
        value = StrH.trim(value);
        config.initsb.ttl(ClientNames.ProxyWorkstationKey, " = ", value);
        String proxyWorkstation = value;

        value = config.properties.getProperty(ClientNames.ProxyPreemptiveAuthKey, ClientNames.ProxyPreemptiveAuthDefault);
        value = StrH.trim(value);
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
        return new AwsClientConfig(clientConfig, providerChain, region, endpoint, availabilityZone, groupId);
//        config.properties.put(ClientNames.ConfiKey, awsClientConfig);
//        return awsClientConfig;
    }

    public static class AwsClientConfig
    {
        public final ClientConfiguration clientConfig;
        public final DefaultAWSCredentialsProviderChain providerChain;
        public final Region region;
        public final String endpoint;
        public final String availabilityZone;
        public final String groupId;

        //@formatter:off
        public AwsClientConfig(
                        ClientConfiguration clientConfig, 
                        DefaultAWSCredentialsProviderChain providerChain, 
                        Region region,
                        String endpoint, 
                        String availabilityZone,
                        String groupId)
        //@formatter:on
        {
            this.clientConfig = clientConfig;
            this.providerChain = providerChain;
            this.region = region;
            this.endpoint = endpoint;
            this.availabilityZone = availabilityZone;
            this.groupId = groupId;
        }
    }
    
    public enum ClientType
    {
        Ec2, Ses, Sqs;
    }
}