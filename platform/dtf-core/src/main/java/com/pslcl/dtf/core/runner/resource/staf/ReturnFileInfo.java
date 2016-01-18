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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ReturnFileInfo
{
    private final static int NumberOfKeys = 2;

    private final HashMap<String, String> map;
    private final CompletionInfo compInfo;
    public final List<String> keys;
    public final List<String> values;
    public final AtomicInteger keyCount;
    public final AtomicInteger valueCount;

    public ReturnFileInfo(CompletionInfo compInfo)
    {
        map = new HashMap<String, String>();
        this.compInfo = compInfo;
        keys = new ArrayList<String>(NumberOfKeys);
        keys.add("rc");
        keys.add("data");
        values = new ArrayList<String>(NumberOfKeys);
        keyCount = new AtomicInteger(0);
        valueCount = new AtomicInteger(0);
    }

    /**
     * @param value
     * @return true if data is full
     */
    public boolean setValue(String value)
    {
        map.put(keys.get(valueCount.get()), value);
        if (valueCount.incrementAndGet() > NumberOfKeys)
            return true;
        return false;
    }

    public void setKeyValue(String key) throws Exception
    {
        switch (compInfo.state)
        {
            case MapClass:
            case CompInfo:
            case CompInfoKeys:
            case DisplayName:
            case KeyName:
            case KeyValue:
            case EndCompInfo:
            case StartData:
                break;
            case RfiKeys:
                if (!key.equals(CompletionInfo.StafStructKeys))
                    throw new Exception("expected: " + CompletionInfo.StafStructKeys + " rec: " + key);
                compInfo.state = CompletionInfo.State.RfiDisplayName;
                break;
            case RfiDisplayName:
                if (!key.equals(CompletionInfo.StafDisplayName))
                    throw new Exception("expected: " + CompletionInfo.StafDisplayName + " rec: " + key);
                compInfo.state = CompletionInfo.State.RfiKeyName;
                break;
            case RfiKeyName:
                switch (keyCount.get())
                {
                    case 0:
                        if (!key.equals(CompletionInfo.StafCompInfoParam1Name))
                            throw new Exception("expected: " + CompletionInfo.StafCompInfoParam1Name + " rec: " + key);
                        break;
                    case 1:
                        if (!key.equals(CompletionInfo.StafRfiParam2Name))
                            throw new Exception("expected: " + CompletionInfo.StafCompInfoParam2Name + " rec: " + key);
                        break;
                }
                compInfo.state = CompletionInfo.State.RfiKeyValue;
                break;
            case RfiKeyValue:
                switch (keyCount.get())
                {
                    case 0:
                        if (!key.equals(CompletionInfo.StafCompInfoParam1Value))
                            throw new Exception("expected: " + CompletionInfo.StafCompInfoParam1Value + " rec: " + key);
                        break;
                    case 1:
                        if (!key.equals(CompletionInfo.StafRfiParam2Value))
                            throw new Exception("expected: " + CompletionInfo.StafCompInfoParam2Value + " rec: " + key);
                        break;
                }
                keys.add(key);
                if (keyCount.incrementAndGet() < NumberOfKeys)
                    compInfo.state = CompletionInfo.State.RfiDisplayName;
                else
                    compInfo.state = CompletionInfo.State.RfiEnd;
                break;
            case RfiEnd:
                if (!key.equals(CompletionInfo.StafReturnFileInfoMapClass))
                    throw new Exception("expected: " + CompletionInfo.StafReturnFileInfoMapClass + " rec: " + key);
                compInfo.state = CompletionInfo.State.StartData;
                break;
            default:
                break;
        }
    }
    
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }

    public TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName() + ": ");
        format.level.incrementAndGet();
        for(String key : keys)
            format.ttl(key, ": ", map.get(key));
        format.level.decrementAndGet();
        return format;
    }
}
