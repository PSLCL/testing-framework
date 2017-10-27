package com.pslcl.dtf.resource.aws;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazon.sqs.javamessaging.SQSSession;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.messageQueue.MessageQueueBase;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.AwsClientConfig;
import com.pslcl.dtf.resource.aws.AwsClientConfiguration.ClientType;

/**
 * Access AWS SQS via JMS
 * 
 */

@SuppressWarnings("javadoc")
public class Sqs extends MessageQueueBase {
    
    // Note: Whether hard-coded or not, this queue is identified here as being unique. So multiple RunnerService and Sqs class objects may exist, but there is only one QueueStore message queue.
    
    private volatile String queueStoreName;
    private volatile AwsClientConfig awsClientConfig;
    private volatile boolean isDestroyed = false;
    private final Logger log;
    
    private AmazonSQSMessagingClientWrapper sqsClient = null;
    private SQSConnection connection = null;
    private MessageConsumer consumer = null;

    private static final long queueWaitTime = 5000L;
    private static final long queuePollTimeout = 5000L;

    public Sqs() {
        this.log = LoggerFactory.getLogger(getClass());
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        super.init(config);
        config.initsb.ttl("Message Queue initialization");
        config.initsb.level.incrementAndGet();
        queueStoreName = StrH.trim(config.properties.getProperty(ResourceNames.MsgQueNameKey, ResourceNames.MsgQueClassDefault));
        config.initsb.ttl(ResourceNames.MsgQueNameKey, " = ", queueStoreName);
        if(queueStoreName == null)
            throw new Exception(ResourceNames.MsgQueNameKey + " must be specified in configuration properties file");
        config.initsb.ttl("sqsClient:");
        config.initsb.level.incrementAndGet();
        awsClientConfig = AwsClientConfiguration.getClientConfiguration(config, ClientType.Sqs);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        connect();
    }
    
    @Override
    public void destroy() 
    {
        try
        {
            isDestroyed = true;
            //TODO: should the cleanupQueueStoreAccess call to connection.stop be on this cycle only? both?
            connection.stop();
        } catch (JMSException e)
        {
            LoggerFactory.getLogger(getClass()).error("failed to cleanup Sqs connection", e);
        }
    }

    private void processMessage(Message message) {
        String prependString = "SQS.processMessage()";
        if (message != null) {
            try {
                // jmsMessageID is set unique by JMS producer, as for example: UUID jmsMessageID = "ID:" + java.util.UUID.randomUUID();
                String jmsMessageID = message.getJMSMessageID(); // begins with "ID:", by JMS specification
                String strQueueStoreNumber = ((TextMessage)message).getText();
                log.debug("\n");
                prependString += " msgID " + jmsMessageID + ", strQueueStoreNumber " +  strQueueStoreNumber + ".";
                if (jmsMessageID != null && strQueueStoreNumber != null) {
                    // design decision: Object message will not, as it could, be stored as key value pair "jmsMessageID/hexStrQueueStoreNumber." Message instead passes out here, as state for eventual ack.
                    log.debug(prependString + " Submits to RunnerService to form or reject a test run.");
                    submitQueueStoreNumber(strQueueStoreNumber, message); // choose to pass message via DAO
                    return;
                }
                log.debug(prependString + " Drop msg - jmsMessageID or strQueueStoreNumber are null");
            } catch (JMSException jmse) {
                log.debug(prependString + " Drop msg - JMS message could not be examined " + jmse, jmse);
            } catch (Throwable t) {
                log.debug(prependString + "Drop msg - JMS message rejected by RunnerService - " + t, t);
            }

            // .submitQueueStoreNumber() failed to be deliver message to dtf-runner
            log.warn(prependString + " Message not acked");
        } else {
            log.debug(prependString + " Dropped - null message cannot be processed");
        }
    }

    
    // MessageQueue interface implementation
    
    private void connect() throws Exception
    {
        if (sqsClient == null || !queueStoreExists()) {
            if (connection != null) {
                connection.close();
                connection = null;
            }

            // com.fasterxml.jackson.core.Versioned            // required at run time for Sqs connect, below
            // com.amazonaws.services.sqs.AmazonSQS            // required at run time for SQSConnectionFactory.builder()
            // org.joda.time.format.DateTimeFormat             // required at run time for Sqs connect
            // org.apache.http.protocol.HttpContext            // required at run time for Sqs connect, below

            SQSConnectionFactory connectionFactory =  SQSConnectionFactory.builder()
                    .withClientConfiguration(awsClientConfig.clientConfig)
                    .withAWSCredentialsProvider(awsClientConfig.providerChain)
                    .withRegion(awsClientConfig.region)
                    .build();
            connection = connectionFactory.createConnection();
            
            // check for queue; fill sqsClient member for future use
            sqsClient = connection.getWrappedAmazonSQSClient();
            // note: this next checks everything and can throw exceptions; no real action takes place prior to this
            if (!queueStoreExists())
                throw new JMSException("message queue " + queueStoreName + " not reachable");
            log.debug("Sqs connects to message queue " + queueStoreName);
        } else {
            log.debug("DEBUG: Sqs already connected to message queue " + queueStoreName);
        }
    }

    private void startMessageRetrieveProcess(){
        new Thread(() -> {
            while(!isDestroyed){
                if(isAvailableToProcess()){
                    Message message = null;
                    try {
                        message = consumer.receive(queuePollTimeout);
                    } catch (JMSException e) {
                        log.debug("Failed to receive message from SQS.", e);
                    }
                    if(message != null) {
                        processMessage(message);
                    }
                } else {
                    try {
                        Thread.sleep(queueWaitTime);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }).start();
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
        consumer = session.createConsumer(queueStore);

        connection.start();

        startMessageRetrieveProcess();
        log.debug("Sqs.initQueueStoreGet() successful, for message queue " + queueStoreName);
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