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
package com.pslcl.dtf.resource.aws.provider.person;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.person.PersonConfigData;

@SuppressWarnings("javadoc")
public class PersonReserveFuture implements Callable<List<ResourceReserveDisposition>>
{
    public static final String SnsMidStr = "sns";

    private final Logger log;
    private final AwsPersonProvider provider;
    private final List<ResourceDescription> resources;
    private final int timeoutSeconds;

    public PersonReserveFuture(AwsPersonProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        log = LoggerFactory.getLogger(getClass());
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public List<ResourceReserveDisposition> call() throws Exception
    {
        List<ResourceReserveDisposition> list = new ArrayList<ResourceReserveDisposition>();

        for (ResourceDescription resource : resources)
        {
            try
            {
                ProgressiveDelayData pdelayData = new ProgressiveDelayData(provider, resource.getCoordinates());
                PersonConfigData config = PersonConfigData.init(pdelayData, resource, provider.defaultPersonConfigData);
                pdelayData.maxDelay = config.snsMaxDelay;
                pdelayData.maxRetries = config.snsMaxRetries;

                resource.getCoordinates().setManager(pdelayData.provider.manager);
                resource.getCoordinates().setProvider(pdelayData.provider);
                checkForTopic(pdelayData, config);
                //@formatter:off
                list.add(new ResourceReserveDisposition(
                                resource, 
                                new ReservedResource(
                                                resource.getCoordinates(), 
                                                resource.getAttributes(), 
                                                timeoutSeconds)));                    
                //@formatter:on
            } catch (Exception e)
            {
                log.warn(getClass().getSimpleName() + ".reserve has invalid resources: " + resource.toString());
                ResourceReserveDisposition disposition = new ResourceReserveDisposition(resource);
                disposition.setInvalidResource();
                list.add(disposition);
            }
        }
        return null;
    }

    private void checkForTopic(ProgressiveDelayData pdelayData, PersonConfigData config) throws FatalResourceException
    {
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(SnsMidStr, "listTopics");
        do
        {
            try
            {
                ByteBuffer bb;
                RawMessage rm = new RawMessage().withData(bb);
                SendRawEmailRequest ser = new SendRawEmailRequest().withRawMessage(rawMessage)
                provider.manager.sesClient.sensetEndpoint(config.endpoint);
                provider.manager.snsClient.setEndpoint(config.endpoint);
                
                
                //formatter:off
                PublishRequest pr = new PublishRequest()
                    .withMessage("the message")
                    .withSubject("the subject")
                    .withTopicArn(config.topicArn);
                //formatter:on
                provider.manager.snsClient.publish(pr);
                
                ListTopicsResult listTopicsResult = provider.manager.snsClient.listTopics();
                String nextToken = listTopicsResult.getNextToken();
                List<Topic> topics = listTopicsResult.getTopics();

                // ListTopicResult contains only 100 topics hence use next token to get
                // next 100 topics.
                while (nextToken != null)
                {
                    listTopicsResult = provider.manager.snsClient.listTopics();
                    nextToken = listTopicsResult.getNextToken();
                    topics.addAll(listTopicsResult.getTopics());
                }
                for (Topic t : topics)
                {
                    if (t.getTopicArn().equals(config.topicArn))
                        return;
                }
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    throw fre;
            }
        } while (true);
    }
}
