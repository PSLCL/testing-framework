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

import java.util.Map;

import com.pslcl.dtf.core.util.StrH;

public class ResourceDescImpl implements ResourceDescription
{
    private String name;
    private Map<String, String> attributes;
    private int reference;

    public ResourceDescImpl(String name, Map<String, String> attributes, int reference)
    {
        this.name = name;
        this.attributes = attributes;
        this.reference = reference;
    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        // TODO Auto-generated method stub
        return attributes;
    }

    @Override
    public long getReference()
    {
        // TODO Auto-generated method stub
        return reference;
    }

    /**
     * 
     * @param resourceDescription Must not be null
     * @return
     */
    public boolean matches(ResourceDescription resourceDescription)
    {
        // match: reference, hash and attributes are equal
        if (this.reference == resourceDescription.getReference() && this.name.equals(resourceDescription.getName()))
        {
            // match the attribute sets to each other
            Map<String, String> rwaAttributes = resourceDescription.getAttributes();
            if (this.attributes.size() != rwaAttributes.size())
                return false;
            // tHese keys and values might be empty strings, but they will not be null; keys are unique in each Map
            for (String key : this.attributes.keySet())
            {
                if (rwaAttributes.containsKey(key))
                {
                    String value = this.attributes.get(key);
                    if (value.equals(rwaAttributes.get(key)))
                        continue;
                }
                return false;
            }
        }
        return true; // every check succeeded
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("{name: ")
            .append(name == null ? "null" : name)
            .append(",ref: ")
            .append(""+reference)
            .append(",attrs: ")
            .append(StrH.mapToString(attributes));
        sb.append("}");
        return sb.toString();
    }
}