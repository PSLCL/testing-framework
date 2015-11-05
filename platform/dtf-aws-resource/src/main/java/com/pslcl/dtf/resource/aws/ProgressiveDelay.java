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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.pslcl.dtf.core.runner.config.status.ResourceStatus;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.exception.FatalClientException;
import com.pslcl.dtf.core.runner.resource.exception.FatalInterruptedException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerTimeoutException;
import com.pslcl.dtf.core.runner.resource.exception.FatalTimeoutException;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;

public class ProgressiveDelay
{
    private final Logger log;
    private final AtomicInteger count;
    private final AtomicLong totalTime;
    private final ProgressiveDelayData pdelayData;
    
    public ProgressiveDelay(ProgressiveDelayData pdelayData)
    {
        log = LoggerFactory.getLogger(getClass());
        this.pdelayData = pdelayData;
        this.count = new AtomicInteger(-1);
        this.totalTime = new AtomicLong(0);
    }
    
    public void reset()
    {
        count.set(-1);
    }
    
    public void retry(String message) throws FatalResourceException
    {
        int cnt = count.incrementAndGet();
        if(cnt >= pdelayData.maxRetries)
            return;  // called by handleException
        long delay = ((long) Math.pow(2, cnt) * 100L);
        delay = Math.min(delay, pdelayData.maxDelay);
        log.debug("count: " + cnt + " delay: " + delay);
        try
        {
            Thread.sleep(delay);
            totalTime.addAndGet(delay);
        }catch(InterruptedException e)
        {
            throw handleException(message, e);
        }
        if(cnt >= pdelayData.maxRetries - 1)
        {
            String msg = getErrorMessage(message, true);
            FatalTimeoutException e = new FatalServerTimeoutException(pdelayData.templateId, pdelayData.reference, msg);
            e.setMaxDelay(pdelayData.maxDelay);
            e.setMaxRetries(pdelayData.maxRetries);
            e.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
            e.fillInStackTrace();
            log.warn(msg, e);
            handleStatusTracker();
            throw e;
        }
    }
    
    private String getErrorMessage(String message, boolean timeout)
    {
        TabToLevel format = new TabToLevel(null);
        format.ttl("\nTemplateId: ", pdelayData.templateId);
        format.level.incrementAndGet();
        format.ttl("reference: ", pdelayData.reference);
        format.ttl("message: ", message);
        if(timeout)
        {
            format.ttl("maxDelay: ", pdelayData.maxDelay, " maxRetries: ", pdelayData.maxRetries);
            format.ttl("totalTime: ", StrH.scaleMilliSeconds(totalTime.get()));
        }
        return format.sb.toString();
    }
    
    private void handleStatusTracker()
    {
        pdelayData.statusTracker.setStatus(pdelayData.templateId, StatusTracker.Status.Error);
        pdelayData.statusTracker.fireResourceStatusChanged(
                        pdelayData.resourceStatus.getNewInstance(pdelayData.resourceStatus, StatusTracker.Status.Error));
    }
    
    public FatalResourceException handleException(String message, Throwable t)
    {
        if(t instanceof AmazonServiceException)
        {
            AmazonServiceException ase = (AmazonServiceException)t;
            ase.getErrorType();
            if(ase.getStatusCode() == 400)
            {
                String msg = getErrorMessage(message, false);
                log.warn(msg, t);
                handleStatusTracker();
                return new FatalServerException(pdelayData.templateId, pdelayData.reference, msg, t);
            }
            try
            {
                retry(message);
                return null;
            }catch(Exception e)
            {
                String msg = getErrorMessage(message, true);
                log.warn(msg, t);
                FatalTimeoutException ftoe = new FatalServerTimeoutException(pdelayData.templateId, pdelayData.reference, msg, t);
                ftoe.setMaxDelay(pdelayData.maxDelay);
                ftoe.setMaxRetries(pdelayData.maxRetries);
                ftoe.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
                handleStatusTracker();
                return ftoe;
            }
        }
        
        if(t instanceof AmazonClientException)
        {
            AmazonClientException ace = (AmazonClientException)t;  
            if(ace.isRetryable())
            {
                try
                {
                    retry(message);
                    return null;
                }catch(Exception e)
                {
                    String msg = getErrorMessage(message, false);
                    log.warn(msg, t);
                    FatalTimeoutException ftoe = new FatalServerTimeoutException(pdelayData.templateId, pdelayData.reference, msg, t);
                    ftoe.setMaxDelay(pdelayData.maxDelay);
                    ftoe.setMaxRetries(pdelayData.maxRetries);
                    ftoe.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
                    handleStatusTracker();
                    return ftoe;
                }
            }
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker();
            return new FatalClientException(pdelayData.templateId, pdelayData.reference, msg, t);
        }
        if(t instanceof IllegalArgumentException)
        {
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker();
            return new FatalClientException(pdelayData.templateId, pdelayData.reference, msg, t);
        }
        if(t instanceof InterruptedException)
        {
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker();
            return new FatalInterruptedException(pdelayData.templateId, pdelayData.reference, msg, t);
        }
        String msg = getErrorMessage(message, false);
        log.warn(msg, t);
        handleStatusTracker();
        return new FatalClientException(pdelayData.templateId, pdelayData.reference, msg, t);
    }
    
    public static class ProgressiveDelayData 
    {
        public volatile String preFixMostName;
        public volatile int maxDelay;
        public volatile int maxRetries; 
        public final ResourceProvider provider; 
        public final StatusTracker statusTracker;
        public final String templateId;
        public final long reference;
        public final ResourceStatus resourceStatus;

        //@formatter:off
        public ProgressiveDelayData(
                        ResourceProvider provider, 
                        StatusTracker statusTracker, 
                        String templateId, 
                        long reference)
        //@formatter:off
        {
            this.provider = provider;
            this.statusTracker = statusTracker;
            this.templateId = templateId;
            this.reference = reference;
            resourceStatus = new ResourceStatus(provider, templateId, reference, StatusTracker.Status.Warn);
        }
        
        public ResourceStatus getResourceStatus(StatusTracker.Status status)
        {
            return resourceStatus.getNewInstance(resourceStatus, status);
        }
        
        public String getFullName(String midString, String post)
        {
            return preFixMostName + "-" + midString + "-" + templateId + (post == null ? "" : " " + post);
        }
        
        public String getHumanName(String midString, String post)
        {
            String tid = templateId;
            if(templateId.length() > 16)
                tid = templateId.substring(16);
            return preFixMostName + "-" + midString + "-" + tid + (post == null ? "" : " " + post);
        }
    }
}
