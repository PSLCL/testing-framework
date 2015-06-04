package com.pslcl.qa.platform;

import javax.jms.JMSException;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

public class Sqs extends MessageQueueDaoAbstract {
    final private String queueName = "q"; // REVIEW: hard coding
    SQSConnection connection = null;
    AmazonSQSMessagingClientWrapper sqsClient = null;
    
    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * 
     * @throws JMSException 
     */
    @Override
    public void connect() throws JMSException {
        // create the builder then the connection; fill connection member for future use
        ClientConfiguration clientConfig = new ClientConfiguration().withConnectionTimeout(120 * 1000)
                .withMaxConnections(400)
                .withMaxErrorRetry(15);
        DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain(); // finds available aws creds
        SQSConnectionFactory connectionFactory =  SQSConnectionFactory.builder()
                .withClientConfiguration(clientConfig)
                .withAWSCredentialsProvider(providerChain)
                .withRegion(Region.getRegion(Regions.US_WEST_2)) // REVIEW: hard coding
                .build();
        connection = connectionFactory.createConnection();
        
        // check for queue; fill sqsClient member for future use
        sqsClient = connection.getWrappedAmazonSQSClient();
        // note: this next checks everything and can throw exceptions; no real action takes place prior to this
        if (!queueExists())
            throw new JMSException("message queue " + queueName + " not reachable");
        System.out.println("Sqs connects to message queue " + queueName);
    }

    @Override
    public boolean queueExists() throws JMSException {
        return sqsClient.queueExists(queueName);
    }
    
    @Override
    public void engage() throws JMSException {
        
    }

    @Override
    public void setInstanceEntry(long instanceNumber) throws JMSException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ackInstanceEntry(long instanceNumber) throws JMSException {
        // TODO Auto-generated method stub
        
    }

}
