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
 * A ResourceQuery is a resource hash and a map of attributes.
 */
public interface ResourceDescription
{
    /**
     * Get the name of the resource.
     * @return The name.
     */
    public String getName();

    /**
     * Get the set of attributes for the resource.
     * @return A map of strings representing the set of attributes for the resource.
     */
    public Map<String, String> getAttributes();

    /**
     * get the unique coordinates for a given resource object.
     * @return the coordinates of the resource.
     */
    public ResourceCoordinates getCoordinates();

    /**
     * DTF system wide source for obtaining a unique resourceId
     */
    public static final AtomicLong resourceIdMaster = new AtomicLong(0);
}