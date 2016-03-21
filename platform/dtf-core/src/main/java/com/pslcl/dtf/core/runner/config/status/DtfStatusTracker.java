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
package com.pslcl.dtf.core.runner.config.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.pslcl.dtf.core.util.executor.ScheduledExecutor;

/**
 * An in-memory implementation of the <code>StatusTracker</code> interface.
 * <p>
 * This class implements the <code>StatusTracker</code> and <code>StatusTrackerMBean</code>
 * interfaces with a simple in-memory <code>HashMap</code> that captures the 
 * various <code>StatusTracker.Status enum</code> values for named subsystems of an application.
 * <p>
 * This class also supports the ENC interface registry <code>DeviceStatus interface</code>
 *  "[1:{01}]" by optionally being able to startup a provider which will reflect the 
 *  consolidated status of the application to external EMIT requestors. 
 *  <p>
 *  The consolidated status (highest level of concern) of all the named subsystems is therefore 
 *  made available via direct API, the DeviceStatus ENC interface and via Java JMX.
 *  The individual current status of any of the named subsystems is also available via
 *  direct API or Java JMX. 
 *  <p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  instantiate an instance of this class as a singleton object to the whole application and include its reference
 *  in the <code>Service</code>'s configuration object, which in turn is typically made available to all 
 *  subsystems of the application. This provides a single source of status for the whole
 *  application which each subsystem of the application can register it's individual status by name.  
 *  <p>
 *  For an example of use, the following optional service utility subsystems require a <code>StatusTracker</code>
 *  <ul>
 *  <li>StatusConnectionStateListener</li>
 *  <li>StatusServerStateListener</li>
 *  <li>BlockingExecutor</li>
 *  <li>ScheduleExecutor</li>
 *  </ul> 
 * @see StatusTracker 
 * @see StatusTrackerMXBean 
 * @see ScheduledExecutor
 */
public class DtfStatusTracker implements StatusTracker, StatusTrackerMXBean
{
    // guarded by statusMap
    private final HashMap<String, Status> statusMap;
    private Status consolidatedStatus;
    // guarded by resourceStatusListeners
    private final List<ResourceStatusListener> resourceStatusListeners;
    
    /**
     * Default constructor.
     */
    public DtfStatusTracker()
    {
        statusMap = new HashMap<String, Status>(); 
        consolidatedStatus = Status.Ok;
        resourceStatusListeners = new ArrayList<ResourceStatusListener>();
    }

    @Override
    public void setStatus(String name, Status status)
    {
        if(name == null || status == null)
            throw new IllegalArgumentException("name == null || status == null");
        synchronized (statusMap)
        {
            statusMap.put(name, status);
            if (status.ordinal() > this.consolidatedStatus.ordinal())
                this.consolidatedStatus = status;
            else
                update();
        }
    }

    @Override
    public void removeStatus(String name)
    {
        synchronized (statusMap)
        {
            statusMap.remove(name);
            update();
        }
    }

    @Override
    public List<String> getNames()
    {
        synchronized (statusMap)
        {
            Set<String> keys = statusMap.keySet();
            return new ArrayList<String>(keys);
        }
    }
    
    @Override
    public Status getStatus(String name)
    {
        if(name == null)
            throw new IllegalArgumentException("name == null");
        synchronized (statusMap)
        {
            Status status = statusMap.get(name);
            if(status != null)
                return status;
            throw new IllegalArgumentException(name + " is an unknown status subsystem");
        }
    }

    @Override
    public Status getStatus()
    {
        synchronized (statusMap)
        {
            return consolidatedStatus;
        }
    }

    @Override
    public void destroy()
    {
    }
    
    private void update()
    {
        // calls to update are within synchronized(statusMap) blocks
        Status status;
        status = Status.Ok;
        for (String iter : this.statusMap.keySet())
        {
            Status iterStatus = this.statusMap.get(iter);
            if (iterStatus.ordinal() > status.ordinal())
                status = iterStatus;
        }
        if (status != consolidatedStatus)
            consolidatedStatus = status;
    }

    @Override
    public void registerResourceStatusListener(ResourceStatusListener listener)
    {
        synchronized(resourceStatusListeners)
        {
            resourceStatusListeners.add(listener);
        }
    }

    @Override
    public void deregisterResourceStatusListener(ResourceStatusListener listener)
    {
        synchronized(resourceStatusListeners)
        {
            resourceStatusListeners.remove(listener);
        }
    }

    @Override
    public void fireResourceStatusChanged(ResourceStatusEvent status)
    {
        try
        {
            Status currentStatus = getStatus(status.statusName);
            if(currentStatus == status.status)
                return;  // current == new nothing to do
        }catch(Exception e)
        {
        }
        setStatus(status.statusName, status.status);
        
        synchronized(resourceStatusListeners)
        {
            for(ResourceStatusListener listener : resourceStatusListeners)
            {
                listener.resourceStatusChanged(status);
            }
        }
        if(status.status == Status.Down)
            removeStatus(status.statusName);
    }
}
