package com.pslcl.qa.runner.resource;

import java.util.List;

/**
 * A Resource provider which allows reserving and binding of Network resource types.
 */
public interface NetworkProvider extends ResourceProvider {

	/**
	 * Acquire a Network.
	 *
	 * @param resource
	 * @return Network object which represents the Network Resource Instance.
	 */
	@Override
	public NetworkInstance bind(ReservedResourceWithAttributes resource) throws BindResourceFailedException;

	/**
	 * Acquire a list of networks. Available networks will be bound and a list containing the resulting
	 * {@link NetworkInstance} objects will be returned. Networks not currently available should be requested later.
	 * 
	 * The resources must be released once they are no longer needed.
	 *
	 * @param resources
	 *            A list of resources with attributes.
	 * @return A list of {@link NetworkInstance} objects which each represent a Network Instance.
	 */
	@Override
	public List<NetworkInstance> bind(List<ReservedResourceWithAttributes> resources);

}
