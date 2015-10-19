package com.pslcl.dtf.common;

import java.lang.Thread.UncaughtExceptionHandler;

import javax.jms.Message;

import org.apache.commons.daemon.Daemon;

import com.pslcl.dtf.common.config.RunnerConfig;

public interface Runner extends Daemon, RunnerMBean, UncaughtExceptionHandler
{
    public RunnerConfig getConfig();

    /**
     * 
     * @param strRunEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).
     * @param message JMS message associated with reNum, used for eventual message ack
     * @throws Exception
     */
    public void submitQueueStoreNumber(String strRunEntryNumber, Message message) throws Exception;

    /**
     * Test pipeline backdoor
     * @return a RunnerMachine object;
     */
    public Object getRunnerMachine();
}