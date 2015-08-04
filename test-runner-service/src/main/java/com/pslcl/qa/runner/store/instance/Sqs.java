package com.pslcl.qa.runner.store.instance;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSSession;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.pslcl.qa.runner.RunnerService;

/**
 * Access AWS SQS via JMS
 * 
 */
public class Sqs extends MessageQueueDaoAbstract {
    
    private final String queueStoreName = "q"; // REVIEW: hard coded
    // Note: Whether hard-coded or not, this queue is identified here as being unique. So multiple RunnerService and Sqs class objects may exist, but there is only one QueueStore message queue.
    
    private AmazonSQSMessagingClientWrapper sqsClient = null;
    private SQSConnection connection = null;
    
    public Sqs(RunnerService runnerService) {
        super(runnerService);
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
                    prependString += " msgID " + jmsMessageID + ", stringQueueStoreNumber " +  strQueueStoreNumber + ". ";
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
                        System.out.println(prependString + "Drop - jmsMessageID or stringQueueStoreNumber are null");
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
    public void connect() throws JMSException {
        if (sqsClient == null || !queueStoreExists()) {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            try {
                Class.forName("org.apache.commons.logging.LogFactory");           // required at run time for new ClientConfiguration()
                Class.forName("com.fasterxml.jackson.core.Versioned");            // required at run time for Sqs connect, below
                Class.forName("com.fasterxml.jackson.databind.ObjectMapper");     // required at run time for new ClientConfiguration()
                Class.forName("com.fasterxml.jackson.annotation.JsonAutoDetect"); // required at run time for new ClientConfiguration()
                Class.forName("com.amazonaws.services.sqs.AmazonSQS");            // required at run time for SQSConnectionFactory.builder()
                Class.forName("org.joda.time.format.DateTimeFormat");             // required at run time for Sqs connect, below
                Class.forName("org.apache.http.protocol.HttpContext");            // required at run time for Sqs connect, below
                Class.forName("org.apache.http.conn.scheme.SchemeSocketFactory"); // required at run time for .createConnection()
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            // create the builder then the connection; fill connection member for future use
            ClientConfiguration clientConfig = new ClientConfiguration()
                    .withConnectionTimeout(120 * 1000)
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