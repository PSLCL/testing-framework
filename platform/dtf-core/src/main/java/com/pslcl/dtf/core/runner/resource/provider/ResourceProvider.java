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

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;

/**
 * This interface defines interactions with a resource provider. The primary responsibility of a resource provider is to
 * instantiate resources. However, all interactions between the platform and the resource are brokered through the
 * provider.
 */
public interface ResourceProvider
{
    /**
     * Acquire a resource.
     * The resource must be released once it is no longer needed.
     * @param resource a resource with attributes.
     * @return Resource object which represents the Resource Instance.
     * @throws ResourceNotReservedException if unable to bind the resource.
     */
    public Future<? extends ResourceInstance> bind(ReservedResource resource) throws ResourceNotReservedException;

    /**
     * Reserve the specified resources if available. Resources will be reserved for timeoutSeconds if not greater than
     * the maximum timeout allowed by the resource provider.
     * 
     * <p>This method may move individual <code>ResourceDescription</code> from the input resources into any of
     * the three of four lists in the returned <code>ResourceQueryResult</code>.
     * <ul>
     * <li>reservedResources - all <code>ResourceDescription</code> from resources that have be reserved at this time.</li>
     * <li>availableResources - will always return an empty set.</li>
     * <li>unavailableResources - all <code>ResourceDescription</code> from resources that could not be reserved at this time.</li>
     * <li>invalidResources - all <code>ResourceDescription</code> from resources that appear to belong to me, but will fail bind with given information.</li>
     * </ul>
     * @param resources A list of resources. Must not be null, maybe empty.
     * @param timeoutSeconds The time period, in seconds, to reserve the resources.
     * @return The {@link ResourceReserveDisposition} listing the resources which were able to be reserved.
     */
    public Future<List<ResourceReserveDisposition>> reserve(List<ResourceDescription> resources, int timeoutSeconds);

    /**
     * Get the name of the provider.
     * @return The name.
     */
    public String getName();

    /**
     * Get the attribute keys supported by the provider.
     * @return The list of keys.
     */
    public List<String> getAttributes();

    /**
     * Daemon init pipe.
     * @param config the current configuration.
     * @throws Exception if the resource can not be initialized.
     */
    public void init(RunnerConfig config) throws Exception;

    /**
     * Daemon destroy pipe.
     */
    public void destroy();

    /** The name of a Machine type Provider */
    public static final String MachineName = "machine";
    /** The name of a Person type Provider */
    public static final String PersonName = "person";
    /** The name of a Network type Provider */
    public static final String NetworkName = "network";

    /**
     * Helper to obtain a Provider type name.
     * @param provider the provider to report type name of.  Must not be null.
     * @return the provider type name. Will return null if provider is not of type; <code>MachineProvider</code> or <code>PersonProvider</code> or <code>NetworkProvider</code>
     */
    public static String getTypeName(ResourceProvider provider)
    {
        if (provider instanceof MachineProvider)
            return MachineName;
        if (provider instanceof PersonProvider)
            return PersonName;
        if (provider instanceof NetworkProvider)
            return NetworkName;
        return null;
    }
}