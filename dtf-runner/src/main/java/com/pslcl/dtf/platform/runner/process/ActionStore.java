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
package com.pslcl.dtf.platform.runner.process;

import java.util.concurrent.ConcurrentHashMap;

public class ActionStore {
   
    /**
     *  Used for pure storage, order of element access is determined by other means.
     *  Neither Long key nor InstanceState value may be null.
     *  Writes by any one thread locks only the relevant element, other threads freely access other elements
     */
    ConcurrentHashMap<Long, RunEntryState> map; 
    
    public ActionStore() {
        map = new ConcurrentHashMap<>();
    }
    
    /**
     * 
     * @note put to an existing Long key overwrites previous the previously held InstanceState
     * @param iNum
     * @param iState
     * @return Previously stored InstanceState when put overwrites it, null otherwise. 
     */
    RunEntryState put(long iNum, RunEntryState iState) {
        return map.put(Long.valueOf(iNum), iState);
    }
    
    /**
     * 
     * @param iNum
     * @return
     */
    RunEntryState get(long iNum) {
        return map.get(Long.valueOf(iNum));
    }
    
    /**
     * 
     * @note for iNum not found, nothing happens
     * @param iNum
     * @return The removed InstanceState
     */
    RunEntryState remove(long iNum) {
        return map.remove(Long.valueOf(iNum));
    }

}