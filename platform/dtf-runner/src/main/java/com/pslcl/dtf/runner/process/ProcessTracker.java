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

import com.pslcl.dtf.runner.DBConnPool;
import com.pslcl.dtf.runner.RunnerService;

public class ProcessTracker {

	// static
	
    /**
     * 
     * Note: This is a static method, because there is only one database.
     * Note: Timeout at five seconds by throwing TimeoutException
     * @param dbConnPool The database connection pool
     * @param reNum The run entry number
     * @return true if result is stored (as true or false); false if result is not stored
     * @throws TimeoutException on timeout
     * @throws Exception on any error
     */
    static public boolean isResultStored(DBConnPool dbConnPool, long reNum) throws TimeoutException, Exception {
    	boolean retBoolean = false;

        // temporarily, comment out these two lines, to allow local testing to proceed, even if a result is already stored
 		Boolean result = RunEntryCore.getResult(dbConnPool, reNum);
 		retBoolean = (result != null);
   		
        return retBoolean;
    }

	
    // instance declarations
    
	private RunnerService runnerService = null;
    
    public ProcessTracker(RunnerService runnerService) {
        this.runnerService = runnerService;
    }
    
    /**
     * 
     * Note: Timeout at five seconds by throwing TimeoutException
     * @param reNum The run entry number
     * @return true if running; false if not running
     * @throws TimeoutException on timeout
     * @throws Exception on any error
     */
    public boolean isRunning(long reNum) throws TimeoutException, Exception {
    	// return true if reNum if found in runEntryState storage
    	boolean retIsRunning = runnerService.runEntryStateStore.get(reNum) != null;
    	return retIsRunning;

    	// TODO: There is a case to consider, where a hopelessly blocked test run is detected.
    	//           Can it be detected? If so, this code could be injected, above:
        //    	if (retInProcess) {
        //    		if (hopelesslyBlocked) {
        //    			destroyTestRun(reNum);
        //    			retInProcess = false; // this can allow the test run to begin again
        //    		}
        //    	}
    }

}