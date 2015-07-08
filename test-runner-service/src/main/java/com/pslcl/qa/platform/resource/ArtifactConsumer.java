package com.pslcl.qa.platform.resource;


/**
 *  A consumer of Artifacts.
 */
public interface ArtifactConsumer  {
	/**
    *
    * @param component
    * @param version
    * @param platform
    * @param name
    * @param artifactHash
    */
   void updateArtifact( String component, String version, String platform, String name, String artifactHash );

   /**
    * @param component
    * @param version
    * @param platform
    * @param name
    */
   void removeArtifact( String component, String version, String platform, String name );

   /**
    * @param component
    * @param version
    */
   void invalidateArtifacts( String component, String version );
}
