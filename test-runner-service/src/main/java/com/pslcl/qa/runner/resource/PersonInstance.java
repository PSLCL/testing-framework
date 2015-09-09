package com.pslcl.qa.runner.resource;

import com.pslcl.qa.runner.ArtifactNotFoundException;

/**
 * Represents a Person Resource instance.
 */
public interface PersonInstance extends ResourceInstance {

    /** Ask a person to follow instructions to inspect an artifact.
    *
    * @param instructionsHash
     * @param artifactName
     * @param artifactHash
    */
   void inspect( String instructionsHash, String artifactName, String artifactHash )
                 throws ResourceNotFoundException, ArtifactNotFoundException;

}