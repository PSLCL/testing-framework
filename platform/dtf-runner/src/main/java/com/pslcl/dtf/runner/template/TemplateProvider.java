/*
 * Copyright (c) 2010-2017, Panasonic Corporation.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private final Object synchObj;
    private final MultiValuedMap<byte[],InstancedTemplate> reusableInstancedTemplates; // MultiValuedMap: more than one template may exist for any one template hash key
    private long templateUniqueInstanceID;
    private final Map<Long,InstancedTemplate> templateReleaseMap; // tracks each instance by unique marker number key
    // Note: Although Java is strictly pass by value, the value we store in this map is a reference to an InstancedTemplate object. The map does not hold the Java object itself.
    //       When that value is eventually extracted from this map, it is still a reference to the original object; all transitory changes in that object are reflected.

    private boolean goingDown;
    private final ResourceProviders resourceProviders;
    private volatile StatusTracker statusTracker;
    private final Logger log;
    private final String simpleName;

    /**
     *
     * @param templateHash byte array hash of the template
     * @return
     */
    private Object[] getStoredReusableInstancedTemplates(byte [] templateHash) {
        Collection<InstancedTemplate> collectionIT = this.reusableInstancedTemplates.get(templateHash);
        Object[] arrayIT = collectionIT.toArray();

        // employ special workaround
        if (arrayIT.length==0) {
            // Workaround to a bug with MultiValuedMap<byte[], InstancedTemplate>, where
            //  after a test run stores a template, a new test run cannot find it.

            // This appears not be be sufficient, but needs more testing to know; this first attempt is a smoother workaround.
//          boolean keyIsContained = this.reusableInstancedTemplates.containsKey(templateHash);
//          if (!keyIsContained) {
//              byte [] workaroundTemplateHash = Arrays.copyOf(templateHash, templateHash.length);
//              boolean keyIsContained_2ndTry = this.reusableInstancedTemplates.containsKey(workaroundTemplateHash);
//              if (keyIsContained_2ndTry) {
//                  log.debug(this.simpleName + "getStoredReusableInstancedTemplates() <smoother workaround> successfully looked up key workaroundTemplateHash " + DBTemplate.getId(workaroundTemplateHash));
//              }
//          }

            // this is the "working" workaround to the bug with MultiValuedMap<byte[], InstancedTemplate>
            // 1st: find that our templateHash is stored (which refers to 1+ InstancedTemplate objects)
            @SuppressWarnings("ZeroLengthArrayAllocation ")
            byte[] matchedTemplateHash = this.getMatchedHeldHashKey(templateHash); // this method is a workaround to failing this.reusabeInstancedTemplates.get(templateHash);
            if (matchedTemplateHash == null) {
                log.debug(".getStoredReusableInstancedTemplates() has no stored reusableITs for templateHash: " + DBTemplate.getId(templateHash));
                return new Object[]{};
            }
            Collection<InstancedTemplate> localCollectionIT = this.reusableInstancedTemplates.get(matchedTemplateHash);
            arrayIT = localCollectionIT.toArray();
            log.debug(".getStoredReusableInstancedTemplates() <workaround kicks in> to find " + arrayIT.length + " reusableITs, for templateHash: " + DBTemplate.getId(templateHash));
        }

        return arrayIT;
    }

    /**
     * Note: Call this only with this.synchObj locked
     * @param templateHash byte array hash of the template
     * @return InstancedTemplate
     */
    private InstancedTemplate obtainOneReusableInstancedTemplate(byte[] templateHash) {
        // get the collection of matching but identical reusable InstancedTemplates, and pick one
        Object[] arrayIT = this.getStoredReusableInstancedTemplates(templateHash);
        log.debug(this.simpleName + "obtainOneReusableInstancedTemplate() finds " + arrayIT.length + " reusableITs, for templateHash: " + DBTemplate.getId(templateHash));

        InstancedTemplate retIT = null;
        if (arrayIT.length > 0)
            retIT = (InstancedTemplate)arrayIT[0];
        return retIT;
    }

    /**
     * Workaround technique
     *
     * @param templateHash byte array hash of the template
     * @return
     */
    private byte[] getMatchedHeldHashKey(byte [] templateHash) {
        // the workaround is to get the full keySet() first, and use it
        //    the interesting thing is the the found matching byte[] seems to be acceptable, rather than using param templateHash directly
        //      even though these 2 byte arrays are supposedly identical
        //      So the bug being worked around is apparently that .keySet() enables something in MultiValuedMap
        Set<byte[]> heldHashKeys = this.reusableInstancedTemplates.keySet();
        for (byte[] heldHashKey : heldHashKeys) {
            if (Arrays.equals(heldHashKey, templateHash))
                return heldHashKey;
        }
        return null;
    }

    /**
     * Note: Caller must lock on this.synchObj
     * @param templateHash byte array hash of the template
     * @return boolean true for one entry removed
     */
    private boolean removeOneEntry_reusableTemplateList(byte [] templateHash) {
        // if in the reusable list, remove one entry having our templateHash
        Object[] originalArrayIT = this.getStoredReusableInstancedTemplates(templateHash);
        log.debug(".removeOneEntry_reusableTemplateList() finds " + originalArrayIT.length + " originally stored reusableITs, for templateHash: " + DBTemplate.getId(templateHash));
        int lengthArrayIT = originalArrayIT.length;
        if (lengthArrayIT == 0)
            return false;

        // 1st: find that our templateHash is stored (which refers to 1+ InstancedTemplate objects)
        byte[] matchedTemplateHash = this.getMatchedHeldHashKey(templateHash); // this method is a workaround to failing this.reusabeInstancedTemplates.get(templateHash);
        if (matchedTemplateHash == null) {
            log.debug(".removeOneEntry_reusableTemplateList() has no stored reusableITs to remove");
            return false;
        }

        // 2nd: remove all InstancedTemplate instances, for key templateHash, from .reusableInstancedTemplates
        /*Collection<InstancedTemplate> localCollectionRemovedIT =*/
        this.reusableInstancedTemplates.remove(matchedTemplateHash); // removes all entries for templateHash

        // 3rd: EXCEPT for *** 1 **** entry, add back all InstancedTemplate objects to the reusable list
        for (int i=1; i<lengthArrayIT; i++)
            this.reusableInstancedTemplates.put(templateHash, (InstancedTemplate)(originalArrayIT[i]));
        log.debug(".removeOneEntry_reusableTemplateList() removed one InstancedTemplate entry, for templateHash " + DBTemplate.getId(templateHash) +
                ", " + (lengthArrayIT-1) + " now held");
        return true;
    }

    /**
     *
     */
    public TemplateProvider()
    {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.templateUniqueInstanceID = 0;
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
     * @param iT The InstancedTemplate
     */
    long addToReleaseMap(InstancedTemplate iT) {
        synchronized(this.synchObj) {
            // leave this.reusableInstancedTemplates alone (this method has nothing to do with that)
            long retUnique = this.templateUniqueInstanceID++;
            this.templateReleaseMap.put(retUnique, iT);
            log.debug(this.simpleName + "addToReleaseMap() adds " + iT.getTemplateID() + " to templateReleaseMap, assigns templateUniqueInstanceID " + retUnique);
            return retUnique;
        } // end synchronized()
    }

    /**
     * Release the template, or determine that it shall be reused.
     *
     * @param iT The InstancedTemplate
     */
    public void releaseTemplate(InstancedTemplate iT) {
        boolean reuse = false;
        long templateInstanceID = iT.getTemplateInstanceID();

        // Use locking to avoid conflict: the same template can be released by normal template processing, or by us (TemplateProvider) being asked to go down, such as happens at dtf application exit.
        synchronized(this.synchObj) {
            if (!this.goingDown) {
                // determine template reuse
//              if (iT.isReusable()) // temporarily TODO: template reuse not supported yet- enable this after reuse works
//                  reuse = true;
            }

            if (reuse) {
                byte[] templateHash = iT.getTemplateHash();
                    this.reusableInstancedTemplates.put(templateHash, iT);
                    log.debug(this.simpleName + "releaseTemplate() will reuse templateID  " + iT.getTemplateID() +  ", of templateInstanceID " + templateInstanceID +  "; adds templateHash key to reusableInstancedTemplates: " + DBTemplate.getId(templateHash));
            } else {
                // if no entry is found here, then this template had bound no resources
                if (this.templateReleaseMap.containsKey(templateInstanceID)) {
                    // as a setup for actual iT destroy(), below, remove iT from our 2 lists
                    InstancedTemplate removedIT = this.templateReleaseMap.remove(templateInstanceID);
                    if (removedIT != null)
                        log.debug(this.simpleName + "releaseTemplate() removes template " + iT.getTemplateID() + ", from templateReleaseMap, of templateInstanceID " + templateInstanceID);
                }
                // TODO: remove this entry only if this iT had been placed in this list, originally
                this.removeOneEntry_reusableTemplateList(iT.getTemplateHash());
            }
        } // end synchronized()

        // perform actual iT destroy, if indicated
        if (!reuse) {
            log.debug(simpleName + "releaseTemplate() destroys template, for templateID " + iT.getTemplateID() + ", templateInstanceID " + templateInstanceID);
            iT.destroy();
        }
    }

    /**
     * Get an instanced template. An instanced template has executed all its steps. It can be reused, unless disqualified.
     *
     * Instance a template
     *     By instantiating a new template and running its steps for the first time, or
     *     By reusing a template and not running any of its steps.
     *
     * Note: Blocking call
     * @param reCore The RunEntryCore object
     * @param dbTemplate The DBTemplate object that described the template
     * @param runnerMachine The RunnerMachine
     * @return The InstancedTemplate
     * @throws Exception on any error
     */
    public InstancedTemplate getInstancedTemplate(RunEntryCore reCore, DBTemplate dbTemplate, RunnerMachine runnerMachine) throws Exception {
        // Note: At some future time of dtf-runner shutdown, we release every template. To distinguish them, iT's constructor assigned each iT instance a unique mark number (a Long).
        // Note: At any one time, there can exist multiple instanced templates of the same templateID string. They differ mainly in which test run (or parent template) had used each of these instanced templates.

        // First check our tracking- is a matching reusable template available?
        InstancedTemplate iT = null;
        if (!dbTemplate.isTopLevelTemplate()) {
            byte [] templateHash = dbTemplate.hash;
            synchronized (this.synchObj) {
                iT = this.obtainOneReusableInstancedTemplate(templateHash);
                if (iT != null) {
                    // we reuse iT; remove iT instance from this.reusableInstancedTemplates
                    log.debug(this.simpleName + "getInstancedTemplate() obtains template by reuse: " + iT.getTemplateID() + ", of templateInstanceID " + iT.getTemplateInstanceID());
                    this.removeOneEntry_reusableTemplateList(templateHash);
                    // Note: This is early impl with no smarts to optimize anything. At this line, they asked for an instantiated template, they get it as a reused one, and now it is not available to another user for reuse.
                }
            }
        }
        if (iT == null)
            iT = new InstancedTemplate(reCore, dbTemplate, runnerMachine); // assigns a Long unique marker number and runs template steps
        return iT;
    }

    @Override
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
            case Down:
                break;
        }
    }

}
