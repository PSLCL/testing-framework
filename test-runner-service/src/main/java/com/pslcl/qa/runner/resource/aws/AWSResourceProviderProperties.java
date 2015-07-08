package com.pslcl.qa.runner.resource.aws;

public class AWSResourceProviderProperties {
	/**
	 * Resource Group ID - Used to tag AWS resources so that they are identifiable by the resource provider.
	 */
	public String GroupID;
	private static final String GroupIDKey = "com.pslcl.qa.platform.resource.aws.GroupID";
	private static final String DefaultGroupID = "AWSTestResource";
	
}
