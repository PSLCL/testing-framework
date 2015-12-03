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

/**
 * A ResourceDescription is a set of correlated coordinates  and a map of attributes.
 */
public interface ResourceDescription
{
    /**
     * Get the name of the resource.
     * @return The name.  Must never be null;
     */
    public String getName();

    /**
     * Get the set of attributes for the resource.
     * <p>The implementor is responsible for the thread safety of this map and
     * should typically clone its master map for the return value.
     * @return A map of strings representing the set of attributes for the resource.  Must never be null, maybe empty.
     */
    public Map<String, String> getAttributes();

    /**
     * Add an attribute to the resources attributes in a thread safe manner.
     * <p>Provider implementations will populate the master with defaults that it fills in. 
     * @param key of the value being added.  Must not be null.
     * @param value being added. May be null;
     */
    public void addAttribute(String key, String value);

    /**
     * get the unique coordinates for a given resource object.
     * @return the coordinates of the resource.  Must never be null;
     */
    public ResourceCoordinates getCoordinates();

    /**
     * DTF system wide source for obtaining a unique resourceId
     */
    public static final AtomicLong resourceIdMaster = new AtomicLong(0);
}