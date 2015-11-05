/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dtf.core.util;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("javadoc")
public class TimeoutTask implements Callable<Void>
{
    private final FutureTask<Void> timeoutTask;
    private final AtomicBoolean timedout;
    private final AtomicBoolean completed;
    
    public TimeoutTask(ScheduledThreadPoolExecutor timer, int timeout)
    {
        timedout = new AtomicBoolean(false);
        completed = new AtomicBoolean(false);
        timeoutTask = new FutureTask<Void>(this);
        timer.schedule(timeoutTask, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public Void call() throws Exception
    {
        synchronized (this)
        {
            completed.set(true);
            timedout.set(true);
            notifyAll();
        }
        return null;
    }
    
    public void completed()
    {
        synchronized(this)
        {
            completed.set(true);
            timedout.set(false);
            timeoutTask.cancel(true);   
            notifyAll();
        }
    }
    
    public void cancel(boolean interruptRunning)
    {
        synchronized(this)
        {
            completed.set(false);
            timedout.set(false);
            timeoutTask.cancel(interruptRunning);   
            notifyAll();
        }
    }
    
    /**
     * 
     * @return false if timed out, true if completed normally
     * @throws InterruptedException 
     */
    public boolean waitForComplete() throws InterruptedException
    {
        synchronized (this)
        {
            if(completed.get())
                return timedout.get();
            wait();
        }
        return timedout.get();
    }
}
