package com.pslcl.dtf.core.runner.resource;

import java.util.concurrent.atomic.AtomicBoolean;

import com.pslcl.dtf.core.util.TabToLevel;

/**
 * ResourceReserveDisposition identifies the outcome of a resource reserve request, with three reports:
 *     An unavailable resource.
 *     An invalid resource.
 *     A reserved resource.
 *
 * <p>Invalid is returned for encountered situations that prevent reservation. Two known cases are:
 *           The .reserve() call parameters not able to be interpreted.
 *           The reserve is denied because an identical reservation already exists, having identical identifiers. This uniqueness is offered in normal operation, but testbeds can produce non-unique reserve requests.  
 * <p>Unavailable is returned if the reserve request is both:
 *           Not invalid.
 *           Not able to be reserved by this ResourceProvider. Examples: attributes not recognized or not supported, or request is for person when the provider is for a machine.
 * <p>ReserverResource is returned when a resource instance can be reserved. This means that the request is not invalid, and not unavailable. 
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
    
    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("ResourceReserveDisposition:");
        format.level.incrementAndGet();
        format.ttl("invalidResource = " + invalidResource.get());
        format.ttl("unavailableResource = " + unavailableResource.get());
        format.ttl("Input Resource:");
        format.level.incrementAndGet();
        format.ttl(inputResourceDescription.getCoordinates().toString(format));
        format.level.decrementAndGet();
        format.ttl("Reserved Resource:");
        format.level.incrementAndGet();
        if(reservedResource == null)
            format.ttl("null");
        else
            format.ttl(reservedResource.getCoordinates().toString(format));
        format.level.decrementAndGet();
        return format.sb.toString();
    }
}