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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
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
    private final Logger log;
    private final String simpleName;

    /**
     * constructor
     */
    public ResourceProviders() {
        resourceManagers = new ArrayList<ResourcesManager>();
        resourceProviders = new ArrayList<ResourceProvider>();
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
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
    public ResourceReserveResult reserve(List<ResourceDescription> reserveResourceRequests, int timeoutSeconds) throws Exception {
    	// First, make a copy rrReq of reserveResourceRequests. We can therefore modify the contents of rrReq and it will not modify the contents of reserveResourceRequests.
    	//      (This is needed even though Java is defined as being pass by value. In this area of language definition, Java is different from several other languages, at a fundamental level.
    	//		 It is only the value of the object pointer that is passed by value - modifications to the actual passed objects are still reflected back to the caller.
    	//       It is as if Java passes its objects by reference, but that statement is not considered accurate in Java. Hence, this paragraph and that we make and use copy rrReq)
    	List<ResourceDescription> rrReq = new ArrayList<>();
    	for (ResourceDescription rd: reserveResourceRequests)
    		rrReq.add(rd);
    	
        // start retRrr with empty lists; afterward merge individual reservation results into retRrr
        ResourceReserveResult retRrr = new ResourceReserveResult(new ArrayList<ReservedResource>(),
                                                                 new ArrayList<ResourceDescription>(),
                                                                 new ArrayList<ResourceDescription>());

        // Until a reservation is made: Invite every known ResourceProvider, to reserve each resource in rrReq (holds contents of param reserveResourceRequests).
        //    Current solution- ask each ResourceProvider, in turn. TODO: Perhaps optimize by asking each ResourceProvider directly, taking advantage of knowledge of each resource provider's hash and attributes.
        //    To accomplish this work, this.resourceProviders holds a list of all types of ResourceProvider (like AWSMachineProvider).
        for (ResourceProvider rp : this.resourceProviders) {
            // but 1st, to avoid multiple reservations: for any past success in filling any of the several rr'S in retRrr, strip list rrReq of that entry
            for (ReservedResource rr : retRrr.getReservedResources()) {
                for (ResourceDescription rd : rrReq) {
                	// rr is a ReservedResource class object (of the ResourceDescription interface), as filled by rp.reserveIfAvailable() down below.
                	// Each element of rrReq is thought to be a ResourceDescriptionImpl (of the ResourceDescription interface); 
                	//     So rd is thought to be a ResourceDescriptionImpl class object (of the ResourceDescription interface). Check it now:
                	// Check equality of rd to rr (for utility, create temporary rr_rdi from rr)
                	ResourceDescImpl rr_rdi = new ResourceDescImpl(rr.getName(), // name comes from the template step as string "machine", "person" or "network"
                												   rr.getCoordinates(),  rr.getAttributes());
                    if (rr_rdi.equals(rd)) {
                        rrReq.remove(rd);
                        break; // done with this rd; as we leave the for loop, we also avoid the trouble caused by altering rrReq 
                    }
                }
            }
            if (rrReq.isEmpty()) {
                // The act of finding rrReq to be empty means all requested resources are reserved.
                //     retRrr reflects that case: it holds all original reservation requests.
                //     We do not not need to check further- not for this rp and not for any remaining rp that has not been checked yet.
                break;
            }

            // note: asynch behavior has been intended all along, but only now is it possible
            // rp.reserveIfAvailable() was formerly synchronous and blocking
            
            // TODO: Now that rp.reserveIfAvailable() is asynch (returns a future), arrange this code and the calling code to split up things so that each needed rp is brought into asynch use, in its turn 
            //		 And after that, recognize that the corresponding binds (which are already asynch), must follow on.
            
            // Until that TODO is implemented, this will have to do: it blocks to get all the reserves in hand at the same time
            Future<ResourceReserveResult> future = rp.reserve(rrReq, timeoutSeconds);

            // When a person resource is requested to be reserved (in rrReq), I have seen AWSMachineProvider *wrongly* return a reserved entry (visible in Eclipse as future.outcome.reserved).
            // We would like to detect this in code, and put out a helpful log message, but this is not possible, right here.
            //    We don't have a code way to query that future (not in api of Future).
            //    Further, we cannot learn anything from rrReq, because rrReq can hold multiple entries of machine, person, and network,
            //        and we can't know which rrReq entries are being handled by rp and which are not being handled by rp.
            ResourceReserveResult localRrr = future.get();
            // Everything in localRrr was filled by rp.
            // So check every ReservedResource entry of localRrr to see that their individual "name"s match the "name"'s of their individual ResourceProviders, and match the "name" of rp
            String rpName = rp.getName();
            for (ReservedResource rr : localRrr.getReservedResources()) {
            	String rrName = rr.getName();
            	String rrRPName = rr.getResourceProvider().getName();
            	if (rpName!=rrName || rpName!=rrRPName) {
            		log.debug(simpleName + "reserveIfAvailable() finds mismatched rpName " + rpName + ", rrName " + rrName + ", rrRPName " + rrRPName);
            		throw new Exception("reserveIfAvailable() finds mismatched ReservedResource name and ResourceProvider names");
            	}
            }
            // Note: Even though we checked these things for integrity, we cannot check that the contents of localRrr (a set of rr's of various resource types) actually match what was requested in rrReq.
            //       Reason: rrReq may hold a variety of resource requests (of differing types), and we have no way to match them up with the array of resultant resource requests held in localRrr.
            //               Even though we could imagine a one to one relationship of inputs to outputs, that ordering is eliminated by the fact that some outputs go to other lists in localRrr, instead of reservedResources.
            //       Almost, we are forced to give up on testing this. However, there is an incomplete solution: for every output type, check that such a request type exists in the input.
            //           This is not a kluge, it is just that it is incomplete. This is not a hail mary, it simply reflects that, when we have no api to check input/output match, we can only check what we can.
            for (ReservedResource rr : localRrr.getReservedResources()) {
            	String rrTypeName = rr.getName(); // "machine" or "person" or "network"
            	boolean success = false;
            	for (ResourceDescription rd : rrReq) {
            		String rdTypeName = rd.getName(); // "machine" or "person" or "network"
            		if (rrTypeName.equals(rdTypeName)) {
            			success = true;
            			break;
            		}
            	}
            	if (!success) {
            		if (true) { // false: temporarily allow a not-requested resource type to pass through, in order to let deploys and inspects to see it and to error out
            			log.debug(simpleName + "reserveIfAvailable() finds ReserveResource type " + rrTypeName + " does not match any original resource request type");
            			throw new Exception("reserveIfAvailable() finds ReservedResource type was not requested");
            		}
            	}
            }
            
            retRrr.merge(localRrr); // merge localRrr into retRrr
        }
        return retRrr;
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