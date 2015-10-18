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
package com.pslcl.qa.runner.config.util;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("javadoc")
public class TabToLevel
{
    public final StringBuilder sb;
    public final AtomicInteger level;
    
    public TabToLevel(StringBuilder sbIn)
    {
        if(sbIn == null)
            sbIn = new StringBuilder();
        sb = sbIn;
        level = new AtomicInteger(0);
    }
    
    public void ttl(Object ... values)
    {
        String[] array = new String[values.length];
        for(int i=0; i < array.length; i++)
            array[i] = (values[i] == null ? "null" : values[i].toString());
        tabToLevel(true, array);
    }

    public void tabToLevel(boolean eol, String ... values)
    {
        for(int i=0; i < level.get(); i++)
            sb.append("\t");
        for(int j=0; j < values.length; j++)
            sb.append(values[j]);
        if(eol)
            sb.append("\n");
    }
    
    public void indentedOk()
    {
        level.incrementAndGet();
        ttl("ok");
        level.decrementAndGet();
    }
    
}
