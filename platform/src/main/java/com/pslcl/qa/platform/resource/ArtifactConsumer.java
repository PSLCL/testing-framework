package com.pslcl.qa.platform.resource;

import com.pslcl.qa.platform.Hash;

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
    * @param hash
    */
   void updateArtifact( String component, String version, String platform, String name, Hash hash );

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
