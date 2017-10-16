package com.pslcl.dtf.core.storage.mysql;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.PortalConfig;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.storage.DTFStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class MySQLDtfStorage implements DTFStorage {
    private final Logger log;
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

    public boolean isReadOnly() {
        return this.read_only;
    }

    private void openDatabase(){
        //TODO Create connection pool.

        try
        {
            // Setup the connection with the DB
            this.read_only = false;
            String user = this.config.dbUser();
            String password = this.config.dbPassword();
            if (user == null || password == null) {
                user = "guest";
                password = "";
                this.read_only = true;
            }

            String connectstring = String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s", config.dbHost(), config.dbPort(), config.dbSchema(), user, password);
            // TODO: replace superseded javax.sql.DriverManager with javax.sql.DataSource
            this.connect = DriverManager.getConnection(connectstring);

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
            if (this.connect != null) {
                this.connect.close();
            }
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
    public boolean describedTemplateHasTestInstanceMatch(long pkDescribedTemplate) throws Exception {
        return false;
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
