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

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ProcessCommandData
{
    public final static String ProcessShellRequest = "start shell command ";
    public final static String ProcessPowershellRequest = "start shell \"powershell.exe %c\" command ";
    
    private boolean fdn;
    private boolean fileOnly;
    private String sandbox;
    private String basePath;
    private String fileName;
    private String host;
    private String command;
    private boolean wait;
    private int timeout;
    private Object context;
    private boolean useWorkingDir;
    private String s3Bucket;
    private String logSourceFolder;
    private ResourceCoordinates coordinates;
    private boolean windows;

    public ProcessCommandData(String sandbox, String basePath, String fileName, boolean fdn, boolean fileOnly, ResourceCoordinates coordinates, String s3Bucket, String logSourceFolder, boolean windows)
    {
        this.sandbox = sandbox;
        this.basePath = basePath;
        this.fileName = fileName;
        this.fdn = fdn;
        this.fileOnly = fileOnly;
        this.coordinates = coordinates;
        this.s3Bucket = s3Bucket;
        this.logSourceFolder = logSourceFolder;
        this.windows = windows;
    }

    public ProcessCommandData(ProcessCommandData commandData)
    {
        fdn = commandData.isFdn();
        fileOnly = commandData.isFileOnly();
        sandbox = commandData.getSandbox();
        basePath = commandData.getBasePath();
        fileName = commandData.getFileName();
        host = commandData.getHost();
        command = commandData.getCommand();
        wait = commandData.isWait();
        timeout = commandData.getTimeout();
        context = commandData.getContext();
        useWorkingDir = commandData.isUseWorkingDir();
        coordinates = commandData.coordinates;
        s3Bucket = commandData.s3Bucket;
        logSourceFolder = commandData.logSourceFolder;
        windows = commandData.windows;
    }

    public synchronized boolean isFdn()
    {
        return fdn;
    }

    public synchronized String getFdn()
    {
        char separator = '\\';
        int idx = basePath.indexOf('/');
        if(idx != -1)
            separator = '/';
        return StrH.addTrailingSeparator(basePath, separator) + fileName;
    }

    public synchronized boolean isWindows() {return windows;}

    public synchronized ResourceCoordinates getCoordinates() {return coordinates;}

    public synchronized String getS3Bucket() {return s3Bucket;}

    public synchronized String getLogSourceFolder() {return logSourceFolder;}

    public synchronized boolean isFileOnly()
    {
        return fileOnly;
    }

    public synchronized boolean isUseWorkingDir()
    {
        return useWorkingDir;
    }

    public synchronized void setUseWorkingDir(boolean useWorkingDir)
    {
        this.useWorkingDir = useWorkingDir;
    }

    public synchronized String getSandbox()
    {
        return sandbox;
    }

    public synchronized void setSandbox(String sandbox)
    {
        this.sandbox = sandbox;
    }

    public synchronized String getPowershellCommand()
    {
        return getProcessCmd(ProcessPowershellRequest);
    }

    public synchronized String getShellCommand()
    {
        return getProcessCmd(ProcessShellRequest);
    }

    private String getProcessCmd(String stafPrefix)
    {
        StringBuilder cmd = new StringBuilder(stafPrefix).append("\"");
        if(!fdn)
            cmd.append(command);
        else
            cmd.append(basePath);
        if (wait)
            cmd.append("\" wait ");
        else
            cmd.append("\" ");
//            cmd.append("\" notify onend ");
        cmd.append("returnstdout ")
        .append("returnstderr ");
        if(!fdn && useWorkingDir)
            cmd.append("workdir ").append(basePath);
        return cmd.toString();
    }

    public synchronized String getBasePath()
    {
        return basePath;
    }

    public synchronized void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public synchronized String getFileName()
    {
        return fileName;
    }

    public synchronized void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public synchronized String getHost()
    {
        return host;
    }

    public synchronized void setHost(String host)
    {
        this.host = host;
    }

    public synchronized String getCommand()
    {
        return command;
    }

    public synchronized void setCommand(String command)
    {
        this.command = command;
    }

    public synchronized boolean isWait()
    {
        return wait;
    }

    public synchronized void setWait(boolean wait)
    {
        this.wait = wait;
    }

    public synchronized Object getContext()
    {
        return context;
    }

    public synchronized void setContext(Object context)
    {
        this.context = context;
    }

    
    public synchronized int getTimeout()
    {
        return timeout;
    }

    public synchronized void setTimeout(int timeout)
    {
        this.timeout = timeout;
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
        format.ttl("host: ", host);
        format.ttl("fdn: ", fdn);
        format.ttl("basePath: ", basePath);
        format.ttl("fileName: ", fileName);
        format.ttl("fileOnly: ", fileOnly);
        format.ttl("sandbox: ", sandbox);
        format.ttl("command: ", command);
        format.ttl("wait: ", wait);
        format.ttl("timeout: ", timeout);
        format.ttl("useWorkingDir: ", useWorkingDir);
        format.ttl("context: ", (context == null ? "null" : context.getClass().getName()));
        return format;
    }
}
