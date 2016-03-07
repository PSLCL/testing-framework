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
package com.pslcl.dtf.core.runner.resource;

import java.util.List;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;

/**
 * Resource Manager of all resources for a given silo.
 * <p>
 * Where a silo might be Amazon AWS or Google, etc.
 * <p>
 * Declares a single point of contact for the <code>TemplateProvider</code> to first 
 * instantiate a manager via configuration/reflection and then have that manager
 * instantiate all the ResourceProviders that it supports.  For example:
 * <ul>
 * <li><code>MachineProvider</code></li>
 * <li><code>NetworkProvider</code></li>
 * <li><code>PersonProvider</code></li>
 * <li>Future extensibility</li>
 * </ul>
 * This manager will also be responsible for the handling the release and cleanup of resources.
 */
public interface ResourcesManager
{
    /**
     * Apache Daemon level init.
     * @param config The daemon/system level configuration.  Must not be null.
     * @throws Exception if the <code>ResourcesManager</code> did not initialize and start
     */
    public void init(RunnerConfig config) throws Exception;
    
    /**
     * Apache Daemon level destroy.
     */
    public void destroy();
    
    /**
     * Return a list of supported ResourceProviders.
     * @return return a list of ResourceProvider implementations.  Must never be null, may be empty.
     */
    public List<ResourceProvider> getResourceProviders();
    
    /**
     * Set the current run identifier for the given template identifier.
     * <p>
     * The manager should set the runId for all <code>ResourceInstances</code> that it has
     * which are associated with the given templateId. 
     * @param templateInstanceId the template containing all <code>ResourceInstances</code> 
     * which should be set to the given runId.  Must never be null.
     * @param runId the current run identifier that should be associated with the given templateId. 
     */
    public void setRunId(long templateInstanceId, long runId);
    
    /**
     * Release all resources associated with the given templateId.
     * <p>
     * Release all resources that may have been reserved or bound to the given templateId.  
     * If isResusable is true the <code>TemplateProvider</code> is indicating 
     * that it will likely be asking for the same template resources again soon.  
     * The implementation should then optimize these resources where possible.
     * @param templateInstanceId The templateId to release all resources for.  Must never be null.
     * @param isReusable If true, the implementation should optimize, otherwise it 
     * should destroy the resources. 
     */
    public void release(long templateInstanceId, boolean isReusable);
    
    /**
     * Release the given resource.
     * <p>
     * Release the given resource.  
     * If isResusable is true the <code>TemplateProvider</code> is indicating 
     * that it will likely be asking for the same resource again soon.  
     * The implementation should then optimize this resource where possible.
     * @param templateId The templateId associated with the resource.  Must never be null.
     * @param resourceId the resource to be released.
     * @param isReusable If true, the implementation should optimize, otherwise it 
     * should destroy the resource. 
     */
    public void release(long templateInstanceId, long resourceId, boolean isReusable);

    /**
     * Catastrophic call for cleanup.
     * <p>
     * The manager should attempt to clean up any/all resources, including 
     * persistent silo specific objects which are not currently in its memory 
     * maps but for which it recognizes it has created in the past. 
     */
    public void forceCleanup();
}