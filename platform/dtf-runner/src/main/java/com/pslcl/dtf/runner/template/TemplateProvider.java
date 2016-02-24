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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusEvent;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusListener;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * 
 * 
 *
 */
public class TemplateProvider implements ResourceStatusListener {
	// template tracking is synchronized
	private Object synchObj;
	private final MultiValuedMap<byte[],InstancedTemplate> reusableInstancedTemplates; // MultiValuedMap: more than one template may exist for any one template hash key
	private long uniqueMark;
	private final Map<Long,InstancedTemplate> templateReleaseMap; // tracks each instance by unique marker number key
	// Note: Although Java is strictly pass by value, the value we store in this map is a reference to an InstancedTemplate object. The map does not hold the Java object itself.
	//       When that value is eventually extracted from this map, it is still a reference to the original object; all transitory changes in that object are reflected.
	
	private boolean goingDown;
    private final ResourceProviders resourceProviders;
    private volatile StatusTracker statusTracker;
    private final Logger log;
    private final String simpleName;
    
    /**
     * @note Call this only with this.synchObj locked 
     * @param templateHash
     * @return
     */
    private InstancedTemplate obtainOneReusableInstancedTemplate(byte[] templateHash) {
    	// get the collection of matching but identical reusable InstancedTemplates, and pick one
		Collection<InstancedTemplate> collectionIT = reusableInstancedTemplates.get(templateHash);
		Object[] arrayIT = collectionIT.toArray();
		InstancedTemplate retIT = null;
		if (arrayIT.length > 0)
			retIT = (InstancedTemplate)arrayIT[0];
		return retIT;	
    }
    
    /**
     * @note Caller must lock on this.synchObj
     * @param templateHash
     */
    private void removeOneEntry_reusableList(byte [] templateHash) {
		// if in the reusable list, remove one entry having our key: 1st find all entries for our hash key, 2nd clear the list, 3rd remove one from the original list and store the smaller list back to the resuable list
		Collection<InstancedTemplate> collectionIT = reusableInstancedTemplates.get(templateHash);
		Object[] arrayIT = collectionIT.toArray();
		
		this.reusableInstancedTemplates.remove(templateHash); // removes all entries for the key
		
		for (int i=1; i < arrayIT.length; i++)
			this.reusableInstancedTemplates.put(templateHash, (InstancedTemplate)(arrayIT[i]));
		
		if (arrayIT.length > 0)
			log.debug(this.simpleName + "removeOneEntry)reusableList() found " + arrayIT.length + " entries in reusableInstancedTemplates, for template hash " + templateHash + "; 1 entry removed");
    }


    /**
     * 
     */
    public TemplateProvider() 
    {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
    	this.uniqueMark = 0;
    	this.synchObj = new Object();
    	this.templateReleaseMap = new HashMap<>();
        this.reusableInstancedTemplates = new ArrayListValuedHashMap<byte[],InstancedTemplate>();
        this.resourceProviders = new ResourceProviders();
        this.goingDown = false;
    }

    public ResourceProviders getResourceProviders()
    {
        return resourceProviders;
    }
    
    public void init(RunnerConfig config) throws Exception
    {
        statusTracker = config.statusTracker;
        statusTracker.registerResourceStatusListener(this);
        resourceProviders.init(config);
    }

    /**
     * Cleanup operation of the TemplateProvider
     */
    public void destroy() 
    {
    	log.debug(this.simpleName + "- Destroying TemplateProvider");
    	this.goingDown = true;
    	// release (and destroy) each known InstancedTemplate
    	while (true) {
        	synchronized(this.synchObj) {
    			if (this.templateReleaseMap.isEmpty())
    				break;
    			InstancedTemplate oneFoundIT = InstancedTemplate.class.cast(this.templateReleaseMap.values().toArray()[0]);
    			this.releaseTemplate(oneFoundIT); // besides releasing oneFoundIT, this removes its entry in this.templateReleaseMap and this.reusableInstancedTemplates
    		} // end synchronized()
    	}

    	// remaining cleanup
        resourceProviders.destroy();
        statusTracker.deregisterResourceStatusListener(this);
    }
    
	/**
	 * 
	 * @param iT
	 */
	long addToReleaseMap(InstancedTemplate iT) {
    	synchronized(this.synchObj) {
    		// leave this.reusableInstancedTemplates alone (this method has nothing to do with that)
    		long retUnique = this.uniqueMark++;
    		this.templateReleaseMap.put(retUnique, iT);
        	log.debug(this.simpleName + "addToReleaseMap() adds " + iT.getTemplateID() + " to templateReleaseMap, assigns uniqueMark " + retUnique);
    		return retUnique;
		} // end synchronized()
	}

	
    /**
     * Release the template, or determine that it shall be reused.
     * 
     * @param iT
     */
    public void releaseTemplate(InstancedTemplate iT) {
		boolean reuse = false;
    	boolean found = false;
		long templateUniqueMark = iT.getUniqueMark();
		
    	// Use locking to avoid conflict: the same template can be released by normal template processing, or by us (TemplateProvider) being asked to go down, such as happens at dtf application exit.
    	synchronized(this.synchObj) {
    		if (!this.goingDown) {
        		// determine template reuse
    			if (iT.isReusable()) // TODO: improve this obvious shortcut
    				reuse = true;
    		}
    		
    		if (reuse) {
    			byte[] templateHash = iT.getTemplateHash();
        		this.reusableInstancedTemplates.put(templateHash, iT);
    			log.debug(this.simpleName + "releaseTemplate() found adds template of hash " + templateHash + " to reusableInstancedTemplates");
    		} else {
    			// actually release iT
        		if (this.templateReleaseMap.containsKey(templateUniqueMark)) {
        			// as a setup for actual iT destroy(), below, remove iT from our 2 lists
        			found = true;
        			InstancedTemplate removedIT = this.templateReleaseMap.remove(templateUniqueMark);
        			if (removedIT != null)
        	        	log.debug(this.simpleName + "releaseTemplate() removes template " + iT.getTemplateID() + ", from templateReleaseMap, of uniqueMark " + templateUniqueMark);
        			this.removeOneEntry_reusableList(iT.getTemplateHash());
        		}
    		}
    	} // end synchronized()

    	// perform actual iT destroy, if indicated
    	String templateID = iT.getTemplateID();
    	if (found) {
			log.debug(simpleName + "releaseTemplate() destroys template, for template hash " + templateID + ", uniqueMark " + templateUniqueMark);
    		iT.destroy();
    	} else if (!reuse){
			log.debug(simpleName + "releaseTemplate() finds no entry in templateReleaseMap, for template hash " + templateID + ", uniqueMark " + templateUniqueMark);
    		// no entry <= no resources were bound
    	} else {
    		log.debug("reuse");
    	}
    }
    
    /**
     * Get an instanced template. An instanced template has executed all its steps. It can be reused, unless disqualified.
     * 
     * Instance a template
     *     By instantiating a new template and running its steps for the first time, or
     *     By reusing a template and not running any of its steps.
     * 
     * @param reCore
     * @param dbTemplate
     * @param runnerMachine
     * @return
     * @throws Exception
     */
    public InstancedTemplate getInstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
        // First check our tracking- is a matching reusable template available?
        // Note: At any one time, there can exist multiple instanced templates of the same templateID string. They differ only, or nearly only, in which test run (or parent template) had used each one.
        //       At some future time of dtf-runner shutdown, we release every template. To distinguish them, iT's constructor assigned each iT instance a unique mark number (a Long).
    	InstancedTemplate iT = null;
    	byte [] templateHash = dbTemplate.hash;
    	synchronized (this.synchObj) {
    		iT = this.obtainOneReusableInstancedTemplate(templateHash); 
            if (iT != null) {
            	// reuse iT
            	this.removeOneEntry_reusableList(templateHash);
                // Note: This is early impl with no smarts to optimize anything. At this line, they asked for an instantiated template, they get it as a reused one, and now it is not available to another user for reuse.
            }
    	}
        if (iT == null)
        	iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine); // sets internal StepsParser object, assigns a Long unique marker number, and runs template steps
        return iT;
    }
    
    public void resourceStatusChanged(ResourceStatusEvent status)
    {
//        int templateStepNumber = StepsParser.resourceToLineMap.get(status.coordinate);  
//        LoggerFactory.getLogger(getClass()).debug("\n" + getClass().getSimpleName() + ".resourceStatusChanged hit: " + status.toString());
        switch(status.status)
        {
            case Error:
                break;
            case Alert:
                break;
            case Ok:
                break;
            case Warn:
                break;
            default:
                break;
        }
    }
    
}