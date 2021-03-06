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

import java.util.List;


/**
 * An application status tracking interface.
 * <p>
 * This interface is patterned after the ENC interface registry <code>DeviceStatus interface "[1:{01}]"</code>
 *  An <code>enum</code> declaring ok, warn and error states which correspond to the DeviceStatus property 
 *  values is provided. 
 *  <p>
 *  The implementer of this interface should able to handle the individual status of multiple named application
 *  subsystems.  It should also provide a consolidated status (highest level of concern) of all the 
 *  named subsystems. 
 *  <p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  instantiate an instance of a given implementation of this interface as a singleton object 
 *  to the whole application and include its reference in the <code>Service</code>'s configuration 
 *  object, which in turn is typically made available to all subsystems of the application. 
 *  This provides a single source of status for the whole application where each subsystem of the application 
 *  can register it's individual status by name.
 *  <p>
 *  Because some implementations of this interface might require configuration information, the <code>Service</code> 
 *  custom configuration object is also declared and handed into the init method.   
 *  <p>
 *  The service utilities provides the <code>MemoryStatusTracker</code> implementation of this interface.
 * @see DtfStatusTracker
 */
public interface StatusTracker
{
    /**
     * Set the named subsystem's status. 
     * <p>
     * if the name is new, it is added, otherwise the named subsystems status is updated to the given value.
     * @param name the subsystem to set the status for.  Must not be null.
     * @param status the status to be set. Must not be null.
     */
    public void setStatus(String name, Status status);
    
    /**
     * Remove the named subsystem from the <code>StatusTracker</code>'s state. 
     * @param name the subsystem to be removed.  Must not be null.
     * <p>if the given name does not exist, no action is taken.
     */
    public void removeStatus(String name);
    
    /**
     * Return a list of currently known subsystem names. 
     * @return a list of all currently known subsystem names. Must not return null, but may return an empty list.
     */
    public List<String> getNames();
    
    /**
     * Return the status of the given name's subsystem.
     * @param name the subsystem name to return the status for.  Must not be null.
     * @return the status of the named subsystem.  Must not return null.
     * @throws IllegalArgumentException if the given name is unknown.
     */
    public Status getStatus(String name);
    
    /**
     * Return the consolidated status of all known subsystems.
     * @return the consolidated status.  Must not return null.
     * <p>If there are no registered subsystems <code>Status.Ok</code>
     * is returned.
     */
    public Status getStatus();
    
    /**
     * Begin Providing the Status interface on the given <code>DOFObject</code>.
     * <p>
     * Start providing the ENC interface registry <code>DeviceStatus interface</code>
     *  "[1:{01}]".  The consolidated status of all reporting subsystems will be made 
     *  available with any changes to the consolidated status firing a notification to
     *  subscribers of the DeviceStatus interface. 
     * @param object the <code>DOFObject</code> to provide on.  Must not be null.
     */

    /**
     * Shutdown the StatusTracker and release all resources. 
     */
    public void destroy();
    
    
    /**
     * Register a ResourceStatusListener
     * @param listener to be registered
     */
    public void registerResourceStatusListener(ResourceStatusListener listener);
    
    /**
     * Deregister a ResourceStatusListener
     * @param listener to be registered
     */
    public void deregisterResourceStatusListener(ResourceStatusListener listener);
    
    /**
     * Fire a resource status changed event
     * @param statusEvent event to signal.
     */
    public void fireResourceStatusChanged(ResourceStatusEvent statusEvent);
    
    /**
     * An enumeration of all possible Status states.
     * <p>The value is associated with a user determined severity where <code>Ok</code> less-than <code>Warn</code> less-than <code>Error</code>. 
     */
    public enum Status
    {
        /**
         * Typically reflects normal operation.
         */
        Ok, 
        
        /**
         * Typically reflects an abnormality occurred.
         */
        Warn,
        
        /**
         * Typically reflects a serious abnormality occurred.
         */
        Error,
        
        /**
         * Typically reflects a serious abnormality occurred of which a human should be notified.
         */
        Alert,
        
        /**
         * The subsystem is down and the status name is being removed.
         */
        Down;
        
        /**
         * Return the <code>Status</code> based on the given ordinal value.
         * @param ordinal the ordinal value of the <code>Status</code> to return.
         * @return the <code>Status</code> whose ordinal value is the given value.
         * @throws IllegalArgumentException if ordinal is less than 0 or greater than 2
         */
        public static Status valueOf(int ordinal)
        {
            switch(ordinal)
            {
                case 0:
                    return Ok;
                case 1:
                    return Warn;
                case 2:
                    return Error;
                case 3:
                    return Alert;
                case 4:
                    return Down;
                default:
                    throw new IllegalArgumentException("ordinal " + ordinal + " does not map to a known Status");
            }
        }
    }
}
