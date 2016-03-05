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

import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ResourceCoordinates
{
    private ResourcesManager manager;
    private ResourceProvider provider;
    public final String templateId;
    public final long templateInstanceId;
    public final long resourceId;
    private long runId;
    
    /**
     * Construct a full Resource Coordinates
     * @param manager may be null for various contexts and states.
     * @param provider may be null for various contexts and states.
     * @param templateId must never be null.
     * @param resourceId must always be a valid unique ID for a given resource for the life of the runner.
     * @param runId -1 if unknown, otherwise the unique ID that identifies an individual test run.
     */
    public ResourceCoordinates(ResourcesManager manager, ResourceProvider provider, String templateId, long templateInstanceId, long resourceId, long runId)
    {
        if(templateId == null)
            throw new IllegalArgumentException("templateId == null");
        this.manager = manager;
        this.provider = provider;
        this.templateId = templateId;
        this.templateInstanceId = templateInstanceId;
        this.resourceId = resourceId;
        this.runId = runId;
    }
    
    /**
     * Construct a full Resource Coordinates from a partial.
     * @param manager may be null for various contexts and states.
     * @param provider may be null for various contexts and states.
     * @param coordinates must never be null.
     * @param resourceId must always be a valid unique ID for a given resource for the life of the runner.
     * @param runId -1 if unknown, otherwise the unique ID that identifies an individual test run.
     */
    public ResourceCoordinates(ResourcesManager manager, ResourceProvider provider, ResourceCoordinates coordinates)
    {
        if(coordinates == null)
            throw new IllegalArgumentException("coordinates == null");
        this.manager = manager;
        this.provider = provider;
        this.templateId = coordinates.templateId;
        this.templateInstanceId = coordinates.templateInstanceId;
        this.resourceId = coordinates.resourceId;
        this.runId = coordinates.runId;
    }
    
    /**
     * Construct a Resource ID's only Coordinates
     * @param templateId must never be null.
     * @param resourceId must always be a valid unique ID for a given resource for the life of the runner.
     * @param runId -1 if unknown, otherwise the unique ID that identifies an individual test run.
     */
    public ResourceCoordinates(String templateId, long templateInstanceId, long resourceId, long runId)
    {
        if(templateId == null)
            throw new IllegalArgumentException("templateId == null");
        this.manager = null;
        this.provider = null;
        this.templateId = templateId;
        this.templateInstanceId = templateInstanceId;
        this.resourceId = resourceId;
        this.runId = runId;
    }
    
    public synchronized ResourcesManager getManager()
    {
        return manager;
    }

    public synchronized void setManager(ResourcesManager manager)
    {
        this.manager = manager;
    }
    
    public synchronized ResourceProvider getProvider()
    {
        return provider;
    }

    public synchronized void setProvider(ResourceProvider provider)
    {
        this.provider = provider;
    }
    
    public synchronized long getRunId()
    {
        return runId;
    }

    public synchronized void setRunId(long runId)
    {
        this.runId = runId;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (resourceId ^ (resourceId >>> 32));
        result = prime * result + (int) (runId ^ (runId >>> 32));
        result = prime * result + (int) (templateInstanceId ^ (runId >>> 32));
        result = prime * result + ((templateId == null) ? 0 : templateId.hashCode());
        return result;
    }
    
    // DO NOT include manager and provider in equals/hashCode
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ResourceCoordinates))
            return false;
        ResourceCoordinates other = (ResourceCoordinates) obj;
        if (resourceId != other.resourceId)
            return false;
        if (runId != other.runId)
            return false;
        if (templateId == null)
        {
            if (other.templateId != null)
                return false;
        } else if (!templateId.equals(other.templateId))
            return false;
        if(templateInstanceId != other.templateInstanceId)
            return false;
        return true;
    }
    
    public TabToLevel toString(TabToLevel format)
    {
        format.ttl("resourceId: " + resourceId);
        format.level.incrementAndGet();
        format.ttl("manager: ", (manager == null ? "null" : manager.getClass().getName()));
        format.ttl("provider: ", (provider == null ? "null" : provider.getClass().getName()));
        format.ttl("templateId: " + templateId);
        format.ttl("templateInstanceId: " + templateId);
        format.ttl("runId: " + runId);
        format.level.decrementAndGet();
        return format;
    }
    
    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        return toString(format).sb.toString(); 
    }
}