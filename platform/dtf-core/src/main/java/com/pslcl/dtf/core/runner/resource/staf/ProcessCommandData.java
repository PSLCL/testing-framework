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

import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class ProcessCommandData
{
    public final static String ProcessShellRequest = "start shell command ";
    public final static String ProcessRequest = "start command ";
    
    private boolean fdn;
    private String basePath;
    private String fileName;
    private String host;
    private String command;
    private boolean wait;
    private int timeout;
    private Object context;
    private boolean useWorkingDir;

    public ProcessCommandData(String basePath, String fileName, boolean fdn)
    {
        this.fdn = fdn;
        this.basePath = basePath;
        this.fileName = fileName;
        this.useWorkingDir = useWorkingDir;
    }

    public ProcessCommandData(ProcessCommandData commandData)
    {
        basePath = commandData.getBasePath();
        fileName = commandData.getFileName();
        host = commandData.getHost();
        command = commandData.getCommand();
        wait = commandData.isWait();
        timeout = commandData.getTimeout();
        context = commandData.getContext();
        fdn = commandData.isFdn();
        useWorkingDir = commandData.isUseWorkingDir();
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
    
    public synchronized boolean isUseWorkingDir()
    {
        return useWorkingDir;
    }

    public synchronized void setUseWorkingDir(boolean useWorkingDir)
    {
        this.useWorkingDir = useWorkingDir;
    }

    public synchronized String getProcessCommand()
    {
        return getProcessCmd(ProcessRequest);
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
        format.ttl("command: ", command);
        format.ttl("wait: ", wait);
        format.ttl("timeout: ", timeout);
        format.ttl("useWorkingDir: ", useWorkingDir);
        format.ttl("context: ", (context == null ? "null" : context.getClass().getName()));
        return format;
    }
}
