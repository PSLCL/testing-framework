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

import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.runner.RunnerService;
import com.pslcl.dtf.runner.template.TemplateProvider;


/**
 * RunnerMachine knows how to do everything with templates, test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.  
 */
public class RunnerMachine {

    
    // class members

    private final AtomicBoolean initialized;
    private volatile TemplateProvider templateProvider;
    private volatile RunnerConfig config;
    
    /** Constructor
     * 
     * @param runnerService
     */
    public RunnerMachine() {
        initialized = new AtomicBoolean(false);
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
            LoggerFactory.getLogger(getClass()).error(getClass().getSimpleName() + ".getService called before daemon init completed");
            return null;  //TODO: yea bad things are going to happen, never happen right??  
            // remove this if no constructor and no init method could cause this ... i.e. only possible after daemon.start();
        }
        return (RunnerService)config.runnerService;
    }
    
    public void init(RunnerConfig config) throws Exception
    {
        this.config = config;
        templateProvider = new TemplateProvider();
        config.statusTracker.registerResourceStatusListener(templateProvider);
        templateProvider.init(config);
        initialized.set(true);
    }
    
    public void destroy() 
    {
        config.statusTracker.deregisterResourceStatusListener(templateProvider);
        templateProvider.destroy();
        initialized.set(false);
    }
    
    /**
     * Accept new template to become a test run, with an entry in table run
     * 
     * @note runnerService.ackQueueStoreEntry() must eventually be called; it may quickly be called in this method, or eventually from a distant thread
     * @oaram reNum
     * @param message An opaque Java object, used to acknowledge the message when processing is complete
     * @throws Exception
     */
    public void initiateProcessing(long reNum, Object message) throws Exception {
        try {
            RunEntryState reState = new RunEntryState(reNum, message);
            Action action = reState.getAction();  // Action.INITIALIZE
            Action nextAction = action.act(reState, null, getService());            
            // .act() stores nextAction in reState and returns it
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling reNum " + reNum + ": " + e + ". Message remains in the QueueStore.");
            throw e;
        }
    }
    
    /**
     * @note can drop reNum, the given run entry
     * @param reNum
     */
    void engageNewRunEntry(long reNum, RunEntryState reState) throws Exception {
        // TODO. May choose to not process this new template; would be because this RunnerService node is overloaded. Or because this run entry already has a result stored
        // boolean doProcess = determineDoProcess(reNum, reState);
        // if (doProcess)
        {
            RunEntryState reStateOld = getService().actionStore.put(reNum, reState);
            // TODO: For reStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
        }
        
        try {
            // launch independent thread
            new RunEntryTask(this, reNum);
        } catch (Exception e) {
        	throw e;
        }
        
    }

    /**
     * 
     * @param reNum
     */
    void removeTemplateRun(long reNum) {
        RunEntryState reStateOld = getService().actionStore.remove(reNum);
        // TODO: Shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
    }
    
}