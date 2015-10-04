/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.qa.runner.config.executor;


/**
 * Declares the required BlockingExecutorConfig interface
 */
public interface BlockingExecutorConfig extends ExecutorQueueConfig
{
    /**
     * Get the maximum queue size.
     * </p>
     * @return the maximum queue size.
     */
    public int getMaximumPoolSize();

    /**
     * Get the allow core threads to timeout flag.
     * </p>
     * Should the number of core threads collapse if no activity is seen?
     * The default is true.
     * @return enable the core threads should collapse if true and remain if false.
     * @see #getKeepAliveTime()
     */
    public boolean isAllowCoreThreadTimeout();

    /**
     * Get the keep alive delay for core threads.
     * </p>
     * If the allow core thread timeout is true, this is the delay before they
     * will start to collapse.
     * @return the delay in milliseconds before core pool threads will collapse is idle.
     * @see #isAllowCoreThreadTimeout()
     */
    public int getKeepAliveTime();
    
    /**
     * Get the maximum time to block queue input if queue is full.
     * </p>
     * If the queue is full when a new input is attempted this value is checked
     * to see if the inputing thread should be blocked for some period of time
     * before to allow the queue to clear before throwing an exception.
     * 
     * @return blocking time in milliseconds.
     */
    public int getMaximumBlockingTime();
}
