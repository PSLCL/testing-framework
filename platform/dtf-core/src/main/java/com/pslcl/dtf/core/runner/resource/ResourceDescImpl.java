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
import java.util.concurrent.atomic.AtomicLong;

import com.pslcl.dtf.core.util.StrH;

public class ResourceDescImpl implements ResourceDescription
{
    private static final AtomicLong resourceIdMaster = new AtomicLong(0);
    
    private final String name;
    private final Map<String, String> attributes;
    private final String templateId;
    private final long resourceId;
    private final int stepNumber;

    public ResourceDescImpl(String name, Map<String, String> attributes, String templateId, int stepNumber)
    {
        this.name = name;
        this.attributes = attributes;
        this.templateId = templateId;
        resourceId = resourceIdMaster.incrementAndGet();
        this.stepNumber = stepNumber;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    @Override
    public String getTemplateId()
    {
        return templateId;
    }
    
    @Override
    public long getResourceId()
    {
        return resourceId;
    }

    public long getStepNumber()
    {
        return stepNumber;
    }
    
    /**
     * 
     * @param resourceDescription Must not be null
     * @return
     */
    public boolean matches(ResourceDescription resourceDescription)
    {
        // match: reference, hash and attributes are equal
        //@formatter:off
        if (templateId.equals(resourceDescription.getTemplateId()) && 
            resourceId == resourceDescription.getResourceId() &&
            name.equals(resourceDescription.getName()))
            //@formatter:on
        {
            // match the attribute sets to each other
            Map<String, String> rwaAttributes = resourceDescription.getAttributes();
            if (this.attributes.size() != rwaAttributes.size())
                return false;
            // these keys and values might be empty strings, but they will not be null; keys are unique in each Map
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
            .append(",templateId: " + templateId)
            .append(",resouceId: " + resourceId)
            .append(",stepNumber: " + stepNumber)
            .append(",attrs: ")
            .append(StrH.mapToString(attributes));
        sb.append("}");
        return sb.toString();
    }
}