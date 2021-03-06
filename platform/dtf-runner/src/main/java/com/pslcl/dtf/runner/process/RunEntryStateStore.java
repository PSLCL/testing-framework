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

import java.util.concurrent.ConcurrentHashMap;

public class RunEntryStateStore {
   
    /**
     *  Used for pure storage, order of element access is determined by other means.
     *  Neither Long key nor InstanceState value may be null.
     *  Writes by any one thread locks only the relevant element, other threads freely access other elements
     */
    ConcurrentHashMap<Long, RunEntryState> map; 
    
    public RunEntryStateStore() {
        map = new ConcurrentHashMap<>();
    }
    
    /**
     * 
     * @param configurationMaxSize
     * @return
     */
    public synchronized boolean isMaxSizeReached(int configurationMaxSize) {
    	boolean limitReached = (configurationMaxSize <= map.size());
    	return limitReached;
    }
    
    /**
     * 
     * Note: Put to an existing Long key overwrites previous the previously held InstanceState
     * @param iNum
     * @param reState
     * @return Previously stored InstanceState when put overwrites it, null otherwise. 
     */
    synchronized RunEntryState put(long reNum, RunEntryState reState) {
        return map.put(Long.valueOf(reNum), reState);
    }
   
    /**
     * 
     * @param iNum
     * @return
     */
    synchronized RunEntryState get(long reNum) {
        return map.get(Long.valueOf(reNum));
    }

    synchronized RunEntryState remove(long reNum) {
    	return map.remove(Long.valueOf(reNum));
    }
    
}