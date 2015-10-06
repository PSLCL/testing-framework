package com.pslcl.qa.runner.resource.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
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

