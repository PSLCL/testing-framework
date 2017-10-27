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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RunEntryStateStore {

    /**
     *  Used for pure storage, order of element access is determined by other means.
     *  Neither Long key nor InstanceState value may be null.
     *  Writes by any one thread locks only the relevant element, other threads freely access other elements
     */
    private Map<Long, RunEntryState> map;

    public RunEntryStateStore() {
        map = new ConcurrentHashMap<>();
    }

    /**
     *
     * @param configurationMaxSize The stored, configured max size to compare against the Run Entry State storage map.
     * @return Whether or not the stored max size or limit is reached.
     */
    public boolean isMaxSizeReached(int configurationMaxSize) {
        return configurationMaxSize <= map.size();
    }

    /**
     *
     * Note: Put to an existing Long key overwrites previous the previously held InstanceState
     * @param reNum The test run identifier
     * @param reState The run entry state
     */
    void put(long reNum, RunEntryState reState) {
        map.put(reNum, reState);
    }

    /**
     *
     * @param reNum The test run identifier
     * @return The RunEntryState of the specified test run.
     */
    RunEntryState get(long reNum) { return map.get(reNum); }

    /**
     * Remove the run entry state of the specified test run.
     *
     * @param reNum The test run identifier
     */
    void remove(long reNum) { map.remove(reNum); }

}