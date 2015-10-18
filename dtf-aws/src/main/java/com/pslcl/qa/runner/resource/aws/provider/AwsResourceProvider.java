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
package com.pslcl.qa.runner.resource.aws.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.resource.aws.AwsClientConfiguration;
import com.pslcl.qa.runner.resource.aws.AwsClientConfiguration.AwsClientConfig;

public abstract class AwsResourceProvider
{
    protected final Logger log;
    protected volatile RunnerServiceConfig config;
    protected volatile AmazonEC2Client ec2Client;
    
    protected AwsResourceProvider()
    {
        log = LoggerFactory.getLogger(getClass());
    }
    
    protected void init(RunnerServiceConfig config) throws Exception
    {
        this.config = config;
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        AwsClientConfig cconfig = AwsClientConfiguration.getClientConfiguration(config);
        ec2Client = new AmazonEC2Client(cconfig.clientConfig);
        ec2Client.setEndpoint(cconfig.endpoint);
        config.initsb.indentedOk();
        
        config.initsb.level.decrementAndGet();
    }
    
    protected void destroy()
    {
        ec2Client.shutdown();
    }
}
//        DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
//        ec2Client.
//        
//        
//        ConnectionFactory connectionFactory =  EC2ConnectionFactory.builder()
//                        .withClientConfiguration(awsClientConfig.clientConfig)
//                        .withAWSCredentialsProvider(awsClientConfig.providerChain)
//                        .withRegion(Region.getRegion(Regions.US_WEST_2)) // REVIEW: hard coding
//                        .build();
//                connection = connectionFactory.createConnection();
//                
//                // check for queue; fill sqsClient member for future use
//                sqsClient = connection.getWrappedAmazonSQSClient();

