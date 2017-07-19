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
import java.util.concurrent.ExecutorService;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ConfigureFuture implements Callable<RunnableProgram>
{
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final boolean windows;
    private final ResourceCoordinates coordinates;
    private final String s3Bucket;
    private final String logFolder;
    private final MachineInstance machineInstance;
    private final ExecutorService executor;

    public ConfigureFuture(String host, String linuxSandbox, String winSandbox, ExecutorService executor, String partialDestPath, boolean windows, ResourceCoordinates coordinates, String s3Bucket, String logFolder, MachineInstance machineInstance)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.windows = windows;
        this.coordinates = coordinates;
        this.s3Bucket = s3Bucket;
        this.logFolder = logFolder;
        this.machineInstance = machineInstance;
        this.executor = executor;
        Logger log = LoggerFactory.getLogger(getClass());
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
    public RunnableProgram call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("ConfigureFuture");
        ProcessCommandData cmdData = DeployFuture.getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows, coordinates, s3Bucket, logFolder);
        cmdData.setHost(host);
        cmdData.setWait(true);
        cmdData.setContext(machineInstance);
        cmdData.setUseWorkingDir(true);
        if (windows)
        {
            cmdData.setCommand(cmdData.getFileName());
            Thread.currentThread().setName(tname);
            StafRunnableProgram srp = StafSupport.issueProcessShellRequest(cmdData);
            srp.setExecutorService(executor);
            return srp;
        }
        cmdData.setCommand("sudo ./" + cmdData.getFileName());
        StafRunnableProgram srp = StafSupport.issueProcessShellRequest(cmdData);
        srp.setExecutorService(executor);
        Thread.currentThread().setName(tname);
        return srp;
    }
}