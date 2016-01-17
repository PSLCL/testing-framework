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

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class DeleteFuture implements Callable<Void>
{
    private final static String Service = "process";
    private final static String Request = "start shell command ";
    private final String host;
    private final String sandboxPath;
    private final String partialDestPath;
    private final boolean windows;

    public DeleteFuture(String host, String sandboxPath, String partialDestPath, boolean windows)
    {
        this.host = host;
        this.sandboxPath = windows ? sandboxPath.replace('/', '\\') : sandboxPath.replace('\\', '/');
        this.partialDestPath = partialDestPath;
        this.windows = windows;
        TabToLevel format = new TabToLevel();
        format.ttl(getClass().getSimpleName());
        format.level.incrementAndGet();
        format.ttl("host = ", host);
        format.ttl("sandboxPath = ", sandboxPath);
        format.ttl("partialDestPath = ", partialDestPath);
        LoggerFactory.getLogger(getClass()).debug(format.toString());
    }

    @Override
    public Void call() throws Exception
    {
        // where partialDestPath is one of these two
        // 1. lib/someApp.jar
        // 2. topLevelFile
        String penultimate = partialDestPath.replace('\\', '/');        // lib/someApp.jar, topLevelFile
        String fileName = StrH.getAtomicNameFromPath(penultimate);      // someApp.jar, topLevelFile
        boolean fileOnly = penultimate.equals(fileName);                // false, true
        penultimate = StrH.getPenultimateNameFromPath(penultimate);     // lib, topLevelFile 
        String path = StrH.addTrailingSeparator(sandboxPath, windows ? '\\' : '/'); // /opt/dtf/sandbox/
        if(windows)
            penultimate = penultimate.replace('/', '\\');
        if(!fileOnly)
            path = StrH.addTrailingSeparator(path + penultimate, windows ? '\\' : '/');  // /opt/dtf/sandbox/lib/, /opt/dtf/sandbox/  
        String newName = path + fileName;                               // /opt/dtf/sandbox/lib/someApp.jar, /opt/dtf/sandbox/topLevelFile 
        String sudo = "sudo ";
        if(windows)
            sudo="";
        issueRequest(sudo + "rm " + newName);
        return null;
    }
    
    private void issueRequest(String clParams) throws Exception
    {
        //@formatter:off
        StringBuilder sb = new StringBuilder(Request)
                        .append("\"")
                        .append(clParams)
//                        .append("\"");
                        .append("\" wait");
        //@formatter:on
        StafSupport.request(host, Service, sb.toString());
    }
}