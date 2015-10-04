package com.pslcl.qa.runner.config;

import java.util.UUID;

import org.apache.commons.daemon.DaemonContext;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSystem;

import com.pslcl.qa.runner.RunnerService;
import com.pslcl.qa.runner.config.cli.CliBase;
import com.pslcl.qa.runner.config.cli.DtfCliCommand;
import com.pslcl.qa.runner.config.executor.BlockingExecutor;
import com.pslcl.qa.runner.config.executor.BlockingExecutorConfiguration;
import com.pslcl.qa.runner.config.executor.BlockingExecutorConfiguration.ExecutorConfig;
import com.pslcl.qa.runner.config.executor.ScheduledExecutor;
import com.pslcl.qa.runner.config.executor.ScheduledExecutorConfiguration;
import com.pslcl.qa.runner.config.executor.ScheduledExecutorConfiguration.ScheduledExecutorConfig;
import com.pslcl.qa.runner.config.status.DofSystemConfiguration;
import com.pslcl.qa.runner.config.status.StatusTracker;
import com.pslcl.qa.runner.config.status.StatusTrackerProvider;
import com.pslcl.qa.runner.config.util.TabToLevel;

public class RunnerServiceConfig extends CliBase
{
    private final static String OpendofStatusProviderKey = "com.pslcl.qa.runner.opendof-status-provider";
    private final static String OpendofStatusProviderIdKey = "com.pslcl.qa.runner.opendof-status-provider-id";

    private final static String OpendofStatusProviderDefault = "false";
    private final static String OpendofStatusProviderIdDefault = "[128:{" + UUID.randomUUID().toString() + "}]";

    public final TabToLevel initsb;
    public final StatusTracker statusTracker;
    public volatile RunnerService runnerService;
    public volatile BlockingExecutor blockingExecutor;
    public volatile ScheduledExecutor scheduledExecutor;

    private volatile boolean provideStatus;
    private volatile DOF dof;
    private volatile DOFObjectID statusOid;
    private volatile DOF.Config dofConfig;
    private volatile DOFSystem.Config systemConfig;

    public RunnerServiceConfig(DaemonContext daemonContext, RunnerService runnerService)
    {
        super(daemonContext.getArguments(), null, true, false, DefaultMaxHelpWidth);
        this.runnerService = runnerService;
        statusTracker = new StatusTrackerProvider();
        initsb = new TabToLevel(getInitsb());
    }

    public void init() throws Exception
    {
        addCommand(new DtfCliCommand(this, null));
        validateCommands();

        initsb.level.incrementAndGet(); // l1
        initsb.ttl("Status Tracker:");
        initsb.level.incrementAndGet(); // l2
        String value = properties.getProperty(OpendofStatusProviderKey, OpendofStatusProviderDefault);
        initsb.ttl(OpendofStatusProviderKey, " = ", value);
        provideStatus = Boolean.parseBoolean(value);
        DOF.Config dconfig = null;
        DOFSystem.Config sconfig = null;
        DOFObjectID soid = null;
        if (provideStatus)
        {
            dconfig = new DOF.Config.Builder().build();
            sconfig = DofSystemConfiguration.propertiesToConfig(this);
            value = properties.getProperty(OpendofStatusProviderIdKey, OpendofStatusProviderIdDefault);
            initsb.ttl(OpendofStatusProviderIdKey, " = ", value);
            soid = DOFObjectID.create(value);
        }
        initsb.level.decrementAndGet(); // l1
        dofConfig = dconfig;
        systemConfig = sconfig;
        statusOid = soid;
        
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
        if (!provideStatus)
            return;
        DOF dof = new DOF(dofConfig);
        //TODO: if this is to be supported opendof timeout properties need to be added
        DOFSystem system = dof.createSystem(systemConfig, 15000);
        DOFObject statusProvideObject = system.createObject(statusOid);
        statusTracker.beginStatusProvider(statusProvideObject);
    }

    public void daemonDestroy()
    {
        if (!provideStatus)
            return;
        dof.destroy();
    }
}
