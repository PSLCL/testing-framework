package com.pslcl.qa.runner.config;

import java.util.List;

import org.opendof.core.oal.DOFObject;


/**
 * An application status tracking interface.
 * </p>
 * This interface is patterned after the ENC interface registry <code>DeviceStatus interface "[1:{01}]"</code>
 *  An <code>enum</code> declaring ok, warn and error states which correspond to the DeviceStatus property 
 *  values is provided. 
 *  </p>
 *  The implementer of this interface should able to handle the individual status of multiple named application
 *  subsystems.  It should also provide a consolidated status (highest level of concern) of all the 
 *  named subsystems. 
 *  </p>
 *  Typically a <code>org.emitdo.service.util.Service</code> implementation's System Level will 
 *  instantiate an instance of a given implementation of this interface as a singleton object 
 *  to the whole application and include its reference in the <code>Service<T></code>'s configuration 
 *  object, which in turn is typically made available to all subsystems of the application. 
 *  This provides a single source of status for the whole application where each subsystem of the application 
 *  can register it's individual status by name.
 *  </p>
 *  Because some implementations of this interface might require configuration information, the <code>Service<T></code> 
 *  custom configuration object is also declared and handed into the init method.   
 *  </p>
 *  The service utilities provides the <code>MemoryStatusTracker</code> implementation of this interface.
 * @see StatusTrackerProvider
 */
public interface StatusTracker
{
    /**
     * Set the named subsystem's status. 
     * </p>
     * if the name is new, it is added, otherwise the named subsystems status is updated to the given value.
     * @param name the subsystem to set the status for.  Must not be null.
     * @param status the status to be set. Must not be null.
     */
    public void setStatus(String name, Status status);
    
    /**
     * Remove the named subsystem from the <code>StatusTracker</code>'s state. 
     * @param name the subsystem to be removed.  Must not be null.
     * </p>if the given name does not exist, no action is taken.
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
     * </p>If there are no registered subsystems <code>Status.Ok</code>
     * is returned.
     */
    public Status getStatus();
    
    /**
     * Begin Providing the Status interface on the given <code>DOFObject</code>.
     * </p>
     * Start providing the ENC interface registry <code>DeviceStatus interface</code>
     *  "[1:{01}]".  The consolidated status of all reporting subsystems will be made 
     *  available with any changes to the consolidated status firing a notification to
     *  subscribers of the DeviceStatus interface. 
     * @param object the <code>DOFObject</code> to provide on.  Must not be null.
     */

    public void beginStatusProvider(DOFObject object);

    /**
     * End providing the <code>DeviceStatus</code> interface.
     * </p>No action taken if never started or already ended.
     */
    public void endStatusProvider();
    
    /**
     * Shutdown the StatusTracker and release all resources. 
     */
    public void destroy();
    
    /**
     * An enumeration of all possible Status states.
     * </p>The value is associated with a user determined severity where Ok < Warn < Error. 
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
        Error;
        
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
                default:
                    throw new IllegalArgumentException("ordinal " + ordinal + " does not map to a known Status");
            }
        }
    }
}
