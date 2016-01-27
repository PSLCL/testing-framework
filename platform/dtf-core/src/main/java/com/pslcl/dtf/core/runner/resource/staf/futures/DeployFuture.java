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

    public DeployFuture(String host, String linuxSandbox, String winSandbox, String partialDestPath, String sourceUrl, boolean windows)
    {
        this.host = host;
        this.linuxSandbox = linuxSandbox;
        this.winSandbox = winSandbox;
        this.partialDestPath = partialDestPath;
        this.sourceUrl = sourceUrl;
        this.windows = windows;
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
            log.debug(format.toString());
        }
    }

    @Override
    public Void call() throws Exception
    {
        ProcessCommandData cmdData = getCommandPath(partialDestPath, linuxSandbox, winSandbox, windows);
        cmdData.setUseWorkingDir(false);
        String urlFile = StrH.getAtomicName(sourceUrl, '/');            // hashname
        
        cmdData.setHost(host);
        cmdData.setWait(true);
        cmdData.setContext(null);
        
        if(windows)
        {
            cmdData.setCommand("md \"" + cmdData.getBasePath() + "\"");
            StafSupport.issueProcessShellRequest(cmdData);  // if sandbox exists we ignore the error
//            if(!cmdData.isFileOnly())
//            {
//                cmdData = new ProcessCommandData(cmdData);
//                cmdData.setUseWorkingDir(true);
//                cmdData.setCommand("md \"" + cmdData.getBasePath() + "\"");
//                executeStafProcess(cmdData, false);
//            }
            cmdData = new ProcessCommandData(cmdData);
            cmdData.setUseWorkingDir(true);
            
            StringBuilder cmd = new StringBuilder()
                            .append("wget -Outfile ")
                            .append(urlFile)
                            .append(" ")
                            .append(sourceUrl);
            cmdData.setCommand(cmd.toString());
            executeStafProcess(cmdData, true, true);
            
            cmdData = new ProcessCommandData(cmdData);
            cmdData.setCommand("ren " + urlFile + " " + cmdData.getFileName());
            executeStafProcess(cmdData, false, true);
            return null;
        }
        // linux
        cmdData.setCommand("sudo mkdir -p " + cmdData.getBasePath());
        executeStafProcess(cmdData, false, true);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setUseWorkingDir(true);
        cmdData.setCommand("sudo wget -O " + urlFile + " " + sourceUrl);
        executeStafProcess(cmdData, false, false);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setCommand("sudo mv " + urlFile + " " + cmdData.getFileName());
        executeStafProcess(cmdData, false, true);
        
        cmdData = new ProcessCommandData(cmdData);
        cmdData.setCommand("sudo chmod 777 " + cmdData.getFileName());
        executeStafProcess(cmdData, false, true);
        return null;
    }

    private void executeStafProcess(ProcessCommandData cmdData, boolean powershell, boolean failOnServiceError) throws Exception
    {
        StafRunnableProgram runnableProgram = null;
        if(powershell)
            runnableProgram = StafSupport.issueProcessPowershellRequest(cmdData);
        else
            runnableProgram = StafSupport.issueProcessShellRequest(cmdData);
        if(runnableProgram.getRunResult() != 0)
        {
            if(failOnServiceError)
                throw new Exception("Staf requested process failed: " + runnableProgram.result.getCompletionSysErr());
            log.warn("Staf requested process failed: " + runnableProgram.result.getCompletionSysErr());
        }
    }
    
    public static ProcessCommandData getCommandPath(String partialDestPath, String linuxSandbox, String winSandbox, boolean windows) throws Exception
    {
        // where partialDestPath is one of these three
        // 1. lib/someApp.jar
        // 2. topLevelFile
        // 3. fdn given don't prepend sandbox
        
        // deal with partialDestPath as if linux for now
        String path = partialDestPath;                                  // lib/someApp.jar, topLevelFile, /opt/dtf/sandbox/doit.sh or c:\opt\dtf\sandbox\doit.sh
        String penultimate = partialDestPath.replace('\\', '/');        // lib/someApp.jar, topLevelFile, /opt/dtf/sandbox/doit.sh 
        String fileName = StrH.getAtomicName(penultimate, '/');         // someApp.jar, topLevelFile, doit.sh
        boolean fileOnly = penultimate.equals(fileName);                // false, true, false
        boolean fdn = penultimate.contains(":");                        // false, false, true windows - false linux
        if(!fdn)
            fdn = penultimate.startsWith("/");                          // false, false, true
        if(!fdn)
        {
            penultimate = StrH.getPenultimateNameFromPath(penultimate); // lib, null
            if(penultimate == null)
                penultimate = "";                                       // lib, ""
            else
                penultimate += "/";                                     // lib/, ""
            if(windows)
            {
                penultimate = penultimate.replace('/', '\\');           // lib\someApp.jr, topLevelFile 
                path = StrH.addTrailingSeparator(winSandbox, '\\');     // c:\opt\dtf\sandbox\
            }else
                path = StrH.addTrailingSeparator(linuxSandbox, '/');     // /opt/dtf/sandbox\
                
            if (!fileOnly)
                path = StrH.addTrailingSeparator(path + penultimate, '/');  // /opt/dtf/sandbox/lib/, /opt/dtf/sandbox/
        }
        path = StrH.stripTrailingSeparator(path);        
        return new ProcessCommandData((windows ? winSandbox : linuxSandbox), path, fileName, fdn, fileOnly);
    }
}