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
package com.pslcl.dtf.common.config;

import java.util.Properties;

import org.apache.commons.daemon.DaemonContext;

import com.pslcl.dtf.common.Runner;
import com.pslcl.dtf.common.config.cli.CliBase;
import com.pslcl.dtf.common.config.cli.CliCommand;
import com.pslcl.dtf.common.config.cli.CliDaemonContext.CliDaemonController;
import com.pslcl.dtf.common.config.cli.DtfCli;
import com.pslcl.dtf.common.config.executor.BlockingExecutor;
import com.pslcl.dtf.common.config.executor.BlockingExecutorConfiguration;
import com.pslcl.dtf.common.config.executor.BlockingExecutorConfiguration.ExecutorConfig;
import com.pslcl.dtf.common.config.executor.ScheduledExecutor;
import com.pslcl.dtf.common.config.executor.ScheduledExecutorConfiguration;
import com.pslcl.dtf.common.config.executor.ScheduledExecutorConfiguration.ScheduledExecutorConfig;
import com.pslcl.dtf.common.config.status.StatusTracker;
import com.pslcl.dtf.common.config.status.StatusTrackerProvider;
import com.pslcl.dtf.common.config.util.TabToLevel;

/**
 * Global configuration object
 * 
 */

public class RunnerConfig
{
    public final TabToLevel initsb;
    public final StatusTracker statusTracker;
    private final CliBase cliBase;
    public final Properties properties;
    public volatile Runner runnerService;
    public volatile BlockingExecutor blockingExecutor;
    public volatile ScheduledExecutor scheduledExecutor;
    public volatile CliCommand command;

    private volatile boolean provideStatus;

    public RunnerConfig(DaemonContext daemonContext, Runner runnerService)
    {
        this.runnerService = runnerService;
        statusTracker = new StatusTrackerProvider();
        CliBase cbase = null;
        if(daemonContext.getController() instanceof CliDaemonController)
        {
            cbase = ((CliDaemonController)daemonContext.getController()).cliBase;
            if(cbase == null)
                cbase = new DtfCli(daemonContext.getArguments());
        }
        if(cbase == null)
            cbase = new DtfCli(daemonContext.getArguments());
        cliBase = cbase;
        properties = cliBase.properties;
        initsb = new TabToLevel(cliBase.getInitsb());
    }

    public void init() throws Exception
    {
        command = cliBase.validateCommands();

        initsb.level.incrementAndGet(); // l1
        initsb.ttl("Status Tracker:");
        initsb.level.incrementAndGet(); // l2
        initsb.level.decrementAndGet(); // l1
        
        initsb.ttl("Blocking Executor Initialization");
        initsb.level.incrementAndGet();
        blockingExecutor = new BlockingExecutor();
        ExecutorConfig beconfig = BlockingExecutorConfiguration.propertiesToConfig(this);
        blockingExecutor.init(beconfig);
        initsb.level.decrementAndGet();
        
        initsb.ttl("Scheduled Executor Initialization");
        initsb.level.incrementAndGet();
        scheduledExecutor = new ScheduledExecutor();
        ScheduledExecutorConfig seconfig = ScheduledExecutorConfiguration.propertiesToConfig(this);
        scheduledExecutor.init(seconfig);
        initsb.level.decrementAndGet();
    }

    public void daemonStart() throws Exception
    {
    }

    public void daemonDestroy()
    {
        if (!provideStatus)
            return;
    }
}
