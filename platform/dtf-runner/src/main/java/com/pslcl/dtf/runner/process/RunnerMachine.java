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
package com.pslcl.dtf.runner.process;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.runner.DBConnPool;
import com.pslcl.dtf.runner.RunnerService;
import com.pslcl.dtf.runner.template.TemplateProvider;


/**
 * RunnerMachine knows how to do everything with templates, test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.
 */
public class RunnerMachine {

    // instance declarations

    private final AtomicBoolean initialized;
    private volatile TemplateProvider templateProvider;
    private volatile RunnerConfig config;
    private final DBConnPool dbConnPool;
    private final Logger log;
    private final String simpleName;


    // public class methods

    /** Constructor
     *
     * @param dbConnPool The database connection pool
     * @throws Exception on any error
     */
    public RunnerMachine(DBConnPool dbConnPool) throws Exception {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.dbConnPool = dbConnPool;
        this.initialized = new AtomicBoolean(false);
    }

    public DBConnPool getDBConnPool() {
        return this.dbConnPool;
    }

    public RunnerConfig getConfig()
    {
        return config;
    }

    public TemplateProvider getTemplateProvider()
    {
        return templateProvider;
    }

    public RunnerService getService()
    {
        if(!initialized.get())
        {
            log.error(simpleName + ".getService called before daemon init completed");
            return null;  //TODO: yea bad things are going to happen, never happen right??
            // remove this if no constructor and no init method could cause this ... i.e. only possible after daemon.start();
        }
        return (RunnerService)config.runnerService;
    }

    /**
     * Note: Pair init() with a destroy() call
     * @param config The RunnerConfig
     * @throws Exception on any error
     */
    public void init(RunnerConfig config) throws Exception
    {
        this.config = config;
        templateProvider = new TemplateProvider();
        config.statusTracker.registerResourceStatusListener(templateProvider);
        templateProvider.init(config);
        initialized.set(true);
    }

    public void destroy() throws Exception
    {
        initialized.set(false);
        templateProvider.destroy();
        config.statusTracker.deregisterResourceStatusListener(templateProvider);
    }

    /**
     * Initiate a new test run, corresponding to a top level template that has an entry in table run).
     *
     * Note: When this call succeeds, runnerService.ackQueueStoreEntry() must be called. This can happen in the future from a distant thread, or very quickly.
     * @param reNum The run entry number
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Throwable on any error
     */
    public void initiateProcessing(long reNum, Object message) throws Throwable {
        try {
            RunEntryState reState = new RunEntryState(reNum, message);
            Action action = reState.getAction();  // Action.INITIALIZE
            /*Action nextAction =*/ action.act(reState, null, this.getService());
            // .act() stores a computed next action in reState and returns it as nextAction
        } catch (Exception e) {
            log.warn(simpleName + "initiateProcessing() finds Exception while handling reNum " + reNum + ": " + e + ". Message remains in the QueueStore.");
            throw e;
        }
    }

    /**
     * Note: For possible detected conditions, this can potentially drop reNum, the given run entry number.
     * @param reNum
     * @param reState
     * @throws Exception
     */
    void engageRunEntry(long reNum, RunEntryState reState) throws Exception {
        // check runnerService availability, by configured limit or otherwise
        if (this.getService().isAvailableToProcess())
        {
            log.debug(simpleName + "engageRunEntry() processes reNum/action/message " + reNum + "/" +
                                   reState.getAction() + "/" + reState.getMessage());
            RunEntryState reStateOld = this.getService().runEntryStateStore.get(reNum);
            if (reStateOld != null) {
                log.warn("engageRunEntry() finds existing reState while processing reNum/action/message " + reNum +
                         "/" + reState.getAction() +    "/" + reState.getMessage() + ". reStateOld holds " +
                         reStateOld.getRunEntryNumber() + "/" + reStateOld.getAction() + "/" + reStateOld.getMessage());
                throw new Exception("Attempt to launch duplicate runEntry, the new runEntry is dropped");
            }
            this.getService().runEntryStateStore.put(reNum, reState);

            log.info("Starting test run " + reNum);
            try {
                // launch independent thread
                new RunEntryTask(this, reNum);
            } catch (Exception e) {
                this.getService().runEntryStateStore.remove(reNum);
                LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() +
                                                          ".engageRunEntry() failed to engage reNum " + reNum);
                throw e;
            }
        } else {
            LoggerFactory.getLogger(getClass()).debug(getClass().getSimpleName() + ".engageRunEntry() postpones reNum "
                                                      + reNum + ": runnerService limit reached");
        }
    }

    /**
     *
     * @param reNum
     */
    void disengageRunEntry(long reNum) {
        /*RunEntryState reStateOld =*/ getService().runEntryStateStore.remove(reNum);
        // TODO: Shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
    }

}