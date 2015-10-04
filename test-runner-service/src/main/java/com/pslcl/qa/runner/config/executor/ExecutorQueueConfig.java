/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.qa.runner.config.executor;

import com.pslcl.qa.runner.config.status.StatusTracker;
import com.pslcl.qa.runner.config.status.StatusTrackerProvider;


/**
 * Declares the required ExecutorQueueConfig interface
 */
public interface ExecutorQueueConfig
{
    /**
     * Get the queues core pool size.
     * </p>
     * @return size the core pool size.
     */
    public int getCorePoolSize();

    /**
     * Get the thread name prefix for pool threads.
     * </p>
     * @return name the thread prefix name to use.  Must not be null.
     * @throws IllegalArgumentException if name is null.
     */
    public String getThreadNamePrefix();

    /**
     * Get the <code>StatusTracker</code> to use.
     * </p>
     * @return the <code>StatusTracker</code> to use.  Must not return null.
     * @see StatusTrackerProvider
     */
    public StatusTracker getStatusTracker();

    /**
     * Get the <code>StatusTracker</code> subsystem name to use.
     * @return the <code>StatusTracker</code>'s subsystem name.
     * @see StatusTrackerProvider
     */
    public String getStatusSubsystemName();
}