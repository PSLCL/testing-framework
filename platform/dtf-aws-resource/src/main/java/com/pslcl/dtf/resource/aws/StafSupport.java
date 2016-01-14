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
package com.pslcl.dtf.resource.aws;

import org.slf4j.LoggerFactory;

import com.ibm.staf.STAFException;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;

@SuppressWarnings("javadoc")
public class StafSupport
{
    public static final String StafHandleName = "dtf-staf-handle";
    private static STAFHandle handle;
    
    private StafSupport()
    {
    }
    
    public static STAFHandle getStafHandle() throws Exception
    {
        synchronized (StafHandleName)
        {
            if(handle != null)
                return handle;
            try
            {
                handle = new STAFHandle(StafHandleName);
            }catch(STAFException e)
            {
                STAFResult result = new STAFResult(e.rc, "failed to obtain handle name: " + StafHandleName);
                checkStafResult(result);
            }
        }
        return handle;
    }
    
    public static void ping(String host) throws Exception
    {
        STAFResult result = getStafHandle().submit2(host, "PING", "PING");
        checkStafResult(result);
    }
    
    public static void checkStafResult(STAFResult result) throws Exception
    {
        String msg = null;
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
        if (result.rc == STAFResult.Ok)
            return;
        throw new Exception("StafException, rc: " + result.rc + "("+msg+") result: " + result.result);
    }
    
    public static void destroy()
    {
        synchronized (StafHandleName)
        {
            if(handle != null)
            {
                try
                {
                    handle.unRegister();
                } catch (STAFException e)
                {
                    STAFResult result = new STAFResult(e.rc, "failed to unRegister handle name: " + StafHandleName);
                    try
                    {
                        checkStafResult(result);
                    } catch (Exception e1)
                    {
                        LoggerFactory.getLogger(StafSupport.class).warn("failed to cleanup staf", e1);
                    }
                }
            }
        }

    }
}
