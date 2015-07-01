package com.pslcl.qa.platform.process;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class InstanceCore {

    // sub classes

    /**
     * Relevant data base info for this test instance
     */
    private class DBTestInstance {
        long pk_test_instance;      // INT(11) in test_instance; note: this is also held in parent class as InstanceCore.pk_test_instance
        long fk_described_template; // INT(11) in described_template
        long fk_run;                // INT(11) in run
        Date due_date;              // DATETIME in test_instance TODO: double check needed Java type
        int phase;                  // INT in test_instance TODO: double check needed Java type
        boolean iSynchronized;      // BOOLEAN synchronized in test_instance TODO: double check needed Java type

        // from described_template
        byte[] fk_version_set;      // BINARY(32) 32 byte array in described_template; use ResultSet.getBytes()
        long fk_template;           // INT(11) in described_template
        byte[] description_hash;    // BINARY(32) 32 byte array in described_template
        boolean dtSynchronized;     // BOOLEAN synchronized in described_template TODO: double check needed Java type

        // from template
        byte [] hash;               // BINARY(32) 32 byte array in template
        boolean enabled;            // BOOLEAN in template
        String steps;               // MEDIUMTEXT in template

        // holds multiple instances of dt_line
        Map<Long,DBDTLine> pkToDTLine;

        /**
         *  Constructor
         */
        DBTestInstance(long pk_test_instance) {
            this.pk_test_instance = pk_test_instance;
            pkToDTLine = new HashMap<Long,DBDTLine>();
        }

    }

    /**
     * Relevant data base info for this test instance
     */
    private class DBDTLine {
        // from dt_line
        long pk_dt_line;            // INT(11) in dt_line
        int line;                   // INT in dt_line
        String description;         // MEDIUMTEXT in dt_line

        // from artifact_to_dt_line
        boolean is_primary;         // BOOLEAN
        String reason;              // VARCHAR(45)

        // from artifact
        long pk_artifact;           // INT(11)
        boolean aSynchronized;      // BOOLEAN
        String platform;            // VARCHAR(45)
        String internal_build;      // VARCHAR(45)
        String name;                // VARCHAR(45)

        // from version
        long pk_version;            // INT(11)
        String version;             // VARCHAR(45)
        Date scheduled_release;     // DATE
        Date actual_release;        // DATE
        int sort_order;             // INT
        
        // from content
        byte[] pk_content;          // BINARY(32)
        boolean is_generated;       // BOOLEAN

        /**
         *  Constructor
         */
        DBDTLine() {
            is_primary = false;
            reason = null;
            aSynchronized = false;
            platform = null;
            internal_build = null;
            name = null;
            version = null;
            scheduled_release = null;
            actual_release = null;
            sort_order = -1;
            pk_content = null;
            is_generated = false;
        }

    }


    // class members

    /**
     * The connection to the database.
     */
    private long pk_test_instance;
    private Connection connect = null;
    private boolean read_only = true;
    private DBTestInstance dbTestInstance = null;


    // private methods

    // meant to be called once only; if more than once becomes useful, might work but review
    private void loadTestInstanceData() {
        if (connect == null) {
            System.out.println("<internal> InstanceCore.loadTestInstance() finds no database connection and exits");
            return;
        }

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String strINum = String.valueOf(pk_test_instance);
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT fk_described_template, fk_run, due_date, phase, test_instance.synchronized, fk_version_set, fk_template, description_hash, described_template.synchronized, hash, enabled, steps " +
                                                "FROM test_instance " +
                                                "JOIN described_template ON fk_described_template = pk_described_template " +
                                                "JOIN template           ON fk_template = pk_template " +
                                                "WHERE pk_test_instance =" + strINum );
            // everything in this query is 1:1 relationship, so resultSet has exactly 1 or 0 entry

            if ( resultSet.next() ) {
                dbTestInstance.fk_described_template = resultSet.getLong("fk_described_template"); // null entry returns 0
                dbTestInstance.fk_run = resultSet.getLong("fk_run");                               // null entry returns 0
                dbTestInstance.due_date = resultSet.getDate("due_date");
                dbTestInstance.phase = resultSet.getInt("phase");
                dbTestInstance.iSynchronized = resultSet.getBoolean("test_instance.synchronized");

                dbTestInstance.fk_version_set = resultSet.getBytes("fk_version_set");
                dbTestInstance.fk_template = resultSet.getLong("fk_template");                     // null entry returns 0
                dbTestInstance.description_hash = resultSet.getBytes("description_hash");
                dbTestInstance.dtSynchronized = resultSet.getBoolean("described_template.synchronized");

                dbTestInstance.hash = resultSet.getBytes("hash");
                dbTestInstance.enabled = resultSet.getBoolean("enabled");
                dbTestInstance.steps = resultSet.getString("steps");

                System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads 1:1 data from test_instance " + dbTestInstance.pk_test_instance + ", fk_described_template " + dbTestInstance.fk_described_template +
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

        // get corresponding multiple lines of data
        try {
            String strFKDT = String.valueOf(dbTestInstance.fk_described_template);
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_dt_line, line, description " +
                                                "FROM dt_line " +
                                                "WHERE fk_described_template =" + strFKDT );
            while ( resultSet.next() ) {
                DBDTLine dtLine = new DBDTLine();
                dtLine.pk_dt_line = resultSet.getLong("pk_dt_line"); // null entry returns 0
                dtLine.line = resultSet.getInt("line");
                dtLine.description = resultSet.getString("description");
                System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads line data from dt_line " + dtLine.pk_dt_line);

                dbTestInstance.pkToDTLine.put(dtLine.pk_dt_line, dtLine);
            }
        } catch(Exception e) {
            System.out.println("InstanceCore.loadTestInstanceData() exception on dtLine access for iNum " + pk_test_instance + ": "+ e);
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }

        // get corresponding artifact information; not every dtLine has corresponding artifact information
        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            try {
                String strPKDTLine = String.valueOf(dtLine.pk_dt_line);
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT is_primary, reason, pk_artifact, fk_version, fk_content, synchronized, platform, internal_build, name " +
                                                    "FROM artifact_to_dt_line " +
                                                    "JOIN artifact ON fk_artifact = pk_artifact " +
                                                    "WHERE fk_dt_line =" + strPKDTLine );
                if ( resultSet.next() ) {
                    dtLine.is_primary = resultSet.getBoolean("is_primary");
                    dtLine.reason = resultSet.getString("reason");
                    dtLine.pk_artifact = resultSet.getLong("pk_artifact");
                    dtLine.pk_version = resultSet.getLong("fk_version");
                    System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads artifact data for dt_line " + dtLine.pk_dt_line);

                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                }
            } catch(Exception e) {
                System.out.println("InstanceCore.loadTestInstanceData() exception on dtLine access for iNum " + pk_test_instance + ": "+ e);
            } finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        } // end for()
        
        // get corresponding version information; not every dtLine has corresponding version information
        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            try {
                String strPKVersion = String.valueOf(dtLine.pk_version);
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT version, scheduled_release, actual_release, sort_order " +
                                                    "FROM version " +
                                                    "WHERE pk_version =" + strPKVersion );
                if ( resultSet.next() ) {
                    dtLine.version = resultSet.getString("version");
                    dtLine.scheduled_release = resultSet.getDate("scheduled_release");
                    dtLine.actual_release = resultSet.getDate("actual_release");
                    dtLine.sort_order = resultSet.getInt("sort_order");
                    System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads version data for dt_line " + dtLine.pk_dt_line);

                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                }
            } catch(Exception e) {
                System.out.println("InstanceCore.loadTestInstanceData() exception on dtLine access for iNum " + pk_test_instance + ": "+ e);
            } finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        } // end for()
        
        // get corresponding content information; not every dtLine has corresponding content information
        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            try {
                String strPKArtifact = String.valueOf(dtLine.pk_artifact);
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT pk_content, is_generated " +
                                                    "FROM content " +
                                                    "JOIN artifact ON fk_content = pk_content " + 
                                                    "WHERE pk_artifact =" + strPKArtifact );
                if ( resultSet.next() ) {
                    dtLine.pk_content = resultSet.getBytes("pk_content");
                    dtLine.is_generated = resultSet.getBoolean("is_generated");
                    System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads content data for dt_line " + dtLine.pk_dt_line);

                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                }
            } catch(Exception e) {
                System.out.println("InstanceCore.loadTestInstanceData() exception on dtLine access for iNum " + pk_test_instance + ": "+ e);
            } finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        } // end for()         
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
        dbTestInstance = new DBTestInstance(this.pk_test_instance);
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

        // REVIEW: all these needed? testInstanceNumber same as iCore.pk_test_instance same as this.dbTestInstance.pk_test_instance

        // this.dbTestInstance is used to execute this test instance by following steps; aka instantiate this test run to generate a test result
        System.out.println( "executeTestInstance() has data base info for test instance " + iCore.pk_test_instance + ", finding described_template " + this.dbTestInstance.fk_described_template + " and template " + this.dbTestInstance.fk_template );
        System.out.println( "executeTestInstance() finds run: " + this.dbTestInstance.fk_run);
        System.out.println( "executeTestInstance() finds due date: " + this.dbTestInstance.due_date);
        System.out.println( "executeTestInstance() finds phase: " + this.dbTestInstance.phase);
        System.out.println( "executeTestInstance() finds iSynchronized: " + this.dbTestInstance.iSynchronized);

        System.out.println( "executeTestInstance() finds version set: " + this.dbTestInstance.fk_version_set);
        System.out.println( "executeTestInstance() finds description_hash: " + this.dbTestInstance.description_hash);
        System.out.println( "executeTestInstance() finds dtSynchronized: " + this.dbTestInstance.dtSynchronized);

        System.out.println( "executeTestInstance() finds hash: " + this.dbTestInstance.hash);
        System.out.println( "executeTestInstance() finds enabled: " + this.dbTestInstance.enabled);
        System.out.println( "executeTestInstance() finds steps:\n" + this.dbTestInstance.steps);

        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            System.out.println("\nexecuteTestInstance() finds line data from pk_dt_line " + dtLine.pk_dt_line + ", line " + dtLine.line + "\nDescription " + dtLine.description +
                               "\nReason for artifact: " + dtLine.reason + "\nArtifact Info: is_primary " + dtLine.is_primary +
                               ", synchronized " + dtLine.aSynchronized + ", platform " + dtLine.platform +", internal_build " + dtLine.internal_build + ", name " + dtLine.name +
                               "\nVersion of artifact: " + dtLine.version + ", scheduled_release " + dtLine.scheduled_release + ", actual_release " + dtLine.actual_release + ", sort_order " + dtLine.sort_order +
                               "\nContent of artifact: " + dtLine.pk_content + ", is_generated " + dtLine.is_generated);
        }
        System.out.println();
   }

}