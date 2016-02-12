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
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.runner.resource.instance.MachineInstance;
import com.pslcl.dtf.core.util.StrH;
import com.pslcl.dtf.runner.process.ProcessTracker;
import com.pslcl.dtf.runner.process.RunEntryStateStore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * Control the Runner Service startup and shutdown.
 * 
 * RunnerService has no requirement that it be instantiated more than once, but it is coded to allow that possibility.
 * Static references are limited to the Action enum (holds pure code), the QueueStore (one only), and the template database (one only).
 */
public class RunnerService implements Runner, RunnerServiceMBean
{
    // instance declarations 
    
    private volatile MessageQueue mq = null;
    private volatile RunnerConfig config = null;
    private volatile DBConnPool dbConnPool = null;
    private volatile QAPortalAccess qaPortalAccess = null;

    /** the process classes */
    public volatile RunnerMachine runnerMachine = null;
    public volatile RunEntryStateStore runEntryStateStore = null; // holds RunEntryState of each reNum
    public volatile ProcessTracker processTracker = null;

    
    // public class methods

    /**
     * Constructor
     */
    public RunnerService()
    {
        // Setup what we can, prior to knowing configuration. Most setup is done in the init method.
        Thread.setDefaultUncaughtExceptionHandler(this);
        
        // Note: For this class that implements the Daemon interface, do not use a local variable for a logger.
        //       We use LoggerFactory.getLogger(getClass()), to allow more predictable behavior in the face of the worst failures and the follow-on stop() and destroy() calls.
    }

    public RunnerMachine getRunnerMachine()
    {
        return runnerMachine;
    }
    
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
            config = new RunnerConfig(daemonContext, this);
            config.init();

            config.initsb.ttl("Initialize JMX: ");
            config.initsb.indentedOk();
            config.initsb.ttl("Initialize RunnerMachine:");
            config.initsb.indentedOk();
            config.initsb.ttl("Initialize MessageQueueBase:");
            config.initsb.level.incrementAndGet(); // l2
            String daoClass = config.properties.getProperty(ResourceNames.MsgQueClassKey, ResourceNames.MsgQueClassDefault);
            daoClass = StrH.trim(daoClass);
            config.initsb.ttl(ResourceNames.MsgQueClassKey, " = ", daoClass);
            this.mq = (MessageQueue) Class.forName(daoClass).newInstance();
            this.mq.init(config);
                
            config.initsb.indentedOk();
            config.initsb.level.decrementAndGet();

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName("pslcl.dtf.platform:type=RunnerService"));

            this.dbConnPool = new DBConnPool();
            this.dbConnPool.init(config);
            this.runnerMachine = new RunnerMachine(dbConnPool);
            this.runnerMachine.init(config);
            this.runEntryStateStore = new RunEntryStateStore();
            this.processTracker = new ProcessTracker(this);
            this.qaPortalAccess = new QAPortalAccess();
            this.qaPortalAccess.init(config);
        } catch (Exception e)
        {
        	LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + config.initsb.sb.toString(), e);
            throw new DaemonInitException(getClass().getSimpleName() + " failed:", e);
        }
    }

    @Override
    public void start() throws Exception
    {
        try
        {
        	config.initsb.ttl(getClass().getSimpleName() + " start");
            if(mq != null) // can be configured to disable the queue, see init
            {
                if (mq.queueStoreExists())
                {
                    // Setup QueueStore DAO-referenced message handler (a standard callback from the JMS spec)
                    mq.initQueueStoreGet();
                } else
                {
                	LoggerFactory.getLogger(getClass()).warn(getClass().getSimpleName() + ".start exits- QueueStore message queue not available.");
                    throw new Exception("QueueStore not available");
                }
            }
            config.initsb.indentedOk();
        } catch (Exception e)
        {
        	LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + config.initsb.sb.toString(), e);
            throw e;
        }
        LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + config.initsb.sb.toString());
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
        this.dbConnPool.destroy();
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

    
    // public methods 
    
    public QAPortalAccess getQAPortalAccess() {
    	return this.qaPortalAccess;
    }
    
    /**
     * 
     * @param strRunEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).
     * @param message JMS message associated with reNum, used for eventual message ack
     * @throws Throwable
     */
    public void submitQueueStoreNumber(String strRunEntryNumber, Message message) throws Throwable
    {
        try
        {
            long reNum = Long.parseLong(strRunEntryNumber);
            try
            {
                if (ProcessTracker.isResultStored(this.dbConnPool, reNum)) {
                    LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".submitQueueStoreNumber() finds reNum " + reNum + " has a non-null result already stored. Acking this reNum now.");
                    ackRunEntry(message);
                } else if (processTracker.isRunning(reNum)) {
                	LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".submitQueueStoreNumber() finds reNum " + reNum + ", work already processing. No action taken. ");
                } else {
                	LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".submitQueueStoreNumber() submits reNum " + reNum + " for testRun processing. ");
                    // This call must ack the message, or cause it to be acked out in the future. Failure to do so will repeatedly re-introduce this reNum.
                    runnerMachine.initiateProcessing(reNum, message);
                }
            } catch (Exception e) {
                // do nothing; reNum remains in InstanceStore, we will see it again
            	LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".submitQueueStoreNumber() sees exception for reNum " + reNum + ". Leave reNum in QueueStore. Exception msg: " + e);
                throw e;
            }
        } catch (Exception e) {
            throw e; // the original caller must ack the message
        }
    }

    /**
     *
     * @note Classes that do not know about the JMS library's Message class, can call this instead.
     * @param message Original opaque message associated with a run entry number, used now to ack the message
     * @throws Exception 
     */
    public void ackRunEntry(Object message) throws Exception {
    	if (!javax.jms.Message.            // javax.jms.Message is a Java interface
    			class.isAssignableFrom(message.getClass())) {
    		throw new Exception("parameter message is not a JMS Message");
    	}
        mq.ackQueueStoreEntry((Message)message);
    }

}