package com.pslcl.qa.platform.generator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URLEncoder;
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

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import com.pslcl.qa.platform.Attributes;
import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.generator.TestInstance.Action.ArtifactUses;

/**
 * This class represents the relationship between the program and external resources like
 * filesystems and databases. It also contains all of the synchronization code.
 */
public class Core {
    private static class DBDescribedTemplate {
        long pk;
        @SuppressWarnings("unused")
        long fk_template;
        DescribedTemplate.Key key;
        Hash description;
    }
    private static class DBTestInstance {
        long fk_test;       // INT(11) in test
        @SuppressWarnings("unused")
        String name;        // VARCHAR(100) from test
        @SuppressWarnings("unused")
        String description; // LONGTEXT from test
        String script;      // VARCHAR(200) from test
        long pk_test_instance;
        long fk_described_template;
        long fk_run;
        long pk_template; // added here to avoid a lookup in the executeTestInstance()
    }
    private static class DBTemplateInfo {
        @SuppressWarnings("unused")
        long pk_described_template; // INT(11) in described_template
        byte [] fk_module_set;      // BINARY(32) in described_template
        @SuppressWarnings("unused")
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
    private File artifacts;
    private Map<Long,DBTestInstance> pktiToTI = new HashMap<Long,DBTestInstance>();
    private Map<Long,List<DBTestInstance>> pktToTI = new HashMap<Long,List<DBTestInstance>>();
    private Map<Long,DBDescribedTemplate> pkToDT = new HashMap<Long,DBDescribedTemplate>();
    private Map<DescribedTemplate.Key,DBDescribedTemplate> keyToDT = new HashMap<DescribedTemplate.Key,DBDescribedTemplate>();
    private Map<Long,Long> dtToTI = new HashMap<Long,Long>();
    
    // tech spec term of "test run" is this test instance, from table test_instance
    private void loadTestInstances() {
        if (connect == null) {
            System.out.println("<internal> Core.loadTestInstances() finds no database connection and exits");
            return;
        }
        
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
        if (connect == null) {
            System.out.println("<internal> Core.loadHashes() finds no database connection and exits");
            return;
        }
        
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_described_template, fk_module_set, fk_template, hash, description_hash FROM described_template JOIN template ON fk_template = pk_template" );
            while ( resultSet.next() ) {
                DBDescribedTemplate dbTemplate = new DBDescribedTemplate();
                dbTemplate.pk = resultSet.getLong( "pk_described_template" );
                dbTemplate.fk_template = resultSet.getLong( "fk_template" );
                dbTemplate.key = new DescribedTemplate.Key( new Hash( resultSet.getBytes( "hash" ) ), new Hash( resultSet.getBytes( "fk_module_set" ) ) );
                dbTemplate.description = new Hash( resultSet.getBytes( "description_hash" ) );

                pkToDT.put( dbTemplate.pk, dbTemplate );

                if ( keyToDT.containsKey( dbTemplate.key ) ) {
                    safeClose( resultSet ); resultSet = null;
                    safeClose( statement ); statement = null;
                    throw new Exception( "Duplicate DescribedTemplate.Key " + dbTemplate.pk + " " + dbTemplate.key.getTemplateHash().toString() + ":" + dbTemplate.key.getModuleHash().toString() );
                }
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
    
    public static class Config {
        private String db_host;
        private Integer db_port;
        private String db_user;
        private String db_password;
        private String db_schema;
        private String artifacts_dir;
        private String generators_dir;
        private String shell;
        
        Config() {
            try {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("JavaScript");
                File fs = new File("portal/config/config.js");
                Bindings binding = engine.createBindings();
                binding.put("process",  null );
                binding.put("env",  System.getenv() );
                binding.put("module", new NodeModule() );
                
                engine.eval( new FileReader(fs), binding );
                this.db_host = (String) engine.eval( "config.mysql.host;", binding );
                this.db_port = (Integer) engine.eval( "config.mysql.port;", binding );
                this.db_user = (String) engine.eval( "config.mysql.user;", binding );
                this.db_password = (String) engine.eval( "config.mysql.password;", binding );
                this.db_schema = (String) engine.eval( "config.mysql.db;", binding );
                this.artifacts_dir = (String) engine.eval( "config.artifacts_dir;", binding );
                this.generators_dir = (String) engine.eval( "config.generators_dir;", binding );
                this.shell = (String) engine.eval( "config.shell;", binding );
            }
            catch ( Exception e ) {
                System.err.println( "ERROR: Config exception: " + e.getMessage() );
            }
        }
        
        public String dbHost() {
            return db_host;
        }
        
        public Integer dbPort() {
            return db_port;
        }
        
        public String dbUser() {
            return db_user;
        }
        
        public String dbPassword() {
            return db_password;
        }
        
        public String dbSchema() {
            return db_schema;
        }
        
        public String dirArtifacts() {
            return artifacts_dir;
        }
        
        public String dirGenerators() {
            return generators_dir;
        }
        
        public String shell() {
            return shell;
        }
    };
    
    Config config = new Config();
    
    public Config getConfig() {
        return config;
    }
    
    public Core( long pk_test ) {
        this.pk_target_test = pk_test;
        
        
        String dir = config.dirArtifacts();
        if ( dir != null )
            this.artifacts = new File(dir);

        if ( ! this.artifacts.isDirectory() )
            //noinspection ResultOfMethodCallIgnored
            this.artifacts.mkdirs();
        
        openDatabase();
        
        if (connect == null) {
            System.err.println( "Core constructor fails without database connection");
        } else {
            /* Load the description and template hashes */
            loadHashes();
            
            loadTestInstances();
        }
    }

    /**
     * Close the core object, releasing any resources.
     */
    public void close() {
        closeDatabase();
    }

    private static class NodeModule {
        @SuppressWarnings("unused")
        public Object exports;
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
            read_only = false;
            String user = config.dbUser();
            String password = config.dbPassword();
            if ( user == null || password == null ) {
                user = "guest";
                password = "";
                read_only = true;
            }

            Class.forName("com.mysql.jdbc.Driver"); // required at run time only for .getConnection(): mysql-connector-java-5.1.35.jar
            String connectstring = String.format( "jdbc:mysql://%s:%d/%s?user=%s&password=%s", config.dbHost(), config.dbPort(), config.dbSchema(), user, password );
            connect = DriverManager.getConnection(connectstring);
        }
        catch ( Exception e ) {
            read_only = true;
            System.err.println( "ERROR: Could not open database connection, " + e.getMessage() );
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
    public void executeTestInstance(long testInstanceNumber) {
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
                resultSet = statement.executeQuery( "SELECT pk_described_template, fk_module_set, pk_template, description_hash, hash, steps, enabled FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance = " + str_fkTestInstanceNumber + " AND pk_template = " + str_fkTemplate );
                // exactly one resultSet (because we required test_instance.fk_described_template to match described_template.pk_described_template)
                if ( resultSet.next() ) {
                    DBTemplateInfo dbtemplateinfo = new DBTemplateInfo();
                    dbtemplateinfo.pk_described_template = resultSet.getLong( "pk_described_template" ); // table entry will not be null
                    dbtemplateinfo.fk_module_set = resultSet.getBytes("fk_module_set");
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
                    System.out.println( "executeTestInstance() finds module set: " + dbtemplateinfo.fk_module_set);
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
     * Return a list of artifact providers.
     * @return The list of provider class names.
     */
    public List<String> readArtifactProviders() {
        List<String> result = new ArrayList<String>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT * FROM artifact_provider" );
            while ( resultSet.next() ) {
                String name = resultSet.getString( "classname" );
                result.add( name );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not read artifact providers, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }
    
    /**
     * Add a test plan to the database.
     * @param name The name of the test plan.
     * @param description The description of the test plan.
     * @return The primary key of the new test plan, or zero if there is an error or in read-only mode. If the test plan already exists then
     * the existing primary key is returned;
     */
    public long addTestPlan( String name, String description ) {
        PreparedStatement statement = null;
        long pk = findTestPlan( name, description );

        // This will work in read-only mode to return an existing module.
        if ( pk != 0 || read_only )
            return pk;

        try {
            statement = connect.prepareStatement( "INSERT INTO test_plan (name, description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS );
            statement.setString( 1, name );
            statement.setString( 2, description );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add test plan, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    /**
     * Add a test  to the database.
     * @param pk_test_plan The primary key of the test plan.
     * @param name The name of the test.
     * @param description The description of the test.
     * @param script The script of the test.
     * @return The primary key of the new test, or zero if there is an error or in read-only mode. If the test already exists then
     * the existing primary key is returned;
     */
    public long addTest( long pk_test_plan, String name, String description, String script ) {
        PreparedStatement statement = null;
        long pk = findTest( pk_test_plan, name, description, script );

        // This will work in read-only mode to return an existing module.
        if ( pk != 0 || read_only )
            return pk;

        try {
            statement = connect.prepareStatement( "INSERT INTO test (fk_test_plan, name, description, script) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
            statement.setLong( 1,  pk_test_plan );
            statement.setString( 2, name );
            statement.setString( 3, description );
            statement.setString( 4,  script );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add test, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    /**
     * Add a module to the database.
     * @param module The module to add.
     * @return The primary key of the new module, or zero if there is an error or in read-only mode. If the module already exists then
     * the existing primary key is returned;
     */
    public long addModule( Module module ) {
        PreparedStatement statement = null;
        long pk = findModule( module );

        // This will work in read-only mode to return an existing module.
        if ( pk != 0 || read_only )
            return pk;

        String attributes = new Attributes( module.getAttributes()).toString();
        //TODO: Release date, actual release date, status, order all need to be added.
        try {
            statement = connect.prepareStatement( "INSERT INTO module (organization, name, attributes, version, sequence) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
            statement.setString( 1, module.getOrganization() );
            statement.setString( 2, module.getName() );
            statement.setString( 3, attributes );
            statement.setString( 4, module.getVersion() );
            statement.setString( 5, module.getSequence() );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add module, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
    }

    /**
     * Delete a module.
     * @param pk_module The primary key of the module to delete.
     */
    public void deleteModule( long pk_module ) {
        PreparedStatement statement = null;

        if ( read_only )
            return;

        try {
            statement = connect.prepareStatement( "DELETE FROM module WHERE pk_module=?" );
            statement.setLong( 1, pk_module );
            statement.executeUpdate();
            safeClose( statement ); statement = null;
            
            statement = connect.prepareStatement( "DELETE FROM artifact WHERE merged_from_module=?" );
            statement.setLong( 1,  pk_module );
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not delete module, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
    }
    
    /**
     * Delete previous builds of the same version as this module.
     * @param module The module that previous versions of should be deleted. It is required
     * that this module not be already added to the database.
     */
    public void deletePriorVersion( Module module ) {
        long pk;
        while ( (pk = findModuleWithoutSequence(module)) != 0 )
            deleteModule( pk );
    }
    
    /**
     * Add content given a hash and inputstream. If the contents exist then the file is assumed to be correct
     * and the database is still updated.
     * @param h The hash of the file.
     * @param is An input stream for the contents.
     */
    public Hash addContent( InputStream is ) {
        File tmp;
        
        try {
            tmp = File.createTempFile( "artifact",  "hash" );
            FileOutputStream os = new FileOutputStream( tmp );
            IOUtils.copy( is, os );
            os.close();
        }
        catch ( Exception e ) {
            // Cannot even determine the hash, so we don't know if it has already been added or not.
            System.err.println( "ERROR: Could not add content, " + e.getMessage() );
            return null;
        }
        
        Hash h = Hash.fromContent( tmp );
        File target = new File( this.artifacts, h.toString() );
        if ( target.exists() ) {
            FileUtils.deleteQuietly( tmp );
            return h;   // Filesystem and DB must match, file already cached.
        }
        
        // If read-only and it doesn't exist, then it cannot be added.
        if ( read_only )
            return null;

        PreparedStatement statement = null;
        try {
            // Move the file to the cache
            FileUtils.moveFile( tmp, target );
            
            statement = connect.prepareStatement( "INSERT INTO content (pk_content, is_generated) VALUES (?,1)" );
            statement.setBinaryStream(1, new ByteArrayInputStream(h.toBytes()));
            statement.executeUpdate();
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add content, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }
        
        return h;
    }
    
    /**
     * Return a file for content that exists in the cache.
     * @param h The hash of the file to return.
     * @return A file if it exists, null otherwise.
     */
    public File getContentFile( Hash h ) {
        File f = new File( artifacts, h.toString() );
        if ( f.exists() )
            return f;
        
        return null;
    }
    
    /**
     * Add an artifact to a particular module and configuration, given a name and hash of the content.
     * @param pk_module The module the artifact relates to.
     * @param configuration The configuration the artifact is part of.
     * @param name The name of the artifact.
     * @param mode The POSIX mode of the artifact.
     * @param content The hash of the file content, which must already exist in the system.
     * @param merge_source True of the artifact is associated with a merged module.
     * @param derived_from_artifact If non-zero, the primary key of the artifact that this artifact is derived from (for example, an archive file).
     * @param merged_from_module If non-zero, the primary key of the module that this artifact is merged from. 
     * @return
     */
    public long addArtifact( long pk_module, String configuration, String name, int mode, Hash content, boolean merge_source, long derived_from_artifact, long merged_from_module ) {
        PreparedStatement statement = null;
        long pk = 0;

        if ( read_only )
            return pk;

        try {
            if ( merged_from_module != 0 )
                statement = connect.prepareStatement( "INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact, merged_from_module) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
            else
                statement = connect.prepareStatement( "INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
               
            statement.setLong( 1, pk_module );
            statement.setBinaryStream(2, new ByteArrayInputStream(content.toBytes()));
            statement.setString( 3, configuration );
            statement.setString( 4, name );
            statement.setInt( 5,  mode );
            statement.setBoolean( 6, merge_source );
            statement.setLong( 7, derived_from_artifact );
            if ( merged_from_module != 0 )
                statement.setLong( 8, merged_from_module );
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if ( keys.next() )
                pk = keys.getLong( 1 );
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Could not add artifact to module, " + e.getMessage() );
        }
        finally {
            safeClose( statement ); statement = null;
        }

        return pk;
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

        try {
            statement = connect.prepareStatement( "DELETE content FROM content LEFT JOIN artifact ON content.pk_content = artifact.fk_content WHERE artifact.fk_content IS NULL AND content.is_generated=0" );
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

    /**
     * This class represents a module that is backed by the core database. Operations on the module will refer
     * to database content.
     */
    private static class DBModule implements Module {
        private Core core;
        private long pk;
        private String organization;
        private String name;
        private Attributes attributes;
        private String version;
        private String sequence;

        
        DBModule( Core core, long pk, String organization, String name, String attribute_string, String version, String sequence ) {
            this.core = core;
            this.pk = pk;
            this.organization = organization;
            this.name = name;
            this.attributes = new Attributes(attribute_string);
            this.version = version;
            this.sequence = sequence;
        }
        
        @Override
        public String getOrganization() {
            return organization;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public Map<String, String> getAttributes() {
            return attributes.getAttributes();
        }

       @Override
        public String getSequence() {
            return sequence;
        }

        @Override
        public List<Artifact> getArtifacts() {
            return core.getArtifacts( pk, null, null );
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern) {
            return core.getArtifacts( pk, namePattern, null );
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern, String configuration) {
            return core.getArtifacts( pk, namePattern, configuration );
        }
    }
    
    /**
     * This class represents an artifact that is represented in the database. It is the class
     * returned to generators.
     */
    private static class DBArtifact implements Artifact {
        private Core core;
        private long pk;
        private Module module;
        private String configuration;
        private String name;
        private int mode;
        private Hash hash;
        
        /**
         * Construct an artifact associated with a component, name, version, platform and variant. The content
         * associated with the artifact is passed as a hash.
         * @param core The core managing the database.
         * @param pk The primary key of the artifact.
         * @param module The module the artifact belongs to.
         * @param configuration The configuration the artifact belongs to.
         * @param name The name of the artifact.
         * @param mode The POSIX mode of the artifact.
         * @param hash The hash of the artifact contents.
         */
        public DBArtifact( Core core, long pk, Module module, String configuration, String name, int mode, Hash hash ) {
            this.core = core;
            this.pk = pk;
            this.module = module;
            this.configuration = configuration;
            this.name = name;
            this.mode = mode;
            this.hash = hash;
        }

        public long getPK() {
            return pk;
        }
        
        @Override
        public Module getModule() {
            return module;
        }

        @Override
        public String getConfiguration() {
            return configuration;
        }

        @Override
        public String getName() {
            return name;
        }

        @SuppressWarnings("unused")
        public String getEncodedName() {
            try {
                return URLEncoder.encode(name, "UTF-8");
            }
            catch ( Exception e ) {
                // This should never happen, as UTF-8 is a required charset.
                return "error";
            }
        }

        @Override
        public Content getContent() {
            return new DBContent( core, hash );
        }

        @Override
        public int getPosixMode() {
            return mode;
        }
        
        //@Override
        //public String getValue( Template template ) {
        //    return module.getOrganization() + "#" + module.getName() + " " + getEncodedName() + " " + hash.toString();
        //}
    }
    
    private static class DBContent implements Content {
        Core core;
        Hash hash;

        DBContent( Core core, Hash hash ) {
            this.hash = hash;
        }

        @Override
        public Hash getHash() {
            return hash;
        }

        @Override
        public String getValue( Template template ) {
            return getHash().toString();
        }

        @Override
        public InputStream asStream() {
            File f = core.getContentFile( hash );
            try {
                return new FileInputStream( f );
            }
            catch ( Exception e ) {
                // Ignore
            }
            
            return null;
        }

        @Override
        public byte[] asBytes() {
            File f = core.getContentFile( hash );
            try {
                return FileUtils.readFileToString( f ).getBytes();
            }
            catch ( Exception e ) {
                // Ignore
            }
            
            return null;
        }
    }
    
    /**
     * Return a set of all modules known to the database.
     * @return
     */
    public Iterable<Module> createModuleSet() {
        List<Module> set = new ArrayList<Module>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_module, organization, name, attributes, version, sequence FROM module" );
            while ( resultSet.next() ) {
                DBModule M = new DBModule( this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6) );
                set.add( M );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: createModuleSet() exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    /**
     * Create a set of all modules that match the specified organization and module name.
     * @param organization The organizations to filter on.
     * @param name The module name to filter on.
     * @return A set of modules.
     */
    Iterable<Module> createModuleSet(String organization, String name) {
        List<Module> set = new ArrayList<Module>();
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.createStatement();
            resultSet = statement.executeQuery( String.format( "SELECT pk_module, organization, name, attributes, version, sequence" +
                    " FROM module" +
                    " WHERE organization = " + organization + " AND name = '" + name + "'" ) );
            while ( resultSet.next() ) {
                DBModule M = new DBModule( this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6) );
                set.add( M );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: createModuleSet() exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    /**
     * Return a set of all artifacts that the specified artifact depends on. Dependencies are stored in a
     * corresponding artifact with '.dep' added. These are typically merged artifacts so they are not typically
     * distributed.
     * The '.dep' file contains artifact references, one per line, with the following formats:
     * First, a line with a single field. This field is a name regex (defined my MySQL) for other artifacts in the same module.
     * Second, a line with two fields separated by a comma. The first field is a module reference and the second is a version.
     * The module reference is specified as 'org#module#attributes' and the version as 'version/configuration'. In all fields
     * a dollar sign can be used to substitute the value from the artifact that dependencies are being found from. This
     * means that '$#$#$,$/,$.dep' is used to search for the '.dep' file. If any of the fields is empty then it will not be used
     * in the search. If attributes are specified then they must match exactly (and be formatted correctly including URL-encoding).
     * If the dollar sign is used in attributes and additional attributes are specified then they will be reformatted correctly.
     * Each artifact name is only accepted once, with the first match taking priority. Artifacts are ordered by sorting
     * organization, module, attributes, version (all increasing order) and sequence (decreasing order).
     * @param artifact The artifact to search dependencies for. The corresponding '.dep' file must be in the
     * same module as the artifact (likely by being merged).
     * @return A set of dependent artifacts.
     */
    Iterable<Artifact> findDependencies( Artifact artifact ) {
        List<Artifact> set = new ArrayList<Artifact>();
        Statement statement = null;
        ResultSet resultSet = null;

        // Artifact searches are always done from the perspective of merged modules.
        long pk = findModule( artifact.getModule() );
        if ( pk == 0 )
            return set; // This should not happen
        
        try {
            // We are willing to find any artifact from any merged module
            statement = connect.createStatement();
            String query = String.format( "SELECT artifact.fk_content" +
                    " FROM artifact" +
                    " WHERE artifact.fk_module = %d AND artifact.name = '%s.dep'", pk, artifact.getName() );
            resultSet = statement.executeQuery( query );
            if ( resultSet.next() ) {
                // We only care about the first match that we find.
                Hash hash = new Hash( resultSet.getBytes(1) );
                File f = new File( this.artifacts, hash.toString() );
                LineIterator iterator = new LineIterator( new FileReader( f ) );
                
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
                
                // Each line is a dependency. The first field is a name regex, the second (optional) is a version.
                while ( iterator.hasNext() ) {
                    String line = iterator.next();
                    String[] fields = line.split(",");
                    if ( fields.length == 1 ) {
                        statement = connect.createStatement();
                        query = String.format( "SELECT artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" +
                                " FROM artifact" +
                                " WHERE artifact.fk_module = %d AND artifact.name REGEXP '%s" + "'", pk, fields[0] );
                        resultSet = statement.executeQuery( query );
                        while ( resultSet.next() ) {
                            Module mod = artifact.getModule();
                            Artifact A = new DBArtifact( this, resultSet.getLong(1), mod, resultSet.getString(7), resultSet.getString(8), resultSet.getInt(9), new Hash( resultSet.getBytes(10) ) );
                            set.add( A );
                        }
                    }
                    else if ( fields.length == 3 ) {
                        statement = connect.createStatement();

                        String[] mod_fields = fields[0].split("#");
                        String[] ver_fields = fields[1].split("/");
                        
                        String organization = mod_fields[0];
                        String module = mod_fields.length > 1 ? mod_fields[1] : "";
                        String attributes = mod_fields.length > 2 ? mod_fields[2] : "";
                        String version = ver_fields[0];
                        String configuration = ver_fields.length > 1 ? ver_fields[1] : "";
                        
                        organization = organization.replace( "$", artifact.getModule().getOrganization() );
                        module = module.replace( "$", artifact.getModule().getName() );
                        attributes = attributes.replace( "$", artifact.getModule().getAttributes().toString() );
                        attributes = new Attributes( attributes ).toString();
                        version = version.replace( "$", artifact.getModule().getVersion() );
                        configuration = configuration.replace( "$", artifact.getConfiguration() );

                        
                        String organization_where = organization.length() > 0 ? " AND module.organization='" + organization + "'" : "";
                        String module_where = module.length() > 0 ? " AND module.name='" + module + "'" : "";
                        String attributes_where = attributes.length() > 0 ? " AND module.attributes='" + attributes + "'" : "";
                        String version_where = version.length() > 0 ? " AND module.version='" + version + "'" : "";
                        String configuration_where = configuration.length() > 0 ? " AND module.configuration='" + configuration + "'" : "";
                        
                        query = String.format( "SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.sequence, artifact.pk_artifact, artifact.name, artifact.configuration, artifact.mode, artifact.fk_content" +
                                " FROM artifact" +
                                " JOIN module ON module.pk_module = artifact.fk_module" +
                                " WHERE artiface.merge_source=0 AND artifact.name REGEXP '%s'%s%s%s%s%s" +
                                " ORDER BY module.organization, module.name, module.attributes, module.version, module.configuration, module.sequence DESC", fields[2], organization_where, module_where, attributes_where, version_where, configuration_where );
                        resultSet = statement.executeQuery( query );
                        Set<String> found = new HashSet<String>();
                        while ( resultSet.next() ) {
                            String artifact_name = resultSet.getString(8);
                            if ( found.contains( artifact_name ) )
                                continue;
                            
                            DBModule dbmod = new DBModule(this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6) );
                            Artifact A = new DBArtifact( this, resultSet.getLong(7), dbmod, resultSet.getString(8), resultSet.getString(9), resultSet.getInt(10), new Hash( resultSet.getBytes(11) ) );
                            set.add( A );
                        }
                    }
                    else
                        throw new Exception( "ERROR: Illegal line (" + line + ") in " + artifact.getName() + ".dep" );
                }
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: findDependencies() exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    /**
     * Return a set of artifacts given the specified requirements. First, only attributes from modules with at least the specified
     * attributes are included. Second, if specified, the artifacts must come from the given configuration. The names specified are patterns
     * acceptable to MySQL regex search.
     * The result set includes a list of sets of matching artifacts. For each element in the list the array of results contains the
     * artifact that matches the parameter in the same position, all of which will come from the same module.
     * @param required A parameter set or null. Any module considered for artifacts must contain at least these attributes.
     * @param configuration the configuration to check, or null. 
     * @param name Artifact names, including MySQL REGEXP patterns.
     * @return
     */
    public Iterable<Artifact[]> createArtifactSet( Attributes required, String configuration, String ... name ) {
        Statement statement = null;
        ResultSet resultSet = null;
        List<Artifact[]> set = new ArrayList<Artifact[]>();
        Map<Long,DBModule> moduleMap = new HashMap<Long,DBModule>();
        Map<Long,Artifact[]> artifactMap = new HashMap<Long, Artifact[]>();
        
        for ( int name_index = 0; name_index < name.length; name_index++ ) {
            String artifact_name = name[ name_index ];
            try {
                statement = connect.createStatement();
                String configuration_match = "";
                if ( configuration != null )
                    configuration_match = " AND artifact.configuration='" + configuration + "'";
                
                String queryStr = String.format( "SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" +
                        " FROM artifact" +
                        " JOIN module ON module.pk_module = artifact.fk_module" +
                        " WHERE artifact.merge_source=0 AND artifact.name REGEXP '%s'%s" +
                        " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC", artifact_name, configuration_match );
                resultSet = statement.executeQuery( queryStr );
                while ( resultSet.next() ) {
                    // Verify that if requested, the module/version has all required attributes.
                    Attributes possesses = new Attributes( resultSet.getString(4) );
                    if ( required != null ) {
                        boolean mismatch = false;
                        for ( Map.Entry<String,String> entry : required.getAttributes().entrySet() ) {
                            if ( possesses.get( entry.getKey() ) != entry.getValue() ) {
                                mismatch = true;
                                break;
                            }
                        }
                        
                        if ( mismatch )
                            continue; // Move to the next result
                    }
                    
                    long pk_found = resultSet.getLong(1);
                    Artifact[] artifacts;
                    if ( artifactMap.containsKey( pk_found ) )
                        artifacts = artifactMap.get( pk_found );
                    else {
                        artifacts = new Artifact[ name.length ];
                        artifactMap.put( pk_found, artifacts );
                    }

                    DBModule module = null;
                    if ( moduleMap.containsKey( pk_found ) )
                        module = moduleMap.get( pk_found );
                    else {
                        module = new DBModule( this, pk_found, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6) );
                        moduleMap.put( pk_found, module );
                    }
                    
                    if ( artifacts[ name_index ] == null ) {
                        Artifact A = new DBArtifact( this, resultSet.getLong(7), module, resultSet.getString(8), resultSet.getString(9), resultSet.getInt(10), new Hash( resultSet.getBytes(11) ) ); 
                        artifacts[ name_index ] = A;
                    }
                }
            }
            catch ( Exception e ) {
                System.err.println( "ERROR: createArtifactSet() exception " + e.getMessage() );
                e.printStackTrace( System.err );
            }
            finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        }

        for ( Long pk : artifactMap.keySet() ) {
            Artifact[] list = artifactMap.get(pk);
            int found = 0;
            for ( int i = 0; i < list.length; i++ )
                if ( list[i]  != null )
                    found += 1;

            if ( found == name.length )
                set.add( list );
        }

        return set;
    }

    /**
     * Get the list of generators configured for all the tests.
     * @return A map where the keys are the primary keys of the tests and the values are the string to run the generator.
     */
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
            System.err.println( "ERROR: getGenerators() exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return result;
    }

    /**
     * Return artifacts associated with a particular module (including version). Both name and configuration optional.
     * @param pk_module The primary key of the module to return artifacts for.
     * @param name The name, which can include MySQL REGEXP patterns and is also optional.
     * @param configuration The configuration, or null to include all.
     * @return The list of matching artifacts.
     */
    public List<Artifact> getArtifacts(long pk_module, String name, String configuration ) {
        Statement statement = null;
        ResultSet resultSet = null;
        List<Artifact> set = new ArrayList<Artifact>();
        String name_match = "";
        String configuration_match = "";
        String separator = "";
        String intro = "";
        
        if ( name != null )
            name_match = "artifact.name REGEXP '" + name + "'";

        if ( configuration != null )
            configuration_match = "artifact.configuration = '" + configuration + "'";
        
        if ( name != null && configuration != null )
            separator = " AND ";
        
        if ( name != null || configuration != null )
            intro = " AND ";
        
        try {
            Map<Long,DBModule> modules = new HashMap<Long,DBModule>();
            
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content, artifact.merged_from_module" +
                    " FROM artifact" +
                    " JOIN module ON module.pk_module = artifact.fk_module" +
                    " WHERE module.pk_module = " + pk_module +
                    intro + name_match + separator + configuration_match +
                    " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC");
            while ( resultSet.next() ) {
                // Ignore dtf_test_generator artifacts that are merged from other modules
                int merged_from_module = resultSet.getInt( 12 );
                if ( merged_from_module > 0 && configuration.equals( "dtf_test_generator" ) )
                    continue;
                
                DBModule module = null;
                long pk_found = resultSet.getLong(1);
                
                if ( modules.containsKey( pk_found ) )
                    module = modules.get( pk_found );
                else {
                    module = new DBModule( this, pk_found, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6) );
                    modules.put( pk_found, module );
                }
                
                if ( set.contains( resultSet.getString( 8 )) )
                    continue;
                
                Artifact A = new DBArtifact( this, resultSet.getLong(7), module, resultSet.getString(8), resultSet.getString(9), resultSet.getInt(10), new Hash( resultSet.getBytes(11) ) ); 
                set.add( A );
            }

        }
        catch ( Exception e ) {
            System.err.println( "ERROR: getArtifacts() exception " + e.getMessage() );
            e.printStackTrace( System.err );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return set;
    }

    /**
     * Return whether the specified version is associated with the current core target test. This is true
     * if there is a relationship from the test through the test plan to the component and version.
     * @param version
     * @return
     */
    boolean isAssociatedWithTest( Module module ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        long pk = findModule( module );
        if ( pk == 0 )
            return false;
        
        try {
            statement = connect.prepareStatement( "SELECT test.pk_test" +
                    " FROM test" +
                    " JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan" +
                    " JOIN module_to_test_plan ON module_to_test_plan.fk_test_plan = test_plan.pk_test_plan" +
                    " WHERE test.pk_test = ? AND module_to_test_plan.fk_module = ?" );
            statement.setLong( 1, pk_target_test );
            statement.setLong( 2, pk );
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

    public long findTestPlan( String name, String description ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT test_plan.pk_test_plan" +
                    " FROM test_plan" +
                    " WHERE test_plan.name = '" + name + "'" +
                    " AND test_plan.description = '" + description + "'" );
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "test_plan.pk_test_plan" );
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

    long findTest( long pk_test_plan, String name, String description, String script ) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connect.prepareStatement( "SELECT test.pk_test" +
                    " FROM test" +
                    " WHERE test.fk_test_plan = ?" +
                    " AND test.name = ?" +
                    " AND test.description = ? " +
                    " AND test.script = ?" );
            statement.setLong( 1, pk_test_plan );
            statement.setString( 2, name );
            statement.setString( 3, description );
            statement.setString( 4,  script );

            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "test.pk_test" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find test, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

    public long findModule( Module module ) {
        // Short-cut the lookup if it is one of our modules.
        if ( module instanceof DBModule ) {
            DBModule dbmod = (DBModule) module;
            if ( dbmod.pk != 0 )
                return dbmod.pk;
        }
        
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        String attributes = new Attributes( module.getAttributes() ).toString();
        try {
            statement = connect.prepareStatement( "SELECT module.pk_module" +
                    " FROM module" +
                    " WHERE module.organization = '" + module.getOrganization() + "'" +
                    " AND module.name = '" + module.getName() + "'" +
                    " AND module.attributes = '" + attributes + "'" +
                    " AND module.version = '" + module.getVersion() + "'" +
                    " AND module.sequence = '" + module.getSequence() + "'" );
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "module.pk_module" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find module, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

    public long findModuleWithoutSequence( Module module ) {
        // Short-cut the lookup if it is one of our modules.
        if ( module instanceof DBModule ) {
            DBModule dbmod = (DBModule) module;
            if ( dbmod.pk != 0 )
                return dbmod.pk;
        }
        
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        String attributes = new Attributes( module.getAttributes() ).toString();
        try {
            statement = connect.prepareStatement( "SELECT module.pk_module" +
                    " FROM module" +
                    " WHERE module.organization = '" + module.getOrganization() + "'" +
                    " AND module.name = '" + module.getName() + "'" +
                    " AND module.attributes = '" + attributes + "'" +
                    " AND module.version = '" + module.getVersion() + "'" );
            resultSet = statement.executeQuery();
            if ( resultSet.isBeforeFirst() ) {
                resultSet.next();
                return resultSet.getLong( "module.pk_module" );
            }
        }
        catch ( Exception e ) {
            System.err.println( "ERROR: Couldn't find module, " + e.getMessage() );
        }
        finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        return 0;
    }

/*    public void syncGeneratedContent( Content sync ) {
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
*/
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
                    DBArtifact artifact = (DBArtifact) iter.next();
                    
                    try {                       
                        statement = connect.prepareStatement( "INSERT INTO artifact_to_dt_line (fk_artifact, fk_dt_line, is_primary, reason) VALUES (?,?,?,?)" );
                        statement.setLong( 1, artifact.getPK() );
                        statement.setLong( 2, linepk );
                        statement.setInt( 3, au.getPrimary() ? 1 : 0 );
                        statement.setString( 4, au.getReason() );
                        statement.executeUpdate();
                        safeClose( statement ); statement = null;
                    }
                    catch ( Exception e ) {
                        System.err.println( "ERROR: Failed to relate artifact to line, " + e.getMessage() );
                    }
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
        // TODO: Figure out if this logic is correct. Doesn't appear to be.
        for ( DescribedTemplate child : dt.getDependencies() ) {
            @SuppressWarnings("unused")
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
                statement = connect.prepareStatement( "INSERT INTO described_template (fk_module_set, fk_template, description_hash, synchronized) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS );
                statement.setBinaryStream(1, new ByteArrayInputStream( dt.getKey().getModuleHash().toBytes() ) );
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
                
                statement = connect.prepareStatement( "SELECT pk_described_template FROM described_template WHERE fk_module_set=? AND fk_template=?" );
                statement.setBinaryStream(1, new ByteArrayInputStream( dt.getKey().getModuleHash().toBytes() ) );
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
            // TODO: Figure out if this is correct.
            @SuppressWarnings("unused")
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
                
                // Insert all of the module references
                List<TestInstance.Action> actions = ti.getActions();
                for ( TestInstance.Action action : actions ) {
                    ArtifactUses au = action.getArtifactUses();
                    if ( au == null )
                        continue;
                    
                    Iterator<Artifact> iter = au.getArtifacts();
                    while ( iter.hasNext() ) {
                        Artifact artifact = iter.next();
                        
                        try {
                            long pk_module = findModule( artifact.getModule() );
                            statement2 = connect.prepareStatement( "INSERT INTO module_to_test_instance ( fk_module, fk_test_instance ) VALUES (?,?)" );
                            statement2.setLong( 1, pk_module );
                            statement2.setLong( 2, ti.pk );
                            statement2.execute();
                            safeClose( statement2 ); statement2 = null;
                        }
                        catch ( Exception e ) {
                            // Ignore, since many times this will be a duplicate.
                        }
                    }
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

    public void reportResult( String hash, boolean result ) {
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
