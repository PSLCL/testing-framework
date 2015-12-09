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
package com.pslcl.dtf.resource.aws.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.resource.aws.AwsResourcesManager;

@SuppressWarnings("javadoc")
public abstract class AwsResourceProvider
{
    protected final Logger log;
    public final AwsResourcesManager manager;
    public volatile RunnerConfig config;
    public volatile AmazonEC2Client ec2Client;
    
    protected AwsResourceProvider(AwsResourcesManager manager)
    {
        log = LoggerFactory.getLogger(getClass());
        this.manager = manager;
    }

    public void setEc2Client(AmazonEC2Client ec2Client)
    {
        this.ec2Client = ec2Client;
    }
    
    protected void init(RunnerConfig config) throws Exception
    {
        config.initsb.ttl(getClass().getSimpleName(), " Initialization");
        config.initsb.level.incrementAndGet();
        this.config = config;
        this.ec2Client = ec2Client;
    }
    
    protected void destroy()
    {
        ec2Client.shutdown();
    }
}

