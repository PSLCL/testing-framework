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
package com.pslcl.dtf.runner;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.Runner;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.messageQueue.MessageQueue;
import com.pslcl.dtf.runner.process.ActionStore;
import com.pslcl.dtf.runner.process.ProcessTracker;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * Control the Runner Service startup and shutdown.
 * 
 * RunnerService has no requirement that it be instantiated more than once, but it is coded to allow that possibility.
 * Static references are limited to the Action enum (holds pure code), the QueueStore (one only), and the template database (one only).
 */
public class RunnerService implements Runner, RunnerServiceMBean
{
    // static declarations

    public static final String QueueStoreDaoClassKey = "pslcl.dtf.resource.mq-class";
    public static final String QueueStoreDaoClassDefault = "com.pslcl.dtf.resource.aws.Sqs";

    private volatile MessageQueue mq;
    private volatile RunnerConfig config;

    /** the process classes */
    public volatile RunnerMachine runnerMachine;
    public volatile ActionStore actionStore; // holds state of each template; TODO: actionStore is instantiated here, but not yet otherwise used 
    public volatile ProcessTracker processTracker;

    // public class methods

    /**
     * Constructor
     */
    public RunnerService()
    {
        // Setup what we can, prior to knowing configuration
        // Most setup should be done in the init method.
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public RunnerMachine getRunnerMachine()
    {
        return runnerMachine;
    }
    
    @Override
    public RunnerConfig getConfig()
    {
        return config;
    }
    
    // Daemon interface implementations

    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException
    {
        try
        {
            // The RunnerServiceConfig may initialize logging based on commandline switches and/or configuration file
            // and yet this method can fail with a DaemonInitException, in which case other methods of this class
            // will be called (stop, destroy).  Because of this, its best to not declare a local instance or static
            // logger variable in this class, always use the factory for every log.
            config = new RunnerConfig(daemonContext, this);
            config.init();

            runnerMachine = new RunnerMachine();
            actionStore = new ActionStore(); // TODO: any order required?
            processTracker = new ProcessTracker(this);

            config.initsb.ttl("Initialize JMX: ");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName("com.pslcl.qa.platform:type=RunnerService"));
            config.initsb.indentedOk();
            config.initsb.ttl("Initialize RunnerMachine:");
            runnerMachine.init(config);
            config.initsb.indentedOk();

            config.initsb.ttl("Initialize QueueStoreDao:");
            config.initsb.level.incrementAndGet(); // l2
            String daoClass = config.properties.getProperty(QueueStoreDaoClassKey, QueueStoreDaoClassDefault);
            config.initsb.ttl(QueueStoreDaoClassKey, " = ", daoClass);
            mq = (MessageQueue) Class.forName(daoClass).newInstance();
            mq.init(config);
            config.initsb.indentedOk();
            config.initsb.level.decrementAndGet();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(config.initsb.sb.toString(), e);
            throw new DaemonInitException(getClass().getSimpleName() + " failed:", e);
        }
        // process RunnerService config
    }

    @Override
    public void start() throws Exception
    {
        try
        {
            config.initsb.ttl(getClass().getSimpleName() + " start");
            if (mq.queueStoreExists())
            {
                // Setup QueueStore DAO-referenced message handler (a standard callback from the JMS spec)
                mq.initQueueStoreGet();
            } else
            {
                LoggerFactory.getLogger(getClass()).warn("RunnerService.start exits- QueueStore message queue not available.");
                throw new Exception("QueueStore not available");
            }
            config.initsb.indentedOk();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(config.initsb.sb.toString(), e);
            throw e;
        }
        LoggerFactory.getLogger(getClass()).debug(config.initsb.sb.toString());
    }

    @Override
    public void stop() throws Exception
    {
        LoggerFactory.getLogger(getClass()).debug("Stopping RunnerService.");
        if (runnerMachine != null)
            runnerMachine.destroy();
        // Destroy the Status Tracker
        if (config != null)
            config.statusTracker.destroy();
    }

    /**
     * Cleanup objects created by the service.
     */
    @Override
    public void destroy()
    {
        // jsvc calls this to destroy resources created in init()
        LoggerFactory.getLogger(getClass()).info("Destroying RunnerService.");
    }

    // RunnerServiceMBean interface implementations

    @Override
    public short getStatus()
    {
        return (short) config.statusTracker.getStatus().ordinal();
    }

    @Override
    public float getLoad()
    {
        synchronized (this)
        {
            // TODO Is there a better measurement of load than this?
            double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            double memLoad = (double) memUse.getUsed() / memUse.getMax();
            if (memLoad > cpuLoad)
                return (float) memLoad;
            return (float) cpuLoad;
        }
    }

    // UncaughtExceptionHandler interface implementation

    /**
     * Process information about an uncaught Exception.
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        String msg = "FATAL ERROR: Uncaught exception in thread " + thread;
        LoggerFactory.getLogger(getClass()).error(msg, ex);
        LoggerFactory.getLogger(getClass()).debug("The FATAL ERROR message (at error log level) is issued by the handler RunnerService.uncaughtException()");
        // Since the state is unknown at this point, we may not be able to perform a graceful exit.
        System.exit(1); // forces termination of all threads in the JVM
    }

    /**
     * 
     * @param strRunEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).
     * @param message JMS message associated with reNum, used for eventual message ack
     * @throws Exception
     */
    public void submitQueueStoreNumber(String strRunEntryNumber, Message message) throws Exception
    {
        try
        {
            // the ordinary method
            long reNum = Long.parseLong(strRunEntryNumber);
            LoggerFactory.getLogger(getClass()).debug("RunnerService.submitQueueStoreNumber() finds reNum " + reNum);
            try
            {
                if (ProcessTracker.resultStored(reNum))
                {
                    ackRunEntry(message);
                    System.out.println("RunnerService.submitQueueStoreNumber() finds reNum " + reNum + ", result already stored. Acking this reNum now.");
                } else if (processTracker.inProcess(reNum))
                {
                    System.out.println("RunnerService.submitInstanceNumber_Store() finds reNum " + reNum + ", work already processing");
                } else
                {
                    // This call must ack the message, or cause it to be acked out in the future. Failure to do so will repeatedly re-introduce this reNum.
                    runnerMachine.initiateProcessing(reNum, message);
                }
            } catch (Exception e)
            {
                // do nothing; reNum remains in InstanceStore, we will see it again
                System.out.println("RunnerService.submitInstanceNumber_Store() sees exception for reNum " + reNum + ". Leave reNum in QueueStore. Exception msg: " + e);
            }
        } catch (Exception e)
        {
            throw e; // recipient must ack the message
        }
    }

    /**
     * 
     * @param message Original opaque message associated with a run entry number, used now to ack the message
     * @throws JMSException
     */
    private void ackRunEntry(Object message) throws JMSException
    {
        // this call is for classes that do not know about JMS
        mq.ackQueueStoreEntry((Message) message);
    }

}