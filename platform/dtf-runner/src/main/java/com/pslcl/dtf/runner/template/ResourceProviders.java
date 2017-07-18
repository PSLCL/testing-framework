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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.ResourcesManager;
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
    private final List<ResourcesManager> resourceManagers;
    private final List<ResourceProvider> resourceProviders;
    private final Logger log;
    private final String simpleName;

    /**
     * constructor
     */
    public ResourceProviders() {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
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

    /**
     * Note: Pair init() with a destroy() call
     * @param config The RunnerConfig
     * @throws Exception on any error
     */
    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();

        config.initsb.ttl(ResourcesManager.class.getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        configToManagers(config, ResourceNames.ResourceManagerClassKey, ResourceNames.ResourceManagerClassDefault);
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
        try {
            int size = resourceManagers.size();
            for(int i=0; i < size; i++)
                resourceManagers.get(i).destroy();
            resourceManagers.clear();
            resourceProviders.clear();
        } catch(Exception e) {
            log.error(this.simpleName + ".destroy() failed", e);
        }
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