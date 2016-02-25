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
    private final HashMap<Long, List<Future<RunnableProgram>>> runnablePrograms;
    private final InstanceFinder instanceFinder;
    private final ImageFinder imageFinder;
    public volatile MachineConfigData defaultMachineConfigData;

    public AwsMachineProvider(AwsResourcesManager manager)
    {
        super(manager);
        reservedMachines = new HashMap<Long, MachineReservedResource>();
        boundInstances = new HashMap<Long, AwsMachineInstance>();
        stalledRelease = new HashMap<Long, AwsMachineInstance>();
        runnablePrograms = new HashMap<Long, List<Future<RunnableProgram>>>();
        instanceFinder = new InstanceFinder();
        imageFinder = new ImageFinder();
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
         * 5. if reusable
         *      a. stop any RunnableProgram's
         *      b. nuke sandboxes
         *      c. clean up runnablePrograms map
         *      a. if the above pass, add them to stalled release collection, otherwise destroy them.
         * 6. A repeating check stalled release task will check for timed out stalls and destroy them. 
         *********************************************************************/

        List<AwsMachineInstance> instancesInTemplate = new ArrayList<AwsMachineInstance>();
        synchronized (boundInstances)
        {
            synchronized (reservedMachines)
            {
                List<Long> releaseList = new ArrayList<Long>();
                releasePossiblePendings(templateInstanceId, isReusable); // this will clean up the reserved list for given template 
                for (Entry<Long, AwsMachineInstance> entry : boundInstances.entrySet())
                {
                    ResourceCoordinates coordinates = entry.getValue().getCoordinates();
                    if (coordinates.templateInstanceId == templateInstanceId)
                        releaseList.add(entry.getKey());
                }
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
            return;
        if (isReusable)
        {
            for (AwsMachineInstance instance : instancesInTemplate)
            {
                if (!instance.reservedResource.reusable.get())
                {
                    isReusable = false;
                    break;
                }
                long t1 = System.currentTimeMillis();
                long delta = t1 - instance.instantiationTime;
                delta = TimeUnit.MINUTES.convert(delta, TimeUnit.MILLISECONDS);
                if (delta >= instance.mconfig.stallReleaseMinutes)
                {
                    isReusable = false;
                    break;
                }
            }
        }

        if (isReusable)
        {
            sanitizeInstance(templateInstanceId, instancesInTemplate);
            return;
        }
        synchronized (runnablePrograms)
        {
            runnablePrograms.remove(templateInstanceId);
        }
        deleteInstances(templateInstanceId, instancesInTemplate, null);
    }

    private void deleteInstances(long templateInstanceId, List<AwsMachineInstance> instancesInTemplate, ResourceCoordinates coordinates)
    {
        List<Future<Void>> futures = new ArrayList<Future<Void>>();
        for (AwsMachineInstance instance : instancesInTemplate)
        {
            if (coordinates != null)
            {
                if (!instance.reservedResource.resource.getCoordinates().equals(coordinates))
                    continue;
            }
            ProgressiveDelayData pdelayData = new ProgressiveDelayData(this, instance.getCoordinates());
            futures.add(config.blockingExecutor.submit(new ReleaseMachineFuture(this, instance.getCoordinates(), instance.ec2Instance, null, null, pdelayData)));
        }
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
    }

    private void sanitizeInstance(long templateInstanceId, List<AwsMachineInstance> instancesInTemplate)
    {
        List<AwsMachineInstance> deletedList = new ArrayList<AwsMachineInstance>();
        for(AwsMachineInstance instance : instancesInTemplate)
        {
            try
            {
                instance.delete(null).get();
            } catch (Exception e)
            {
                deletedList.add(instance);
                deleteInstances(templateInstanceId, instancesInTemplate, instance.getCoordinates());
            }
        }
        
        List<Future<RunnableProgram>> rplist = null;
        synchronized(runnablePrograms)
        {
            rplist = runnablePrograms.remove(templateInstanceId);
        }
        for(Future<RunnableProgram> rp : rplist)
        {
            try
            {
                StafRunnableProgram srp = (StafRunnableProgram)rp.get();
                AwsMachineInstance machineInstance = (AwsMachineInstance)srp.getCommandData().getContext();
                ResourceCoordinates coord = machineInstance.getCoordinates();
                if(srp.isRunning())
                {
                    Integer ccode = srp.kill().get();
                    if(ccode != 0)
                    {
                        deletedList.add(machineInstance);
                        deleteInstances(templateInstanceId, instancesInTemplate, coord);
                    }
                }
                synchronized(stalledRelease)
                {
                    stalledRelease.put(coord.resourceId, machineInstance);
                }
            } catch (Exception e)
            {
                // nothing further we can really do if this fails, instead of warning for manual cleanup, nuke the whole template's worth
                deleteInstances(templateInstanceId, instancesInTemplate, null);
                return;
            }
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
                                log.debug(rresource.toString(format).toString());
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
        config.scheduledExecutor.schedule(new StalledReleaseTask(), 1, TimeUnit.MINUTES);
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
        }
    }
}
