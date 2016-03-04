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
package com.pslcl.dtf.resource.aws.instance.machine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.amazonaws.services.ec2.model.Instance;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.futures.ConfigureFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeleteFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeployFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.RunFuture;
import com.pslcl.dtf.core.util.TabToLevel;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class AwsMachineInstance implements MachineInstance
{
    public volatile MachineReservedResource reservedResource;
    public final Instance ec2Instance;
    public final MachineConfigData mconfig;
    public final RunnerConfig rconfig;
    public final AtomicBoolean sanitizing;
    public final AtomicBoolean destroyed;
    public final AtomicBoolean taken;
    public final long instantiationTime;
    

    public AwsMachineInstance(MachineReservedResource reservedResource, MachineConfigData mconfig, RunnerConfig rconfig)
    {
        this.reservedResource = reservedResource;
        this.mconfig = mconfig;
        this.rconfig = rconfig;
        sanitizing = new AtomicBoolean(false);
        destroyed = new AtomicBoolean(false);
        taken = new AtomicBoolean(false);
        ec2Instance = reservedResource.ec2Instance;
        instantiationTime = System.currentTimeMillis();
    }

    @Override
    public String getName()
    {
        return reservedResource.resource.getName();
    }

    @Override
    public Map<String, String> getAttributes()
    {
        Map<String, String> map = reservedResource.resource.getAttributes();
        synchronized (map)
        {
            return new HashMap<String, String>(map);
        }
    }

    @Override
    public void addAttribute(String key, String value)
    {
        Map<String, String> map = reservedResource.resource.getAttributes();
        synchronized (map)
        {
            map.put(key, value);
        }
    }

    @Override
    public ResourceProvider getResourceProvider()
    {
        return reservedResource.resource.getCoordinates().getProvider();
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return reservedResource.resource.getCoordinates();
    }

    @Override
    public Future<Void> deploy(String partialDestPath, String url) throws Exception
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if (platform != null && platform.length() > 0)
            windows = true;
        //@formatter:off
        DeployFuture deployFuture = new DeployFuture(
                        ec2Instance.getPublicIpAddress(), 
                        mconfig.linuxSandboxPath, mconfig.winSandboxPath, 
                        partialDestPath, url, windows, reservedResource.resource.getCoordinates().getRunId());
        //@formatter:on
        return reservedResource.provider.config.blockingExecutor.submit(deployFuture);
    }
    
    @Override
    public Future<CableInstance> connect(NetworkInstance network)// throws IncompatibleResourceException
    {
        AwsNetworkInstance instance = (AwsNetworkInstance) network;
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(reservedResource.provider, reservedResource.resource.getCoordinates());
        return instance.runnerConfig.blockingExecutor.submit(new ConnectFuture(this, (AwsNetworkInstance) network, pdelayData));
    }

    @Override
    public Future<RunnableProgram> run(String command)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if (platform != null && platform.length() > 0)
            windows = true;
        //@formatter:off
        RunFuture df = new RunFuture(
                        ec2Instance.getPublicIpAddress(), 
                        mconfig.linuxSandboxPath, mconfig.winSandboxPath, 
                        command, null, windows, 
                        reservedResource.resource.getCoordinates().getRunId(), this);
        //@formatter:on
        Future<RunnableProgram> rpf = reservedResource.provider.config.blockingExecutor.submit(df);
        reservedResource.provider.addRunnableProgram(reservedResource.resource.getCoordinates().resourceId, rpf);
        return rpf;
    }

    @Override
    public Future<RunnableProgram> configure(String command)
    {
        reservedResource.reusable.set(false);
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if (platform != null && platform.length() > 0)
            windows = true;
        //@formatter:off
        ConfigureFuture cf = new ConfigureFuture(
                        ec2Instance.getPublicIpAddress(), 
                        mconfig.linuxSandboxPath, mconfig.winSandboxPath, 
                        command, windows, 
                        reservedResource.resource.getCoordinates().getRunId(), this);
        //@formatter:on
        Future<RunnableProgram> rpf = reservedResource.provider.config.blockingExecutor.submit(cf);
        reservedResource.provider.addRunnableProgram(reservedResource.resource.getCoordinates().resourceId, rpf);
        return rpf;
    }

    @Override
    public Future<RunnableProgram> start(String command)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if (platform != null && platform.length() > 0)
            windows = true;
        //@formatter:off
        RunFuture df = new RunFuture(
                        ec2Instance.getPublicIpAddress(), 
                        mconfig.linuxSandboxPath, mconfig.winSandboxPath, 
                        command, rconfig.blockingExecutor, windows, 
                        reservedResource.resource.getCoordinates().getRunId(), this);
        Future<RunnableProgram> rpf = reservedResource.provider.config.blockingExecutor.submit(df);
        reservedResource.provider.addRunnableProgram(reservedResource.resource.getCoordinates().resourceId, rpf);
        return rpf;
    }

    @Override
    public Future<Void> delete(String partialDestPath)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if (platform != null && platform.length() > 0)
            windows = true;
        //@formatter:off
        DeleteFuture df = new DeleteFuture(
                        ec2Instance.getPublicIpAddress(), 
                        mconfig.linuxSandboxPath, mconfig.winSandboxPath, 
                        partialDestPath, windows, reservedResource.resource.getCoordinates().getRunId());
        //@formatter:on
        return reservedResource.provider.config.blockingExecutor.submit(df);
    }

    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        AwsNetworkInstance instance = (AwsNetworkInstance) network;
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(reservedResource.provider, reservedResource.resource.getCoordinates());
        return instance.runnerConfig.blockingExecutor.submit(new DisconnectFuture(this, (AwsNetworkInstance) network, pdelayData));
    }
    
    public String toString()
    {
        TabToLevel format = new TabToLevel();
        return toString(format).toString();
    }
    
    public TabToLevel toString(TabToLevel format)
    {
        format.ttl(getClass().getSimpleName());
        format.level.incrementAndGet();
        format.ttl("sanitizing: ", sanitizing);
        format.ttl("destroyed: ", destroyed);
        format.ttl("taken: ", taken);
        SimpleDateFormat sdf = new SimpleDateFormat();
        String time = sdf.format(new Date(instantiationTime));
        format.ttl("instantiationTime:", time);
        format.ttl("reservedResource:");
        format.level.incrementAndGet();
        reservedResource.toString(format);
        format.level.decrementAndGet();
        format.level.decrementAndGet();
        return format;
    }
    
    public enum AwsInstanceState
    {
        //@formatter:off
        Pending("pending"), 
        Running("running"), 
        ShuttingDown("shutting-down"), 
        Terminated("terminated"), 
        Stopping("stopping"), 
        Stopped("stopped");
        //@formatter:on

        public static AwsInstanceState getState(String awsState) throws Exception
        {
            if (awsState.equals("pending"))
                return Pending;
            if (awsState.equals("running"))
                return Running;
            if (awsState.equals("shutting-down"))
                return ShuttingDown;
            if (awsState.equals("terminated"))
                return Terminated;
            if (awsState.equals("stopping"))
                return Stopping;
            if (awsState.equals("stopped"))
                return Stopped;
            throw new Exception("Unknown AWS Instance State: " + awsState);
        }

        private AwsInstanceState(String awsState)
        {
            this.awsState = awsState;
        }

        public final String awsState;
    }
}
