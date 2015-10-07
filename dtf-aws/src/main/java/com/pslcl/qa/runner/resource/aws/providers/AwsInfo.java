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
package com.pslcl.qa.runner.resource.aws.providers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;

public class AwsInfo {

	public static final String ENV_REGION = "AWS_REGION";

	// AWS Tag Keys
	public static final String SYSTEMIDKEY = "PDSPSystemID";
	public static final String AUTHENTICATORIDKEY = "PDSPAuthenticatorID";
	public static final String CONNECTIONTHRESHOLDKEY = "PDSPConnectionCountThreshold";
	public static final String THREADTHRESHOLDKEY = "PDSPThreadCountThreshold";
	// AWS VPC Subnet Keys
	public static final String EIPSTOREKEY = "PDSPStaticIPs";
	public static final String PDSPSTATICHOSTNAME = "PDSPStaticHostname";
	// AWS Tag Keys used by Puppet for DSP (documented here for completeness)
	public static final String VERSIONKEY = "PDSPVersion";
	public static final String NODETYPEKEY = "PDSPNodeType";
	public static final String CONFIGPRIVATEPATHKEY = "PDSPConfigPrivatePath";
	public static final String CONFIGPUBLICPATHKEY = "PDSPConfigPublicPath";
	// AWS Tag Keys used by Puppet for DSPNodeCheck (documented here for completeness)
	public static final String CHECKADDRESSKEY = "PDSPAddress";
	public static final String CHECKDOMAINSKEY = "PDSPCheckDomains";
	public static final String CHECKREGIONSKEY = "PDSPCheckRegions";
	public static final String CHECKOPTIONSKEY = "PDSPCheckOptions";
	// AWS Tag Keys used for Auto Scaling Groups
    public static final String AUTOSCALEGROUPNAMEKEY = "aws:autoscaling:groupName";

	public static final String AUTHSUFFIX = "-auth";
	public static final String RUNNING = "running";
	public static final String META_ADDRESS = "http://169.254.169.254/latest/meta-data/";
	public static final String INSTANCEID = "instance-id";
	public static final String INSTANCETYPE = "instance-type";
	public static final String LOCALIP = "local-ipv4";
	public static final String PUBLICIP = "public-ipv4";
	public static final String PUBLICHOSTNAME = "public-hostname";
	public static final String AVAILABILITYZONE = "placement/availability-zone";
	public static final String MACADDRESS = "mac";
	public static final String AWSURLSUFF = ".amazonaws.com";
	public static final String AWSEC2PRE = "ec2.";
	public static final String AWSELBPRE = "elasticloadbalancing.";
	public static final String AWSCWPRE = "monitoring.";
	public static final String AWSASPRE = "autoscaling.";

	public static final String CONN = "conn";
	
	private static HashMap<String,String> localKeys = new HashMap<String,String>();
	private static HashSet<String> storedRegionList = new HashSet<String>();
	private static String storedLocalRegion = null;
	
	public static String getEC2Endpoint() {
		return getEC2Endpoint(getRegion());
	}
	
	public static String getEC2Endpoint(String region) {
		return AWSEC2PRE + region + AWSURLSUFF;
	}
	
	public static String getELBEndpoint() {
		return getELBEndpoint(getRegion());
	}
	
	public static String getELBEndpoint(String region) {
		if(region == null || region.isEmpty()) region = AwsInfo.getRegion();
		return AWSELBPRE + region + AWSURLSUFF;
	}
	
	public static String getCWEndpoint() {
		return getCWEndpoint(getRegion());
	}
	
	public static String getCWEndpoint(String region) {
		if(region == null || region.isEmpty()) region = AwsInfo.getRegion();
		return AWSCWPRE + region + AWSURLSUFF;
	}
	
	public static String getAutoScaleEndpoint() {
		return getAutoScaleEndpoint(getRegion());
	}
	
	public static String getAutoScaleEndpoint(String region) {
		if(region == null || region.isEmpty()) region = AwsInfo.getRegion();
		return AWSASPRE + region + AWSURLSUFF;
	}
	
	public static String getSubnetID(){
		String macID = getEC2MetaInfo(MACADDRESS);
		return getEC2MetaInfo("network/interfaces/macs/"+ macID + "/subnet-id");
	}
	
	public static String getRegion(){
		if (storedLocalRegion == null) {
			try {
				storedLocalRegion = System.getenv(AwsInfo.ENV_REGION);
			} catch (Exception e) {
				// Ignore
			}
		}
		if (storedLocalRegion == null || storedLocalRegion.isEmpty()) {
			// This is not the ideal way to obtain the region, since AWS could change the availability zone format.
			// However, this is the best we've got for now if not set in the environment.
			try {
				String currentZone = getEC2MetaInfo(AwsInfo.AVAILABILITYZONE);
				List<Region> regions = new AmazonEC2Client().describeRegions().getRegions();
				
				for (Region region : regions) {
					if(currentZone.contains(region.getRegionName()))
						storedLocalRegion = region.getRegionName();
				}
			} catch (Exception e) {
				throw new IllegalStateException(AwsInfo.ENV_REGION + " not set in environment.");
			}
		}
		return storedLocalRegion;
	}
	
	public static List<String> getAllRegions() {
		List<String> strRegions = new ArrayList<String>();
		try{
			List<Region> regions = new AmazonEC2Client().describeRegions().getRegions();
			for(Region region : regions){
				strRegions.add(region.getRegionName());
			}
		}catch(Exception e){
			strRegions.addAll(storedRegionList);
		}
		storedRegionList.addAll(strRegions);
		return strRegions;
	}
	
	public static String getTagValue(String tagName){
		String tagValue = null;
		AmazonEC2Client client = new AmazonEC2Client();
		
		client.setEndpoint(AwsInfo.getEC2Endpoint());
		try{
		List<Reservation> reservations = client.describeInstances(new DescribeInstancesRequest().withInstanceIds(AwsInfo.getEC2MetaInfo(AwsInfo.INSTANCEID))).getReservations();
			if(reservations.size() > 0){
				Instance instance = reservations.get(0).getInstances().get(0);
				List<Tag> tags = instance.getTags();
				localKeys = (HashMap<String, String>) convertTagsToMap(tags);
				tagValue = localKeys.get(tagName);
			}
		}catch(Exception e){
			tagValue = localKeys.get(tagName);
		}

		return tagValue;
	}
	
	public static String getTagValue(String tagName, String instanceID, String region){
		String tagValue = null;
		AmazonEC2Client client = new AmazonEC2Client();
		if(instanceID == null || instanceID == "") instanceID = AwsInfo.getEC2MetaInfo(AwsInfo.INSTANCEID);
		client.setEndpoint(AwsInfo.getEC2Endpoint(region));
		try{
			List<Reservation> reservations = client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceID)).getReservations();
			if(reservations.size() > 0){
				Instance instance = reservations.get(0).getInstances().get(0);
				List<Tag> tags = instance.getTags();
				for(Tag tag : tags){
					if(tag.getKey().equals(tagName)) tagValue = tag.getValue();
				}
			}
		}catch(Exception e){
			//not available right now. Return null.
		}
		
		return tagValue;
	}
	
	public static Map<String,String> convertTagsToMap(List<Tag> tags){
		Map<String,String>tagMap = new HashMap<String,String>();
		if(tags == null) return tagMap;
		for(Tag tag : tags){
			tagMap.put(tag.getKey(), tag.getValue());
		}
		
		return tagMap;
	}
	
	public static String getEC2MetaInfo(String type) {
		String urlStr = META_ADDRESS + type;
        try {
            URL url = new URL( urlStr );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod( "GET" );

            conn.connect();
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
            String text = reader.readLine();

            conn.disconnect();
            return text;
        }
        catch ( IOException ex ) {
            return null;
        }
	}

	public enum EC2TYPE {
		MICRO, SMALL, MEDIUM, LARGE, EXTRALARGE, UNKNOWN;
		
		private static final ArrayList<String> micros = new ArrayList<String>();
		private static final ArrayList<String> smalls = new ArrayList<String>();
		private static final ArrayList<String> mediums = new ArrayList<String>();
		private static final ArrayList<String> larges = new ArrayList<String>();
		private static final ArrayList<String> xlarges = new ArrayList<String>();
		
		static {
			// These are classified here based on memory size primarily, and may not match the exact instance type from AWS.
			// 613MiB
			micros.add("t1.micro");
			// 1.7GiB
			smalls.add("m1.small");
			smalls.add("c1.medium");
			// 3.75GiB
			mediums.add("m1.medium");
			mediums.add("m3.medium");
			mediums.add("c3.large");
			// 7GiB
			larges.add("m1.large");
			larges.add("m3.large");
			larges.add("c1.xlarge");
			larges.add("c3.xlarge");
			// >15GiB
			xlarges.add("m1.xlarge");
			xlarges.add("m3.xlarge");
			xlarges.add("m3.2xlarge");
			xlarges.add("m2.xlarge");
			xlarges.add("m2.2xlarge");
			xlarges.add("m2.4xlarge");
			xlarges.add("c3.2xlarge");
			xlarges.add("c3.4xlarge");
			xlarges.add("c3.8xlarge");
		}
		
		public static EC2TYPE getEc2Type(String instanceType) {
			if(micros.contains(instanceType)) return MICRO;
			if(smalls.contains(instanceType)) return SMALL;
			if(larges.contains(instanceType)) return LARGE;
			if(mediums.contains(instanceType)) return MEDIUM;
			if(xlarges.contains(instanceType)) return EXTRALARGE;

			return UNKNOWN;
		}
	}
}
