package com.pslcl.qa.runner.resource;

import java.util.List;
import java.util.concurrent.Future;

/**
 * A Resource provider which allows reserving and binding of Person resource types.
 */
public interface PersonProvider extends ResourceProvider, ArtifactConsumer {

	/**
	 * Acquire a Person.
	 *
	 * @param resource
	 * @return Person object which represents the Person Resource Instance.
	 */
	@Override
	public Future<PersonInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException;

	/**
	 * Acquire a list of persons. A list is returned containing Future objects that will be set with the
	 * resulting {@link PersonInstance} once bound.
	 * 
	 * The resources must be released once they are no longer needed.
	 *
	 * @param resources
	 *            A list of resources with attributes.
	 * @return A list of {@link PersonInstance} objects which each represent a Person Instance.
	 */
	@Override
	public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback);
}
