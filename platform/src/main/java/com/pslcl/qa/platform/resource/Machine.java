package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.ArtifactNotFoundException;

/**
 * Represents a Machine Resource instance.
 */
public interface Machine extends Resource {
    
    /** Place an artifact on a machine.
    *
    * @param componentName
    * @param artifactName
    * @param artifactHash
    */
   void deploy( String componentName, String artifactName, String artifactHash )
                throws ResourceNotFoundException, ArtifactNotFoundException;

   /** Ask a person to follow instructions to inspect an artifact.
    *
    * @param instructionsHash
    * @param componentName
    * @param artifactName
    * @param artifactHash
    */
   void inspect( String instructionsHash, String componentName, String artifactName, String artifactHash )
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