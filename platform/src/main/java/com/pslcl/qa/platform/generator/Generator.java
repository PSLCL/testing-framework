package com.pslcl.qa.platform.generator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.platform.Attributes;
import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.generator.Template.Parameter;

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
     * Maps parameter references to parameters.
     */
    private Map<String, Parameter> parameterReferenceMap;

    /**
     * Create a generator, which in turn can be used to generate test instances.
     * @param pk_test The primary key of the test that any generated test instances are related to.
     */
    public Generator( long pk_test ) {
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
    
    void addParameterReference(String uuid, Parameter parameter) throws IllegalStateException{
    	if(parameterReferenceMap == null) throw new IllegalStateException("Test has not been started.");
    			
    	parameterReferenceMap.put(uuid, parameter);
    }

    private static class StringContent implements Content {
        String content;
        Hash hash;
        
        StringContent( String content ) {
            hash = Hash.fromContent( content );
        }
        
        @Override
        public String getValue(Template template) throws Exception {
            return hash.toString();
        }

        @Override
        public Hash getHash() {
            return hash;
        }

        @Override
        public InputStream asStream() {
            return new ByteArrayInputStream( content.getBytes() );
        }

        @Override
        public byte[] asBytes() {
            return content.getBytes();
        }       
    }
    
    /**
     * Create content that can be used in a test.
     * @param content
     * @return
     */
    public Content createContent( String content ) {
        Content result = new StringContent( content );
        if ( ! addedContent.containsKey( result.getHash() ) ) {
            addedContent.put( result.getHash(), result );
        }
        else
            result = addedContent.get( result.getHash() );

        return result;
    }

    /**
     * Create an iterable set of all modules known to the generator.
     * @return An iterable set of all modules.
     */
    public Iterable<Module> createModuleSet() {
        return core.createModuleSet();
    }

    /**
     * Create an iterable set of all modules known to the generator for a specific organization and module name.
     * @param organization The organization of the module.
     * @param module The module name.
     * @return An iterable set of modules.
     */
    public Iterable<Module> createModuleSet(String organization, String module) {
        return core.createModuleSet(organization, module);
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
     * Create an iterator over artifacts that match a set of search criteria that include attributes that the
     * providing module must contain, the configuration that the artifact must belong to, and a set of artifact
     * names that may include regex patterns. Multiple artifact names may be specified.
     * The result set will include sets of matching artifacts, with each set coming from compatible modules (although
     * the modules may not be exactly the same). The first artifact from each matching module will be returned,
     * with the latest sequence having priority.
     * Modules are compatible if they have the same organization, module name, and version. They must have at
     * least the attributes specified.
     * @param attributes A set of attributes that each matching module must provide. Null is allowed, in which case there
     * is no filtering done by attribute.
     * @param configuration The configuration that each artifact must be in. Null is allowed, in which case there
     * is no fitering done by configuration.
     * @param name The name of the artifact that should be returned. Regex patterns are allowed as defined in MySQL.
     * @return An iterator over the set of artifacts. Each entry contains an array of artifacts in the same order
     * as the input parameters.
     */
    public Iterable<Artifact[]> createArtifactSet( Attributes attributes, String configuration, String ... name ) {
        return core.createArtifactSet( attributes, configuration, name );
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
     * Declare that the running test has been assigned to a particular email. This is
     * typically not used during generation, but is useful for populating test data.
     * @param email The email address of the assigned owner.
     */
    public void assign( String email ) {
        if ( activeTestInstance == null ) {
            System.err.println( "ERROR: There is no test being generated." );
            return;
        }

        activeTestInstance.assign( email );
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
        
        parameterReferenceMap = new HashMap<String, Parameter>();        
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
        parameterReferenceMap.clear();
        parameterReferenceMap = null;
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
            System.err.println( "ERROR: Failure to close generator, " + e.getMessage() );
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
