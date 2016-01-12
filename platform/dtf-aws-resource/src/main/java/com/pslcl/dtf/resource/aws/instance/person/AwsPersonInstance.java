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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.provider.person.AwsPersonProvider;
import com.pslcl.dtf.resource.aws.provider.person.PersonReservedResource;

@SuppressWarnings("javadoc")
public class AwsPersonInstance implements PersonInstance
{
    public final PersonReservedResource reservedResource;
    public final ResourceDescription resource;
    public final RunnerConfig runnerConfig;
    public final PersonConfigData pconfig;
    public final String recipient;

    public AwsPersonInstance(PersonReservedResource reservedResource, RunnerConfig runnerConfig)
    {
        this.reservedResource = reservedResource;
        this.pconfig = reservedResource.pconfig;
        this.recipient = reservedResource.email;
        resource = reservedResource.resource;
        this.runnerConfig = runnerConfig;
    }

    @Override
    public String getName()
    {
        return resource.getName();
    }

    @Override
    public Map<String, String> getAttributes()
    {
        Map<String, String> map = resource.getAttributes();
        synchronized (map)
        {
            return new HashMap<String, String>(map);
        }
    }

    @Override
    public void addAttribute(String key, String value)
    {
        Map<String, String> map = resource.getAttributes();
        synchronized (map)
        {
            map.put(key, value);
        }
    }

    @Override
    public ResourceProvider getResourceProvider()
    {
        return resource.getCoordinates().getProvider();
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return resource.getCoordinates();
    }

    @Override
    public Future<Void> inspect(String instructions, InputStream fileContent, String fileName)
    {
        try
        {
//            Destination destination = new Destination().withToAddresses(new String[] {recipient});
//            Content subject = new Content().withData(pconfig.subject);
//            Content textBody = new Content().withData(instructions);
//            Body body = new Body().withText(textBody);
//            Message message = new Message().withSubject(subject).withBody(body);
//
//            //@formatter:off
//            SendEmailRequest request = new SendEmailRequest()
//                .withSource(pconfig.sender)
//                .withDestination(destination)
//                .withMessage(message);
//            //@formatter:on
            AmazonSimpleEmailServiceClient sesClient = ((AwsPersonProvider) reservedResource.getResourceProvider()).manager.sesClient; 
//            sesClient.sendEmail(request);
            //@formatter:off
            InspectWithAttachmentFuture iwif = new InspectWithAttachmentFuture(
                            sesClient, pconfig, recipient, fileName, fileContent, instructions, reservedResource.pdelayData);
            //@formatter:off
            return runnerConfig.blockingExecutor.submit(iwif);
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).info("look here", e);
        }
        return null;
    }
}
