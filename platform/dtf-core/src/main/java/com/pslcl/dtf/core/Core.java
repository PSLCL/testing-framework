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
package com.pslcl.dtf.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//import javax.annotation.Nullable; // requires an external jar
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.generator.template.TestInstance;
import com.pslcl.dtf.core.generator.template.TestInstance.Action.ArtifactUses;

/**
 * This class represents the relationship between the program and external resources like
 * filesystems and databases. It also contains all of the synchronization code.
 */
public class Core
{
    private static class DBDescribedTemplate
    {
        private long pk = 0L;
        @SuppressWarnings("unused")
        private long fk_template = 0L; // Review: this is filled, but is it used thereafter?
        private DescribedTemplate.Key key = null;
        private Hash description = null;
    }

    @SuppressWarnings("unused")
    private static class DBTestInstance
    {
        private long fk_test = 0L; // INT(11) in test
        private String name = null; // VARCHAR(100) from test
        private String description = null; // LONGTEXT from test
        private String script = null; // VARCHAR(200) from test
        private long pk_test_instance = 0L;
        private long fk_described_template = 0L;
        private long fk_run = 0L;
        private long pk_template = 0L; // added here to avoid a lookup in the executeTestInstance()
    }

    @SuppressWarnings("unused")
    private static class DBTemplateInfo
    {
        private long pk_described_template = 0L; // INT(11) in described_template
        private byte[] fk_module_set = null; // BINARY(32) in described_template
        private long fk_template = 0L; // INT(11) in described_template
        private byte[] description_hash = null; // BINARY(32) in described_template
        private byte[] hash = null; // BINARY(32) in template
        private String steps = null; // MEDIUMTEXT in template
        private boolean enabled = false; // BOOLEAN in template
    }

    private final Logger log;
    private String singleQuote = "'";

    /**
     * The connection to the database.
     */
    private Connection connect = null;

    private File artifacts = null;

    // these 2 maps coordinate table described_template with table test_instance, to minimize table-reading activity
    //    these are filled by .loadHashes() and (.add() or .syncDescribedTemplates())
    //    these are read back by .add() and .check()
    private Map<DescribedTemplate.Key, DBDescribedTemplate> keyToDT = new HashMap<>();
    private Map<Long, Long> dtToTI = new HashMap<>();

    // this map had been filled by .loadHashes() and .add(), but was never read back (because an alternate approach is used to achieve its benefit)
    // private final Map<Long, DBDescribedTemplate> pkToDT = new HashMap<Long, DBDescribedTemplate>();

    /**
     * The private key of the test that is being generated.
     */
    private long pk_target_test = 0;
    private boolean read_only = false;

    private void loadHashes()
    {
        if (connect == null)
        {
            this.log.warn("<internal> Core.loadHashes() finds no database connection and exits");
            return;
        }

        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT pk_described_template, fk_module_set, fk_template, hash, description_hash FROM described_template JOIN template ON fk_template = pk_template");
            while (resultSet.next())
            {
                DBDescribedTemplate dbTemplate = new DBDescribedTemplate();
                dbTemplate.pk = resultSet.getLong("pk_described_template");
                dbTemplate.fk_template = resultSet.getLong("fk_template");
                dbTemplate.key = new DescribedTemplate.Key(new Hash(resultSet.getBytes("hash")), new Hash(resultSet.getBytes("fk_module_set")));
                dbTemplate.description = new Hash(resultSet.getBytes("description_hash"));
//              pkToDT.put(dbTemplate.pk, dbTemplate);

                if (keyToDT.containsKey(dbTemplate.key))
                {
                    safeClose(resultSet);
                    resultSet = null;
                    safeClose(statement);
                    statement = null;
                    throw new Exception("Duplicate DescribedTemplate.Key " + dbTemplate.pk + " " + dbTemplate.key.getTemplateHash().toString() + ":" + dbTemplate.key.getModuleHash().toString());
                }
                keyToDT.put(dbTemplate.key, dbTemplate);
            }

            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;

            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT pk_test_instance, fk_described_template FROM test_instance WHERE fk_test=" + Long.toString(pk_target_test));
            while (resultSet.next())
            {
                long pk = resultSet.getLong("pk_test_instance");
                long fk = resultSet.getLong("fk_described_template");
                dtToTI.put(fk, pk);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.loadHashes() could not read described_template, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }
    }

    // TODO: find out why this cannot be made private, since public .getConfig() is used by the caller
    @SuppressWarnings("PackageVisibleInnerClass")
    static class Config
    {
        private String db_host = null;
        private Integer db_port = null;
        private String db_user = null;
        private String db_password = null;
        private String db_schema = null;
        private String artifacts_dir = null;
        private String generators_dir = null;
        private String shell = null;
        private String sqs_endpoint = null;
        private String sqs_queue_name = null;
        private String sqs_access_key_id = null;
        private String sqs_secret_access_key = null;

        Config()
        {
            try
            {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("JavaScript");
                Bindings binding = engine.createBindings();
                binding.put("process", null);
                binding.put("env", System.getenv());
                binding.put("module", new NodeModule());
                binding.put("home_dir", Paths.get("").toAbsolutePath().normalize().toString());

                File fs = new File("portal/config/config.js");
                engine.eval(new FileReader(fs), binding);
                this.db_host = (String) engine.eval("config.mysql.host;", binding);
                this.db_port = (Integer) engine.eval("config.mysql.port;", binding);
                this.db_user = (String) engine.eval("config.mysql.user;", binding);
                this.db_password = (String) engine.eval("config.mysql.password;", binding);
                this.db_schema = (String) engine.eval("config.mysql.db;", binding);
                this.artifacts_dir = (String) engine.eval("config.artifacts_dir;", binding);
                this.generators_dir = (String) engine.eval("config.generators_dir;", binding);
                this.shell = (String) engine.eval("config.shell;", binding);
                this.sqs_endpoint = (String) engine.eval("config.sqs.endpoint;", binding);
                this.sqs_queue_name = (String) engine.eval("config.sqs.queue_name;", binding);
                this.sqs_access_key_id = (String) engine.eval("config.sqs.access_key_id;", binding);
                this.sqs_secret_access_key = (String) engine.eval("config.sqs.secret_access_key;", binding);
            } catch (Exception e)
            {
                LoggerFactory.getLogger(getClass()).warn("<internal> Core.Config constructor exception, msg: " + e);
            }
        }

        private String dbHost()
        {
            return db_host;
        }

        private Integer dbPort()
        {
            return db_port;
        }

        private String dbUser()
        {
            return db_user;
        }

        private String dbPassword()
        {
            return db_password;
        }

        private String dbSchema()
        {
            return db_schema;
        }

        private String dirArtifacts()
        {
            return artifacts_dir;
        }

        String dirGenerators()
        {
            return generators_dir;
        }

        String shell()
        {
            return shell;
        }

        String sqsEndpoint(){
            return sqs_endpoint;
        }

        String sqsQueueName(){
            return sqs_queue_name;
        }

        String sqsAccessKeyID(){
            return sqs_access_key_id;
        }

        String sqsSecretAccessKey(){
            return sqs_secret_access_key;
        }
    }

    private Config config = new Config();

    public Config getConfig()
    {
        return config;
    }

    public Core(long pk_test)
    {
        this.log = LoggerFactory.getLogger(getClass());
        this.pk_target_test = pk_test;

        String dir = config.dirArtifacts();
        if (dir != null)
            this.artifacts = new File(dir);
        else {
            this.log.error("<internal> Core constructor: null artifact directory, return with no further action");
            return;
        }

        if (!this.artifacts.isDirectory())
           //noinspection ResultOfMethodCallIgnored
            this.artifacts.mkdirs();

        openDatabase();

        if (connect == null)
        {
            this.log.error("<internal> Core constructor fails without database connection");
        } else
        {
            /* Load the description and template hashes */
            loadHashes();

            //            loadTestInstances();
        }
    }

    /**
     * Close the core object, releasing any resources.
     */
    public void close()
    {
        closeDatabase();
    }

    private static class NodeModule
    {
//      @SuppressWarnings("unused")
//      public Object exports = null;
    }

    /**
     * Open the database connection if it is not already open. Environment variables are used
     * to determine the DB host, user, and password. The DTF_DB_HOST variable is required.
     * DTF_DB_USER and DTF_DB_PASSWORD are optional, and if not specified then a guest
     * account with no password is used. This sets the 'read_only' flag, which disables all
     * database modifications.
     */
    private void openDatabase()
    {
        if (connect != null)
            return;

        try
        {

            // Setup the connection with the DB
            read_only = false;
            String user = config.dbUser();
            String password = config.dbPassword();
            if (user == null || password == null)
            {
                user = "guest";
                password = "";
                read_only = true;
            }

            String connectstring = String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s", config.dbHost(), config.dbPort(), config.dbSchema(), user, password);
            // TODO: replace superseded javax.sql.DriverManager with javax.sql.DataSource
            connect = DriverManager.getConnection(connectstring);
        } catch (Exception e)
        {
            this.log.error("<internal> Core.openDatabase() could not open database connection, " + e.getMessage());
            read_only = true;
        }
    }

    /**
     * Close the database connection if it is open.
     */
    private void closeDatabase()
    {
        try
        {
            if (connect != null)
            {
                connect.close();
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.closeDatabase() could not close database connection, " + e.getMessage());
        } finally
        {
            connect = null;
        }
    }

    private void safeClose(ResultSet r)
    {
        try
        {
            if (r != null)
                r.close();
        } catch (Exception ignore)
        {
            // Ignore
        }
    }

    private void safeClose(Statement s)
    {
        try
        {
            if (s != null)
                s.close();
        } catch (Exception ignore)
        {
            // Ignore
        }
    }

    public boolean isReadOnly()
    {
        return read_only;
    }

//  private Map<Long, DBTestInstance> pktiToTI = new HashMap<Long, DBTestInstance>();
//    /**
//     * Return the "ready" test instance matching the specified test instance number.
//     * @param testInstanceNumber The specified test instance number.
//     * @return The test instance, or null if the corresponding test instance does not exist or has a matching run table entry.
//     */
//    @SuppressWarnings("ConditionalExpressionWithNegatedCondition")
//    private DBTestInstance readReadyTestInstance_testInstance(long testInstanceNumber)
//    {
//        DBTestInstance retVal = pktiToTI.get(testInstanceNumber);
//        return (retVal.fk_run != 0) ? null : // null for filled .fk_run (shows that test instance is past being "ready")
//                                      retVal;
//    }

//    /**
//     * Return a set of all test instances matching the specified test number and test instance number that have not yet run and are ready to run.
//     * @param testNumber The specified test number.
//     * @param testInstanceNumber The specified test instance number.
//     * @return The set
//     */
//    Set<Long> readReadyTestInstances_test(long testNumber, long testInstanceNumber)
//    {
//        Set<Long> retSet = new HashSet<Long>();
//        Set<Long> fullSet = readReadyTestInstances_test(testNumber);
//        if (fullSet.contains(testInstanceNumber))
//        {
//            for (Long setMember : fullSet)
//            {
//                if (setMember == testInstanceNumber)
//                    retSet.add(setMember);
//            }
//        }
//        return retSet;
//    }

//    private Map<Long, List<DBTestInstance>> pktToTI = new HashMap<Long, List<DBTestInstance>>();
//    /**
//     * Return a set of all test instances matching the specified test number that have not yet run and are ready to run.
//     * @param testNumber The specified test number.
//     * @return The set.
//     */
//    private Set<Long> readReadyTestInstances_test(long testNumber)
//    {
//        Set<Long> retSet = new HashSet<Long>();
//        List<DBTestInstance> list_pkTest_ToMany_pkTestInstance = pktToTI.get(testNumber);
//        if (list_pkTest_ToMany_pkTestInstance != null)
//        {
//            for (DBTestInstance dbti : list_pkTest_ToMany_pkTestInstance)
//            {
//                if (dbti.fk_run == 0) // 0: null in database: not yet run
//                    retSet.add(dbti.pk_test_instance);
//            }
//        }
//        return retSet;
//    }

//    /**
//     * From a given test instance number, execute the corresponding test instance (aka test run).
//     *
//     *  @param testInstanceNumber The test instance number
//     */
//    public void executeTestInstance(long testInstanceNumber)
//    {
//        // We are an independent process. We have access to the database,
//        //   to a Resource Manager that has access to artifacts and resources,
//        //   and to everything else needed to cause our test instance to be executed.
//
//        DBTestInstance dbti = readReadyTestInstance_testInstance(testInstanceNumber);
//        if (dbti != null && dbti.fk_described_template != 0)
//        {
//            // This simple line requires that .pk_template be placed in DBTestInstance.
//            String str_fkTemplate = Long.toString(dbti.pk_template);
//
////          // This lookup works just as well to fill str_fkTemplate, and does not require the presence of .pk_template being placed in DBTestInstance.
////          //  We choose instead to eliminate map pkToDT, which has no other use
////          DBDescribedTemplate dbdt = pkToDT.get(Long.valueOf(dbti.fk_described_template));
////          str_fkTemplate = Long.toString(dbdt.fk_template);
//
//            Statement statement = null;
//            ResultSet resultSet = null;
//            try
//            {
//                statement = connect.createStatement();
//                resultSet = statement.executeQuery("SELECT pk_described_template, fk_module_set, pk_template, description_hash, hash, steps, enabled" +
//                        "                          FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template" +
//                        "                          WHERE pk_test_instance = " + Long.toString(testInstanceNumber) + " AND pk_template = " + str_fkTemplate);
//                // exactly one resultSet (because we required test_instance.fk_described_template to match described_template.pk_described_template)
//                if (resultSet.next())
//                {
//                    DBTemplateInfo dbtemplateinfo = new DBTemplateInfo();
//                    dbtemplateinfo.pk_described_template = resultSet.getLong("pk_described_template"); // table entry will not be null
//                    dbtemplateinfo.fk_module_set = resultSet.getBytes("fk_module_set");
//                    dbtemplateinfo.pk_template = resultSet.getLong("pk_template");
//                    dbtemplateinfo.description_hash = resultSet.getBytes("description_hash");
//                    dbtemplateinfo.hash = resultSet.getBytes("hash");
//                    dbtemplateinfo.steps = resultSet.getString("steps");
//                    dbtemplateinfo.enabled = resultSet.getBoolean("enabled");
//
//                    if (resultSet.next())
//                        this.log.warn("<internal> Core.executeTestInstance(): More than one ResultSet found. This is unexpected. Dropping all but the first ResultSet which was just accessed and which may be wrong data; test instance processing proceeds.");
//
//                    // dbtemplateinfo is used to execute this test instance by following steps; aka instantiate this test run to generate a test result
//                    this.log.trace("<internal> Core.executeTestInstance() has data base info for test instance " + dbti.pk_test_instance + " for test " + dbti.fk_test + ", finding described_template " + dbti.fk_described_template + " and template " + dbti.pk_template);
//                    this.log.trace("<internal> Core.executeTestInstance() finds test script: " + dbti.script);
//                    this.log.trace("<internal> Core.executeTestInstance() finds enabled: " + dbtemplateinfo.enabled);
//                    this.log.trace("<internal> Core.executeTestInstance() finds module set: " + dbtemplateinfo.fk_module_set);
//                    this.log.trace("<internal> Core.executeTestInstance() finds description_hash: " + dbtemplateinfo.description_hash);
//                    this.log.trace("<internal> Core.executeTestInstance() finds hash: " + dbtemplateinfo.hash);
//                    this.log.trace("<internal> Core.executeTestInstance() finds steps: \n" + dbtemplateinfo.steps);
//
//                    // Establish everything and make the test run execute.
//
//                    // Wait for a test result.
//
//                    // this simulates waiting for a test result; wait a random time from 1 to 5 seconds
//                    Random random = new Random();
//                    @SuppressWarnings("MagicNumber")
//                    int sleep = random.nextInt(5001 - 500) + 500; // 0.5 to 5 seconds
//                    try
//                    {
//                        Thread.sleep(sleep);
//                    } catch (Exception ignore)
//                    {
//                    }
//
//                    // Place the test result in the database and mark the test instance as complete in the database.
//
//                    // simulated false condition, but database is readonly for a while, so this gives a quick return without storing anything
//                    reportResult("junk", false, null, null, null, null);
//                    this.log.debug("<internal> Core.executeTestInstance() exits after execution msec of " + sleep + '\n');
//                } else
//                {
//                    this.log.warn("<internal> Core.executeTestInstance: no DBTemplateInfo found");
//                }
//            } catch (Exception e)
//            {
//                this.log.error("<internal> Core.executeTestInstance(): Could not read template table, " + e.getMessage());
//            } finally
//            {
//                try
//                {
//                    if (resultSet != null)
//                        resultSet.close();
//                    if (statement != null)
//                        statement.close();
//                } catch (SQLException ignore)
//                {
//                    // TODO: log this?
//                }
//            }
//        } else
//        {
//            this.log.warn("<internal> Core.executeTestInstance: ready test instance not found: " + testInstanceNumber);
//        }
//    }

    /**
     * Return a list of artifact providers.
     * @return The list of provider class names.
     */
    List<String> readArtifactProviders()
    {
        List<String> result = new ArrayList<String>();
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM artifact_provider");
            while (resultSet.next())
            {
                String name = resultSet.getString("classname");
                result.add(name);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.readArtifactProviders(): Could not read artifact providers, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return result;
    }

    void prepareToLoadModules()
    {
        if (read_only)
            return;

        // Update missing count.
        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("UPDATE module SET missing_count=missing_count+1");
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.prepareToLoadModules(): Could not supdate missing_count, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    void finalizeLoadingModules(int deleteThreshold)
    {
        if (read_only)
            return;

        // Remove modules that have been missing for too long.
        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("DELETE FROM module WHERE missing_count > ?");
            statement.setLong(1, deleteThreshold);
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.finalizeLoadingModules(): Could not delete module, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    /**
     * Add a test plan to the database.
     * @param name The name of the test plan.
     * @param description The description of the test plan.
     * @return The primary key of the new test plan, or zero if there is an error or in read-only mode. If the test plan already exists then
     * the existing primary key is returned;
     */
    long addTestPlan(String name, String description)
    {
        long pk = findTestPlan(name, description);

        // This will work in read-only mode to return an existing module.
        if (pk != 0 || read_only)
            return pk;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("INSERT INTO test_plan (name, description) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setString(2, description);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    pk = keys.getLong(1);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.addTestPlan(): Could not add test plan, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
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
    long addTest(long pk_test_plan, String name, String description, String script)
    {
        long pk = findTest(pk_test_plan, name, description, script);

        // This will work in read-only mode to return an existing module.
        if (pk != 0 || read_only)
            return pk;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("INSERT INTO test (fk_test_plan, name, description, script) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, pk_test_plan);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, script);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    pk = keys.getLong(1);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.addTest(): Could not add test, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }

        return pk;
    }

    /**
     * Add a module to the database.
     * @param module The module to add.
     * @return The primary key of the new module, or zero if there is an error or in read-only mode. If the module already exists then
     * the existing primary key is returned;
     */
    long addModule(Module module)
    {
        long pk = findModule(module);

        // This will work in read-only mode to return an existing module.
        if (pk != 0 || read_only)
            return pk;

        PreparedStatement statement = null;
        String attributes = new Attributes(module.getAttributes()).toString();
        //TODO: Release date, actual release date, order all need to be added.
        try
        {
            statement = connect.prepareStatement("INSERT INTO module (organization, name, attributes, version, status, sequence) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, module.getOrganization());
            statement.setString(2, module.getName());
            statement.setString(3, attributes);
            statement.setString(4, module.getVersion());
            statement.setString(5, module.getStatus());
            statement.setString(6, module.getSequence());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    pk = keys.getLong(1);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.addModule(): Could not add module, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }

        return pk;
    }

    /**
     * Delete a module.
     * @param pk_module The primary key of the module to delete.
     */
    private void deleteModule(long pk_module)
    {
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("DELETE FROM module WHERE pk_module=?");
            statement.setLong(1, pk_module);
            statement.executeUpdate();
            safeClose(statement);
            statement = null;

            statement = connect.prepareStatement("DELETE FROM artifact WHERE merged_from_module=?");
            statement.setLong(1, pk_module);
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.deleteModule: Could not delete module, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    /**
     * Delete all previous sequence build numbers of the same version as this module.
     * @param module The module that previous builds of should be deleted. It is required
     * that this module not be already added to the database.
     */
    void deletePriorBuildSequenceNumbers(Module module)
    {
        while (true) {
            long pk = findModuleWithoutPriorSequence(module);
            if (pk == 0)
                break;
            deleteModule(pk);
        }
    }

    /**
     * Add content given an input stream. If the content is already stored then a new file is created anyway,
     * assumed to be correct, and the new file is stored and the database is updated.
     * @param is An input stream for the content.
     * @param length The length of the stream, or (0 or -1) if the entire stream is to be added.
     * @return Hash of the added content
     */
//  @Nullable
    @SuppressWarnings("ReturnOfNull")
    Hash addContent(InputStream is, long length) {
        File tmp;
        FileOutputStream os = null;

        try {
            tmp = File.createTempFile("artifact", "hash"); // tmp has filename "artifact.hash"
            os = new FileOutputStream(tmp);

            @SuppressWarnings("MagicNumber")
            byte[] content = new byte[1024];
            long remaining = length;
            while (true) {
                int consumed = is.read(content, 0, content.length);
                if (consumed < 0) {
                    // consumed -1: no data was read into buffer content because end of stream is reached
                    if (length > 0) { // for length of (0 or -1), we are asked to consume all bytes of stream is
                        // full length was specified but not every byte was consumed
                        this.log.error("<internal> Core.addContent(): End of file while expanding content.");
                        return null;
                    }
                    // We are asked to consume entire stream (no length specified) and we finally encountered stream end.
                    // There is an odd case where the input stream contained 0 bytes, for that case: output stream os AND file temp are empty.
                    break;
                }

                if (consumed > 0) {
                    os.write(content, 0, consumed);
                    if (length > 0) {
                        // we were given actual length, so check byte consumption
                        remaining -= consumed;
                        if (remaining == 0)
                            break;
                        if (remaining < 0) {
                            this.log.error("<internal> Core.addContent(): incorrect bytes consumed while expanding content. Expected/actual consumed: " + length + "/" + length+remaining);
                            return null;
                        }
                    }
                } else {
                    // Exit for problem: We are here for consumed 0. This would be an infinite loop because "remaining" is not reduced.
                    this.log.error("<internal> Core.addContent(): Infinite loop exited while expanding content.");
                    return null;
                }
            } // end while()
        } catch (Exception e) {
            // Cannot even determine the hash, so we don't know if it has already been added or not.
            this.log.error("<internal> Core.addContent(): Could not add content, " + e.getMessage());
            return null;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
        }

        Hash h = Hash.fromContent(tmp);
        if (h == null) {
            this.log.error("<internal> Core.addContent() Could not compute hash of an artifact's content.");
            return null;
        }

        File target = new File(this.artifacts, h.toString());
        boolean ignore_insert_failure = false;

        if (target.exists()) {
            FileUtils.deleteQuietly(tmp);
            ignore_insert_failure = true;
        }

        // If read-only and it doesn't exist, then it cannot be added. If it was already cached, assume exists.
        if (read_only) {
            //TODO: Really need to check the DB and remove the file if it isn't present, otherwise the
            // cache and DB are out of sync.
            return ignore_insert_failure ? h : null;
        }

        PreparedStatement statement = null;
        try {
            // Move the file to the cache
            FileUtils.moveFile(tmp, target);

            statement = connect.prepareStatement("INSERT INTO content (pk_content, is_generated) VALUES (?,1)");
            statement.setBinaryStream(1, new ByteArrayInputStream(h.toBytes()));
            statement.executeUpdate();
        } catch (Exception e) {
            if (!ignore_insert_failure) {
                this.log.error("<internal> Core.addContent(): Could not add content, " + e.getMessage());
                FileUtils.deleteQuietly(tmp);
                return null;
            }
        } finally {
            safeClose(statement);
            statement = null;
        }

        return h;
    }

    /**
     * Return a file for content that exists in the cache.
     * @param h The hash of the file to return.
     * @return A file if it exists, null otherwise.
     *
     */
//  @Nullable
    @SuppressWarnings("ReturnOfNull")
    File getContentFile(Hash h)
    {
        File f = new File(this.artifacts, h.toString());
        if (f.exists())
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
     * @param merge_source True if the artifact is associated with a merged module.
     * @param derived_from_artifact If non-zero, the primary key of the artifact that this artifact is derived from (for example, an archive file).
     * @param merged_from_module If non-zero, the primary key of the module that this artifact is merged from.
     * @return The primary key of the added artifact, as stored in the artifact table
     */
    @SuppressWarnings("IfStatementWithNegatedCondition")
    long addArtifact(long pk_module, String configuration, String name, int mode, Hash content, boolean merge_source, long derived_from_artifact, long merged_from_module)
    {
        long pk = 0;
        if (read_only)
            return pk;

        PreparedStatement statement = null;
        try
        {
            if (merged_from_module != 0)
                statement = connect.prepareStatement("INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact, merged_from_module) VALUES (?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            else
                statement = connect.prepareStatement("INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

            statement.setLong(1, pk_module);
            statement.setBinaryStream(2, new ByteArrayInputStream(content.toBytes()));
            statement.setString(3, configuration);
            statement.setString(4, name);
            statement.setInt(5, mode);
            statement.setBoolean(6, merge_source);
            statement.setLong(7, derived_from_artifact);
            if (merged_from_module != 0)
                statement.setLong(8, merged_from_module);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    pk = keys.getLong(1);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.addArtifact(): Could not add artifact to module, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }

        return pk;
    }

    /**
     * Clear the is_generated flag on all content. If not set before pruneContent() is called, then the content
     * will be deleted by pruneContent() unless associated with an artifact.
     */
    void clearGeneratedContent()
    {
        if (read_only)
            return;

        // Mark test instances for later cleanup.
        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("UPDATE content SET is_generated=0");
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.clearGeneratedContent(): Could update generated flags, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    /**
     * Remove all non-generated content that is not referenced by any artifact.
     */
    void pruneContent()
    {
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("DELETE content FROM content LEFT JOIN artifact ON content.pk_content = artifact.fk_content WHERE artifact.fk_content IS NULL AND content.is_generated=0");
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.pruneContent(): Couldn't prune content, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    void pruneTemplates()
    {
        if (read_only)
            return;

        // Find all top-level templates.
        ResultSet foundTemplates = null;
        PreparedStatement deleteTemplate = null;

        // Determine the set of all referenced templates.
        try
        {
            // TODO: see that fk_template is not a column of table test_instance
            PreparedStatement findTemplates = connect.prepareStatement("select distinct fk_template from test_instance");
            foundTemplates = findTemplates.executeQuery();
            Set<Long> used = new HashSet<Long>();
            while (foundTemplates.next())
            {
                long pk = foundTemplates.getLong("fk_template");
                getRequiredTemplates(pk, used);
            }

            safeClose(findTemplates);
            findTemplates = null;
            safeClose(foundTemplates);
            foundTemplates = null;

            deleteTemplate = connect.prepareStatement("DELETE FROM template WHERE pk_template=?");
            findTemplates = connect.prepareStatement("select pk_template from template");
            foundTemplates = findTemplates.executeQuery();
            while (foundTemplates.next())
            {
                long pk = foundTemplates.getLong("pk_template");
                if (!used.contains(pk))
                {
                    // Delete the template. This will delete all the related tables.
                    deleteTemplate.setLong(1, pk);
                    deleteTemplate.executeUpdate();
                }
            }
        } catch (Exception ignore)
        {
            safeClose(foundTemplates);
            foundTemplates = null;
            safeClose(deleteTemplate);
            deleteTemplate = null;
        }
    }

    /**
     * This class represents a module that is backed by the core database. Operations on the module will refer
     * to database content.
     */
    private static class DBModule implements Module
    {
        private Core core;
        private long pk;
        private String organization;
        private String name;
        private Attributes attributes;
        private String version;
        private String status;
        private String sequence;

        DBModule(Core core, long pk, String organization, String name, String attribute_string, String version, String status, String sequence)
        {
            this.core = core;
            this.pk = pk;
            this.organization = organization;
            this.name = name;
            this.attributes = new Attributes(attribute_string);
            this.version = version;
            this.status = status;
            this.sequence = sequence;
        }

        @Override
        public String getOrganization()
        {
            return organization;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getVersion()
        {
            return version;
        }

        @Override
        public String getStatus()
        {
            return status;
        }

        @Override
        public Map<String, String> getAttributes()
        {
            return attributes.getAttributes();
        }

        @Override
        public String getSequence()
        {
            return sequence;
        }

        @Override
        public List<Artifact> getArtifacts()
        {
            return core.getArtifacts(pk, null, null);
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern)
        {
            return core.getArtifacts(pk, namePattern, null);
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern, String configuration)
        {
            return core.getArtifacts(pk, namePattern, configuration);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((organization == null) ? 0 : organization.hashCode());
            result = prime * result + (int) (pk ^ (pk >>> 32));
            result = prime * result + ((sequence == null) ? 0 : sequence.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DBModule other = (DBModule) obj;
            if (attributes == null) {
                if (other.attributes != null)
                    return false;
            } else if (!attributes.equals(other.attributes))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (organization == null) {
                if (other.organization != null)
                    return false;
            } else if (!organization.equals(other.organization))
                return false;
            if (pk != other.pk)
                return false;
            if (sequence == null) {
                if (other.sequence != null)
                    return false;
            } else if (!sequence.equals(other.sequence))
                return false;
            if (status == null) {
                if (other.status != null)
                    return false;
            } else if (!status.equals(other.status))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }
    }

    /**
     * This class represents an artifact that is represented in the database. It is the class
     * returned to generators.
     */
    private static class DBArtifact implements Artifact
    {
        private Core core;
        private long pk;
        private Module module;
        private String configuration;
        private String name;
        private int posixMode;
        private Hash hash;
        private String targetFilePath;

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
         * @param targetDirectory The target directory to which the artifact should be deployed.
         */
        DBArtifact(Core core, long pk, Module module, String configuration, String name, int mode, Hash hash, String targetDirectory)
        {
            this.core = core;
            this.pk = pk;
            this.module = module;
            this.configuration = configuration;
            this.name = name;
            this.posixMode = mode;
            this.hash = hash;
            this.targetFilePath = targetDirectory;
        }

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
        DBArtifact(Core core, long pk, Module module, String configuration, String name, int mode, Hash hash)
        {
            this(core, pk, module, configuration, name, mode, hash, null);
        }

        long getPK()
        {
            return pk;
        }

        @Override
        public Module getModule()
        {
            return module;
        }

        @Override
        public String getConfiguration()
        {
            return configuration;
        }

        @Override
        public String getName()
        {
            return name;
        }

//        @SuppressWarnings("unused")
//        public String getEncodedName()
//        {
//            try
//            {
//                return URLEncoder.encode(name, "UTF-8");
//            } catch (Exception ignore)
//            {
//                // This should never happen, as UTF-8 is a required charset.
//                return "error";
//            }
//        }

        @Override
        public Content getContent()
        {
            return new DBContent(core, hash);
        }

        @Override
        public int getPosixMode()
        {
            return posixMode;
        }

        @Override
        public String getTargetFilePath() {
            return this.targetFilePath;
        }

        @Override
        public void setTargetFilePath(String targetFilePath) {
            this.targetFilePath = targetFilePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
            result = prime * result + ((hash == null) ? 0 : hash.hashCode());
            result = prime * result + posixMode;
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + (int) (pk ^ (pk >>> 32));
            result = prime * result + ((targetFilePath == null) ? 0 : targetFilePath.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DBArtifact other = (DBArtifact) obj;
            if (configuration == null) {
                if (other.configuration != null)
                    return false;
            } else if (!configuration.equals(other.configuration))
                return false;
            if (hash == null) {
                if (other.hash != null)
                    return false;
            } else if (!hash.equals(other.hash))
                return false;
            if (posixMode != other.posixMode)
                return false;
            if (module == null) {
                if (other.module != null)
                    return false;
            } else if (!module.equals(other.module))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (pk != other.pk)
                return false;
            if (targetFilePath == null) {
                if (other.targetFilePath != null)
                    return false;
            } else if (!targetFilePath.equals(other.targetFilePath))
                return false;
            return true;
        }

        //@Override
        //public String getValue( Template template ) {
        //    return module.getOrganization() + "#" + module.getName() + " " + getEncodedName() + " " + hash.toString();
        //}
    }

    private static class DBContent implements Content
    {
        private Core core;
        private Hash hash;

        DBContent(Core core, Hash hash)
        {
            this.core = core;
            this.hash = hash;
        }

        @Override
        public Hash getHash()
        {
            return hash;
        }

        @Override
        public String getValue(Template template)
        {
            return getHash().toString();
        }

        @Override
//      @Nullable
        @SuppressWarnings("ReturnOfNull")
        public InputStream asStream()
        {
            File f = core.getContentFile(hash);
            if (f != null) {
                try {
                    return new FileInputStream(f);
                } catch (Exception ignore) {
                    // Ignore
                }
            }

            return null;
        }

        @Override
        @SuppressWarnings("ZeroLengthArrayAllocation")
        public byte[] asBytes()
        {
            File f = core.getContentFile(hash);
            if (f != null) {
                try {
                    return FileUtils.readFileToString(f).getBytes();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
            return new byte[0]; // this is better than returning null, which poses a null pointer threat to the caller
        }
    }

    /**
     * Return a set of all modules known to the database.
     * @return The set of modules
     */
    public Iterable<Module> createModuleSet()
    {
        Collection<Module> set = new ArrayList<Module>();
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT pk_module, organization, name, attributes, version, status, sequence FROM module");
            while (resultSet.next())
            {
                DBModule M = new DBModule(this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7));
                set.add(M);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.createModuleSet(): createModuleSet() exception " + e.getMessage());
            this.log.debug("stack trace: ", e);
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return set;
    }

    /**
     * Create a set of all modules that match the specified organization and module name.
     * @param organization The organizations to filter on.
     * @param name The module name to filter on.
     * @return A set of modules.
     */
    public Iterable<Module> createModuleSet(String organization, String name)
    {
        Collection<Module> set = new ArrayList<Module>();
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery(new String("SELECT pk_module, organization, name, attributes, version, status, sequence" + " FROM module" + " WHERE organization = " + organization + " AND name = '" + name + "'"));
            while (resultSet.next())
            {
                DBModule M = new DBModule(this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7));
                set.add(M);
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.createModuleSet(): createModuleSet() exception " + e.getMessage());
            this.log.debug("stack trace: ", e);
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return set;
    }

    /**
     * Return a set of all artifacts that the specified artifact depends on. Dependencies are stored in a
     * corresponding artifact with '.dep' added. These are typically merged artifacts so they are not typically
     * distributed.
     * The '.dep' file contains artifact references, one per line, with the following formats:
     * First, a line with a single field. This field is a name regex (defined my MySQL) for other artifacts in the same module.
     * Second, a line with three fields separated by a comma. The first field is a module reference and the second is a version.
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
    public Iterable<Artifact> findDependencies(Artifact artifact)
    {
        Collection<Artifact> set = new ArrayList<Artifact>();

        // Artifact searches are always done from the perspective of merged modules.
        long pk = findModule(artifact.getModule());
        if (pk == 0)
            return set; // This should not happen

        Statement statement = null;
        ResultSet resultSet = null;
        LineIterator iterator = null;
        try
        {
            // We are willing to find any artifact from any merged module
            statement = connect.createStatement();
            String query = String.format("SELECT artifact.fk_content" + " FROM artifact" + " WHERE artifact.fk_module = %d AND artifact.name = '%s.dep'", pk, artifact.getName());
            resultSet = statement.executeQuery(query);
            if (resultSet.next())
            {
                // We only care about the first match that we find.
                Hash hash = new Hash(resultSet.getBytes(1));
                File f = new File(this.artifacts, hash.toString());
                iterator = new LineIterator(new FileReader(f));

                safeClose(resultSet);
                resultSet = null;
                safeClose(statement);
                statement = null;

                // Each line is a dependency. The first field is a name regex, the second (optional) is a version.
                while (iterator.hasNext())
                {
                    String line = iterator.next();
                    String[] fields = line.split(",");
                    if (fields.length == 1 || fields.length == 2)
                    {
                        statement = connect.createStatement();
                        query = String.format("SELECT artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" + " WHERE artifact.fk_module = %d AND artifact.name REGEXP '%s" + "'", pk, fields[0]);
                        resultSet = statement.executeQuery(query);
                        while (resultSet.next())
                        {
                            String name = resultSet.getString(3);
                            String targetName = name;
                            if(fields.length == 2){
                                targetName = getTargetName(name, fields[1]);
                            }
                            Module mod = artifact.getModule();
                            Artifact A = new DBArtifact(this, resultSet.getLong(1), mod, resultSet.getString(2), name, resultSet.getInt(4), new Hash(resultSet.getBytes(5)), targetName);
                            set.add(A);
                        }
                        safeClose(resultSet);
                        resultSet = null;
                        safeClose(statement);
                        statement = null;

                    } else if (fields.length == 3 || fields.length == 4)
                    {
                        statement = connect.createStatement();

                        String[] mod_fields = fields[0].split("#");
                        String[] ver_fields = fields[1].split("/");

                        String organization = mod_fields[0];
                        String module = mod_fields.length > 1 ? mod_fields[1] : "";
                        String attributes = mod_fields.length > 2 ? mod_fields[2] : "";
                        String version = ver_fields[0];
                        String configuration = ver_fields.length > 1 ? ver_fields[1] : "";

                        // TODO: how to @SuppressWarnings() here, or just do what it wants
                        organization = organization.replace("$", artifact.getModule().getOrganization());
                        module = module.replace("$", artifact.getModule().getName());
                        attributes = attributes.replace("$", artifact.getModule().getAttributes().toString());
                        attributes = new Attributes(attributes).toString();
                        version = version.replace("$", artifact.getModule().getVersion());
                        configuration = configuration.replace("$", artifact.getConfiguration());

                        String organization_where = organization.length() > 0 ? " AND module.organization='" + organization + this.singleQuote : "";
                        String module_where = module.length() > 0 ? " AND module.name='" + module + this.singleQuote : "";
                        String attributes_where = attributes.length() > 0 ? " AND module.attributes='" + attributes + this.singleQuote : "";
                        String version_where = version.length() > 0 ? " AND module.version='" + version + this.singleQuote : "";
                        String configuration_where = configuration.length() > 0 ? " AND artifact.configuration='" + configuration + this.singleQuote : "";

                        query = String.format("SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.status, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" + " JOIN module ON module.pk_module = artifact.fk_module" + " WHERE artifact.merge_source=0 AND artifact.name REGEXP '%s'%s%s%s%s%s"
                                        + " ORDER BY module.organization, module.name, module.attributes, module.version, artifact.configuration, module.sequence DESC", fields[2], organization_where, module_where, attributes_where, version_where, configuration_where);
                        resultSet = statement.executeQuery(query);
                        Collection<String> found = new HashSet<String>();
                        while (resultSet.next())
                        {
                            String artifact_name = resultSet.getString(10);
                            if (found.contains(artifact_name))
                                continue;

                            String targetName = artifact_name;
                            if(fields.length == 4)
                                targetName = getTargetName(artifact_name, fields[3]);

                            @SuppressWarnings("MagicNumber")
                            DBModule dbmod = new DBModule(this, resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7));
                            @SuppressWarnings("MagicNumber")
                            Artifact A = new DBArtifact(this, resultSet.getLong(8), dbmod, resultSet.getString(9), artifact_name, resultSet.getInt(11), new Hash(resultSet.getBytes(12)), targetName);

                            set.add(A);
                            found.add(artifact_name);
                        }

                        safeClose(resultSet);
                        resultSet = null;
                        safeClose(statement);
                        statement = null;
                    } else
                        throw new Exception("ERROR: Illegal line (" + line + ") in " + artifact.getName() + ".dep");
                }
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findDependencies(): findDependencies() exception " + e.getMessage());
            this.log.debug("stack trace: ", e);
        } finally
        {
            if(iterator != null) iterator.close();
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return set;
    }

    /**
     * Remove default directory from artifact name and replace it with the target directory instead.
     *
     * @param artifactName The name of the artifact
     * @param targetDirectory The target directory.
     * @return The name of the artifact in the target directory.
     */
    @SuppressWarnings("MagicCharacter")
    private String getTargetName(String artifactName, String targetDirectory){
        if (artifactName.endsWith("/"))
            throw new IllegalArgumentException("Artifact name must not end with '/': " + artifactName);

        int nameStartIndex = 0;
        if(artifactName.contains("/"))
            nameStartIndex = artifactName.lastIndexOf('/') + 1;

        String targetName = artifactName.substring(nameStartIndex);
        if(!targetDirectory.endsWith("/"))
            targetDirectory += "/";

        return targetDirectory + targetName;
    }

    /**
     * Return a set of artifacts given the specified requirements. First, only attributes from modules with at least the specified
     * attributes are included. Second, if specified, the artifacts must come from the given configuration. The names specified are patterns
     * acceptable to MySQL regex search.
     * The result set includes a list of sets of matching artifacts. For each element in the list the array of results contains the
     * artifact that matches the parameter in the same position, all of which will come from the same module.
     * @param required A parameter set or null. Any module wed for artifacts must contain at least these attributes.
     * @param configuration the configuration to check, or null.
     * @param name Artifact names, including MySQL REGEXP patterns.
     * @return The set of artifacts
     */
    public Iterable<Artifact[]> createArtifactSet(Attributes required, String configuration, String... name)
    {
        Statement statement = null;
        ResultSet resultSet = null;
        Map<Long, DBModule> moduleMap = new HashMap<Long, DBModule>();
        Map<Long, Artifact[]> artifactMap = new HashMap<Long, Artifact[]>();

        for (int name_index = 0; name_index < name.length; name_index++)
        {
            String artifact_name = name[name_index];
            try
            {
                statement = connect.createStatement();
                String configuration_match = "";
                if (configuration != null)
                    configuration_match = " AND artifact.configuration='" + configuration + this.singleQuote;

                String queryStr = String.format("SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.status, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" + " JOIN module ON module.pk_module = artifact.fk_module" + " WHERE artifact.merge_source=0 AND artifact.name REGEXP '%s'%s"
                                + " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC", artifact_name, configuration_match);
                resultSet = statement.executeQuery(queryStr);
                while (resultSet.next())
                {
                    // Verify that if requested, the module/version has all required attributes.
                    Attributes possesses = new Attributes(resultSet.getString(4));
                    if (required != null)
                    {
                        boolean mismatch = false;
                        for (Map.Entry<String, String> entry : required.getAttributes().entrySet())
                        {
                            if (!possesses.get(entry.getKey())
                                          .equals(entry.getValue()))
                            {
                                mismatch = true;
                                break;
                            }
                        }

                        if (mismatch)
                            continue; // Move to the next result
                    }

                    long pk_found = resultSet.getLong(1);
                    Artifact[] locArtifacts;
                    if (artifactMap.containsKey(pk_found))
                        locArtifacts = artifactMap.get(pk_found);
                    else
                    {
                        locArtifacts = new Artifact[name.length];
                        artifactMap.put(pk_found, locArtifacts);
                    }

                    DBModule module = null;
                    if (moduleMap.containsKey(pk_found))
                        module = moduleMap.get(pk_found);
                    else
                    {
                        module = new DBModule(this, pk_found, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7));
                        moduleMap.put(pk_found, module);
                    }

                    if (locArtifacts[name_index] == null)
                    {
                        Artifact A = new DBArtifact(this, resultSet.getLong(8), module, resultSet.getString(9), resultSet.getString(10), resultSet.getInt(11), new Hash(resultSet.getBytes(12)));
                        locArtifacts[name_index] = A;
                    }
                }
            } catch (Exception e)
            {
                this.log.error("<internal> Core.createArtifactSet() exception msg: " + e.getMessage());
                this.log.debug("stack trace: ", e);
            } finally
            {
                safeClose(resultSet);
                resultSet = null;
                safeClose(statement);
                statement = null;
            }
        }

        Collection<Artifact[]> set = new ArrayList<Artifact[]>();
        for (Map.Entry<Long, Artifact[]> longEntry : artifactMap.entrySet())
        {
            Artifact[] list = longEntry.getValue();
            int found = 0;
            for (Artifact artifactElement : list)
                if (artifactElement != null)
                    found += 1;

            if (found == name.length)
                set.add(list);
        }

        return set;
    }

    /**
     * Get the list of generators configured for all the tests.
     * @return A map where the keys are the primary keys of the tests and the values are the string to run the generator.
     */
    public Map<Long, String> getGenerators()
    {
        Map<Long, String> result = new HashMap<Long, String>();
        Statement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT test.pk_test, test.script" + " FROM test" + " JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan" + " WHERE test.script != ''");
            while (resultSet.next())
            {
                result.put(resultSet.getLong(1), resultSet.getString(2));
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.getGenerators: getGenerators() exception " + e.getMessage());
            this.log.debug("stack trace: ", e);
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return result;
    }

    /**
     * Update the generator status for a test, setting the last execute time and output.
     * @param pk_test The primary key of the test.
     * @param stdout The standard output of the generator run.
     * @param stderr The standard error of the generator run.
     */
    void updateTest(long pk_test, String stdout, String stderr)
    {
        // Mark a module as found.

        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("UPDATE test SET last_run=?, last_stdout=?, last_stderr=? WHERE pk_test=?");
            statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            statement.setString(2, stdout);
            statement.setString(3, stderr);
            statement.setLong(4, pk_test);
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.updateTest(): Could not update test, " + e.getMessage());
        } finally
        {
            safeClose(statement);
            statement = null;
        }

    }

    /**
     * Return artifacts associated with a particular module (including version). Both name and configuration optional.
     * @param pk_module The primary key of the module to return artifacts for.
     * @param name The name, which can include MySQL REGEXP patterns and is also optional.
     * @param configuration The configuration, or null to include all.
     * @return The list of matching artifacts.
     */
    public List<Artifact> getArtifacts(long pk_module, String name, String configuration)
    {
        String name_match = "";

        if (name != null)
            name_match = "artifact.name REGEXP '" + name + this.singleQuote;

        String configuration_match = "";
        if (configuration != null)
            configuration_match = "artifact.configuration = '" + configuration + this.singleQuote;

        String separator = "";
        if (name != null && configuration != null)
            separator = " AND ";

        String intro = "";
        if (name != null || configuration != null)
            intro = " AND ";

        Statement statement = null;
        ResultSet resultSet = null;

        // Choose Set over List because Set automatically rejects duplicate entries
        HashSet<Artifact> set = new HashSet<Artifact>();
        try
        {
            statement = connect.createStatement();
            resultSet = statement.executeQuery("SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.status, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content, artifact.merged_from_module" + " FROM artifact" + " JOIN module ON module.pk_module = artifact.fk_module" + " WHERE module.pk_module = " + pk_module + intro + name_match + separator + configuration_match
                            + " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC");
            Map<Long, DBModule> modules = new HashMap<Long, DBModule>();
            while (resultSet.next())
            {
                // Ignore dtf_test_generator artifacts that are merged from other modules
                int merged_from_module = resultSet.getInt(13);
                if (merged_from_module > 0 && "dtf_test_generator".equals(configuration))
                    continue;

                DBModule module = null;
                long pk_found = resultSet.getLong(1);

                if (modules.containsKey(pk_found))
                    module = modules.get(pk_found);
                else
                {
                    module = new DBModule(this, pk_found, resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getString(5), resultSet.getString(6), resultSet.getString(7));
                    modules.put(pk_found, module);
                }

//                // set is now changed to HashSet<Artifact>, which cannot contain duplicate elements.
//                // When set was List<Artifact>, it was hard to detect matching entry: this next line of code could not return true: set is not List<String>
//                if (set.contains(resultSet.getString(8)))
//                    continue;

                Artifact A = new DBArtifact(this, resultSet.getLong(8), module, resultSet.getString(9), resultSet.getString(10), resultSet.getInt(11), new Hash(resultSet.getBytes(12)));
                set.add(A); // ignored return value is true for "added," false for already in place
            }

        } catch (Exception e)
        {
            this.log.error("<internal> Core.getArtifacts(): getArtifacts() exception " + e.getMessage());
            this.log.error("stack trace", e);
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return new ArrayList<Artifact>(set);
    }

    /**
     * Return whether the specified module is associated with the current core target test. This is true
     * if there is a relationship from the test through the test plan to the component and version.
     * @param module The module.
     * @return boolean
     */
    boolean isAssociatedWithTest(Module module)
    {
        long pk = findModule(module);
        if (pk == 0)
            return false;

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            statement = connect.prepareStatement("SELECT test.pk_test" + " FROM test" + " JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan" + " JOIN module_to_test_plan ON module_to_test_plan.fk_test_plan = test_plan.pk_test_plan" + " WHERE test.pk_test = ? AND module_to_test_plan.fk_module = ?");
            statement.setLong(1, pk_target_test);
            statement.setLong(2, pk);
            resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst())
            {
                return true;
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.isAssociatedWithTest: Failure determining test association, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return false;
    }

    private long findTestPlan(String name, String description)
    {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.prepareStatement("SELECT test_plan.pk_test_plan" + " FROM test_plan" +
                                                      " WHERE test_plan.name = '" + name + this.singleQuote + " AND test_plan.description = '" + description + this.singleQuote);
            resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst())
            {
                resultSet.next();
                return resultSet.getLong("test_plan.pk_test_plan");
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findTestPlan(): Couldn't find test plan, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return 0;
    }

    private long findTest(long pk_test_plan, String name, String description, String script)
    {
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try
        {
            statement = connect.prepareStatement("SELECT test.pk_test" + " FROM test" + " WHERE test.fk_test_plan = ?" + " AND test.name = ?" + " AND test.description = ? " + " AND test.script = ?");
            statement.setLong(1, pk_test_plan);
            statement.setString(2, name);
            statement.setString(3, description);
            statement.setString(4, script);

            resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst())
            {
                resultSet.next();
                return resultSet.getLong("test.pk_test");
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findTest(): Couldn't find test, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return 0;
    }

    long findModule(Module module)
    {
        // Short-cut the lookup if it is one of our modules.
        if (module instanceof DBModule)
        {
            @SuppressWarnings("CastToConcreteClass")
            DBModule dbmod = (DBModule) module;
            if (dbmod.pk != 0)
                return dbmod.pk;
        }

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        String attributes = new Attributes(module.getAttributes()).toString();
        try
        {
            statement = connect.prepareStatement("SELECT module.pk_module" + " FROM module" + " WHERE module.organization = '" + module.getOrganization() + "'" + " AND module.name = '" + module.getName() + "'" + " AND module.attributes = '" + attributes + "'" + " AND module.version = '" + module.getVersion() + "'" + " AND module.sequence = '" + module.getSequence() + "'");
            resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst())
            {
                resultSet.next();
                return resultSet.getLong("module.pk_module");
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findModule(): Couldn't find module, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return 0;
    }

    void updateModule(long pk_module)
    {
        // Mark a module as found.
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement(String.format("UPDATE module SET missing_count=0 WHERE pk_module=%d", pk_module));
            statement.executeUpdate();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.updateModule(): Couldn't update module, " + e.getMessage());

            //TODO: handle
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    private long findModuleWithoutPriorSequence(Module module)
    {
        // Short-cut the lookup if it is one of our modules (i.e. is already in the database).
        if (module instanceof DBModule)
        {
            @SuppressWarnings("CastToConcreteClass")
            DBModule dbmod = (DBModule) module;
            if (dbmod.pk != 0)
                return dbmod.pk;
        }

        // here for submitted module not in database (similar module entries may exist in db, even of same version)
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        String attributes = new Attributes(module.getAttributes()).toString();
        try
        {
            statement = connect.prepareStatement("SELECT module.pk_module" + " FROM module" + " WHERE module.organization = '" + module.getOrganization() + "'" + " AND module.name = '" + module.getName() + "'" + " AND module.attributes = '" + attributes + "'" + " AND module.version = '" + module.getVersion() + "'");
            resultSet = statement.executeQuery();
            if (resultSet.isBeforeFirst())
            {
                // resultSet.next() gives the actual first row of the overall resultSet
                // REVIEW: do we have assurance that the returned ordering is by submittal order?
                resultSet.next();
                return resultSet.getLong("module.pk_module");
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findModuleWithoutPriorSequence(): Couldn't find module, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
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
//    void addToSet(DescribedTemplate dt, Set<DescribedTemplate> set)
//    {
//        if (!set.contains(dt))
//        {
//            set.add(dt);
//            for (DescribedTemplate child : dt.getDependencies())
//                addToSet(child, set);
//        }
//    }

    private void addActions(DescribedTemplate dt, long pk) throws Exception
    {
        PreparedStatement statement = null;

        for (int i = 0; i < dt.getActionCount(); i++)
        {
            TestInstance.Action A = dt.getAction(i);

            statement = connect.prepareStatement("INSERT INTO dt_line (fk_described_template,line,fk_child_dt,description) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, pk);
            statement.setInt(2, i);

            DescribedTemplate child = A.getIncludedTemplate();
            if (child == null)
                statement.setNull(3, Types.NULL);
            else
                statement.setLong(3, child.getPK());

            statement.setString(4, A.getDescription());
            statement.executeUpdate();

            long linepk = 0;
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next())
                    linepk = keys.getLong(1);
            }

            safeClose(statement);
            statement = null;

            //TODO: This doesn't handle dependencies, which need to roll up.
            statement = connect.prepareStatement("INSERT INTO dt_to_dt (fk_parent,fk_child) VALUES (?,?)");
            statement.setLong(1, pk);
            statement.setLong(2, linepk);
            statement.executeUpdate();
            safeClose(statement);
            statement = null;

            ArtifactUses au = A.getArtifactUses();
            if (au != null)
            {
                Iterator<Artifact> iter = au.getArtifacts();
                while (iter.hasNext())
                {
                    @SuppressWarnings("CastToConcreteClass")
                    DBArtifact artifact = (DBArtifact) iter.next();

                    try
                    {
                        statement = connect.prepareStatement("INSERT INTO artifact_to_dt_line (fk_artifact, fk_dt_line, is_primary, reason) VALUES (?,?,?,?)");
                        statement.setLong(1, artifact.getPK());
                        statement.setLong(2, linepk);
                        statement.setInt(3, au.getPrimary() ? 1 : 0);
                        statement.setString(4, au.getReason());
                        statement.executeUpdate();
                        safeClose(statement);
                        statement = null;
                    } catch (Exception e)
                    {
                        this.log.error("<internal> Core.addActions() Failed to relate artifact to line, " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Add a described template - it is known to not exist.
     * @param dt The described template to add.
     * @param result The result to report, if any.
     * @param owner The owner to assign, if any.
     * @param start The start time, or null.
     * @param ready The ready time, or null.
     * @param complete The complete time, or null.
     * @return The key information for the added described template.
     */
    private DBDescribedTemplate add(DescribedTemplate dt, Boolean result, String owner, Date start, Date ready, Date complete) throws Exception
    {
        DescribedTemplate.Key proposed_key = dt.getKey();

        if (keyToDT.containsKey(proposed_key))
            return keyToDT.get(proposed_key);

        // Recursive check for all dependencies
        // TODO: Figure out if this logic is correct. Doesn't appear to be.
        for (DescribedTemplate child : dt.getDependencies())
        {
            if (!keyToDT.containsKey(child.getKey())) {
                DBDescribedTemplate dbdt = add(child, null, null, null, null, null);
            } else {
                DBDescribedTemplate dbdt = check(child);
            }
        }

        try
        {
            /* All described template additions are handled as transactions. */
            connect.setAutoCommit(false);

            long pk_template = syncTemplate(dt.getTemplate());
            if (result != null || owner != null)
                reportResult(dt.getTemplate().getHash().toString(), result, owner, start, ready, complete);

            PreparedStatement statement = null;
            long pk = 0;
            try
            {
                statement = connect.prepareStatement("INSERT INTO described_template (fk_module_set, fk_template, description_hash, synchronized) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statement.setBinaryStream(1, new ByteArrayInputStream(dt.getKey().getModuleHash().toBytes()));
                statement.setLong(2, pk_template);
                statement.setBinaryStream(3, new ByteArrayInputStream(dt.getDocumentationHash().toBytes()));
                statement.setInt(4, 1); // Default is synchronized.
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next())
                        pk = keys.getLong(1);
                }

                safeClose(statement);
                statement = null;

                addActions(dt, pk);
            } catch (SQLException ignore)
            {
                //TODO: Figure out that this is a duplicate key or not.
                safeClose(statement);
                statement = null;

                statement = connect.prepareStatement("SELECT pk_described_template FROM described_template WHERE fk_module_set=? AND fk_template=?");
                statement.setBinaryStream(1, new ByteArrayInputStream(dt.getKey().getModuleHash().toBytes()));
                statement.setLong(2, pk_template);
                ResultSet query = statement.executeQuery();

                if (query.next())
                    pk = query.getLong(1);

                safeClose(statement);
                statement = null;
                safeClose(query);
            } finally
            {
                safeClose(statement);
                statement = null;
            }

            connect.commit();
            connect.setAutoCommit(true);

            DBDescribedTemplate dbdt = new DBDescribedTemplate();
            dbdt.pk = pk;
            dbdt.key = dt.getKey();
//          pkToDT.put(dbdt.pk, dbdt);
            keyToDT.put(dbdt.key, dbdt);
            return dbdt;
        } catch (Exception e)
        {
            this.log.error("<internal> Core.add(): Failed to add described template: " + e);
            this.log.debug("stack trace: ", e);
            try
            {
                connect.rollback();
                connect.setAutoCommit(true);
            } catch (Exception e1)
            {
                // TODO: This is really bad - failure to restore state.
            }
        }

        return null;
    }

    /**
     * Check that an existing template is correct. If the template exists then the children
     * must exist, but their documentation may be out of date.
     * @param dt The described template to check. Results are not currently checked.
     * @return DBDescribedTemplate
     */
    private DBDescribedTemplate check(DescribedTemplate dt) throws Exception
    {
        // Recursive check for all dependencies
        for (DescribedTemplate child : dt.getDependencies())
        {
            // TODO: Figure out if this is correct.
            if (!keyToDT.containsKey(child.getKey()))
                throw new Exception("Parent template exists, child does not.");
            DBDescribedTemplate dbdt = check(child);
        }

        DBDescribedTemplate me = keyToDT.get(dt.getKey());
        if (me == null)
            throw new Exception("Request to check a non-existent template.");

        if (dt.getDocumentationHash().equals(me.description))
            return me;

        // Documentation needs to be recreated.
        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("DELETE FROM dt_line WHERE fk_described_template = ?");
            statement.setLong(1, me.pk);
            statement.executeUpdate();
        } finally
        {
            safeClose(statement);
            statement = null;
        }

        try
        {
            connect.setAutoCommit(false);

            try
            {
                statement = connect.prepareStatement("UPDATE described_template SET description_hash=? WHERE pk_described_template=?");
                statement.setBinaryStream(1, new ByteArrayInputStream(dt.getDocumentationHash().toBytes()));
                statement.setLong(2, me.pk);
                statement.executeUpdate();
            } finally
            {
                safeClose(statement);
                statement = null;
            }

            addActions(dt, me.pk);

            connect.commit();
            connect.setAutoCommit(true);
        } catch (Exception ignore)
        {
            connect.rollback();
            connect.setAutoCommit(true);
        }

        return me;
    }

    /**
     * Compare all described templates, deleting those that should not exist, adding
     * those that need to be created, and updating those that need to be updated.
     * Updates are limited to documentation changes.
     * @param testInstances A list of test instances to be synced.
     * @throws Exception on any error
     * @return count of added Described Templates.
     */
    public int syncDescribedTemplates(Iterable<TestInstance> testInstances) throws Exception
    {
        int addedDescribedTemplatesCount = 0;
        int checkedNotAddedDescribedTemplatesCount = 0;
        for (TestInstance ti : testInstances)
        {
            DBDescribedTemplate dbdt;
            DescribedTemplate.Key key = ti.getTemplate().getKey();

            if (!keyToDT.containsKey(key))
            {
                // add the template
                dbdt = add(ti.getTemplate(), ti.getResult(), ti.getOwner(), ti.getStart(), ti.getReady(), ti.getComplete());
                ++addedDescribedTemplatesCount;

            } else
            {
                // check the stored template
                dbdt = check(ti.getTemplate());
                ++checkedNotAddedDescribedTemplatesCount;
            }

            if (dbdt != null) {
                // We have the described template. There should be a Test Instance that relates the
                // current test (pk_test) to the current described template.
                if (!dtToTI.containsKey(dbdt.pk))
                {
                    // No test instance, add it.
                    PreparedStatement statement2 = null;
                    try
                    {
                        statement2 = connect.prepareStatement("INSERT INTO test_instance (fk_test, fk_described_template, phase, synchronized) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                        statement2.setLong(1, pk_target_test);
                        statement2.setLong(2, dbdt.pk);
                        //TODO: Determine the phase
                        statement2.setLong(3, 0);
                        statement2.setInt(4, 1); // Default is synchronized.
                        statement2.executeUpdate();

                        try (ResultSet keys = statement2.getGeneratedKeys()) {
                            if (keys.next())
                                ti.pk = keys.getLong(1);
                        }
                    } catch (Exception e)
                    {
                        this.log.error("<internal> Core.syncDescribedTemplates(): Could not add described_template to test_instance: " + e.getMessage());
                    }

                    safeClose(statement2);
                    statement2 = null;

                    // Insert all of the module references
                    List<TestInstance.Action> actions = ti.getActions();
                    for (TestInstance.Action action : actions)
                    {
                        ArtifactUses au = action.getArtifactUses();
                        if (au == null)
                            continue;

                        Iterator<Artifact> iter = au.getArtifacts();
                        while (iter.hasNext())
                        {
                            Artifact artifact = iter.next();

                            try
                            {
                                // TODO: When adding 529 test instances, is it ok that 1000 entries are added to this table?
                                //       This table has a 1000 entry limit. Is this a bug?
                                long pk_module = findModule(artifact.getModule());
                                statement2 = connect.prepareStatement("INSERT INTO module_to_test_instance ( fk_module, fk_test_instance ) VALUES (?,?)");
                                statement2.setLong(1, pk_module);
                                statement2.setLong(2, ti.pk);
                                statement2.execute();
                                safeClose(statement2);
                                statement2 = null;
                            } catch (Exception ignore)
                            {
                                // Ignore, since many times this will be a duplicate.
                            }
                        }
                    }

                    safeClose(statement2);
                    statement2 = null;
                    dbdt.pk = ti.pk;
                    dtToTI.put(dbdt.pk, ti.pk);
                }

                // If the ti has a result recorded, then make sure it is reflected in the run table.
                if (ti.getResult() != null || ti.getOwner() != null)
                {
                    Statement statement2 = null;
                    ResultSet resultSet = null;
                    Boolean dbResult = null;
                    String dbOwner = null;
                    try
                    {
                        statement2 = connect.createStatement();
                        resultSet = statement2.executeQuery("SELECT result, owner FROM run JOIN test_instance ON test_instance.fk_run = run.pk_run WHERE test_instance.pk_test_instance=" + Long.toString(ti.pk));

                        if (resultSet.next())
                        {
                            dbResult = resultSet.getBoolean("result");
                            if (resultSet.wasNull())
                                dbResult = null;

                            dbOwner = resultSet.getString("owner");
                            if (resultSet.wasNull())
                                dbOwner = null;
                        }
                    } catch (Exception ignore) {
                        // Ignore
                    } finally {
                        safeClose(resultSet);
                        resultSet = null;
                        safeClose(statement2);
                        statement2 = null;
                    }

                    // Check the run status, fix it if the status is known.
                    if (!Objects.equals(dbResult, ti.getResult()) ||
                        (dbOwner==null ? ti.getOwner()!=null : !Objects.equals(dbOwner, ti.getOwner())))
                    {
                        reportResult(ti.getTemplate().getTemplate().getHash().toString(), ti.getResult(), ti.getOwner(), ti.getStart(), ti.getReady(), ti.getComplete());
                    }
                }
            } else {
                this.log.debug("<internal> Core.syncDescribedTemplates() computes null DBDescribedTemplate");
                throw new Exception("null DBDescribedTemplate");
            } // end if (dbdt)
        } // end for()

        if (checkedNotAddedDescribedTemplatesCount > 0)
            this.log.debug("<internal> Core.syncDescribedTemplates() checked (without adding) " + checkedNotAddedDescribedTemplatesCount + " described templates in database");
        return addedDescribedTemplatesCount;
    }

    public long syncTemplate(Template sync)
    {
        long pk = 0;

        if (read_only)
        {
            this.log.error("<internal> Core.syncTemplate(): database is read-only");
            this.log.error("<internal> Core.syncTemplate(): ------------------------");
            this.log.error("<internal> Core.syncTemplate(): Template: " + sync.getHash().toString());
            this.log.error("<internal> Core.syncTemplate(): Script: " + sync.toStandardString());
            this.log.error("<internal> Core.syncTemplate(): ------------------------");
            return pk;
        }

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            statement = connect.prepareStatement("SELECT pk_template FROM template WHERE hash=?");
            statement.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));

            resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst())
            {
                safeClose(resultSet);
                resultSet = null;
                safeClose(statement);
                statement = null;

                statement = connect.prepareStatement("INSERT INTO template (hash, steps, enabled) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statement.setBinaryStream(1, new ByteArrayInputStream(sync.getHash().toBytes()));
                statement.setString(2, sync.toStandardString());
                statement.setInt(3, 1); // Default is enabled.
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next())
                        pk = keys.getLong(1);
                }

                safeClose(statement);
                statement = null;
            } else
            {
                resultSet.next();
                pk = resultSet.getLong("pk_template");
                safeClose(resultSet);
                resultSet = null;
                safeClose(statement);
                statement = null;
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.syncTemplate(): Couldn't synchronize template, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        return pk;
    }

    private List<Hash> getExistingTopArtifacts(long pk)
    {
        List<Hash> result = new ArrayList<Hash>();
        PreparedStatement findArtifacts = null;
        ResultSet foundArtifacts = null;

        // Read all the related artifacts
        try
        {
            findArtifacts = connect.prepareStatement(String.format("select fk_content from template_to_all_content where fk_template='%d'", pk));
            foundArtifacts = findArtifacts.executeQuery();
            while (foundArtifacts.next())
            {
                Hash hash = new Hash(foundArtifacts.getBytes(1));
                result.add(hash);
            }
        } catch (SQLException ignore)
        {
            // Ignore
        } finally
        {
            safeClose(foundArtifacts);
            foundArtifacts = null;
            safeClose(findArtifacts);
            findArtifacts = null;
        }

        return result;
    }

    private void getRequiredTopArtifacts(long pk, Set<Hash> combined)
    {
        PreparedStatement findArtifacts = null;
        ResultSet foundArtifacts = null;
        ResultSet foundChildren = null;

        // Read all the related artifacts
        try
        {
            findArtifacts = connect.prepareStatement(String.format("select fk_content from template_to_content where fk_template='%d'", pk));
            foundArtifacts = findArtifacts.executeQuery();
            while (foundArtifacts.next())
            {
                Hash hash = new Hash(foundArtifacts.getBytes(1));
                combined.add(hash);
            }

            PreparedStatement findChildren = connect.prepareStatement(String.format("select fk_child from template_to_template where fk_parent='%d'", pk));
            foundChildren = findChildren.executeQuery();
            while (foundChildren.next())
            {
                long fk = foundChildren.getLong("fk_child");
                getRequiredTopArtifacts(fk, combined);
            }
        } catch (Exception ignore)
        {
            // Ignore
        } finally
        {
            safeClose(foundArtifacts);
            foundArtifacts = null;
            safeClose(foundChildren);
            findArtifacts = null;
        }
    }

    private void getRequiredTemplates(long pk, Set<Long> combined)
    {
        // If a template is already added then its children must also already be added.
        if (combined.contains(pk))
            return;

        // Add me
        combined.add(pk);

        // Find and add all my children.
        PreparedStatement findChildren = null;
        ResultSet foundChildren = null;
        try
        {
            findChildren = connect.prepareStatement(String.format("select fk_child from template_to_template where fk_parent='%d'", pk));
            foundChildren = findChildren.executeQuery();
            while (foundChildren.next())
            {
                long fk = foundChildren.getLong("fk_child");
                getRequiredTemplates(fk, combined);
            }
        } catch (Exception ignore)
        {
            // Ignore.
        } finally
        {
            safeClose(foundChildren);
            foundChildren = null;
            safeClose(findChildren);
            findChildren = null;
        }
    }

    /**
     * Roll up the artifact relationships for all top-level templates. Top-level templates are those referenced
     * directly from a test instance. This roll-up allows SQL queries to map from an artifact to a test instance
     * for artifact result reports.
     */
    void syncTopTemplateRelationships()
    {
        // Find all top-level templates.
        if (read_only)
            return;

        /* Templates always have the same "contents" and "relationships" or their hash would change. This
         * means that the worst synchronization problem can be a crash while we were in the process of adding
         * relationships. An existing relationship will never be wrong.
         */
        ResultSet foundTemplates = null;
        PreparedStatement insertArtifact = null;
        try
        {
            PreparedStatement findTemplates = connect.prepareStatement("select distinct fk_template from test_instance");
            foundTemplates = findTemplates.executeQuery();
            while (foundTemplates.next())
            {
                long pk = foundTemplates.getLong("fk_template");
                List<Hash> existing = getExistingTopArtifacts(pk);
                Set<Hash> required = new HashSet<Hash>();
                getRequiredTopArtifacts(pk, required);

                // Worst case we missed adding some last time.
                required.removeAll(existing);
                for (Hash h : required)
                {
                    // Need to add the relationship.
                    insertArtifact = connect.prepareStatement("INSERT INTO template_to_all_content (fk_template, fk_content) VALUES (?,?)");
                    insertArtifact.setLong(1, pk);
                    insertArtifact.setBinaryStream(2, new ByteArrayInputStream(h.toBytes()));
                    insertArtifact.executeUpdate();
                }
            }
        } catch (Exception ignore)
        {
            // Ignore.
        } finally
        {
            safeClose(foundTemplates);
            foundTemplates = null;
            safeClose(insertArtifact);
            insertArtifact = null;
        }
    }

    public void syncTemplateRelationships(Template sync)
    {
        if (read_only)
            return;

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            for (Template t : sync.allTemplates)
            {
                statement = connect.prepareStatement(String.format("select fk_parent, fk_child from template_to_template where fk_parent='%d' and fk_child='%d'", sync.getPK(), t.getPK()));
                resultSet = statement.executeQuery();
                if (!resultSet.isBeforeFirst())
                {
                    // There were no matches. Time to insert. Need to determine if the content exists.
                    safeClose(resultSet);
                    resultSet = null;
                    safeClose(statement);
                    statement = null;

                    statement = connect.prepareStatement("INSERT INTO template_to_template (fk_parent, fk_child) VALUES (?,?)");
                    statement.setLong(1, sync.getPK());
                    statement.setLong(2, t.getPK());
                    statement.executeUpdate();

                    safeClose(statement);
                    statement = null;
                } else
                {
                    safeClose(resultSet);
                    resultSet = null;
                }
            }

            for (Content a : sync.artifacts)
            {
                statement = connect.prepareStatement("select fk_template, fk_content from template_to_content where fk_template=? and fk_content=?");
                statement.setLong(1, sync.getPK());
                statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
                resultSet = statement.executeQuery();
                if (!resultSet.isBeforeFirst())
                {
                    // There were no matches. Time to insert. Need to determine if the content exists.
                    safeClose(resultSet);
                    resultSet = null;
                    safeClose(statement);
                    statement = null;

                    statement = connect.prepareStatement("INSERT INTO template_to_content (fk_template, fk_content) VALUES (?,?)");
                    statement.setLong(1, sync.getPK());
                    statement.setBinaryStream(2, new ByteArrayInputStream(a.getHash().toBytes()));
                    statement.executeUpdate();

                    safeClose(statement);
                    statement = null;
                } else
                {
                    safeClose(resultSet);
                    resultSet = null;
                }
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.syncTemplateRelationships(): Couldn't synchronize template relationships, " + e.getMessage());
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }
    }

    public void startSyncTestInstance(long pk_test)
    {
        // Mark test instances for later cleanup.
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement(String.format("UPDATE test_instance SET synchronized=0 WHERE fk_test=%d", pk_test));
            statement.executeUpdate();
        } catch (Exception ignore)
        {
            //TODO: handle
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    public void stopSyncTestInstance(long pk_test)
    {
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement(String.format("DELETE FROM test_instance WHERE synchronized=0 AND fk_test=%d", pk_test));
            statement.executeUpdate();
        } catch (Exception e)
        {
            //TODO: handle
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

    /**
     * Retrieve test instances held for the given test.
     * @param pk_test pk_test value in table test.
     * @return The list.
     * @throws Exception on fail
     */
    List<Long> getTestInstances(long pk_test) throws Exception{
        Statement find_test_instance = null;
        List<Long> testInstanceList = new ArrayList<Long>();

        try {
            find_test_instance = connect.createStatement();
            try (ResultSet test_instances = find_test_instance.executeQuery("SELECT pk_test_instance FROM test_instance WHERE fk_test = " + pk_test)) {
                while (test_instances.next())
                    testInstanceList.add(test_instances.getLong("pk_test_instance"));
            }
        } catch(Exception e) {
            this.log.error("<internal> Core.getTestInstances(pk_test) exception msg: " + e);
            throw e;
        } finally {
            safeClose(find_test_instance);
        }
        return testInstanceList;
    }

    /**
     * Retrieve test instances held for the given test, that also use the given module id.
     * @param pk_test pk_test value in table test.
     * @param idModule pk_module of potentially matching entry in table module.
     * @return The list.
     * @throws Exception on fail
     */
    List<Long> getTestInstances(long pk_test, long idModule) throws Exception{
        Statement find_test_instance = null;
        List<Long> testInstanceList = new ArrayList<Long>();

        try {
            find_test_instance = connect.createStatement();
            try (ResultSet test_instances = find_test_instance.executeQuery("SELECT pk_test_instance FROM test_instance" +
                                                                            " INNER JOIN module_to_test_instance" +
                                                                            "  ON pk_test_instance = fk_test_instance " +
                                                                            "WHERE fk_test = " + pk_test +
                                                                            "  AND fk_module = " + idModule)) {
                while (test_instances.next())
                    testInstanceList.add(test_instances.getLong("pk_test_instance"));
            }
        } catch(Exception e) {
            this.log.error("<internal> Core.getTestInstances(pk_test, idModule) exception msg: " + e);
            throw e;
        } finally {
            safeClose(find_test_instance);
        }
        return testInstanceList;
    }

    private long findTestInstance(TestInstance sync, long pk_test)
    {
        PreparedStatement find_test_instance = null;
        ResultSet test_instances = null;
        PreparedStatement find_versions = null;
        ResultSet his_versions = null;

        try
        {
            find_test_instance = connect.prepareStatement("SELECT pk_test_instance FROM test_instance WHERE fk_template=? AND fk_test=?");
            find_test_instance.setLong(1, sync.getTemplate().getPK());
            find_test_instance.setLong(2, pk_test);
            test_instances = find_test_instance.executeQuery();
            while (test_instances.next())
            {
                long pk = test_instances.getLong("pk_test_instance");

                // We found a candidate, but need to verify that its version references exactly match.
                find_versions = connect.prepareStatement("SELECT fk_version FROM test_instance_to_version WHERE fk_test_instance=?");
                find_versions.setLong(1, pk);
                his_versions = find_versions.executeQuery();
                boolean extras = false;

                Collection<Long> my_versions = new ArrayList<Long>();
                //                for ( Version v : sync.getVersions() )
                //                    my_versions.add( v.getPK() );
                while (his_versions.next() && !extras)
                {
                    Long vk = his_versions.getLong("fk_version");
                    if (!my_versions.contains(vk))
                        extras = true;

                    my_versions.remove(vk); // Note, this is remove by object, not index.
                }

                safeClose(his_versions);
                his_versions = null;
                safeClose(find_versions);
                find_versions = null;

                if (extras)
                    continue;

                if (my_versions.size() == 0)
                    return pk; // All versions matched.

                // No match, keep searching.
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.findTestInstance() exception msg: " + e);

            // TODO: handle
        } finally
        {
            safeClose(test_instances);
            test_instances = null;
            safeClose(find_test_instance);
            find_test_instance = null;
            safeClose(his_versions);
            his_versions = null;
            safeClose(find_versions);
            find_versions = null;
        }

        return 0;
    }

    /**
     * Synchronize the specified test instance belonging to the specified test. The test instance
     * information itself is verified and the hashes are checked against the loaded information. If
     * these match then no further work is done.
     * @param sync The test instance to synchronize.
     * @param pk_test The test that the instance is related to.
     * @return The test instance number
     */
    public long syncTestInstance(TestInstance sync, long pk_test)
    {
        long pk = 0;

        //TODO: Clean up
        if (read_only)
        {
            this.log.error("<internal> Core.syncTestInstance(): database is read-only");
            this.log.error("<internal> Core.syncTestInstance(): ------------------------");
            //            System.err.println( "Template: " + sync.getTemplate().getHash().toString() );
            this.log.error("<internal> Core.syncTestInstance(): Test Instance for Test \" + Long.toString(pk_test)");
            this.log.error("<internal> Core.syncTestInstance(): Versions:");
                //            for ( Version v : sync.getVersions() )
                //                System.err.println( "\t" + v.getComponent() + ", " + v.getVersion() );
            this.log.error("<internal> Core.syncTestInstance(): ------------------------");
            return pk;
        }

        //        sync.getTemplate().sync();
        //        sync.getDescription().sync();
        //        for ( Version v : sync.getVersions() )
        //            v.sync();

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try
        {
            pk = findTestInstance(sync, pk_test);

            if (pk == 0)
            {
                // There were no matches. Time to insert. Need to determine if the content exists.

                // Get the component list associated with the test
                statement = connect.prepareStatement(String.format("SELECT distinct pk_component" + " FROM component" + " JOIN component_to_test_plan ON component_to_test_plan.fk_component = component.pk_component" + " JOIN test_plan ON test_plan.pk_test_plan = component_to_test_plan.fk_test_plan" + " JOIN test ON test.fk_test_plan = test_plan.pk_test_plan" + " WHERE test.pk_test='%d'", pk_test));
                resultSet = statement.executeQuery();

                // TODO: See why components here is never read. Is impl incomplete?
                List<Long> components = new ArrayList<Long>();
                while (resultSet.next())
                {
                    components.add(resultSet.getLong(1));
                }

                safeClose(resultSet);
                resultSet = null;
                safeClose(statement);
                statement = null;

                statement = connect.prepareStatement("INSERT INTO test_instance (fk_test, fk_template, fk_description, phase, synchronized) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, pk_test);
                //                statement.setLong(2, sync.getTemplate().getPK());
                //                statement.setLong(3, sync.getDescription().getPK());
                //TODO: Determine the phase
                statement.setLong(4, 0);
                statement.setInt(5, 1); // Default is synchronized.
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next())
                        pk = keys.getLong(1);
                }

                safeClose(statement);
                statement = null;

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
            } else
            {
                // TODO: Validate the due date and phase
                statement = connect.prepareStatement("UPDATE test_instance SET synchronized=1, fk_description=? WHERE pk_test_instance=?");
                //                statement.setLong( 1, sync.getDescription().getPK() );
                statement.setLong(2, pk);
                statement.executeUpdate();
            }
        } catch (Exception e)
        {
            this.log.error("<internal> Core.syncTestInstance() exception msg : " + e);
            // TODO: handle
        } finally
        {
            safeClose(resultSet);
            resultSet = null;
            safeClose(statement);
            statement = null;
        }

        //        if ( sync.getResult() != null )
        //            reportResult( sync.getTemplate().getHash().toString(), sync.getResult() );

        return pk;
    }

//  @Nullable
    @SuppressWarnings("ReturnOfNull")
    public Long getInstanceRun(long testInstanceNumber) throws Exception
    {
        Statement statement = null;
        try
        {
            statement = connect.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT fk_run FROM test_instance WHERE pk_test_instance = " + testInstanceNumber);
            if(resultSet.next()){

                long result = resultSet.getLong("fk_run");
                if(!resultSet.wasNull())
                    return result;
            }
            return null;
        } catch(Exception e)
        {
            this.log.error("<internal> Core.getInstanceRun() exception msg: " + e);
            throw e;
        } finally
        {
            safeClose(statement);
        }
    }

    /**
     *
     * @param runID The runID
     * @param result The result
     * @throws Exception on failure
     */
    void addResultToRun(long runID, boolean result) throws Exception {
        if (read_only)
            return;

        Statement statement = null;
        try
        {
            statement = connect.createStatement();
            int rowsUpdated = statement.executeUpdate("Update run SET result = " + result + ", end_time = NOW() WHERE pk_run = " + runID);
            if(rowsUpdated == 0){
                throw new Exception("Failed to update run result. Run with id " + runID + " not found.");
            }
        } catch(Exception e)
        {
            this.log.error("<internal> Core.addResultToRun() exception msg: " + e);
            throw e;
        } finally
        {
            safeClose(statement);
        }
    }

//  @Nullable
    @SuppressWarnings("ReturnOfNull")
    Long createInstanceRun(long testInstanceNumber, String owner) throws Exception
    {
        if (read_only)
            throw new Exception("Database connection is read only.");

        String hash;
        ResultSet resultSet = null;
        Statement templateStatement = null;
        try
        {
            templateStatement = connect.createStatement();
            resultSet = templateStatement.executeQuery("SELECT hash FROM described_template JOIN test_instance ON fk_described_template = pk_described_template JOIN template ON fk_template = pk_template WHERE pk_test_instance = " + testInstanceNumber);
            if(resultSet.next()){
                hash = new Hash(resultSet.getBytes("hash")).toString();
            }
            else{
                this.log.error("<internal> Core.createInstanceRun(): Cannot find template for test instance " + testInstanceNumber);
                throw new Exception("Cannot find template for test instance " + testInstanceNumber);
            }
        } catch(Exception e)
        {
            this.log.error("<internal> Core.createInstanceRun() exception msg: " + e);
            throw e;
        } finally
        {
            safeClose(templateStatement);
        }

        PreparedStatement runStatement = null;
        try
        {
            runStatement = connect.prepareStatement("call add_run(?, ?, ?, ?, ?, ?)");
            runStatement.setString(1, hash);
            runStatement.setNull(2, Types.BOOLEAN);

            if (owner != null)
                runStatement.setString(3, owner);
            else
                runStatement.setNull(3, Types.VARCHAR);

            runStatement.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
            runStatement.setNull(5, Types.TIMESTAMP);
            runStatement.setNull(6, Types.TIMESTAMP);
            if(runStatement.execute()){
                // we have a ResultSet
                resultSet = runStatement.getResultSet();
                boolean result = resultSet.next();
                if(result){
                    return resultSet.getLong("pk_run");
                }
            }
            else
                return null; // no ResultSet
        } catch (Exception e)
        {
            this.log.error("<internal> Core.createInstanceRun() exception msg: " + e);
            // TODO: handle
            throw e;
        } finally
        {
            safeClose(runStatement);
            runStatement = null;
        }
        throw new Exception("Failed to add new run for test instance " + testInstanceNumber);
    }

    void reportResult(String hash, Boolean result, String owner, Date start, Date ready, Date complete)
    {
        if (read_only)
            return;

        PreparedStatement statement = null;
        try
        {
            statement = connect.prepareStatement("call add_run(?, ?, ?, ?, ?, ?)");
            statement.setString(1, hash);

            if (result != null)
                statement.setBoolean(2, result);
            else
                statement.setNull(2, Types.BOOLEAN);

            if (owner != null)
                statement.setString(3, owner);
            else
                statement.setNull(3, Types.VARCHAR);

            if (start != null)
                statement.setTimestamp(4, new java.sql.Timestamp(start.getTime()));
            else
                statement.setNull(4, Types.TIMESTAMP);

            if (ready != null)
                statement.setTimestamp(5, new java.sql.Timestamp(ready.getTime()));
            else
                statement.setNull(5, Types.TIMESTAMP);

            if (complete != null)
                statement.setTimestamp(6, new java.sql.Timestamp(complete.getTime()));
            else
                statement.setNull(6, Types.TIMESTAMP);
            statement.execute();
        } catch (Exception e)
        {
            this.log.error("<internal> Core.reportResult() exception msg: " + e);
            // TODO: handle
        } finally
        {
            safeClose(statement);
            statement = null;
        }
    }

//    public static class TestTopLevelRelationships
//    {
//        public static void main(String[] args)
//        {
//            Core core = new Core(0);
//            core.syncTopTemplateRelationships();
//        }
//    }
//
//    public static class TestPruneTemplates
//    {
//        public static void main(String[] args)
//        {
//            Core core = new Core(0);
//            core.pruneTemplates();
//        }
//    }
}
