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
package com.pslcl.qa.runner.resource;

import java.util.List;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.config.RunnerServiceConfig;

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
	public Future<? extends ResourceInstance> bind(ReservedResourceWithAttributes resource) throws BindResourceFailedException;

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
	public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources) throws BindResourceFailedException;

	/**
	 * Release a bound resource instance. Any bound resources must be released.
	 * 
	 * @param resource
	 *            The resource instance to release.
	 * @param isReusable
	 *            Whether or not the Resource can be reused.
	 */
	public void release(ResourceInstance resource, boolean isReusable);

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
	 * <p>This method may move individual <code>ResourceWithAttributes</code> from the input resources into any of
	 * the three of four lists in the returned <code>ResourceQueryResult</code>.
	 * <ul>
	 * <li>reservedResources - will always return an empty list</li>
     * <li>availableResources - all <code>ResourceWithAttributes</code> from resources that could be reserved at this time.</li>
     * <li>unavailableResources - all <code>ResourceWithAttributes</code> from resources that could not be reserved at this time.</li>
     * <li>invalidResources - all <code>ResourceWithAttributes</code> from resources that appear to belong to me, but will fail bind with given information.</li>
	 * </ul>
	 * @param resources A list of resources with attributes. Must not be null, maybe empty.
	 * @return A {@link ResourceQueryResult} containing information about the availability of resource on this resource
	 *         provider.
	 */
	public ResourceQueryResult queryResourceAvailability(List<ResourceWithAttributes> resources);


	/**
	 * Reserve the specified resources if available. Resources will be reserved for timeoutSeconds if not greater than
	 * the maximum timeout allowed by the resource provider.
     * 
     * <p>This method may move individual <code>ResourceWithAttributes</code> from the input resources into any of
     * the three of four lists in the returned <code>ResourceQueryResult</code>.
     * <ul>
     * <li>reservedResources - all <code>ResourceWithAttributes</code> from resources that have be reserved at this time.</li>
     * <li>availableResources - will always return an empty set.</li>
     * <li>unavailableResources - all <code>ResourceWithAttributes</code> from resources that could not be reserved at this time.</li>
     * <li>invalidResources - all <code>ResourceWithAttributes</code> from resources that appear to belong to me, but will fail bind with given information.</li>
     * </ul>
     * @param resources A list of resources with attributes. Must not be null, maybe empty.
	 * @param resources The requested resources.
	 * @param timeoutSecond The time period, in seconds, to reserve the resources.
	 * @return The {@link ResourceQueryResult} listing the resources which were able to be reserved.
	 */
	public ResourceQueryResult reserveIfAvailable(List<ResourceWithAttributes> resources, int timeoutSeconds);

//	/**
//	 * Get a list of resource code names supported by the Resource Provider. These code names identify supported
//	 * resource types.
//	 * 
//	 * @note Intent is that this information is not required by users, and is offered for possible optimization.
//	 * @return A list of code name strings, each representing one resource.
//	 */
//	public List<String> getNames();
//
//	/**
//	 * Get the map of attributes supported by the resource provider, for the given resource code name.
//	 * 
//	 * @param name The name for the attributes.
//	 * @note Intent is that this information is not required by users, and is offered for possible optimization.
//	 * @return The map of attributes.
//	 */
//	public Map<String, String> getAttributes(String name);
	
	/**
	 * Daemon init pipe.
	 * @param config the current configuration.
	 * @throws Exception if the resource can not be initialized.
	 */
	public void init(RunnerServiceConfig config) throws Exception;
	
    /**
     * Daemon destroy pipe.
     */
    public void destroy();
}