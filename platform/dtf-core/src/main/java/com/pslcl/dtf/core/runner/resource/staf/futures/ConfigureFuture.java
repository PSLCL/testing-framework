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
package com.pslcl.dtf.core.runner.resource.staf.futures;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeployFuture.CommandData;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ConfigureFuture implements Callable<Integer>
{
    private final Logger log;
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final boolean windows;
    private final Object context;

    public ConfigureFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, boolean windows, Object context)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.windows = windows;
        this.context = context;
        log = LoggerFactory.getLogger(getClass());
        if (log.isDebugEnabled())
        {
            TabToLevel format = new TabToLevel();
            format.ttl(getClass().getSimpleName());
            format.level.incrementAndGet();
            format.ttl("host = ", host);
            format.ttl("linuxSandbox = ", linuxSandbox);
            format.ttl("winSandbox = ", winSandbox);
            format.ttl("partialDestPath = ", partialDestPath);
            format.ttl("windows = ", windows);
            log.debug(format.toString());
        }
    }

    @Override
    public Integer call() throws Exception
    {
        CommandData commandData = DeployFuture.getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows);
        String sudo = "sudo ";
        if (windows)
            sudo = "";
//        StafSupport.issueProcessRequest(host, sudo + "md " + commandData.basePath, true, null);
        StafSupport.issueProcessRequest(host, sudo + "xxdir", true, null);
        return 1;
//        return StafSupport.issueProcessRequest(host, sudo + commandData.getFdn(), true, context).getRunResult();
    }
}