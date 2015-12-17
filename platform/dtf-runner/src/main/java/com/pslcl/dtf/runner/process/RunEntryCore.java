/*
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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.runner.template.InstancedTemplate;

/**
 * RunEntryCore is used to instantiate templates.
 * From a top level template down, each nested template calls this class. Hence, it is recursive.
 */
public class RunEntryCore {

    // static private declarations

    /** The common connection to the database. */
	static private Integer connectCount = 0; // ref count; also used as the synchronization object
    static private Connection connect = null; // not used for testing, only for as actual connect object
    static private boolean read_only = true;
    
    /**
     * Open the database connection if it is not already open. Environment variables are used
     * to determine the DB host, user, and password. The DTF_TEST_DB_HOST variable is required.
     * DTF_TEST_DB_USER and DTF_TEST_DB_PASSWORD are optional, and if not specified then a guest
     * account with no password is used. This sets the 'read_only' flag, which disables all
     * database modifications.
     * 
     * @note This is ref counted. Match every call to openDatabaseConnection() with a later call to closeDatabaseConnection(). Every database-user method calls openDatabaseConnection() first and closeDatabaseConnection() last.
     */
    static private void openDatabaseConnection() throws Exception {
    	synchronized (connectCount) {
    		if (connectCount.intValue() < 0) {
    			LoggerFactory.getLogger("RunEntryCore").error("RunEntryCore.openDatabaseConnection finds illegal connectCount value " + connectCount.intValue());
    			throw new Exception("RunEntryCore, improper ref count");
    		}

    		if (connectCount.intValue() == 0) {
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
                    // com.mysql.jdbc.Driver required, at run time only, for .getConnection()
                    connect = DriverManager.getConnection("jdbc:mysql://"+host+"/qa_portal?user="+user+"&password="+password);
                } catch ( Exception e ) {
                    LoggerFactory.getLogger("RunEntryCore").error("openDatabaseConnection() could not open database connection, " + e);
                    throw e;
                }
    		}
            ++connectCount;
    	} // end synchronized()
    }

    /**
     * Close the database connection if it is open.
     */
    static private void closeDatabaseConnection() throws Exception {
    	synchronized (connectCount) {
            --connectCount;
    		if (connectCount.intValue() < 0) {
    			LoggerFactory.getLogger("RunEntryCore").error("RunEntryCore.closeDatabaseConnection finds illegal connectCount value " + connectCount.intValue());
    			throw new Exception("RunEntryCore, improper ref count");
    		}

    		if (connectCount.intValue() == 0) {
                try {
                    connect.close();
                } catch ( Exception e ) {
                    LoggerFactory.getLogger("RunEntryCore").error("closeDatabaseConnection() could not close database connection, " + e);
                    throw e;
                }
    		}
    	} // end synchronized()
    }

    static private void safeClose( ResultSet r ) {
        try {
            if ( r != null )
                r.close();
        }
        catch ( Exception e ) {
            // Ignore
        }
    }

    static private void safeClose( Statement s ) {
        try {
            if ( s != null )
                s.close();
        }
        catch ( Exception e ) {
            // Ignore
        }
    }

    
    // static public declarations
    
    /**
     * @note Pair unprepare() with unprepare()
     */
    static public void prepare() throws Exception {
    	RunEntryCore.openDatabaseConnection();
    }
    
    /**
     * 
     */
    static public void unprepare() throws Exception {
    	RunEntryCore.closeDatabaseConnection();
    }
    
    /**
     * 
     * @param reNum
     * @return
     */
    static public Boolean getResult(long reNum) throws Exception {
    	Boolean retBoolean = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
       		RunEntryCore.openDatabaseConnection();
            statement = connect.createStatement();
            String strRENum = String.valueOf(reNum);

            // These table run entries are filled: pk_run, fk_template, start_time, and maybe owner.
            // topDBTemplate.reNum was filled in constructor; now fill all other entries of topDBTemplate
            //     acquire run table information for entry reNum
            //     acquire the matching template table information
            resultSet = statement.executeQuery( "SELECT pk_run, result " +
                                                "FROM run " +
                                                "WHERE pk_run = " + strRENum );
            if ( resultSet.next() ) {
                retBoolean = resultSet.getBoolean("result");
                if (resultSet.next())
                    throw new Exception("resultSet wrongly has more than one entry");
            } else {
                throw new Exception("template data not present");
            }
        } catch(Exception e) {
            LoggerFactory.getLogger("RunEntryCore").error("getResult() exception for reNum " + reNum + ": "+ e);
            throw e;
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
       		RunEntryCore.closeDatabaseConnection(); // ref counting can keep it open until the very end
        }
   		
    	return retBoolean;
    }

    
    // class instance members
    
    private final Logger log;
    private final String simpleName;
    private Long reNum;
    private DBTemplate topDBTemplate = null; // describes the top-level template only, i.e. the template that is matched to the run table entry known as reNum


    // private methods

    /**
     * load data for reNum in topDBTemplate
     */
    private void loadRunEntryData() throws Exception {
        // meant to be called once only; if more than once becomes useful, might work but review
        Statement statement = null;
        ResultSet resultSet = null;
        try {
        	RunEntryCore.openDatabaseConnection();
            statement = connect.createStatement();

            if (reNum != null) {
                String strRENum = String.valueOf(this.reNum);

                // These table run entries are filled: pk_run, fk_template, start_time, and maybe owner.
                // topDBTemplate.reNum was filled in constructor; now fill all other entries of topDBTemplate
                //     acquire run table information for entry reNum
                //     acquire the matching template table information
                resultSet = statement.executeQuery( "SELECT pk_template, hash, enabled, steps, artifacts, start_time, ready_time, end_time, result, owner " +
                                                    "FROM template " +
                                                    "JOIN run ON fk_template = pk_template " +
                                                    "WHERE pk_run = " + strRENum );
                if ( resultSet.next() ) {
                    topDBTemplate.pk_template = resultSet.getLong("pk_template");
                    topDBTemplate.hash = resultSet.getBytes("hash");
                    topDBTemplate.enabled = resultSet.getBoolean("enabled");
                    topDBTemplate.steps = resultSet.getString("steps");
                    topDBTemplate.artifacts = resultSet.getBytes("artifacts");
                    topDBTemplate.start_time = resultSet.getDate("start_time");
                    topDBTemplate.ready_time = resultSet.getDate("ready_time");
                    topDBTemplate.end_time = resultSet.getDate("end_time");
                    topDBTemplate.result = (resultSet.getObject("result") != null) ? resultSet.getBoolean("result") : null;
                    topDBTemplate.owner = resultSet.getString("owner");
                    log.debug(simpleName + "<internal> loadRunEntryData() loads data from run-template matched records for reNum " + this.reNum + ", pk_template " + topDBTemplate.pk_template);
                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                } else {
                    throw new Exception("template data not present");
                }
            } else {
            	// Note: Is it impossible to be here, now? It means that reCore was instantiated without coming through the path that has an reNum and a matching entry in table run.     
            	
                // acquire template table information
                resultSet = statement.executeQuery( "SELECT pk_template, hash, enabled, steps " +
                                                    "FROM template " );
                if ( resultSet.next() ) {
                    topDBTemplate.pk_template = resultSet.getLong("pk_template");
                    topDBTemplate.hash = resultSet.getBytes("hash");
                    topDBTemplate.enabled = resultSet.getBoolean("enabled");
                    topDBTemplate.steps = resultSet.getString("steps");

                    // run table not queried, so set defaults in topDBTemplate (for the missing run table entries)
                    topDBTemplate.artifacts = null;
                    topDBTemplate.start_time = new Date();
                    topDBTemplate.ready_time = null;
                    topDBTemplate.end_time = null;
                    topDBTemplate.result = null;
                    topDBTemplate.owner = "dtf-runner";
                    log.debug(simpleName + "<internal> loadRunEntryData() loads template data only for test run with no table run entry, for pk_template " + topDBTemplate.pk_template);
                    if (resultSet.next())
                        throw new Exception("resultSet wrongly has more than one entry");
                } else {
                    throw new Exception("template data not present");
                }
            }
        } catch(Exception e) {
            log.error(simpleName + "loadRunEntryData() exception for reNum " + this.reNum + ": "+ e);
            throw e;
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
       		RunEntryCore.closeDatabaseConnection(); // ref counting can keep it open until the very end
        }
    }

    /**
     *
     */
    private void storeReadyRunEntryData() throws Exception {
        // assume template.steps and template.hash are not null, but check template.enabled
        if (topDBTemplate.enabled == false)
            throw new IllegalArgumentException("topDBTemplate.enabled is false");
        topDBTemplate.ready_time = new Date(); // now; assume start_time was filled when the reNum run table entry was placed
        if (topDBTemplate.owner == null)
            topDBTemplate.owner = "dtf-runner";
        writeRunEntryData(); // ready_time and owner have changed
    }

    /**
     *
     */
    private void storeResultRunEntryData() throws Exception {
        // Calling this with null for topDBTemplate.result makes no sense. It may be an error. TODO: check that and log it, and maybe reject it entirely.
        topDBTemplate.end_time = new Date(); // now
        writeRunEntryData();
    }

    /**
     * @note: if reNum.result field has a value (is not null), this does not overwrite anything
     * @note: does not overwrite run.pk_run, run.fk_template, run.start_time; dtf-runner does not own these
     * @note: dtf-runner also do not own table template, and does not write it
     *
     */
    private void writeRunEntryData() throws Exception {
    	Boolean previousStoredResult = RunEntryCore.getResult(reNum); 
    	if (previousStoredResult == null) { // this check is a fail-safe
	        Statement statement = null;
	        try {
	        	RunEntryCore.openDatabaseConnection();
	            statement = connect.createStatement();
	            String strRENum = String.valueOf(topDBTemplate.reNum);
	            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	            String readyTime = "null";
	            if (topDBTemplate.ready_time != null)
	                readyTime = "'" + sdf.format(topDBTemplate.ready_time) + "'";
	            String endTime = "null";
	            if (topDBTemplate.end_time != null)
	                endTime = "'" + sdf.format(topDBTemplate.end_time) + "'";
	            String owner = "null";
	            if (topDBTemplate.owner != null)
	                owner = "'" + topDBTemplate.owner + "'";
	            Boolean result = null;
	            if (topDBTemplate.result != null)
	                result = topDBTemplate.result;
	            byte [] artifacts = null;
	            if (topDBTemplate.artifacts != null)
	                artifacts = topDBTemplate.artifacts;
	
	            String str = "UPDATE run " +
	                         "SET ready_time = " + readyTime + ", " +
	                               "end_time = " + endTime   + ", " +
	                                  "owner = " + owner + ", " +
	                                 "result = " + result + ", " +
	                              "artifacts = " + artifacts + " " +
	                         "WHERE pk_run = " + strRENum;
	
	            statement.executeUpdate( str ); // returns the matching row count: 1 for pk_run row exists, or 0
	        } catch(Exception e) {
	            log.error(simpleName + "writeRunEntryData() exception for reNum " + topDBTemplate.reNum + ": "+ e);
	        } finally {
	            safeClose( statement ); statement = null;
	       		RunEntryCore.closeDatabaseConnection(); // ref counting can keep it open until the very end
	        }
    	} else {
    		log.warn(simpleName + "writeRunEntryData() does not overwrite a previously stored result, for reNum " + topDBTemplate.reNum);
    	}
    }


    // public methods

    /** constructor
     *
     * @param pk_described_template
     * @throws Exception 
     */
    public RunEntryCore( Long reNum ) throws Exception {
    	this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.reNum = reNum;
        topDBTemplate = new DBTemplate(this.reNum);
        try {
          	// fill all fields of topDBTemplate from the existing existing database entries
			loadRunEntryData(); // throws Exception for no database entry for this.reNum
			storeReadyRunEntryData(); // can throw IllegalArgumentException for template.enabled false, and SQL write related exceptions
		} catch (Exception e) {
			throw e;
		}
    }

    public boolean isReadOnly() {
        return read_only;
    }

    public DBTemplate getDBTemplate() {
    	return topDBTemplate;
    }
    
    public long getRENum() {
    	return reNum;
    }
    
    /**
     * Execute the test run specified by the run entry number.
     * @return the result of this test run; result is stored already
     */
    public boolean testRun(Long reNum, RunnerMachine runnerMachine) throws Exception {
        // TODO: remove reNum parameter; it is built into RunEntryCore and is present in this.topDBTemplate

        // We are an independent process in our own thread. We have access
        //   to an Artifact Provider
        //   to a Resource Provider
        //   to a Template Provider
        //   to everything else needed to cause our test run to execute
        // We might have access to the database but the goal is to minimize use of it in this method call- everything is found in structure this.topDBTemplate.

        // On arriving here our reNum (aka pk_run) exists in table run.
        //     We expect that no other thread will be filling our result field of table run.
        //
        //     A "top level" test run is the one selected by a QA group process, for the purpose of storing its returned test result in table run.
        //         As a top level test run proceeds, nested templates may generate pseudo test runs, whose test results may or may not be stored in table run.
        //     At the time of running a top level test run, it is anticipated that the normal QA group process will have already associated a test instance with it, or more than one.
        //         Multiple test instances can link to one test run (because multiple test instances can link to one template).
        //             Typically, when multiple test instances link to one test run, they share the same common artifact or set of artifacts.
        //     There is the issue of properly associating test instances with test runs:
        //         For any top level test run, we expect that at least one entry of table test_instance is associated with it.
        //         Any one test instance is associated with only one test run.
        //         At the completion of a top level test run, if there is no test instance association, it means that other test runs supplanted our original test instance association.
        //             This situation may be legal, but it may not be intended, and should be investigated to prove that it is legal.
        //     We understand that the pseudo test runs of nested templates will return a result that may or may not be stored in table run.
        //         Regardless of that, these pseudo test runs may often, or even usually, not be connected to any test instance.

        // Our input is this.topDBTemplate. It has been filled from our reNum entry in table run and its one linked entry in table template. Its start_time and ready_time are filled and stored to table run.

        this.topDBTemplate.result = new Boolean(false);
        InstancedTemplate iT = null;
        boolean result;
        try {
            iT = runnerMachine.getTemplateProvider().getInstancedTemplate(this, this.topDBTemplate, runnerMachine);
            this.topDBTemplate.result = new Boolean(true); // no exception means success
            runnerMachine.getTemplateProvider().releaseTemplate(iT); // A top level template is never reused, so cleanup; this call then releases those nested templates that are not held for reuse
        } catch (Exception e) {
        	if (iT != null)
        		iT.destroy();
            throw e;
        } finally {
            result = this.topDBTemplate.result.booleanValue();
            log.debug(simpleName + "testRun() stores to database this result: " + result);
        	storeResultRunEntryData();
        }
        return result; // true
    }

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