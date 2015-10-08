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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.MachineProvider;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceNotFoundException;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceStatusCallback;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */
public class AwsMachineProvider extends ResourceProvider implements MachineProvider
{
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;

    public AwsMachineProvider()
    {
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
    }

    @Override
    public void init(RunnerServiceConfig config) throws Exception
    {
        super.init(config);
        instanceFinder.init(config);
        imageFinder.init(config);
    }        

    @Override
    public void destroy()
    {
        super.destroy();
    }

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
        return null;
    }

    @Override
    public void releaseReservedResource(ReservedResourceWithAttributes resource)
    {
    }

    @Override
    public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException
    {
        InstanceType instanceType = instanceFinder.findInstance(resource);
        String imageId = imageFinder.findImage(ec2Client, resource);
        instanceFinder.checkLimits(instanceType);
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
        
