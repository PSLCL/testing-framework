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

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class DeleteFuture implements Callable<Void>
{
    private final static String Service = "process";
    private final static String Request = "start shell command ";

    private final Logger log;
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final boolean windows;
    private final ResourceCoordinates coordinates;
    private final String s3Bucket;
    private final String loggingSourceFolder;

    public DeleteFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, boolean windows, ResourceCoordinates coordinates, String s3Bucket, String loggingSourceFolder)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.windows = windows;
        this.coordinates = coordinates;
        this.s3Bucket = s3Bucket;
        this.loggingSourceFolder = loggingSourceFolder;
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
            format.ttl("windows = ", windows);
            log.debug(format.toString());
        }
    }

    @Override
    public Void call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("DeleteFuture");
        ProcessCommandData commandData = DeployFuture.getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows, coordinates, s3Bucket, loggingSourceFolder);
        if(windows)
            issueRequest("rd " + commandData.getFdn() + " /s /q");
        else
            issueRequest("sudo " + "rm -rf" + commandData.getFdn() + " ");
        Thread.currentThread().setName(tname);
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