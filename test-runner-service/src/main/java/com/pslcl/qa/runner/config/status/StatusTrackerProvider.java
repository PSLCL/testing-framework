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
package com.pslcl.qa.runner.config.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFNotSupportedException;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObject.DefaultProvider;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFRequest.Subscribe;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.value.DOFUInt8;

import com.pslcl.qa.runner.config.executor.ScheduledExecutor;

/**
 * An in-memory implementation of the <code>StatusTracker</code> interface.
 * </p>
 * This class implements the <code>StatusTracker</code> and <code>StatusTrackerMBean</code>
 * interfaces with a simple in-memory <code>HashMap</code> that captures the 
 * various <code>StatusTracker.Status enum</code> values for named subsystems of an application.
 * </p>
 * This class also supports the ENC interface registry <code>DeviceStatus interface</code>
 *  "[1:{01}]" by optionally being able to startup a provider which will reflect the 
 *  consolidated status of the application to external EMIT requestors. 
 *  </p>
 *  The consolidated status (highest level of concern) of all the named subsystems is therefore 
 *  made available via direct API, the DeviceStatus ENC interface and via Java JMX.
 *  The individual current status of any of the named subsystems is also available via
 *  direct API or Java JMX. 
 *  </p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  instantiate an instance of this class as a singleton object to the whole application and include its reference
 *  in the <code>Service<T></code>'s configuration object, which in turn is typically made available to all 
 *  subsystems of the application. This provides a single source of status for the whole
 *  application which each subsystem of the application can register it's individual status by name.  
 *  </p>
 *  For an example of use, the following optional service utility subsystems require a <code>StatusTracker</code>
 *  <ul>
 *  <li>StatusConnectionStateListener</li>
 *  <li>StatusServerStateListener</li>
 *  <li>BlockingExecutor</li>
 *  <li>ScheduleExecutor</li>
 *  </ul> 
 * @see StatusTracker 
 * @see StatusTrackerMXBean 
 * @see StatusConnectionStateListener
 * @see StatusServerStateListener
 * @see ScheduledExecutor
 */
public class StatusTrackerProvider implements StatusTracker, StatusTrackerMXBean
{
    // guarded by statusMap
    private final HashMap<String, Status> statusMap;
    private Status consolidatedStatus;
    private DOFOperation.Provide provideOperation;
    
    /**
     * Default constructor.
     */
    public StatusTrackerProvider()
    {
        statusMap = new HashMap<String, Status>(); 
        consolidatedStatus = Status.Ok;
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
            {
                this.consolidatedStatus = status;
                if(provideOperation != null)
                    provideOperation.getObject().changed(statusProperty);
            }else
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
        synchronized (statusMap)
        {
            if(provideOperation != null)
            {
                provideOperation.cancel();
                provideOperation = null;
            }
        }
    }
    
    /**
     * Begin Providing the Status interface on the given <code>DOFObject</code>.
     * </p>
     * Start providing the ENC interface registry <code>DeviceStatus interface</code>
     *  "[1:{01}]".  The consolidated status of all reporting subsystems will be made 
     *  available with any changes to the consolidated status firing a notification to
     *  subscribers of the DeviceStatus interface. 

     * @param object the <code>DOFObject</code> to provide on.
     */
    @Override
    public void beginStatusProvider(DOFObject object)
    {
        if(object == null)
            throw new IllegalArgumentException("object == null");
        synchronized (statusMap)
        {
            provideOperation = object.beginProvide(DEF, DOF.TIMEOUT_NEVER, new ProviderImpl(), null);
        }
    }

    /**
     * End providing the <code>DeviceStatus interface</code>.
     */
    @Override
    public void endStatusProvider()
    {
        synchronized (statusMap)
        {
            if(provideOperation != null)
                provideOperation.cancel();
        }
    }
    
//    /**
//     * Determines the load percentage of the service. This is a value between 0.0 and 1.0, including 1.0 as a possible value.
//     * </p>
//     * This default implemenation determines load based on the Operating System's CPU load and the JVM memory load.
//     * @return the current load
//     */
//    @Override
//    public float getLoad()
//    {
//        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
//        MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
//        double memLoad = (double) memUse.getUsed() / memUse.getMax();
//        if (memLoad > cpuLoad)
//            return (float) memLoad;
//        return (float) cpuLoad;
//    }
    

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
        {
            consolidatedStatus = status;
            if(provideOperation != null)
                provideOperation.getObject().changed(statusProperty);
        }
    }
    
    private class ProviderImpl extends DefaultProvider
    {
        @Override
        public void get(Provide operation, DOFRequest.Get request, Property property)
        {
            if(!property.getInterfaceID().equals(IID))
                return;
            if (property.getItemID() == statusItemId)
            {
                request.respond(new DOFUInt8((short)getStatus().ordinal()));
            } else
            {
                request.respond(new DOFNotSupportedException());
            }
        }

        @Override
        public void subscribe(Provide operation, Subscribe request, Property property, int minPeriod)
        {
            request.respond();
        }
    }
    
    private static final int statusItemId = 1;
    private static DOFType statusType = DOFUInt8.TYPE;
    private static final DOFInterfaceID IID = DOFInterfaceID.create("[1:{01}]");
    private static final DOFInterface DEF = new DOFInterface.Builder(IID).addProperty(statusItemId, false, true, statusType).build();
    private static final DOFInterface.Property statusProperty = DEF.getProperty(statusItemId);
}
