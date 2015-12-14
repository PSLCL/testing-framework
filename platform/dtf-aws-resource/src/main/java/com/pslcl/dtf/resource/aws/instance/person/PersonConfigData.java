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
package com.pslcl.dtf.resource.aws.instance.person;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;

@SuppressWarnings("javadoc")
public class PersonConfigData
{
    public volatile String topicArn;
    public volatile int snsMaxDelay;
    public volatile int snsMaxRetries;
    public volatile String endpoint;

    private PersonConfigData()
    {
    }

    public static PersonConfigData init(ProgressiveDelayData pdelayData, ResourceDescription resource, PersonConfigData defaultData) throws Exception
    {
        TabToLevel format = new TabToLevel();
        PersonConfigData data = new PersonConfigData();
        format.ttl(PersonConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();
        data.topicArn = getAttribute(ProviderNames.SnsTopicArnKey, defaultData.topicArn, resource, format);
        data.endpoint = getAttribute(ClientNames.EndpointKey, defaultData.endpoint, resource, format);
        data.snsMaxDelay = Integer.parseInt(getAttribute(ProviderNames.SnsMaxDelayKey, ""+defaultData.snsMaxDelay, resource, format));
        data.snsMaxRetries = Integer.parseInt(getAttribute(ProviderNames.SnsMaxRetriesKey, ""+defaultData.snsMaxRetries, resource, format));
        format.level.decrementAndGet();
        LoggerFactory.getLogger(PersonConfigData.class).debug(format.sb.toString());
        return data;
    }
    
    public static PersonConfigData init(RunnerConfig config) throws Exception
    {
        PersonConfigData data = new PersonConfigData();
        config.initsb.ttl(SubnetManager.class.getSimpleName() + " init:");
        config.initsb.level.incrementAndGet();
        data.topicArn = getAttribute(config, ProviderNames.SnsTopicArnKey, ProviderNames.SnsTopicArnDefault);
        data.endpoint = getAttribute(config, ClientNames.EndpointKey, ClientNames.EndpointDefault);
        data.snsMaxDelay = Integer.parseInt(getAttribute(config, ProviderNames.SnsMaxDelayKey, ProviderNames.SnsMaxDelayDefault));
        data.snsMaxRetries = Integer.parseInt(getAttribute(config, ProviderNames.SnsMaxRetriesKey, ProviderNames.SnsMaxRetriesDefault));
        config.initsb.level.decrementAndGet();
        return data;
    }
    
    private static String getAttribute(String key, String defaultValue, ResourceDescription resource, TabToLevel format)
    {
        String value = resource.getAttributes().get(key);
        if(value == null)
        {
            value = defaultValue;
            resource.addAttribute(key, value);
            format.ttl(key, " = ", value, " (default injected)");
        }else
            format.ttl(key, " = ", value);
        return value;
    }
    
    private static String getAttribute(RunnerConfig config, String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        config.initsb.ttl(key, " = ", value);
        return value;
    }
}