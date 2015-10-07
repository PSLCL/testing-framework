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
