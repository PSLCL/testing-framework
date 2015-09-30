package com.pslcl.qa.runner.resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider is to
 * instantiate resources. However, all interactions between the platform and the resource are brokered through the
 * provider.
 */
public interface ResourceProvider {

	/**
	 * Acquire a resource.
	 * 
	 * The resource must be released once it is no longer needed.
	 *
	 * @param resource
	 *            A resource with attributes.
	 * @param statusCallback
	 *            callback for {@link ResourceStatus} change notification.
	 * @return Resource object which represents the Resource Instance.
	 * @throws BindResourceFailedException if unable to bind the resource.
	 */
	public Future<? extends ResourceInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException;

	/**
	 * Acquire a list of resources. Resources will be bound and a list containing the resulting
	 * ResourceInstance objects will be returned.
	 * 
	 * The resources must be released once they are no longer needed.
	 *
	 * @param resources
	 *            A list of resources with attributes.
	 * @param statusCallback
	 *            callback for {@link ResourceStatus} change notification.
	 * @return A list of ResourceInstance objects which each represent a Resource Instance.
	 * @throws BindResourceFailedExcpetion if unable to bind all of the listed resources.
	 */
	public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback) throws BindResourceFailedException;

	/**
	 * Release a bound resource instance. Any bound resources must be released.
	 * 
	 * @param resource
	 *            The resource instance to release.
	 */
	public void release(ResourceInstance resource);

	/**
	 * Release a reserved resource. If a resource is bound, the reservation is automatically released.
	 * 
	 * @param resource
	 *            The reserved resource to release.
	 */
	public void releaseReservedResource(ReservedResourceWithAttributes resource);

	/**
	 * Check whether the specified resource is available.
	 * 
	 * @param resource
	 *            A resource with attributes.
	 * 
	 * @return True if the specified resource is available. False otherwise.
	 * 
	 * @throws ResourceNotFoundException
	 *             Thrown if the resourceHash is not known by the resource provider.
	 */
	public boolean isAvailable(ResourceWithAttributes resource) throws ResourceNotFoundException;

	/**
	 * Query the Resource Provider for the availability of the specified resources. Resources are not bound or reserved,
	 * and availability may change after this method returns.
	 * 
	 * @param resources
	 *            A list of resources with attributes.
	 * @return A {@link ResourceQueryResult} containing information about the availability of resource on this resource
	 *         provider.
	 */
	public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources);

	/**
	 * Reserve the specified resources if available. Resources will be reserved for timeoutSeconds if not greater than
	 * the maximum timeout allowed by the resource provider.
	 * 
	 * @param resources
	 *            The requested resources.
	 * @param timeoutSecond
	 *            The time period, in seconds, to reserve the resources.
	 * @return The {@link ResourceQueryResult} listing the resources which were able to be reserved.
	 */
	public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds);

	/**
	 * Get a list of resource code names supported by the Resource Provider. These code names identify supported
	 * resource types.
	 * 
	 * @note Intent is that this information is not required by users, and is offered for possible optimization.
	 * @return A list of code name strings, each representing one resource.
	 */
	public List<String> getNames();

	/**
	 * Get the map of attributes supported by the resource provider, for the given resource code name.
	 * 
	 * @param hash
	 *            The hash for the attributes.
	 * @note Intent is that this information is not required by users, and is offered for possible optimization.
	 * @return The map of attributes.
	 */
	public Map<String, String> getAttributes(String name);

}