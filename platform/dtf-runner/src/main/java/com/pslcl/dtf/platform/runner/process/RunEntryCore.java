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
package com.pslcl.dtf.platform.runner.process;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.pslcl.dtf.platform.runner.template.InstancedTemplate;
import com.pslcl.dtf.platform.runner.template.TemplateProvider;

/**
 * RunEntryCore is used to instantiate templates.
 * From a top level template down, each nested template calls this class. Hence, it is recursive. 
 */
public class RunEntryCore {

    // class members
    
    /** The common connection to the database. */
    private static AtomicInteger connectCount = new AtomicInteger(0); // ref counting
	private static Connection connect = null;
    private static boolean read_only = true;

    /* instance members */
    private Long reNum;
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
            statement = connect.createStatement();
            
            if (reNum != null) {
            	String strRENum = String.valueOf(this.reNum);
            	
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
            } else {
                // acquire template table information
                resultSet = statement.executeQuery( "SELECT pk_template, hash, enabled, steps " +
                                                    "FROM template " );
                if ( resultSet.next() ) {
                    dbTemplate.pk_template = resultSet.getLong("pk_template");
                    dbTemplate.hash = resultSet.getBytes("hash");
                    dbTemplate.enabled = resultSet.getBoolean("enabled");
                    dbTemplate.steps = resultSet.getString("steps");
                    
                    // run table not queried, so set defaults in dbTemplate (for the missing run table entries)
                    dbTemplate.artifacts = null;
                    dbTemplate.start_time = new Date();
                    dbTemplate.ready_time = null;
                    dbTemplate.end_time = null;
                    dbTemplate.result = null;
                    dbTemplate.owner = "dtf-runner";
                    System.out.println("      <internal> RunEntryCore.loadRunEntryData() loads template data only for test run with no table run entry, for pk_template " + dbTemplate.pk_template);
                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                } else {
                    throw new Exception("template data not present");
                }
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
     * @note: does not overwrite run.pk_run, run.fk_template, run.start_time; dtf-runner does not own these
     * @note: dtf-runner also do not own table template, and does not write it 
     */
    private void writeRunEntryData() {
        Statement statement = null;
        try {
            statement = connect.createStatement();
            String strRENum = String.valueOf(dbTemplate.reNum);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            String readyTime = "null";
            if (dbTemplate.ready_time != null)
            	readyTime = "'" + sdf.format(dbTemplate.ready_time) + "'";
            String endTime = "null";
            if (dbTemplate.end_time != null)
            	endTime = "'" + sdf.format(dbTemplate.end_time) + "'";
            String owner = "null";
            if (dbTemplate.owner != null)
            	owner = "'" + dbTemplate.owner + "'";
            Boolean result = null;
            if (dbTemplate.result != null)
            	result = dbTemplate.result;
            byte [] artifacts = null;
            if (dbTemplate.artifacts != null)
            	artifacts = dbTemplate.artifacts;
            
            String str = "UPDATE run " +
                         "SET ready_time = " + readyTime + ", " +
                               "end_time = " + endTime   + ", " +
                                  "owner = " + owner + ", " +
                                 "result = " + result + ", " +
                              "artifacts = " + artifacts + " " +
                         "WHERE pk_run = " + strRENum;
            
            statement.executeUpdate( str ); // returns the matching row count: 1 for pk_run row exists, or 0
        } catch(Exception e) {
            System.out.println("RunEntryCore.writeRunEntryData() exception for reNum " + dbTemplate.reNum + ": "+ e);
        } finally {
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
        if ( connect == null ) {
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
        connectCount.incrementAndGet();
    }

    /**
     * Close the database connection if it is open.
     */
    private void closeDatabase() {
    	if (connectCount.decrementAndGet() <= 0) {
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
    public RunEntryCore( Long reNum ) {
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
     * @return the result of this test run
     */
    public boolean testRun(Long reNum, TemplateProvider tp) throws Exception {
    	// TODO: remove reNum parameter; it is built into RunEntryCore and is present in this.dbTemplate
    	
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
    	//     At the time of running a top level test run, it is anticipated that the normal QA group process will have already associated a test instance with it, or more than one.
    	//         Multiple test instances can link to one test run (because multiple test instances can link to one template).
    	//             Typically, when multiple test instances link to one test run, they share the same common artifact or set of artifacts.
    	//     There is the issue of properly associating test instances with test runs: 
        //         For any top level test run, we expect that at least one entry of table test_instance is associated with it.
    	//		   Any test instance is associated with only one test run.
        //         At the completion of a top level test run, if there is no test instance association, it means that other test runs supplanted ours.
        //             This situation may be legal, but it may not be intended, and should be investigated to prove that it is legal.
        //     We understand that test runs of nested templates will return a result that may or may not be stored in table run, but regardless of that, may not be connected to any test instance.

    	// Our input is this.dbTemplate. It has been filled from our reNum entry in table run and its one linked entry in table template. Its start_time and ready_time are filled and stored to table run.
        // Instantiate the template. An instanced template has resources attached (with bind), its nested templates instanced (with include), and has enough information that we can call tp to deploy artifacts, connect networks, issue runs, etc.
        InstancedTemplate iT;
        try {
            iT = tp.getInstancedTemplate(this.dbTemplate); // throws Exception which fails this test run
        } catch (Exception e) {
            e.printStackTrace(); // TODO: output message
            throw e;
        }
        
        // Instantiate the test run. To iT, deploy artifacts, issue runs, etc.
        // TODO
        
        
        

        
        
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
        
        return false; // TODO
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