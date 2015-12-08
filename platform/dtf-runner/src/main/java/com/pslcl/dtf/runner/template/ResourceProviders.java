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
package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveResult;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH.StringPair;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders
{

    public static final String ManagerClassKey = "pslcl.dtf.runner.template.resource-manager-class"; 
    public static final String ManagerClassDefault = "com.pslcl.dtf.resource.aws.AwsResourcesManager";
    
    private final List<ResourcesManager> resourceManagers;
    private final List<ResourceProvider> resourceProviders;

    /**
     * constructor
     */
    public ResourceProviders() {
        resourceManagers = new ArrayList<ResourcesManager>();
        resourceProviders = new ArrayList<ResourceProvider>();
    }
    
    public List<ResourcesManager> getManagers()
    {
        return new ArrayList<ResourcesManager>(resourceManagers);
    }
    
    public List<ResourceProvider> getProviders()
    {
        return new ArrayList<ResourceProvider>(resourceProviders);
    }
    
    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        
        config.initsb.ttl(ResourcesManager.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        configToManagers(config, ManagerClassKey, ManagerClassDefault);
        config.initsb.level.decrementAndGet();
    }
    
    private void configToManagers(RunnerConfig config, String key, String defaultValue) throws Exception
    {
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(key, config.properties);
        int size = list.size();
        if(size == 0)
        {
            // add the default
            StringPair pair = new StringPair(key, defaultValue);
            list.add(pair);
            ++size;
        }
        
        for(int i=0; i < size; i++)
        {
            Entry<String,String> entry = list.get(i);
            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
            ResourcesManager rm = (ResourcesManager)Class.forName(entry.getValue()).newInstance();
            rm.init(config);
            resourceManagers.add(rm);
            resourceProviders.addAll(rm.getResourceProviders());
        }
    }
    
    public void destroy() 
    {
        try
        {
            int size = resourceManagers.size();
            for(int i=0; i < size; i++)
                resourceManagers.get(i).destroy();
            resourceManagers.clear();
            resourceProviders.clear();
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".destroy failed", e);
        }
    }
    

    public ResourceReserveResult reserveIfAvailable(List<ResourceDescription> reserveResourceRequests, int timeoutSeconds) throws Exception {
        
        // This class has a list of all types of ResourceProvider (like AWSMachineProvider).

    	// Identify each ResourceProvider (like AWSMachineProvider) that understands specific requirements of individual elements of param ResourceDescription and can reserve the corresponding resource.
        // Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes.

        // start retRqr with empty lists; afterward merge reservation results into retRqr
        ResourceReserveResult retRqr = new ResourceReserveResult(new ArrayList<ReservedResource>(),
                                                             new ArrayList<ResourceDescription>(),
                                                             new ArrayList<ResourceDescription>());

        // Until a reservation is made: Invite every known ResourceProvider to reserve each resource in param reserveResourceRequests, as it can and as it will.
        for (ResourceProvider rp : resourceProviders) {
            // but 1st, to avoid multiple reservations: for any past success in filling any of the several rrwa S in retRqr, strip list reserveResourceRequests of that entry
            for (ReservedResource rr : retRqr.getReservedResources()) {
                for (ResourceDescription rd : reserveResourceRequests) {
                    ResourceDescImpl rdi = ResourceDescImpl.class.cast(rd);
                    if (rdi.equals(rr)) {
                        reserveResourceRequests.remove(rd);
                        break; // done with this rd; as we leave the for loop, we also avoid the trouble caused by altering reserveResourceRequests 
                    }
                }                
            }
            if (reserveResourceRequests.isEmpty()) {
                // The act of finding reserveResourceRequests to be empty means all requested resources are reserved.
                //     retRqr reflects that case: it holds all original reservation requests.
                //     We do not not need to check further- not for this rp and not for any remaining rp that has not been checked yet.
                break;
            }

            // asynch behavior
            // chad modified this so it would build, final implementation may or may not want to block here
            Future<ResourceReserveResult> future = rp.reserveIfAvailable(reserveResourceRequests, timeoutSeconds);
            ResourceReserveResult localRqr = future.get();
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> reservedResources) throws ResourceNotReservedException 
    {
        List<Future<? extends ResourceInstance>> retRiList = new ArrayList<>();
        for(int i=0; i < reservedResources.size(); i++) 
        {
            ReservedResource reservedResource = reservedResources.get(i);
            retRiList.add(reservedResource.getResourceProvider().bind(reservedResource));
        }
        return retRiList;
    }  
}