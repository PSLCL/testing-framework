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
package com.pslcl.dtf.core.util.executor;

import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.config.status.DtfStatusTracker;


/**
 * Declares the required ExecutorQueueConfig interface
 */
public interface ExecutorQueueConfig
{
    /**
     * Get the queues core pool size.
     * <p>
     * @return size the core pool size.
     */
    public int getCorePoolSize();

    /**
     * Get the thread name prefix for pool threads.
     * <p>
     * @return name the thread prefix name to use.  Must not be null.
     * @throws IllegalArgumentException if name is null.
     */
    public String getThreadNamePrefix();

    /**
     * Get the <code>StatusTracker</code> to use.
     * <p>
     * @return the <code>StatusTracker</code> to use.  Must not return null.
     * @see DtfStatusTracker
     */
    public StatusTracker getStatusTracker();

    /**
     * Get the <code>StatusTracker</code> subsystem name to use.
     * @return the <code>StatusTracker</code>'s subsystem name.
     * @see DtfStatusTracker
     */
    public String getStatusSubsystemName();
}