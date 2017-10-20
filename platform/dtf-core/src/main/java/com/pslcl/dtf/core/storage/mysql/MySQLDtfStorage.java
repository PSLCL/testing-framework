package com.pslcl.dtf.core.storage.mysql;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.PortalConfig;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.storage.DTFStorage;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;

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
            String query = "INSERT INTO test_plan (name, description) VALUES (?,?)";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
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
     * @return primary key of found test plan or zero for none
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
            String query = "INSERT INTO test (fk_test_plan, name, description, script) VALUES (?,?,?,?)";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setLong(1, pk_test_plan);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, description);
                preparedStatement.setString(4, script);
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
    public long addModule(Module module) throws SQLException {
        long pk;
        try {
            pk = findModule(module);
        } catch (SQLException sqle) {
            this.log.error("<internal> MySQLDtfStorage.addModule(): Continues even though couldn't lookup existing module, msg: " + sqle);
            return 0;
        }

        // let read-only mode return an existing module
        if (pk==0 && !this.read_only) {
            // add our new module
            String attributes = new Attributes(module.getAttributes()).toString();
            String query = "INSERT INTO module (organization, name, attributes, version, status, sequence) VALUES (?,?,?,?,?,?)";
            // TODO: Release date, actual release date, order all need to be added.
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, module.getOrganization());
                preparedStatement.setString(2, module.getName());
                preparedStatement.setString(3, attributes);
                preparedStatement.setString(4, module.getVersion());
                preparedStatement.setString(5, module.getStatus());
                preparedStatement.setString(6, module.getSequence());
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
     * A Module can be a DBModule, i.e. it is stored in the database, in table module.
     * @param module The module to determine presence in the module table.
     * @return private key pk_module of module's entry in table module, where 0 means no such entry
     */
    private long isDBModule(Module module) {
        long retPk_module = 0;
        if (module instanceof Core.DBModule) {
            Core.DBModule dbmod = (Core.DBModule) module;
            if (dbmod.pk == 0) {
                int x = 30; // does this happen?
            }
            retPk_module = dbmod.pk; // 0 means not really in table module
        }
        return retPk_module;
    }

    @Override
    public long findModule(Module module) throws SQLException {
        // Short-cut the lookup if it is one of our modules (i.e. is already in the database).
        long pk = this.isDBModule(module);
        if (pk != 0)
             return pk;

        String attributes = new Attributes(module.getAttributes()).toString();
        String query = "SELECT module.pk_module" + " FROM module" +
                       " WHERE module.organization = '" + module.getOrganization() + "'" +
                       "   AND module.name = '" + module.getName() + "'" +
                       "   AND module.attributes = '" + attributes + "'" +
                       "   AND module.version = '" + module.getVersion() + "'" +
                       "   AND module.sequence = '" + module.getSequence() + "'";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.isBeforeFirst()) { // true for resultSet has row(s); false for no rows exist
                resultSet.next();
                return resultSet.getLong("module.pk_module");
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean artifactFileHashStoredInDB(Hash h) throws SQLException {
        String query = "SELECT pk_content FROM content WHERE pk_content = ?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setBytes(1, h.toBytes());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();

//          // this alternate exit is a double check
//          while (resultSet.next()) {
//              byte [] bytes = resultSet.getBytes("pk_content");
//              String strDbContent = Hash.bytesToHex(bytes);
//              String strParamContent = h.toString();
//              if (strDbContent.equals(strParamContent))
//                  return true;
//              return true;
//          }
//          return false;

            }
        }
    }

    @Override
    public long findModuleWithoutPriorSequence(Module module) throws SQLException  {
        // Short-cut the lookup if it is one of our modules (i.e. is already in the database).
        long pk = isDBModule(module);
        if (pk != 0)
            return pk;

        // here for submitted module not in database (similar module entries may exist in db, even of same version)
        String attributes = new Attributes(module.getAttributes()).toString();
        String query = "SELECT module.pk_module" + " FROM module" +
                       " WHERE module.organization = '" + module.getOrganization() + "'" +
                       "   AND module.name = '" + module.getName() + "'" +
                       "   AND module.attributes = '" + attributes + "'" +
                       "   AND module.version = '" + module.getVersion() + "'";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.isBeforeFirst()) {
                // resultSet.next() gives the actual first row of the overall resultSet
                // REVIEW: do we have assurance that the returned ordering is by submittal order?
                resultSet.next();
                return resultSet.getLong("module.pk_module");
            }
            return 0;
        }
    }

    @Override
    public void deleteModule(long pk_module) throws SQLException {
        if (!this.read_only) {
            String query = "DELETE FROM module WHERE pk_module=?";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setLong(1, pk_module);
                preparedStatement.executeUpdate();

                String query1 = "DELETE FROM artifact WHERE merged_from_module=?";
                try (PreparedStatement preparedStatement1 = this.connect.prepareStatement(query1)) {
                    preparedStatement1.setLong(1, pk_module);
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    @Override
    public void addContent(Hash content) throws SQLException {
        String query = "INSERT INTO content (pk_content, is_generated) VALUES (?,1)";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setBinaryStream(1, new ByteArrayInputStream(content.toBytes()));
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public long addArtifact(long pk_module, String configuration, String name, int mode, Hash content, boolean merge_source, long derived_from_artifact, long merged_from_module) throws SQLException {
        if (this.read_only)
            return 0;

        long pk = 0;
        String query = merged_from_module != 0 ? "INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact, merged_from_module) VALUES (?,?,?,?,?,?,?,?)" :
                                                 "INSERT INTO artifact (fk_module, fk_content, configuration, name, mode, merge_source, derived_from_artifact) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, pk_module);
            preparedStatement.setBinaryStream(2, new ByteArrayInputStream(content.toBytes()));
            preparedStatement.setString(3, configuration);
            preparedStatement.setString(4, name);
            preparedStatement.setInt(5, mode);
            preparedStatement.setBoolean(6, merge_source);
            preparedStatement.setLong(7, derived_from_artifact);
            if (merged_from_module != 0)
                preparedStatement.setLong(8, merged_from_module);
            preparedStatement.executeUpdate();
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next())
                    pk = keys.getLong(1);
            }
        }
        return pk;
    }

    @Override
    public void clearGeneratedContent() throws SQLException {
        if (!this.read_only) {
            // Mark test instances for later cleanup.
            String query = "UPDATE content SET is_generated=0";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public void pruneContent() throws SQLException {
        if (!this.read_only) {
            String query = "DELETE content FROM content" +
                           " LEFT JOIN artifact ON content.pk_content = artifact.fk_content" +
                           " WHERE artifact.fk_content IS NULL" +
                           "   AND content.is_generated=0";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.executeUpdate();
            }
        }
    }

    /**
     *
     * @param pk_described_template primary key of a row of table described_template
     * @param combined accumulating set of primary keys in table described_template
     * @throws SQLException on error.
     */
    private void getRequiredTemplates(long pk_described_template, Set<Long> combined) throws SQLException {
        // If a template is already added then its children must also already be added.
        if (!combined.contains(pk_described_template)) {
            // Add me
            combined.add(pk_described_template);

            // Find and add all my children.
            String queryFindChildren = String.format("SELECT fk_child FROM dt_to_dt" +
                                                     " WHERE fk_parent='%d'", pk_described_template);
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(queryFindChildren);
                 ResultSet rsFoundChildren = preparedStatement.executeQuery()) {
                while (rsFoundChildren.next()) {
                    long fkChild = rsFoundChildren.getLong("fk_child");
                    this.getRequiredTemplates(fkChild, combined);
                }
            }
        }
    }

    @Override
    public void pruneTemplates() throws SQLException {
        if (!this.read_only
            && this.read_only) { // TODO: remove this line- it prevents execution of the following code that needs work
            // TODO: This has been broken since described_template was added as intermediary to table template; this code has had mismatched references ever since

            // Find all top-level described_template rows referenced in table test_instance.
            String queryFindDescribedTemplates = "SELECT DISTINCT fk_described_template FROM test_instance";
            try (PreparedStatement psFindDescribedTemplates = this.connect.prepareStatement(queryFindDescribedTemplates);
                 ResultSet rsFoundDescribedTemplates = psFindDescribedTemplates.executeQuery()) {
                Set<Long> usedDescribedTemplates = new HashSet<Long>();
                // Add all child described_template rows.
                while (rsFoundDescribedTemplates.next()) {
                    long pk_described_template = rsFoundDescribedTemplates.getLong("fk_described_template");
                    this.getRequiredTemplates(pk_described_template, usedDescribedTemplates);
                }

                // TODO: the above filled usedDescribedTemplates with pk of described templates;
                //       the below uses it again, but mixes in pk of templates.
                //       Resolve: The two are not the same. A template may be referenced by multiple described_template's.
                //                Is it true that we have no interest in pruning described_templates?
                //                How best to prune unused templates only?

                String queryFindTemplates = "SELECT pk_template FROM template";
                try (PreparedStatement psFindTemplates = this.connect.prepareStatement(queryFindTemplates);
                     ResultSet rsFoundTemplates1 = psFindTemplates.executeQuery()) {
                    while (rsFoundTemplates1.next()) {
                        long pk = rsFoundTemplates1.getLong("pk_template");
                        if (!usedDescribedTemplates.contains(pk)) {
                            String queryDeleteTemplate = "DELETE FROM template WHERE pk_template=?";
                            try (PreparedStatement preparedStatement2 = this.connect.prepareStatement(queryDeleteTemplate)) {
                                // Delete the template. This will delete all the related tables.
                                preparedStatement2.setLong(1, pk);
                                preparedStatement2.executeUpdate();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Iterable<Module> createModuleSet(Core core) throws SQLException {
        Collection<Module> set = new ArrayList<Module>();
        String query = "SELECT pk_module, organization, name, attributes, version, status, sequence FROM module";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Core.DBModule M = new Core.DBModule(core, resultSet.getLong(1),
                                                          resultSet.getString(2),
                                                          resultSet.getString(3),
                                                          resultSet.getString(4),
                                                          resultSet.getString(5),
                                                          resultSet.getString(6),
                                                          resultSet.getString(7));
                set.add(M);
            }
        }
        return set;
    }

    @Override
    public Iterable<Module> createModuleSet(Core core, String organization, String name) throws SQLException {
        Collection<Module> set = new ArrayList<Module>();
        String query = "SELECT pk_module, organization, name, attributes, version, status, sequence" + " FROM module" +
                       " WHERE organization = " + organization +
                       "   AND name = '" + name + "'";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Core.DBModule M = new Core.DBModule(core, resultSet.getLong(1),
                                                          resultSet.getString(2),
                                                          resultSet.getString(3),
                                                          resultSet.getString(4),
                                                          resultSet.getString(5),
                                                          resultSet.getString(6),
                                                          resultSet.getString(7));
                set.add(M);
            }
        }
        return set;
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
                /* we expect exactly one entry in resultSet; to check, these lines give count of 1
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
