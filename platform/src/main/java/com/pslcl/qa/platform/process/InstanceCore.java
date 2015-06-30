package com.pslcl.qa.platform.process;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

public class InstanceCore {

    // sub classes

    /**
     * Relevant data base info for this test instance
     */
    private class DBTestInstance {
      //long pk_test_instance;  note: this is held in parent class as InstanceCore.pk_test_instance
        Date due_date;              // DATETIME in test_instance TODO: double check needed Java type
        int phase;                  // INT in test_instance TODO: double check needed Java type
        boolean syncronized;        // BOOLEAN synchronized in test_instance TODO: double check needed Java type

        long fk_run;                // INT(11) in run
 
        long fk_described_template; // INT(11) in described_template
        long fk_template;           // INT(11) in described_template
        byte[] fk_version_set;      // BINARY(32) 32 byte array in described_template; use ResultSet.getBytes()
        byte[] description_hash;    // BINARY(32) 32 byte array in described_template
        
        byte [] hash;               // BINARY(32) 32 byte array in template 
        boolean enabled;            // BOOLEAN in template
        String steps;               // MEDIUMTEXT in template

    }

    
    // class members
    
    /**
     * The connection to the database.
     */
    private Connection connect = null;
    private boolean read_only = true;
    private long pk_test_instance;
    private DBTestInstance dbTestInstance = null;
    
    // private methods
    
    private void loadTestInstanceData() {
        if (connect == null) {
            System.out.println("<internal> InstanceCore.loadTestInstance() finds no database connection and exits");
            return;
        }
        
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connect.createStatement();
            String strINum = String.valueOf(pk_test_instance);
            resultSet = statement.executeQuery( "SELECT fk_described_template, fk_run, fk_version_set, description_hash, fk_template, hash, steps, enabled FROM test_instance JOIN described_template ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance =" + strINum );
            // everything is 1:1 relationship, so resultSet has exactly 1 or 0 entry
            
            
//            fk_version_set description_hash hash steps enabled
//            
//            resultSet = statement.executeQuery( "SELECT pk_described_template, fk_version_set, pk_template, description_hash, hash, steps, enabled FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance = " + str_fkTestInstanceNumber + " AND pk_template = " + str_fkTemplate );

            
            
            if ( resultSet.next() ) {
                dbTestInstance = new DBTestInstance();
                dbTestInstance.fk_described_template = resultSet.getLong("fk_described_template"); // null table entry returns 0
                dbTestInstance.fk_run = resultSet.getLong("fk_run"); // null table entry returns 0
                dbTestInstance.fk_version_set = resultSet.getBytes("fk_version_set");
                dbTestInstance.description_hash = resultSet.getBytes("description_hash");
                dbTestInstance.fk_template = resultSet.getLong("fk_template");
                dbTestInstance.hash = resultSet.getBytes("hash");
                dbTestInstance.steps = resultSet.getString("steps");
                dbTestInstance.enabled = resultSet.getBoolean("enabled");
                System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads data, fk_described_template " + dbTestInstance.fk_described_template +
                                                     ", pk_template " + dbTestInstance.fk_template + (dbTestInstance.fk_run!=0 ? ", TEST RESULT ALREADY STORED" : ""));
                if (resultSet.next())
                    throw new Exception("resultSet wrongly has more than one entry");
            } else {
                throw new Exception("instance data not present");
            }
        } catch(Exception e) {
            System.out.println("InstanceCore.loadTestInstanceData() exception for iNum " + pk_test_instance + ": "+ e);
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }
        
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
            read_only = true;
            if (user != null && password != null) {
                read_only = false;
            }
            if ( user == null || password == null ) {
                user = "guest";
                password = "";
            }
            Class.forName("com.mysql.jdbc.Driver"); // required at run time only for .getConnection(): mysql-connector-java-5.1.35.jar
            connect = DriverManager.getConnection("jdbc:mysql://"+host+"/qa_portal?user="+user+"&password="+password);
        } catch ( Exception e ) {
            System.err.println( "ERROR: InstanceCore.openDatabase() could not open database connection, " + e.getMessage() );
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
        } catch ( Exception e ) {
            System.err.println( "ERROR: Could not close database connection, " + e.getMessage() );
        } finally {
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
    
    
    // public methods
        
    /** constructor
     * 
     * @param pk_test_instance
     */
    public InstanceCore( long pk_test_instance ) {
        this.pk_test_instance = pk_test_instance;
        openDatabase();
        
        if (connect == null) {
            System.err.println( "Core constructor fails without database connection");
        } else {
//            /* Load the description and template hashes */
//            loadHashes();
            
            loadTestInstanceData();
        }
    }
    
    /**
     * Close the InstanceCore object, releasing any resources.
     */
    public void close() {
        closeDatabase();
    }

    public boolean isReadOnly() {
        return read_only;
    }
    
    /**
     * From a given test instance number, execute the corresponding test instance (aka test run). 
     * @param iCore TODO
     */
    public void executeTestInstance(long testInstanceNumber, InstanceCore iCore) {
        // We are an independent process. We have access to the database,
        //   to a Resource Manager that has access to artifacts and resources,
        //   and to everything else needed to cause our test instance to be executed.
        
        // this.dbTestInstance is used to execute this test instance by following steps; aka instantiate this test run to generate a test result
        System.out.println( "executeTestInstance() has data base info for test instance " + iCore.pk_test_instance + ", finding described_template " + this.dbTestInstance.fk_described_template + " and template " + this.dbTestInstance.fk_template );
        System.out.println( "executeTestInstance() finds due date: " + this.dbTestInstance.due_date);
        System.out.println( "executeTestInstance() finds phase: " + this.dbTestInstance.phase);
        System.out.println( "executeTestInstance() finds syncronized: " + this.dbTestInstance.syncronized);
        System.out.println( "executeTestInstance() finds run: " + this.dbTestInstance.fk_run);
        System.out.println( "executeTestInstance() finds version set: " + this.dbTestInstance.fk_version_set);
        System.out.println( "executeTestInstance() finds description_hash: " + this.dbTestInstance.description_hash);
        System.out.println( "executeTestInstance() finds hash: " + this.dbTestInstance.hash);
        System.out.println( "executeTestInstance() finds enabled: " + this.dbTestInstance.enabled);
        System.out.println( "executeTestInstance() finds steps: \n" + this.dbTestInstance.steps);        
   }    
    
}