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

import java.util.Map;

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class QueryResult
{
    public final static String HandleKey = "handle";
    public final static String HandleNameKey = "handleName";
    public final static String TitleKey = "title";
    public final static String WorkloadKey = "workload";
    public final static String ShellKey = "shell";
    public final static String CommandKey = "command";
    public final static String ParamsKey = "params";
    public final static String WorkDirKey = "workdir";
    public final static String FocusKey = "focus";
    public final static String UserKey = "userName";
    public final static String KeyKey = "key";
    public final static String PidKey = "pid";
    public final static String StartModeKey = "startMode";
    public final static String StartTimeKey = "startTimestamp";
    public final static String EndTimeKey = "endTimestamp";
    public final static String RcKey = "rc";

    public final STAFResult result;
    public final String handle;
    public final String handleName;
    public final String title;
    public final String workload;
    public final String shell;
    public final String command;
    public final String params;
    public final String workdir;
    public final String focus;
    public final String userName;
    public final String key;
    public final String pid;
    public final String startMode;
    public final String startTimestamp;
    public final String endTimestamp;
    public final String rc;

    public QueryResult(STAFResult result)
    {
        this.result = result;
        @SuppressWarnings("rawtypes")
        Map map = (Map) result.resultContext.getRootObject();
        handle = (String) map.get(HandleKey);
        handleName = (String) map.get(HandleNameKey);
        title = (String) map.get(TitleKey);
        workload = (String) map.get(WorkloadKey);
        shell = (String) map.get(ShellKey);
        command = (String) map.get(CommandKey);
        params = (String) map.get(ParamsKey);
        workdir = (String) map.get(WorkDirKey);
        focus = (String) map.get(FocusKey);
        userName = (String) map.get(UserKey);
        key = (String) map.get(KeyKey);
        pid = (String) map.get(PidKey);
        startMode = (String) map.get(StartModeKey);
        startTimestamp = (String) map.get(StartTimeKey);
        endTimestamp = (String) map.get(EndTimeKey);
        rc = (String) map.get(RcKey);
    }

    public boolean isRunning()
    {
        return endTimestamp == null;
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
        format.ttl("handle = ", handle);
        format.ttl("handleName = ", handleName);
        format.ttl("title = ", title);
        format.ttl("workload = ", workload);
        format.ttl("shell = ", shell);
        format.ttl("command = ", command);
        format.ttl("params = ", params);
        format.ttl("workdir = ", workdir);
        format.ttl("focus = ", focus);
        format.ttl("userName = ", userName);
        format.ttl("key = ", key);
        format.ttl("pid = ", pid);
        format.ttl("startMode = ", startMode);
        format.ttl("startTimestamp = ", startTimestamp);
        format.ttl("endTimestamp = ", endTimestamp);
        format.ttl("rc = ", rc);
        format.level.decrementAndGet();
        return format;
    }
}
