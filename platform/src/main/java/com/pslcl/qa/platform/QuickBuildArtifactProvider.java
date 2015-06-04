package com.pslcl.qa.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class bridges a set of QuickBuild configurations to the test platform. It assumes a particular
 * structure of configurations, and further assumes that artifacts follow a particular naming scheme.
 * The following assumptions are made:
 *
 * The system downloads and caches artifacts in a directory, with each artifact expanded by its hash and
 * a single combined index file. All information is synchronized, and will be recreated if deleted.
 */
public class QuickBuildArtifactProvider implements ArtifactProvider {
    /**
     * This class represents a component, which is just a wrapped string.
     */
    private static class Component {
        private String component;

        public Component( String component ) {
            this.component = component;
        }

        public String toString() {
            return component;
        }

        public int hashCode() {
            return component.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o.getClass() != Component.class )
                return false;

            Component other = (Component) o;
            return this.component.compareTo( other.component ) == 0;
        }
    }

    /**
     * This class represents a version, which is just a wrapped string.
     */
    private static class Version {
        private String version;

        public Version( String version) {
            this.version = version;
        }

        public String toString() {
            return version;
        }

        public int hashCode() {
            return version.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o.getClass() != Version.class )
                return false;

            Version other = (Version) o;
            return this.version.compareTo( other.version ) == 0;
        }
    }

    /**
     * This class represents a platform, which is just a wrapped string.
     */
    private static class Platform {
        private String platform;

        public Platform( String platform) {
            this.platform = platform;
        }

        public String toString() {
            return platform;
        }

        public int hashCode() {
            return platform.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o.getClass() != Platform.class )
                return false;

            Platform other = (Platform) o;
            return this.platform.compareTo( other.platform ) == 0;
        }
    }

    /**
     * This class represents a internal build, which is just a wrapped string.
     */
    private static class InternalBuild {
        private String internal_build;

        public InternalBuild(String internal_build) {
            this.internal_build = internal_build;
        }

        public String toString() {
            return internal_build;
        }

        public int hashCode() {
            return internal_build.hashCode();
        }

        public boolean equals( Object o ) {
            if ( o.getClass() != InternalBuild.class )
                return false;

            InternalBuild other = (InternalBuild) o;
            return this.internal_build.compareTo( other.internal_build) == 0;
        }
    }

    /**
     * This class maintains an index of artifacts. The index is stored in a text file that is part of the
     * broader cache of artifact information. The index is used to respond to all external queries
     * of the ArtifactProvider interface - only the file contents are not in the index.
     */
    private static class ArtifactIndex {
        /**
         * This class represents a single artifact, identified by its component, version, platform, and name.
         * Additionally the archive is tracked for easier deletion when an entire archive is deleted.
         * The hash is part of the index, and is also used to provide the content.
         */
        private static class Artifact {
            private Component component;
            private Version version;
            private Platform platform;
            private InternalBuild internal_build;
            private String archive;
            private String name;
            private Hash hash;
            private boolean cached = true;

            public Artifact( Component component, Version version, Platform platform, InternalBuild internal_build, String archive, String name, Hash hash ) {
                this.component = component;
                this.version = version;
                this.platform = platform;
                this.internal_build = internal_build;
                this.archive = archive;
                this.name = name;
                this.hash = hash;
            }
        }

        /**
         * The directory of the cache, which is the location of the index file.
         */
        private File cache;

        /**
         * The stored list of platforms that are checked in each archive filename.
         */
        private List<String> platforms = new ArrayList<String>();

        /**
         * The index of all artifacts. This is a nested set of Maps that correspond to the different
         * levels that the ArtifactProvider interface defines, and makes it easier to answer questions
         * based on the keyset of each level.
         * The levels are: component -> version -> platform -> internal_build -> name -> artifact
         */
        private Map<Component,Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>>> artifactIndex = new HashMap<Component, Map<Version, Map<Platform, Map<InternalBuild, Map<String,Artifact>>>>>();

        /**
         * An index of all artifacts (by hash) that have been found. This is used to prune the artifact cache.
         * This is modified in multiple threads and must be synchronized.
         */
        private final List<String> contentIndex = new ArrayList<String>();

        /**
         * A flag that indicates whether the artifactIndex has been modified and needs to write
         * written back to storage.
         */
        private boolean indexModified = false;

        /**
         * Construct an ArtifactIndex, specifying the cache location and the supported platforms. The 'src' and 'dist'
         * platforms are managed internally.
         * @param cache The directory where the index file is stored.
         * @param platforms A list of platforms, used to identify platforms from archive filenames.
         */
        public ArtifactIndex( File cache, List<String> platforms ) {
            this.cache = cache;
            this.platforms = platforms;

            if ( ! cache.isDirectory() )
                //noinspection ResultOfMethodCallIgnored
                cache.mkdirs();

            load();
        }

        /**
         * This class holds deferred archives that need to be merged in later with other loaded artifacts.
         */
        private static class DeferredArchive {
            private File archive;
            private InternalBuild internal_build;
            private String merge;

            public DeferredArchive( File archive, InternalBuild internal_build, String merge ) {
                this.archive = archive;
                this.internal_build = internal_build;
                this.merge = merge;
            }
        }

        private List<DeferredArchive> toMerge = new ArrayList<DeferredArchive>();
        private List<DeferredArchive> toGenerate = new ArrayList<DeferredArchive>();

        /**
         * Merge all archives that were flagged into the other loaded components.
         * @throws Exception Thrown if the archive cannot be added.
         */
        public void merge() throws Exception {
            for ( DeferredArchive da : toMerge ) {
                LineIterator lineIterator = new LineIterator( new StringReader( da.merge ) );
                while ( lineIterator.hasNext() ) {
                    String line = lineIterator.next();
                    String[] fields = line.split( "," );
                    Component component = new Component( fields[0] );
                    VersionRange range = new VersionRange( fields[1] );

                    if ( ! artifactIndex.containsKey( component ) )
                        continue;

                    for ( Version v : artifactIndex.get( component ).keySet() ) {
                        if ( range.contains( v.version ) )
                            add( component, v, da.internal_build, da.archive, null, false );
                    }
                }
            }
        }

        ExecutorService adder = Executors.newFixedThreadPool( 2 );
        List<BuildAdder> added = new ArrayList<BuildAdder>();

        private class BuildAdder implements Runnable {
            Component component;
            Version version;
            InternalBuild internal_build;
            File archive;
            Exception thrown = null;

            BuildAdder( Component component, Version version, InternalBuild internal_build, File archive ) {
                this.component = component;
                this.version = version;
                this.internal_build = internal_build;
                this.archive = archive;
            }

            public void run() {
                TarArchiveInputStream ti = null;
                try {
                    /* Source archives are not expanded, and become artifacts themselves. */
                    if (archive.getName().endsWith(".src.tar.gz")) {
                        /* Create the archive/artifact cache entry */
                        Hash h = Hash.fromContent(archive);
                        synchronized ( contentIndex ) {
                            contentIndex.add(h.toString());
                        }
                        File src_cache = new File(cache, h.toString());
                        if (!src_cache.exists()) {
                            IOUtils.copy(new FileInputStream(archive), new FileOutputStream(src_cache));
                        }

                        /* Create the artifact entry. */
                        Artifact a = new Artifact(component, version, new Platform("src"), internal_build, archive.getName(), archive.getName(), h);
                        add(a, false);
                        return;
                    }

                    /* Determine the platform based on the list of platform types. Default is "dist". */
                    String platform = "dist";
                    for (String P : platforms) {
                        if (archive.getName().contains(P)) {
                            platform = P;
                            break;
                        }
                    }

                    /* Uncompress and unarchive the file, creating entries for each artifact found inside. */
                    InputStream fis = new FileInputStream(archive);
                    InputStream is = new GzipCompressorInputStream(fis);
                    ti = new TarArchiveInputStream(is);
                    TarArchiveEntry entry;
                    while ((entry = ti.getNextTarEntry()) != null) {
                        if (entry.isDirectory())
                            continue;

                        String artifact = entry.getName();
                        /* Standardize on no leading dot. */
                        if (artifact.startsWith("./"))
                            artifact = artifact.substring(2);

                        /* Extract the contents to a memory cache, determine the hash, and see if the file is already cached. */
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        final int BUFFER_MAX_SIZE = 8192;
                        byte[] buffer = new byte[BUFFER_MAX_SIZE];

                        int count;
                        while ((count = ti.read(buffer, 0, BUFFER_MAX_SIZE)) != -1) {
                            bos.write(buffer, 0, count);
                        }

                        bos.close();

                        Hash h = Hash.fromContent(new ByteArrayInputStream(bos.toByteArray()));
                        synchronized ( contentIndex ) {
                            contentIndex.add(h.toString());
                        }

                        /* Check the cache and add the file if not found. */
                        File f = new File(cache, h.toString());
                        if (!f.exists()) {
                            FileOutputStream fos = new FileOutputStream(f);
                            bos.writeTo(fos);
                            fos.close();
                        }

                        /* Define the artifact and add it to the index. */
                        Artifact a = new Artifact(component, version, new Platform(platform), internal_build, archive.getName(), artifact, h);
                        add(a, false);
                    }
                }
                catch ( Exception e ) {
                    thrown = e;
                }
                finally {
                    if ( ti != null ) try {
                        ti.close();
                    }
                    catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }

        /**
         * Add an archive to the index and cache all of its artifacts.
         * @param component The component of the archive.
         * @param version The version of the archive.
         * @param internal_build The internal_build of the archive.
         * @param archive A file containing the archive.
         * @param merge Versions that this archive should be merged with, or null if none. This will add the
         * archive to each matching component/version instead of those passed, and after everything has been loaded.
         * @param generator True if the archive represents a generator (which does not contain artifacts).
         * @throws Exception Thrown if there are issues with the files in the cache.
         */
        public void add( Component component, Version version, InternalBuild internal_build, File archive, String merge, boolean generator ) throws Exception {
            if ( ! archive.exists() || ! archive.isFile() || ! archive.canRead() )
                throw new Exception( String.format( "ERROR: Archive %s is not valid - must exist as a readable file.", archive.getAbsolutePath() ) );

            if ( merge != null ) {
                toMerge.add( new DeferredArchive( archive, internal_build, merge ) );
                return;
            }

            if ( generator ) {
                toGenerate.add( new DeferredArchive( archive, internal_build, "" ) );
                return;
            }

            BuildAdder task = new BuildAdder( component, version, internal_build, archive );
            adder.execute( task );
            added.add( task );
        }

        public void waitForArtifacts() throws Exception {
            adder.shutdown();
            while ( ! adder.isTerminated() ) {
                try {
                    Thread.sleep(500);
                }
                catch ( Exception e ) {
                    // Ignore.
                }
            }

            // Check for any exceptions
            for ( BuildAdder b : added ) {
                if ( b.thrown != null )
                    throw b.thrown;
            }
        }

        /**
         * Add an artifact to the index. This is called from multiple threads and so the entire routine
         * is synchronized rather than synchronizing each data structure.
         * @param a The artifact to add.
         * @param cached True if the artifact is from a cache, False if verified from an archive.
         * @throws Exception Thrown if the artifact being added doesn't match the index.
         */
        private synchronized void add( Artifact a, boolean cached ) throws Exception {
            /* Check each level of the index, creating it if needed. */
            Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>> versionMap;
            if ( artifactIndex.containsKey( a.component ) )
                versionMap = artifactIndex.get( a.component );
            else {
                versionMap = new HashMap<Version, Map<Platform, Map<InternalBuild, Map<String,Artifact>>>>();
                artifactIndex.put( a.component, versionMap );
            }

            Map<Platform,Map<InternalBuild, Map<String,Artifact>>> platformMap;
            if ( versionMap.containsKey( a.version ) )
                platformMap = versionMap.get( a.version );
            else {
                platformMap = new HashMap<Platform, Map<InternalBuild, Map<String,Artifact>>>();
                versionMap.put( a.version, platformMap );
            }

            Map<InternalBuild, Map<String, Artifact>> internal_buildMap;
            if ( platformMap.containsKey( a.platform ) )
                internal_buildMap = platformMap.get( a.platform );
            else {
                internal_buildMap = new HashMap<InternalBuild, Map<String, Artifact>>();
                platformMap.put( a.platform, internal_buildMap );
            }

            Map<String,Artifact> nameMap;
            if ( internal_buildMap.containsKey( a.internal_build) )
                nameMap = internal_buildMap.get( a.internal_build);
            else {
                nameMap = new HashMap<String, Artifact>();
                internal_buildMap.put( a.internal_build, nameMap );
            }

            if ( ! nameMap.containsKey( a.name) ) {
                nameMap.put( a.name, a );
                a.cached = cached;
                indexModified = true;
                return;
            }

            /* The artifact was already in the map, so we just compare. Most of the fields must be the
             * same or the map would not have ended up at the same location.
             */
            Artifact existing = nameMap.get( a.name );
            if ( existing.archive.compareTo( a.archive ) != 0 || existing.hash.compareTo( a.hash ) != 0 ) {
                if ( existing.cached == cached )
                    throw new Exception( String.format( "ERROR: Index doesn't match existing archive, archive %s, artifact %s, existing archive %s, added archive %s, existing hash %s, added hash %s, added component %s, added version %s, added platform %s, added internal build %s.",
                            a.archive, a.name, existing.archive, a.archive, existing.hash.toString(), a.hash.toString(), a.component.component, a.version.version, a.platform.platform, a.internal_build.internal_build ) );

                nameMap.put( a.name, a );
                existing = a;
            }

            existing.cached = cached;
        }

        /**
         * Load the cached index file, creating artifacts in the index. These are not marked as synchronized,
         * so the pruning call will remove artifacts that no longer exist in the archives.
         */
        private void load() {
            File file = new File( cache, "index" );
            try {
                LineIterator it = FileUtils.lineIterator( file, "UTF-8" );
                try {
                    while ( it.hasNext() ) {
                        String line = it.nextLine();
                        String[] fields = line.split( ":" );
                        if ( fields.length != 7 )
                            throw new Exception( "Illegal line " + line + " in index cache." );

                        ArtifactIndex.Artifact a = new Artifact( new Component( fields[0] ), new Version( fields[1] ), new Platform( fields[2] ), new InternalBuild( fields[3] ), fields[4], fields[5], new Hash( DatatypeConverter.parseHexBinary( fields[6] ) ) );
                        add( a, true );
                    }
                }
                finally {
                    it.close();
                }
            }
            catch ( Exception e ) {
                // Ignore whatever we have loaded.
                artifactIndex.clear();
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        /**
         * Save the file index, overwriting any previous version. This is only required if some change is made
         * to the index.
         * @throws Exception Thrown if there is a file error writing the index.
         */
        public void save() throws Exception {
            prune();

            if ( ! indexModified )
                return;

            File file = new File( cache, "index" );
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            OutputStream os = new FileOutputStream( file );
            OutputStreamWriter osw = new OutputStreamWriter( os, "UTF-8" );

            for ( Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>> components : artifactIndex.values() ) {
                for ( Map<Platform,Map<InternalBuild,Map<String,Artifact>>> versions : components.values() ) {
                    for ( Map<InternalBuild,Map<String,Artifact>> platforms : versions.values() ) {
                        for ( Map<String,Artifact> internal_builds : platforms.values() ) {
                            for ( Artifact a : internal_builds.values() ) {
                                osw.write( a.component + ":" + a.version + ":" + a.platform + ":" + a.internal_build + ":" + a.archive + ":" + a.name + ":" + a.hash + "\n" );
                            }
                        }
                    }
                }
            }

            osw.close();
            os.close();
        }

        /**
         * Prune the two structures maintained by the index: the artifactIndex and the cache.
         */
        void prune() {
            /* Scan all the entries, remove those that are only from cache. */

            Iterator<Map.Entry<Component,Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>>>> componentMapIterator = artifactIndex.entrySet().iterator();
            while ( componentMapIterator.hasNext() ) {
                Map.Entry<Component,Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>>> componentMapEntry = componentMapIterator.next();
                Map<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>> versionMap = componentMapEntry.getValue();

                Iterator<Map.Entry<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>>> versionMapIterator = versionMap.entrySet().iterator();
                while ( versionMapIterator.hasNext() ) {
                    Map.Entry<Version,Map<Platform,Map<InternalBuild,Map<String,Artifact>>>> versionMapEntry = versionMapIterator.next();
                    Map<Platform,Map<InternalBuild,Map<String,Artifact>>> platformMap = versionMapEntry.getValue();

                    Iterator<Map.Entry<Platform,Map<InternalBuild,Map<String,Artifact>>>> platformMapIterator = platformMap.entrySet().iterator();
                    while ( platformMapIterator.hasNext() ) {
                        Map.Entry<Platform,Map<InternalBuild,Map<String,Artifact>>> platformMapEntry = platformMapIterator.next();
                        Map<InternalBuild,Map<String,Artifact>> internalBuildMap = platformMapEntry.getValue();

                        Iterator<Map.Entry<InternalBuild,Map<String,Artifact>>> internalBuildMapIterator = internalBuildMap.entrySet().iterator();
                        while ( internalBuildMapIterator.hasNext() ) {
                            Map.Entry<InternalBuild,Map<String,Artifact>> internalBuildMapEntry = internalBuildMapIterator.next();
                            Map<String,Artifact> nameMap = internalBuildMapEntry.getValue();

                            List<String> to_remove = new ArrayList<String>();
                            Iterator<Map.Entry<String,Artifact>> nameMapIterator = nameMap.entrySet().iterator();
                            while ( nameMapIterator.hasNext() ) {
                                Map.Entry<String,Artifact> nameMapEntry = nameMapIterator.next();
                                Artifact a = nameMapEntry.getValue();

                                if ( a.cached ) {
                                    to_remove.add( nameMapEntry.getKey() );
                                    indexModified = true;
                                }
                            }

                            for ( String N : to_remove )
                                nameMap.remove( N );
                            
                            if ( nameMap.size() == 0 )
                                internalBuildMapIterator.remove();
                        }

                        if ( internalBuildMap.size() == 0 )
                            platformMapIterator.remove();
                    }

                    if ( platformMap.size() == 0 )
                        versionMapIterator.remove();
                }

                if ( versionMap.size() == 0 )
                    componentMapIterator.remove();
            }

            /* Check the entire cache, remove files that are not found. */
            String[] files = cache.list();
            for ( String file : files ) {
                synchronized ( contentIndex ) {
                    if (!contentIndex.contains(file)) {
                        File to_delete = new File(cache, file);
                        //noinspection ResultOfMethodCallIgnored
                        to_delete.delete();
                    }
                }
            }
        }
    }

    /**
     * This class represents a Build in the QuickBuild system. Each build has a unique ID and is
     * associated with a component and version. Certain builds are identified as applying to other builds through merging, this is tracked
     * from the "version_merge" variable.
     */
    private static class Build {
        private int id;
        private Component component;
        private Version version = new Version( "" );
        private InternalBuild internal_build;
        private String merge = null;
        private boolean generator = false;

        public Build( int id, Component component, InternalBuild internal_build) {
            this.id = id;
            this.component = component;
            this.internal_build = internal_build;
        }
    }

    /**
     * This class represents a Configuration in the QuickBuild system. The QuickBuild API does not provide
     * full path information for configurations, and so the entire structure must be queried and then
     * the paths determined by building the hierarchy. Each configuration has an ID, a parent, and other
     * information.
     */
    private static class Configuration {
        private int id;
        private int parent;
        private String name;
        private String path = "";
        private String component = "";
        private String internal_build = "";
        List<Configuration> children = new ArrayList<Configuration>();
        List<Build> builds = new ArrayList<Build>();

        public Configuration( int id, int parent, String name ) {
            this.id = id;
            this.parent = parent;
            this.name = name;
        }

        /**
         * Set the full path of the configuration. Based on knowledge of the QuickBuild structure,
         * the component name is determined as being part of the path.
         * @param path The full path to this component.
         */
        private void setPath( String path ) {
            this.path = path;

            // Determine our component name.
            if ( path.startsWith( "root/release/" ) )
                component = name;
            else if ( path.startsWith( "root/internal/release/" ) )
                component = name;                // This is for backward compatibility.
            else if ( path.startsWith( "root/internal/stage/" ) )
                component = name;                // This is for backward compatibility.
            else if ( path.startsWith( "root/internal/qa-new/" ) )
                component = name;
            else if ( path.startsWith( "root/internal/testing/" ) ) {
                // This is for backward compatibility. We ignore the last part (contains version)
                String C = path;
                C = C.replace( "root/internal/testing/", "" );
                String[] N = C.split( "/" );
                component = N[0];
            }

            int index = component.lastIndexOf( "~" );
            if ( index > 0 ) {
                internal_build = component.substring( index+1 );
                component = component.substring( 0, index );
            }
        }

        /**
         * Given the parent path, set the current path and the path of all children.
         * @param parent The path of the parent, or null if there is no parent.
         */
        public void setPaths( String parent ) {
            if ( parent != null )
                setPath( parent + "/" + name );
            else
                setPath( name );

            for ( Configuration child : children )
                child.setPaths( path );
        }

        /**
         * Add a child configuration.
         * @param child The configuration to add.
         */
        public void add( Configuration child ) {
            children.add( child );
        }

        /**
         * Add a child build of this component.
         * @param build The build to add.
         */
        public void add( Build build ) {
            builds.add( build );
        }
    }

    private String endpoint = null;
    private File cache = null;
    private List<String> platforms = new ArrayList<String>();
    private Map<Integer,Configuration> configurationMap = new HashMap<Integer, Configuration>();
    private ArtifactIndex artifactIndex = null;

    /**
     * Read information on every configuration in the system.
     */
    private void getConfigurations() {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet( endpoint + "/rest/configurations?recursive=true" );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64( auth.getBytes( Charset.forName( "US-ASCII" ) ) );
            String authHeader = "Basic " + new String( encodedAuth );
            request.setHeader( HttpHeaders.AUTHORIZATION, authHeader );

            HttpResponse response = client.execute( request );
            String responseString = new BasicResponseHandler().handleResponse( response );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource( new StringReader( responseString ) );
            Document doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName( "com.pmease.quickbuild.model.Configuration" );
            for ( int c = 0; c < nList.getLength(); c++ ) {
                Node nNode = nList.item( c );

                if ( nNode.getNodeType() != Node.ELEMENT_NODE )
                    continue;

                Element eElement = (Element) nNode;
                int id = Integer.parseInt( eElement.getElementsByTagName( "id" ).item( 0 ).getTextContent() );
                String name = eElement.getElementsByTagName( "name" ).item( 0 ).getTextContent();

                /* The root does not have a parent. */
                int parent = 0;
                NodeList parents = eElement.getElementsByTagName( "parent" );
                if ( parents.getLength() > 0 )
                    parent = Integer.parseInt( parents.item( 0 ).getTextContent() );

                configurationMap.put( id, new Configuration( id, parent, name ) );
            }

            /* All of the configurations are read, so build the hierarchy. */
            for ( Configuration c : configurationMap.values() ) {
                Configuration p = configurationMap.get( c.parent );
                if ( p != null )
                    p.add( c );
            }

            /* Obtain the root node (always 1) and update paths. */
            Configuration root = configurationMap.get( 1 );
            root.setPaths( null );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not read configurations, " + e.getMessage() );
        }
    }

    private void download( Build build, String name, Date lastModified ) {
        try {
            System.err.println( "Downloading: " + name );
            DefaultHttpClient client = new DefaultHttpClient();
            // TODO: Get the endpoint here.
            HttpGet request = new HttpGet( endpoint + String.format( "/download/%d/artifacts/%s", build.id, name ) );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64( auth.getBytes( Charset.forName( "US-ASCII" ) ) );
            String authHeader = "Basic " + new String( encodedAuth );
            request.setHeader( HttpHeaders.AUTHORIZATION, authHeader );

            HttpResponse response = client.execute( request );
            File buildDir = new File( cache, Integer.toString( build.id ) );
            File target = new File( buildDir, name );
            if ( target.exists() )
                //noinspection ResultOfMethodCallIgnored
                target.delete();

            //noinspection ResultOfMethodCallIgnored
            target.getParentFile().mkdirs();
            //target.createNewFile();
            OutputStream os = new FileOutputStream( target );
            IOUtils.copy( response.getEntity().getContent(), os );
            os.close();
            //noinspection ResultOfMethodCallIgnored
            target.setLastModified(lastModified.getTime());

            artifactIndex.add( build.component, build.version, build.internal_build, target, build.merge, build.generator );
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }
    }

    /**
     * The list of all found archives, used to prune archives that are no longer
     * present in the QuickBuild server.
     */
    List<String> foundArchives = new ArrayList<String>();

    /**
     * Declare to the provider that an archive file with the specified properties exists. If
     * the file matches the information in the archive cache then notify the cache that the
     * contents should be validated against the file that already exists. Otherwise, the file
     * is downloaded to the archive cache and the index updated.
     * @param build The build that contains the archive.
     * @param name The name of the archive.
     * @param size The size of the archive (bytes).
     * @param lastModified The last modified date.
     * @throws Exception Thrown if the component cannot be checked.
     */
    private void declareArchive( Build build, String name, int size, Date lastModified ) throws Exception {
        foundArchives.add( build.id + "-" + name );

        File f = new File( cache, Integer.toString( build.id ) );
        File p = new File( f, name );
        if ( p.length() == size && p.lastModified() == lastModified.getTime() ) {
            artifactIndex.add( build.component, build.version, build.internal_build, p, build.merge, build.generator );
            return;
        }

        /* Download the archive, which if successful will add files to the index. */
        download( build, name, lastModified );
    }

    /**
     * Prune the cache of archives, deleting those that are not currently found in QuickBuild.
     */
    private void pruneArchives() {
        for ( File build : cache.listFiles() ) {
            if ( build.isDirectory() ) {
                for ( File archive : build.listFiles() ) {
                    String name = build.getName() + "-" + archive.getName();
                    if ( foundArchives.contains( name ) )
                        continue;

                    //noinspection ResultOfMethodCallIgnored
                    archive.delete();
                }

                if ( build.list().length == 0 )
                    //noinspection ResultOfMethodCallIgnored
                    build.delete();
            }
        }
    }

    private void getArchives( Build build ) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet( endpoint + String.format( "/rest/files?build_id=%d", build.id ) );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            HttpResponse response = client.execute(request);
            String responseString = new BasicResponseHandler().handleResponse( response );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader( responseString ));
            Document doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName( "com.pmease.quickbuild.FileInfo" );
            boolean has_artifacts = false;
            for ( int f = 0; f < nList.getLength(); f++ ) {
                Node nNode = nList.item( f );

                if ( nNode.getNodeType() != Node.ELEMENT_NODE )
                    continue;

                Element eElement = (Element) nNode;

                String name = eElement.getElementsByTagName( "name" ).item( 0 ).getTextContent();
                if ( name.compareTo( "artifacts" ) == 0 ) {
                    has_artifacts = true;
                    break;
                }
            }

            if ( ! has_artifacts )
                return;

            request = new HttpGet( endpoint + String.format( "/rest/files?build_id=%d&path=artifacts", build.id ) );
            request.setHeader( HttpHeaders.AUTHORIZATION, authHeader );

            response = client.execute( request );
            responseString = new BasicResponseHandler().handleResponse( response );

            is = new InputSource( new StringReader( responseString ) );
            doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            nList = doc.getElementsByTagName( "com.pmease.quickbuild.FileInfo" );
            for ( int f = 0; f < nList.getLength(); f++ ) {
                Node nNode = nList.item( f );

                if ( nNode.getNodeType() != Node.ELEMENT_NODE )
                    continue;

                Element eElement = (Element) nNode;

                String name = eElement.getElementsByTagName( "name" ).item( 0 ).getTextContent();
                if ( ! name.endsWith( ".tar.gz" ) )
                    continue;

                int size = Integer.parseInt( eElement.getElementsByTagName( "size" ).item( 0 ).getTextContent() );
                Date lastModified = new Date( Long.parseLong(eElement.getElementsByTagName("lastModified").item(0).getTextContent()) );

                declareArchive( build, name, size, lastModified );
            }

            //TODO: Determine if we are going to assume that build artifacts don't change.
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }
    }

    /**
     * Get the builds associated with a specified configuration.
     * @param configuration The configuration to get the builds for.
     * @param latest_only True indicates that only the latest successful build should be used, false indicates to use all builds.
     *                    Latest means the largest build number.
     */
    private void getBuilds( Configuration configuration, boolean latest_only ) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet( endpoint + String.format( "/rest/builds?configuration_id=%d&recursive=false&status=SUCCESSFUL&count=50", configuration.id ) );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            HttpResponse response = client.execute(request);
            String responseString = new BasicResponseHandler().handleResponse( response );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader( responseString ));
            Document doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName( "com.pmease.quickbuild.model.Build" );

            List<Build> found = new ArrayList<Build>();
            int id_of_latest_build = 0;
            for ( int i = 0; i < nList.getLength(); i++ ) {
                Node nNode = nList.item( i );

                if ( nNode.getNodeType() != Node.ELEMENT_NODE )
                    continue;

                Element eElement = (Element) nNode;

                int id = Integer.parseInt(eElement.getElementsByTagName("id").item(0).getTextContent());
                if ( id > id_of_latest_build )
                    id_of_latest_build = id;

                //TODO: If configuration.component is not set, then get it from the version

                // Traverse variables
                Node varsRoot = eElement.getElementsByTagName( "secretAwareVariableValues" ).item( 0 );
                Element varsRootElement = (Element) varsRoot;

                String version_dotted = "";
                String force_version = "";
                String version_merge = null;
                boolean is_generator = false;

                NodeList varsList = varsRootElement.getElementsByTagName( "entry" );
                for ( int v = 0; v < varsList.getLength(); v++ ) {
                    Node varRoot = varsList.item( v );
                    Element varRootElem = (Element) varRoot;

                    NodeList varInfo = varRootElem.getElementsByTagName( "string" );
                    String variable = varInfo.item( 0 ).getTextContent();
                    String value = "";
                    if ( varInfo.getLength() > 1 ) {
                        value = varInfo.item( 1 ).getTextContent().trim();
                    }

                    if ( variable.compareTo( "emit_dotted_version" ) == 0 )
                        version_dotted = value;
                    if ( variable.compareTo( "force_version" ) == 0 )
                        force_version = value;
                    if ( variable.compareTo( "version_merge" ) == 0 )
                        version_merge = value;
                    if ( variable.compareTo( "qa_generator" ) == 0 && value.compareToIgnoreCase( "true" ) == 0 )
                        is_generator = true;
                }

                if ( version_dotted.length() == 0 )
                    version_dotted = force_version;

                Build b = new Build( id, new Component( configuration.component ), new InternalBuild( configuration.internal_build) );
                b.version = new Version( version_dotted );
                b.merge = version_merge;
                b.generator = is_generator;
                found.add( b );
            }

            for ( Build b : found ) {
                if ( latest_only && b.id != id_of_latest_build )
                    continue;

                configuration.add( b );
                getArchives( b );
            }
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }
    }

    /**
     * Retrieve build information for all interesting configuration.
     * @throws Exception Thrown if there are errors getting or processing the builds.
     */
    private void getBuilds() throws Exception {
        for ( Configuration c : configurationMap.values() ) {
            boolean interesting = false;
            boolean latest_only = false;

            if ( c.path.startsWith( "root/internal/qa-new/" ) ) {
                interesting = true;
                latest_only = true;
            }
            else if ( c.path.startsWith( "root/internal/release/" ) )
                interesting = true;
            else if ( c.path.startsWith( "root/release/" ) )
                interesting = true;
            else if ( c.path.startsWith( "root/internal/testing/" ) ) {
                interesting = true;
                latest_only = true;
            }
            else if ( c.path.startsWith( "root/internal/stage/" ) )
                interesting = true;

            if ( interesting )
                getBuilds( c, latest_only );
        }
    }

    /**
     * Retrieve the platforms for a particular build agent.
     * @param agent The string definition (per QuickBuild) of the agent.
     */
    private void getPlatforms( String agent ) {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet( endpoint + String.format( "/rest/user_attributes/%s", agent ) );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            HttpResponse response = client.execute(request);
            String responseString = new BasicResponseHandler().handleResponse( response );
            //System.out.println( responseString );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader( responseString ));
            Document doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName( "entry" );
            for ( int temp = 0; temp < nList.getLength(); temp++ ) {

                Node nNode = nList.item(temp);

                if ( nNode.getNodeType() == Node.ELEMENT_NODE ) {

                    Element eElement = (Element) nNode;

                    NodeList varInfo = eElement.getElementsByTagName("string");
                    String variable = varInfo.item( 0 ).getTextContent();
                    String value = "";
                    if ( varInfo.getLength() > 1 ) {
                        value = varInfo.item( 1 ).getTextContent().trim();
                    }

                    if ( variable.compareTo( "buildvm_platforms" ) == 0 ) {
                        String[] node_platforms = value.split(",");
                        for ( String P : node_platforms )
                            if ( ! platforms.contains( P ) )
                                platforms.add( P );
                    }
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }
    }

    /**
     * Retrieve all platforms in the build system by iterating each active agent.
     */
    private void getPlatforms() {
        try {
            Class.forName("org.apache.commons.logging.LogFactory"); // not required at compile time, but required for DefaultHttpClient() jars 4.3.2 and 4.3.5 to use at run time 
            DefaultHttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet( endpoint + "/rest/buildagents/active" );
            String auth = "qauser" + ":" + "qauser";
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
            String authHeader = "Basic " + new String(encodedAuth);
            request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);

            HttpResponse response = client.execute(request);
            String responseString = new BasicResponseHandler().handleResponse( response );
            //System.out.println( responseString );

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader( responseString ));
            Document doc = dBuilder.parse( is );
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName( "com.pmease.quickbuild.grid.GridNode" );
            for ( int temp = 0; temp < nList.getLength(); temp++ ) {

                Node nNode = nList.item(temp);

                if ( nNode.getNodeType() == Node.ELEMENT_NODE ) {

                    Element eElement = (Element) nNode;

                    String host = eElement.getElementsByTagName("hostName").item(0).getTextContent();
                    String port = eElement.getElementsByTagName("port").item(0).getTextContent();

                    getPlatforms( host + ":" + port );
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }
    }

    /**
     * All work is done in the init() call.
     */
    public QuickBuildArtifactProvider(String endpoint) {
    	this.endpoint = endpoint;
    }

    /**
     * Prepare the provider to return information.
     * @throws Exception Thrown if the information cannot be obtained. Calling the routine later may
     * work.
     */
    public void init() throws Exception {
        /* Initialize all common variables. */
        platforms = new ArrayList<String>();
        configurationMap = new HashMap<Integer, Configuration>();
        foundArchives = new ArrayList<String>();
        String QB_CACHE = System.getenv("QB_CACHE");

        cache = new File( QB_CACHE );

        System.err.println( "QuickBuild provider init() started at " + new Date() );

        /* Platforms are required in order to avoid parsing each artifact archive. */
        System.err.println( "Getting platforms..." );
        getPlatforms();

        /* Create the artifact index. This will load the cache. */
        artifactIndex = new ArtifactIndex( cache, platforms );

        /* Configurations are required in order to understand path names. */
        System.err.println( "Getting configurations..." );
        getConfigurations();

        /* Get the builds for each configuration that is important to us. */
        System.err.println( "Getting builds..." );
        getBuilds();

        /* Builds that need to be merged are downloaded, but not added to the artifactList. This adds them.
         * Note that the artifactIndex must be pruned before it can be merged.
         */
        artifactIndex.prune();
        artifactIndex.merge();

        /* Wait for the artifactIndex to be complete. */
        System.err.println( "Waiting for artifactIndex..." );
        artifactIndex.waitForArtifacts();

        /* Check the artifact cache. */
        System.err.println( "Pruning archive cache..." );
        pruneArchives();

        /* Save the artifact index. If this is successful (or not required) then we are ready to respond to queries. */
        artifactIndex.save();

        System.err.println("QuickBuild provider init() completed at " + new Date());
    }

    /**
     * Close the artifact provider, allowing it to free resources. The same provider
     * may be used again if init() is called.
     */
    public void close() {
        System.err.println( new Date() );
    }

    /**
     * Return the components associated with a project.
     * @param project The project to search.
     * @return A set of components.
     */
    public Set<String> getComponents( String project ) {
        Set<String> result = new HashSet<String>();
        for ( Component c : artifactIndex.artifactIndex.keySet() )
            result.add( c.component );
        return result;
    }

    /**
     * Return the versions associated with a project and component.
     * @param project The project to search.
     * @param component The component to search.
     * @return A set of versions.
     */
    public Set<String> getVersions( String project, String component ) {
        if ( ! artifactIndex.artifactIndex.containsKey( new Component( component ) ) )
            return new HashSet<String>();

        Set<String> result = new HashSet<String>();
        for ( Version v : artifactIndex.artifactIndex.get( new Component( component ) ).keySet() )
            result.add( v.version );

        return result;
    }

    /**
     * Return the platforms associated with a project, component, and version.
     * @param project The project to search.
     * @param component The component to search.
     * @param version The version to search.
     * @return A set of platforms.
     */
    public Set<String> getPlatforms( String project, String component, String version ) {
        Component c = new Component( component );
        Version v = new Version( version );

        if ( ! artifactIndex.artifactIndex.containsKey( c ) )
            return new HashSet<String>();

        if ( ! artifactIndex.artifactIndex.get( c ).containsKey( v ) )
            return new HashSet<String>();

        Set<String> result = new HashSet<String>();
        for ( Platform p : artifactIndex.artifactIndex.get( c ).get( v ).keySet() )
            result.add( p.platform );

        return result;
    }

    /**
     * This class is a provider for content. It only retrieves the content if requested, so it delays the
     * creation of the objects until needed.
     */
    private static class ContentProvider implements Content {
        File cache;
        Hash hash;

        public ContentProvider( File cache, Hash hash ) {
            this.cache = cache;
            this.hash = hash;
        }

        public ContentProvider( File archive ) {
            this.cache = archive;
            this.hash = null;
        }

        /**
         * Return the content as a stream.
         * @return A stream containing the content.
         */
        public InputStream asStream() {
            try {
                if ( hash == null )
                    return new FileInputStream( cache );

                File b = new File( cache, hash.toString() );
                return new FileInputStream( b );
            }
            catch ( Exception e ) {
                return null;
            }
        }

        /**
         * Return the content as a byte array.
         * @return A byte array containing the content.
         */
        public byte[] asBytes() {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileInputStream fis;
                if ( hash == null )
                    fis = new FileInputStream( cache );
                else {
                    File b = new File(cache, hash.toString());
                    fis = new FileInputStream(b);
                }

                IOUtils.copy( fis, os );
                fis.close();
                os.close();

                return os.toByteArray();
            }
            catch ( Exception e ) {
                return null;
            }
        }
    }

    /**
     * Iterate over the artifacts associated with a project, component, version, and platform. The specified callback
     * is called for each artifact.
     * @param project The project to limit results to.
     * @param component The component to iterate.
     * @param version  The version to iterate.
     * @param platform The platform to iterate.
     * @param callback The callback that is called for each artifact.
     */
    public void iterateArtifacts( String project, String component, String version, String platform, ArtifactNotifier callback ) {
        Component c = new Component( component );
        Version v = new Version( version );
        Platform p = new Platform( platform );

        if ( ! artifactIndex.artifactIndex.containsKey( c ) )
            return;

        if ( ! artifactIndex.artifactIndex.get( c ).containsKey( v ) )
            return;

        if ( ! artifactIndex.artifactIndex.get( c ).get( v ).containsKey( p ) )
            return;

        for ( InternalBuild internal_build : artifactIndex.artifactIndex.get( c ).get( v ).get( p ).keySet() ) {
            for ( Map.Entry<String,ArtifactIndex.Artifact> entry : artifactIndex.artifactIndex.get( c ).get( v ).get( p ).get( internal_build ).entrySet() ) {
                ArtifactIndex.Artifact artifact = entry.getValue();
                callback.artifact( project, c.component, v.version, p.platform, internal_build.internal_build, entry.getKey(), artifact.hash, new ContentProvider( cache, artifact.hash ) );
            }
        }
    }

    public void iterateGenerators( GeneratorNotifier callback ) {
        for (ArtifactIndex.DeferredArchive da : artifactIndex.toGenerate ) {
            // For now, generators must be noarch.
            if ( da.archive.getName().endsWith( ".noarch.tar.gz" ) )
                callback.generator( da.archive.getName(), new ContentProvider( da.archive ));
        }
    }
}
