package com.pslcl.qa.runner.resource;

import com.pslcl.qa.runner.ArtifactNotFoundException;

/**
 * Represents a Machine Resource instance.
 */
public interface MachineInstance extends ResourceInstance {
    
    /** Place an artifact on a machine.
     * @param artifactName
     * @param artifactHash
    */
   void deploy( String artifactName, String artifactHash )
                throws ResourceNotFoundException, ArtifactNotFoundException;

   /** Connect a machine to a network.
    *
    * @param networkRef
    */
   void connect( String networkRef )  throws ResourceNotFoundException;

   /** Run a program artifact on a machine.
    *
    * @param componentName
    * @param artifactName
    * @param artifactHash
    * @param params
    * @return
    */
   boolean run( String componentName, String artifactName, String artifactHash, String params )
                throws ResourceNotFoundException, ArtifactNotFoundException;
   
}