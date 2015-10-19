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
package com.pslcl.dtf.common.config.status;

import java.util.List;

import com.pslcl.dtf.common.config.status.StatusTracker.Status;

/**
 * A complementary JMX interface to the <code>StatusTrackerProvider</code> implementation.
 * </p>
 * This interface defines the set of methods for reporting status information to JMX.
 *  </p>
 *  The consolidated status (highest level of concern) of all the named subsystems 
 *  as well as a list of all subsystem names, and ability to obtain the individual 
 *  status of each named subsystem is provided.
 * @see StatusTracker
 * @see StatusTrackerProvider 
 */
public interface StatusTrackerMXBean
{
    /**
     * Get the consolidated status of all known subsystems.
     * @return the consolidated status. Must not return null.
     */
    public Status getStatus();
    
    /**
     * Return a list of currently known subsystem names. 
     * @return a list of all currently known subsystem names.  Must not return null, may return an empty list.
     */
    public List<String> getNames();
    
    /**
     * Return the status of the given named subsystem.
     * @param name the subsystem name to return the status for.  Must not be null.
     * @return the status of the named subsystem.  Must not return null.
     */
    public Status getStatus(String name);
}
