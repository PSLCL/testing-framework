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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourcesManager;
import com.pslcl.dtf.resource.aws.provider.AwsMachineProvider;
import com.pslcl.dtf.resource.aws.provider.AwsNetworkProvider;
import com.pslcl.dtf.resource.aws.provider.AwsPersonProvider;

public class ResourcesController implements ResourcesManager
{
    public static final String MachineProviderClassKey = "pslcl.dtf.runner.template.machine-provider-class"; 
    public static final String PersonProviderClassKey = "pslcl.dtf.runner.template.person-provider-class"; 
    public static final String NetworkProviderClassKey = "pslcl.dtf.runner.template.network-provider-class";
    
    public static final String MachineProviderClassDefault = "com.pslcl.dtf.resource.aws.provider.AwsMachineProvider";
    public static final String PersonProviderClassDefault = "com.pslcl.dtf.resource.aws.provider.AwsPersonProvider";
    public static final String NetworkProviderClassDefault = "com.pslcl.dtf.resource.aws.provider.AwsNetworkProvider";
    
    private final List<ResourceProvider> resourceProviders;
    private final AwsMachineProvider machineProvider;
    private final AwsNetworkProvider networkProvider;
    private final AwsPersonProvider personProvider;

    public ResourcesController()
    {
         resourceProviders = new ArrayList<ResourceProvider>();
         machineProvider = new AwsMachineProvider(this);
         networkProvider = new AwsNetworkProvider(this);
         personProvider = new AwsPersonProvider(this);
         resourceProviders.add(machineProvider);
         resourceProviders.add(networkProvider);
         resourceProviders.add(personProvider);
    }
    
    @Override
    public List<ResourceProvider> getResourceProviders()
    {
        return new ArrayList<ResourceProvider>(resourceProviders);
    }
    
    @Override
    public void init(RunnerConfig config) throws Exception
    {
        config.initsb.level.incrementAndGet();
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
    }
        
//        // Note: Do not include ResourceProviders in this list
//        config.initsb.ttl(MachineProvider.class.getSimpleName(), " Initialization");
//        config.initsb.level.incrementAndGet();
//        configToProviders(config, MachineProviderClassKey, MachineProviderClassDefault);
//        config.initsb.level.decrementAndGet();
//        
//        config.initsb.ttl(PersonProvider.class.getSimpleName(), " Initialization");
//        config.initsb.level.incrementAndGet();
//        configToProviders(config, PersonProviderClassKey, PersonProviderClassDefault);
//        config.initsb.level.decrementAndGet();
//        
//        config.initsb.ttl(NetworkProvider.class.getSimpleName(), " Initialization");
//        config.initsb.level.incrementAndGet();
//        configToProviders(config, NetworkProviderClassKey, NetworkProviderClassDefault);
//        config.initsb.level.decrementAndGet();
//        config.initsb.level.decrementAndGet();
//        config.initsb.level.decrementAndGet();
    
//    private void configToProviders(RunnerConfig config, String key, String defaultValue) throws Exception
//    {
//        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(key, config.properties);
//        int size = list.size();
//        if(size == 0)
//        {
//            // add the default
//            StringPair pair = new StringPair(key, defaultValue);
//            list.add(pair);
//            ++size;
//        }
//        
//        for(int i=0; i < size; i++)
//        {
//            Entry<String,String> entry = list.get(i);
//            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
//            ResourceProvider rp = (ResourceProvider)Class.forName(entry.getValue()).newInstance();
//            rp.init(config);
//            resourceProviders.add(rp);
//        }
//    }
    
    @Override
    public void destroy() 
    {
        try
        {
            int size = resourceProviders.size();
            for(int i=0; i < size; i++)
                resourceProviders.get(i).destroy();
            resourceProviders.clear();
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".destroy failed", e);
        }
    }
}
