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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Instance;
import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.exception.IncompatibleResourceException;
import com.pslcl.dtf.core.runner.resource.instance.CableInstance;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.runner.resource.instance.NetworkInstance;
import com.pslcl.dtf.core.runner.resource.instance.RunnableProgram;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.resource.aws.provider.machine.MachineReservedResource;

@SuppressWarnings("javadoc")
public class AwsMachineInstance implements MachineInstance
{
    private final Logger log;
    private final String name;
    private final Map<String, String> attributes;
    private String description;
    private int timeoutSeconds;
    private final ResourceCoordinates coordinates;
    public final Instance ec2Instance;

    public AwsMachineInstance(MachineReservedResource reservedResource)
    {
        log = LoggerFactory.getLogger(getClass());
        name = reservedResource.resource.getName();
        attributes = reservedResource.resource.getAttributes();
        coordinates = reservedResource.resource.getCoordinates();
        ec2Instance = reservedResource.ec2Instance;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Map<String, String> getAttributes()
    {
        synchronized (attributes)
        {
            return new HashMap<String, String>(attributes);
        }
    }

    @Override
    public void addAttribute(String key, String value)
    {
        synchronized (attributes)
        {
            attributes.put(key, value);
        }
    }
    
    @Override
    public ResourceProvider getResourceProvider()
    {
        return coordinates.getProvider();
    }

    @Override
    public ResourceCoordinates getCoordinates()
    {
        return coordinates;
    }


    @Override
    public Future<CableInstance> connect(NetworkInstance network) throws IncompatibleResourceException
    {
        return null;
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
    public Future<Void> deploy(String filename, String artifactHash)
    {
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

//    public static class WaitForInstanceState implements Callable<AwsInstanceState>
//    {
//        private final Instance instance;
//        private final AwsInstanceState state;
//        private final int pollDelay;
//        private final int timeout;
//        private final RunnerConfig config;
//        private final AtomicBoolean interrupted;
//        private final AtomicBoolean timedOut;
//
//        public WaitForInstanceState(Instance instance, AwsInstanceState state, RunnerConfig config, int pollDelay, int timeout)
//        {
//            this.instance = instance;
//            this.state = state;
//            this.pollDelay = pollDelay;
//            this.timeout = timeout;
//            this.config = config;
//            interrupted = new AtomicBoolean(false);
//            timedOut = new AtomicBoolean(false);
//        }
//
//        @Override
//        public AwsInstanceState call() throws Exception
//        {
//            Timeout timeoutTask = new Timeout(this, config.scheduledExecutor, timeout);
//            config.blockingExecutor.execute(timeoutTask);
//            do
//            {
//                if (AwsInstanceState.getState(instance.getState().getName()) == state)
//                {
//                    timeoutTask.cancel();
//                    return state;
//                }
//                if(interrupted.get())
//                    throw new InterruptedException();
//                if(timedOut.get())
//                    throw new Exception("timedout exception");
//                Thread.sleep(pollDelay);
//            } while (true);
//        }
//
//        public void interrupted()
//        {
//            interrupted.set(true);
//        }
//
//        public void timeout()
//        {
//            timedOut.set(true);
//        }
//    }

//    public static class Timeout implements Runnable
//    {
//        private final AtomicBoolean cancel;
//        private final TimeoutTask timeoutTask;
//        private final WaitForInstanceState caller;
//
//        public Timeout(WaitForInstanceState caller, ScheduledExecutor timer, int timeout)
//        {
//            cancel = new AtomicBoolean(false);
//            this.caller = caller;
//            timeoutTask = new TimeoutTask(timer, timeout);
//        }
//
//        @Override
//        public void run()
//        {
//            try
//            {
//                timeoutTask.waitForComplete();
//                caller.timeout();
//            } catch (InterruptedException e)
//            {
//                caller.interrupted();
//            }
//        }
//
//        public void cancel()
//        {
//            timeoutTask.cancel(true);
//        }
//    }

    public enum AwsInstanceState
    {
        Pending("pending"), Running("running"), ShuttingDown("shutting-down"), Terminated("terminated"), Stopping("stopping"), Stopped("stopped");

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
