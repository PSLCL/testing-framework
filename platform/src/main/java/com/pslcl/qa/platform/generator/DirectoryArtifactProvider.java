package com.pslcl.qa.platform.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.pslcl.qa.platform.Hash;

/**
 * This class provides artifacts from a file system. The structure of the file system is
 * component/version/platform/(artifacts). There are several special platforms as well, "noarch", "src", and "dist".
 */
public class DirectoryArtifactProvider implements ArtifactProvider {
    private File base;

    public DirectoryArtifactProvider( File base ) {
        this.base = base;
    }

    public void init() {
        if ( ! base.exists() || ! base.isDirectory() )
            throw new IllegalArgumentException( "base directory does not exist or is not a directory." );
    }

    public void close() {
    }

    public Set<String> getComponents( String project ) {
        return new HashSet<String>( Arrays.asList( base.list() ) );
    }

    public Set<String> getVersions( String project, String component ) {
        File C = new File( base, component );
        return new HashSet<String>( Arrays.asList( C.list() ) );
    }

    public Set<String> getPlatforms( String project, String component, String version ) {
        File C = new File( base, component );
        File V = new File( C, version );
        return new HashSet<String>( Arrays.asList( V.list() ) );
    }

    private static class ContentProvider implements Content {
        File file;

        public ContentProvider( File base, String project, String component, String version, String platform, String name ) {
            File C = new File( base, component );
            File V = new File( C, version );
            File P = new File( V, platform );
            file = new File( P, name );
        }

        public InputStream asStream() {
            try {
                return new FileInputStream( file );
            }
            catch ( Exception e ) {
                return null;
            }
        }

        public byte[] asBytes() {
            //TODO: Implement
            return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
    private void iterate( String project, String component, String version, String platform, String d, File b, ArtifactNotifier callback ) {
        for ( File f : b.listFiles() ) {
            String name = (d != null ? d + "/" : "") + f.getName();

            if ( f.isDirectory() )
                iterate( project, component, version, platform, name, f, callback );
            else {
                Hash hash = Hash.fromContent( f );
                // TODO: Add variant support.
                callback.artifact( project, component, version, platform, "", name, hash, new ContentProvider( base, project, component, version, platform, name ) );
                //System.err.println( "Declared artifact " + component + "/" + version + "/" + platform + "/" + name + " [" + hash  + "]");
            }
        }
    }

    public void iterateArtifacts( String project, String component, String version, String platform, ArtifactNotifier callback ) {
        File C = new File( base, component );
        File V = new File( C, version );
        File P = new File( V, platform );

        iterate( project, component, version, platform, null, P, callback );
    }

    public void iterateGenerators( GeneratorNotifier callback ) {
        //TODO: Add generators
    }
}
