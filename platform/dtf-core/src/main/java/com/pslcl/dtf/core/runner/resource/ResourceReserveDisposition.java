package com.pslcl.dtf.core.runner.resource;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ResourceReserveDisposition identifies the outcome of a resource reserve request.
 * 
 *
 */
public class ResourceReserveDisposition
{
    private final ResourceDescription inputResourceDescription;
    private final ReservedResource reservedResource;
    private final AtomicBoolean invalidResource;
    private final AtomicBoolean unavailableResource;

    /**
     * @param inputResourceDescription the requested resource.
     * @param reservedResource Must not be null
     */
    public ResourceReserveDisposition(ResourceDescription inputResourceDescription, ReservedResource reservedResource)
    {
        this.inputResourceDescription = inputResourceDescription;
        this.reservedResource = reservedResource;
        invalidResource = new AtomicBoolean(false);
        unavailableResource = new AtomicBoolean(false);
    }

    /**
     * @note This establishes that resource is unavailable; follow this with .setInvalidResource() to establish invalid resource, instead. 
     * @param inputResourceDescription the requested resource.
     */
    public ResourceReserveDisposition(ResourceDescription inputResourceDescription)
    {
        this.inputResourceDescription = inputResourceDescription;
        this.reservedResource = null;
        invalidResource = new AtomicBoolean(false);
        unavailableResource = new AtomicBoolean(true);
    }

    /**
     * 
     */
    public void setInvalidResource()
    {
        invalidResource.set(true);
        unavailableResource.set(false);
    }

    /**
     * @return "input" ResourceDescription, will not be null
     */
    public ResourceDescription getInputResourceDescription()
    {
        return inputResourceDescription;
    }

    /**
     * @return "output" reservedResource, will be null for invalid or unavailable resource
     */
    public ReservedResource getReservedResource()
    {
        return reservedResource;
    }

    /**
     * @return true for invalid resource, otherwise false
     */
    public boolean isInvalidResource()
    {
        return invalidResource.get();
    }

    /**
     * @return true for unavailable resource, otherwise false
     */
    public boolean isUnavailableResource()
    {
        return unavailableResource.get();
    }
}