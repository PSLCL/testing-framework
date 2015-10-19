package com.pslcl.dtf.resource.aws;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.LoggerFactory;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSSession;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.pslcl.dtf.common.config.RunnerConfig;
import com.pslcl.dtf.common.messageQueue.MessageQueueBase;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.AwsClientConfig;

/**
 * Access AWS SQS via JMS
 * 
 */
public class Sqs extends MessageQueueBase {
    
    public static final String QueueStoreNameKey = "com.pslcl.qa.runner.store.instance.queue-name";
    public static final String QueueStoreNameDefault = "q";
    
    private volatile String queueStoreName;
    private volatile AwsClientConfig awsClientConfig;
    
    // Note: Whether hard-coded or not, this queue is identified here as being unique. So multiple RunnerService and Sqs class objects may exist, but there is only one QueueStore message queue.
    
    private AmazonSQSMessagingClientWrapper sqsClient = null;
    private SQSConnection connection = null;
    
    public Sqs() {
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        super.init(config);
        config.initsb.ttl("Message Queue initialization");
        config.initsb.level.incrementAndGet();
        queueStoreName = config.properties.getProperty(QueueStoreNameKey, QueueStoreNameDefault);
        config.initsb.ttl(QueueStoreNameKey, " = ", queueStoreName);
        awsClientConfig = AwsClientConfiguration.getClientConfiguration(config);
        config.initsb.level.decrementAndGet();
        connect(awsClientConfig);
    }
    
    @Override
    public void destroy() 
    {
        try
        {
            //TODO: should the cleanupQueueStoreAccess call to connection.stop be on this cycle only? both?
            connection.stop();
        } catch (JMSException e)
        {
            LoggerFactory.getLogger(getClass()).error("failed to cleanup Sqs connection", e);
        }
    }
    
    // MessageListener interface implementations
    
    private class GetQueueStoreCallback implements MessageListener {
        private final Sqs sqs;
        
        public GetQueueStoreCallback(Sqs sqs) {
            this.sqs = sqs;
        }
        
        @Override
        public void onMessage(Message message) {
            String prependString = "GetQueueStoreCallback.onMessage()";
            if (message != null) {
                try {
                    // jmsMessageID is set unique by JMS producer, as for example: UUID jmsMessageID = "ID:" + java.util.UUID.randomUUID(); 
                    String jmsMessageID = message.getJMSMessageID(); // begins with "ID:", by JMS specification
                    String strQueueStoreNumber = ((TextMessage)message).getText();
                    System.out.println("\n");
                    prependString += " msgID " + jmsMessageID + ", strQueueStoreNumber " +  strQueueStoreNumber + ". ";
                    if (jmsMessageID != null && strQueueStoreNumber != null) {
                        System.out.println(prependString);
                        // design decision: Object message will not, as it could, be stored as key value pair "jmsMessageID/hexStrQueueStoreNumber." Message instead passes out here, as state for eventual ack.
                        if (sqs.submitQueueStoreNumber(strQueueStoreNumber, message)) // choose to pass message via DAO
                        {
                            System.out.println(prependString + "Submitted to RunnerService");
                            return;
                        }
                        System.out.println(prependString + "Drop - rejected by RunnerService");
                    } else {
                        System.out.println(prependString + "Drop - jmsMessageID or strQueueStoreNumber are null");
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println(prependString + "Drop - rejected by RunnerService " + e);
                }
                
                try {
                    sqs.ackQueueStoreEntry(message); // choose to use DAO access to message
                    System.out.println(prependString + "Acked");
                } catch (JMSException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                System.out.println(prependString + "Dropped - null message cannot be processed or acked");
            }
        }
    }
    
    
    // QueueStoreDAO interface implementation
    
    /**
     * Establish connection to a specific AWS SQS message queue in a specific AWS region
     * 
     * @throws JMSException 
     */
    @Override
    public void connect() throws JMSException 
    {
        //TODO: Clint I think we can remove this connect from the interface 
    }
    
    private void connect(AwsClientConfig awsClientConfig) throws Exception
    {
        if (sqsClient == null || !queueStoreExists()) {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            //TODO: Clint review as he had this code in. 
            // this is not likely a good practice for the build system which handles compile time requirements 
            // separate from deployed runtime requirements.  Also if using ivy or maven this will be picked up
            // automatically as needed via the compile/runtime separation of all dependencies to any depth.
            // suggest moving your personal eclipse build to ivy or maven.
            // also see AwsClientConfiguration where they have been commented out there at this time.
            
            Class.forName("com.fasterxml.jackson.core.Versioned");            // required at run time for Sqs connect, below
            Class.forName("com.amazonaws.services.sqs.AmazonSQS");            // required at run time for SQSConnectionFactory.builder()
            Class.forName("org.joda.time.format.DateTimeFormat");             // required at run time for Sqs connect, below
            Class.forName("org.apache.http.protocol.HttpContext");            // required at run time for Sqs connect, below
            
            SQSConnectionFactory connectionFactory =  SQSConnectionFactory.builder()
                    .withClientConfiguration(awsClientConfig.clientConfig)
                    .withAWSCredentialsProvider(awsClientConfig.providerChain)
                    .withRegion(Region.getRegion(Regions.US_WEST_2)) // REVIEW: hard coding
                    .build();
            connection = connectionFactory.createConnection();
            
            // check for queue; fill sqsClient member for future use
            sqsClient = connection.getWrappedAmazonSQSClient();
            // note: this next checks everything and can throw exceptions; no real action takes place prior to this
            if (!queueStoreExists())
                throw new JMSException("message queue " + queueStoreName + " not reachable");
            System.out.println("Sqs connects to message queue " + queueStoreName); // TODO: log through slf4j
        } else {
            System.out.println("DEBUG: Sqs already connected to message queue " + queueStoreName);
        }
    }

    @Override
    public boolean queueStoreExists() throws JMSException {
        return sqsClient.queueExists(queueStoreName);
    }
    
    @Override
    public void initQueueStoreGet() throws JMSException {
        // setup asynchronous receive 
        // UNORDERED_ACKNOWLEDGE mode: apply ack to only one specific message (see Amazon SQS docs)
        // TextMessage mode (message payload is String). 

        Session session = connection.createSession(false, SQSSession.UNORDERED_ACKNOWLEDGE); // API also allows a subset of constants to come from Session
        // note: connection and session are not tied to an actual queue

        // create our Java object of the queue (not the actual message queue on the AWS account)
        Queue queueStore = session.createQueue(queueStoreName);

        // create our Consumer of the QueueStore message queue (as opposed to other possible message queues that could be accessible)
        MessageConsumer consumer = session.createConsumer(queueStore);
        
        GetQueueStoreCallback getQueueStoreCallback = new GetQueueStoreCallback(this);
        consumer.setMessageListener(getQueueStoreCallback);
        
        connection.start();
        System.out.println("Sqs.initQueueStoreGet() successful, for message queue " + queueStoreName); // TODO: log through slf4j
    }

    @Override
    public void ackQueueStoreEntry(String jmsMessageID) throws JMSException {
        // not needed: instead, we use ackQueueStoreEntry(Message message)
    }
    
    @Override
    public void initQueueStoreSet() throws JMSException {
        // RunnerService does not set entries in the QueueStore
    }
    
    @Override
    public void setQueueStoreEntry(long queueStoreEntryNumber) throws JMSException {
        // RunnerService does not set entries in the QueueStore
    }

    @Override
    public void cleanupQueueStoreAccess() throws JMSException {
        connection.stop(); // matches the .start() in initQueueStoreGet()
        
    }
    
}