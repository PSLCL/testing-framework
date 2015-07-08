package com.pslcl.qa.platform;

/**
 * Defines the set of methods for reporting status information to JMX.  
 */
public interface RunnerServiceMBean {
	   
    /**
     * Analyzes all possible causes for error or warning statuses and returns the most severe status.  
     * @return the current status of the service
     */
    public short getStatus();
   
    /**
     * Determines the load percentage of the service. This is a value between 0.0 and 1.0, including 1.0 as a possible value.
     * @return the current load
     */
    public float getLoad();

}
