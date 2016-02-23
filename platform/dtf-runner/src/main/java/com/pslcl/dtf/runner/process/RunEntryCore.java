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
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.runner.DBConnPool;
import com.pslcl.dtf.runner.RunnerService;
import com.pslcl.dtf.runner.template.InstancedTemplate;

/**
 * RunEntryCore is used to instantiate templates.
 * From a top level template down, each nested template calls this class. Hence, it is recursive.
 */
public class RunEntryCore {

    // static methods
	
    /**
     *
     * @param dbConnPool
     * @param reNum
     * @return null if no if no result is stored, or true or false
     */
    static public Boolean getResult(DBConnPool dbConnPool, long reNum) throws Exception {
    	Boolean retBoolean = null;
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
        	connection = dbConnPool.getConnection();
            statement = connection.createStatement();
            String strRENum = String.valueOf(reNum);
            resultSet = statement.executeQuery( "SELECT pk_run, result " +
                                                "FROM run " +
                                                "WHERE pk_run = " + strRENum );
            if ( resultSet.next() ) {
                retBoolean = resultSet.getBoolean("result"); // note: false is returned for SQL_NULL
                // the really cool api is: getBoolean first to get true or false, but only AFTER that, check to see if database column really holds null.
                if (resultSet.wasNull())
                	retBoolean = null;
                if (resultSet.next())
                    throw new Exception("resultSet wrongly has more than one entry");
            } else {
                throw new Exception("template data not present");
            }
        } catch(Exception e) {
            LoggerFactory.getLogger("RunEntryCore").error("getResult() exception for reNum " + reNum + ": "+ e);
            throw e;
        } finally {
            try {
                if ( resultSet != null )
                    resultSet.close();
            } catch ( Exception e ) {
                // ignore
            }
            resultSet = null;
            
            try {
                if ( statement != null )
                	statement.close();
            } catch ( Exception e ) {
                // ignore
            }
            statement = null;
            
            connection.close();
        }
    	return retBoolean;
    }	

    
    // class instance members
    
    private final Logger log;
    private final String simpleName;
    private DBConnPool dbConnPool;
    private Long reNum;
    private DBTemplate topDBTemplate = null; // describes the top-level template only, i.e. the template that is matched to the run table entry known as reNum
    private CancelTask cancelTask;
    private boolean testRunIsCanceled;


    // private methods

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
    
    /**
     * load data for reNum in topDBTemplate
     */
    private void loadRunEntryData() throws Exception {
        // meant to be called once only; if more than once becomes useful, might work but review
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
        	connection = this.dbConnPool.getConnection();
        	statement = connection.createStatement();
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
                    topDBTemplate.result = (resultSet.getObject("result") != null) ? resultSet.getBoolean("result") : null;
                    topDBTemplate.owner = resultSet.getString("owner");
                    
                    topDBTemplate.start_time = resultSet.getDate("start_time");
                    Timestamp timestamp = resultSet.getTimestamp("start_time");
                    if (timestamp != null)
                    	topDBTemplate.start_time = new Date(timestamp.getTime());
                    
                    topDBTemplate.ready_time = resultSet.getDate("ready_time");
                    timestamp = resultSet.getTimestamp("ready_time");
                    if (timestamp != null)
                    	topDBTemplate.ready_time = new Date(timestamp.getTime());
                    
                    topDBTemplate.end_time = resultSet.getDate("end_time");
                    timestamp = resultSet.getTimestamp("end_time");
                    if (timestamp != null)
                    	topDBTemplate.end_time = new Date(timestamp.getTime());
                    
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
        	// can get here when MySQL Server is not running
            log.error(simpleName + "loadRunEntryData() exception for reNum " + this.reNum + ": "+ e);
            throw e;
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
            if (connection != null)
            	connection.close();
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
		try { 
	        writeRunEntryData(); // ready_time and owner have changed
		} catch (Exception e) {
			log.debug(this.simpleName + "storeReadyRunEntryData() database write failure");
			throw e;
		}
    }

    /**
     *
     */
    private void storeResultRunEntryData() throws Exception {
        topDBTemplate.end_time = new Date(); // now
        try {
            writeRunEntryData();
        } catch (Exception e) {
			log.debug(this.simpleName + "storeResultRunEntryData() database write failure");
			throw e;
        }
    }

    /**
     * @note: if reNum.result field has a value (is not null), this does not overwrite anything
     * @note: does not overwrite run.pk_run, run.fk_template, run.start_time; dtf-runner does not own these
     * @note: dtf-runner also do not own table template, and does not write it
     *
     */
    private void writeRunEntryData() throws Exception {
        // temporarily, comment out these next 5 lines, to allow overwriting a past non-null result
//    	Boolean previousResultIsStored = RunEntryCore.getResult(this.dbConnPool, this.reNum);
//    	if (previousResultIsStored != null) { // this specific test is a fail-safe
//    		log.warn(simpleName + "writeRunEntryData() does not overwrite a previously stored result, for reNum " + topDBTemplate.reNum);
//    		throw new Exception("Database write not accomplished");
//    	} else
    		
    	{
    		if (!this.dbConnPool.getReadOnly()) {
                Connection connection = null;
    	        Statement statement = null;
    	        try {
    	        	connection = this.dbConnPool.getConnection();
    	            connection.setAutoCommit(true);
    	        	statement = connection.createStatement();
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
   	                Boolean result = topDBTemplate.result;
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
    	            throw e;
    	        } finally {
    	            safeClose( statement ); statement = null;
    	            connection.close();
    	        }    			
    		} else {
    			log.warn(simpleName + "writeRunEntryData() finds readonly status; does not write a result, for reNum " + topDBTemplate.reNum);
    			throw new Exception("Database write not accomplished");
    		}
    	}
    }


    // public methods

    /** constructor
     *
     * @param pk_described_template
     * @throws Exception 
     */
    public RunEntryCore(DBConnPool dbConnPool, Long reNum) throws Exception {
    	this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.dbConnPool = dbConnPool;
        this.reNum = reNum;
        topDBTemplate = new DBTemplate(this.reNum);
        this.cancelTask = null;
        this.testRunIsCanceled = false;
        try {
          	// fill all fields of topDBTemplate from the existing existing database entries
			loadRunEntryData(); // throws Exception for no database entry for this.reNum
			storeReadyRunEntryData(); // can throw IllegalArgumentException for template.enabled false, and SQL write related exceptions
		} catch (Exception e) {
			throw e;
		}
    }

    public DBTemplate getDBTemplate() {
    	return topDBTemplate;
    }
    
    public long getRENum() {
    	return reNum;
    }
    
    /**
     * 
     * @param templateHash
     * @return
     */
    public DBTemplate getTemplateInfo(String templateHash) throws Exception {
    	DBTemplate retDBTemplate = new DBTemplate(-1L);
    	retDBTemplate.hash = templateHash.getBytes();
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
        	connection = this.dbConnPool.getConnection();
        	statement = connection.createStatement();
        	String str = "SELECT pk_template, hash, enabled, steps " +
                         "FROM template " +
                         "WHERE hash = " + templateHash;
            resultSet = statement.executeQuery(str);
            if ( resultSet.next() ) {
                retDBTemplate.pk_template = resultSet.getLong("pk_template");
                retDBTemplate.hash = resultSet.getBytes("hash");
                retDBTemplate.enabled = resultSet.getBoolean("enabled");
                retDBTemplate.steps = resultSet.getString("steps");
            }
        } catch (Exception e) {
        	log.debug(this.simpleName + ".getTemplateInfo() does not find template for template hash " + templateHash);
        	throw e;
        } finally {
            safeClose( resultSet ); resultSet = null;
            safeClose( statement ); statement = null;
            if (connection != null)
            	connection.close();
        }
    	return retDBTemplate;
    }
    
    private void closeCancelTask() {
    	if (this.cancelTask != null)
    		this.cancelTask.close();
    }
    
    /**
     * Execute the test run specified by the run entry number.
     * @return the result of this test run; result is stored already
     */
    public boolean testRun(RunnerMachine runnerMachine) throws Exception {
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

        Boolean result = new Boolean(false);
        boolean testRunSuccess = false;
        InstancedTemplate iT = null;
        try {
        	// Setup test run cancellation, prior to starting our test run.
        	// 	   While a test run is in progress, a user can cancel it, by entering a fail result in "our" run table entry.  
        	// Setup local task to watch for on the fly run cancellation. Place it as a member of RunEntryCore (ie: this), passed into and accessible while the test run executes its template steps.
//        	this.cancelTask = new CancelTask(this, runnerMachine);
        	// temporarily, comment out the above line, to avoid CancelTask activity
        	
			log.debug(this.simpleName + ".testRun() launches template instantiation for top level template " + this.topDBTemplate.getTemplateId());
        	// Start our test run. This executes all the template steps of our top level template (represented by this.topDBTemplate).
            iT = runnerMachine.getTemplateProvider().getInstancedTemplate(this, this.topDBTemplate, runnerMachine);
            result = !iT.getForceNullResult() ? new Boolean(true) : // No exception means test run success.
                                                null;               // Although the test run succeeded, the result should be marked null. 
            testRunSuccess = true;
        } catch (Throwable t) {
        	log.debug(simpleName + "testRun errors out, reNum : " + this.reNum);
            throw t;
        } finally {
        	this.closeCancelTaskStoreResultAckMessageQueue(runnerMachine.getService(), result);
        }
        
    	// note: for !testRunSuccess, the template has already cleaned itself up (it was handled internally, by exception processing code, because we can't do it- iT is null for that)
        if (testRunSuccess)
            runnerMachine.getTemplateProvider().releaseTemplate(iT); // A top level template is never reused, so cleanup; this call then releases those nested templates that are not held for reuse
        	
        return testRunSuccess; // always true, for exception not thrown
    }
    
    public boolean isTestRunCanceled() {
    	return this.testRunIsCanceled;
    }
    
    /**
     * Never call this after the dtf-test-runner stores a false test run result.
     */
    void checkForRunCancel() {
		try {
			Boolean result = RunEntryCore.getResult(this.dbConnPool, this.reNum);
			// result null:  test run not canceled
			//        true:  test run passed, not canceled
			//        false: test run canceled by a force that is outside dtf-runner
			//    (or false could mean that we are called wrongly, after we stored a false test run result) 
	    	if (result!=null &&	result==false) {
	    		// cancel this test run
	    		this.testRunIsCanceled = true;
	    	}
	    	log.debug(this.simpleName + ".checkRunCancel() called for reNum " + this.reNum + ", finds test run " + (this.testRunIsCanceled?"CANCELED":"running"));
		} catch (Exception e) {
			log.warn(this.simpleName + "for reNum " + this.reNum + ", checkRunCancel exception, msg: " + e.getMessage());
			// swallow exception, we will check run cancel again, in a while.
		}
    }
    
    /**
     * 
     * @param runnerService
     * @param result
     * @throws Exception Swallows all operational exceptions, will throw things like null pointer exception.
     */
    public void closeCancelTaskStoreResultAckMessageQueue(RunnerService runnerService, Boolean result) throws Exception {
        // Our input is this.topDBTemplate. It has been filled from our reNum entry in table run and its one linked entry in table template. Its start_time and ready_time are filled and stored to table run.
    	this.closeCancelTask();
        this.topDBTemplate.result = result;
        this.storeResultAndAckMessageQueue(runnerService);
    }
    
    /**
     * 
     * @param runnerService
     * @throws Exception Swallows all operational exceptions, will throw things like null pointer exception.
     */
    private void storeResultAndAckMessageQueue(RunnerService runnerService) throws Exception {
    	boolean resultNowStored = false;
    	Boolean storedResult = RunEntryCore.getResult(this.dbConnPool, reNum);
    	if (storedResult == null) {
    		// note: we do not get here when test run is canceled
        	try {
    			storeResultRunEntryData();
    			resultNowStored = true;
    	        log.debug(this.simpleName + ".storeResultAndAckMessageQueue(), for reNum " + this.topDBTemplate.reNum + ", stored to database this result: " + this.topDBTemplate.result); // result can be null, true, or false
    		} catch (Exception e) {
    			// swallow this exception, it does not relate to the actual test run
                log.debug(this.simpleName + ".storeResultAndAckMessageQueue(), for reNum " + this.topDBTemplate.reNum + ", FAILED to store to database this result: " + this.topDBTemplate.result + "; message queue not acked"); // result can be null, true, or false
    		}
    	} else {
           	log.debug(simpleName + ".storeResultAndAckMessageQueue() finds result already stored for reNum " + this.topDBTemplate.reNum);
    	}

    	if (storedResult!=null || resultNowStored) {
           	// ack the message queue
    		
            // temporarily, comment out these next 9 lines, to prevent acking the message queue
//           	try {
//    			RunEntryState reState = runnerService.runEntryStateStore.get(this.reNum);
//    			Object message = reState.getMessage();
//    			runnerService.ackRunEntry(message);
//               	log.debug(simpleName + ".storeResultAndAckMessageQueue(), for reNum " + this.topDBTemplate.reNum + ", acked message queue");
//    		} catch (Exception e) {
//    			// swallow this exception, it does not relate to the actual test run
//                log.warn(this.simpleName + ".storeResultAndAckMessageQueue(), for reNum " + this.topDBTemplate.reNum + ", sees stored result but FAILED to ack the message queue");
//    		}
    	}
    }
    
        // unneeded- our real approach involves an api to come to us soon
//        // prove that api works to get files from the build machine
//        String endpoint = null;
//        QuickBuildArtifactProvider qbap = new QuickBuildArtifactProvider(endpoint);
//        try {
//            qbap.init();
//        } catch (Exception e) {
//            // Auto-generated catch block
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
//            // Auto-generated method stub
//
//        }
//
//    }

}