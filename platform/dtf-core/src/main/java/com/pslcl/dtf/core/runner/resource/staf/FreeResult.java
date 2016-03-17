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
package com.pslcl.dtf.core.runner.resource.staf;

import java.util.Map;

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class FreeResult
{
    public final static String StoppedProcessesKey = "freedProcesses";
    public final static String TotalProcessesKey = "totalProcesses";

    public final STAFResult result;
    public final String freedProcesses;
    public final String totalProcesses;
    public final int ccode;

    public FreeResult(STAFResult result, boolean byHandle)
    {
        this.result = result;
        if(byHandle)
        {
            freedProcesses = "";
            totalProcesses = "";
            ccode = result.rc;
            return;
        }
        @SuppressWarnings("rawtypes")
        Map map = (Map) result.resultContext.getRootObject();
        freedProcesses = (String) map.get(StoppedProcessesKey);
        totalProcesses = (String) map.get(TotalProcessesKey);
        int ok;
        if(freedProcesses == null || freedProcesses.length() == 0)
            ok = 1;
        int total = Integer.parseInt(freedProcesses);
        if(total == 0)
            ok = 1;
        else
            ok = 0;
        ccode = ok;
    }

    @Override
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }

    public TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName() + ":");
        format.level.incrementAndGet();
        format.ttl("freedProcesses = ", freedProcesses);
        format.ttl("totalProcesses = ", totalProcesses);
        format.level.decrementAndGet();
        return format;
    }
}
