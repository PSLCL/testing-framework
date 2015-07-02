package com.pslcl.qa.platform.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents a simple sink of artifacts.
 */
public class ArtifactSink implements ArtifactProvider.ArtifactNotifier {
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

    public void artifact( String project, String component, String version, String platform, String internal_build, String name, Hash hash, ArtifactProvider.Content content ) {
        entries.add( new Entry( platform, internal_build, name, hash, content ) );
    }
}
