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

import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.ProcessResult;
import com.pslcl.dtf.core.runner.resource.staf.StopResult;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@SuppressWarnings("javadoc")
public class StafRunnableProgram implements RunnableProgram
{
    static final String NotALinuxFile = "<nodot>";

    private final static String ProcessQueryHandle = "query handle ";
    private final static String ProcessStopHandle = "stop handle ";
    private final static String ProcessFreeHandle = "free handle ";

    public final ProcessResult result;
    private final ProcessCommandData commandData;
    private ExecutorService executor;
    private boolean stopped;

    public StafRunnableProgram(ProcessCommandData commandData) throws Exception
    {
        this(null, commandData);
    }

    public StafRunnableProgram(STAFResult result, ProcessCommandData commandData) throws Exception
    {
        this.commandData = commandData;
        if(result == null)
            this.result = null;
        else
            this.result = new ProcessResult(result, commandData.isWait());
    }

    @SuppressWarnings("unused")
    public synchronized boolean isStopped()
    {
        return stopped;
    }

    synchronized void setStopped(StopResult result)
    {
        this.stopped = true;
        this.result.setStopResult(result);
    }

    @SuppressWarnings("unused")
    public synchronized ExecutorService getExecutorService()
    {
        return executor;
    }

    synchronized void setExecutorService(ExecutorService executor)
    {
        this.executor = executor;
    }

    public synchronized String getProcessQueryCommand()
    {
        return ProcessQueryHandle + " " + getProcessHandle();
    }

    private String getProcessHandle()
    {
        if(commandData.isWait())
            return null;
        return result.getServiceHandle();
    }

    public synchronized String getProcessStopCommand()
    {
        return ProcessStopHandle + " " + getProcessHandle();
    }

    public synchronized String getProcessFreeCommand()
    {
        return ProcessFreeHandle + " " + getProcessHandle();
    }

    public ProcessResult getResult()
    {
        return result;
    }

    public ProcessCommandData getCommandData()
    {
        return commandData;
    }

    @Override
    public Future<Integer> kill()
    {
        return executor.submit(new KillFuture(this));
    }

    @Override
    public synchronized boolean isRunning()
    {
        //TODO: poll or notify? needs added here
        return !commandData.isWait() && !stopped;
    }

    @Override
    public synchronized Integer getRunResult()
    {
        if(isRunning())
            return null;
        //TODO: when runforever with kill for stop but get "start" ccode ... this is broken here
        return result.getServiceCcode();
    }

    @Override
    public void captureLogsToS3()
    {
        try
        {
            ResourceCoordinates coordinates = commandData.getCoordinates();

            StringBuilder keyprefix = new StringBuilder("/");
            keyprefix.append("runId-").append(coordinates.getRunId()).append("/");
            keyprefix.append("templateId-").append(coordinates.templateIdToHexString()).append("/");
            keyprefix.append("templateInstanceId-").append(coordinates.templateInstanceId).append("/");
            keyprefix.append("resourceId-").append(coordinates.resourceId);

            if(commandData.isWindows())
            {
                // powershell -NoExit -Command "& {Import-Module 'C:\Program Files (x86)\AWS Tools\PowerShell\AWSPowerShell\AWSPowerShell.psd1'; Initialize-AWSDefaultConfiguration; write-s3object -keyprefix /testId/templateId/runid/instanceid -folder C:\var\opt\pslcl\dtf\log -bucketname dtf-staf-logging}
                // "write-s3object -keyprefix /testId/templateId/runid/instanceid -folder C:\var\opt\pslcl\dtf\log -bucketname dtf-staf-logging";
//                StringBuilder cmd = new StringBuilder("powershell -NoExit -Command \"& {Import-Module 'C:\\Program Files (x86)\\AWS Tools\\PowerShell\\AWSPowerShell\\AWSPowerShell.psd1'; Initialize-AWSDefaultConfiguration; write-s3object -keyprefix ");
                StringBuilder cmd = new StringBuilder("powershell -Command \"& {Import-Module 'C:\\Program Files (x86)\\AWS Tools\\PowerShell\\AWSPowerShell\\AWSPowerShell.psd1'; Initialize-AWSDefaultConfiguration; write-s3object -keyprefix ");
                cmd.append(keyprefix);
                String logFolder = StrH.stripTrailingSeparator(commandData.getLogFolder());
                cmd.append(" -folder ").append(logFolder);
                cmd.append(" -bucketname ").append(commandData.getS3Bucket());
                cmd.append("}");

                //@formatter:off
                RunFuture df = new RunFuture(
                        commandData.getHost(),
                        null, commandData.getSandbox(),
                        cmd.toString(), executor, true, true,
                        commandData.getCoordinates(), commandData.getSandbox(),
                        null, null);
                //@formatter:on
                RunnableProgram rp = executor.submit(df).get();
                RunnableProgram.logProgramResults(rp, commandData.getCoordinates().getRunId());
                return;
            }
////aws s3api put-object --bucket dtf-staf-logging --key testId/templateId/runId/
//            StringBuilder cmd = new StringBuilder(NotALinuxFile + "aws s3api put-object --bucket ");
//            cmd.append(commandData.getS3Bucket());
//            String key = StrH.addTrailingSeparator(keyprefix.toString(), '/');
//            cmd.append(" --key ").append(key);
//            //@formatter:off
//                RunFuture df = new RunFuture(
//                        commandData.getHost(),
//                        commandData.getSandbox(), null,
//                        cmd.toString(), executor, true, false,
//                        commandData.getCoordinates(), commandData.getSandbox(),
//                        commandData.getLogFolder(), null);
//                //@formatter:on
//            executor.submit(df).get();

//aws s3 cp --recursive /var/opt/pslcl/dtf/log s3://dtf-staf-logging/testId/templateId/runId/instanceId
            StringBuilder cmd = new StringBuilder(NotALinuxFile+ "aws s3 cp --recursive ");
            String logFolder = StrH.stripTrailingSeparator(commandData.getLogFolder());
            cmd.append(logFolder);
            cmd.append(" s3://").append(commandData.getS3Bucket()).append(keyprefix);

            //@formatter:off
                RunFuture df = new RunFuture(
                        commandData.getHost(),
                        commandData.getSandbox(), null,
                        cmd.toString(), executor, true, false,
                        commandData.getCoordinates(), commandData.getSandbox(),
                        null, null);
                //@formatter:on
            RunnableProgram rp = executor.submit(df).get();
            RunnableProgram.logProgramResults(rp, commandData.getCoordinates().getRunId());
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).warn("failed to capture logs to s3:\n" + commandData.toString());
        }
    }

    @Override
    public synchronized String toString()
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n");
        return toString(format).toString();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public synchronized TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName() + ":");
        format.level.incrementAndGet();
        format.ttl("running = ", isRunning());
        if(result != null)
            result.toString(format);
        commandData.toString(format);
        format.level.decrementAndGet();
        return format;
    }
}
