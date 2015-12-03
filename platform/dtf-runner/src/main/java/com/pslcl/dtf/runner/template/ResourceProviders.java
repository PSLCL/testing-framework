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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
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
    
    /**
     * 
     * @param reserveResourceRequests
     * @param timeoutSeconds
     * @return
     * @throws Exception
     */
    public ResourceQueryResult reserveIfAvailable(List<ResourceDescription> reserveResourceRequests, int timeoutSeconds) throws Exception {
    	// First, make a copy rrr of reserveResourceRequests. We can therefore modify the contents of rrr and it will not modify the contents of reserveResourceRequests.
    	//      (This is needed even though Java is stated to be pass by value:
    	//		 It is only the value of the object pointer that is passed by value - modifications to objects, that are passed, are still reflected back to the caller.
    	//       It is as if Java passes objects by reference, but that statement is not considered accurate in Java. In this area, Java is different from several other languages, at a fundamental level.)
    	List<ResourceDescription> rrr = new ArrayList<>();
    	for (ResourceDescription rd: reserveResourceRequests)
    		rrr.add(rd);
    	
        // start retRqr with empty lists; afterward merge individual reservation results into retRqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResource>(),
                                                             new ArrayList<ResourceDescription>(),
                                                             new ArrayList<ResourceDescription>(),
                                                             new ArrayList<ResourceDescription>());

        // Until a reservation is made: Invite every known ResourceProvider, to reserve each resource in rrr (holds contents of param reserveResourceRequests).
        //    Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes.
        //    To accomplish this work, this.resourceProviders holds a list of all types of ResourceProvider (like AWSMachineProvider).
        for (ResourceProvider rp : resourceProviders) {
            // but 1st, to avoid multiple reservations: for any past success in filling any of the several rrwa S in retRqr, strip list rrr of that entry
            for (ReservedResource rr : retRqr.getReservedResources()) {
                for (ResourceDescription rd : rrr) {
                	// rr is a ReservedResource class object (of the ResourceDescription interface), as filled by rp.reserveIfAvailable() down below
                	// each element of rrr is thought to be a ResourceDescriptionImpl (of the ResourceDescription interface) 
                	//     so rd is thought to be a ResourceDescriptionImpl class object (of the ResourceDescription interface) - we will check this now
                	// Check equality of rd to rr (for utility, create temporary rr_rdi from rr)
                	ResourceDescImpl rr_rdi = new ResourceDescImpl(rr.getName(), rr.getCoordinates(),  rr.getAttributes());
                    if (rr_rdi.equals(rd)) {
                        rrr.remove(rd);
                        break; // done with this rd; as we leave the for loop, we also avoid the trouble caused by altering rrr 
                    }
                }                
            }
            if (rrr.isEmpty()) {
                // The act of finding rrr to be empty means all requested resources are reserved.
                //     retRqr reflects that case: it holds all original reservation requests.
                //     We do not not need to check further- not for this rp and not for any remaining rp that has not been checked yet.
                break;
            }

            // note: asynch behavior has been intended all along, but only now is it possible
            // rp.reserveIfAvailable() was formerly synchronous and blocking
            
            // TODO: Now that rp.reserveIfAvailable() is asynch (returns a future), arrange this code and the calling code to split up things so that each needed rp is brought into asynch use, in its turn 
            //		 And after that, recognize that the corresponding binds (which are already asynch), must follow on.
            
            // Until that TODO is implemented, this will have to do: it blocks to get all the reserves in hand at the same time
            Future<ResourceQueryResult> future = rp.reserveIfAvailable(rrr, timeoutSeconds);
            ResourceQueryResult localRqr = future.get();
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> reservedResources) throws ResourceNotReservedException 
    {
        // Note: Current implementation assumes only one instance of dtf-runner is running;
    	//       for each resource in reservedResources, we know it was reserved by a resource provider known to dtf-runner.
        List<Future<? extends ResourceInstance>> retRiList = new ArrayList<>();
        for(int i=0; i < reservedResources.size(); i++) 
        {
            ReservedResource reservedResource = reservedResources.get(i);
            retRiList.add(reservedResource.getResourceProvider().bind(reservedResource));
        }
        return retRiList;
    }  
}