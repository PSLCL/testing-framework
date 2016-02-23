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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import com.amazonaws.services.ec2.model.Instance;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.exception.FatalServerTimeoutException;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.ProcessCommandData;
import com.pslcl.dtf.core.runner.resource.staf.futures.ConfigureFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeleteFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.DeployFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.PingFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.RunFuture;
import com.pslcl.dtf.core.runner.resource.staf.futures.StafRunnableProgram;
import com.pslcl.dtf.resource.aws.ProgressiveDelay;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;
import com.pslcl.dtf.resource.aws.provider.AwsResourceProvider;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class AwsMachineInstance implements MachineInstance
{
    private final MachineReservedResource reservedResource;
    public final Instance ec2Instance;
    public final MachineConfigData mconfig;
    public final RunnerConfig rconfig;
    
    public AwsMachineInstance(MachineReservedResource reservedResource, MachineConfigData mconfig, RunnerConfig rconfig)
    {
        this.reservedResource = reservedResource;
        this.mconfig = mconfig;
        this.rconfig = rconfig;
        ec2Instance = reservedResource.ec2Instance;
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
    public Future<CableInstance> connect(NetworkInstance network)// throws IncompatibleResourceException
    {
        AwsNetworkInstance instance = (AwsNetworkInstance) network;
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(reservedResource.provider, reservedResource.resource.getCoordinates());
        return  instance.runnerConfig.blockingExecutor.submit(new ConnectFuture(this, (AwsNetworkInstance) network, pdelayData));
    }

    @Override
    public Future<RunnableProgram> run(String command)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if(platform != null && platform.length() > 0)
            windows = true;
        RunFuture df = new RunFuture(ec2Instance.getPublicIpAddress(), mconfig.linuxSandboxPath, mconfig.winSandboxPath, command, null, windows, this);
        return reservedResource.provider.config.blockingExecutor.submit(df);
    }

    @Override
    public Future<RunnableProgram> configure(String command)
    {
        reservedResource.reusable.set(false);
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if(platform != null && platform.length() > 0)
            windows = true;
        ConfigureFuture cf = new ConfigureFuture(ec2Instance.getPublicIpAddress(), mconfig.linuxSandboxPath, mconfig.winSandboxPath, command, windows, this);
        return reservedResource.provider.config.blockingExecutor.submit(cf);
    }

    @Override
    public Future<RunnableProgram> start(String command)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if(platform != null && platform.length() > 0)
            windows = true;
        RunFuture df = new RunFuture(ec2Instance.getPublicIpAddress(), mconfig.linuxSandboxPath, mconfig.winSandboxPath, command, rconfig.blockingExecutor, windows, this);
        return reservedResource.provider.config.blockingExecutor.submit(df);
    }

    @Override
    public Future<Void> deploy(String partialDestPath, String url) throws Exception
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if(platform != null && platform.length() > 0)
            windows = true;
        ProcessCommandData cmdData = new ProcessCommandData(null, null, null, false, false);
        StafRunnableProgram runnableProgram = new StafRunnableProgram(null, cmdData);
        cmdData.setHost(ec2Instance.getPublicIpAddress());
        cmdData.setWait(true);
        
        //@formatter:off
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(
                        (AwsResourceProvider)reservedResource.resource.getCoordinates().getProvider(), 
                        reservedResource.resource.getCoordinates());
        //@formatter:on
        pdelayData.maxDelay = mconfig.ec2MaxDelay;
        pdelayData.maxRetries = mconfig.ec2MaxRetries;
        pdelayData.preFixMostName = mconfig.resoucePrefixName;
        ProgressiveDelay pdelay = new ProgressiveDelay(pdelayData);
        String msg = pdelayData.getHumanName(MachineInstanceFuture.Ec2MidStr, "stafPing");
        do
        {
            if (reservedResource.bindFutureCanceled.get())
                throw new CancellationException();
            try
            {
                PingFuture pingFuture = new PingFuture(runnableProgram);
                Integer rc = reservedResource.provider.config.blockingExecutor.submit(pingFuture).get();
                if(rc != null && rc == 0)
                    break;
                pdelay.retry(msg);
            }catch(FatalServerTimeoutException fstoe)
            {
                throw fstoe;
            }
            catch (Exception e)
            {
                try
                {
                    pdelay.retry(msg);
                }catch(FatalServerTimeoutException fstoe)
                {
                    throw fstoe;
                }
            }
        } while (true);
        DeployFuture deployFuture = new DeployFuture(ec2Instance.getPublicIpAddress(), mconfig.linuxSandboxPath, mconfig.winSandboxPath, partialDestPath, url, windows);
        return reservedResource.provider.config.blockingExecutor.submit(deployFuture);
    }

    @Override
    public Future<Void> delete(String partialDestPath)
    {
        String platform = ec2Instance.getPlatform();
        boolean windows = false;
        if(platform != null && platform.length() > 0)
            windows = true;
        DeleteFuture df = new DeleteFuture(ec2Instance.getPublicIpAddress(), mconfig.linuxSandboxPath, mconfig.winSandboxPath, partialDestPath, windows);
        return reservedResource.provider.config.blockingExecutor.submit(df);
    }
    
    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        AwsNetworkInstance instance = (AwsNetworkInstance) network;
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(reservedResource.provider, reservedResource.resource.getCoordinates());
        return  instance.runnerConfig.blockingExecutor.submit(new DisconnectFuture(this, (AwsNetworkInstance) network, pdelayData));
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
