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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.staf.STAFException;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import com.pslcl.dtf.core.util.TabToLevel;

@SuppressWarnings("javadoc")
public class StafSupport
{
    //FIXME: set me false before commit/push
    public static final boolean DtfTesting = true;  // do not actually issue staf requests if true

    private final static String StafHandleName = "dtf-staf-handle";
    private final static String ProcessService = "process";
    private final static String PingService = "ping";
    @SuppressWarnings("unused")
    private final static String ProcessRequest = "start shell command ";
    @SuppressWarnings("unused")
    private final static String ServiceReturnCode = "Return Code:";

    private static Logger log = LoggerFactory.getLogger(StafSupport.class);
    private static STAFHandle handle;

    private StafSupport()
    {
    }

    public static StopResult processStop(StafRunnableProgram runnableProg) throws Exception
    {
        if(DtfTesting)
            return new StopResult();
        String cmd = runnableProg.getProcessStopCommand();
        TabToLevel format = null;
        if (log.isDebugEnabled())
        {
            format = new TabToLevel();
            format.ttl("\n", StafSupport.class.getSimpleName(), ".processStop:");
            format.level.incrementAndGet();
            format.ttl("stafCmd = ", ProcessService + " " + cmd);
        }
        
        StopResult stopResult = null;
        STAFResult result;
        boolean resultParse = false;
        try
        {
            QueryResult qresult = processQuery(runnableProg);
            if(qresult.isRunning())
            {
                result = request(runnableProg.getCommandData().getHost(), ProcessService, cmd, format);
                resultParse = true;
                stopResult = new StopResult(result, true);  // currently only byHandle is supported
            }else
                stopResult = new StopResult();
            processFree(runnableProg);
        }finally
        {
            if (log.isDebugEnabled())
            {
                if(stopResult != null)
                    log.debug(stopResult.toString(format).toString());
                else
                {
                    if(resultParse && format != null)
                    {
                        format.ttl("\nStafRunnableProgram failed to parse the result");
                        log.debug(format.toString());
                    }
                }
            }
        }
        return stopResult;
    }
    
    @SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
    public static FreeResult processFree(StafRunnableProgram runnableProg) throws Exception
    {
        if(DtfTesting)
            return new FreeResult();
        String cmd = runnableProg.getProcessFreeCommand();
        TabToLevel format = null;
        if (log.isDebugEnabled())
        {
            format = new TabToLevel();
            format.ttl("\n", StafSupport.class.getSimpleName(), ".processFree:");
            format.level.incrementAndGet();
            format.ttl("stafCmd = ", ProcessService + " " + cmd);
        }
        
        FreeResult freeResult = null;
        STAFResult result;
        boolean resultParse = false;
        try
        {
            QueryResult qresult = processQuery(runnableProg);
            if(qresult.isRunning())
            {
                result = StafSupport.request(runnableProg.getCommandData().getHost(), ProcessService, cmd, format);
                resultParse = true;
                freeResult = new FreeResult(result, true);  // currently only byHandle is supported
            }
        }finally
        {
            if (log.isDebugEnabled())
            {
                if(freeResult != null)
                    log.debug(freeResult.toString(format).toString());
                else
                {
                    if(resultParse && format != null)
                    {
                        format.ttl("\nStafRunnableProgram failed to parse the result");
                        log.debug(format.toString());
                    }
                }
            }
        }
        return freeResult;
    }
    
    public static QueryResult processQuery(StafRunnableProgram runnableProg) throws Exception
    {
        if(DtfTesting)
            return new QueryResult(new STAFResult());
        String cmd = runnableProg.getProcessQueryCommand();
        TabToLevel format = null;
        if (log.isDebugEnabled())
        {
            format = new TabToLevel();
            format.ttl("\n", StafSupport.class.getSimpleName(), ".processQuery:");
            format.level.incrementAndGet();
            format.ttl("stafCmd = ", ProcessService + " " + cmd);
        }
        
        QueryResult queryResult = null;
        STAFResult result;
        boolean resultParse = false;
        try
        {
            result = StafSupport.request(runnableProg.getCommandData().getHost(), ProcessService, cmd, format);
            resultParse = true;
            queryResult = new QueryResult(result);
        }finally
        {
            if (log.isDebugEnabled())
            {
                if(queryResult != null)
                    log.debug(queryResult.toString(format).toString());
                else
                {
                    if(resultParse && format != null)
                    {
                        format.ttl("\nStafRunnableProgram failed to parse the result");
                        log.debug(format.toString());
                    }
                }
            }
        }
        return queryResult;
    }
    
    @SuppressWarnings("null")
    public static STAFResult processPing(StafRunnableProgram runnableProg) throws Exception
    {
        if(DtfTesting)
            return new STAFResult();
        TabToLevel format = null;
        if (log.isDebugEnabled())
        {
            format = new TabToLevel();
            format.ttl("\n", StafSupport.class.getSimpleName(), ".processPing:");
            format.level.incrementAndGet();
            format.ttl("stafCmd = ", PingService + " " + PingService);
        }
        
        STAFResult result = null;
        try
        {
           result = StafSupport.request(runnableProg.getCommandData().getHost(), PingService, PingService, format);
        }finally
        {
            if (log.isDebugEnabled())
            {
                if(result != null)
                    log.debug(result.toString());
                else
                {
                    //noinspection ConstantConditions
                    format.ttl("\nPing Ping failed");
                    log.debug(format.toString());
                }
            }
        }
        return result;
    }
    
    public static StafRunnableProgram issueProcessShellRequest(ProcessCommandData commandData) throws Exception
    {
        return issueProcessRequest(commandData, false);
    }
    
    public static StafRunnableProgram issueProcessPowershellRequest(ProcessCommandData commandData) throws Exception
    {
        return issueProcessRequest(commandData, true);
    }
    
    @SuppressWarnings("null")
    private static StafRunnableProgram issueProcessRequest(ProcessCommandData commandData, boolean powershell) throws Exception
    {
        TabToLevel format = null;
        if (log.isDebugEnabled())
        {
            format = new TabToLevel();
            format.sb.append("\n").append(StafSupport.class.getSimpleName());
            if(!powershell)
                format.sb.append(".issueProcessShellRequest:");
            else
                format.sb.append(".issueProcessPowershellRequest:");
            format.sb.append("\n");
            format.level.incrementAndGet();
            //noinspection ResultOfMethodCallIgnored
            commandData.toString(format);
        }

        String cmd = commandData.getPowershellCommand();
        if(!powershell)
            cmd = commandData.getShellCommand();
        if (log.isDebugEnabled())
            //noinspection ConstantConditions
            format.ttl("stafCmd = ", ProcessService + " " + cmd);
        
        StafRunnableProgram runnableProgram = null;
        STAFResult result;
        boolean resultParse = false;
        try
        {
            result = StafSupport.request(commandData.getHost(), ProcessService, cmd, format);
            resultParse = true;
            runnableProgram = new StafRunnableProgram(result, commandData);
        }finally
        {
            if (log.isDebugEnabled())
            {
                if(runnableProgram != null)
                    log.debug(runnableProgram.toString(format).toString());
                else
                {
                    if(resultParse)
                        //noinspection ConstantConditions
                        format.ttl("\nStafRunnableProgram failed to parse the result");
                    //noinspection ConstantConditions
                    log.debug(format.toString());
                }
            }
        }
        return runnableProgram;
    }

    public static STAFResult request(String host, String service, String request) throws Exception
    {
        return request(host, service, request, null);
    }

    private static STAFResult request(String host, String service, String request, TabToLevel format) throws Exception
    {
        if(DtfTesting)
            return new STAFResult();

        STAFResult result = getStafHandle().submit2(host, service, request);
        checkStafResult(result, format);
        return result;
    }

    @SuppressWarnings("WeakerAccess")
    public static STAFHandle getStafHandle() throws Exception
    {
        synchronized (StafHandleName)
        {
            if (handle != null)
                return handle;
            try
            {
                handle = new STAFHandle(StafHandleName);
            } catch (STAFException e)
            {
                STAFResult result = new STAFResult(e.rc, "failed to obtain handle name: " + StafHandleName);
                checkStafResult(result, null);
            }
        }
        return handle;
    }

    @SuppressWarnings("SameParameterValue")
    public static void ping(String host) throws Exception
    {
        request(host, "ping", "ping");
    }

    @SuppressWarnings("WeakerAccess")
    public static void checkStafResult(STAFResult result, TabToLevel format) throws Exception
    {
        try
        {
            getResultRc(result, true, format);
        } catch (Exception e)
        {
            throw new Exception(e.getMessage(), e);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static String getResultRc(STAFResult result, boolean throwOnError, TabToLevel format)
    {
        String msg;
        switch (result.rc)
        {
            case 0:
                msg = "Ok";
                break;
            case 1:
                msg = "InvalidAPI";
                break;
            case 2:
                msg = "UnknownService";
                break;
            case 3:
                msg = "InvalidHandle";
                break;
            case 4:
                msg = "HandleAlreadyExists";
                break;
            case 5:
                msg = "HandleDoesNotExist";
                break;
            case 6:
                msg = "UnknownError";
                break;
            case 7:
                msg = "InvalidRequestString";
                break;
            case 8:
                msg = "InvalidServiceResult";
                break;
            case 9:
                msg = "REXXError";
                break;
            case 10:
                msg = "BaseOSError";
                break;
            case 11:
                msg = "ProcessAlreadyComplete";
                break;
            case 12:
                msg = "ProcessNotComplete";
                break;
            case 13:
                msg = "VariableDoesNotExist";
                break;
            case 14:
                msg = "UnResolvableString";
                break;
            case 15:
                msg = "InvalidResolveString";
                break;
            case 16:
                msg = "NoPathToMachine";
                break;
            case 17:
                msg = "FileOpenError";
                break;
            case 18:
                msg = "FileReadError";
                break;
            case 19:
                msg = "FileWriteError";
                break;
            case 20:
                msg = "FileDeleteError";
                break;
            case 21:
                msg = "STAFNotRunning";
                break;
            case 22:
                msg = "CommunicationError";
                break;
            case 23:
                msg = "TrusteeDoesNotExist";
                break;
            case 24:
                msg = "InvalidTrustLevel";
                break;
            case 25:
                msg = "AccessDenied";
                break;
            case 26:
                msg = "STAFRegistrationError";
                break;
            case 27:
                msg = "ServiceConfigurationError";
                break;
            case 28:
                msg = "QueueFull";
                break;
            case 29:
                msg = "NoQueueElement";
                break;
            case 30:
                msg = "NotifieeDoesNotExist";
                break;
            case 31:
                msg = "InvalidAPILevel";
                break;
            case 32:
                msg = "ServiceNotUnregisterable";
                break;
            case 33:
                msg = "ServiceNotAvailable";
                break;
            case 34:
                msg = "SemaphoreDoesNotExist";
                break;
            case 35:
                msg = "NotSemaphoreOwner";
                break;
            case 36:
                msg = "SemaphoreHasPendingRequests";
                break;
            case 37:
                msg = "Timeout";
                break;
            case 38:
                msg = "JavaError";
                break;
            case 39:
                msg = "ConverterError";
                break;
            case 40:
                msg = "NotUsed";
                break;
            case 41:
                msg = "InvalidObject";
                break;
            case 42:
                msg = "InvalidParm";
                break;
            case 43:
                msg = "RequestNumberNotFound";
                break;
            case 44:
                msg = "InvalidAsynchOption";
                break;
            case 45:
                msg = "RequestNotComplete";
                break;
            case 46:
                msg = "ProcessAuthenticationDenied";
                break;
            case 47:
                msg = "InvalidValue";
                break;
            case 48:
                msg = "DoesNotExist";
                break;
            case 49:
                msg = "AlreadyExists";
                break;
            case 50:
                msg = "DirectoryNotEmpty";
                break;
            case 51:
                msg = "DirectoryCopyError";
                break;
            case 52:
                msg = "DiagnosticsNotEnabled";
                break;
            case 53:
                msg = "HandleAuthenticationDenied";
                break;
            case 54:
                msg = "HandleAlreadyAuthenticated";
                break;
            case 55:
                msg = "InvalidSTAFVersion";
                break;
            case 56:
                msg = "RequestCancelled";
                break;
            case 57:
                msg = "CreateThreadError";
                break;
            case 58:
                msg = "MaximumSizeExceeded";
                break;
            case 59:
                msg = "MaximumHandlesExceeded";
                break;
            default:
                msg = "unknown or user error";
                break;
        }
        if (throwOnError && result.rc != 0)
        {
            if(format != null)
            {
                format.ttl("\nSTAF request failed");
                format.level.incrementAndGet();
                format.ttl("rc: ", result.rc, " (", msg, ")" );
                format.ttl("result: ", result.result);
                format.level.decrementAndGet();
            }
            throw new RuntimeException("StafException, rc: " + result.rc + " (" + msg + ") result: " + result.result);
        }
        return result.rc + " - " + msg;
    }

    public static void destroy()
    {
        synchronized (StafHandleName)
        {
            if (handle != null)
            {
                try
                {
                    handle.unRegister();
                } catch (STAFException e)
                {
                    STAFResult result = new STAFResult(e.rc, "failed to unRegister handle name: " + StafHandleName);
                    try
                    {
                        checkStafResult(result, null);
                    } catch (Exception e1)
                    {
                        log.warn("failed to cleanup staf", e1);
                    }
                }
            }
        }

    }
}
