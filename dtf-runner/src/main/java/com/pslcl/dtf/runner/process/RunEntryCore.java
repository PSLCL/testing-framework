/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.runner.process;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import com.pslcl.dtf.runner.template.InstancedTemplate;
import com.pslcl.dtf.runner.template.TemplateProvider;

public class RunEntryCore {

    // class members
    
    /**
     * The connection to the database.
     */
    private Connection connect = null;
    private boolean read_only = true;
    private long reNum;
    private DBTemplate dbTemplate;

    
    // private methods

    /**
     * load data for reNum in class dbTemplate
     */
    private void loadRunEntryData() {
        // meant to be called once only; if more than once becomes useful, might work but review
        if (connect == null) {
            System.out.println("<internal> RunEntryCore.loadRunEntryData() finds no database connection and exits");
            return;
        }

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            String strRENum = String.valueOf(this.reNum);
            statement = connect.createStatement();

            // these table run entries are filled: pk_run, fk_template, start_time, and maybe owner
            // dbTemplate.reNum was filled in constructor, now fill all other entries of dbTemplate
            //     acquire run table information for entry reNum
            //     acquire the matching template table information
            resultSet = statement.executeQuery( "SELECT pk_template, hash, enabled, steps, artifacts, start_time, ready_time, end_time, result, owner " +
                                                "FROM template " +
                                                "JOIN run ON fk_template = pk_template " +
                                                "WHERE pk_run = " + strRENum );
            if ( resultSet.next() ) {
                dbTemplate.pk_template = resultSet.getLong("pk_template");
                dbTemplate.hash = resultSet.getBytes("hash");
                dbTemplate.enabled = resultSet.getBoolean("enabled");
                dbTemplate.steps = resultSet.getString("steps");
                dbTemplate.artifacts = resultSet.getBytes("artifacts");
                dbTemplate.start_time = resultSet.getDate("start_time");
                dbTemplate.ready_time = resultSet.getDate("ready_time");
                dbTemplate.end_time = resultSet.getDate("end_time");
                dbTemplate.result = (resultSet.getObject("result") != null) ? resultSet.getBoolean("result") : null;
                dbTemplate.owner = resultSet.getString("owner");
                System.out.println("      <internal> RunEntryCore.loadRunEntryData() loads data from run-template matched records for reNum " + this.reNum + ", pk_template " + dbTemplate.pk_template);
                if (resultSet.next())
                    throw new Exception("resultSet wrongly has more than one entry");
            } else {
                throw new Exception("template data not present");
            }
        } catch(Exception e) {
            System.out.println("RunEntryCore.loadRunEntryData() exception for reNum " + this.reNum + ": "+ e);
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
        }
    }
    
    /**
     * 
     */
    private void updateRunEntryData() {
        // assume template.steps and template.hash are not null, but check enabled
    	if (dbTemplate.enabled == false)
    		throw new IllegalArgumentException("dbTemplate.enabled is false");
    	dbTemplate.ready_time = new Date(); // now; assume start_time was filled when the reNum run table entry was placed
    	if (dbTemplate.owner == null)
    		dbTemplate.owner = "dtf-runner";
    	writeRunEntryData(); // ready_time and owner have changed
    }

    /**
     * 
     */
    private void writeRunEntryData() {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connect.createStatement();
            // TODO: implement the NySQL run table write command
            
        } catch(Exception e) {
            System.out.println("RunEntryCore.writeRunEntryData() exception for reNum " + dbTemplate.reNum + ": "+ e);
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
            // com.mysql.jdbc.Driver required at run time only for .getConnection()
            connect = DriverManager.getConnection("jdbc:mysql://"+host+"/qa_portal?user="+user+"&password="+password);
        } catch ( Exception e ) {
            System.err.println( "ERROR: RunEntryCore.openDatabase() could not open database connection, " + e.getMessage() );
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
     * @param pk_described_template
     */
    public RunEntryCore( long reNum ) {
        this.reNum = reNum;
        dbTemplate = new DBTemplate(this.reNum);
        openDatabase();
        if (connect == null)
            System.err.println( "RunEntryCore constructor fails without database connection");
        else {
            loadRunEntryData();
            updateRunEntryData(); // can throw IllegalArgumentException, and SQL write related exceptions
        }
    }

    /**
     * Close the RunEntryCore object, releasing any resources.
     */
    public void close() {
        closeDatabase();
    }

    public boolean isReadOnly() {
        return read_only;
    }
    
    /**
     * Execute the test run specified by the run entry number.
     * @return TODO
     */
    public boolean testRun(long reNum, TemplateProvider tp) throws Exception {
        // We are an independent process in our own thread. We have access
        //   to an Artifact Provider
        //   to a Resource Provider
        //   to a Template Provider
        //   to everything else needed to cause our test run to execute
        // We might have access to the database but the goal is to minimize use of it in this method call- everything is found in structure this.dbTemplate.
    	
        // On arriving here our reNum (aka pk_run) exists in table run.
        //     DECISION: We could check (here or before) to prove that pk_run of table run has null for its result field.
    	//               But we instead require that this be checked in whatever process placed reNum into the message queue.
    	//               A human can manually place an reNum, without double checking, and we will make a test run out of it.
        //     We expect that no other thread will be filling our result field of table run.
    	//
    	//     A "top level" test run is the one selected by a QA group process, for the purpose of storing its returned test result in table run.
    	//         As a top level test run proceeds, nested templates may generate nested test runs, whose test results may or may not be stored in table run.
    	//     At the time of running a top level test run, it is anticipated that the normal QA group process will have already associated a test instance with it. There is the issue of properly associating test instances with test runs: 
        //         For any top level test run, we expect that at least one entry of table test_instance is associated with it.
    	//		   Any test instance is associated with only one test run.
        //         At the completion of a top level test run, if there is no test instance association, it means that other test runs supplanted ours.
        //             This situation may be legal, but it may not be intended, and should be investigated to prove that it is legal.
        //     We understand that test runs of nested templates will return a result that may or may not be stored in table run, but regardless of that, may not be connected to any test instance.

    	// Our input is this.dbTemplate. It has been filled from our reNum entry in table run and its one linked entry in table template. Its start_time and ready_time are filled and stored to table run.
        // Instantiate the template. An instanced template has resources attached (with bind), its nested templates instanced (with include), and has enough information that we can call tp to deploy artifacts, connect networks, issue runs, etc.
        //     iT has a class member for template nesting
        InstancedTemplate it;
        try {
            it = tp.getInstancedTemplate(this.dbTemplate); // throws Exception which fails this test run
        } catch (Exception e) {
            e.printStackTrace(); // TODO: output message
            throw e;
        }
        
        // proceed to deploy artifacts, issue runs, etc.
        
        
        
        
// - - - - - - - - - - - - - - - - - - Below is replaced by things above here
        // this.dbDescribedTemplate is used to execute this one variation of (perhaps multiple) test instances. These test instances share the same artifact.
        // TODO: See if the template is already available.
        // Instance template by following steps, then apply selected artifact to the instanced template.
        // This converts a test instance to a test raun.
        // Running the test run generates a test result.
        // The test result applies to as many test instances as are controlled by parameter describedTemplateNumber (to match test_instance.fk_described_template)
//        System.out.println( "\nRunEntryCore() has data base info for dtNum " + this.pk_described_template + ", corresponding to template number " + this.dbDescribedTemplate.fk_template +
//                            "; test instance count " + this.dbDescribedTemplate.pkdtToDBTestInstance.size() + "; recorded test run count (attached to this template number ) " + this.dbDescribedTemplate.pkdtToDBRun.size());
//        System.out.println( "RunEntryCore() finds module set: " + this.dbDescribedTemplate.fk_module_set);
//        System.out.println( "RunEntryCore() finds description_hash: " + this.dbDescribedTemplate.description_hash);
//        System.out.println( "RunEntryCore() finds dtSynchronized: " + this.dbDescribedTemplate.dtSynchronized);
//        System.out.println( "RunEntryCore() finds template_hash: " + this.dbDescribedTemplate.template_hash);
//        System.out.println( "RunEntryCore() finds enabled: " + this.dbDescribedTemplate.enabled);
//        System.out.println( "RunEntryCore() finds steps:\n" + this.dbDescribedTemplate.steps + "\n");
//        
//        int runCount = dbDescribedTemplate.pkdtToDBRun.size();
//        System.out.println( "RunEntryCore() finds " + runCount + " associated record(s) in table run." + ((runCount==1) ? "":"") + "\n");
//        
//        int instanceCount = dbDescribedTemplate.pkdtToDBTestInstance.size();
//        System.out.println( "RunEntryCore() finds " + instanceCount + " associated record(s) in table test_instance." + ((instanceCount>0)? "":" 1 or more are needed"));
//        
//        for (DBTestInstance dbti: dbDescribedTemplate.pkdtToDBTestInstance.values()) {
//            System.out.println( "RunEntryCore() has data base info for test instance " + dbti.pk_test_instance );
//            System.out.println( "RunEntryCore() finds run: " + dbti.fk_run);
//            System.out.println( "RunEntryCore() finds due date: " + dbti.due_date);
//            System.out.println( "RunEntryCore() finds phase: " + dbti.phase);
//            System.out.println( "RunEntryCore() finds iSynchronized: " + dbti.iSynchronized);
//        }
//        System.out.println();
//
//        int dtLineCount = dbDescribedTemplate.pkdtToDTLine.size();
//        System.out.println( "RunEntryCore() finds " + dtLineCount + " associated record(s) in table dt_line." + ((dtLineCount>0)? "":" 1 or more are needed"));
//
//        for (DBDTLine dtLine: dbDescribedTemplate.pkdtToDTLine.values()) {
//            String strReason = dtLine.reason; // found sometimes as null, sometimes as empty string
//            if (strReason!=null && strReason.isEmpty())
//                strReason = "Unspecified as empty string";
//
//            System.out.println("\nRunEntryCore() finds line data from pk_dt_line " + dtLine.pk_dt_line + ", line " + dtLine.line +
//                               "\nDtLineDescription " + dtLine.dtLineDescription +
//                               "\nReason for artifact: " + strReason +
//                               "\nArtifact Info: is_primary " + dtLine.is_primary + ", configuration " + dtLine.configuration + ", artifactName " + dtLine.artifactName + ", mode " + dtLine.mode + ", merge_source " + dtLine.merge_source +
//                                                                                    ", derived_from_artifact " + dtLine.derived_from_artifact + ", merged_from_module " + dtLine.merged_from_module +
//                               "\nVersion of artifact: " + dtLine.version + ", sequence " + dtLine.sequence + ", attributes " + dtLine.attributes + ", scheduled_release " + dtLine.scheduled_release +
//                                                                                    ", actual_release " + dtLine.actual_release + ", sort_order " + dtLine.sort_order +
//                               "\nContent of artifact: " + dtLine.pk_content + ", is_generated " + dtLine.is_generated +
//                               "\nResource of dtLine, hash: " + dtLine.resourceHash + ", name " + dtLine.resourceName + ", description " + dtLine.resourceDescription);
//        }
//        System.out.println();
//
//        if (dbDescribedTemplate.enabled) {
//            if (dbDescribedTemplate.template_hash != null && dbDescribedTemplate.steps != null) {
//                System.out.println("RunEntryCore() finds enabled described template of hash " + dbDescribedTemplate.template_hash + ", with steps\n");
//                DescribedTestRun dtr = new DescribedTestRun(dbDescribedTemplate, tp);
//                dtr.init();
//                dtr.initRunInfo();
//            } else {
//                System.out.println("RunEntryCore() finds enabled described template with unexpected null for hash, or steps, or both");
//            }
//        } else {
//            System.out.println("RunEntryCore() finds disabled described template of hash " + dbDescribedTemplate.template_hash + ", with steps\n");
//        }
        
        // unneeded- our real approach involves instantiating templates
//        for (DBDTLine dtLine: dbTestInstance.pkToDTLine.values()) {
//            if (dtLine.resourceHash!=null && dtLine.resourceDescription!=null && !dtLine.resourceDescription.isEmpty()) {
//                System.out.println("RunEntryCore() finds valid resource hash " + dtLine.resourceHash + ", resource description " + dtLine.resourceDescription + ", of resource name " + dtLine.resourceName + "\n");
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
//                System.out.println("processDescribedTemplate() finds valid component name " + dtLine.componentName + " and version " + dtLine.version + "\n");
//                
//                qbap.iterateArtifacts(null, dtLine.componentName, dtLine.version, dtLine.platform, artifactFoundCallback);
//            }
//        }
        
        return false;
    }
    
    
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