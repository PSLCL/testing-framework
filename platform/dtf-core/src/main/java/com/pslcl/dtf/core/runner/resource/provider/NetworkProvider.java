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
package com.pslcl.dtf.core.runner.resource.provider;

import java.util.List;
import java.util.concurrent.Future;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;


/**
 * A Resource provider which allows reserving and binding of Network resource types.
 */
public interface NetworkProvider extends ResourceProvider
{
    /**
     * Acquire a list of resources. Resources will be bound and a list containing the resulting
     * ResourceInstance objects will be returned.
     * 
     * The resources must be released once they are no longer needed.
     *
     * @param resources a list of resources.
     * @return A list of ResourceInstance objects which each represent a Resource Instance.
     * @throws ResourceNotReservedException if unable to bind all of the listed resources.
     */
    public List<Future<NetworkInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException;
}
