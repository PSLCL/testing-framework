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
package com.pslcl.dtf.core.runner.resource;

import java.util.Date;
import java.util.Map;

import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ReservedResource implements ResourceDescription
{
    private final Map<String, String> attributes;
    private final ResourceCoordinates coordinates;
    private final long endTime;

    public ReservedResource(ResourceCoordinates coordinates, Map<String, String> attributes, int timeoutSeconds)
    {
        this.coordinates = coordinates;
        this.attributes = attributes;
        this.endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
    }

    // implement ResourceDescription interface

    @Override
    public String getName()
    {
        if(coordinates.getProvider() == null)
            throw new IllegalArgumentException("getName has been called but the given coordinates did not contain a provider");
        return coordinates.getProvider().getName();
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return coordinates;
    }
    
    // instance methods

    /**
     * Get the ResourceProvider that reserved the resource.
     * @return the ResourceProvider
     */
    public ResourceProvider getResourceProvider()
    {
        return coordinates.getProvider();
    }

    /**
     * Get the number of seconds for which this resource is reserved, measured from now.
     * @return The number of seconds.
     */
    public int getTimeLeft()
    {
        return (int) (endTime - (System.currentTimeMillis() / 1000));
    }

    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\nReservedResource:");
        format.level.incrementAndGet();
        coordinates.toString(format);
        format.ttl(StrH.mapToString(attributes));
        format.ttl(new Date(endTime).toString());
        return format.sb.toString();
    }
}