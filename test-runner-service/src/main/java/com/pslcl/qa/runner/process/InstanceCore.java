package com.pslcl.qa.runner.process;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.pslcl.qa.runner.template.TemplateInstance;

public class InstanceCore {

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
                                                "WHERE pk_test_instance = " + strINum );
            // everything in this query is 1:1 relationship, so resultSet has exactly 1 or 0 entry

            if ( resultSet.next() ) {
                dbTestInstance.pk_described_template = resultSet.getLong("fk_described_template"); // null entry returns 0
                dbTestInstance.fk_run = resultSet.getLong("fk_run");                               // null entry returns 0
                dbTestInstance.due_date = resultSet.getDate("due_date");
                dbTestInstance.phase = resultSet.getInt("phase");
                dbTestInstance.iSynchronized = resultSet.getBoolean("test_instance.synchronized");

                dbTestInstance.fk_version_set = resultSet.getBytes("fk_version_set");
                dbTestInstance.fk_template = resultSet.getLong("fk_template");                     // null entry returns 0
                dbTestInstance.description_hash = resultSet.getBytes("description_hash");
                dbTestInstance.dtSynchronized = resultSet.getBoolean("described_template.synchronized");

                dbTestInstance.template_hash = resultSet.getBytes("hash");
                dbTestInstance.enabled = resultSet.getBoolean("enabled");
                dbTestInstance.steps = resultSet.getString("steps");

                System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads 1:1 data from test_instance " + dbTestInstance.pk_test_instance + ", pk_described_template " + dbTestInstance.pk_described_template +
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
            String strPKDT = String.valueOf(dbTestInstance.pk_described_template);
            statement = connect.createStatement();
            resultSet = statement.executeQuery( "SELECT pk_dt_line, line, description " +
                                                "FROM dt_line " +
                                                "WHERE fk_described_template = " + strPKDT );
            while ( resultSet.next() ) {
                DBDTLine dtLine = new DBDTLine();
                dtLine.pk_dt_line = resultSet.getLong("pk_dt_line"); // null entry returns 0
                dtLine.line = resultSet.getInt("line");
                dtLine.dtLineDescription = resultSet.getString("description");
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
                                                    "WHERE fk_dt_line = " + strPKDTLine );
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
                                                    "WHERE pk_version = " + strPKVersion );
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
                                                    "WHERE pk_artifact = " + strPKArtifact );
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

        // get corresponding component information
        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            try {
                String strPKVersion = String.valueOf(dtLine.pk_version);
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT name " +
                                                    "FROM component " +
                                                    "JOIN version ON fk_component = pk_component " +
                                                    "WHERE pk_version = " + strPKVersion );
                if ( resultSet.next() ) {
                    dtLine.componentName = resultSet.getString("name");
                    System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads component data for dt_line " + dtLine.pk_dt_line);

                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                }
            } catch(Exception e) {
                System.out.println("InstanceCore.loadTestInstanceData() exception on dtLine access for iNum " + pk_test_instance + ": "+ e);
            } finally {
                safeClose( resultSet ); resultSet = null;
                safeClose( statement ); statement = null;
            }
        }

        // get corresponding resource information; not every dtLine has corresponding resource information
        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            try {
                String strPKDTLine = String.valueOf(dtLine.pk_dt_line);
                statement = connect.createStatement();
                resultSet = statement.executeQuery( "SELECT hash, name, resource.description " +
                                                    "FROM dt_line " +
                                                    "JOIN resource ON fk_resource = pk_resource " +
                                                    "WHERE pk_dt_line = " + strPKDTLine );
                if ( resultSet.next() ) {
                    dtLine.resourceHash = resultSet.getBytes("hash");
                    dtLine.resourceName = resultSet.getString("name");
                    dtLine.resourceDescription = resultSet.getString("description");
                    System.out.println("      <internal> InstanceCore.loadTestInstanceData() loads resource data for dt_line " + dtLine.pk_dt_line);

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

        if (connect == null)
            System.err.println( "InstanceCore constructor fails without database connection");
        else
            loadTestInstanceData();
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
        // We are an independent process. We have access
        //   to a Resource Manager that has access to artifacts and resources,
        //   and to everything else needed to cause our test instance to be executed.
        // We might have access to the database but the goal is not to access it- everything is found in this.dbTestInstance.

        // REVIEW: all these needed? testInstanceNumber same as iCore.pk_test_instance same as this.dbTestInstance.pk_test_instance

        // this.dbTestInstance is used to execute this test instance by following steps; aka instantiate this test run to generate a test result
        System.out.println( "executeTestInstance() has data base info for test instance " + iCore.pk_test_instance + ", finding described_template " + this.dbTestInstance.pk_described_template + " and template " + this.dbTestInstance.fk_template );
        System.out.println( "executeTestInstance() finds run: " + this.dbTestInstance.fk_run);
        System.out.println( "executeTestInstance() finds due date: " + this.dbTestInstance.due_date);
        System.out.println( "executeTestInstance() finds phase: " + this.dbTestInstance.phase);
        System.out.println( "executeTestInstance() finds iSynchronized: " + this.dbTestInstance.iSynchronized);

        System.out.println( "executeTestInstance() finds version set: " + this.dbTestInstance.fk_version_set);
        System.out.println( "executeTestInstance() finds description_hash: " + this.dbTestInstance.description_hash);
        System.out.println( "executeTestInstance() finds dtSynchronized: " + this.dbTestInstance.dtSynchronized);

        System.out.println( "executeTestInstance() finds template_hash: " + this.dbTestInstance.template_hash);
        System.out.println( "executeTestInstance() finds enabled: " + this.dbTestInstance.enabled);
        System.out.println( "executeTestInstance() finds steps:\n" + this.dbTestInstance.steps);

        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
            String strReason = dtLine.reason; // found sometimes as null, sometimes as empty string
            if (strReason!=null && strReason.isEmpty())
                strReason = "Unspecified as empty string";
            
            if (dtLine.platform == null) {
                int x = 30; // this happens every time
            } else {
                int x = 30; // this never happens
            }
            
            System.out.println("\nexecuteTestInstance() finds line data from pk_dt_line " + dtLine.pk_dt_line + ", line " + dtLine.line +
                               "\nDtLineDescription " + dtLine.dtLineDescription +
                               "\nReason for artifact: " + strReason +
                               "\nArtifact Info: is_primary " + dtLine.is_primary + ", synchronized " + dtLine.aSynchronized + ", platform " + dtLine.platform +", internal_build " + dtLine.internal_build + ", artifactName " + dtLine.artifactName +
                               "\nVersion of artifact: " + dtLine.version + ", scheduled_release " + dtLine.scheduled_release + ", actual_release " + dtLine.actual_release + ", sort_order " + dtLine.sort_order +
                               "\nContent of artifact: " + dtLine.pk_content + ", is_generated " + dtLine.is_generated +
                               "\nComponent of artifact: " + dtLine.componentName +
                               "\nResource of dtLine, hash: " + dtLine.resourceHash + ", name " + dtLine.resourceName + ", description " + dtLine.resourceDescription);
        }
        System.out.println();

        if (dbTestInstance.enabled) {
            if (dbTestInstance.template_hash != null && dbTestInstance.steps != null) {
                System.out.println("executeTestInstance() finds enabled template of hash " + dbTestInstance.template_hash + ", with steps\n");
                TemplateInstance ti = new TemplateInstance(dbTestInstance);
                ti.instantiate();
            } else {
                System.out.println("executeTestInstance() finds enabled template wrongly with null hash or null steps");
            }
        } else {
            System.out.println("executeTestInstance() finds disabled template of hash " + dbTestInstance.template_hash + ", with steps\n");
        }

        
        
        
        // unneeded- our real approach involves instantiating templates
//        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
//            if (dtLine.resourceHash!=null && dtLine.resourceDescription!=null && !dtLine.resourceDescription.isEmpty()) {
//                System.out.println("executeTestInstance() finds valid resource hash " + dtLine.resourceHash + ", resource description " + dtLine.resourceDescription + ", of resource name " + dtLine.resourceName + "\n");
//                ResourceProvider rp = new Machine();
//            }
//        }
        
        
        
        // unneeded- our real approach involves an api to come to us soon
//        // prove that api works to get files from the build machine
//        String endpoint = null;
//        QuickBuildArtifactProvider qbap = new QuickBuildArtifactProvider(endpoint);
//        try {
//            qbap.init();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        ArtifactNotifier artifactFoundCallback = new ArtifactFoundCallback();
//                
//        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
//        if (dtLine.componentName != null && !dtLine.componentName.isEmpty() && dtLine.version != null && !dtLine.version.isEmpty()) {
//                System.out.println("executeTestInstance() finds valid component name " + dtLine.componentName + " and version " + dtLine.version + "\n");
//                
//                qbap.iterateArtifacts(null, dtLine.componentName, dtLine.version, dtLine.platform, artifactFoundCallback);
//            }
//        }
    }
    
    
//    private class Machine implements MachineProvider {
//        
//        private String resourceHash = null;
//        private String resourceDescription;
//        // or maybe instead the private member is a map or list of setResource() calls
//        
//        @Override
//        public MachineInstance bind(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
//            // TODO Auto-generated method stub
//            return null;
//        }
//        
//        @Override
//        public void release(ResourceInstance resource) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void setResource(String resourceHash, String resourceDescription) {
//            // TODO Auto-generated method stub
//            
//        }
//        
//        @Override
//        public boolean isAvailable(String resourceHash, String resourceAttributes) throws ResourceNotFoundException {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public void updateArtifact(String component, String version, String platform, String name, Hash hash) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void removeArtifact(String component, String version, String platform, String name) {
//            // TODO Auto-generated method stub
//            
//        }
//
//        @Override
//        public void invalidateArtifacts(String component, String version) {
//            // TODO Auto-generated method stub
//            
//        }
//        
//    }

    
//    private class ArtifactFoundCallback implements ArtifactNotifier {
//
//        @Override
//        public void artifact(String project, String component, String version, String platform, String internal_build, String name, Hash hash, Content content) {
//            // TODO Auto-generated method stub
//            
//        }
//        
//    }
    
}