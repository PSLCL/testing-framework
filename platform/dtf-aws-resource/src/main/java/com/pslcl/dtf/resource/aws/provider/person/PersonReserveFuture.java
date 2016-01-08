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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ClientNames;
import com.pslcl.dtf.resource.aws.instance.person.PersonConfigData;

@SuppressWarnings("javadoc")
public class PersonReserveFuture implements Callable<List<ResourceReserveDisposition>>
{
    public static final String SnsMidStr = "sns";

    private final AwsPersonProvider provider;
    private final List<ResourceDescription> resources;
    private final int timeoutSeconds;

    public PersonReserveFuture(AwsPersonProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public List<ResourceReserveDisposition> call() throws Exception
    {
        List<ResourceReserveDisposition> list = new ArrayList<ResourceReserveDisposition>();

        /*
         * For now, we are doing a simple round robin of the site default list of inspectors 
         * unless the individual resource attributes has specified an inspector in which case
         * that individual will be used.  In either case, we are not going to do anything 
         * special on reserve, it will always succeed (assuming its of type person).
         */
        for (ResourceDescription resource : resources)
        {
            if(!ResourceProvider.PersonName.equals(resource.getName()))
            {
                list.add(new ResourceReserveDisposition(resource));
                continue;
            }
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(provider, resource.getCoordinates());
            PersonConfigData config = PersonConfigData.init(pdelayData, resource, provider.defaultPersonConfigData);
            String email = null;
            int roundRobinIndex = -1;
            if(config.givenInspector == null)
            {
                roundRobinIndex = provider.getNextInspectorIndex();
                email = config.inspectors.get(roundRobinIndex);
            }else
                email = config.sender;
            pdelayData.preFixMostName = provider.config.properties.getProperty(ClientNames.TestShortNameKey, ClientNames.TestShortNameDefault);
            PersonReservedResource rresource = new PersonReservedResource(provider, resource, timeoutSeconds, config, email);
            ScheduledFuture<?> future = provider.config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
            rresource.setTimerFuture(future);
            resource.getCoordinates().setManager(provider.manager);
            resource.getCoordinates().setProvider(provider);
            list.add(new ResourceReserveDisposition(resource, rresource));
            HashMap<Long, PersonReservedResource> reservedMap = provider.getReservedPeople();
            synchronized(reservedMap)
            {
                reservedMap.put(resource.getCoordinates().resourceId, rresource);
            }
        }
        return list;
    }

//    private void checkForTopic(ProgressiveDelayData pdelayData, PersonConfigData config) throws FatalResourceException
//    {
//        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
//        String msg = pdelayData.getHumanName(SnsMidStr, "listTopics");
//        do
//        {
//            try
//            {
//                ByteBuffer bb;
//                RawMessage rm = new RawMessage().withData(bb);
//                SendRawEmailRequest ser = new SendRawEmailRequest().withRawMessage(rawMessage)
//                provider.manager.sesClient.sensetEndpoint(config.endpoint);
//                provider.manager.snsClient.setEndpoint(config.endpoint);
//                
//                
//                //formatter:off
//                PublishRequest pr = new PublishRequest()
//                    .withMessage("the message")
//                    .withSubject("the subject")
//                    .withTopicArn(config.topicArn);
//                //formatter:on
//                provider.manager.snsClient.publish(pr);
//                
//                ListTopicsResult listTopicsResult = provider.manager.snsClient.listTopics();
//                String nextToken = listTopicsResult.getNextToken();
//                List<Topic> topics = listTopicsResult.getTopics();
//
//                // ListTopicResult contains only 100 topics hence use next token to get
//                // next 100 topics.
//                while (nextToken != null)
//                {
//                    listTopicsResult = provider.manager.snsClient.listTopics();
//                    nextToken = listTopicsResult.getNextToken();
//                    topics.addAll(listTopicsResult.getTopics());
//                }
//                for (Topic t : topics)
//                {
//                    if (t.getTopicArn().equals(config.topicArn))
//                        return;
//                }
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//    }
}
