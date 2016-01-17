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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ProcessResult
{
    private final Logger log;
    public final STAFResult result;
    public final int rc;
    public final String key;
    public final List<ReturnFileInfo> fileList;

    @SuppressWarnings("rawtypes")
    public ProcessResult(STAFResult result) throws Exception
    {
        log = LoggerFactory.getLogger(getClass());
        try
        {
            this.result = result;
            Object rootObject =  result.resultContext.getRootObject();
            if(!(rootObject instanceof Map))
                throw new Exception(getClass().getSimpleName() + " invalid rootObject type: " + rootObject.toString());
            Map map = (Map) result.resultContext.getRootObject();
            if (!map.get(StafSupport.StafMapClassKey).equals(StafSupport.StafProcessMapClassDefault))
                throw new Exception("Staf unexpected Process Result Map Class");
            rc = Integer.parseInt((String) map.get(StafSupport.StafRcKey));
            key = (String) map.get(StafSupport.StafKeyKey);
            List<ReturnFileInfo> myFiles = null;
            List returnedFiles = (List)map.get(StafSupport.StafFileListKey);
            if(returnedFiles != null)
            {
                myFiles = new ArrayList<ReturnFileInfo>();
                for(int i=0; i < returnedFiles.size(); i++)
                    myFiles.add(new ReturnFileInfo((Map)returnedFiles.get(i), i));
            }
            this.fileList = myFiles;
            
        } catch (Exception e)
        {
            log.info("\n***** ProcessResult exception: " + e.getClass().getName() + " : " + e.getMessage());
            throw e;
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
        format.ttl(getClass().getSimpleName() + ":");
        format.level.incrementAndGet();
        format.ttl("rc = " + rc);
        format.ttl("key = ", key);
        format.ttl("fileList:");
        format.level.incrementAndGet();
        if(fileList != null)
        {
            for(ReturnFileInfo fi : fileList)
                fi.toString(format);
        }else
            format.ttl("null");
        format.level.decrementAndGet();
        format.level.decrementAndGet();
        return format;
    }
    
    public class ReturnFileInfo
    {
        public final int streamIdx;
        public final int rc;
        public final String data;

        @SuppressWarnings("rawtypes")
        private ReturnFileInfo(Map fileMap, int streamIdx)
        {
            rc = Integer.parseInt((String) fileMap.get(StafSupport.StafRcKey));
            data = (String) fileMap.get(StafSupport.StafFileDataKey);
            this.streamIdx = streamIdx;
        }
        
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

