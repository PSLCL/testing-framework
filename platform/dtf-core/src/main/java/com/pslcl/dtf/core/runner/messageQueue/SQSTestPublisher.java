package com.pslcl.dtf.core.runner.messageQueue;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class SQSTestPublisher {
	
	private String awsEndpointURL;
	private AWSCredentialsProvider credentialsProvider;
	private ClientConfiguration clientConfig;
	private String queueName;
	private AmazonSQSClient client;
	private String queueUrl;

	/**
	 * Create a SQSTestPublisher.
	 * 
	 * @param awsEndpointURL Specifies the sqs endpoint url
	 * @param credentialsProvider The def
	 * @param clientConfig
	 * @param queueName The name of the queue. Must not be null.
	 */
	public SQSTestPublisher(String awsEndpointURL, AWSCredentialsProvider credentialsProvider, ClientConfiguration clientConfig, String queueName) {
		this.awsEndpointURL = awsEndpointURL;
		this.credentialsProvider = credentialsProvider;
		this.clientConfig = clientConfig;
		this.queueName = queueName;
	}
	
	/**
	 * Initialize the AmazonSQSClient.
	 */
	public void init(){
		if(credentialsProvider == null){
			credentialsProvider = new DefaultAWSCredentialsProviderChain();
		}
		
		if(clientConfig == null){
			client = new AmazonSQSClient(credentialsProvider);
		} else {
			client = new AmazonSQSClient(credentialsProvider, clientConfig);
		}
		client.setEndpoint(awsEndpointURL);
		GetQueueUrlRequest urlRequest = new GetQueueUrlRequest()
		      .withQueueName(queueName);
		queueUrl = client.getQueueUrl(urlRequest).getQueueUrl();
	}
	
	/**
	 * Request that a test run be executed by the Test Runner Service.
	 */
	public void publishTestRunRequest(long runID) throws AmazonClientException {
		SendMessageRequest sendRequest = new SendMessageRequest(queueUrl, Long.toString(runID));
		client.sendMessage(sendRequest);
	}
}
