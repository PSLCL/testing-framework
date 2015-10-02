package com.pslcl.qa.runner.resource.aws;

import javax.jms.JMSException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.pslcl.qa.runner.config.RunnerServiceConfig;

/**
 * Access AWS SQS via JMS
 * 
 */
public class AwsClientConfiguration
{
    public static final String AwsClientConfiKey="com.pslcl.qa.aws.client-config";
    
    private AwsClientConfiguration()
    {
    }

    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * 
     * @throws JMSException 
     */
    public static AwsClientConfig getClientConfiguration(RunnerServiceConfig config) throws ClassNotFoundException
    {
        AwsClientConfig awsClientConfig = (AwsClientConfig) config.getProperties().get(AwsClientConfiKey);
        if(awsClientConfig != null)
            return awsClientConfig;
        
        Class.forName("org.apache.commons.logging.LogFactory"); // required at run time for new ClientConfiguration()
        Class.forName("com.fasterxml.jackson.databind.ObjectMapper"); // required at run time for new ClientConfiguration()
        Class.forName("com.fasterxml.jackson.annotation.JsonAutoDetect"); // required at run time for new ClientConfiguration()
        Class.forName("org.apache.http.conn.scheme.SchemeSocketFactory"); // required at run time for .createConnection()

        ClientConfiguration clientConfig = new ClientConfiguration().withConnectionTimeout(120 * 1000).withMaxConnections(400).withMaxErrorRetry(15);
        DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain(); // finds available aws creds
        awsClientConfig = new AwsClientConfig(clientConfig, providerChain);
        config.getProperties().put(AwsClientConfiKey, awsClientConfig);
        return awsClientConfig;
    }
    
    public static class AwsClientConfig
    {
        public final ClientConfiguration clientConfig;
        public final DefaultAWSCredentialsProviderChain providerChain;
        
        public AwsClientConfig(
                        ClientConfiguration clientConfig, 
                        DefaultAWSCredentialsProviderChain providerChain)
        {
            this.clientConfig = clientConfig;
            this.providerChain = providerChain;
        }
    }
}