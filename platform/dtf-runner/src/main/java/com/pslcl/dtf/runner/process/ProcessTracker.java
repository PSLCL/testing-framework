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
package com.pslcl.dtf.runner.process;

import java.util.concurrent.TimeoutException;

import com.pslcl.dtf.runner.RunnerService;

public class ProcessTracker {
    
    private RunnerService runnerService = null;
    
    public ProcessTracker(RunnerService runnerService) {
        this.runnerService = runnerService;
    }
    
    /**
     * 
     * @note This is a static method, because there is only one database.
     * @note Timeout at five seconds by throwing TimeoutException
     * @param dtNum
     * @return
     * @throws Exception
     */
    public static boolean resultStored(long dtNum) throws TimeoutException, Exception {
        return false; // TODO
    }
    
    /**
     * 
     * @note Timeout at five seconds by throwing TimeoutException
     * @param instanceNumber
     * @return
     * @throws Exception
     */
    public boolean inProcess(long dtNum) throws TimeoutException, Exception {
        // TODO: Expand this, perhaps. Shown is the basic truth: if result is not stored, then it is inProcess.
        //       But this simple truth ignores the case where progress is hopelessly blocked.
        //       So make a decision:
        //           Is there anything that can be added here that could fix up a blocked test instance, by replacing it with a new test instance?
        //           If not, the this single line of code is appropriate.
        return runnerService.actionStore.get(dtNum) != null;
    }

}
