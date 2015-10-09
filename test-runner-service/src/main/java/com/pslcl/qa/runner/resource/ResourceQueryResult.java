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
package com.pslcl.qa.runner.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold and resolve results
 */
public class ResourceQueryResult
{
    // all lists guarded by reservedResources
    private final List<ReservedResource> reservedResources;
    private final List<ResourceDescription> availableResources;
    private final List<ResourceDescription> unavailableResources;
    private final List<ResourceDescription> invalidResources;

    public ResourceQueryResult(List<ReservedResource> reservedResources, List<ResourceDescription> availableResources, List<ResourceDescription> unavailableResources, List<ResourceDescription> invalidResources)
    {
        this.reservedResources = reservedResources;
        this.availableResources = availableResources;
        this.unavailableResources = unavailableResources;
        this.invalidResources = invalidResources;
    }

    /**
     * Get a list of reserved resources.
     * 
     * @return A list of reserved resources.
     */
    public List<ReservedResource> getReservedResources()
    {
        synchronized (reservedResources)
        {
            return new ArrayList<ReservedResource>(reservedResources);
        }
    }

    /**
     * Add a reserved resource to the list of reserved resources.
     *  
     * @param rrwa The reserved resource to add.
     */
    public void reservedResources_add(ReservedResource rrwa) {
        synchronized (reservedResources) {
            this.reservedResources.add(rrwa);
        }
    }
    
    /**
     * Get the list of available resources. These resources are not bound and may become unavailable after this object is created.
     * 
     * @return A list of resources available at the time this object was created.
     */
    public List<ResourceDescription> getAvailableResources()
    {
        synchronized (reservedResources)
        {
            return new ArrayList<ResourceDescription>(availableResources);
        }
    }

    /**
     * Add an available resource to the list of available resources.
     *  
     * @param rwa The available resource to add.
     */
    public void availabledResources_add(ResourceDescription rwa) {
        synchronized (reservedResources) {
            this.availableResources.add(rwa);
        }
    }

    /**
     * Get the list of unavailable resources. These resources may become available after this object is created.
     * 
     * @return A list of resources unavailable at the time this object was created.
     */
    public List<ResourceDescription> getUnavailableResources()
    {
        synchronized (reservedResources)
        {
            return new ArrayList<ResourceDescription>(unavailableResources);
        }
    }

//    /**
//     * Add an unavailable resource to the list of unavailable resources.
//     *  
//     * @param rwa The unavailable resource to add.
//     */
//    public void unavailabledResourcesAdd(ResourceWithAttributes rwa) {
//        synchronized (reservedResources) {
//            this.availableResources.add(rwa);
//        }
//    }
    
    /**
     * Get the list of requested resources not understood by the resource provider.
     * 
     * @return A list of requested resources not understood by the resource provider.
     */
    public List<ResourceDescription> getInvalidResources()
    {
        synchronized (reservedResources)
        {
            return new ArrayList<ResourceDescription>(invalidResources);
        }
    }
    
//    /**
//     * Add an invalid resource to the list of invalid resources.
//     *  
//     * @param rwa The invalid resource to add.
//     */
//    public void invalidResourcesAdd(ResourceWithAttributes rwa) {
//        synchronized (reservedResources) {
//            this.invalidResources.add(rwa);
//        }
//    }
    

    /**
     * Merge a given ResourceQueryResult to this object
     * 
     * @note Once the reservation is made (a reservedResource entry is placed into the reserved list), it will not be removed
     * @param localRqr Caller is responsible to ensure that localRqr does not contain a ReservedResourceWithAttributes entry that is already stored in the reservedResources entry of this object.
     */
    public void merge(ResourceQueryResult localRqr)
    {
        // localRqr is composed of 1 result for every original resource request; keep in mind that each such result fills only one of the RQR's four lists.

        synchronized (reservedResources)
        {

            // Merge all reservations found in localRqr.
            for (ReservedResource rrwa : localRqr.getReservedResources())
            {
                // record the successful reservation found in localRqr 
                this.reservedResources.add(rrwa);
                // remove the corresponding entry in the 3 "fail" lists that we may hold from previous calls
                this.availableResources.remove(rrwa);
                this.unavailableResources.remove(rrwa);
                this.invalidResources.remove(rrwa);
                // caller now sees only the reserved entry
            }

            // Record whatever "fails" are found in localRqr
            for (ResourceDescription rwa : localRqr.getAvailableResources())
            {
                // this rwa is not found in incoming reservedResources, unavailableResources or invalidResources
                this.availableResources.add(rwa); // might add to an entry from a previous call
            }
            for (ResourceDescription rwa : localRqr.getUnavailableResources())
            {
                // this rwa is not found in incoming reservedResources, availableResources or invalidResources
                this.unavailableResources.add(rwa); // might add to an entry from a previous call
            }
            for (ResourceDescription rwa : localRqr.getInvalidResources())
            {
                // this rwa is not found in incoming reservedResources, availableResource or unavailableResources
                this.invalidResources.add(rwa); // might add to an entry from a previous call
            }
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("{reserved: ")
            .append(reservedResources == null ? "{null}" : reservedResources.toString())
            .append(",avilable: ")
            .append(availableResources == null ? "{null}" : availableResources.toString())
            .append(",unavilable: ")
            .append(unavailableResources == null ? "{null}" : unavailableResources.toString())
            .append(",invalid: ")
            .append(invalidResources == null ? "{null}" : invalidResources.toString());
        sb.append("}");
        return sb.toString();
    }
}
