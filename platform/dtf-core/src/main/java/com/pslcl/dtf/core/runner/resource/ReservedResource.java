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

/**
 * A resource with attributes that has been reserved by the resource provider. After a timeout period
 * specified by timeoutSeconds has passed, or if the resource is bound, the resource is no longer reserved.
 */
public class ReservedResource implements ResourceDescription
{
    private final String name;
    private final Map<String, String> attributes;
    private final String templateId;
    private final long reference;
    private final ResourceProvider resourceProvider;
    private final long endTime;

    /**
     * constructor
     * @param resourceDescription
     * @param resourceProvider
     * @param timeoutSeconds
     */
    public ReservedResource(ResourceDescription resourceDescription, ResourceProvider resourceProvider, int timeoutSeconds)
    {
        //@formatter:off
        this(
            resourceDescription.getName(), 
            resourceDescription.getAttributes(), 
            resourceDescription.getTemplateId(), 
            resourceDescription.getReference(), 
            resourceProvider, 
            timeoutSeconds);
        //@formatter:off
    }

    /**
     * constructor
     * @param name
     * @param attributes
     * @param reference
     * @param resourceProvider
     * @param timeoutSeconds
     */
    
    //@formatter:off
    public ReservedResource(
        String name, 
        Map<String, String> attributes, 
        String templateId, 
        long reference, 
        ResourceProvider resourceProvider, 
        int timeoutSeconds)
    //@formatter:on
    {
        this.name = name;
        this.attributes = attributes;
        this.templateId = templateId;
        this.reference = reference;
        this.resourceProvider = resourceProvider;
        this.endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
    }

    // implement ResourceDescription interface

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
    public long getReference()
    {
        return reference;
    }

    // instance methods

    /**
     * Get the ResourceProvider that reserved the resource.
     * @return the ResourceProvider
     */
    public ResourceProvider getResourceProvider()
    {
        return resourceProvider;
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
        StringBuilder sb = new StringBuilder("{name: ").append(name == null ? "null" : name).append(",ref: ").append("" + reference).append(",end: ").append(new Date(endTime).toString()).append(",attrs: ").append(StrH.mapToString(attributes));
        sb.append("}");
        return sb.toString();
    }
}