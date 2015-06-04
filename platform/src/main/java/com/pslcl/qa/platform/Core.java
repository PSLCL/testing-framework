package com.pslcl.qa.platform;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

/**
 * This class represents the relationship between the program and external resources like
 * filesystems and databases. It also contains all of the synchronization code.
 */
public class Core {
    private static class DBDescribedTemplate {
        long pk;
        long fk_template;
        DescribedTemplate.Key key;
        Hash description;
    }
    private static class DBTestInstance {
        long fk_test;       // INT(11) in test
        String name;        // VARCHAR(100) from test
        String description; // LONGTEXT from test
        String script;      // VARCHAR(200) from test
        long pk_test_instance;
        long fk_described_template;
        long fk_run;
        long pk_template; // added here to avoid a lookup in the executeTestInstance()
    }
    private static class DBTemplateInfo {
        long pk_described_template; // INT(11) in described_template
        byte [] fk_version_set;     // BINARY(32) in described_template
        long pk_template;           // INT(11) in described_template
        byte [] description_hash;   // BINARY(32) in described_template
        byte [] hash;               // BINARY(32) in template
        String steps;               // MEDIUMTEXT in template
        boolean enabled;            // BOOLEAN in template
    }

    /**
     * The connection to the database.
     */
    private Connection connect = null;
    @SuppressWarnings("unused")
    private String project;
    private File artifacts;
    private Map<Long,DBTestInstance> pktiToTI = new HashMap<Long,DBTestInstance>();
    private Map<Long,List<DBTestInstance>> pktToTI = new HashMap<Long,List<DBTestInstance>>();
    private Map<Long,DBDescribedTemplate> pkToDT = new HashMap<Long,DBDescribedTemplate>();
    private Map<DescribedTemplate.Key,DBDescribedTemplate> keyToDT = new HashMap<DescribedTemplate.Key,DBDescribedTemplate>();
    private Map<Long,Long> dtToTI = new HashMap<Long,Long>();
    
    // tech spec term of "test run" is this test instance, from table test_instance
    private void loadTestInstances() {
        int testInstanceCount = 0;
        int notYetRunCount = 0;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            // this first line gives 54606 test instances. way more than the other lines which give 2830 instances. For any one row in dt_line, there are multiple matching rows in described_template, so they get counted multiple items
//          resultSet = statement.executeQuery( "SELECT fk_test, name, test.description, script, pk_test_instance, test_instance.fk_described_template, fk_run FROM test_instance JOIN test ON pk_test = fk_test JOIN described_template ON test_instance.fk_described_template = pk_described_template JOIN template ON fk_template = pk_template JOIN dt_line ON pk_described_template = dt_line.fk_described_template" );
            resultSet = statement.executeQuery( "SELECT fk_test, name, test.description, script, pk_test_instance, test_instance.fk_described_template, fk_run, pk_template FROM test_instance JOIN test ON pk_test = fk_test JOIN described_template ON test_instance.fk_described_template = pk_described_template JOIN template ON fk_template = pk_template" );
//          resultSet = statement.executeQuery( "SELECT fk_test, name, test.description, script, pk_test_instance, test_instance.fk_described_template, fk_run FROM test_instance JOIN test ON pk_test = fk_test JOIN described_template ON test_instance.fk_described_template = pk_described_template" );
//          resultSet = statement.executeQuery( "SELECT fk_test, name, test.description, script, pk_test_instance, test_instance.fk_described_template, fk_run FROM test_instance JOIN test ON pk_test = fk_test" );
            
            while ( resultSet.next() ) {
                DBTestInstance dbTestInstance = new DBTestInstance();
                dbTestInstance.fk_test = resultSet.getLong( "fk_test" ); // table entry will not be null
                dbTestInstance.name = resultSet.getString("name");
                dbTestInstance.description = resultSet.getString("description");
                dbTestInstance.script = resultSet.getString("script");
                dbTestInstance.pk_test_instance = resultSet.getLong("pk_test_instance"); // null table entry returns 0 
                dbTestInstance.fk_described_template = resultSet.getLong("fk_described_template"); // null table entry returns 0
                dbTestInstance.fk_run = resultSet.getLong("fk_run"); // null table entry returns 0
                dbTestInstance.pk_template = resultSet.getLong("pk_template");

                // Add this dbTestInstance to the pktiToTi saved list. This is 1:1 relationship of pk_test_instance key to dbTestInstance.
                pktiToTI.put(Long.valueOf(dbTestInstance.pk_test_instance), dbTestInstance);
                
                // Merge this dbTestInstance into the pktToTI saved list. Note: We have multiple DBTestInstance for any fk_test key
                List<DBTestInstance> currentList = pktToTI.get(dbTestInstance.fk_test);
                if (currentList == null) {
                    currentList = new ArrayList<DBTestInstance>();
                }
                currentList.add(dbTestInstance);
                pktToTI.put( Long.valueOf(dbTestInstance.fk_test), currentList );
                if (dbTestInstance.fk_run == 0)
                    ++notYetRunCount;
                ++testInstanceCount;
            }
        } catch (Exception e) {
            System.out.println("Core.loadTestInstances() exception "+ e);
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
            System.out.println("      <internal> Core.loadTestInstances() loads " + testInstanceCount + " test instances to pktToTI; yet to run count is " + notYetRunCount);
        }
    }
    
    private void loadHashes() {
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_described_template, fk_version_set, fk_template, hash, description_hash FROM described_template JOIN template ON fk_template = pk_template" );
            while ( resultSet.next() ) {
                DBDescribedTemplate dbTemplate = new DBDescribedTemplate();
                dbTemplate.pk = resultSet.getLong( "pk_described_template" );
                dbTemplate.fk_template = resultSet.getLong( "fk_template" );
                dbTemplate.key = new DescribedTemplate.Key( new Hash( resultSet.getBytes( "hash" ) ), new Hash( resultSet.getBytes( "fk_version_set" ) ) );
                dbTemplate.description = new Hash( resultSet.getBytes( "description_hash" ) );

                pkToDT.put( dbTemplate.pk, dbTemplate );

                if ( keyToDT.containsKey( dbTemplate.key ) )
                    throw new Exception( "Duplicate DescribedTemplate.Key " + dbTemplate.pk + " " + dbTemplate.key.getTemplateHash().toString() + ":" + dbTemplate.key.getVersionHash().toString() );
                else {
                    keyToDT.put( dbTemplate.key, dbTemplate );
                }
            }
            
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
            
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_test_instance, fk_described_template FROM test_instance WHERE fk_test=" + Long.toString( pk_target_test ) );
            while ( resultSet.next() ) {
                long pk = resultSet.getLong( "pk_test_instance" );
                long fk = resultSet.getLong( "fk_described_template" );
                dtToTI.put( fk, pk );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not read described_template, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }
    }

    /**
     * The private key of the test that is being generated.
     */
    private long pk_target_test = 0;
    private boolean read_only = false;

    Core( long pk_test ) {
        this.pk_target_test = pk_test;
        this.project = System.getenv("DTF_TEST_PROJECT");
        String dir = System.getenv("DTF_TEST_ARTIFACTS");
        if ( dir != null )
            this.artifacts = new File(dir);

        openDatabase();

        /* Load the description and template hashes */
        loadHashes();
        
        loadTestInstances();
    }

    /**
     * Close the core object, releasing any resources.
     */
    public void close() {
        closeDatabase();
    }

    /**
     * Open the database connection if it is not already open. Environment variables are used
     * to determine the DB host, user, and password. The DTF_TEST_DB_HOST variable is required.
     * DTF_TEST_DB_USER and DTF_TEST_DB_PASSWORD are optional, and if not specified then a guest
     * account with no password is used. This sets the 'read_only' flag, which disables all
     * database modifications.
     */
    private void openDatabase() {
        if ( connect != null )
            return;

        try {
            // Setup the connection with the DB
            String host = System.getenv("DTF_TEST_DB_HOST");
            String user = System.getenv("DTF_TEST_DB_USER");
            String password = System.getenv("DTF_TEST_DB_PASSWORD");
            read_only = false;
            if ( user == null || password == null ) {
                user = "guest";
                password = "";
                read_only = true;
            }

            Class.forName("com.mysql.jdbc.Driver"); // not required at compile time, but required for .getConnection() to use at run time
            connect = DriverManager.getConnection("jdbc:mysql://"+host+"/qa_portal?user="+user+"&password="+password);
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not open database connection, " + e.getMessage() );
            read_only = true;
        }
    }

    /**
     * Close the database connection if it is open.
     */
    private void closeDatabase() {
        try {
            if ( connect != null ) {
                connect.close();
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not close database connection, " + e.getMessage() );
        }
        finally {
            connect = null;
        }
    }

    private void safeClose( ResultSet r ) {
        try {
            if ( r != null )
                r.close();
        }
        catch ( Exception e ) {
            // Ignore
        }
    }

    private void safeClose( Statement s ) {
        try {
            if ( s != null )
                s.close();
        }
        catch ( Exception e ) {
            // Ignore
        }
    }

    public boolean isReadOnly() {
        return read_only;
    }

    /**
     * Return the test instance matching the specified test instance number.
     * @param testNumber
     * @return
     */
    DBTestInstance readReadyTestInstance_testInstance(long testInstanceNumber) {
        DBTestInstance retVal = pktiToTI.get(Long.valueOf(testInstanceNumber));
        return (retVal.fk_run != 0) ? null : retVal;
    }
    
    /**
     * Return a set of all test instances matching the specified test number and test instance number that have not yet run and are ready to run.
     * @param testNumber
     * @return
     */
    Set<Long> readReadyTestInstances_test(long testNumber, long testInstanceNumber) {
        Set<Long> retSet = new HashSet<Long>();
        Set<Long> fullSet = readReadyTestInstances_test(testNumber);
        if (fullSet.contains(Long.valueOf(testInstanceNumber))) {
            for (Long setMember : fullSet) {
                if (setMember.longValue() == testInstanceNumber)
                    retSet.add(setMember);
            }
        }
        return retSet;
    }
    
    /**
     * Return a set of all test instances matching the specified test number that have not yet run and are ready to run.
     * @param testNumber
     * @return
     */
    Set<Long> readReadyTestInstances_test(long testNumber) {
        Set<Long> retSet = new HashSet<Long>();
        List<DBTestInstance> list_pkTest_ToMany_pkTestInstance = pktToTI.get(Long.valueOf(testNumber));
        if (list_pkTest_ToMany_pkTestInstance != null) {
            for (DBTestInstance dbti: list_pkTest_ToMany_pkTestInstance) {
                if (dbti.fk_run == 0) // 0: null in database: not yet run 
                    retSet.add(Long.valueOf(dbti.pk_test_instance));
            }
        }
        return retSet;
    }

    /**
     * From a given test instance number, execute the corresponding test instance (aka test run). 
     */
    void executeTestInstance(long testInstanceNumber) {
        // We are an independent process. We have access to the database,
        //   to a Resource Manager that has access to artifacts and resources,
        //   and to everything else needed to cause our test instance to be executed.

        DBTestInstance dbti = readReadyTestInstance_testInstance(testInstanceNumber);
        if (dbti != null && dbti.fk_described_template != 0) {
            String str_fkTestInstanceNumber = Long.toString(testInstanceNumber);

            // This simple line requires that .pk_template be placed in DBTestInstance. 
            String str_fkTemplate = Long.toString(dbti.pk_template);
            
//            // This lookup works just as well to fill str_fkTemplate, and does not require the presence of .pk_template being placed in DBTestInstance.
//            DBDescribedTemplate dbdt = pkToDT.get(Long.valueOf(dbti.fk_described_template));
//            str_fkTemplate = Long.toString(dbdt.fk_template);
            
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT pk_described_template, fk_version_set, pk_template, description_hash, hash, steps, enabled FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance = " + str_fkTestInstanceNumber + " AND pk_template = " + str_fkTemplate );
                // exactly one resultSet (because we required test_instance.fk_described_template to match described_template.pk_described_template)
                if ( resultSet.next() ) {
                    DBTemplateInfo dbtemplateinfo = new DBTemplateInfo();
                    dbtemplateinfo.pk_described_template = resultSet.getLong( "pk_described_template" ); // table entry will not be null
                    dbtemplateinfo.fk_version_set = resultSet.getBytes("fk_version_set");
                    dbtemplateinfo.pk_template = resultSet.getLong("pk_template");
                    dbtemplateinfo.description_hash = resultSet.getBytes("description_hash");
                    dbtemplateinfo.hash = resultSet.getBytes("hash");
                    dbtemplateinfo.steps = resultSet.getString("steps");
                    dbtemplateinfo.enabled = resultSet.getBoolean("enabled");

                    if (resultSet.next())
                        System.out.println( "WARN: More than one ResultSet found. This is unexpected. Dropping all but the first ResultSet which was just accessed and which may be wrong data; test instance processing proceeds." );
                    
                    // dbtemplateinfo is used to execute this test instance by following steps; aka instantiate this test run to generate a test result
                    System.out.println( "executeTestInstance() has data base info for test instance " + dbti.pk_test_instance + " for test " + dbti.fk_test + ", finding described_template " + dbti.fk_described_template + " and template " + dbti.pk_template );
                    System.out.println( "executeTestInstance() finds test script: " + dbti.script);
                    System.out.println( "executeTestInstance() finds enabled: " + dbtemplateinfo.enabled);
                    System.out.println( "executeTestInstance() finds version set: " + dbtemplateinfo.fk_version_set);
                    System.out.println( "executeTestInstance() finds description_hash: " + dbtemplateinfo.description_hash);
                    System.out.println( "executeTestInstance() finds hash: " + dbtemplateinfo.hash);
                    System.out.println( "executeTestInstance() finds steps: \n" + dbtemplateinfo.steps);
                    
                    // Establish everything and make the test run execute.
                    
                   
                    // Wait for a test result.
                    
                    // this simulates waiting for a test result; wait a random time from 1 to 5 seconds
                    Random random = new Random();
                    int sleep = random.nextInt(5001-500) + 500; // 0.5 to 5 seconds
                    try {
                        Thread.sleep(sleep);
                    } catch (Exception e) {
                    }
                    
                    // Place the test result in the database and mark the test instance as complete in the database.
                    

                    // simulated false condition, but database is readonly for a while, so this gives a quick return without storing anything
                    reportResult( new String("junk"), false );

                    System.out.println( "executeTestInstance() exits after execution msec of " + sleep);
                    System.out.println("");
                }
                else {
                    System.out.println( "WARN: no DBTemplateInfo found" );
                }
            } catch (Exception e) {
                System.err.println( "ERROR: Could not read template table, " + e.getMessage() );
            } finally {
                try {
                    resultSet.close();
                    statement.close();
                } catch (SQLException e) {
                    // log this?
                }
            }
        } else {
            System.out.println( "WARN: ready test instance not found: " + testInstanceNumber );
        }
    }
    
    /**
     * Return a map of all components that exist in the database.
     * @return A map from each existing component (by name) to its primary key.
     */
    public Map<String,Long> readComponents() {
        Map<String,Long> result = new HashMap<String,Long>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT * FROM component" );
            while ( resultSet.next() ) {
                String name = resultSet.getString( "name" );
                Long pk = resultSet.getLong( "pk_component" );
                result.put( name, pk );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not read components, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }

    /**
     * Add a component to the database.
     * @param component The component name.
     * @return The primary key of the new component, or zero if there is an error or in read-only mode.
     */
    public long addComponent( String component ) {
        PreparedStatement statement = null;
        long pk = 0;

        if ( read_only )
            return pk;

        try {
            statement = connect.prepareStatement("INSERT INTO component (name) values (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString( 1, component );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add component, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    /* Note that there is no deleteComponent - that is a manual operation. */

    /**
     * REturn a map of all versions related to a specified component.
     * @param fk_component The component to return versions for.
     * @return A map from the version string to the primary key of the version.
     */
    public Map<String,Long> readVersions( long fk_component ) {
        Map<String,Long> result = new HashMap<String,Long>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( String.format( "SELECT * FROM version WHERE fk_component=%d", fk_component ) );
            while ( resultSet.next() ) {
                String version = resultSet.getString( "version" );
                Long pk = resultSet.getLong( "pk_version" );
                result.put( version, pk );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not read versions for component " + fk_component + ", " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }

    /**
     * Add a version to a specified component.
     * @param fk_component The primary key of the component to add the version to.
     * @param version The version to add.
     * @return The primary key of the new version, or zero if there is an error or in read-only mode.
     */
    public long addVersion( long fk_component, String version ) {
        PreparedStatement statement = null;
        long pk = 0;

        if ( read_only )
            return pk;

        //TODO: Release date, actual release date, status, order all need to be added.
        try {
            statement = connect.prepareStatement( "INSERT INTO version (fk_component, version) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS );
            statement.setLong( 1, fk_component );
            statement.setString( 2, version );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add version to component, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    /**
     * Delete a version.
     * @param pk_version The primary key of the version to delete.
     */
    public void deleteVersion( long pk_version ) {
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( "DELETE FROM version WHERE pk_version=?" );
            statement.setLong( 1, pk_version );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not delete version, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }

    /**
     * Give a version and a set of artifacts, synchronize the database with the artifacts. There
     * may be many artifacts associated with a version.
     * @param pk_version The primary key of the version (and by extension the component) for these artifacts.
     * @param artifacts A set of artifacts that should exist.
     */
    public void synchronizeArtifacts( long pk_version, ArtifactSink artifacts ) {
        PreparedStatement clear_synchronized = null;
        PreparedStatement delete_not_synchronized = null;
        PreparedStatement find_artifact = null;
        PreparedStatement mark_synchronized = null;
        PreparedStatement find_content = null;
        PreparedStatement create_content = null;
        PreparedStatement create_artifact = null;
        ResultSet resultSet = null;

        if ( read_only )
            return;

        /* Given that the number of artifacts may be large and we want to preserve the artifact
         * primary key unless there is a change we do the following:
         *   1. Bulk unmark all of the existing artifacts and content.
         *   2. Scan the current artifacts, marking matches.
         *   3. Add current artifacts that do not match (default marked).
         *   4. Bulk delete database artifacts that are unmarked.
         */
        try {
            clear_synchronized = connect.prepareStatement( "UPDATE artifact SET synchronized=0 WHERE fk_version=?" );
            clear_synchronized.setLong( 1,  pk_version );
            clear_synchronized.executeUpdate();
            safeClose( clear_synchronized ); clear_synchronized = null;

            if ( ! this.artifacts.isDirectory() )
                //noinspection ResultOfMethodCallIgnored
                this.artifacts.mkdirs();

            connect.setAutoCommit( false );

            find_artifact = connect.prepareStatement( "SELECT pk_artifact FROM artifact WHERE fk_version=? and fk_content=? AND name=?" );
            mark_synchronized = connect.prepareStatement( "UPDATE artifact SET synchronized=1 WHERE pk_artifact=?" );
            find_content = connect.prepareStatement( "SELECT pk_content FROM content WHERE pk_content=?" );
            create_content = connect.prepareStatement( "INSERT INTO content (pk_content) VALUES (?)" );
            create_artifact = connect.prepareStatement( "INSERT INTO artifact (fk_version, fk_content, synchronized, platform, internal_build, name) VALUES (?,?,?,?,?,?)" );

            for ( ArtifactSink.Entry entry : artifacts.entries ) {
                // Always update the file if it doesn't exist, independent of database.
                File a = new File( this.artifacts, entry.hash.toString() );
                if ( ! a.exists() ) {
                    FileOutputStream os = new FileOutputStream( a );
                    InputStream is = entry.content.asStream();
                    IOUtils.copy(is, os);
                    is.close();
                    os.close();
                }

                find_artifact.setLong( 1, pk_version );
                find_artifact.setBinaryStream(2, new ByteArrayInputStream(entry.hash.toBytes()));
                find_artifact.setString( 3, entry.name );

                resultSet = find_artifact.executeQuery();
                if ( ! resultSet.isBeforeFirst() ) {
                    // There were no matches. Time to insert. Need to determine if the content exists.
                    safeClose( resultSet ); resultSet = null;

                    find_content.setBinaryStream(1, new ByteArrayInputStream(entry.hash.toBytes()));
                    resultSet = find_content.executeQuery();
                    if ( ! resultSet.isBeforeFirst() ) {
                        // There is no content. Need to add.
                        safeClose( resultSet ); resultSet = null;

                        create_content.setBinaryStream( 1, new ByteArrayInputStream( entry.hash.toBytes() ) );
                        create_content.executeUpdate();
                    }

                    create_artifact.setLong( 1, pk_version );
                    create_artifact.setBinaryStream(2, new ByteArrayInputStream(entry.hash.toBytes()));
                    create_artifact.setInt(3, 1);  // Additions are synchronized
                    create_artifact.setString(4, entry.platform);
                    create_artifact.setString( 5, entry.internal_build);
                    create_artifact.setString( 6, entry.name );
                    create_artifact.executeUpdate();
                }
                else {
                    resultSet.next();
                    Long pk = resultSet.getLong( "pk_artifact" );
                    safeClose( resultSet ); resultSet = null;

                    mark_synchronized.setLong( 1, pk );
                    mark_synchronized.executeUpdate();
                }
            }

            connect.commit();
            connect.setAutoCommit( true );

            delete_not_synchronized = connect.prepareStatement( "DELETE FROM artifact WHERE synchronized=0 AND fk_version=?" );
            delete_not_synchronized.setLong( 1, pk_version );
            delete_not_synchronized.executeUpdate();
            safeClose( delete_not_synchronized ); delete_not_synchronized = null;
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't synchronize artifacts, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet );
            safeClose( clear_synchronized );
            safeClose( delete_not_synchronized );
            safeClose( find_content );
            safeClose( create_content );
            safeClose( mark_synchronized );
            safeClose( create_artifact );
            safeClose( find_artifact );
        }
    }

    /**
     * Clear the is_generated flag on all content. If not set before pruneContent() is called, then the content
     * will be deleted by pruneContent() unless associated with an artifact.
     */
    public void clearGeneratedContent() {
        // Mark test instances for later cleanup.
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( "UPDATE content SET is_generated=0" );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could update generated flags, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }

    /**
     * Remove all non-generated content that is not referenced by any artifact.
     */
    public void pruneContent() {
        PreparedStatement statement = null;

        if ( read_only )
            return;

        // TODO: This will have to change for created content.
        try {
            statement = connect.prepareStatement( "DELETE content FROM content LEFT JOIN artifact ON content.pk_content = artifact.fk_content WHERE artifact.fk_content IS NULL AND artifact.is_generated=0" );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't prune content, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }


    public void pruneTemplates() {
        // Find all top-level templates.
        PreparedStatement findTemplates = null;
        ResultSet foundTemplates = null;
        PreparedStatement deleteTemplate = null;

        if ( read_only )
            return;

        // Determine the set of all referenced templates.
        Set<Long> used = new HashSet<Long>();
        try {
            findTemplates = connect.prepareStatement( "select distinct fk_template from test_instance" );
            foundTemplates = findTemplates.executeQuery();
            while ( foundTemplates.next() ) {
                long pk = foundTemplates.getLong( "fk_template" );
                getRequiredTemplates( pk, used );
            }

            safeClose( findTemplates ); findTemplates = null;
            safeClose( foundTemplates ); foundTemplates = null;

            deleteTemplate = connect.prepareStatement( "DELETE FROM template WHERE pk_template=?" );
            findTemplates = connect.prepareStatement( "select pk_template from template" );
            foundTemplates = findTemplates.executeQuery();
            while ( foundTemplates.next() ) {
                long pk = foundTemplates.getLong( "pk_template" );
                if ( ! used.contains( pk ) ) {
                    // Delete the template. This will delete all the related tables.
                    deleteTemplate.setLong( 1, pk );
                    deleteTemplate.executeUpdate();
                }
            }
        }
        catch ( Exception e ) {
            safeClose( foundTemplates ); foundTemplates = null;
            safeClose( deleteTemplate ); deleteTemplate = null;
        }
    }

    Iterable<Version> createVersionSet() {
        List<Version> set = new ArrayList<Version>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( String.format( "SELECT component.name," +
                    " version.version" +
                    " FROM component" +
                    " JOIN version ON component.pk_component = version.fk_component" ) );
            while ( resultSet.next() ) {
                Version V = new Version( this, resultSet.getString(1), resultSet.getString(2) );
                set.add( V );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    Iterable<Artifact> findDependencies( Artifact artifact ) {
        List<Artifact> set = new ArrayList<Artifact>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            String query = String.format( "SELECT component.name," +
                    " version.version, artifact.platform, artifact.internal_build, artifact.name, artifact.fk_content" +
                    " FROM artifact" +
                    " JOIN version ON version.pk_version = artifact.fk_version" +
                    " JOIN component ON component.pk_component = version.fk_component" +
                    " WHERE artifact.name = '%s.dep" + "' AND" +
                    " version.version = '%s' AND" +
                    " artifact.internal_build = '%s'", artifact.getName(), artifact.getVersion().getVersion(), artifact.getVariant() );
            resultSet = statement.executeQuery( query );
            if ( resultSet.next() ) {
                Hash hash = new Hash( resultSet.getBytes(6) );
                File f = new File( this.artifacts, hash.toString() );
                LineIterator iterator = new LineIterator( new FileReader( f ) );
                while ( iterator.hasNext() ) {
                    String line = iterator.next();
                    String[] fields = line.split(",");
                    if ( fields.length == 1 ) {
                        safeClose( resultSet ); resultSet = null;
                        safeClose( statement ); statement = null;

                        statement = connect.createStatement();
                        query = String.format( "SELECT component.name," +
                                " version.version, artifact.pk_artifact, artifact.platform, artifact.internal_build, artifact.name, artifact.fk_content" +
                                " FROM artifact" +
                                " JOIN version ON version.pk_version = artifact.fk_version" +
                                " JOIN component ON component.pk_component = version.fk_component" +
                                " WHERE artifact.name LIKE '%s" + "' AND" +
                                " version.version = '%s' AND" +
                                " artifact.internal_build = '%s'", fields[0], artifact.getVersion().getVersion(), artifact.getVariant() );
                        resultSet = statement.executeQuery( query );
                        while ( resultSet.next() ) {
                            String component = resultSet.getString(1);
                            String version = resultSet.getString(2);
                            hash = new Hash( resultSet.getBytes(7) );
                            set.add( new Artifact( resultSet.getLong(3), resultSet.getString(6), new Version( this, component, version ), artifact.getPlatform(), artifact.getVariant(), hash ) );
                        }
                    }
                    else if ( fields.length == 3 ) {
                        VersionRange range = new VersionRange( fields[1] );
                        int[] version = null;
                        String chosenVersion = "";

                        safeClose( resultSet ); resultSet = null;
                        safeClose( statement ); statement = null;

                        statement = connect.createStatement();
                        query = String.format( "SELECT component.name," +
                                " version.version " +
                                " FROM version" +
                                " JOIN component ON component.pk_component = version.fk_component" +
                                " WHERE component.name = '%s" + "'", fields[0] );
                        resultSet = statement.executeQuery( query );
                        while ( resultSet.next() ) {
                            String V = resultSet.getString(2);
                            if ( range.contains( V ) ) {
                                String[] nums = V.split( "[^0-9]" );
                                int[] num = new int[nums.length];
                                for ( int i = 0; i < num.length; i++ )
                                    num[i] = Integer.parseInt( nums[i] );

                                if ( version == null ) {
                                    version = num;
                                    chosenVersion = V;
                                    continue;
                                }

                                for ( int i = 0; i < num.length || i < version.length; i++ ) {
                                    int chosen = i < version.length ? version[i] : 0;
                                    int possible = i < num.length ? num[i] : 0;
                                    if ( possible > chosen ) {
                                        version = num;
                                        chosenVersion = V;
                                        continue;
                                    }
                                }
                            }
                        }

                        safeClose( resultSet ); resultSet = null;
                        safeClose( statement ); statement = null;

                        statement = connect.createStatement();
                        query = String.format( "SELECT component.name," +
                                " version.version, artifact.pk_artifact, artifact.platform, artifact.internal_build, artifact.name, artifact.fk_content" +
                                " FROM artifact" +
                                " JOIN version ON version.pk_version = artifact.fk_version" +
                                " JOIN component ON component.pk_component = version.fk_component" +
                                " WHERE artifact.name LIKE '%s" + "' AND" +
                                " version.version = '%s' AND" +
                                " artifact.internal_build = '%s'", fields[2], chosenVersion, artifact.getVariant() );
                        resultSet = statement.executeQuery( query );
                        while ( resultSet.next() ) {
                            String component = resultSet.getString(1);
                            hash = new Hash( resultSet.getBytes(7) );
                            set.add( new Artifact( resultSet.getLong(3), resultSet.getString(6), new Version( this, component, chosenVersion ), artifact.getPlatform(), artifact.getVariant(), hash ) );
                        }
                    }
                    else
                        throw new Exception( "ERROR: Illegal line (" + line + ") in " + artifact.getName() + ".dep" );
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    public Iterable<Artifact[]> createArtifactSet( String internal_build, String ... name ) {
        Statement statement = null;
        ResultSet resultSet = null;
        List<Artifact[]> set = new ArrayList<Artifact[]>();
        Map<String,Artifact[]> map = new HashMap<String, Artifact[]>();

        for ( int name_index = 0; name_index < name.length; name_index++ ) {
            String artifact_name = name[ name_index ];
            try {
                statement = connect.createStatement();
                resultSet = statement.executeQuery( String.format( "SELECT component.name," +
                        " version.version, artifact.pk_artifact, artifact.platform, artifact.internal_build, artifact.name, artifact.fk_content" +
                        " FROM artifact" +
                        " JOIN version ON version.pk_version = artifact.fk_version" +
                        " JOIN component ON component.pk_component = version.fk_component" +
                        " WHERE artifact.name LIKE '%s' AND artifact.internal_build = '%s'", artifact_name, internal_build ) );
                while ( resultSet.next() ) {
                    String V = resultSet.getString(1) + ":" + resultSet.getString(2);
                    Artifact[] artifacts;
                    if ( map.containsKey( V ) )
                        artifacts = map.get( V );
                    else {
                        artifacts = new Artifact[ name.length ];
                        map.put( V, artifacts );
                    }

                    if ( artifacts[ name_index ] == null )
                        artifacts[ name_index ] = new Artifact( resultSet.getLong(3), resultSet.getString(6), new Version( this, resultSet.getString(1), resultSet.getString(2) ), resultSet.getString(4), internal_build, new Hash( resultSet.getBytes(7) ) );
                }
            }
            catch ( Exception e ) {
                System.err.println( "ERROR: Exception " + e.getMessage() );
                e.printStackTrace( System.err );
            }
            finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        }

        for ( String V : map.keySet() ) {
            Artifact[] list = map.get(V);
            int found = 0;
            for ( int i = 0; i < list.length; i++ )
                if ( list[i]  != null )
                    found += 1;

            if ( found == name.length )
                set.add( list );
        }

        return set;
    }

    public Map<Long,String> getGenerators() {
        Map<Long,String> result = new HashMap<Long,String>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( String.format( "SELECT test.pk_test, test.script" +
                    " FROM test" +
                    " JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan" +
                    " WHERE test.script != ''") );
            while ( resultSet.next() ) {
                result.put( resultSet.getLong(1), resultSet.getString(2) );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }

    public Set<String> getPlatforms( String component, String version ) {
        Set<String> result = new HashSet<String>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( String.format( "SELECT DISTINCT artifact.platform" +
                    " FROM component" +
                    " JOIN version ON component.pk_component = version.fk_component" +
                    " JOIN artifact ON version.pk_version = artifact.fk_version" +
                    " WHERE component.name='%s' AND version.version='%s'", component, version) );
            while ( resultSet.next() ) {
                result.add( resultSet.getString(1) );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }

    public List<Artifact> getArtifacts( String version, String platform, String name ) {
        Statement statement = null;
        ResultSet resultSet = null;
        List<Artifact> set = new ArrayList<Artifact>();
        String name_match = "";

        if ( name != null )
            name_match = "artifact.name LIKE '" + name + "' AND ";

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT component.name," +
                    " version.version, artifact.pk_artifact, artifact.platform, artifact.internal_build, artifact.name, artifact.fk_content" +
                    " FROM artifact" +
                    " JOIN version ON version.pk_version = artifact.fk_version" +
                    " JOIN component ON component.pk_component = version.fk_component" +
                    " WHERE " + name_match + "artifact.platform = '" + platform + "' AND version.version = '" + version + "'" );
            while ( resultSet.next() ) {
                //TODO: Handle internal_build.
                set.add( new Artifact( resultSet.getLong(3), resultSet.getString(6), new Version(this, resultSet.getString(1), resultSet.getString(2) ), resultSet.getString(4), "", new Hash( resultSet.getBytes(7) ) ) );
            }

        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    long findComponent( String component ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT component.pk_component" +
                    " FROM component" +
                    " WHERE component.name = '" + component + "'");
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "component.pk_component" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find the component, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

    /**
     * Return whether the specified version is associated with the current core target test. This is true
     * if there is a relationship from the test through the test plan to the component and version.
     * @param version
     * @return
     */
    boolean isAssociatedWithTest( Version version ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT test.pk_test" +
                    " FROM test" +
                    " JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan" +
                    " JOIN component_to_test_plan ON component_to_test_plan.fk_test_plan = test_plan.pk_test_plan" +
                    " WHERE test.pk_test = ? AND component_to_test_plan.fk_component = ?" );
            statement.setLong( 1, pk_target_test );
            statement.setLong( 2, version.getComponentPK() );
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                return true;
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Failure determining test association, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return false;
    }

    long findTestPlan() {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT test.fk_test_plan" +
                    " FROM test" +
                    " WHERE test.pk_test = ?");
            statement.setLong( 1, pk_target_test );

            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "test.fk_test_plan" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find test plan, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

    long findVersion( long pk_component, String version ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT version.pk_version" +
                    " FROM version" +
                    " JOIN component ON component.pk_component = version.fk_component" +
                    " WHERE component.pk_component = " + pk_component + " AND version.version = '" + version + "'");
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "version.pk_version" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find version, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

    public void syncGeneratedContent( Content sync ) {
        PreparedStatement find_content = null;
        PreparedStatement create_content = null;
        PreparedStatement mark_synchronized = null;
        ResultSet resultSet = null;

        if ( read_only ) {
            System.err.println( "------------------------" );
            System.err.println( "Generated Content: " + sync.getHash().toString() );
            System.err.println( "Content: " + sync.getContent() );
            System.err.println( "------------------------" );
            return;
        }

        try {
            if ( ! this.artifacts.isDirectory() )
                //noinspection ResultOfMethodCallIgnored
                this.artifacts.mkdirs();

            find_content = connect.prepareStatement( "SELECT hex(pk_content) FROM content WHERE pk_content=?" );
            create_content = connect.prepareStatement( "INSERT INTO content (pk_content,is_generated) VALUES (?,1)" );
            mark_synchronized = connect.prepareStatement( "UPDATE content SET is_generated=1 WHERE pk_content=?" );

            // Always update the file if it doesn't exist, independent of database.
            File a = new File( this.artifacts, sync.getHash().toString() );
            if ( ! a.exists() ) {
                FileUtils.writeStringToFile( a, sync.getContent() );
            }


            find_content.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
            resultSet = find_content.executeQuery();
            if ( ! resultSet.isBeforeFirst() ) {
                // There is no content. Need to add.
                safeClose( resultSet ); resultSet = null;

                create_content.setBinaryStream( 1, new ByteArrayInputStream( sync.getHash().toBytes() ) );
                create_content.executeUpdate();
            }
            else {
                safeClose( resultSet ); resultSet = null;

                mark_synchronized.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
                mark_synchronized.executeUpdate();
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't synchronize content, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( find_content ); find_content = null;
            safeClose( create_content ); create_content = null;
            safeClose( mark_synchronized ); mark_synchronized = null;
        }
    }

    void addToSet( DescribedTemplate dt, Set<DescribedTemplate> set ) {
        if ( ! set.contains( dt ) ) {
            set.add( dt );
            for ( DescribedTemplate child : dt.getDependencies() )
                addToSet( child, set );
        }
    }

    private void addActions( DescribedTemplate dt, long pk ) throws Exception {
        PreparedStatement statement = null;
        ResultSet keys;
        
        for ( int i = 0; i < dt.getActionCount(); i++ ) {
            TestInstance.Action A = dt.getAction( i );
            
            statement = connect.prepareStatement( "INSERT INTO dt_line (fk_described_template,line,fk_child_dt,fk_resource,description) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
            statement.setLong( 1, pk );
            statement.setInt( 2, i );
            
            DescribedTemplate child = A.getIncludedTemplate();
            if ( child == null )
                statement.setNull( 3,  Types.NULL );
            else
                statement.setLong( 3, child.getPK() );
            
            Resource bound = A.getBoundResource();
            if ( bound == null )
                statement.setNull( 4, Types.NULL );
            else
                statement.setLong( 4,  bound.getPK() );

            statement.setString( 5, A.getDescription() );
            statement.executeUpdate();

            long linepk = 0;
            keys = statement.getGeneratedKeys();
            if ( keys.next() )
                linepk = keys.getLong( 1 );

            safeClose( statement ); statement = null;
            
            //TODO: This doesn't handle dependencies, which need to roll up.
            statement = connect.prepareStatement( "INSERT INTO dt_to_dt (fk_parent,fk_child) VALUES (?,?)" );
            statement.setLong( 1,  pk );
            statement.setLong( 2, linepk );
            statement.executeUpdate();
            safeClose( statement ); statement = null;
            
            TestInstance.Action.ArtifactUses au = A.getArtifactUses();
            if ( au != null ) {
                Iterator<Artifact> iter = au.getArtifacts();
                while ( iter.hasNext() ) {
                    Artifact artifact = iter.next();
                    
                    statement = connect.prepareStatement( "INSERT INTO artifact_to_dt_line (fk_artifact, fk_dt_line, is_primary, reason) VALUES (?,?,?,?)" );
                    statement.setLong( 1, artifact.getPK() );
                    statement.setLong( 2, linepk );
                    statement.setInt( 3, au.getPrimary() ? 1 : 0 );
                    statement.setString( 4, au.getReason() );
                    statement.executeUpdate();
                    safeClose( statement ); statement = null;
                }
            }
        }

    }
    
    /**
     * Add a described template - it is known to not exist.
     * @param dt The described template to add.
     * @return The key information for the added described template.
     */
    private DBDescribedTemplate add( DescribedTemplate dt, Boolean result ) throws Exception {
        DescribedTemplate.Key proposed_key = dt.getKey();
        
        if ( keyToDT.containsKey( proposed_key ) )
            return keyToDT.get( proposed_key );

        // Recursive check for all dependencies
        for ( DescribedTemplate child : dt.getDependencies() ) {
            DBDescribedTemplate dbdt;
            if ( ! keyToDT.containsKey( child.getKey() ) )
                dbdt = add( child, null );
            else
                dbdt = check( child, null );
        }

        try {
            /* All described template additions are handled as transactions. */
            connect.setAutoCommit( false );

            long pk = 0;
            long pk_template = syncTemplate( dt.getTemplate() );
            if ( result != null )
                reportResult( dt.getTemplate().getHash().toString(), result );

            PreparedStatement statement = null;
            ResultSet query = null;
            try {
                statement = connect.prepareStatement( "INSERT INTO described_template (fk_version_set, fk_template, description_hash, synchronized) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
                statement.setBinaryStream(1, new ByteArrayInputStream( dt.getKey().getVersionHash().toBytes() ) );
                statement.setLong(2, pk_template );
                statement.setBinaryStream(3, new ByteArrayInputStream( dt.getDocumentationHash().toBytes()) );
                statement.setInt(4, 1); // Default is synchronized.
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if ( keys.next() )
                    pk = keys.getLong( 1 );

                safeClose( statement ); statement = null;
                
                addActions ( dt, pk );
            }
            catch ( SQLException e ) {
                //TODO: Figure out that this is a duplicate key or not.
                safeClose( statement ); statement = null;
                
                statement = connect.prepareStatement( "SELECT pk_described_template FROM described_template WHERE fk_version_set=? AND fk_template=?" );
                statement.setBinaryStream(1, new ByteArrayInputStream( dt.getKey().getVersionHash().toBytes() ) );
                statement.setLong(2, pk_template );
                query = statement.executeQuery();

                if ( query.next() )
                    pk = query.getLong( 1 );

                safeClose( statement ); statement = null;
                safeClose( query );
            }
            finally {
                safeClose( statement ); statement = null;
            }

            connect.commit();
            connect.setAutoCommit( true );
            
            DBDescribedTemplate dbdt = new DBDescribedTemplate();
            dbdt.pk = pk;
            dbdt.key = dt.getKey();
            pkToDT.put( dbdt.pk, dbdt );
            keyToDT.put( dbdt.key, dbdt );
            return dbdt;
        }
        catch ( Exception e ) {
            try {
                connect.rollback();
                connect.setAutoCommit( true );
            }
            catch ( Exception ex ) {
                // TODO: This is really bad - failure to restore state.
            }
        }

        return null;
    }

    /**
     * Check that an existing template is correct. If the template exists then the children
     * must exist, but their documentation may be out of date.
     * @param dt The described template to check.
     * @return
     */
    private DBDescribedTemplate check( DescribedTemplate dt, Boolean result ) throws Exception {
        // Recursive check for all dependencies
        for ( DescribedTemplate child : dt.getDependencies() ) {
            DBDescribedTemplate dbdt;
            if ( ! keyToDT.containsKey( child.getKey() ) )
                throw new Exception( "Parent template exists, child does not." );
            else
                dbdt = check( child, null );
        }

        DBDescribedTemplate me = keyToDT.get( dt.getKey() );
        if ( me == null )
            throw new Exception( "Request to check a non-existent template." );
        
        if ( dt.getDocumentationHash().equals( me.description ) )
            return me;
        
        // Documentation needs to be recreated.
        PreparedStatement statement = null;
        try {
            statement = connect.prepareStatement( "DELETE FROM dt_line WHERE fk_described_template = ?" );
            statement.setLong( 1, me.pk );
            statement.executeUpdate();
        }
        finally {
            safeClose( statement ); statement = null;
        }

        try {
            connect.setAutoCommit( false );
            
            try {
                statement = connect.prepareStatement( "UPDATE described_template SET description_hash=? WHERE pk_described_template=?" );
                statement.setBinaryStream(1, new ByteArrayInputStream( dt.getDocumentationHash().toBytes() ) );
                statement.setLong( 2,  me.pk );
                statement.executeUpdate();
            }
            finally {
                safeClose( statement ); statement = null;
            }
            
            addActions( dt, me.pk );
            
            connect.commit();
            connect.setAutoCommit( true );
        }
        catch ( Exception e ) {
            connect.rollback();
            connect.setAutoCommit( true );
        }

        return me;
    }

    /**
     * Compare all described templates, deleting those that should not exist, adding
     * those that need to be created, and updating those that need to be updated.
     * Updates are limited to documentation changes.
     * @param allTestInstances A list of all test instances, which refer to all the test instances.
     */
    void syncDescribedTemplates( List<TestInstance> allTestInstances ) throws Exception {
        for ( TestInstance ti : allTestInstances ) {
            DBDescribedTemplate dbdt;
            DescribedTemplate.Key key = ti.getTemplate().getKey();

            if ( ! keyToDT.containsKey( key ) ) {
                // Add the template
                dbdt = add( ti.getTemplate(), ti.getResult() );
            }
            else
                dbdt = check( ti.getTemplate(), ti.getResult() );

            // We have the described template. There should be a Test Instance that relates the
            // current test (pk_test) to the current described template.
            if ( ! dtToTI.containsKey( dbdt.pk ) ) {
                // No test instance, add it.
                PreparedStatement statement2 = null;
                try {
                    statement2 = connect.prepareStatement( "INSERT INTO test_instance (fk_test, fk_described_template, phase, synchronized) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
                    statement2.setLong(1, pk_target_test );
                    statement2.setLong( 2, dbdt.pk );
                    //TODO: Determine the phase
                    statement2.setLong(3, 0);
                    statement2.setInt(4,1);  // Default is synchronized.
                    statement2.executeUpdate();
    
                    ResultSet keys = statement2.getGeneratedKeys();
                    if ( keys.next() )
                        ti.pk = keys.getLong( 1 );
                }
                catch ( Exception e ) {
                    
                }
                
                safeClose( statement2 ); statement2 = null;
                dbdt.pk = ti.pk;
                dtToTI.put( dbdt.pk, ti.pk );
            }
            
            // If the ti has a pass/fail state then make sure it is reflected.
            Statement statement2 = null;
            ResultSet resultSet = null;
            Boolean dbResult = null;
            
            if ( ti.getResult() != null ) {
                try {
                    statement2 = connect.createStatement();
                    resultSet = statement2.executeQuery( "SELECT passed FROM run JOIN test_instance ON test_instance.fk_run = run.pk_run WHERE test_instance.pk_test_instance=" + Long.toString( ti.pk ) );
                    
                    if ( resultSet.next() ) {
                        dbResult = resultSet.getBoolean( "passed" );
                        if ( resultSet.wasNull() )
                            dbResult = null;
                    }
                }
                catch ( Exception e ) {
                    // Ignore
                }
                finally {
                    safeClose( resultSet ); resultSet = null;
                    safeClose( statement2 ); statement2 = null;
                }
        
                // Check the run status, fix it if the status is known.
                if ( (dbResult == null) || (dbResult != ti.getResult()) ) {
                    reportResult( ti.getTemplate().getTemplate().getHash().toString(), ti.getResult() );
                }
            }
        }
    }

    public long syncTemplate( Template sync ) {
        long pk = 0;

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        if ( read_only ) {
            System.err.println( "------------------------" );
            System.err.println( "Template: " + sync.getHash().toString() );
            System.err.println( "Script: " + sync.toStandardString() );
            System.err.println( "------------------------" );
            return pk;
        }

        try {
            statement = connect.prepareStatement( "SELECT pk_template FROM template WHERE hash=?" );
            statement.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
            
            resultSet = statement.executeQuery();
            if ( ! resultSet.isBeforeFirst() ) {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;

                statement = connect.prepareStatement( "INSERT INTO template (hash, steps, enabled) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS );
                statement.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
                statement.setString(2, sync.toStandardString());
                statement.setInt(3, 1);	// Default is enabled.
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if ( keys.next() )
                    pk = keys.getLong( 1 );

                safeClose( statement ); statement = null;
            }
            else {
                resultSet.next();
                pk = resultSet.getLong( "pk_template" );
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't synchronize template, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    private List<Hash> getExistingTopArtifacts( long pk ) {
        List<Hash> result = new ArrayList<Hash>();
        PreparedStatement findArtifacts = null;
        ResultSet foundArtifacts = null;

        // Read all the related artifacts
        try {
            findArtifacts = connect.prepareStatement( String.format( "select fk_content from template_to_all_content where fk_template='%d'", pk ) );
            foundArtifacts = findArtifacts.executeQuery();
            while ( foundArtifacts.next() ) {
                Hash hash = new Hash( foundArtifacts.getBytes(1) );
                result.add( hash );
            }
        }
        catch ( Exception e ) {
            // Ignore
        }
        finally {
            safeClose( foundArtifacts ); foundArtifacts = null;
            safeClose( findArtifacts ); findArtifacts = null;
        }

        return result;
    }

    private void getRequiredTopArtifacts( long pk, Set<Hash> combined ) {
        PreparedStatement findArtifacts = null;
        ResultSet foundArtifacts = null;
        PreparedStatement findChildren = null;
        ResultSet foundChildren = null;

        // Read all the related artifacts
        try {
            findArtifacts = connect.prepareStatement( String.format( "select fk_content from template_to_content where fk_template='%d'", pk ) );
            foundArtifacts = findArtifacts.executeQuery();
            while ( foundArtifacts.next() ) {
                Hash hash = new Hash( foundArtifacts.getBytes(1) );
                combined.add( hash );
            }

            findChildren = connect.prepareStatement( String.format( "select fk_child from template_to_template where fk_parent='%d'", pk ) );
            foundChildren = findChildren.executeQuery();
            while ( foundChildren.next() ) {
                long fk = foundChildren.getLong( "fk_child" );
                getRequiredTopArtifacts( fk, combined );
            }
        }
        catch ( Exception e ) {
            // Ignore
        }
        finally {
            safeClose( foundArtifacts ); foundArtifacts = null;
            safeClose( foundChildren ); findArtifacts = null;
        }
    }

    private void getRequiredTemplates( long pk, Set<Long> combined ) {
        PreparedStatement findChildren = null;
        ResultSet foundChildren = null;

        // If a template is already added then its children must also already be added.
        if ( combined.contains( pk ) )
            return;

        // Add me
        combined.add( pk );

        // Find and add all my children.
        try {
            findChildren = connect.prepareStatement( String.format( "select fk_child from template_to_template where fk_parent='%d'", pk ) );
            foundChildren = findChildren.executeQuery();
            while ( foundChildren.next() ) {
                long fk = foundChildren.getLong( "fk_child" );
                getRequiredTemplates( fk, combined );
            }
        }
        catch ( Exception e ) {
            // Ignore.
        }
        finally {
            safeClose( foundChildren ); foundChildren = null;
            safeClose( findChildren ); findChildren = null;
        }
    }

    /**
     * Roll up the artifact relationships for all top-level templates. Top-level templates are those referenced
     * directly from a test instance. This roll-up allows SQL queries to map from an artifact to a test instance
     * for artifact result reports.
     */
    public void syncTopTemplateRelationships() {
        // Find all top-level templates.
        PreparedStatement findTemplates = null;
        ResultSet foundTemplates = null;
        PreparedStatement insertArtifact = null;

        if ( read_only )
            return;

        /* Templates always have the same "contents" and "relationships" or their hash would change. This
         * means that the worst synchronization problem can be a crash while we were in the process of adding
         * relationships. An existing relationship will never be wrong.
         */
        try {
            findTemplates = connect.prepareStatement( "select distinct fk_template from test_instance" );
            foundTemplates = findTemplates.executeQuery();
            while ( foundTemplates.next() ) {
                long pk = foundTemplates.getLong( "fk_template" );
                List<Hash> existing = getExistingTopArtifacts( pk );
                Set<Hash> required = new HashSet<Hash>();
                getRequiredTopArtifacts( pk, required );

                // Worst case we missed adding some last time.
                required.removeAll( existing );
                for ( Hash h : required ) {
                    // Need to add the relationship.
                    insertArtifact = connect.prepareStatement( "INSERT INTO template_to_all_content (fk_template, fk_content) VALUES (?,?)" );
                    insertArtifact.setLong(1, pk);
                    insertArtifact.setBinaryStream(2, new ByteArrayInputStream( h.toBytes()) );
                    insertArtifact.executeUpdate();
                }
            }
        }
        catch ( Exception e ) {
            // Ignore.
        }
        finally {
            safeClose( foundTemplates ); foundTemplates = null;
            safeClose( insertArtifact ); insertArtifact = null;
        }
    }

    public void syncTemplateRelationships( Template sync ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        if ( read_only )
            return;

        try {
            for ( Template t : sync.allTemplates ) {
                statement = connect.prepareStatement( String.format( "select fk_parent, fk_child from template_to_template where fk_parent='%d' and fk_child='%d'",
                        sync.getPK(), t.getPK() ) );
                resultSet = statement.executeQuery();
                if ( ! resultSet.isBeforeFirst() ) {
                    // There were no matches. Time to insert. Need to determine if the content exists.
                    safeClose( resultSet ); resultSet = null;
                    safeClose( statement ); statement = null;

                    statement = connect.prepareStatement( "INSERT INTO template_to_template (fk_parent, fk_child) VALUES (?,?)" );
                    statement.setLong(1, sync.getPK());
                    statement.setLong(2, t.getPK());
                    statement.executeUpdate();

                    safeClose( statement ); statement = null;
                }
                else {
                    safeClose( resultSet ); resultSet = null;
                }
            }

            for ( Content a : sync.artifacts ) {
                statement = connect.prepareStatement( "select fk_template, fk_content from template_to_content where fk_template=? and fk_content=?" );
                statement.setLong( 1, sync.getPK() );
                statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
                resultSet = statement.executeQuery();
                if ( ! resultSet.isBeforeFirst() ) {
                    // There were no matches. Time to insert. Need to determine if the content exists.
                    safeClose( resultSet ); resultSet = null;
                    safeClose( statement ); statement = null;

                    statement = connect.prepareStatement( "INSERT INTO template_to_content (fk_template, fk_content) VALUES (?,?)" );
                    statement.setLong(1, sync.getPK());
                    statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
                    statement.executeUpdate();

                    safeClose( statement ); statement = null;
                }
                else {
                    safeClose( resultSet ); resultSet = null;
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't synchronize template relationships, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }
    }

    public void startSyncTestInstance( long pk_test ) {
        // Mark test instances for later cleanup.
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( String.format( "UPDATE test_instance SET synchronized=0 WHERE fk_test=%d", pk_test ) );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            //TODO: handle
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }

    public void stopSyncTestInstance( long pk_test ) {
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( String.format( "DELETE FROM test_instance WHERE synchronized=0 AND fk_test=%d", pk_test ) );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            //TODO: handle
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }

    private long findTestInstance( TestInstance sync, long pk_test ) {
        PreparedStatement find_test_instance = null;
        ResultSet test_instances = null;
        PreparedStatement find_versions = null;
        ResultSet his_versions = null;

        try {
            find_test_instance = connect.prepareStatement( "SELECT pk_test_instance FROM test_instance WHERE fk_template=? AND fk_test=?" );
            find_test_instance.setLong( 1, sync.getTemplate().getPK() );
            find_test_instance.setLong( 2, pk_test );
            test_instances = find_test_instance.executeQuery();
            while ( test_instances.next() ) {
                long pk = test_instances.getLong( "pk_test_instance" );

                // We found a candidate, but need to verify that its version references exactly match.
                List<Long> my_versions = new ArrayList<Long>();
                //	            for ( Version v : sync.getVersions() )
                //	            	my_versions.add( v.getPK() );

                find_versions = connect.prepareStatement( "SELECT fk_version FROM test_instance_to_version WHERE fk_test_instance=?" );
                find_versions.setLong( 1, pk );
                his_versions = find_versions.executeQuery();
                boolean extras = false;
                while ( his_versions.next() && ! extras ) {
                    Long vk = his_versions.getLong( "fk_version" );
                    if ( ! my_versions.contains( vk ) )
                        extras = true;

                    my_versions.remove( vk );	// Note, this is remove by object, not index.
                }

                safeClose( his_versions ); his_versions = null;
                safeClose( find_versions ); find_versions = null;

                if ( extras )
                    continue;

                if ( my_versions.size() == 0 )
                    return pk;	// All versions matched.

                // No match, keep searching.
            }
        }
        catch ( Exception e ) {
            // TODO: handle
            System.err.println( e.getMessage() );
        }
        finally {
            safeClose( test_instances ); test_instances = null;
            safeClose( find_test_instance ); find_test_instance = null;
            safeClose( his_versions ); his_versions = null;
            safeClose( find_versions ); find_versions = null;
        }

        return 0;
    }

    void findResource( Resource R ) {
        PreparedStatement find_resource = null;
        ResultSet resources = null;

        try {
            find_resource = connect.prepareStatement( "SELECT pk_resource, name FROM resource WHERE hash=?" );
            find_resource.setBinaryStream(1, new ByteArrayInputStream(R.hash.toBytes()));
            resources = find_resource.executeQuery();
            while ( resources.next() ) {
                R.pk = resources.getLong( "pk_resource" );
                R.name = resources.getString( "name" );
                return;
            }
        }
        catch ( Exception e ) {
            // TODO: handle
            System.err.println( e.getMessage() );
        }
        finally {
            safeClose( resources ); resources = null;
            safeClose( find_resource ); find_resource = null;
        }

        R.pk = 0;
    }
    
    /**
     * Synchronize the specified test instance belonging to the specified test. The test instance
     * information itself is verified and the hashes are checked against the loaded information. If
     * these match then no further work is done.
     * @param sync The test instance to synchronize.
     * @param pk_test The test that the instance is related to.
     * @return
     */
    public long syncTestInstance( TestInstance sync, long pk_test ) {
        long pk = 0;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        //TODO: Clean up
        if ( read_only ) {
            System.err.println( "------------------------" );
            System.err.println( "Test Instance for Test " + Long.toString( pk_test ) );
            //    		System.err.println( "Template: " + sync.getTemplate().getHash().toString() );
            System.err.println( "Versions:" );
            //    		for ( Version v : sync.getVersions() )
            //    			System.err.println( "\t" + v.getComponent() + ", " + v.getVersion() );
            System.err.println( "------------------------" );
            return pk;
        }

        //        sync.getTemplate().sync();
        //        sync.getDescription().sync();
        //        for ( Version v : sync.getVersions() )
        //            v.sync();

        try {
            pk = findTestInstance( sync, pk_test );

            if ( pk == 0 ) {
                // There were no matches. Time to insert. Need to determine if the content exists.

                // Get the component list associated with the test
                statement = connect.prepareStatement( String.format( "SELECT distinct pk_component" +
                        " FROM component" +
                        " JOIN component_to_test_plan ON component_to_test_plan.fk_component = component.pk_component" +
                        " JOIN test_plan ON test_plan.pk_test_plan = component_to_test_plan.fk_test_plan" +
                        " JOIN test ON test.fk_test_plan = test_plan.pk_test_plan" +
                        " WHERE test.pk_test='%d'", pk_test ) );
                resultSet = statement.executeQuery();
                List<Long> components = new ArrayList<Long>();
                while ( resultSet.next() ) {
                    components.add( resultSet.getLong(1) );
                }

                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;

                statement = connect.prepareStatement( "INSERT INTO test_instance (fk_test, fk_template, fk_description, phase, synchronized) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
                statement.setLong(1, pk_test);
                //                statement.setLong(2, sync.getTemplate().getPK());
                //                statement.setLong(3, sync.getDescription().getPK());
                //TODO: Determine the phase
                statement.setLong(4, 0);
                statement.setInt(5,1);	// Default is synchronized.
                statement.executeUpdate();

                ResultSet keys = statement.getGeneratedKeys();
                if ( keys.next() )
                    pk = keys.getLong( 1 );

                safeClose( statement ); statement = null;

                /*                for ( Version v : sync.getVersions() ) {
                    statement = connect.prepareStatement( String.format( "select fk_test_instance, fk_version from test_instance_to_version where fk_test_instance='%d' and fk_version='%d'",
                            pk, v.getPK() ) );
                    resultSet = statement.executeQuery();
                    if ( ! resultSet.isBeforeFirst() ) {
                        // There were no matches. Time to insert. Need to determine if the content exists.
                        safeClose( resultSet ); resultSet = null;
                        safeClose( statement ); statement = null;

                        boolean primary = components.contains( v.getComponentPK() );
                        statement = connect.prepareStatement( "insert into test_instance_to_version (fk_test_instance, fk_version, is_primary) values (?,?,?)" );
                        statement.setLong(1, pk);
                        statement.setLong(2, v.getPK());
                        statement.setBoolean(3, primary);
                        statement.executeUpdate();

                        safeClose( statement ); statement = null;
                    }
                    else {
                        safeClose( resultSet ); resultSet = null;
                    }
                } */
            }
            else {
                // TODO: Validate the due date and phase
                statement = connect.prepareStatement( "UPDATE test_instance SET synchronized=1, fk_description=? WHERE pk_test_instance=?" );
                //                statement.setLong( 1, sync.getDescription().getPK() );
                statement.setLong( 2, pk );
                statement.executeUpdate();
            }
        }
        catch ( Exception e ) {
            // TODO: handle
            System.err.println( e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        //        if ( sync.getResult() != null )
        //        	reportResult( sync.getTemplate().getHash().toString(), sync.getResult() );

        return pk;
    }

    void reportResult( String hash, boolean result ) {
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( String.format( "call add_run('%s', %b)",
                    hash, result ) );
            statement.execute();
        }
        catch ( Exception e ) {
            // TODO: handle
            System.err.println( e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }

    private static class ResourceInit {
        Hash hash;
        String name;
        String description;
        
        ResourceInit( Hash hash, String name, String description ) {
            this.hash = hash;
            this.name = name;
            this.description = description;
        }
    }
    
    ResourceInit[] resources = {
            new ResourceInit( Hash.fromContent( "person" ), "Person", "This is a person" ),
            new ResourceInit( Hash.fromContent( "machine" ), "Machine", "This is a machine" )
    };
    
    private void addResources() {
        PreparedStatement statement = null;
        try {
            for ( ResourceInit ri : resources ) {
                statement = connect.prepareStatement ( "INSERT INTO resource (hash, name, description) VALUES (?,?,?)" );
                statement.setBinaryStream( 1, new ByteArrayInputStream( ri.hash.toBytes() ) );
                statement.setString( 2, ri.name );
                statement.setString( 3, ri.description );
                statement.execute();
                safeClose( statement ); statement = null;
            }
        }
        catch ( Exception e ) {
            // Ignore
        }
        
        safeClose( statement );
    }
    
    public static class AddResources {
        public static void main( String[] args ) {
            Core core = new Core( 0 );
            core.addResources();
            core.close();
        }
    }
    
    public static class TestTopLevelRelationships {
        public static void main( String[] args ) {
            Core core = new Core( 0 );
            core.syncTopTemplateRelationships();
        }
    }

    public static class TestPruneTemplates {
        public static void main( String[] args ) {
            Core core = new Core( 0 );
            core.pruneTemplates();
        }
    }
}
