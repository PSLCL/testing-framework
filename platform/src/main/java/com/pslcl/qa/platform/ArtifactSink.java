package com.pslcl.qa.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents a simple sink of artifacts.
 */
class ArtifactSink implements ArtifactProvider.ArtifactNotifier {
    static class Entry {
        String name;
        Hash hash;
        String platform;
        String internal_build;
        ArtifactProvider.Content content;

        Entry( String platform, String internal_build, String name, Hash hash, ArtifactProvider.Content content ) {
            this.platform = platform;
            this.internal_build = internal_build;
            this.name = name;
            this.hash = hash;
            this.content = content;
        }
    }

    List<Entry> entries = Collections.synchronizedList( new ArrayList<Entry>() );

    ArtifactSink() {
    }

    public void artifact( String project, String component, String version, String platform, String internal_build, String name, Hash hash, ArtifactProvider.Content content ) {
        entries.add( new Entry( platform, internal_build, name, hash, content ) );
    }
}
