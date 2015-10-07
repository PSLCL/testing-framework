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

/**
 * A Resource provider which allows reserving and binding of Machine resource types.
 */
public interface MachineProvider extends ResourceProvider, ArtifactConsumer {

	/**
	 * Acquire a Machine.
	 * 
	 * This resource must be released once it is no longer needed.
	 *
	 * @param resource
	 * @return MachineInstance object which represents the Machine as a ResourceInstance.
	 * @note This MachineInstance is more explicitly defined than the ResourceInstance returned by
	 *       ResourceProvider.bind().
	 * @see ResourceProvider
	 */
	@Override
	public Future<MachineInstance> bind(ReservedResourceWithAttributes resource, ResourceStatusCallback statusCallback) throws BindResourceFailedException;

	/**
	 * Acquire a list of reserved machines. A list is returned containing Future objects that will be set with the
	 * resulting {@link MachineInstance} once bound.
	 * 
	 * The resources must be released once they are no longer needed.
	 *
	 * @param resources
	 *            A list of resources with attributes.
	 * @return A list of {@link MachineInstance} objects which each represent a Machine Instance.
	 */
	@Override
	public List<Future<? extends ResourceInstance>> bind(List<ReservedResourceWithAttributes> resources, ResourceStatusCallback statusCallback);
}
