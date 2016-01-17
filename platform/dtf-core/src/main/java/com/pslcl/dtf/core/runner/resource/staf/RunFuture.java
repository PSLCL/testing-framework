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

import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class RunFuture implements Callable<RunnableProgram>
{
    private final String host;
    private final String sandboxPath;
    private final String partialDestPath;
    private final boolean start;
    private final boolean windows;
    private final Object context;

    public RunFuture(String host, String sandboxPath, String partialDestPath, boolean start, boolean windows, Object context)
    {
        this.host = host;
        this.sandboxPath = windows ? sandboxPath.replace('/', '\\') : sandboxPath.replace('\\', '/');
        this.partialDestPath = partialDestPath;
        this.start = start;
        this.windows = windows;
        this.context = context;
        TabToLevel format = new TabToLevel();
        format.ttl(getClass().getSimpleName());
        format.level.incrementAndGet();
        format.ttl("host = ", host);
        format.ttl("sandboxPath = ", sandboxPath);
        format.ttl("partialDestPath = ", partialDestPath);
        LoggerFactory.getLogger(getClass()).debug(format.toString());
    }

    @Override
    public RunnableProgram call() throws Exception
    {
        String path = getCommandPath(partialDestPath, sandboxPath, windows);
        String sudo = "sudo ";
        if(windows)
            sudo="";
        return StafSupport.issueProcessRequest(host, sudo + path, true, context);
    }
    
    public static String getCommandPath(String partialDestPath, String sandboxPath, boolean windows) throws Exception
    {
        // where partialDestPath is one of these three
        // 1. lib/someApp.jar
        // 2. topLevelFile
        // 3. fdn given don't prepend sandbox
        String penultimate = partialDestPath.replace('\\', '/');        // lib/someApp.jar, topLevelFile, /opt/dtf/sandbox/doit.sh 
        String fileName = StrH.getAtomicName(penultimate, '/');         // someApp.jar, topLevelFile, doit.sh
        boolean fileOnly = penultimate.equals(fileName);                // false, true, false
        boolean fdn = penultimate.contains(":");                        // false, false, true windows - false linux
        if(!fdn)
            fdn = penultimate.startsWith("/");                          // false, false, true
        if(!fdn)
        {
            penultimate = StrH.getPenultimateNameFromPath(penultimate);     // lib, null
            if(penultimate == null)
                penultimate = "";                                           // lib, ""
            else
                penultimate += "/";                                         // lib/, ""
        }
        String path = penultimate;                                          // lib/, "", /opt/dtf/sandbox/doit.sh
        if (!fdn)
        {
            path = StrH.addTrailingSeparator(sandboxPath, '/');             // /opt/dtf/sandbox/
            if (!fileOnly)
                path = StrH.addTrailingSeparator(path + penultimate, '/');  // /opt/dtf/sandbox/lib/, /opt/dtf/sandbox/
        }
        path += penultimate + fileName;
        if(windows)
            path = path.replace('/', '\\');
        return path;
    }
    
}