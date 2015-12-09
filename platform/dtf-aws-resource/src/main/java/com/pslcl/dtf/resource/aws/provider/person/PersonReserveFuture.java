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
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceReserveResult;

@SuppressWarnings("javadoc")
public class PersonReserveFuture implements Callable<ResourceReserveResult>
{
    private final AwsPersonProvider provider;
    private final  List<ResourceDescription> resources;
    private final int timeoutSeconds;
    
    public PersonReserveFuture(AwsPersonProvider provider, List<ResourceDescription> resources, int timeoutSeconds)
    {
        this.provider = provider;
        this.resources = resources;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public ResourceReserveResult call() throws Exception
    {
        List<ReservedResource> reservedResources = new ArrayList<ReservedResource>();
        List<ResourceDescription> unavailableResources = new ArrayList<ResourceDescription>();
        List<ResourceDescription> invalidResources = new ArrayList<ResourceDescription>();
        ResourceReserveResult result = new ResourceReserveResult(reservedResources, unavailableResources, invalidResources);
        
        for (ResourceDescription resource : resources)
        {
            try
            {
//                SubnetConfigData subnetConfig = SubnetConfigData.init(resource, null, defaultSubnetConfigData);
//                ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, config.statusTracker, resource.getCoordinates());
//                PersonConfigData config = PersonConfigData.init(pdelayData, null, ) 
//                
//                if (manager.subnetManager.availableSgs.get() > 0)
//                {
//                    resource.getCoordinates().setManager(manager);
//                    resource.getCoordinates().setProvider(this);
//                    
//                    pdelayData.maxDelay = subnetConfig.sgMaxDelay;
//                    pdelayData.maxRetries = subnetConfig.sgMaxRetries;
//                    pdelayData.preFixMostName = subnetConfig.resoucePrefixName;
//                    GroupIdentifier groupId = manager.subnetManager.getSecurityGroup(pdelayData, subnetConfig.permissions, subnetConfig);
//                    PersonReservedResource rresource = new PersonReservedResource(pdelayData, resource, groupId);
//                    ScheduledFuture<?> future = config.scheduledExecutor.schedule(rresource, timeoutSeconds, TimeUnit.SECONDS);
//                    rresource.setTimerFuture(future);
//                    reservedResources.add(new ReservedResource(resource.getCoordinates(), resource.getAttributes(), timeoutSeconds));
//                    synchronized (reservedPeople)
//                    {
//                        reservedPeople.put(resource.getCoordinates().resourceId, rresource);
//                    }
//                } else
//                    unavailableResources.add(resource);
            } catch (Exception e)
            {
//                log.warn(getClass().getSimpleName() + ".reserve has invalid resources: " + resource.toString());
                invalidResources.add(resource);
                LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".reserveIfAvailable failed: " + resource.toString(), e);
            }
        }
        return result;
//        do
//        {
//            try
//            {
//                provider.manager.snsClient.listTopics();
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
//        return null;
//        return provider.internalReserveIfAvailable(resources, timeoutSeconds);
    }
    
    private void checkForTopic()
    {
//        do
//        {
//            try
//            {
//                provider.manager.snsClient.listTopics();
//                break;
//            } catch (Exception e)
//            {
//                FatalResourceException fre = pdelay.handleException(msg, e);
//                if (fre instanceof FatalException)
//                    throw fre;
//            }
//        } while (true);
    }
}
