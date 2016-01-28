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
package com.pslcl.dtf.core.runner.resource.instance;

import java.util.concurrent.Future;

/**
 * 
 * 
 *
 */
public interface RunnableProgram
{
    /**
	 * Stop the program command on a machine and return a Future with the {@link Future<Integer>} result of the corresponding stop command, or the exit code of
	 * the stop script, being set once the stop script has completed.
     * 
     * @return a Future with the {@link Future<Integer>} result of the corresponding stop command, or the exit code of
	 * the stop script, being set once the stop script has completed.
     */
    public Future<Integer> kill();

    /**
     * Determine the running state of the program.
     * 
     * @return {@code True} if the program is running. {@code False} otherwise.
     */
    public boolean isRunning();
    
    /**
     * Return a program run result, if available.
     * 
     * @note Null return indicates that the program run result was not available.
     * @note Zero return indicates that the program completed with a successful result. 
     * @note Non-zero return indicates that the program completed with a failed result.
     * 
     * @return The program run result, or null.
     */
    public Integer getRunResult();

}