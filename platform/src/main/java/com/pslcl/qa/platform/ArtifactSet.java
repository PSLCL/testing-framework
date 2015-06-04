package com.pslcl.qa.platform;

import java.util.Iterator;

/**
 * This class represents a set of artifacts that is represented as a single artifact. The
 * benefit is that the set will be transferred and represented as a single file, with the contents
 * archived and compressed.
 * The set can contain artifacts from several different versions.
 */
public class ArtifactSet extends Artifact {
    public ArtifactSet() {
        super( 0, "*", null, "*", "*", null );
    }

    public ArtifactSet( Artifact ... artifacts ) {
        super( 0, "*", null, "*", "*", null );
    }

    public void add( Artifact ... artifacts ) {
        //TODO: Implement
    }

    public void add( Iterator<Artifact> artifacts ) {
        //TODO: Implement
    }

    public void close() {
        //TODO: Implement
    }
}
