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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusEvent;
import com.pslcl.dtf.core.runner.config.status.ResourceStatusListener;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

public class TemplateProvider implements ResourceStatusListener {
	private long uniqueMark; // synchronized access only
	private final Map<Long,InstancedTemplate> templateReleaseMap; // tracks Long unique marker numbers; synchronized access only
	// Note: Although Java is strictly pass by value, the value we store in this map is a reference to an InstancedTemplate object. The map does not hold the Java object itself.
	//       When that value is eventually extracted from this map, it is still a reference to the original object; all transitory changes in that object are reflected.
	
    private final Map<byte[],InstancedTemplate> availableInstancedTemplates; // note: this populates in the destroy method
    private final ResourceProviders resourceProviders;
    private volatile StatusTracker statusTracker;
    private final Logger log;
    private final String simpleName;
    
    public TemplateProvider() 
    {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
    	this.uniqueMark = 0;
    	this.templateReleaseMap = new HashMap<>();
        this.availableInstancedTemplates = new HashMap<byte[],InstancedTemplate>();
        this.resourceProviders = new ResourceProviders();
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

    public void destroy(byte [] template_hash, InstancedTemplate iT) {
        // can also choose to destroy iT; this is early impl with no smarts
        availableInstancedTemplates.put(template_hash, iT);
    }
    
    /**
     * Cleanup operation of the TemplateProvider
     */
    public void destroy() 
    {
    	log.debug(this.simpleName + "- Destroying TemplateProvider");
    	// release (and destroy) each known InstancedTemplate
    	while (true) {
    		synchronized(this.templateReleaseMap) {
    			if (this.templateReleaseMap.isEmpty())
    				break;
    			InstancedTemplate oneFoundIT = InstancedTemplate.class.cast(this.templateReleaseMap.values().toArray()[0]);
    			this.releaseTemplate(oneFoundIT); // besides releasing oneFoundIT, this removes its entry in this.templateReleaseMap
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
    	synchronized(this.templateReleaseMap) {
    		long retUnique = this.uniqueMark++;
    		this.templateReleaseMap.put(retUnique, iT);
    		return retUnique;
		} // end synchronized()
	}

	
    /**
     * Release the template, or determine that it is being released by other code.
     * 
     * @param iT
     */
    public void releaseTemplate(InstancedTemplate iT) {
    	boolean found = false;
    	String templateID = iT.getTemplateID();
		long templateUniqueMark = iT.getUniqueMark();
    	
    	// Avoid conflict: the same template can be released by normal template processing, or by us (TemplateProvider) being asked to go down, such as happens at dtf application exit.
    	synchronized (this.templateReleaseMap) {
    		if (this.templateReleaseMap.containsKey(templateUniqueMark)) {
    			found = true;
    			this.templateReleaseMap.remove(templateUniqueMark);
    		}
    	} // end synchronized()

    	if (found) {
			log.debug(simpleName + "releaseTemplate() releases template, for template hash " + templateID + ", uniqueMark " + templateUniqueMark);
    		iT.destroy();
    	} else {
			log.debug(simpleName + "releaseTemplate() finds no entry in templateReleaseMap, for template hash " + templateID + ", uniqueMark " + templateUniqueMark);
    		// no entry <= no resources were bound  
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
        InstancedTemplate iT = availableInstancedTemplates.get(dbTemplate.hash);
        if (iT != null) {
            // we will now re-use iT
            availableInstancedTemplates.remove(dbTemplate.hash);
            // Note: This is early impl with no smarts to optimize anything. At this line, they asked for the instantiated template, they get it, and now it is not available to another user.
        } else {
        	iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine); // sets internal StepsParser object, assigns a Long unique marker number, and runs template steps
        }
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