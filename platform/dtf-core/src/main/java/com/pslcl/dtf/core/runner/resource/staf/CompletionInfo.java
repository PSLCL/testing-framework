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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.staf.STAFUtil;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class CompletionInfo
{
    public final static String StafRcKey = "rc";
    public final static String StafKeyKey = "key";
    public final static String StafMapClassKey = "staf-map-class-name";
    public final static String StafFileListKey = "fileList";
    public final static String StafFileDataKey = "data";

    public final static String StafMapClassMap = "map-class-map";
    public final static String StafProcessMapClass = "STAF/Service/Process/CompletionInfo";
    public final static String StafStructKeys = "keys";
    public final static String StafDisplayName = "display-name";
    public final static String StafCompInfoParam1Name = "Return Code";
    public final static String StafCompInfoParam2Name = "Key";
    public final static String StafRfiParam2Name = "Data";
    public final static String StafCompInfoParam3Name = "Files";
    public final static String StafCompInfoParam1Value = "rc";
    public final static String StafCompInfoParam2Value = "key";
    public final static String StafRfiParam2Value = "data";
    public final static String StafCompInfoParam3Value = "fileList";
    public final static String StafCompInfoInnerStruct = "name";
    public final static String StafCompInfoEnd = "STAF/Service/Process/CompletionInfo:35:STAF/Service/Process/ReturnFileInfo";

    public final static String StafReturnFileInfoMapClass = "STAF/Service/Process/ReturnFileInfo";
    public final static String StafResultHeader = "@SDT/*";
    public final static String StafResultStructHeader = "@SDT/{";
    public final static String StafResultStruct = "@SDT/[";
    public final static String StafResultParam = "@SDT/$S";
    public final static String StafResultNullParam = "@SDT/$";
    public final static String StafResultEndDecl = "@SDT/%";

    private final static int NumberOfKeys = 3;

    public final List<String> keys;
    public final List<String> values;
    public final List<ReturnFileInfo> fileList;
    private ReturnFileInfo returnFileInfo;
    public final Map<String, String> map;
    public final AtomicInteger keyCount;
    public final AtomicInteger valueCount;
    public State state; // this is single threaded parse, not thread safe 

    public CompletionInfo()
    {
        map = new HashMap<String, String>();
        keys = new ArrayList<String>();
        values = new ArrayList<String>(NumberOfKeys);
        fileList = new ArrayList<ReturnFileInfo>(NumberOfKeys);
        keyCount = new AtomicInteger(0);
        valueCount = new AtomicInteger(0);
        state = State.MapClass;
    }

    public static CompletionInfo unmarshallCompletionInfo(String result) throws Exception
    {
        if (!result.startsWith(CompletionInfo.StafResultHeader))
            throw new Exception("invalid wait response string: " + result);
        CompletionInfo completionInfo = new CompletionInfo();
        CompletionInfo.traverseProcessResult(result, completionInfo);
        return completionInfo;
    }

    private static void traverseProcessResult(String result, CompletionInfo completionInfo) throws Exception
    {
        // @SDT/*:678:  - stripped before first call
        // @SDT/{:587::13:map-class-map
        //     @SDT/{:559::35:STAF/Service/Process/CompletionInfo
        //         @SDT/{:261::4:keys
        //             @SDT/[3:189:
        //                 @SDT/{:56::12:display-name
        //                     @SDT/$S:11:Return Code:3:key
        //                     @SDT/$S:2:rc
        //                 @SDT/{:48::12:display-name
        //                     @SDT/$S:3:Key:3:key
        //                     @SDT/$S:3:key
        //                 @SDT/{:55::12:display-name
        //                     @SDT/$S:5:Files:3:key
        //                     @SDT/$S:8:fileList:4:name
        //                     @SDT/$S:35:STAF/Service/Process/CompletionInfo:35:STAF/Service/Process/ReturnFileInfo
        //                         @SDT/{:198::4:keys
        //                         @SDT/[2:126:
        //                             @SDT/{:56::12:display-name
        //                                 @SDT/$S:11:Return Code:3:key
        //                                 @SDT/$S:2:rc
        //                             @SDT/{:50::12:display-name
        //                                 @SDT/$S:4:Data:3:key
        //                                 @SDT/$S:4:data:4:name
        //                     @SDT/$S:35:STAF/Service/Process/ReturnFileInfo
        //     @SDT/%:70::35:STAF/Service/Process/CompletionInfo
        //         @SDT/$S:1:0
        //         @SDT/$0:0:
        //         @SDT/[0:0:
        //        }
        //        }

        int idx = 0;
        String dstr = null;

        if (result.startsWith(StafResultHeader))
        {
            idx = StafResultHeader.length();
            dstr = result.substring(idx);
            dstr = STAFUtil.unwrapData(result.substring(idx));
            traverseProcessResult(dstr, completionInfo);
        } else if (result.startsWith(StafResultStructHeader))
        {
            idx = StafResultStructHeader.length();
            dstr = unwrapData(result.substring(idx)); // skip the total remaining size
            dstr = unwrapData(dstr); // get structure name 
            traverseProcessResult(dstr, completionInfo);
        } else if (result.startsWith(StafResultStruct))
        {
            if (completionInfo.state == CompletionInfo.State.Data)
            {
                completionInfo.setKeyValue(null);
                return;
            }

            idx = StafResultStruct.length();
            String cntStr = unwrapData(result.substring(idx), true, false); // grab the number of elements
            dstr = unwrapData(result.substring(idx + cntStr.length())); // move to the first element 
            traverseProcessResult(dstr, completionInfo);
        } else if (result.startsWith(StafResultParam))
        {
            idx = StafResultParam.length();
            dstr = unwrapData(result.substring(idx)); // strip the size
            traverseProcessResult(dstr, completionInfo);
            //                     @SDT/$S:11:Return Code:3:key
            //                     @SDT/$S:2:rc
        } else if (result.startsWith(StafResultNullParam))
        {
            idx = StafResultNullParam.length();
            dstr = unwrapData(result.substring(idx)); // strip the size
            idx = dstr.indexOf('@');
            dstr = dstr.substring(idx);
            completionInfo.setKeyValue(null);
            traverseProcessResult(dstr, completionInfo);
            //                     @SDT/$S:11:Return Code:3:key
            //                     @SDT/$S:2:rc
        } else if (result.startsWith(StafResultEndDecl))
        {
            idx = StafResultEndDecl.length();
            dstr = unwrapData(result.substring(idx), false, true); // strip the total size
            idx += dstr.length();
            dstr = unwrapData(result.substring(idx), false, true); // strip the size
            idx += dstr.length();
            traverseProcessResult(result.substring(idx), completionInfo);
            //                     @SDT/$S:11:Return Code:3:key
            //                     @SDT/$S:2:rc
        }
        if (completionInfo.state == CompletionInfo.State.Done)
            return;
        String key = null;
        if (completionInfo.state == CompletionInfo.State.KeyName || completionInfo.state == CompletionInfo.State.RfiKeyName)
        {
            // @SDT/$S:11:Return Code:3:key
            idx = result.indexOf(':');
            key = result.substring(0, idx);
            completionInfo.setKeyValue(key);
            idx = result.indexOf('@'); // skip :3:key altogether    
            traverseProcessResult(result.substring(idx), completionInfo);
        } else if (completionInfo.state == CompletionInfo.State.KeyValue || completionInfo.state == CompletionInfo.State.RfiKeyValue)
        {
            // @SDT/$S:2:rc
            // @SDT/$S:8:fileList:4:name
            // @SDT/$S:35:STAF/Service/Process/CompletionInfo:35:STAF/Service/Process/ReturnFileInfo
            idx = result.indexOf(':');
            int aidx = result.indexOf('@');
            if (idx < aidx)
            {
                key = result.substring(0, idx);
                completionInfo.setKeyValue(key);
                String nameCnt = unwrapData(result.substring(idx), false, true); // skip the "name" size
                idx += nameCnt.length();
                key = result.substring(idx, aidx);
                if (!key.equals(StafCompInfoInnerStruct))
                    throw new Exception("expected: " + StafCompInfoInnerStruct + " rec: " + key);
                idx = aidx; // skipped "name" here
            } else
            {
                key = result.substring(0, aidx);
                completionInfo.setKeyValue(key);
                idx = aidx;
            }
            traverseProcessResult(result.substring(idx), completionInfo);
        } else
        {
            idx = result.indexOf('@');
            if (idx == -1)
                return;
            key = result.substring(0, idx);
            completionInfo.setKeyValue(key);
            traverseProcessResult(result.substring(key.length()), completionInfo);
        }
    }

    public static String unwrapData(String data)
    {
        return unwrapData(data, false, false);
    }

    public static String unwrapData(String data, boolean structArray, boolean structName)
    {
        if (data != null)
        {
            int colon1Pos = data.indexOf(":");
            int c2strOffset = 1;
            if (structArray)
            {
                colon1Pos = 0;
                c2strOffset = 0;
            }
            if (colon1Pos == 0)
            {
                int colon2Pos = data.indexOf(":", c2strOffset);
                if (colon2Pos > -1)
                {
                    if (structName)
                        colon2Pos++;
                    if (structArray || structName)
                        return data.substring(0, colon2Pos);
                    String newValue = data.substring(colon2Pos + c2strOffset);
                    return newValue;
                }
            }
        }
        return data;
    }

    public void setKeyValue(String key) throws Exception
    {
        switch (state)
        {
            case MapClass:
                if (!key.equals(StafMapClassMap))
                    throw new Exception("expected: " + StafMapClassMap + " rec: " + key);
                state = State.CompInfo;
                break;
            case CompInfo:
                if (!key.equals(StafProcessMapClass))
                    throw new Exception("expected: " + StafProcessMapClass + " rec: " + key);
                state = State.CompInfoKeys;
                break;
            case CompInfoKeys:
                if (!key.equals(StafStructKeys))
                    throw new Exception("expected: " + StafStructKeys + " rec: " + key);
                state = State.DisplayName;
                break;
            case DisplayName:
                if (!key.equals(StafDisplayName))
                    throw new Exception("expected: " + StafDisplayName + " rec: " + key);
                state = State.KeyName;
                break;
            case KeyName:
                switch (keyCount.get())
                {
                    case 0:
                        if (!key.equals(StafCompInfoParam1Name))
                            throw new Exception("expected: " + StafCompInfoParam1Name + " rec: " + key);
                        break;
                    case 1:
                        if (!key.equals(StafCompInfoParam2Name))
                            throw new Exception("expected: " + StafCompInfoParam2Name + " rec: " + key);
                        break;
                    case 2:
                        if (!key.equals(StafCompInfoParam3Name))
                            throw new Exception("expected: " + StafCompInfoParam3Name + " rec: " + key);
                        break;
                }
                state = State.KeyValue;
                break;
            case KeyValue:
                switch (keyCount.get())
                {
                    case 0:
                        if (!key.equals(StafCompInfoParam1Value))
                            throw new Exception("expected: " + StafCompInfoParam1Value + " rec: " + key);
                        break;
                    case 1:
                        if (!key.equals(StafCompInfoParam2Value))
                            throw new Exception("expected: " + StafCompInfoParam2Value + " rec: " + key);
                        break;
                    case 2:
                        if (!key.equals(StafCompInfoParam3Value))
                            throw new Exception("expected: " + StafCompInfoParam3Value + " rec: " + key);
                        break;
                }
                keys.add(key);
                if (keyCount.incrementAndGet() < NumberOfKeys)
                    state = State.DisplayName;
                else
                    state = State.EndCompInfo;
                break;
            case EndCompInfo:
                if (!key.equals(StafCompInfoEnd))
                    throw new Exception("expected: " + StafCompInfoEnd + " rec: " + key);
                returnFileInfo = new ReturnFileInfo(this);
                fileList.add(returnFileInfo);
                state = State.RfiKeys;
                break;
            case StartData:
                if (!key.equals(StafProcessMapClass))
                    throw new Exception("expected: " + StafProcessMapClass + " rec: " + key);
                state = State.Data;
                break;
            case Data:
                // @SDT/$S:1:0  rc
                // @SDT/$0:0:   key
                // @SDT/[0:0:   files
                if (valueCount.get() < NumberOfKeys)
                {
                    map.put(keys.get(valueCount.get()), key);
                    valueCount.incrementAndGet();
                    if (valueCount.get() >= NumberOfKeys)
                        state = State.Done;
                } else
                {
                    returnFileInfo.setValue(key); //TODO: list of rfi's not handled yet
                }
                break;
            default:
                returnFileInfo.setKeyValue(key);
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
        if(fileList != null)
        {
            format.level.incrementAndGet();
            for(ReturnFileInfo rfi : fileList)
                rfi.toString(format);
            format.level.decrementAndGet();
        }
        format.level.decrementAndGet();
        return format;
    }
    
    public enum State
    {
        MapClass, CompInfo, CompInfoKeys, DisplayName, KeyName, KeyValue, EndCompInfo, RfiKeys, RfiDisplayName, RfiKeyName, RfiKeyValue, RfiEnd, StartData, Data, Done;
    }

}
