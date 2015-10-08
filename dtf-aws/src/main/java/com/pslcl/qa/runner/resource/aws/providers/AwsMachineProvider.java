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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ProductCode;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.config.util.PropertiesFile;
import com.pslcl.qa.runner.config.util.StrH;
import com.pslcl.qa.runner.config.util.StrH.StringPair;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.MachineProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceStatusCallback;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.aws.AwsNames;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */
public class AwsMachineProvider extends ResourceProvider implements MachineProvider
{
    private final Map<String, AtomicInteger> limits;
    private final Map<String, InstanceInfo> instances;
    private final List<Filter> defaultImageFilters;
    private final List<String> defaultLocationFilters;
    private volatile String defaultInstanceType;
    private volatile String defaultLocationYear;
    private volatile String defaultLocationMonth;
    private volatile String defaultLocationDot;

    public AwsMachineProvider()
    {
        instances = new HashMap<String, InstanceInfo>();
        limits = new HashMap<String, AtomicInteger>();
        defaultImageFilters = new ArrayList<Filter>();
        defaultLocationFilters = new ArrayList<String>();
    }

    @Override
    public void init(RunnerServiceConfig config) throws Exception
    {
        super.init(config);
        config.initsb.ttl("AWS Default Instance:");
        config.initsb.level.incrementAndGet();
        defaultInstanceType = config.properties.getProperty(AwsNames.InstanceTypeKey, AwsNames.InstanceTypeDefault);
        config.initsb.ttl(AwsNames.InstanceTypeKey, " = ", defaultInstanceType);
        config.initsb.level.decrementAndGet();
        config.initsb.ttl("AWS Instance Type Limits:");
        config.initsb.level.incrementAndGet();
        for(int i=0; i < AwsNames.instanceTypes.length; i++)
        {
            String key = AwsNames.InstanceTypeKeyBase + "." + AwsNames.instanceTypes[i].toString() + AwsNames.InstanceTypeLimit;
            String value = config.properties.getProperty(key, "0");
            config.initsb.ttl(key," = ", value);
            int limit = Integer.parseInt(value);
            limits.put(AwsNames.instanceTypes[i].toString(), new AtomicInteger(limit));
        }
        config.initsb.level.decrementAndGet();
        config.initsb.ttl("AWS Default Image Filters:");
        config.initsb.level.incrementAndGet();
        addImageFilter(AwsNames.ImageArchitectureKey, AwsNames.ImageArchitectureDefault, AwsNames.ImageArchitectureFilter);
        addImageFilter(AwsNames.ImageHypervisorKey, AwsNames.ImageHypervisorDefault, AwsNames.ImageHypervisorFilter);
        addImageFilter(AwsNames.ImageImageTypeKey, AwsNames.ImageImageTypeDefault, AwsNames.ImageImageTypeFilter);
        addImageFilter(AwsNames.ImageIsPublicKey, AwsNames.ImageIsPublicDefault, AwsNames.ImageIsPublicFilter);
        addImageFilter(AwsNames.ImageNameKey, AwsNames.ImageNameDefault, AwsNames.ImageNameFilter);
        addImageFilter(AwsNames.ImageOwnerKey, AwsNames.ImageOwnerDefault, AwsNames.ImageOwnerFilter);
        addImageFilter(AwsNames.ImagePlatformKey, AwsNames.ImagePlatformDefault, AwsNames.ImagePlatformFilter);
        addImageFilter(AwsNames.ImageRootDevTypeKey, AwsNames.ImageRootDevTypeDefault, AwsNames.ImageRootDevTypeFilter);
        addImageFilter(AwsNames.ImageStateKey, AwsNames.ImageStateDefault, AwsNames.ImageStateFilter);
        addImageFilter(AwsNames.BlockingDeviceDeleteOnTerminationKey, AwsNames.BlockingDeviceDeleteOnTerminationDefault, AwsNames.BlockingDeviceDeleteOnTerminationFilter);
        addImageFilter(AwsNames.BlockingDeviceVolumeTypeKey, AwsNames.BlockingDeviceVolumeTypeDefault, AwsNames.BlockingDeviceVolumeTypeFilter);
        addImageFilter(AwsNames.BlockingDeviceVolumeSizeKey, AwsNames.BlockingDeviceVolumeSizeDefault, AwsNames.BlockingDeviceVolumeSizeFilter);
        config.initsb.level.decrementAndGet();
        
        config.initsb.ttl("AWS Image Location Filters:");
        config.initsb.level.incrementAndGet();
        defaultLocationYear = config.properties.getProperty(AwsNames.LocationYearKey, AwsNames.LocationYearDefault);
        config.initsb.ttl(AwsNames.LocationYearKey, " = ", defaultLocationYear);
        defaultLocationMonth = config.properties.getProperty(AwsNames.LocationMonthKey, AwsNames.LocationMonthDefault);
        config.initsb.ttl(AwsNames.LocationMonthKey, " = ", defaultLocationMonth);
        defaultLocationDot = config.properties.getProperty(AwsNames.LocationDotKey, AwsNames.LocationDotDefault);
        config.initsb.ttl(AwsNames.LocationDotKey, " = ", defaultLocationDot);
        
        addLocationFilters();
        config.initsb.level.decrementAndGet();
    }        

    private void addImageFilter(String key, String defaultValue, String filterName)
    {
        String value = config.properties.getProperty(key, defaultValue);
        config.initsb.ttl(key, " = ", value);
        if(value == null)
            return;
        List<String> args = new ArrayList<String>();
        args.add(value);
        Filter filter = new Filter(filterName, args);
        defaultImageFilters.add(filter);
    }
    
    private void addLocationFilters()
    {
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(AwsNames.LocationFeatureKey, config.properties);
        int size = list.size();
        if(size == 0)
        {
            // add the default
            StringPair pair = new StringPair(AwsNames.LocationFeatureKey+"0", AwsNames.LocationBase);
            list.add(pair);
            pair = new StringPair(AwsNames.LocationFeatureKey+"1", AwsNames.LocationHvm);
            list.add(pair);
            size += 2;
        }
        for(int i=0; i < size; i++)
        {
            Entry<String,String> entry = list.get(i);
            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
            defaultLocationFilters.add(entry.getValue());
        }
    }
    
    @Override
    public void destroy()
    {
        super.destroy();
    }

    // implement MachineProvider interface

    @Override
    public Future<MachineInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException
    {

        return config.blockingExecutor.submit(new AwsMachineInstanceFuture(resource));
        
//        RunInstancesRequest runInstancesRequest = 
//                        new RunInstancesRequest();
//                              
//                    runInstancesRequest.withImageId("ami-4b814f22")
//                                       .withInstanceType("m1.small")
//                                       .withMinCount(1)
//                                       .withMaxCount(1)
//                                       .withKeyName("my-key-pair")
//                                       .withSecurityGroups("my-security-group");        
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // implement ResourceProvider interface

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException
    {
        // first check for invalid parameters
        Map<String, String> attrs = resource.getAttributes();
//        AwsNames.InstanceType[] types = AwsNames.InstanceType.values();
        String type = attrs.get(AwsNames.InstanceTypeKey);
        if(type == null)
            type = defaultInstanceType;
        boolean found = false;
        for(int i=0; i < AwsNames.instanceTypes.length; i++)
        {
            if(AwsNames.instanceTypes[i].toString().equals(type))
            {
                found = true;
                break;
            }
        }
        if(!found)
            throw new ResourceNotFoundException(AwsNames.InstanceTypeKey + "=" + type + " is not a valid AWS instance type");
        
        String amiId = attrs.get(AwsNames.InstanceAmiIdKey);
//        if(amiId == null)
//            amiId = AwsNames.AwsAmiIdDefault;
        try
        {
            AwsNames.logInstanceTypes(log);
            AwsNames.logImageAttributeNames(log);
//            DescribeImagesRequest request = new DescribeImagesRequest().withImageIds(amiId);
//            ImageAttributeName ian;
            DescribeImagesRequest request = new DescribeImagesRequest(); //.withOwners("amazon");
//            DescribeImagesRequest request = new DescribeImagesRequest();
//            List<Filter> filters = new ArrayList<Filter>();
//            List<String> args = new ArrayList<String>();
//            args.add("i386");
//            Filter filter = new Filter("architecture", args);
//            filters.add(filter);
            request.setFilters(defaultImageFilters);
            DescribeImagesResult result = ec2Client.describeImages(request);
            
StringBuilder sb = new StringBuilder();
            HashMap<String, Image> uniqueKernelIds = new HashMap<String, Image>();
            HashMap<String, Image> uniquePlatforms = new HashMap<String, Image>();
            HashMap<Integer, Image> uniqueSize = new HashMap<Integer, Image>();
            HashMap<String, Image> uniqueProducts = new HashMap<String, Image>();
            HashMap<String, Image> uniqueLocations = new HashMap<String, Image>();
            HashMap<String, String> uniqueFeatures = new HashMap<String, String>();
            java.util.List<Image> images = result.getImages();
            for(Image image : images)
            {
                // see MAx@AWS's post at 
                // https://forums.aws.amazon.com/thread.jspa?messageID=376434
                // towards the bottom: 
                // "The Amazon Linux AMI strives for a major release each March and September, 
                // which is why you'll see our AMIs adopting a 2013.03, 2013.09, etc. naming system.
                // dot releases off those 6 month releases look like this: 2013.09.1 and 2013.09.2."
                // example: 
                // amazon/aws-elasticbeanstalk-amzn-2014.09.1.x86_64-python26-pv-201410222339
                
                String loc = image.getImageLocation();
                loc = StrH.getAtomicName(loc, '/');
                String[] frags = loc.split("\\.");
                if(frags.length > 3)
                {
                    int index = frags[0].lastIndexOf('-');
                    if(index != -1)
                    {
                        String locHeader = frags[0].substring(0, index);
                        String year = frags[0].substring(index+1);
                        String month = frags[1];
                        String dotRelease = frags[2];
                        String description = frags[3];
                        frags = description.split("-");
                        sb.append("*** year: " + year + " month: " + month + " dot: " + dotRelease + " desc: " + description + "\n");
                        int size = defaultLocationFilters.size();
                        boolean[] fragsFound = new boolean[size];
                        for(int i = 1; i < frags.length; i++)
                        {
                            if(frags[i].startsWith("20"))
                                break;
                            uniqueFeatures.put(frags[i], frags[i]);
                            for(int j=0; j < size; j++)
                            {
                                String filter = defaultLocationFilters.get(j);
                                if(filter.equals(frags[i]))
                                {
                                    fragsFound[j] = true;
                                    break;
                                }
                            }
                        }
                        found = true;
                        for(int i=0; i < size; i++)
                        {
                            if(!fragsFound[i])
                            {
                                found = false;
                                break;
                            }
                        }
                        if(found)
                            uniqueLocations.put(loc, image);
                    }
                }
                java.util.List<ProductCode> products = image.getProductCodes();
                for(ProductCode product : products)
                {
                    String id = product.getProductCodeId();
                    String ptype = product.getProductCodeType();
                    String key = null;
                    if(id != null)
                        key = id;
                    if(ptype != null)
                        key += ptype;
                    if(key != null)
                        uniqueProducts.put(key, image);
                }
                java.util.List<BlockDeviceMapping> blockDeviceMap = image.getBlockDeviceMappings();
                for(BlockDeviceMapping bdmap : blockDeviceMap)
                {
                    EbsBlockDevice bd = bdmap.getEbs();
                    if(bd != null)
                        uniqueSize.put(bd.getVolumeSize(), image);
                }
                String platform = image.getPlatform();
                uniquePlatforms.put(image.getPlatform(), image);
                uniqueKernelIds.put(image.getKernelId(), image);
                log.info(image.toString());
                sb.append(image.getImageId() + " - " + image.getName() + "\n");
            }
            sb.append("Kernel Ids: \n");
for(Entry<String,Image> entry : uniqueKernelIds.entrySet())
    sb.append(entry.getKey() + "\n\t" + entry.getValue().toString() + "\n");
sb.append("Platforms: \n");
for(Entry<String,Image> entry : uniquePlatforms.entrySet())
    sb.append(entry.getKey() + "\n\t" + entry.getValue().toString() + "\n");
for(Entry<Integer,Image> entry : uniqueSize.entrySet())
    sb.append(entry.getKey() + "\n\t" + entry.getValue().toString() + "\n");
for(Entry<String,Image> entry : uniqueProducts.entrySet())
    sb.append(entry.getKey() + "\n\t" + entry.getValue().toString() + "\n");
for(Entry<String,Image> entry : uniqueLocations.entrySet())
    sb.append(entry.getKey() + "\n\t" + entry.getValue().toString() + "\n");
for(Entry<String,String> entry : uniqueFeatures.entrySet())
    sb.append(entry.getKey() + "\n");
sb.append("\timages size: " + images.size() + "\n");
sb.append("\tkernels size: " + uniqueKernelIds.size() + "\n");
sb.append("\tplatforms size: " + uniquePlatforms.size() + "\n");
sb.append("\tdisk size: " + uniqueSize.size() + "\n");
sb.append("\tproducts size: " + uniqueProducts.size() + "\n");
sb.append("\tlocations size: " + uniqueLocations.size() + "\n");
sb.append("\tfeatures size: " + uniqueFeatures.size() + "\n");
log.info(sb.toString());
log.info("look here");
        }catch(Exception e)
        {
            log.warn(getClass().getSimpleName() + ".isAvailable: ec2 client exception", e);
            throw new ResourceNotFoundException("ec2 client exception", e);
        }
        // check for limits
        int avail = limits.get(type).get();
        if(avail < 1)
            return false;
        return true;
    }
    
//    One or more filters.
//    architecture - The image architecture (i386 | x86_64).
//    block-device-mapping.delete-on-termination - A Boolean value that indicates whether the Amazon EBS volume is deleted on instance termination.
//    block-device-mapping.device-name - The device name for the EBS volume (for example, /dev/sdh).
//    block-device-mapping.snapshot-id - The ID of the snapshot used for the EBS volume.
//    block-device-mapping.volume-size - The volume size of the EBS volume, in GiB.
//    block-device-mapping.volume-type - The volume type of the EBS volume (gp2 | standard | io1).
//    description - The description of the image (provided during image creation).
//    hypervisor - The hypervisor type (ovm | xen).
//    image-id - The ID of the image.
//    image-type - The image type (machine | kernel | ramdisk).
//    is-public - A Boolean that indicates whether the image is public.
//    kernel-id - The kernel ID.
//    manifest-location - The location of the image manifest.
//    name - The name of the AMI (provided during image creation).
//    owner-alias - The AWS account alias (for example, amazon).
//    owner-id - The AWS account ID of the image owner.
//    platform - The platform. To only list Windows-based AMIs, use windows.
//    product-code - The product code.
//    product-code.type - The type of the product code (devpay | marketplace).
//    ramdisk-id - The RAM disk ID.
//    root-device-name - The name of the root device volume (for example, /dev/sda1).
//    root-device-type - The type of the root device volume (ebs | instance-store).
//    state - The state of the image (available | pending | failed).
//    state-reason-code - The reason code for the state change.
//    state-reason-message - The message for the state change.
//    tag:key=value - The key/value combination of a tag assigned to the resource.
//    tag-key - The key of a tag assigned to the resource. This filter is independent of the tag-value filter. For example, if you use both the filter "tag-key=Purpose" and the filter "tag-value=X", you get any resources assigned both the tag key Purpose (regardless of what the tag's value is), and the tag value X (regardless of what the tag's key is). If you want to list only resources where Purpose is X, see the tag:key=value filter.
//    tag-value - The value of a tag assigned to the resource. This filter is independent of the tag-key filter.
//    virtualization-type - The virtualization type (paravirtual | hvm).
    
    
    
    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources)
    {
        List<ReservedResourceWithAttributes> reservedResources = new ArrayList<ReservedResourceWithAttributes>(); 
        List<ResourceWithAttributes> availableResources = new ArrayList<ResourceWithAttributes>();
        List<ResourceWithAttributes> unavailableResources = new ArrayList<ResourceWithAttributes>();
        List<ResourceWithAttributes> invalidResources = new ArrayList<ResourceWithAttributes>();
        ResourceQueryResult resourceQueryResult = new ResourceQueryResult(reservedResources, availableResources, unavailableResources, invalidResources);
        for (ResourceWithAttributes resource : resources)
        {
            try
            {
                if (isAvailable(resource))
                    availableResources.add(resource);
                else
                    unavailableResources.add(resource);
            }
            catch (Exception e)
            {
                invalidResources.add(resource);
                log.debug(getClass().getSimpleName() + ".queryResourceAvailable failed", e);
            }
        }
        return resourceQueryResult;
    }

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds)
    {
        ResourceQueryResult queryResult = queryResourceAvailability(resources);
        
        List<ReservedResourceWithAttributes> reservedResources = new ArrayList<ReservedResourceWithAttributes>(); 
        List<ResourceWithAttributes> availableResources = new ArrayList<ResourceWithAttributes>();
        ResourceQueryResult result = new ResourceQueryResult(reservedResources, availableResources, queryResult.getUnavailableResources(), queryResult.getInvalidResources());
        
        for(ResourceWithAttributes requested : resources)
        {
            for(ResourceWithAttributes avail : availableResources)
            {
                if(avail.getName().equals(requested.getName()))
                    reservedResources.add(new ReservedResourceWithAttributes(avail, this, timeoutSeconds));
            }
        }
        return result;
    }

    @Override
    public List<String> getNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getAttributes(String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // implement ArtifactConsumer interface

    @Override
    public void updateArtifact(String component, String version, String platform, String name, String artifactHash)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeArtifact(String component, String version, String platform, String name)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void invalidateArtifacts(String component, String version)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void release(ResourceInstance resource, boolean isReusable)
    {
        // TODO Auto-generated method stub

    }
    
    private class InstanceInfo
    {
        public final String type;
        public final int limit;
        
        private InstanceInfo(String type, int limit)
        {
            this.type = type;
            this.limit = limit;
        }
    }
}

//public DescribeInstanceAttributeResult describeInstanceAttribute(DescribeInstanceAttributeRequest describeInstanceAttributeRequest) 
//public void rebootInstances(RebootInstancesRequest rebootInstancesRequest) 
//public RunInstancesResult runInstances(RunInstancesRequest runInstancesRequest) 

//public DescribeReservedInstancesResult describeReservedInstances(DescribeReservedInstancesRequest describeReservedInstancesRequest) 
//public AttachVpnGatewayResult attachVpnGateway(AttachVpnGatewayRequest attachVpnGatewayRequest) 

//public CreateImageResult createImage(CreateImageRequest createImageRequest) 
//public void deleteSecurityGroup(DeleteSecurityGroupRequest deleteSecurityGroupRequest) 
//public void associateDhcpOptions(AssociateDhcpOptionsRequest associateDhcpOptionsRequest) 
//public StopInstancesResult stopInstances(StopInstancesRequest stopInstancesRequest) 
//public void deleteNetworkInterface(DeleteNetworkInterfaceRequest deleteNetworkInterfaceRequest) 
//public CreateSecurityGroupResult createSecurityGroup(CreateSecurityGroupRequest createSecurityGroupRequest) 
//public DescribeNetworkInterfacesResult describeNetworkInterfaces(DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest) 
//public CreateReservedInstancesListingResult createReservedInstancesListing(CreateReservedInstancesListingRequest createReservedInstancesListingRequest) 
//public void deleteRoute(DeleteRouteRequest deleteRouteRequest) 
//public DescribeSecurityGroupsResult describeSecurityGroups(DescribeSecurityGroupsRequest describeSecurityGroupsRequest) 
//public void deleteSubnet(DeleteSubnetRequest deleteSubnetRequest) 
//public CreateNetworkInterfaceResult createNetworkInterface(CreateNetworkInterfaceRequest createNetworkInterfaceRequest) 
//public ModifyReservedInstancesResult modifyReservedInstances(ModifyReservedInstancesRequest modifyReservedInstancesRequest) 
//public CreateRouteTableResult createRouteTable(CreateRouteTableRequest createRouteTableRequest) 
//public DescribeReservedInstancesListingsResult describeReservedInstancesListings(DescribeReservedInstancesListingsRequest describeReservedInstancesListingsRequest) 
//public ImportInstanceResult importInstance(ImportInstanceRequest importInstanceRequest) 
//public GetConsoleOutputResult getConsoleOutput(GetConsoleOutputRequest getConsoleOutputRequest) 
//public DescribeSubnetsResult describeSubnets(DescribeSubnetsRequest describeSubnetsRequest) 
//public AssociateRouteTableResult associateRouteTable(AssociateRouteTableRequest associateRouteTableRequest) 
//public DescribeInstancesResult describeInstances(DescribeInstancesRequest describeInstancesRequest) 
//public StartInstancesResult startInstances(StartInstancesRequest startInstancesRequest) 
//public CancelReservedInstancesListingResult cancelReservedInstancesListing(CancelReservedInstancesListingRequest cancelReservedInstancesListingRequest) 
//public void createRoute(CreateRouteRequest createRouteRequest) 
//public CopyImageResult copyImage(CopyImageRequest copyImageRequest) 
//public void deleteRouteTable(DeleteRouteTableRequest deleteRouteTableRequest) 
//public DescribeInstanceStatusResult describeInstanceStatus(DescribeInstanceStatusRequest describeInstanceStatusRequest) 
//public CreateSubnetResult createSubnet(CreateSubnetRequest createSubnetRequest) 
//public DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings(DescribeReservedInstancesOfferingsRequest describeReservedInstancesOfferingsRequest) 
//public DescribeReservedInstancesResult describeReservedInstances() throws AmazonServiceException, AmazonClientException;
//public DescribeAvailabilityZonesResult describeAvailabilityZones() throws AmazonServiceException, AmazonClientException;
//public DescribeSpotPriceHistoryResult describeSpotPriceHistory() throws AmazonServiceException, AmazonClientException;
//public DescribeNetworkInterfacesResult describeNetworkInterfaces() throws AmazonServiceException, AmazonClientException;


//    public void setEndpoint(String endpoint) throws java.lang.IllegalArgumentException;
//    public void setRegion(com.amazonaws.regions.Region region) throws java.lang.IllegalArgumentException;
//    public DescribeAvailabilityZonesResult describeAvailabilityZones(DescribeAvailabilityZonesRequest describeAvailabilityZonesRequest) 
//    public DetachVolumeResult detachVolume(DetachVolumeRequest detachVolumeRequest) 
//    public void deleteKeyPair(DeleteKeyPairRequest deleteKeyPairRequest) 
//    public UnmonitorInstancesResult unmonitorInstances(UnmonitorInstancesRequest unmonitorInstancesRequest) 
//    public CreateInstanceExportTaskResult createInstanceExportTask(CreateInstanceExportTaskRequest createInstanceExportTaskRequest) 
//    public GetPasswordDataResult getPasswordData(GetPasswordDataRequest getPasswordDataRequest) 
//    public void authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgressRequest authorizeSecurityGroupEgressRequest) 
//    public ImportKeyPairResult importKeyPair(ImportKeyPairRequest importKeyPairRequest) 
//    public void modifyVpcAttribute(ModifyVpcAttributeRequest modifyVpcAttributeRequest) 
//    public DescribeSpotPriceHistoryResult describeSpotPriceHistory(DescribeSpotPriceHistoryRequest describeSpotPriceHistoryRequest) 
//    public DescribeRegionsResult describeRegions(DescribeRegionsRequest describeRegionsRequest) 
//    public CreateDhcpOptionsResult createDhcpOptions(CreateDhcpOptionsRequest createDhcpOptionsRequest) 
//    public void resetSnapshotAttribute(ResetSnapshotAttributeRequest resetSnapshotAttributeRequest) 
//    public DescribeInternetGatewaysResult describeInternetGateways(DescribeInternetGatewaysRequest describeInternetGatewaysRequest) 
//    public ImportVolumeResult importVolume(ImportVolumeRequest importVolumeRequest) 
//    public RejectVpcPeeringConnectionResult rejectVpcPeeringConnection(RejectVpcPeeringConnectionRequest rejectVpcPeeringConnectionRequest) 
//    public void detachVpnGateway(DetachVpnGatewayRequest detachVpnGatewayRequest) 
//    public void deregisterImage(DeregisterImageRequest deregisterImageRequest) 
//    public DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscriptionRequest describeSpotDatafeedSubscriptionRequest) 
//    public void deleteTags(DeleteTagsRequest deleteTagsRequest)
//    public DescribeAccountAttributesResult describeAccountAttributes(DescribeAccountAttributesRequest describeAccountAttributesRequest) 
//    public CreateVpnGatewayResult createVpnGateway(CreateVpnGatewayRequest createVpnGatewayRequest) 
//    public void enableVolumeIO(EnableVolumeIORequest enableVolumeIORequest) 
//    public void deleteVpnGateway(DeleteVpnGatewayRequest deleteVpnGatewayRequest) 
//    public AttachVolumeResult attachVolume(AttachVolumeRequest attachVolumeRequest) 
//    public DescribeVolumeStatusResult describeVolumeStatus(DescribeVolumeStatusRequest describeVolumeStatusRequest) 
//    public void resetImageAttribute(ResetImageAttributeRequest resetImageAttributeRequest) 
//    public DescribeVpnConnectionsResult describeVpnConnections(DescribeVpnConnectionsRequest describeVpnConnectionsRequest) 
//    public void enableVgwRoutePropagation(EnableVgwRoutePropagationRequest enableVgwRoutePropagationRequest) 
//    public CreateSnapshotResult createSnapshot(CreateSnapshotRequest createSnapshotRequest) 
//    public void deleteVolume(DeleteVolumeRequest deleteVolumeRequest)
//    public DescribeVpcsResult describeVpcs(DescribeVpcsRequest describeVpcsRequest) 
//    public void unassignPrivateIpAddresses(UnassignPrivateIpAddressesRequest unassignPrivateIpAddressesRequest) 
//    public void cancelConversionTask(CancelConversionTaskRequest cancelConversionTaskRequest) 
//    public AssociateAddressResult associateAddress(AssociateAddressRequest associateAddressRequest) 
//    public void deleteCustomerGateway(DeleteCustomerGatewayRequest deleteCustomerGatewayRequest) 
//    public void createNetworkAclEntry(CreateNetworkAclEntryRequest createNetworkAclEntryRequest) 
//    public AcceptVpcPeeringConnectionResult acceptVpcPeeringConnection(AcceptVpcPeeringConnectionRequest acceptVpcPeeringConnectionRequest) 
//    public DescribeExportTasksResult describeExportTasks(DescribeExportTasksRequest describeExportTasksRequest) 
//    public void detachInternetGateway(DetachInternetGatewayRequest detachInternetGatewayRequest) 
//    public CreateVpcPeeringConnectionResult createVpcPeeringConnection(CreateVpcPeeringConnectionRequest createVpcPeeringConnectionRequest) 
//    public DescribeVolumesResult describeVolumes(DescribeVolumesRequest describeVolumesRequest) 
//    public void reportInstanceStatus(ReportInstanceStatusRequest reportInstanceStatusRequest) 
//    public DescribeRouteTablesResult describeRouteTables(DescribeRouteTablesRequest describeRouteTablesRequest) 
//    public DescribeDhcpOptionsResult describeDhcpOptions(DescribeDhcpOptionsRequest describeDhcpOptionsRequest) 
//    public MonitorInstancesResult monitorInstances(MonitorInstancesRequest monitorInstancesRequest) 
//    public DescribeNetworkAclsResult describeNetworkAcls(DescribeNetworkAclsRequest describeNetworkAclsRequest) 
//    public DescribeBundleTasksResult describeBundleTasks(DescribeBundleTasksRequest describeBundleTasksRequest)
//    public void revokeSecurityGroupIngress(RevokeSecurityGroupIngressRequest revokeSecurityGroupIngressRequest) 
//    public DeleteVpcPeeringConnectionResult deleteVpcPeeringConnection(DeleteVpcPeeringConnectionRequest deleteVpcPeeringConnectionRequest) 
//    public CreateInternetGatewayResult createInternetGateway(CreateInternetGatewayRequest createInternetGatewayRequest) 
//    public void deleteVpnConnectionRoute(DeleteVpnConnectionRouteRequest deleteVpnConnectionRouteRequest) 
//    public void detachNetworkInterface(DetachNetworkInterfaceRequest detachNetworkInterfaceRequest) 
//    public void modifyImageAttribute(ModifyImageAttributeRequest modifyImageAttributeRequest) 
//    public CreateCustomerGatewayResult createCustomerGateway(CreateCustomerGatewayRequest createCustomerGatewayRequest) 
//    public CreateSpotDatafeedSubscriptionResult createSpotDatafeedSubscription(CreateSpotDatafeedSubscriptionRequest createSpotDatafeedSubscriptionRequest) 
//    public void attachInternetGateway(AttachInternetGatewayRequest attachInternetGatewayRequest) 
//    public void deleteVpnConnection(DeleteVpnConnectionRequest deleteVpnConnectionRequest) 
//    public DescribeConversionTasksResult describeConversionTasks(DescribeConversionTasksRequest describeConversionTasksRequest) 
//    public CreateVpnConnectionResult createVpnConnection(CreateVpnConnectionRequest createVpnConnectionRequest)
//    public DescribeVpcPeeringConnectionsResult describeVpcPeeringConnections(DescribeVpcPeeringConnectionsRequest describeVpcPeeringConnectionsRequest) 
//    public DescribePlacementGroupsResult describePlacementGroups(DescribePlacementGroupsRequest describePlacementGroupsRequest) 
//    public void deleteNetworkAcl(DeleteNetworkAclRequest deleteNetworkAclRequest) 
//    public void modifyVolumeAttribute(ModifyVolumeAttributeRequest modifyVolumeAttributeRequest) 
//    public DescribeImagesResult describeImages(DescribeImagesRequest describeImagesRequest)
//    public void modifyInstanceAttribute(ModifyInstanceAttributeRequest modifyInstanceAttributeRequest) 
//    public void deleteDhcpOptions(DeleteDhcpOptionsRequest deleteDhcpOptionsRequest) 
//    public void authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest) 
//    public DescribeSpotInstanceRequestsResult describeSpotInstanceRequests(DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest) 
//    public CreateVpcResult createVpc(CreateVpcRequest createVpcRequest) 
//    public DescribeCustomerGatewaysResult describeCustomerGateways(DescribeCustomerGatewaysRequest describeCustomerGatewaysRequest) 
//    public void cancelExportTask(CancelExportTaskRequest cancelExportTaskRequest) 
//    public void modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttributeRequest modifyNetworkInterfaceAttributeRequest)
//    public DescribeNetworkInterfaceAttributeResult describeNetworkInterfaceAttribute(DescribeNetworkInterfaceAttributeRequest describeNetworkInterfaceAttributeRequest) 
//    public RequestSpotInstancesResult requestSpotInstances(RequestSpotInstancesRequest requestSpotInstancesRequest) 
//    public void createTags(CreateTagsRequest createTagsRequest) 
//    public DescribeVolumeAttributeResult describeVolumeAttribute(DescribeVolumeAttributeRequest describeVolumeAttributeRequest) 
//    public AttachNetworkInterfaceResult attachNetworkInterface(AttachNetworkInterfaceRequest attachNetworkInterfaceRequest) 
//    public void replaceRoute(ReplaceRouteRequest replaceRouteRequest) 
//    public DescribeTagsResult describeTags(DescribeTagsRequest describeTagsRequest) 
//    public CancelBundleTaskResult cancelBundleTask(CancelBundleTaskRequest cancelBundleTaskRequest) 
//    public void disableVgwRoutePropagation(DisableVgwRoutePropagationRequest disableVgwRoutePropagationRequest) 
//    public CancelSpotInstanceRequestsResult cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest) 
//    public PurchaseReservedInstancesOfferingResult purchaseReservedInstancesOffering(PurchaseReservedInstancesOfferingRequest purchaseReservedInstancesOfferingRequest) 
//    public void modifySnapshotAttribute(ModifySnapshotAttributeRequest modifySnapshotAttributeRequest) 
//    public DescribeReservedInstancesModificationsResult describeReservedInstancesModifications(DescribeReservedInstancesModificationsRequest describeReservedInstancesModificationsRequest) 
//    public TerminateInstancesResult terminateInstances(TerminateInstancesRequest terminateInstancesRequest) 
//    public void deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscriptionRequest deleteSpotDatafeedSubscriptionRequest) 
//    public void deleteInternetGateway(DeleteInternetGatewayRequest deleteInternetGatewayRequest) 
//    public DescribeSnapshotAttributeResult describeSnapshotAttribute(DescribeSnapshotAttributeRequest describeSnapshotAttributeRequest) 
//    public ReplaceRouteTableAssociationResult replaceRouteTableAssociation(ReplaceRouteTableAssociationRequest replaceRouteTableAssociationRequest) 
//    public DescribeAddressesResult describeAddresses(DescribeAddressesRequest describeAddressesRequest) 
//    public DescribeImageAttributeResult describeImageAttribute(DescribeImageAttributeRequest describeImageAttributeRequest) 
//    public DescribeKeyPairsResult describeKeyPairs(DescribeKeyPairsRequest describeKeyPairsRequest) 
//    public ConfirmProductInstanceResult confirmProductInstance(ConfirmProductInstanceRequest confirmProductInstanceRequest) 
//    public void disassociateRouteTable(DisassociateRouteTableRequest disassociateRouteTableRequest) 
//    public DescribeVpcAttributeResult describeVpcAttribute(DescribeVpcAttributeRequest describeVpcAttributeRequest) 
//    public void revokeSecurityGroupEgress(RevokeSecurityGroupEgressRequest revokeSecurityGroupEgressRequest) 
//    public void deleteNetworkAclEntry(DeleteNetworkAclEntryRequest deleteNetworkAclEntryRequest) 
//    public CreateVolumeResult createVolume(CreateVolumeRequest createVolumeRequest)
//    public DescribeVpnGatewaysResult describeVpnGateways(DescribeVpnGatewaysRequest describeVpnGatewaysRequest) 
//    public void assignPrivateIpAddresses(AssignPrivateIpAddressesRequest assignPrivateIpAddressesRequest) 
//    public void deleteSnapshot(DeleteSnapshotRequest deleteSnapshotRequest) 
//    public ReplaceNetworkAclAssociationResult replaceNetworkAclAssociation(ReplaceNetworkAclAssociationRequest replaceNetworkAclAssociationRequest) 
//    public void disassociateAddress(DisassociateAddressRequest disassociateAddressRequest) 
//    public void createPlacementGroup(CreatePlacementGroupRequest createPlacementGroupRequest) 
//    public BundleInstanceResult bundleInstance(BundleInstanceRequest bundleInstanceRequest) 
//    public void deletePlacementGroup(DeletePlacementGroupRequest deletePlacementGroupRequest) 
//    public void modifySubnetAttribute(ModifySubnetAttributeRequest modifySubnetAttributeRequest) 
//    public void deleteVpc(DeleteVpcRequest deleteVpcRequest) 
//    public CopySnapshotResult copySnapshot(CopySnapshotRequest copySnapshotRequest) 
//    public AllocateAddressResult allocateAddress(AllocateAddressRequest allocateAddressRequest) 
//    public void releaseAddress(ReleaseAddressRequest releaseAddressRequest) 
//    public void resetInstanceAttribute(ResetInstanceAttributeRequest resetInstanceAttributeRequest) 
//    public CreateKeyPairResult createKeyPair(CreateKeyPairRequest createKeyPairRequest) 
//    public void replaceNetworkAclEntry(ReplaceNetworkAclEntryRequest replaceNetworkAclEntryRequest) 
//    public DescribeSnapshotsResult describeSnapshots(DescribeSnapshotsRequest describeSnapshotsRequest) 
//    public CreateNetworkAclResult createNetworkAcl(CreateNetworkAclRequest createNetworkAclRequest) 
//    public RegisterImageResult registerImage(RegisterImageRequest registerImageRequest) 
//    public void resetNetworkInterfaceAttribute(ResetNetworkInterfaceAttributeRequest resetNetworkInterfaceAttributeRequest) 
//    public void createVpnConnectionRoute(CreateVpnConnectionRouteRequest createVpnConnectionRouteRequest)

//    public DescribeRegionsResult describeRegions() throws AmazonServiceException, AmazonClientException;
//    public DescribeInternetGatewaysResult describeInternetGateways() throws AmazonServiceException, AmazonClientException;
//    public DescribeSecurityGroupsResult describeSecurityGroups() throws AmazonServiceException, AmazonClientException;
//    public DescribeSpotDatafeedSubscriptionResult describeSpotDatafeedSubscription() throws AmazonServiceException, AmazonClientException;
//    public DescribeAccountAttributesResult describeAccountAttributes() throws AmazonServiceException, AmazonClientException;
//    public DescribeVolumeStatusResult describeVolumeStatus() throws AmazonServiceException, AmazonClientException;
//    public DescribeVpnConnectionsResult describeVpnConnections() throws AmazonServiceException, AmazonClientException;
//    public DescribeVpcsResult describeVpcs() throws AmazonServiceException, AmazonClientException;
//    public AcceptVpcPeeringConnectionResult acceptVpcPeeringConnection() throws AmazonServiceException, AmazonClientException;
//    public DescribeExportTasksResult describeExportTasks() throws AmazonServiceException, AmazonClientException;
//    public CreateVpcPeeringConnectionResult createVpcPeeringConnection() throws AmazonServiceException, AmazonClientException;
//    public DescribeVolumesResult describeVolumes() throws AmazonServiceException, AmazonClientException;
//    public DescribeReservedInstancesListingsResult describeReservedInstancesListings() throws AmazonServiceException, AmazonClientException;
//    public DescribeRouteTablesResult describeRouteTables() throws AmazonServiceException, AmazonClientException;
//    public DescribeDhcpOptionsResult describeDhcpOptions() throws AmazonServiceException, AmazonClientException;
//    public DescribeNetworkAclsResult describeNetworkAcls() throws AmazonServiceException, AmazonClientException;
//    public DescribeBundleTasksResult describeBundleTasks() throws AmazonServiceException, AmazonClientException;
//    public void revokeSecurityGroupIngress() throws AmazonServiceException, AmazonClientException;
//    public CreateInternetGatewayResult createInternetGateway() throws AmazonServiceException, AmazonClientException;
//    public DescribeConversionTasksResult describeConversionTasks() throws AmazonServiceException, AmazonClientException;
//    public DescribeVpcPeeringConnectionsResult describeVpcPeeringConnections() throws AmazonServiceException, AmazonClientException;
//    public DescribeSubnetsResult describeSubnets() throws AmazonServiceException, AmazonClientException;
//    public DescribePlacementGroupsResult describePlacementGroups() throws AmazonServiceException, AmazonClientException;
//    public DescribeInstancesResult describeInstances() throws AmazonServiceException, AmazonClientException;
//    public DescribeImagesResult describeImages() throws AmazonServiceException, AmazonClientException;
//    public DescribeSpotInstanceRequestsResult describeSpotInstanceRequests() throws AmazonServiceException, AmazonClientException;
//    public DescribeCustomerGatewaysResult describeCustomerGateways() throws AmazonServiceException, AmazonClientException;
//    public DescribeTagsResult describeTags() throws AmazonServiceException, AmazonClientException;
//    public DescribeReservedInstancesModificationsResult describeReservedInstancesModifications() throws AmazonServiceException, AmazonClientException;
//    public void deleteSpotDatafeedSubscription() throws AmazonServiceException, AmazonClientException;
//    public DescribeAddressesResult describeAddresses() throws AmazonServiceException, AmazonClientException;
//    public DescribeKeyPairsResult describeKeyPairs() throws AmazonServiceException, AmazonClientException;
//    public DescribeInstanceStatusResult describeInstanceStatus() throws AmazonServiceException, AmazonClientException;
//    public DescribeVpnGatewaysResult describeVpnGateways() throws AmazonServiceException, AmazonClientException;
//    public DescribeReservedInstancesOfferingsResult describeReservedInstancesOfferings() throws AmazonServiceException, AmazonClientException;
//    public AllocateAddressResult allocateAddress() throws AmazonServiceException, AmazonClientException;
//    public DescribeSnapshotsResult describeSnapshots() throws AmazonServiceException, AmazonClientException;
//    public <X extends AmazonWebServiceRequest> DryRunResult<X> dryRun(DryRunSupportedRequest<X> request) throws AmazonServiceException, AmazonClientException;
//    public void shutdown();
//    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request);
        
