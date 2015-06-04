package com.pslcl.qa.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {
    static boolean trace = false;

    /**
     * Holds the active test instance, or the test instance that is currently being created. This
     * is active between startTest()/completeTest() calls.
     */
    private TestInstance activeTestInstance = null;
    
    /**
     * Holds all test instances created by the generator between creation and the call to close().
     */
    private List<TestInstance> allTestInstances = new ArrayList<TestInstance>();

    /**
     * This map contains all created content defined by this generator. Content is always
     * shared across tests, and is identified by the unique hash of the content itself.
     */
    Map<Hash,Content> addedContent = new HashMap<Hash,Content>();

    /**
     * The core that will be used to interact with the database and to get other common resources.
     */
    Core core;
    
    /**
     * The primary key of the test that the generator will generate instances of.
     */
    private long pk_test;

    /**
     * Create a generator, which in turn can be used to generate test instances.
     * @param pk_test The primary key of the test that any generated test instances are related to.
     */
    public Generator( long pk_test ) {
        this.pk_test = pk_test;
        core = new Core( pk_test );
    }

    /**
     * Declare an artifact in the platform. It is uniquely identified by a UUID, which defines the artifact
     * globally throughout the platform in the context of a single running test. Multiple tests running at
     * the same time receive different artifacts even if the identifier is the same.
     * @return An artifact.
     */
    Artifact declareArtifact() {
        return null;
    }

    /**
     * Create content that can be used in a test.
     * @param content
     * @return
     */
    public Content createContent( String content ) {
        Hash h = Hash.fromContent( content );
        Content c;
        if ( ! addedContent.containsKey( h ) ) {
            c = new Content( core, content );
            addedContent.put( c.getHash(), c );
        }
        else
            c = addedContent.get( h );

        return c;
    }

    /**
     * Create an iterable set of all versions known to the generator.
     * @return An iterable set of all versions.
     */
    public Iterable<Version> createVersionSet() {
        return core.createVersionSet();
    }

    /**
     * Create an iterator over the dependencies of a particular artifact.
     * @param artifact The artifact to find the dependencies of.
     * @return An iterator over the set of dependent artifacts.
     */
    Iterable<Artifact> findDependencies( Artifact artifact ) {
        return core.findDependencies( artifact );
    }

    /**
     * Create an iterator over artifacts that match a given name. The artifacts will be associated with the
     * specified artifact - meaning that all the artifacts returned will be tagged with the identifier of
     * the passed artifact.
     * @param internal_build The name of the internal build from which to pick the executable.
     * @param name The name of the artifact that should be returned.
     * @return An iterator over the set of artifacts.
     */
    public Iterable<Artifact[]> createArtifactSet( String internal_build, String ... name ) {
        return core.createArtifactSet( internal_build, name );
    }

    /**
     * Declare that the running test has passed. Unbinds all of the resources associated with the test.
     */
    public void pass() {
        if ( activeTestInstance == null ) {
            System.err.println( "ERROR: There is no test being generated." );
            return;
        }

        activeTestInstance.pass();
    }

    /**
     * Declare that the running test has failed for a given reason. Unbinds all of the resources associated with the
     * test. This also snapshots the entire platform (preserving logs, etc.).
     */
    public void fail() {
        if ( activeTestInstance == null ) {
            System.err.println( "ERROR: There is no test being generated." );
            return;
        }

        activeTestInstance.fail();
    }

    /**
     * Declare that a new test is being defined. Internally a test is represented as a script and description, along
     * with a set of versions that are used in the test.
     */
    public void startTest() {
        if ( activeTestInstance != null ) {
            System.err.println( "ERROR: A test has already been started, and not yet completed." );
            return;
        }

        activeTestInstance = new TestInstance( core );
    }

    /**
     * Add an action to the current test. This method is usually wrapped by the different resource
     * implementations. Actions are added in the effective order they are taken in.
     * @param action The action to take.
     * @throws Exception The action is invalid.
     */
    public void add( TestInstance.Action action ) throws Exception {
        if ( activeTestInstance == null ) {
            System.err.println( "ERROR: There is no test being generated." );
            return;
        }

        activeTestInstance.addAction( action );
    }

    /**
     * Declare that the test currently being defined is complete.
     */
    public void completeTest() {
        if ( activeTestInstance == null ) {
            System.err.println( "ERROR: There is no active test to complete." );
            return;
        }

        activeTestInstance.close();
        allTestInstances.add( activeTestInstance );
        activeTestInstance = null;
    }

    private void dumpTestInstances() {
        for ( TestInstance ti : allTestInstances ) {
            ti.dump();
        }
    }

    /**
     * Close the generator, synchronizing its results with the database.
     */
    public void close() {
        // If the system is read-only, then just dump the created objects.
        if ( core.isReadOnly() ) {
            dumpTestInstances();
            return;
        }
        
        /* The described template arrays are already loaded. We need to add any defined templates
         * that are not in the database, and remove any that are no longer needed.
         */
        try {
            core.syncDescribedTemplates( allTestInstances );
        }
        catch ( Exception e ) {
            //TODO: Process.
        }
        
        
        /* Read the main contents of the top-level synchronized tables: Content, DescribedTemplate.
         * Content is easy, since it either exists or does not.
         * DescribedTemplate is a relationship between a test instance and the required hashes.
         */
        //TODO: Get the content list
        //TODO: Get the describedtemplate list
        //TODO: Determine add and delete lists
        //TODO: Add content
        //TODO: Add described templates and test instances.


        // All generated content needs to be added or synchronized
        /*        for ( Content c : addedContent.values() ) {
        	c.sync();
        }

        // Templates can be added to the database, this will either
        for ( Template t : testResources.values() ) {
            t.sync();
        }

        for ( Template t : testResources.values() ) {
            t.syncRelationships();
        }

        // Once the templates have been updated, the documentation can be updated to include the correct keys.
        for ( Description d : testDescriptions.values() ) {
            d.sync();
        }

        for ( Description d : testDescriptions.values() ) {
            d.syncRelationships();
        }

        core.startSyncTestInstance( pk_test );

        for ( TestInstance ti : testInstances ) {
            ti.sync( pk_test );
        }

        core.stopSyncTestInstance( pk_test );

        // Now that templates and test instances are synchronized, roll up top-level template relationships.
        core.syncTopTemplateRelationships();
         */        
        core.close();
    }
}
