package com.pslcl.dtf.runner.rest.storage.mysql;

import com.cstkit.common.mysql.MySQLConnectionPool;
import com.pslcl.dtf.core.runner.config.RestServiceStorageConfig;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.rest.module.Artifact;
import com.pslcl.dtf.core.runner.rest.module.Artifacts;
import com.pslcl.dtf.core.runner.rest.module.Module;
import com.pslcl.dtf.core.runner.rest.module.ModuleDetail;
import com.pslcl.dtf.core.runner.rest.module.Modules;
import com.pslcl.dtf.core.runner.rest.module.ReportModule;
import com.pslcl.dtf.core.runner.rest.module.ReportPlan;
import com.pslcl.dtf.core.runner.rest.module.ReportResult;
import com.pslcl.dtf.core.runner.rest.module.ReportTest;
import com.pslcl.dtf.core.runner.rest.module.Reports;
import com.pslcl.dtf.core.runner.rest.runRates.RunRate;
import com.pslcl.dtf.core.runner.rest.runRates.RunRates;
import com.pslcl.dtf.core.runner.rest.stats.Statistics;
import com.pslcl.dtf.core.runner.rest.testPlan.Test;
import com.pslcl.dtf.core.runner.rest.testPlan.TestPlan;
import com.pslcl.dtf.core.runner.rest.testPlan.TestPlanDetail;
import com.pslcl.dtf.core.runner.rest.testPlan.TestPlans;
import com.pslcl.dtf.core.runner.rest.userTest.UserTest;
import com.pslcl.dtf.core.runner.rest.userTest.UserTests;
import com.pslcl.dtf.runner.rest.storage.NotFoundException;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class MySQLRestServiceStorage implements RestServiceStorage
{
    private static final int MaxiumItemsPerPage = 100000;
//    private static final int NotFoundErrorCode = 1054;
//    private static final int AlreadyExistsErrorCode = 1062;
    private static final String Ascending = "asc";
    private static final String Descending = "desc";

    private final Logger logger;
    private volatile MySQLConnectionPool connectionPool;
    private volatile ExecutorService executor;
    private volatile RestServiceStorageConfig sconfig;
//    private volatile String database;

    private volatile String getStatisticsStatement;
    private volatile String getUserTestsNoOwnerStatement;
    private volatile String getUserTestsOwnerStatement;
    private volatile String getStartingCountStatement;
    private volatile String getRunningCountStatement;
    private volatile String getCompletedCountStatement;
//    private volatile String getModulesNoFilterStatement;
//    private volatile String getModulesFilterStatement;
    private volatile String getModulesStartSegmentStatement;
    private volatile String getModulesWhereSegmentStatement;
    private volatile String getModulesGroupByStatement;
    private volatile String getModulesOrderByNameStatement;
    private volatile String getModulesOrderByTestsStatement;
    private volatile String getModulesOrderByPlansStatement;
    private volatile String limitStatement;
    private volatile String OffsetStatement;
    private volatile String getModuleStatement;
    private volatile String getModuleArtifactsStartSegmentStatement;
    private volatile String getModuleArtifactsWhereAndSegmentStatement;
    private volatile String getModuleArtifactsGroupByStatement;
    private volatile String getModuleArtifactsOrderByNameStatement;
    private volatile String getModuleArtifactsOrderByTestsStatement;
    private volatile String getModuleArtifactsOrderByPlansStatement;
    private volatile String getModuleArtifactsOrderByConfigurationStatement;
    private volatile String getModuleReportStatement;

    private volatile String getPlansStartSegmentStatement;
    private volatile String getPlansWhereSegmentStatement;
    private volatile String getPlansGroupByStatement;
    private volatile String getPlansOrderByNameStatement;
    private volatile String getPlansOrderByTestsStatement;
    private volatile String getPlansOrderByDescriptionStatement;

    private volatile String getPlanStatement;
    private volatile String getPlanStartStatement;
    private volatile String getPlanWhereStatement;
    private volatile String getPlanWhereWithLikeStatement;

    public MySQLRestServiceStorage()
    {
        this.logger = LoggerFactory.getLogger(getClass());
    }

    public void init(RunnerConfig config) throws Exception
    {
        sconfig = RestServiceStorageConfig.propertiesToConfig(config);
        executor = config.blockingExecutor;
        String database = sconfig.schema;
        //@formatter:off
        getStatisticsStatement =
            "select (select count(*) from " + database + ".module) as module_count," +
	        "(select count(*) from " + database + ".test_plan) as test_plan_count," +
	        "(select count(*) from " + database + ".test) as test_count," +
	        "(select count(*) from " + database + ".content) as artifact_count," +
	        "(select count(*) from " + database + ".test_instance) as ti_count," +
	        "(select count(*) from " + database + ".test_instance where fk_run is null) as ti_pending," +
	        "(select count(*) from " + database + ".test_instance inner join " + database + ".run on run.pk_run = test_instance.fk_run where end_time is null) as ti_running," +
            "(select count(*) from " + database + ".module where module.pk_module not in(select module_to_test_instance.fk_module from " + database + ".module_to_test_instance)) as untested_module_count";
        getUserTestsNoOwnerStatement =
            "select test_instance.pk_test_instance, run.pk_run from " + database + ".test_instance " +
            "left join " + database + ".run on test_instance.fk_run = run.pk_run " +
            "where owner is not null and end_time is null";
        getUserTestsOwnerStatement =
            "select test_instance.pk_test_instance, run.pk_run from " + database + ".test_instance " +
            "left join " + database + ".run on test_instance.fk_run = run.pk_run " +
            "where owner=? and end_time is null";
        getStartingCountStatement =
            "select floor(unix_timestamp(start_time) + ?) as t,"+
	        "count(*) as total from " + database + ".run where start_time is not null " +
		    "and unix_timestamp(start_time) > ? and unix_timestamp(start_time) < ? group by t";
        getRunningCountStatement =
            "select floor(unix_timestamp(ready_time) + ?) as t," +
	        "count(*) as total from " + database + ".run where ready_time is not null group by t";
        getCompletedCountStatement =
            "select floor(unix_timestamp(end_time) + ?) as t," +
	        "count(*) as total from " + database + ".run where end_time is not null group by t";

        getModulesStartSegmentStatement =
            "select module.pk_module, module.organization, module.name, module.attributes, module.version, module.sequence," +
            "count(distinct fk_test) as tests, count(distinct fk_test_plan) as plans " +
            "from " + database + ".module " +
            "left join " + database + ".module_to_test_instance on (module.pk_module = module_to_test_instance.fk_module) " +
            "left join " + database + ".test_instance on (test_instance.pk_test_instance = module_to_test_instance.fk_test_instance) " +
            "left join " + database + ".test on (test.pk_test = test_instance.fk_test) " +
            "left join " + database + ".test_plan on (test_plan.pk_test_plan = test.fk_test_plan) ";
        getModulesWhereSegmentStatement =
            "where (module.name like '%?%' or organization like '%?%' or attributes like '%?%' or version like '%?%' or sequence like '%?%') ";
        getModulesGroupByStatement = "group by module.pk_module ";
        getModulesOrderByNameStatement =
            "order by module.organization ?, module.name ?, module.attributes ?, module.version ?, module.sequence ?, module.pk_module asc ";
        getModulesOrderByTestsStatement = "order by tests ?, module.pk_module asc ";
        getModulesOrderByPlansStatement = "order by plans ?, module.pk_module asc ";
        limitStatement = "limit ? ";
        OffsetStatement = "offset ?";

        getModuleStatement =
                "select pk_module, organization, name, version, sequence, attributes, scheduled_release, actual_release " +
                "from " + database + ".module where pk_module = ?";

        getModuleArtifactsStartSegmentStatement =
            "select artifact.pk_artifact, artifact.name, artifact.configuration, artifact.derived_from_artifact, artifact.merged_from_module, " +
            "count(distinct fk_test) as tests, count(distinct fk_test_plan) as plans " +
	        "from " + database + ".artifact " +
		    "left join " + database + ".artifact_to_dt_line on (artifact.pk_artifact = artifact_to_dt_line.fk_artifact) " +
            "left join " + database + ".dt_line on (dt_line.pk_dt_line = artifact_to_dt_line.fk_dt_line) " +
            "left join " + database + ".described_template on (described_template.pk_described_template = dt_line.fk_described_template) " +
            "left join " + database + ".test_instance on (test_instance.fk_described_template = described_template.pk_described_template) " +
            "left join " + database + ".test on (test.pk_test = test_instance.fk_test) " +
            "left join " + database + ".test_plan on (test_plan.pk_test_plan =  test.fk_test_plan) " +
            "where artifact.fk_module=? ";
        getModuleArtifactsWhereAndSegmentStatement =
            "and (artifact.name like '%?%' or artifact.configuration like '%?%') ";
        getModuleArtifactsGroupByStatement = "group by artifact.pk_artifact ";
        getModuleArtifactsOrderByNameStatement =
            "order by artifact.name ?, artifact.pk_artifact asc ";
        getModuleArtifactsOrderByTestsStatement = "order by tests ?, artifact.pk_artifact asc ";
        getModuleArtifactsOrderByPlansStatement = "order by plans ?, artifact.pk_artifact asc ";
        getModuleArtifactsOrderByConfigurationStatement = "order by configuration ?, artifact.pk_artifact asc";

        getModuleReportStatement = "call qa_portal.get_instance_list(null, null, ?, null)";

        getPlansStartSegmentStatement =
            "select test_plan.pk_test_plan, test_plan.name, test_plan.description, " +
            "count(distinct test.pk_test) as tests " +
            "from " + database + ".test_plan " +
            "left join " + database + ".test on (test.fk_test_plan = test_plan.pk_test_plan) ";
        getPlansWhereSegmentStatement = "where (test_plan.name like '%?%' or test_plan.description like '%?%') ";
        getPlansGroupByStatement = "group by test_plan.pk_test_plan ";
        getPlansOrderByNameStatement = "order by name ?, pk_test_plan asc ";
        getPlansOrderByTestsStatement =  "order by tests ?, pk_test_plan asc ";
        getPlansOrderByDescriptionStatement =  "order by description ?, pk_test_plan asc ";

        getPlanStatement =
            "select pk_test_plan, name, description " +
	        "from " + database + ".test_plan " +
		    "where pk_test_plan = ?";

        getPlanStartStatement =
            "select pk_test, fk_test_plan, name, description, script " +
            "from " + database + ".test ";
        getPlanWhereStatement = "where fk_test_plan = ? AND pk_test > ? ";
        getPlanWhereWithLikeStatement = "where fk_test_plan = ? and name like '%~%' and pk_test > ?";
       //@formatter:on
    }

    @Override
    public CompletableFuture<Statistics> getStatistics()
    {
        CompletableFuture<Statistics> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getStatistics()");
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(getStatisticsStatement);
                rs = stmt.executeQuery();
                rs.next();
                int modules = rs.getInt(1);
                int testPlans = rs.getInt(2);
                int tests = rs.getInt(3);
                int artifacts = rs.getInt(4);
                int instances = rs.getInt(5);
                int pending = rs.getInt(6);
                int running = rs.getInt(7);
                int untested = rs.getInt(8);
                return future.complete(new Statistics(modules, testPlans, tests, artifacts, instances, pending, running, untested));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<RunRates> getRunRates(final Long from, final Long to, final Long bucket)
    {
        CompletableFuture<RunRates> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            long bucketValue;
            long startTime;
            long endTime;
            if(from == null)
    //            startTime = (new Date().getTime() / 1000) - (7*24*60*60);
                startTime = (new Date().getTime()) - (7*24*60*60*1000);
            else
                startTime = from;
            if(to == null)
//                endTime = new Date().getTime() / 1000;
              endTime = new Date().getTime();
            else
                endTime = to;
            if(bucket == null)
                bucketValue = 3600L;
            else
                bucketValue = bucket;

            if(logger.isTraceEnabled())
                logger.trace("getRunRates({},{},{})", startTime, endTime, bucketValue);

            Connection conn = null;
            PreparedStatement startingStmt = null;
            PreparedStatement runningStmt = null;
            PreparedStatement completedStmt = null;
            ResultSet startingRs = null;
            ResultSet runningRs = null;
            ResultSet completedRs = null;
            try
            {
                conn = connectionPool.getConnection();
                startingStmt = conn.prepareStatement(getStartingCountStatement);
                startingStmt.setLong(1, bucketValue);
                startingStmt.setLong(2, startTime);
                startingStmt.setLong(3, endTime);
                runningStmt = conn.prepareStatement(getRunningCountStatement);
                runningStmt.setLong(1, bucketValue);
                completedStmt = conn.prepareStatement(getCompletedCountStatement);
                completedStmt.setLong(1, bucketValue);
                startingRs = startingStmt.executeQuery();
                runningRs = runningStmt.executeQuery();
                completedRs = completedStmt.executeQuery();
                List<RunRate> startingList = new ArrayList<>();
                List<RunRate> runningList = new ArrayList<>();
                List<RunRate> completedList = new ArrayList<>();
                while(startingRs.next())
                    startingList.add(new RunRate(startingRs.getLong(1), startingRs.getInt(2)));
                while(runningRs.next())
                    runningList.add(new RunRate(runningRs.getLong(1), runningRs.getInt(2)));
                while(completedRs.next())
                    completedList.add(new RunRate(completedRs.getLong(1), completedRs.getInt(2)));
                return future.complete(new RunRates(dateFromLong(startTime), dateFromLong(endTime), startTime, endTime, bucketValue, startingList, runningList, completedList));
            }
            catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(startingStmt, null, startingRs);
                stmtCleanup(runningStmt, null, runningRs);
                stmtCleanup(completedStmt, conn, completedRs);
            }
        });
        return future;
    }


    @Override
    public CompletableFuture<UserTests> getUserTests(String owner)
    {
        CompletableFuture<UserTests> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getUserTests({})", owner);
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = connectionPool.getConnection();
                if(owner == null)
                    stmt = conn.prepareStatement(getUserTestsNoOwnerStatement);
                else
                {
                    stmt = conn.prepareStatement(getUserTestsOwnerStatement);
                    stmt.setString(1, owner);
                }
                rs = stmt.executeQuery();
                List<UserTest> list = new ArrayList<>();
                while(rs.next())
                {
                    int testId = rs.getInt(1);
                    int runId = rs.getInt(2);
                    list.add(new UserTest(testId, runId));
                }
                return future.complete(new UserTests(list));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Modules> getModules(String filter, final String order, String limit, Integer offset)
    {
        CompletableFuture<Modules> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getModules({},{},{},{})", filter, order, limit, offset);

            ParametersInfo params = null;
            try{params = handleParameters(order, limit, offset);}catch(IllegalArgumentException e){return future.completeExceptionally(e);}
            if(!params.order.equals(Modules.OrderNameValue) && !params.order.equals(Modules.OrderTestValue) && !params.order.equals(Modules.OrderPlanValue))
                return future.completeExceptionally(new IllegalArgumentException(Modules.OrderParam + ": " + params.order + " is not a valid value"));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String sql = getModulesStartSegmentStatement;
            if(filter != null)
            {
                sql += getModulesWhereSegmentStatement;
                sql = sql.replace("?", filter);
            }
            sql += getModulesGroupByStatement;
            switch(params.order)
            {
                case Modules.OrderNameValue:
                    sql += getModulesOrderByNameStatement;
                    break;
                case Modules.OrderTestValue:
                    sql += getModulesOrderByTestsStatement;
                    break;
                default:
                    sql += getModulesOrderByPlansStatement;
                    break;
            }
            sql = sql.replace("?", params.direction);
            if(params.limit != -1)
            {
                sql += limitStatement;
                sql = sql.replace("?", "" + params.limit);
            }
            if(offset != null)
            {
                sql += OffsetStatement;
                sql = sql.replace("?", ""+offset);
            }

            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                List<Module> list = new ArrayList<>();
                while(rs.next())
                {
                    long module = rs.getLong(1);
                    String org = rs.getString(2);
                    String name = rs.getString(3);
                    String attrs = rs.getString(4);
                    String version = rs.getString(5);
                    String seq = rs.getString(6);
                    int tests = rs.getInt(7);
                    int plans = rs.getInt(8);
                    list.add(new Module(module, org, name, attrs, version, seq, tests, plans));
                }
                return future.complete(new Modules(list));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<ModuleDetail> getModule(String moduleId)
    {
        CompletableFuture<ModuleDetail> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getModule({})", moduleId);
            if(moduleId == null)
                return future.completeExceptionally(new IllegalArgumentException("module id is null"));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(getModuleStatement);
                stmt.setString(1, moduleId);
                rs = stmt.executeQuery();
                boolean found = rs.next();
                if(!found)
                    return future.completeExceptionally(new NotFoundException("Module ID: " + moduleId + " not found"));
                long pk_module = rs.getLong(1);
                String organization = rs.getString(2);
                String name = rs.getString(3);
                String version = rs.getString(4);
                String sequence = rs.getString(5);
                String attributes = rs.getString(6);
                java.sql.Date date = rs.getDate(7);
                java.time.LocalDate scheduled_release = null;
                if(!rs.wasNull())
                    scheduled_release = date.toLocalDate();
                date = rs.getDate(8);
                LocalDate actual_release = null;
                if(!rs.wasNull())
                    actual_release = date.toLocalDate();
                return future.complete(new ModuleDetail(pk_module, organization, name, attributes, version, sequence, scheduled_release, actual_release));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    public CompletableFuture<Artifacts> getArtifactsForModule(String moduleId, String filter, String order, String limit, Integer offset)
    {
        CompletableFuture<Artifacts> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getArtifactsForModule({},{},{},{},{})", moduleId, filter, order, limit, offset);
            if(moduleId == null)
                return future.completeExceptionally(new IllegalArgumentException("module id is null"));

            ParametersInfo params = null;
            try{params = handleParameters(order, limit, offset);}catch(IllegalArgumentException e){return future.completeExceptionally(e);}
            if(!params.order.equals(Modules.OrderNameValue) && !params.order.equals(Modules.OrderTestValue) && !params.order.equals(Modules.OrderPlanValue) && !params.order.equals(Artifacts.OrderConfigurationValue))
                return future.completeExceptionally(new IllegalArgumentException(Modules.OrderParam + ": " + params.order + " is not a valid value"));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String sql = getModuleArtifactsStartSegmentStatement;
            sql = sql.replace("?", moduleId);

            if(filter != null)
            {
                sql += getModuleArtifactsWhereAndSegmentStatement;
                sql = sql.replace("?", filter);
            }
            sql += getModuleArtifactsGroupByStatement;
            switch(params.order)
            {
                case Modules.OrderNameValue:
                    sql += getModuleArtifactsOrderByNameStatement;
                    break;
                case Modules.OrderTestValue:
                    sql += getModuleArtifactsOrderByTestsStatement;
                    break;
                case Modules.OrderPlanValue:
                    sql += getModuleArtifactsOrderByPlansStatement;
                    break;
                default:
                    sql += getModuleArtifactsOrderByConfigurationStatement;
                    break;
            }
            sql = sql.replace("?", params.direction);
            if(params.limit != -1)
            {
                sql += limitStatement;
                sql = sql.replace("?", ""+params.limit);
            }
            if(offset != null)
            {
                sql += OffsetStatement;
                sql = sql.replace("?", ""+offset);
            }

            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                List<Artifact> list = new ArrayList<>();
                while(rs.next())
                {
                    long artifactId = rs.getLong(1);
                    String name = rs.getString(2);
                    String config = rs.getString(3);
                    long derived = rs.getLong(4);
                    long mergedFrom = rs.getLong(5);
                    int tests = rs.getInt(6);
                    int plans = rs.getInt(7);
                    list.add(new Artifact(artifactId, name, config, derived, mergedFrom, tests, plans));
                }
                return future.complete(new Artifacts(list));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<Reports> getReportsForModule(String moduleId)
    {
        CompletableFuture<Reports> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getReportsForModule({})", moduleId);
            if(moduleId == null)
                return future.completeExceptionally(new IllegalArgumentException("module id is null"));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(getModuleReportStatement);
                stmt.setString(1, moduleId);
                rs = stmt.executeQuery();
//                rs = stmt.getResultSet();
                boolean hasNext = rs.next();
                if(!hasNext)
                    return future.completeExceptionally(new NotFoundException("Module ID: " + moduleId + " not found"));
                List<ReportResult> resultList = new ArrayList<>();
                List<ReportPlan> planList = new ArrayList<>();
                List<ReportTest> testList = new ArrayList<>();
                List<ReportModule> moduleList = new ArrayList<>();
                do
                {
                    long testPlan = rs.getLong(1);
                    long test = rs.getLong(2);
                    long instance = rs.getLong(3);
                    String desc = rs.getString(4);
                    String modules = rs.getString(5);
                    Boolean result = rs.getBoolean(6);
                    if(rs.wasNull())
                        result = null;
                    Instant instant = null;
                    Timestamp ts = rs.getTimestamp(7);
                    if(!rs.wasNull())
                        instant = ts.toInstant();
                    resultList.add(new ReportResult(testPlan, test, instance, desc, modules, result, instant));
                }while(rs.next());
                rs.close();
                hasNext = stmt.getMoreResults();
                if(hasNext)
                {
                    rs = stmt.getResultSet();
                    while(rs.next())
                    {
                        long testPlan = rs.getLong(1);
                        String name = rs.getString(2);
                        String desc = rs.getString(3);
                        planList.add(new ReportPlan(testPlan, name, desc));
                    }
                    rs.close();
                    hasNext = stmt.getMoreResults();
                    if(hasNext)
                    {
                        rs = stmt.getResultSet();
                        while(rs.next())
                        {
                            long test = rs.getLong(1);
                            String name = rs.getString(2);
                            String desc = rs.getString(3);
                            testList.add(new ReportTest(test, name, desc));
                        }
                        rs.close();
                        hasNext = stmt.getMoreResults();
                        if(hasNext)
                        {
                            rs = stmt.getResultSet();
                            while(rs.next())
                            {
                                long module = rs.getLong(1);
                                long offset = rs.getLong(2);
                                String org = rs.getString(3);
                                String name = rs.getString(4);
                                String version = rs.getString(5);
                                String sequence = rs.getString(6);
                                String attributes = rs.getString(7);
                                moduleList.add(new ReportModule(module, offset, org, name, version, sequence, attributes));
                            }
                            rs.close();
                        }
                    }
                }
                return future.complete(new Reports(resultList, planList, testList, moduleList));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<TestPlans> getTestPlans(String filter, final String order, String limit, Integer offset)
    {
        CompletableFuture<TestPlans> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getTestPlans({},{},{},{})", filter, order, limit, offset);

            ParametersInfo params = null;
            try{params = handleParameters(order, limit, offset);}catch(IllegalArgumentException e){return future.completeExceptionally(e);}
            if(!params.order.equals(Modules.OrderNameValue) && !params.order.equals(Modules.OrderTestValue) && !params.order.equals(TestPlans.OrderDescriptionValue))
                return future.completeExceptionally(new IllegalArgumentException(Modules.OrderParam + ": " + params.order + " is not a valid value"));
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String sql = getPlansStartSegmentStatement;
            if(filter != null)
            {
                sql += getPlansWhereSegmentStatement;
                sql = sql.replace("?", filter);
            }
            sql += getPlansGroupByStatement;
            switch(params.order)
            {
                case Modules.OrderNameValue:
                    sql += getPlansOrderByNameStatement;
                    break;
                case Modules.OrderTestValue:
                    sql += getPlansOrderByTestsStatement;
                    break;
                default:
                    sql += getPlansOrderByDescriptionStatement;
                    break;
            }
            sql = sql.replace("?", params.direction);
            if(params.limit != -1)
            {
                sql += limitStatement;
                sql = sql.replace("?", ""+params.limit);
            }
            if(offset != null)
            {
                sql += OffsetStatement;
                sql = sql.replace("?", ""+offset);
            }

            try
            {
                conn = connectionPool.getConnection();
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                List<TestPlan> list = new ArrayList<>();
                while(rs.next())
                {
                    long plan = rs.getLong(1);
                    String name = rs.getString(2);
                    String desc = rs.getString(3);
                    int plans = rs.getInt(4);
                    list.add(new TestPlan(plan, name, desc, plans));
                }
                return future.complete(new TestPlans(list));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(stmt, conn, rs);
            }
        });
        return future;
    }

    @Override
    public CompletableFuture<TestPlanDetail> getTestPlan(String planId, String filter, String after)
    {
        CompletableFuture<TestPlanDetail> future = new CompletableFuture<>();
        executor.submit(() ->
        {
            if(logger.isTraceEnabled())
                logger.trace("getTestPlan({})", planId);
            if(planId == null)
                return future.completeExceptionally(new IllegalArgumentException("plan id is null"));
            Connection conn = null;
            PreparedStatement planStmt = null;
            ResultSet planRs = null;
            PreparedStatement testsStmt = null;
            ResultSet testsRs = null;
            try
            {
                conn = connectionPool.getConnection();
                planStmt = conn.prepareStatement(getPlanStatement);
                planStmt.setString(1, planId);
                planRs = planStmt.executeQuery();
                boolean found = planRs.next();
                if(!found)
                    return future.completeExceptionally(new NotFoundException("Plan ID: " + planId + " not found"));
                long pk_test_plan = planRs.getLong(1);
                String planName = planRs.getString(2);
                String planDesc = planRs.getString(3);

                long afterValue = 0L;
                if(after != null)
                {
                    try
                    {
                        afterValue = Long.parseLong(after);
                    }catch(NumberFormatException ignored)
                    {
                        return future.completeExceptionally(new IllegalArgumentException(TestPlan.AfterParam + ": " + after + " is not a valid value, must be an integer"));
                    }
                }
                String sql = getPlanStartStatement;
                if(filter != null)
                {
                    sql += getPlanWhereWithLikeStatement;
                    sql = sql.replace("~", filter);
                }
                else
                    sql += getPlanWhereStatement;
                testsStmt = conn.prepareStatement(sql);
                testsStmt.setString(1, planId);
                testsStmt.setLong(2, afterValue);
                testsRs = testsStmt.executeQuery();
                List<Test> list = new ArrayList<>();
                while (testsRs.next())
                {
                    long pk_test = testsRs.getLong(1);
                    long fk_test_plan = testsRs.getLong(2);
                    String name = testsRs.getString(3);
                    String desc = testsRs.getString(4);
                    list.add(new Test(pk_test, fk_test_plan, name, desc, null));
                }
                return future.complete(new TestPlanDetail(pk_test_plan, planName, planDesc, list));
            }catch(SQLException e)
            {
                if(logger.isWarnEnabled())
                    logger.warn("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }catch(Exception e)
            {
                if(logger.isErrorEnabled())
                    logger.error("Exception querying database: ", e);
                return future.completeExceptionally(e);
            }finally
            {
                stmtCleanup(planStmt, null, planRs);
                stmtCleanup(testsStmt, conn, testsRs);
            }
        });
        return future;
    }

    public ParametersInfo handleParameters(final String order, String limit, Integer offset) throws IllegalArgumentException
    {
        String theOrder = order;
        String direction = Ascending;
        if(order != null)
        {
            int idx = order.indexOf(Modules.AscendingFlag);
            if(idx != -1)
                theOrder = order.substring(1);
            idx = order.indexOf(Modules.DescendingFlag);
            if(idx != -1)
            {
                theOrder = order.substring(1);
                direction = Descending;
            }
        }else
            theOrder = Modules.OrderNameValue;

        int theLimit = sconfig.maxPageItems;
        if(limit != null)
        {
            if(limit.equals(Modules.LimitAllValue))
            {
                theLimit = -1;      // don't append sql limit flag
                if(offset != null)
                    theLimit = MaxiumItemsPerPage;
            }else
            {
                try
                {
                    theLimit = Integer.parseInt(limit);
                }catch(Exception e)
                {
                    throw new IllegalArgumentException(Modules.LimitParam + " is not an integer value, or all");
                }
            }
        }
        return new ParametersInfo(theOrder, direction, theLimit);
    }

    public void start()
    {
        connectionPool = new MySQLConnectionPool(sconfig.host, sconfig.port, sconfig.schema, sconfig.user, sconfig.password, logger);
    }

    public void destroy()
    {
    }

    private void stmtCleanup(Statement stmt, Connection conn, ResultSet rs)
    {
        try
        {
            if(rs != null)
                rs.close();
        }catch(Exception rsEx)
        {
            logger.debug("rs did not close cleanly", rsEx); //ignore
        }
        try
        {
            if(stmt != null)
                stmt.close();
        }catch(Exception stmtEx)
        {
            logger.debug("stmt did not close cleanly", stmtEx); //ignore
        }
        try
        {
            if(conn != null)
                conn.close();
        }catch(Exception connEx)
        {
            logger.debug("Connection did not close cleanly", connEx); //ignore
        }
    }

    private String dateFromLong(long timestamp)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    @SuppressWarnings("unused")
    private long timestampFromString(String dateStr) throws ParseException
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
        Date date = sdf.parse(dateStr);
        return date.getTime();
    }

    private class ParametersInfo
    {
        private final String order;
        private final String direction;
        private final int limit;

        private ParametersInfo(String order, String direction, int limit)
        {
            this.order = order;
            this.direction = direction;
            this.limit = limit;
        }

    }
}
