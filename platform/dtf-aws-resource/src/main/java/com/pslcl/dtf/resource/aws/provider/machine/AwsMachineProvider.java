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
package com.pslcl.dtf.resource.aws.provider.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.InstanceType;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.config.status.StatusTracker;
import com.pslcl.dtf.core.runner.resource.ReservedResource;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.ResourceReserveDisposition;
import com.pslcl.dtf.core.runner.resource.exception.FatalException;
import com.pslcl.dtf.core.runner.resource.exception.FatalResourceException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotFoundException;
import com.pslcl.dtf.core.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.MachineProvider;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.attr.ProviderNames;
import com.pslcl.dtf.resource.aws.instance.machine.AwsMachineInstance;
import com.pslcl.dtf.resource.aws.instance.machine.MachineConfigData;
import com.pslcl.dtf.resource.aws.instance.machine.MachineInstanceFuture;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;

/**
 * Reserve, bind, control and release instances of AWS machines.
 */

@SuppressWarnings("javadoc")
public class AwsMachineProvider extends AwsResourceProvider implements MachineProvider
{
    private final HashMap<Long, AwsMachineInstance> boundInstances; // key is resourceId
    private final HashMap<Long, MachineReservedResource> reservedMachines; // key is resourceId
    private final HashMap<Long, AwsMachineInstance> stalledRelease; // key is templateInstanceId
    private final List<Future<Void>> deleteInstanceFutures;
    private final HashMap<Long, List<Future<RunnableProgram>>> runnablePrograms; //TODO: double check this is not leaking memory, all calling deleteInstance
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;
    public volatile MachineConfigData defaultMachineConfigData;

    public AwsMachineProvider(AwsResourcesManager manager)
    {
        super(manager);
        reservedMachines = new HashMap<Long, MachineReservedResource>();
        boundInstances = new HashMap<Long, AwsMachineInstance>();
        stalledRelease = new HashMap<Long, AwsMachineInstance>();
        deleteInstanceFutures = new ArrayList<Future<Void>>();
        runnablePrograms = new HashMap<Long, List<Future<RunnableProgram>>>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder(this);
        totalReuseAttemps = new AtomicInteger(0);
        reuseHits = new AtomicInteger(0);
    }

    public void addRunnableProgram(long templateInstanceId, Future<RunnableProgram> runnableProgramFuture)
    {
        synchronized (runnablePrograms)
        {
            List<Future<RunnableProgram>> list = runnablePrograms.get(templateInstanceId);
            if (list == null)
            {
                list = new ArrayList<Future<RunnableProgram>>();
                runnablePrograms.put(templateInstanceId, list);
            }
            list.add(runnableProgramFuture);
        }
    }

    InstanceFinder getInstanceFinder()
    {
        return instanceFinder;
    }

    HashMap<Long, AwsMachineInstance> getBoundInstances()
    {
        return boundInstances;
    }

    public HashMap<Long, MachineReservedResource> getReservedMachines()
    {
        return reservedMachines;
    }

    public void addBoundInstance(long resourceId, AwsMachineInstance instance)
    {
        synchronized (boundInstances)
        {
            boundInstances.put(resourceId, instance);
        }
    }

    public void addReservedMachine(long resourceId, MachineReservedResource reservedResource)
    {
        synchronized (reservedMachines)
        {
            reservedMachines.put(resourceId, reservedResource);
        }
    }

    public void setRunId(long templateInstanceId, long runId)
    {
        synchronized (reservedMachines)
        {
            for (Entry<Long, MachineReservedResource> entry : reservedMachines.entrySet())
            {
                ResourceCoordinates coord = entry.getValue().resource.getCoordinates();
                if (coord.templateInstanceId == templateInstanceId)
                    coord.setRunId(runId);
            }
        }
    }

    public void forceCleanup()
    {
    }

    private final AtomicInteger totalReuseAttemps;
    private final AtomicInteger reuseHits;
    
    public AwsMachineInstance checkForReuse(MachineReservedResource reservedResource)
    {
        totalReuseAttemps.incrementAndGet();
        TabToLevel format = new TabToLevel();
        format.ttl("\ncheckForReuse:");
        format.level.incrementAndGet();
        reservedResource.toString(format, true);
        format.level.incrementAndGet();
        HashMap<Long, AwsMachineInstance> stalledMap;
        synchronized (stalledRelease)
        {
            stalledMap = new HashMap<Long, AwsMachineInstance>(stalledRelease);
        }

        for (Entry<Long, AwsMachineInstance> entry : stalledMap.entrySet())
        {
            AwsMachineInstance stalledInstance = entry.getValue();
            InstanceType instanceType = stalledInstance.reservedResource.instanceType;
            if (instanceType != reservedResource.instanceType)
                continue;
            if (!stalledInstance.reservedResource.imageId.equals(reservedResource.imageId))
                continue;
            
            synchronized (stalledRelease)
            {
                if (stalledInstance.taken.get())
                    continue;
                stalledInstance.taken.set(true);
            }
            format.ttl("found a matching instanceType/imageId may have to wait on sanitization complete");
            log.debug(format.toString());
            do
            {
                if (!stalledInstance.sanitizing.get())
                    break;
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException e)
                {
                }
            } while (true);
            format.ttl("sanitization complete, returning object to use");
            if (stalledInstance.destroyed.get())
            {
                format.ttl("instance cleanup destroyed the intended, returning null");
                addStats(format);
                log.debug(format.toString());
                return null;
            }
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, reservedResource.resource.getCoordinates());
            try
            {
                stalledInstance.mconfig = MachineConfigData.init(pdelayData, reservedResource.resource, format, defaultMachineConfigData);
            } catch (Exception e)
            {
                log.warn("failed to inject the new mconfig", e);
                format.ttl("failed to inject the new mconfig");
                log.debug(format.toString());
                synchronized (stalledRelease)
                {
                    stalledInstance.taken.set(false);
                }
                return null;
            }
            pdelayData.preFixMostName = stalledInstance.mconfig.resourcePrefixName; 
            try
            {
                manager.createNameTag(pdelayData, pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, null), stalledInstance.ec2Instance.getInstanceId());
            } catch (FatalResourceException e)
            {
                log.warn("createNameTag failed", e);
                format.ttl("createIdleNameTag failed, continuing anyway");
            }
            
            stalledInstance.reservedResource = reservedResource;
            reuseHits.incrementAndGet();
            format.ttl("instance match found");
            addStats(format);
            log.debug(format.toString());
            synchronized (stalledRelease)
            {
                stalledRelease.remove(entry.getKey());
            }
            return stalledInstance;
        }
        format.ttl("no stalled instances to check right now");
        addStats(format);
        log.debug(format.toString());
        return null;
    }

    private void addStats(TabToLevel format)
    {
        format.ttl("stats:");
        format.level.incrementAndGet();
        double total = totalReuseAttemps.get();
        double hits = reuseHits.get();
        double misses = total - hits;
        double hitRatio = hits / total;
        format.ttl("totalReuseAttemps: ", total);
        format.ttl("reuseHits: ", hits);
        format.ttl("reuseMisses: ", misses);
        format.ttl("hitRatio: ", hitRatio);
        format.level.decrementAndGet();
    }
    
    public void release(long templateInstanceId, boolean isReusable)
    {
        /**********************************************************************
         * 1. remove resource from reservedMachines.
         * 2. remove resource from boundInstances.
         * 3. determine if resources for this template are reusable
         *      a. if the isReusable flag is true;
         *      b. if configure was called on it, no
         *      c. if they are past the targeted stall time, no otherwise yes 
         * 4. if not clean them up now
         *      a. don't worry about stopping any RunnableProgram's
         *      b. don't worry about nuking sandboxes
         *      c. clean up runnablePrograms map
         *      d. decrement reserved count
         * 5. if reusable
         *      a. stop any RunnableProgram's
         *      b. nuke sandboxes
         *      c. clean up runnablePrograms map
         *      a. if the above pass, add them to stalled release collection, otherwise destroy them.
         * 6. A repeating check stalled release task will check for timed out stalls and destroy them. 
         *********************************************************************/

        if(log.isDebugEnabled())
            log.debug(getClass().getSimpleName() + ".release templateInstanceId: " + templateInstanceId + " isReusable: "  + isReusable);
        TabToLevel format = new TabToLevel();
        format.ttl("\n", getClass().getName() + ".release");
        format.level.incrementAndGet();
        format.ttl("templateInstanceId: " + templateInstanceId);
        format.ttl("isReusable: " + isReusable);
        List<AwsMachineInstance> instancesInTemplate = new ArrayList<AwsMachineInstance>();
        synchronized (boundInstances)
        {
            synchronized (reservedMachines)
            {
                List<Long> releaseList = new ArrayList<Long>();
                releasePossiblePendings(templateInstanceId, isReusable); // this will clean up the reserved list for given template 
                format.ttl("machine instances being released:");
                format.level.incrementAndGet();
                for (Entry<Long, AwsMachineInstance> entry : boundInstances.entrySet())
                {
                    ResourceCoordinates coordinates = entry.getValue().getCoordinates();
                    if (coordinates.templateInstanceId == templateInstanceId)
                    {
                        releaseList.add(entry.getKey());
                        format.ttl(entry.getValue().toString(format, true));
                    }
                }
                format.level.decrementAndGet();
                for (Long key : releaseList)
                {
                    AwsMachineInstance instance = boundInstances.remove(key);
                    reservedMachines.remove(key);
                    if (instance == null)
                        continue;
                    instancesInTemplate.add(instance);
                }
            }
        }
        if (instancesInTemplate.size() == 0)
        {
            if(log.isDebugEnabled())
            {
                format.ttl("no machine instances in the given template");
                log.debug(format.toString());
            }
            return;
        }

        if (!isReusable)
        {
            format.ttl("called with isReusable false deleting any/all");
            log.debug(format.toString());
            synchronized (runnablePrograms)
            {
                runnablePrograms.remove(templateInstanceId);
            }
            deleteInstances(templateInstanceId, instancesInTemplate, null);
            return;
        }

        List<AwsMachineInstance> adjustList = new ArrayList<AwsMachineInstance>();
        format.ttl("Still viable tests: ");
        format.level.incrementAndGet();
        for (AwsMachineInstance instance : instancesInTemplate)
        {
            if (!instance.reservedResource.reusable.get())
            {
                adjustList.add(instance);
                format.ttl("invalid for reuse config was called on:");
                format.level.incrementAndGet();
                instance.toString(format, true);
                format.level.decrementAndGet();
                continue;
            }
        }
        format.level.decrementAndGet();
        for (AwsMachineInstance adjust : adjustList)
        {
            instancesInTemplate.remove(adjust);
            deleteInstances(templateInstanceId, adjustList, adjust.getCoordinates());
        }
        format.ttl("reusable instances: ", instancesInTemplate.size());
        log.debug(format.toString());
        if (instancesInTemplate.size() > 0)
            sanitizeInstance(templateInstanceId, instancesInTemplate);
        return;
    }

    private void deleteInstances(long templateInstanceId, List<AwsMachineInstance> instancesInTemplate, ResourceCoordinates coordinates)
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\n", getClass().getSimpleName(), ".deleteInstances");
        format.level.incrementAndGet();
        for (AwsMachineInstance instance : instancesInTemplate)
        {
            if (coordinates != null)
            {
                if (!instance.reservedResource.resource.getCoordinates().equals(coordinates))
                    continue;
            }
            instance.destroyed.set(true);
            instance.sanitizing.set(false); // the checkReusable may be waiting on this, if so, drop it from its wait and let it error out on first use.
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, instance.getCoordinates());
            ((AwsMachineProvider) instance.getResourceProvider()).instanceFinder.releaseInstance(instance.reservedResource.instanceType);
            synchronized (deleteInstanceFutures)
            {
                if(log.isDebugEnabled())
                    log.debug(getClass().getSimpleName() + ".deleteInstances queuing ReleaseMachineFuture: " + instance.getCoordinates().toString());
                deleteInstanceFutures.add(config.blockingExecutor.submit(new ReleaseMachineFuture(this, instance.getCoordinates(), instance.ec2Instance, null, null, pdelayData)));
            }
            instance.toString(format, true);
            format.ttl(" ");
        }
    }

    private void sanitizeInstance(long templateInstanceId, List<AwsMachineInstance> instancesInTemplate)
    {
        TabToLevel format = new TabToLevel();
        format.ttl("\nsanitizeInstance marking released instances available");
        format.level.incrementAndGet();
        List<AwsMachineInstance> deletedList = new ArrayList<AwsMachineInstance>();
        synchronized (stalledRelease)
        {
            for (AwsMachineInstance instance : instancesInTemplate)
            {
                // optimistic cleanup, give checkForReuse a chance to grab hold of these before totally clean.
                // destroy them out from under checkForReuse on failure to clear
                // they will be waiting on the following flag.
                instance.destroyed.set(false);
                instance.taken.set(false);
                instance.sanitizing.set(true);
                stalledRelease.put(instance.getCoordinates().resourceId, instance);
                instance.toString(format, true);
            }
        }
        log.debug(format.toString());
        format.clear();
        format.ttl("\nsanitizeInstance deleting sandbox");
        for (AwsMachineInstance instance : instancesInTemplate)
        {
            try
            {
                instance.toString(format, true);
                instance.delete(null).get();
            } catch (Exception e)
            {
                format.ttl("delete sandbox failed, nuking this instance");
                instance.destroyed.set(true);
                instance.taken.set(true);
                instance.sanitizing.set(false);
                deletedList.add(instance);
                deleteInstances(templateInstanceId, instancesInTemplate, instance.getCoordinates());
            }
        }
        log.debug(format.toString());
        format.clear();
        format.ttl("\nsanatizeInstance checking runnablePrograms from templateInstanceId: ", templateInstanceId);
        format.level.incrementAndGet();
        List<Future<RunnableProgram>> rplist = null;
        synchronized (runnablePrograms)
        {
            rplist = runnablePrograms.remove(templateInstanceId); // TODO: is this broken?, need to remove one at a time and nuke the whole when 0
        }
        if (rplist != null)
        {
            for (Future<RunnableProgram> rp : rplist)
            {
                try
                {
                    StafRunnableProgram srp = (StafRunnableProgram) rp.get();
                    AwsMachineInstance machineInstance = (AwsMachineInstance) srp.getCommandData().getContext();
                    ResourceCoordinates coord = machineInstance.getCoordinates();
                    if (srp.isRunning())
                    {
                        srp.toString(format);
                        format.ttl(" ");
                        Integer ccode = srp.kill().get();
                        if (ccode != 0)
                        {
                            format.ttl("cleanup of running application failed, nuking instance");
                            machineInstance.destroyed.set(true);
                            machineInstance.taken.set(true);
                            machineInstance.sanitizing.set(false);
                            deletedList.add(machineInstance);
                            deleteInstances(templateInstanceId, instancesInTemplate, coord);
                        }
                    }
                    log.debug(format.toString());
                    machineInstance.sanitizing.set(false);
                } catch (Exception e)
                {
                    format.ttl("cleanup of running application threw exception, manual cleanup may be required");
                    // nothing further we can really do if this fails, instead of warning for manual cleanup, nuke the whole template's worth
                    for (AwsMachineInstance instance : instancesInTemplate)
                    {
                        instance.destroyed.set(true);
                        instance.taken.set(true);
                        instance.sanitizing.set(false);
                    }

                    deleteInstances(templateInstanceId, instancesInTemplate, null);
                    log.debug(format.toString());
                    return;
                }
            }
            log.debug(format.toString());
        }
        for (AwsMachineInstance instance : instancesInTemplate)
        {
            for (AwsMachineInstance dinstance : deletedList)
            {
                if (instance == dinstance)
                    continue;
            }
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, instance.getCoordinates());
            MachineConfigData mconfig = null;
            try
            {
                mconfig = MachineConfigData.init(pdelayData, instance.reservedResource.resource, format, defaultMachineConfigData);
            } catch (Exception e)
            {
                log.warn("MachineConfigData.init failed", e);
                format.ttl("MachineConfigData.init failed, continuing anyway");
            }
            
            instance.disconnect(null);
            
            pdelayData.preFixMostName = mconfig.resourcePrefixName; 
            try
            {
                manager.createIdleNameTag(pdelayData, pdelayData.getIdleName(MachineInstanceFuture.Ec2MidStr), instance.ec2Instance.getInstanceId());
            } catch (FatalResourceException e)
            {
                log.warn("createIdleNameTag failed", e);
                format.ttl("createIdleNameTag failed, continuing anyway");
            }
//                stalledRelease.put(instance.getCoordinates().resourceId, instance);
            instance.sanitizing.set(false);
        }
    }

    // TODO: moved to only creating a single keypair per prefixName verses one per templateInstanceId ... but keep this
    // around for awhile incase this changes back
    @SuppressWarnings("unused")
    private void deleteKeyPair(List<Future<Void>> futures, String prefixTestName, ResourceCoordinates coordinates)
    {
        for (Future<Void> future : futures)
        {
            try
            {
                future.get();
            } catch (Exception e)
            {
                // nothing further we can really do if these fail, futures should have logged error details
                // could email someone to double check manual clean may be needed.
                log.warn(getClass().getSimpleName() + ".release a release future failed, manual cleanup maybe required");
            }
        }
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(AwsMachineProvider.this, coordinates);
        pdelayData.preFixMostName = prefixTestName;

        String name = pdelayData.getFullTemplateIdName(MachineInstanceFuture.KeyPairMidStr, null);
        DeleteKeyPairRequest request = new DeleteKeyPairRequest().withKeyName(name);
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "deleteKeyPair:" + name);
        do
        {
            try
            {
                pdelayData.provider.manager.awsThrottle();
                manager.ec2Client.deleteKeyPair(request);
                break;
            } catch (Exception e)
            {
                FatalResourceException fre = pdelay.handleException(msg, e);
                if (fre instanceof FatalException)
                    break;
            }
        } while (true);
    }

    private void releasePossiblePendings(long templateInstanceId, boolean isReusable)
    {
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        ResourceCoordinates coordinates = null;
        List<Long> releaseList = new ArrayList<Long>();
        // note this is being called already synchronized to reservedMachines
        for (Entry<Long, MachineReservedResource> entry : reservedMachines.entrySet())
        {
            MachineReservedResource rresource = entry.getValue();
            synchronized (rresource)
            {
                coordinates = rresource.resource.getCoordinates();
                if (coordinates.templateInstanceId == templateInstanceId)
                {
                    Future<MachineInstance> future = rresource.getInstanceFuture();
                    if (future != null)
                    {
                        boolean cancelResult = future.cancel(false);
                        /*
                        This attempt will fail if the task has already completed, has already been cancelled,
                        or could not be cancelled for some other reason.
                        To guarantee that we don't kill the future.call thread right in the middle of the ec2Client.runInstances()
                        we use the mayInterruptIfRunning false call to allow the future.call thread to continue.  
                        There is synchronized(reservedMachines) around the critical ec2Client.runInstances call to block it if we
                        are here doing cleanup.  We set a canceled flag so when it resumes it will throw an exception on the future.
                        Within that synchronized block, the call method will have set the ec2Instance prior to our getting here and 
                        we can then execute the terminate command for it. 
                        */
                        rresource.setBindFutureCanceled(true);
                        if (cancelResult)
                        {
                            try
                            {
                                future.get();
                            } catch (Exception e)
                            {
                            } // pass or fail, its going down
                            finally
                            {
                                TabToLevel format = new TabToLevel();
                                format.ttl("\n", getClass().getSimpleName(), ".release cancel pending future handling");
                                log.debug(rresource.toString(format, true).toString());
                                releaseList.add(entry.getKey());
                                if (rresource.ec2Instance != null)
                                {
                                    // the future made it far enough to trigger aws to startup the instance.
                                    ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, coordinates);
                                    futures.add(config.blockingExecutor.submit(new ReleaseMachineFuture(this, coordinates, rresource.ec2Instance, null, /*reservedResource.vpc.getVpcId() reservedResource.subnet.getSubnetId()*/ null, pdelayData)));
                                }
                            }
                        } else
                        {
                            try
                            {
                                future.get();
                            } catch (Exception e)
                            {
                                log.info("release machine code caught a somewhat unexpected exception during cancel cleanup", e);
                            }
                        }
                    } // if there is an instance future
                } // it template id matches
            } // synch on the reserved resource
        }
        for (Long key : releaseList)
            reservedMachines.remove(key);
    }

    public void releaseReservedResource(long templateInstanceId)
    {
        release(templateInstanceId, false);
    }

    @Override
    public void init(RunnerConfig config) throws Exception
    {
        super.init(config);
        instanceFinder.init(config);
        imageFinder.init(config);
        config.initsb.indentedOk();
        defaultMachineConfigData = MachineConfigData.init(config);
        config.initsb.level.decrementAndGet();
        config.scheduledExecutor.scheduleAtFixedRate(new StalledReleaseTask(), 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    @Override
    public Future<MachineInstance> bind(ReservedResource resource) throws ResourceNotReservedException
    {
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, resource.getCoordinates());
        config.statusTracker.fireResourceStatusChanged(pdelayData.resourceStatusEvent.getNewInstance(pdelayData.resourceStatusEvent, StatusTracker.Status.Warn));

        synchronized (reservedMachines)
        {
            MachineReservedResource reservedResource = reservedMachines.get(resource.getCoordinates().resourceId);
            if (reservedResource == null)
                throw new ResourceNotReservedException(resource.getName() + "(" + resource.getCoordinates().resourceId + ") is not reserved");
            reservedResource.getTimerFuture().cancel(true);
            Future<MachineInstance> future = config.blockingExecutor.submit(new MachineInstanceFuture(reservedResource, pdelayData));
            reservedResource.setInstanceFuture(future);
            //            reservedResource.timerFuture.cancel(true);
            return future;
        }
    }

    @Override
    public List<Future<MachineInstance>> bind(List<ReservedResource> resources) throws ResourceNotReservedException
    {
        List<Future<MachineInstance>> list = new ArrayList<Future<MachineInstance>>();
        for (ReservedResource resource : resources)
        {
            list.add(bind(resource));
        }
        return list;
    }

    public boolean isAvailable(ResourceDescription resource) throws ResourceNotFoundException
    {
        return internalIsAvailable(resource, new MachineQueryResult());
    }

    boolean internalIsAvailable(ResourceDescription resource, MachineQueryResult result) throws ResourceNotFoundException
    {
        if (!ResourceProvider.MachineName.equals(resource.getName()))
            return false;
        InstanceType instanceType = instanceFinder.findInstance(resource);
        if (!instanceFinder.checkLimits(instanceType))
            return false;
        instanceFinder.reserveInstance(instanceType);
        result.setInstanceType(instanceType);
        result.setImageId(imageFinder.findImage(manager.ec2Client, resource));
        return true;
    }

    @Override
    public Future<List<ResourceReserveDisposition>> reserve(List<ResourceDescription> resources, int timeoutSeconds)
    {
        return config.blockingExecutor.submit(new MachineReserveFuture(this, resources, timeoutSeconds));
    }

    @Override
    public String getName()
    {
        return ResourceProvider.getTypeName(this);
    }

    @Override
    public List<String> getAttributes()
    {
        List<String> attrs = ResourceNames.getProviderKeys();
        attrs.addAll(ProviderNames.getMachineKeys());
        return attrs;
    }

    
    private class StalledReleaseTask implements Runnable
    {
        private StalledReleaseTask()
        {
        }

        @Override
        public void run()
        {
            TabToLevel format = new TabToLevel();
            format.ttl("\n", getClass().getSimpleName(), "StaledRelease timout check");
            format.level.incrementAndGet();
            long t1 = System.currentTimeMillis();
            HashMap<Long, AwsMachineInstance> stalledMap = null;
            synchronized (stalledRelease)
            {
                stalledMap = new HashMap<Long, AwsMachineInstance>(stalledRelease);
            }
            format.ttl("stalledMap entries:");
            format.level.incrementAndGet();
            for (Entry<Long, AwsMachineInstance> entry : stalledMap.entrySet())
            {
                AwsMachineInstance machineInstance = entry.getValue();
                int configuredTimeout = machineInstance.mconfig.stallReleaseMinutes; 
                long delta = t1 - machineInstance.instantiationTime;
                delta = TimeUnit.MINUTES.convert(delta, TimeUnit.MILLISECONDS);
                format.ttl("configuredTimeout: ", configuredTimeout);
                format.ttl("delta: ", delta);
                long timeout = configuredTimeout;
                long div = delta / configuredTimeout;
//                if(div < 2) // 1 is the first period, 2 is modulus only
//                    div = 0;
//                else
//                    --div;  // modulus to this next period 
                timeout += div * configuredTimeout;
//                timeout += configuredTimeout - (delta % configuredTimeout);
                timeout -= 2; // keep/give it two minutes within the period to cleanup
                format.ttl("timeout: ", timeout);
                if(delta >= timeout)
                {
                    format.ttl("instance has timed out, deleting");
                    machineInstance.toString(format, true);
                    List<AwsMachineInstance> instancesInTemplate = new ArrayList<AwsMachineInstance>();
                    synchronized (stalledRelease)
                    {
                        if (!machineInstance.taken.get())
                        {
                            format.ttl("checkReusable race we won, break any checkreuseable out of its wait loop");
                            machineInstance.toString(format, true);
                            machineInstance.sanitizing.set(false);
                            machineInstance.destroyed.set(true);
                            machineInstance.taken.set(true);
                            instancesInTemplate.add(machineInstance);
                        }
                    }
                    if(instancesInTemplate.size() > 0)
                    {
                        synchronized (stalledRelease)
                        {
                            stalledRelease.remove(entry.getKey());
                        }
                        ResourceCoordinates coord = machineInstance.reservedResource.resource.getCoordinates();
                        deleteInstances(coord.templateInstanceId, instancesInTemplate, null);
                    }
                }else
                {
                    format.ttl("still in limit");
                    machineInstance.toString(format, true);
                }
            }
            log.debug(format.toString());
            synchronized (deleteInstanceFutures)
            {
                List<Future<Void>> completeList = new ArrayList<Future<Void>>();
                for (Future<Void> future : deleteInstanceFutures)
                {
                    try
                    {
                        if (future.isDone())
                        {
                            future.get();
                            completeList.add(future);
                        }
                    } catch (Exception e)
                    {
                        // nothing further we can really do if these fail, futures should have logged error details
                        // could email someone to double check manual clean may be needed.
                        log.warn(getClass().getSimpleName() + ".release a release future failed, manual cleanup maybe required");
                    }
                }
                for (Future<Void> future : completeList)
                    deleteInstanceFutures.remove(future);
            }
        }
    }
}
