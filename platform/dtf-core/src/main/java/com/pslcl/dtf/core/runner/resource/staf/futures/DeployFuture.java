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
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.StafSupport;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class DeployFuture implements Callable<Void>
{
    private final Logger log;
    private final String host;
    private final String linuxSandbox;
    private final String winSandbox;
    private final String partialDestPath;
    private final String sourceUrl;
    private final boolean windows;
    private final ResourceCoordinates coordinates;
    private final String s3Bucket;
    private final int retries;

    public DeployFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, String sourceUrl, boolean windows, ResourceCoordinates coordinates, String s3Bucket, String loggingSourceFolder, int retries)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.sourceUrl = sourceUrl;
        this.windows = windows;
        this.coordinates = coordinates;
        this.s3Bucket = s3Bucket;
        this.retries = retries;
        log = LoggerFactory.getLogger(getClass());
        if (log.isDebugEnabled())
        {
            TabToLevel format = new TabToLevel();
            format.ttl("\n", getClass().getSimpleName(),":");
            format.level.incrementAndGet();
            format.ttl("host = ", host);
            format.ttl("linuxSandboxPath = ", linuxSandbox);
            format.ttl("winSandboxPath = ", winSandbox);
            format.ttl("partialDestPath = ", partialDestPath);
            format.ttl("sourceUrl = ", sourceUrl);
            format.ttl("windows = ", windows);
            format.ttl("runId = ", coordinates.getRunId());
            format.ttl("s3Bucket = ", s3Bucket);
            format.ttl("loggingSourceFolder = ", loggingSourceFolder);
            log.debug(format.toString());
        }
    }

    @Override
    public Void call() throws Exception
    {
        String tname = Thread.currentThread().getName();
        Thread.currentThread().setName("DeployFuture");
        ProcessCommandData cmdData = getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows, coordinates, s3Bucket, null);
        cmdData.setUseWorkingDir(false);
        String urlFile = StrH.getAtomicName(sourceUrl, '/');            // hashname
        
        cmdData.setHost(host);
        cmdData.setWait(true);
        cmdData.setContext(null);
        
        if(windows)
        {
            cmdData.setCommand("md \"" + cmdData.getBasePath() + "\"");
            StafSupport.issueProcessShellRequest(cmdData);  // if sandbox exists we ignore the error
            cmdData = new ProcessCommandData(cmdData);
            cmdData.setUseWorkingDir(true);

            String cmd = "wget -Outfile " + urlFile + " " + sourceUrl;
            cmdData.setCommand(cmd);
            executeStafProcess(cmdData, true, true);
            
            cmdData = new ProcessCommandData(cmdData);
            cmdData.setCommand("ren " + urlFile + " " + cmdData.getFileName());
            executeStafProcess(cmdData, false, true);
            Thread.currentThread().setName(tname);
            return null;
        }
        // linux
        cmdData.setCommand("sudo mkdir -p " + cmdData.getBasePath());
        executeStafProcess(cmdData, false, true);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setUseWorkingDir(true);
        cmdData.setCommand("sudo wget -t " + retries + " " + sourceUrl);
        executeStafProcess(cmdData, false, true);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setCommand("sudo mv " + urlFile + " " + cmdData.getFileName());
        executeStafProcess(cmdData, false, true);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setCommand("sudo chmod 777 " + cmdData.getFileName());
        executeStafProcess(cmdData, false, true);
        Thread.currentThread().setName(tname);
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private void executeStafProcess(ProcessCommandData cmdData, boolean powershell, boolean failOnServiceError) throws Exception
    {
        StafRunnableProgram runnableProgram;
        if(powershell)
            runnableProgram = StafSupport.issueProcessPowershellRequest(cmdData);
        else
            runnableProgram = StafSupport.issueProcessShellRequest(cmdData);
        RunnableProgram.RunResult runResult = runnableProgram.getRunResult();
        if(runResult.runResult != null && runResult.runResult != 0)
        {
            if(failOnServiceError)
                throw new Exception("Staf requested process failed: " + runnableProgram.result.getCompletionSysErr());
            log.warn("Staf requested process failed: " + runnableProgram.result.getCompletionSysErr());
        }
    }

    static ProcessCommandData getCommandPath(String partialDestPath, String linuxSandbox, String winSandbox, boolean windows, ResourceCoordinates coordinates, String s3Bucket, String logFolder) throws Exception
    {
        // where partialDestPath is one of these three
        // 1. lib/someApp.jar arg1 arg2
        // 2. topLevelFile arg1 arg2
        // 3. fdn given don't prepend sandbox arg1 arg2

//        linux
//        1. bin/OperationsTest -requestorMode - maxiterations 10 -address addr -testset
//        2. OperationsTest -requestorMode - maxiterations 10 -address addr -testset
//        3. /opt/dtf/sandbox/OperationsTest -requestorMode - maxiterations 10 -address addr -testset
//        windows
//        1. bin\\OperationsTest -requestorMode - maxiterations 10 -address addr -testset
//        2. OperationsTest -requestorMode - maxiterations 10 -address addr -testset
//        3. c:\\opt\\dtf\\sandbox\\OperationsTest -requestorMode - maxiterations 10 -address addr -testset

        String args = "";
        if(partialDestPath == null)
            partialDestPath = "";
        String cmdPath = partialDestPath;
        int idx = partialDestPath.indexOf(' ');
        if(idx != -1)
        {
            args = partialDestPath.substring(idx); // keep space
            cmdPath = partialDestPath.substring(0, idx);
        }
        // deal with cmdPath as if linux for now
        String path;
        String absoluteLogPath = null;
        String penultimate = cmdPath.replace('\\', '/');        // bin/OperationsTest, OperationsTest, /opt/dtf/sandbox/OperationsTest
        String fileName = StrH.getAtomicName(penultimate, '/');         // OperationsTest, OperationsTest, OperationsTest
        boolean fileOnly = penultimate.equals(fileName);                         // false, true, false
        penultimate = StrH.getPenultimateNameFromPath(penultimate);          // bin, null
        if(penultimate == null)
            penultimate = "";                                                // bin, ""
        else
            penultimate += "/";                                              // bin/, ""
        boolean fdn = penultimate.contains(":");                                 // false, false, true windows - false linux
        if(!fdn)
            fdn = penultimate.startsWith("/");                                   // false, false, true
        if(!fdn)
        {
            if(windows)
            {
                penultimate = penultimate.replace('/', '\\');   // lib\someApp.jr, topLevelFile
                path = StrH.addTrailingSeparator(winSandbox + "\\" + coordinates.getRunId(), '\\');     // c:\opt\dtf\sandbox\
                if(logFolder != null)
                    absoluteLogPath = StrH.addTrailingSeparator(path + logFolder, '\\');
            }else
            {
                path = StrH.addTrailingSeparator(linuxSandbox + "/" + coordinates.getRunId(), '/');     // /opt/dtf/sandbox/80/
                if(logFolder != null)
                    absoluteLogPath = StrH.addTrailingSeparator(path + logFolder, '/');
            }

            if (!fileOnly)
                path = StrH.addTrailingSeparator(path + penultimate, '/');  // /opt/dtf/sandbox/lib/, /opt/dtf/sandbox/80/bin/
        }else
        {
            path = penultimate;
            if(windows)
            {
                path = path.replace('/', '\\');   // lib\someApp.jr, topLevelFile
                if(logFolder != null)
                    absoluteLogPath = StrH.addTrailingSeparator(path + logFolder, '\\');
            }
            else
            if(logFolder != null)
                absoluteLogPath = StrH.addTrailingSeparator(path + logFolder, '/');
        }
        path = StrH.stripTrailingSeparator(path);
        fileName = fileName + args;
        return new ProcessCommandData((windows ? winSandbox : linuxSandbox), path, fileName, fdn, fileOnly, coordinates, s3Bucket, absoluteLogPath, windows);
    }
}