package com.pslcl.qa.runner.resource;

import java.util.List;

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
	public PersonInstance bind(ReservedResourceWithAttributes resource) throws BindResourceFailedException;

	/**
	 * Acquire a list of persons. Available persons will be bound and a list containing the resulting
	 * {@link PersonInstance} objects will be returned. Persons not currently available should be requested later.
	 * 
	 * The resources must be released once they are no longer needed.
	 *
	 * @param resources
	 *            A list of resources with attributes.
	 * @return A list of {@link PersonInstance} objects which each represent a Person Instance.
	 */
	@Override
	public List<PersonInstance> bind(List<ReservedResourceWithAttributes> resources);
}
