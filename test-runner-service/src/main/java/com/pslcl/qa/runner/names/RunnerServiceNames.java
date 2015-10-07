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
package com.pslcl.qa.runner.names;

import com.pslcl.qa.runner.store.instance.Sqs;

public class RunnerServiceNames extends Exception
{
    // see com.pslcl.qa.runner.RunnerService
    public static final String QueueStoreDaoClassKey = "com.pslcl.qa.runner.mq-class";
    public static final String QueueStoreDaoClassDefault = Sqs.class.getName();

    // see com.pslcl.qa.runner.store.instance.Sqs
    public static final String QueueStoreNameKey = "com.pslcl.qa.runner.store.instance.queue-name";
    public static final String QueueStoreNameDefault = "q";

    // see com.pslcl.qa.runner.template.ResourceProviders
    public static final String MachineProviderClassKey = "pslcl.qa.runner.template.machine-provider-class"; 
    public static final String PersonProviderClassKey = "pslcl.qa.runner.template.person-provider-class"; 
    public static final String NetworkProviderClassKey = "pslcl.qa.runner.template.network-provider-class";
    public static final String MachineProviderClassDefault = "com.pslcl.qa.runner.resource.aws.AwsMachineProvider";
    public static final String PersonProviderClassDefault = "com.pslcl.qa.runner.resource.aws.AwsPersonProvider";
    public static final String NetworkProviderClassDefault = "com.pslcl.qa.runner.resource.aws.AwsNetworkProvider";
    
    // see com.pslcl.qa.runner.config.executor.BlockingExecutorConfiguration
    public static final String BlockingExecCorePoolSizeKey = "pslcl.service.util.executor.core-size";
    public static final String MaximumQueueSizeKey = "pslcl.service.util.executor.max-queue-size";
    public static final String MaxBlockingTimeKey = "pslcl.service.util.executor.max-blocking-time";
    public static final String BlockingExecThreadNamePrefixKey = "pslcl.service.util.executor.thread-name";
    public static final String KeepAliveDelayKey = "pslcl.service.util.executor.keep-alive-delay";
    public static final String AllowCoreThreadTimeoutKey = "pslcl.service.util.executor.core-timeout";
    public static final String BlockingExecStatusNameKey = "pslcl.service.util.executor.status-name";
    public static final String BlockingExecCorePoolSizeDefault = "8";
    public static final String MaximumQueueSizeDefault = "128";
    public static final String MaxBlockingTimeDefault = "120000";
    public static final String BlockingExecThreadNamePrefixDefault = "PslclBlockingExecutor";
    public static final String KeepAliveDelayDefault = "120000";
    public static final String AllowCoreThreadTimeoutDefault = "true";
    public static final String BlockingExecStatusNameDefault = "PslclBlockingExecutor";
    
    // see com.pslcl.qa.runner.config.executor.ScheduledExecutorConfiguration
    public static final String ScheduledExedCorePoolSizeKey = "pslcl.service.util.scheduled-executor.core-size";
    public static final String ScheduledExedThreadNamePrefixKey = "pslcl.service.util.scheduled-executor.thread-name";
    public static final String ScheduledExedStatusNameKey = "pslcl.service.util.scheduled-executor.status-name";
    public static final String ScheduledExedCorePoolSizeDefault = "2";
    public static final String ScheduledExedThreadNamePrefixDefault = "PslclScheduledExecutor";
    public static final String ScheduledExedStatusNameDefault = "PslclScheduledExecutor";

    // see com.pslcl.qa.runner.config.status.DofSystemConfiguration
    public static final String CredentialFileKey = "pslcl.dof.system.credentials";
    public static final String TunnelKey = "pslcl.dof.system.tunnel";
    public static final String TunnelDefault = "false";

}