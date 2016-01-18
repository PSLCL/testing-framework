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

import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeployFuture.CommandData;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class RunFuture implements Callable<RunnableProgram>
{
    private final Logger log;
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final boolean start;
    private final boolean windows;
    private final Object context;

    public RunFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, boolean start, boolean windows, Object context)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.start = start;
        this.windows = windows;
        this.context = context;
        log = LoggerFactory.getLogger(getClass());
        if (log.isDebugEnabled())
        {
            TabToLevel format = new TabToLevel();
            format.ttl(getClass().getSimpleName());
            format.level.incrementAndGet();
            format.ttl("host = ", host);
            format.ttl("linuxSandboxPath = ", linuxSandbox);
            format.ttl("winSandboxPath = ", winSandbox);
            format.ttl("partialDestPath = ", partialDestPath);
            format.ttl("start = ", start);
            format.ttl("windows = ", windows);
            LoggerFactory.getLogger(getClass()).debug(format.toString());
        }
    }

    @Override
    public RunnableProgram call() throws Exception
    {
        CommandData commandData = DeployFuture.getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows);
        String sudo = "sudo ";
        if (windows)
            sudo = "";
        return StafSupport.issueProcessRequest(host, sudo + commandData.getFdn(), true, context);
    }
}