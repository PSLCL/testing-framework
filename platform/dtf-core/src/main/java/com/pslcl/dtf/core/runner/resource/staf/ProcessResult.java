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
import java.util.List;
import java.util.Map;

import com.ibm.staf.STAFMarshallingContext;
import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ProcessResult
{
    public final static String StafMapClassKey = "staf-map-class-name";
    public final static String StafMapClassMap = "map-class-map";
    public final static String StafProcessMapClass = "STAF/Service/Process/CompletionInfo";
    public final static String StafRcKey = "rc";
    public final static String StafKeyKey = "key";
    public final static String StafFileListKey = "fileList";
    public final static String StafFileDataKey = "data";

    public final STAFResult result;
    public StopResult stopResult;
    public final boolean wait;
    public volatile int rc;
    public volatile String key;
    public volatile List<ReturnFileStreams> fileList;
    //    public final CompletionInfo complInfo;

    public ProcessResult(STAFResult result, boolean wait) throws Exception
    {
        this.result = result;
        this.wait = wait;
        if (checkMap(result.resultContext.getRootObject()))
            return;
        // must be a string result
        rc = result.rc;
        key = null;
        fileList = null;
        if (STAFMarshallingContext.isMarshalledData(result.result))
            checkMap(STAFMarshallingContext.unmarshall(result.result));
    }

    public synchronized StopResult getStopResult()
    {
        return stopResult;
    }

    public synchronized void setStopResult(StopResult stopResult)
    {
        this.stopResult = stopResult;
    }

    public Integer getStopCcode()
    {
        if(stopResult == null)
            return null;
        return stopResult.ccode;
    }
    
    public String getCompletionSysOut()
    {
        if(fileList == null || fileList.size() < 1)
            return null;
        ReturnFileStreams sysOut = fileList.get(0);
        if(sysOut != null)
            return sysOut.data;
        return null;
    }
    
    public String getCompletionSysErr()
    {
        if(fileList == null || fileList.size() < 2)
            return null;
        ReturnFileStreams sysErr = fileList.get(1);
        if(sysErr != null)
            return sysErr.data;
        return null;
    }
    
    public int getServiceCcode()
    {
        if(fileList == null || fileList.size() < 2)
            return result.rc;
        return rc;
    }
    
    public String getServiceHandle()
    {
        return result.result;
    }
    
    @SuppressWarnings("rawtypes")
    private boolean checkMap(Object rootObject) throws Exception
    {
        if (!(rootObject instanceof Map))
            return false;
        Map map = (Map) result.resultContext.getRootObject();
        if (!map.get(StafMapClassKey).equals(StafProcessMapClass))
            throw new Exception("Staf unexpected Process Result Map Class");
        rc = Integer.parseInt((String) map.get(StafRcKey));
        key = (String) map.get(StafKeyKey);
        List<ReturnFileStreams> myFiles = null;
        List returnedFiles = (List) map.get(StafFileListKey);
        if (returnedFiles != null)
        {
            myFiles = new ArrayList<ReturnFileStreams>();
            for (int i = 0; i < returnedFiles.size(); i++)
                myFiles.add(new ReturnFileStreams((Map) returnedFiles.get(i), i));
        }
        this.fileList = myFiles;
        return true;
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
        format.ttl("rc = ", StafSupport.getResultRc(result, false, null));
        format.ttl("result = ", result.result);
        format.ttl("key = ", key);
        format.ttl("serviceCcode = ", getServiceCcode());
        format.ttl("stopCcode = ", getStopCcode());
        format.ttl("fileList:");
        format.level.incrementAndGet();
        if (fileList != null)
        {
            for (ReturnFileStreams fi : fileList)
                fi.toString(format);
        } else
            format.ttl("null");
        format.level.decrementAndGet();
        format.level.decrementAndGet();
        return format;
    }

    public class ReturnFileStreams
    {
        public final int streamIdx;
        public final int rc;
        public final String data;

        @SuppressWarnings("rawtypes")
        private ReturnFileStreams(Map fileMap, int streamIdx)
        {
            rc = Integer.parseInt((String) fileMap.get(StafRcKey));
            data = (String) fileMap.get(StafFileDataKey);
            this.streamIdx = streamIdx;
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
            format.ttl(getClass().getSimpleName() + ": ", (streamIdx == 0 ? "stdout" : "stderr"));
            format.level.incrementAndGet();
            format.ttl("rc: " + rc);
            format.ttl("data: ", data);
            format.level.decrementAndGet();
            return format;
        }
    }
}
