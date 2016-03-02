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
package com.pslcl.dtf.test.resource.aws.instance.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.PropertiesFile;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.test.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.provider.SubnetManager;

@SuppressWarnings("javadoc")
public class PersonConfigData
{
    public volatile AmazonSimpleEmailServiceClient sesClient;
    public volatile String topicArn;
    public volatile int sesMaxDelay;
    public volatile int sesMaxRetries;
    public final List<String> inspectors;
    public volatile String subject;
    public volatile String sender;
    public volatile String reply;
    public volatile String givenInspector;
    public volatile String resoucePrefixName;
    
//    private final String sender;
//    private final String reply;
//    private final String subject;
//    private final String recipient;
//    private final Region region;
    
    
    private PersonConfigData()
    {
        inspectors = new ArrayList<String>();
    }

    public static PersonConfigData init(ProgressiveDelayData pdelayData, ResourceDescription resource, PersonConfigData defaultData) throws Exception
    {
        TabToLevel format = new TabToLevel();
        PersonConfigData data = new PersonConfigData();
        format.ttl(PersonConfigData.class.getSimpleName() + " init:");
        format.level.incrementAndGet();
        data.sesMaxDelay = Integer.parseInt(getAttribute(ProviderNames.SesMaxDelayKey, "" + defaultData.sesMaxDelay, resource, format));
        data.sesMaxRetries = Integer.parseInt(getAttribute(ProviderNames.SesMaxRetriesKey, "" + defaultData.sesMaxRetries, resource, format));
        data.sender = getAttribute(ResourceNames.InspectSenderKey, defaultData.sender, resource, format);
        data.reply = getAttribute(ResourceNames.InspectReplyKey, defaultData.reply, resource, format);
        data.subject = getAttribute(ResourceNames.InspectSubjectKey, defaultData.subject, resource, format);
        format.ttl(PersonConfigData.class.getSimpleName() + " inspectors:");
        format.level.incrementAndGet();
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(ResourceNames.InspectInspectorKey, resource.getAttributes());
        if (list.size() == 0)
        {
            // nothing from the resource, use site defaults
            int index = 0;
            for (String inspector : defaultData.inspectors)
            {
                String key = ResourceNames.InspectInspectorKey + index++;
                resource.addAttribute(key, inspector);
                format.ttl(key, " = ", inspector, " (default injected)");
                data.inspectors.add(inspector);
            }
        }else
        {
            data.givenInspector = getAttribute(ResourceNames.ResourcePersonEmailKey, defaultData.givenInspector, resource, format);
            format.ttl(ResourceNames.InspectInspectorKey, " = ", data.givenInspector);
        }
        format.level.decrementAndGet();
        format.ttl("Test name prefix:");
        format.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(ProviderNames.ResourcePrefixNameKey, defaultData.resoucePrefixName, resource, format);
        LoggerFactory.getLogger(PersonConfigData.class).debug(format.sb.toString());
        return data;
    }

    public static PersonConfigData init(RunnerConfig config) throws Exception
    {
        PersonConfigData data = new PersonConfigData();
        config.initsb.ttl(SubnetManager.class.getSimpleName() + " init:");
        config.initsb.level.incrementAndGet();
        data.sesMaxDelay = Integer.parseInt(getAttribute(config, ProviderNames.SesMaxDelayKey, ResourceNames.InspectMaxDelayDefault));
        data.sesMaxRetries = Integer.parseInt(getAttribute(config, ProviderNames.SesMaxRetriesKey, ResourceNames.InspectMaxRetriesDefault));
        data.sender = getAttribute(config, ResourceNames.InspectSenderKey, ResourceNames.InspectSenderDefault);
        data.reply = getAttribute(config, ResourceNames.InspectReplyKey, ResourceNames.InspectReplyDefault);
        data.subject = getAttribute(config, ResourceNames.InspectSubjectKey, ResourceNames.InspectSubjectDefault);

        config.initsb.ttl(SubnetManager.class.getSimpleName() + " inspectors:");
        config.initsb.level.incrementAndGet();
        List<Entry<String, String>> list = PropertiesFile.getPropertiesForBaseKey(ResourceNames.InspectInspectorKey, config.properties);
        // if there are no site configured inspectors, there is no possible default.
        
        if(list.size() == 0)
            throw new Exception("at least one " + ResourceNames.InspectInspectorKey + " must be specified in the site configuration properties file");
        for (Entry<String, String> entry : list)
        {
            config.initsb.ttl(entry.getKey(), " = ", entry.getValue());
            data.inspectors.add(entry.getValue());
        }
        data.givenInspector = null;
        config.initsb.ttl("Test name prefix:");
        config.initsb.level.incrementAndGet();
        data.resoucePrefixName = getAttribute(config, ProviderNames.ResourcePrefixNameKey, ProviderNames.ResourcePrefixNameDefault);
        config.initsb.level.decrementAndGet();
        config.initsb.level.decrementAndGet();
        return data;
    }

    private static String getAttribute(String key, String defaultValue, ResourceDescription resource, TabToLevel format)
    {
        String value = resource.getAttributes().get(key);
        value = StrH.trim(value);
        if (value == null)
        {
            value = defaultValue;
            resource.addAttribute(key, value);
            format.ttl(key, " = ", value, " (default injected)");
        } else
            format.ttl(key, " = ", value);
        return value;
    }

    private static String getAttribute(RunnerConfig config, String key, String defaultValue)
    {
        String value = config.properties.getProperty(key, defaultValue);
        value = StrH.trim(value);
        config.initsb.ttl(key, " = ", value);
        return value;
    }
}