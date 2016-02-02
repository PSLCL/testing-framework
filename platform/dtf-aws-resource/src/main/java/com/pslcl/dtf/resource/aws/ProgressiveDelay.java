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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusEvent;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.exception.FatalClientException;
import com.pslcl.dtf.core.runner.resource.exception.FatalInterruptedException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerException;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerTimeoutException;
import com.pslcl.dtf.core.runner.resource.exception.FatalTimeoutException;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;

@SuppressWarnings("javadoc")
public class ProgressiveDelay
{
    private final Logger log;
    private final AtomicInteger count;
    private final AtomicLong totalTime;
    private final AtomicBoolean maxDelayHit;
    private final ProgressiveDelayData pdelayData;
    
    public ProgressiveDelay(ProgressiveDelayData pdelayData)
    {
        log = LoggerFactory.getLogger(getClass());
        this.pdelayData = pdelayData;
        count = new AtomicInteger(-1);
        totalTime = new AtomicLong(0);
        maxDelayHit = new AtomicBoolean(false);
        
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
        long delay = pdelayData.maxDelay;
        if(!maxDelayHit.get())
        {
            delay = ((long) Math.pow(2, cnt) * 100L);
            if(delay > pdelayData.maxDelay)
            {
                delay = pdelayData.maxDelay;
                maxDelayHit.set(true);
            }
        }
        log.debug("count: " + cnt + " delay: " + delay + " totalwait: " + StrH.scaleMilliSeconds(totalTime.get()));
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
            FatalTimeoutException e = new FatalServerTimeoutException(pdelayData.coord, msg);
            e.setMaxDelay(pdelayData.maxDelay);
            e.setMaxRetries(pdelayData.maxRetries);
            e.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
            e.fillInStackTrace();
            log.warn(msg, e);
            handleStatusTracker(StatusTracker.Status.Error);
            throw e;
        }
    }
    
    private String getErrorMessage(String message, boolean timeout)
    {
        TabToLevel format = new TabToLevel();
        pdelayData.coord.toString(format);
        format.ttl("message: ", message);
        if(timeout)
        {
            format.ttl("maxDelay: ", pdelayData.maxDelay, " maxRetries: ", pdelayData.maxRetries);
            format.ttl("totalTime: ", StrH.scaleMilliSeconds(totalTime.get()));
        }
        return format.sb.toString();
    }
    
    private void handleStatusTracker(StatusTracker.Status status)
    {
        AwsResourcesManager.handleStatusTracker(pdelayData, status);
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
                handleStatusTracker(StatusTracker.Status.Alert);
                return new FatalServerException(pdelayData.coord, msg, t);
            }
            try
            {
                retry(message);
                return null;
            }catch(Exception e)
            {
                String msg = getErrorMessage(message, true);
                log.warn(msg, t);
                FatalTimeoutException ftoe = new FatalServerTimeoutException(pdelayData.coord, msg, t);
                ftoe.setMaxDelay(pdelayData.maxDelay);
                ftoe.setMaxRetries(pdelayData.maxRetries);
                ftoe.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
                handleStatusTracker(StatusTracker.Status.Error);
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
                    FatalTimeoutException ftoe = new FatalServerTimeoutException(pdelayData.coord, msg, t);
                    ftoe.setMaxDelay(pdelayData.maxDelay);
                    ftoe.setMaxRetries(pdelayData.maxRetries);
                    ftoe.setTotalWaitTime(StrH.scaleMilliSeconds(totalTime.get()));
                    handleStatusTracker(StatusTracker.Status.Error);
                    return ftoe;
                }
            }
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker(StatusTracker.Status.Alert);
            return new FatalClientException(pdelayData.coord, msg, t);
        }
        if(t instanceof IllegalArgumentException)
        {
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker(StatusTracker.Status.Alert);
            return new FatalClientException(pdelayData.coord, msg, t);
        }
        if(t instanceof InterruptedException)
        {
            String msg = getErrorMessage(message, false);
            log.warn(msg, t);
            handleStatusTracker(StatusTracker.Status.Error);
            return new FatalInterruptedException(pdelayData.coord, msg, t);
        }
        String msg = getErrorMessage(message, false);
        log.warn(msg, t);
        handleStatusTracker(StatusTracker.Status.Alert);
        return new FatalClientException(pdelayData.coord, msg, t);
    }
    
    public static class ProgressiveDelayData 
    {
        public volatile String preFixMostName;
        public volatile int maxDelay;
        public volatile int maxRetries; 
        public final AwsResourceProvider provider; 
        public final StatusTracker statusTracker;
        public volatile ResourceCoordinates coord;
        public final ResourceStatusEvent resourceStatusEvent;

        //@formatter:off
        public ProgressiveDelayData(AwsResourceProvider provider, ResourceCoordinates coordinates)
        //@formatter:off
        {
            this.provider = provider;
            statusTracker = provider.config.statusTracker;
            coord = coordinates;
            resourceStatusEvent = new ResourceStatusEvent(AwsResourcesManager.StatusPrefixStr+coord.resourceId, StatusTracker.Status.Warn, coord);
        }
        
        public ResourceStatusEvent getResourceStatus(StatusTracker.Status status)
        {
            return resourceStatusEvent.getNewInstance(resourceStatusEvent, status);
        }
        
        public String getFullResourceIdName(String midString, String post)
        {
            return preFixMostName + "-" + midString + "-" + coord.resourceId + (post == null ? "" : " " + post);
        }
        
        public String getFullTemplateIdName(String midString, String post)
        {
            return preFixMostName + "-" + midString + "-" + coord.templateId + (post == null ? "" : " " + post);
        }
        
        public String getHumanName(String midString, String post)
        {
            String tid = coord.templateId;
            if(coord.templateId.length() > 12)
                tid = coord.templateId.substring(0, 12);
            return preFixMostName + "-" + midString + "-" + tid + (post == null ? "" : " " + post);
        }
    }
}
