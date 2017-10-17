package com.pslcl.dtf.core.storage.mysql;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.PortalConfig;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.storage.DTFStorage;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLDtfStorage implements DTFStorage {
    private final Logger log;
    private static final String singleQuote = "'";
    private final PortalConfig config;
    private boolean read_only;

    /**
     * The connection to the database.
     */
    private Connection connect;

    /**
     * Constructor opens database, based on config.
     *
     * @param config Configuration of database access.
     */
    public MySQLDtfStorage(PortalConfig config){
        this.log = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.read_only = false;
        this.connect = null;
        this.openDatabase();
    }

    private void openDatabase(){
        try {
            // Setup the connection with the DB
            this.read_only = false;
            String user = this.config.dbUser();
            String password = this.config.dbPassword();
            if (user == null || password == null) {
                user = "guest";
                password = "";
                this.read_only = true;
            }

            // connection pool of size 1 (for now)
            String connectString = String.format("jdbc:mysql://%s:%d/%s", config.dbHost(), config.dbPort(), config.dbSchema());

            BasicDataSource dataSource = new BasicDataSource(); // BasicDataSource implements DataSource interface
            dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            dataSource.setUrl(connectString);
            dataSource.setMaxActive(1); // 1 for now
            dataSource.setMaxIdle(1);
            dataSource.setInitialSize(1);
            dataSource.setValidationQuery("SELECT 1");
            // The query that will be used to validate connections from this pool before returning them to the caller.
            // If specified, this query <strong>MUST</strong> be an SQL SELECT statement that returns at least one row.

            this.connect = dataSource.getConnection();
        } catch (Exception e) {
            this.log.error("<internal> MySQLDtfStorage.openDatabase() could not open database connection, " + e.getMessage());
            read_only = true;
        }
    }

    /**
     * Close the database connection if it is open.
     */
    private void closeDatabase() {
        try {
            if (this.connect != null)
                this.connect.close();
        } catch (Exception e) {
            this.log.error("<internal> MySQLDtfStorage.closeDatabase() could not close database connection, " + e.getMessage());
        } finally {
            this.connect = null;
        }
    }

    @Override
    public void close() {
        this.closeDatabase();
    }

    @Override
    public Connection getConnect() {
        return this.connect;
    }

    @Override
    public boolean isReadOnly() {
        return this.read_only;
    }

    @Override
    public List<String> getArtifactProviders() throws SQLException {
        String query = "SELECT * FROM artifact_provider";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            List<String> result = new ArrayList<String>();
            while (resultSet.next()) {
                String name = resultSet.getString("classname");
                result.add(name);
            }
            return result;
        }
    }

    @Override
    public void updateModulesMissingCount() throws SQLException {
        if (!this.read_only) {
            // Update missing count.
            String query = "UPDATE module SET missing_count=missing_count+1";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public void pruneModules(int deleteThreshold) throws SQLException {
        if (!this.read_only) {
            // Remove modules that have been missing for too long.
            String query = "DELETE FROM module WHERE missing_count > ?";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setLong(1, deleteThreshold);
                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public long addTestPlan(String name, String description) throws SQLException {
        long pk;
        try {
            pk = this.findTestPlan(name, description);
        } catch (SQLException sqle) {
            this.log.error("<internal> MySQLDtfStorage.addTestPlan(): Continues even though couldn't lookup existing test plan, msg: " + sqle);
            return 0;
        }

        // let read-only mode return an existing module
        if (pk==0 && !this.read_only) {
            // add our new test plan
            String query = "INSERT INTO test_plan (name, description) VALUES (?,?)" + Statement.RETURN_GENERATED_KEYS;
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, description);
                preparedStatement.executeUpdate();
                try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                    if (keys.next())
                        pk = keys.getLong(1);
                }
            }
        }
        return pk;
    }

    /**
     *
     * @param name The name of the test plan.
     * @param description The description of the test plan.
     * @return primary key of found test plan
     * @throws SQLException on error
     */
    private long findTestPlan(String name, String description) throws SQLException {
        String query = "SELECT test_plan.pk_test_plan" + " FROM test_plan" +
                       " WHERE test_plan.name = '" + name + singleQuote +
                       "   AND test_plan.description = '" + description + singleQuote;
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.isBeforeFirst()) { // true for resultSet has row(s); false for no rows exist
                resultSet.next();
                return resultSet.getLong("test_plan.pk_test_plan");
            } else {
                return 0;
            }
        }
    }

    @Override
    public long addTest(long pk_test_plan, String name, String description, String script) throws SQLException {
        long pk;
        try {
            pk = this.findTest(pk_test_plan, name, description, script);
        } catch (SQLException sqle) {
            this.log.error("<internal> MySQLDtfStorage.addTest(): Continues even though couldn't lookup existing test, msg: " + sqle);
            return 0;
        }

        // let read-only mode return an existing test
        if (pk==0 && !this.read_only) {
            // add our new test
            String query = "INSERT INTO test (fk_test_plan, name, description, script) VALUES (?,?,?,?)" + Statement.RETURN_GENERATED_KEYS;
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setLong(1, pk_test_plan);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, description);
                preparedStatement.setString(4, script);
                try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                    if (keys.next())
                        pk = keys.getLong(1);
                }
            }
        }
        return pk;
    }

    /**
     *
     * @param pk_test_plan The primary key of the test plan.
     * @param name The name of the test.
     * @param description The description of the test.
     * @param script The script of the test.
     * @return The primary key of the new test, or zero if there is an error or in read-only mode.
     * If the test already exists then the existing primary key is returned.
     */
    private long findTest(long pk_test_plan, String name, String description, String script) throws SQLException {
        String query = "SELECT test.pk_test" + " FROM test" +
                       " WHERE test.fk_test_plan = ?" +
                       "   AND test.name = ?" +
                       "   AND test.description = ? " +
                       "   AND test.script = ?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setLong(1, pk_test_plan);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, description);
            preparedStatement.setString(4, script);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.isBeforeFirst()) {
                    resultSet.next();
                    return resultSet.getLong("test.pk_test");
                } else {
                    return 0;
                }
            }
        }
    }

    @Override
    public boolean describedTemplateHasTestInstanceMatch(long pkDescribedTemplate) throws SQLException {
        String query = "SELECT pk_test_instance FROM test_instance" +
                       " JOIN described_template ON fk_described_template=pk_described_template" +
                       " WHERE pk_described_template=?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setLong(1, pkDescribedTemplate);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    @Override
    public Optional<Core.DBDescribedTemplate> getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws SQLException {
        String query = "SELECT pk_described_template, fk_module_set, description_hash, hash" +
                       " FROM described_template JOIN template ON fk_template = pk_template" +
                       // to this point, we have every described_template/template pair
                       " WHERE hash=? AND fk_module_set=?"; // qualify for the unique pair
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setBytes(1, matchKey.getTemplateHash().toBytes());
            preparedStatement.setBytes(2, matchKey.getModuleHash().toBytes());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                /* we expect exactly one entry in resultSet; to check, these line give count of 1
                    resultSet.last();
                    int count = resultSet.getRow();
                    resultSet.beforeFirst(); // restore resultSet to original state
                */
                if (resultSet.next()) {
                    DescribedTemplate.Key key = new DescribedTemplate.Key(new Hash(resultSet.getBytes("hash")),
                            new Hash(resultSet.getBytes("fk_module_set")));
                    return Optional.of(new Core.DBDescribedTemplate(resultSet.getLong("pk_described_template"), new Hash(resultSet.getBytes("description_hash"))));
                }
                return Optional.empty();
            }
        }
    }
}
