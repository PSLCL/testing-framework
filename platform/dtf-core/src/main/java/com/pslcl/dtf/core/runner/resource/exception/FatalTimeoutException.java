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
package com.pslcl.dtf.core.runner.resource.exception;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;

/**
 * Thrown by a Resource Provider if it fails to bind a requested resource.  
 */
public class FatalTimeoutException extends FatalResourceException
{
    private static final long serialVersionUID = 8827644619962087334L;

    private String totalTime;
    private Integer maxDelay;
    private Integer maxRetries;
    
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * @param coordinates the resource coordinates.  Must not be null;
     */
    public FatalTimeoutException(ResourceCoordinates coordinates)
    {
        super(coordinates);
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param coordinates the resource coordinates.  Must not be null;
     * @param   message   the detail message. The detail message is saved for
     *          later retrieval by the {@link #getMessage()} method.
     */
    public FatalTimeoutException(ResourceCoordinates coordinates, String message)
    {
        super(coordinates, message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param coordinates the resource coordinates.  Must not be null;
     * @param  message the detail message (which is saved for later retrieval
     *         by the {@link #getMessage()} method).
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public FatalTimeoutException(ResourceCoordinates coordinates, String message, Throwable cause)
    {
        super(coordinates, message, cause);
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, {@link
     * java.security.PrivilegedActionException}).
     *
     * @param coordinates the resource coordinates.  Must not be null;
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <tt>null</tt> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @since  1.4
     */
    public FatalTimeoutException(ResourceCoordinates coordinates, Throwable cause)
    {
        super(coordinates, cause);
    }

    /**
     * Constructs a new exception with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack
     * trace enabled or disabled.
     *
     * @param  message the detail message.
     * @param cause the cause.  (A {@code null} value is permitted,
     * and indicates that the cause is nonexistent or unknown.)
     * @param enableSuppression whether or not suppression is enabled
     *                          or disabled
     * @param writableStackTrace whether or not the stack trace should
     *                           be writable
     * @since 1.7
     */
    protected FatalTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * 
     * @return Maximum Delay.
     */
    public synchronized Integer getMaxDelay()
    {
        return maxDelay;
    }
    
    /**
     * 
     * @return Maximum Retries.
     */
    public synchronized Integer getMaxRetries()
    {
        return maxRetries;
    }
    
    /**
     * 
     * @return total wait time.
     */
    public synchronized String getTotalWaitTime()
    {
        return totalTime;
    }
    
    /**
     * @param value the maximum delay value.  
     */
    public synchronized void setMaxDelay(int value)
    {
        maxDelay = value;
    }
    
    /**
     * @param value the maximum retries value.  
     */
    public synchronized void setMaxRetries(int value)
    {
        maxRetries = value;
    }
    
    /**
     * @param value the total wait time value.  
     */
    public synchronized void setTotalWaitTime(String value)
    {
        totalTime = value;
    }
}
