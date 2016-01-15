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
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Instance;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.core.runner.resource.staf.DeployFuture;
import com.pslcl.dtf.resource.aws.ProgressiveDelay.ProgressiveDelayData;
import com.pslcl.dtf.resource.aws.instance.network.AwsNetworkInstance;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class AwsMachineInstance implements MachineInstance
{
    private final MachineReservedResource reservedResource;
    public final ResourceDescription resource;
    public final Instance ec2Instance;
    public final MachineConfigData config;
    
    public AwsMachineInstance(MachineReservedResource reservedResource, MachineConfigData config)
    {
        this.reservedResource = reservedResource;
        this.resource = reservedResource.resource; 
        this.config = config;
        ec2Instance = reservedResource.ec2Instance;
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
    public Future<CableInstance> connect(NetworkInstance network)// throws IncompatibleResourceException
    {
        AwsNetworkInstance instance = (AwsNetworkInstance) network;
        ProgressiveDelayData pdelayData = new ProgressiveDelayData(reservedResource.provider, resource.getCoordinates());
        return  instance.runnerConfig.blockingExecutor.submit(new ConnectFuture(this, (AwsNetworkInstance) network, pdelayData));
    }

    @Override
    public Future<RunnableProgram> run(String command)
    {
        return null;
    }

    @Override
    public Future<Integer> configure(String command)
    {
        return null;
    }

    @Override
    public Future<RunnableProgram> start(String command)
    {
        return null;
    }

    @Override
    public Future<Void> deploy(String partialDestPath, String url)
    {
        try
        {
            String platform = ec2Instance.getPlatform();
            boolean windows = false;
            if(platform != null && platform.length() > 0)
                windows = true;
            DeployFuture df = new DeployFuture(ec2Instance.getPublicIpAddress(), config.deploySandboxPath, partialDestPath, url, windows);
            return reservedResource.provider.config.blockingExecutor.submit(df);
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).info("look here", e);
        }
        return null;
    }

    @Override
    public Future<Void> delete(String filename)
    {
        return null;
    }

    @Override
    public Future<Void> disconnect(NetworkInstance network)
    {
        return null;
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

//	@Override
//	public Future<Integer> stop(String command) {
//		// TODO Auto-generated method stub; comes from adding stop to the parent Java interface
//		return null;
//	}
}
