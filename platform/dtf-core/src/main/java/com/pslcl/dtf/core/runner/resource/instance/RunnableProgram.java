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
package com.pslcl.dtf.core.runner.resource.instance;

import com.pslcl.dtf.core.runner.resource.staf.ProcessResult;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

/**
 */
public interface RunnableProgram
{
    public static void logProgramResults(RunnableProgram runnableProgram, long runID)
    {
        String syserr = null;
        String sysout = null;
        String command = null;
        RunResult result = runnableProgram.getRunResult();
        if(runnableProgram instanceof StafRunnableProgram)
        {
            ProcessResult processResult = ((StafRunnableProgram)runnableProgram).getResult();
            if(processResult != null)
            {
                syserr = processResult.getCompletionSysErr();
                sysout = processResult.getCompletionSysOut();
            }
            command = ((StafRunnableProgram)runnableProgram).getCommandData().getCommand();
        }
        LoggerFactory.getLogger(RunnableProgram.class).debug("Executed run command for test run {}. Command: {}, result: {}, sysout: {}, syserr: {}", runID, command, result, sysout, syserr);
    }

    /**
     * Return a program run result object.
     *
     * @return The program run result.
     */
    public RunResult getRunResult();

    /**
     * Stop the program command on a machine and return a Future with the {@link Future} result of the corresponding
     * stop command, or the exit code of the stop script, being set once the stop script has completed.
     *
     * @return a Future with the {@link Future} result of the corresponding stop command, or the exit code of the stop
     * script, being set once the stop script has completed.
     */
    public Future<Integer> kill();

    /**
     * Determine the running state of the program.
     *
     * @return {@code True} if the program is running. {@code False} otherwise.
     */
    public boolean isRunning();

    /**
     * Captures the instance logging to AWS S3. This method will block.
     */
    public void captureLogsToS3();

    public enum ResultType
    {
        Passed, RunnerFailure, ResourceFailure, TestFailure, StillRunnning
    }

    public class RunResult
    {
        public final ResultType failureType;
        public final Integer runResult;

        public RunResult(ResultType failureType, Integer runResult)
        {
            this.failureType = failureType;
            this.runResult = runResult;
        }

        public String toString()
        {
            return "failureType: " + failureType + " runResult: " + runResult;
        }
    }
}