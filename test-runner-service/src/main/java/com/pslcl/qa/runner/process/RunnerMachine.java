package com.pslcl.qa.runner.process;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import com.pslcl.qa.runner.RunnerService;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.template.TemplateProvider;


/**
 * RunnerMachine knows how to do everything with templates, test instances and test runs. Either directly or through APIs, it controls everything and can access everything.
 * It has the algorithm to assess possibilities, to determine what to do, to optimize, to process test instances, to execute test runs, and to record test results.  
 */
public class RunnerMachine {

    
    // class members

    private final AtomicBoolean initialized;
    private volatile TemplateProvider templateProvider;
    private volatile RunnerServiceConfig config;
    
    /** Constructor
     * 
     * @param runnerService
     */
    public RunnerMachine() {
        initialized = new AtomicBoolean(false);
    }

    public RunnerServiceConfig getConfig()
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
        return config.runnerService;
    }
    
    public void init(RunnerServiceConfig config) throws Exception
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
    public void initiateProcessing(long reNum, Object message) {
        try {
            RunEntryState reState = new RunEntryState(reNum, message);
            Action action = reState.getAction();  // Action.INITIALIZE
            Action nextAction = action.act(reState, null, this.templateProvider, config.runnerService);            
            // .act() stores nextAction in reState and returns it
        } catch (Exception e) {
            System.out.println("RunnerProcessor.initiateProcessing() finds Exception while handling reNum " + reNum + ": " + e + ". Message remains in the QueueStore.");
        }
    }
    
    /**
     * @note can drop reNum, the given run entry
     * @param reNum
     */
    void engageNewRunEntry(long reNum, RunEntryState reState) {
        // TODO. May choose to not process this new template; would be because this RunnerService node is overloaded. Or because this run entry already has a result stored
        // boolean doProcess = determineDoProcess(reNum, reState);
        // if (doProcess)
        {
            RunEntryState reStateOld = config.runnerService.actionStore.put(reNum, reState);
            // TODO: For reStateOld not null, consider: is this a bug, or shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
        }
        
        try {
            new RunEntryTask(this, reNum);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } // launch independent thread
    }

    /**
     * 
     * @param reNum
     */
    void removeTemplateRun(long reNum) {
        RunEntryState reStateOld = config.runnerService.actionStore.remove(reNum);
        // TODO: Shall we cleanup whatever it is that reStateOld shows has been allocated or started or whatever?
    }
    
}