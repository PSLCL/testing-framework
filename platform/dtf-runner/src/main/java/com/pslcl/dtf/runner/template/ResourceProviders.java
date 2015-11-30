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
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceQueryResult;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH.StringPair;

/**
 * Contains ResourceProvider instantiated objects and supplies access to them 
 */
public class ResourceProviders implements ResourceProvider
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
    
    @Override
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
    
    @Override
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
    

    @Override
    public ResourceQueryResult reserveIfAvailable(List<ResourceDescription> reserveResourceRequests, int timeoutSeconds) {
        
        // This class has a list of all types of ResourceProvider (like AWSMachineProvider).

    	// Identify each ResourceProvider (like AWSMachineProvider) that understands specific requirements of individual elements of param ResourceDescription and can reserve the corresponding resource.
        // Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes.

        // start retRqr with empty lists; afterward merge reservation results into retRqr
        ResourceQueryResult retRqr = new ResourceQueryResult(new ArrayList<ReservedResource>(),
                                                             new ArrayList<ResourceDescription>(),
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
            ResourceQueryResult localRqr = rp.reserveIfAvailable(reserveResourceRequests, timeoutSeconds);
            retRqr.merge(localRqr); // merge localRqr into retRqr
        }
        return retRqr;
    }

    @Override
    public Future<? extends ResourceInstance> bind(ReservedResource reservedResource) throws ResourceNotReservedException {
        // Note: Current implementation assumes only one instance is running of dtf-runner; so a resource reserved by a provider automatically matches this dtf-runner
        System.out.println("ResourceProviders.bind(ReservedResource) called with resourceName/resourceAttributes: " + reservedResource.getName() + " / " + reservedResource.getAttributes());

        Future<? extends ResourceInstance> retRI = null;
        try {
            // note that this bind call must fill the return ResourceInstance with not only something like an implementation of MachineInstance or a PersonInstance, but it must fill reference, which comes from param reservedResource
            retRI = reservedResource.getResourceProvider().bind(reservedResource);
        } catch (ResourceNotReservedException e) {
        	System.out.println("ResourceProviders.bind(ReservedResource) receives exception from its specific resource .bind() call " + e);
        	throw e;
        }
        return retRI;
    }

    @Override
    public List<Future<? extends ResourceInstance>> bind(List<ReservedResource> reservedResources) {
        // Note: Current implementation assumes only one instance is running of dtf-runner; so a resource reserved by a provider automatically matches this dtf-runner
        List<Future<? extends ResourceInstance>> retRiList = new ArrayList<>();
        for(int i=0; i<reservedResources.size(); i++) {
            try {
                retRiList.add(this.bind(reservedResources.get(i)));
            } catch (ResourceNotReservedException e) {
                retRiList.add(null);
                System.out.println("ResourceProviders.bind(List<ReservedResource>) stores one null entry in returned list");
            }
        }
        return retRiList;
    }  

    @Override
    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceQueryResult queryResourceAvailability(List<ResourceDescription> resources) {
        // TODO Auto-generated method stub
        return null;
    }

//TODO: AWS implementation does not know what to do with these, chad commented them out of th interface for now    
//    @Override
//    public Map<String, String> getAttributes(String hash) {
//        // ResourceProviders does not supply hashes, attributes or descriptions.
//        return null;
//    }
//
//    @Override
//    public List<String> getNames() {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAttributes()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}