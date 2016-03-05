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

import java.util.HashMap;
import java.util.Map;

import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ResourceDescImpl implements ResourceDescription
{
    private final String name;
    private final ResourceCoordinates coordinates;
    private final Map<String, String> attributes;

    /**
     * Construct a ResourceDescription object.
     * @param name
     * @param coordinatates
     * @param attributes
     */
    public ResourceDescImpl(String name, ResourceCoordinates coordinates,  Map<String, String> attributes)
    {
        this.name = name;
        this.coordinates = coordinates;
        this.attributes = attributes;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return coordinates;
    }
    
    @Override
    public Map<String, String> getAttributes()
    {
        synchronized (attributes)
        {
            return new HashMap<String, String>(attributes);
        }
    }
    
    @Override
    public void addAttribute(String key, String value)
    {
        synchronized (attributes)
        {
            attributes.put(key, value);
        }
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ResourceDescImpl))
            return false;
        ResourceDescImpl other = (ResourceDescImpl) obj;
        if (attributes == null)
        {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (coordinates == null)
        {
            if (other.coordinates != null)
                return false;
        } else if (!coordinates.equals(other.coordinates))
            return false;
        if (name == null)
        {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }
    
    public TabToLevel toString(TabToLevel format)
    {
        format.ttl("ResourceDescImpl:");
        format.level.incrementAndGet();
        format.ttl("name: ", name);
        coordinates.toString(format);
        format.ttl(StrH.mapToString(attributes));
        format.level.decrementAndGet();
        return format;
    }
}