package com.pslcl.dtf.core.storage.mysql;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.PortalConfig;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.TestInstance;
import com.pslcl.dtf.core.storage.DTFStorage;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
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
import java.util.Optional;
import java.util.Set;

public class MySQLDtfStorage implements DTFStorage {
    private static final char forwardSlash = '/';
    private static final String singleQuote = "'";

    private final Logger log;
    private final Core core;
    private final PortalConfig config;
    private boolean read_only;

    /**
     * The connection to the database.
     */
    private Connection connect;

    /**
     * Constructor opens database, based on Core.config.
     *
     * @param core Core
     */
    public MySQLDtfStorage(Core core, PortalConfig portalConfig){
        this.log = LoggerFactory.getLogger(getClass());
        this.core = core;
        this.config = portalConfig;
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
            pk = this.findModule(module);
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
    public Iterable<Module> createModuleSet() throws SQLException {
        Collection<Module> set = new ArrayList<Module>();
        String query = "SELECT pk_module, organization, name, attributes, version, status, sequence FROM module";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Core.DBModule M = new Core.DBModule(this.core, resultSet.getLong(1),
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
    public Iterable<Module> createModuleSet(String organization, String name) throws SQLException {
        Collection<Module> set = new ArrayList<Module>();
        String query = "SELECT pk_module, organization, name, attributes, version, status, sequence" + " FROM module" +
                       " WHERE organization = " + organization +
                       "   AND name = '" + name + "'";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Core.DBModule M = new Core.DBModule(this.core, resultSet.getLong(1),
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

    /**
     * Remove default directory from artifact name and replace it with the target directory instead.
     *
     * @param artifactName The name of the artifact
     * @param targetDirectory The target directory.
     * @return The name of the artifact in the target directory.
     */
    private String getTargetName(String artifactName, String targetDirectory){
        if (artifactName.endsWith("/"))
            throw new IllegalArgumentException("Artifact name must not end with '/': " + artifactName);

        int nameStartIndex = 0;
        if(artifactName.contains("/"))
            nameStartIndex = artifactName.lastIndexOf(forwardSlash) + 1; // '/'
        String targetName = artifactName.substring(nameStartIndex);
        if(!targetDirectory.endsWith("/"))
            targetDirectory += "/";

        return targetDirectory + targetName;
    }

    @Override
    public Iterable<Artifact> findDependencies(Artifact artifact) throws SQLException, IllegalArgumentException {
        // Artifact searches are always done from the perspective of merged modules.
        long pk_module = 0;
        try {
            pk_module = this.findModule(artifact.getModule());
        } catch (Exception sqle) {
            this.log.warn("<internal> .findDependencies(): Continues even though couldn't find associated module, msg: " + sqle);
        }

        Collection<Artifact> set = new ArrayList<Artifact>();
        if (pk_module == 0)
            return set; // For no exception caught above, this should not happen. An artifact that not associated with any module can be pruned.

        String queryContentMatch = String.format("SELECT artifact.fk_content" + " FROM artifact" +
                                                 " WHERE artifact.fk_module = %d" +
                                                 "   AND artifact.name = '%s.dep'", pk_module, artifact.getName());
        try (PreparedStatement psContentMatch = this.connect.prepareStatement(queryContentMatch);
             ResultSet rsContentMatch = psContentMatch.executeQuery()) {
            if (rsContentMatch.next()) {
                // Look only at first resultSet (we only care about the first match that we find).
                Hash hash = new Hash(rsContentMatch.getBytes(1));
                File f = new File(this.core.getArtifactsDirectory(), hash.toString()); // f: actual file from file system
                try {
                    LineIterator iterator = new LineIterator(new FileReader(f));

                    // Each line is a dependency. The first field is a name regex, the second (optional) is a version.
                    while (iterator.hasNext()) {
                        String line = iterator.next();
                        String[] fields = line.split(",");
                        if (fields.length==1 || fields.length==2) {
                            String queryArtifactInfo = String.format("SELECT artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" +
                                                                     " WHERE artifact.fk_module = %d" +
                                                                     "   AND artifact.name REGEXP '%s" + "'", pk_module, fields[0]);
                            try (PreparedStatement psArtifactInfo = this.connect.prepareStatement(queryArtifactInfo);
                                 ResultSet rsArtifactInfo = psArtifactInfo.executeQuery()) {
                                while (rsArtifactInfo.next()) {
                                    String name = rsArtifactInfo.getString(3);
                                    String targetName = name;
                                    if (fields.length == 2) {
                                        targetName = this.getTargetName(name, fields[1]);
                                    }
                                    Module mod = artifact.getModule();
                                    Artifact A = new Core.DBArtifact(this.core, rsArtifactInfo.getLong(1), mod,
                                                                                rsArtifactInfo.getString(2), name,
                                                                                rsArtifactInfo.getInt(4),
                                                                     new Hash(rsArtifactInfo.getBytes(5)), targetName);
                                    set.add(A);
                                }
                            }
                        } else if (fields.length == 3 || fields.length == 4) {
                            String[] mod_fields = fields[0].split("#");
                            String[] ver_fields = fields[1].split("/");

                            String organization = mod_fields[0];
                            String module = mod_fields.length > 1 ? mod_fields[1] : "";
                            String attributes = mod_fields.length > 2 ? mod_fields[2] : "";
                            String version = ver_fields[0];
                            String configuration = ver_fields.length > 1 ? ver_fields[1] : "";

                            // avoid IntelliJ warning "Dynamic regular expression could be replaced by compiled Pattern"
                            CharSequence dollarSign = "$"; // this is the precomputation of "$" that IntelliJ asks for

                            organization = organization.replace(dollarSign, artifact.getModule().getOrganization());
                            module = module.replace(dollarSign, artifact.getModule().getName());
                            attributes = attributes.replace(dollarSign, artifact.getModule().getAttributes().toString());
                            attributes = new Attributes(attributes).toString();
                            version = version.replace(dollarSign, artifact.getModule().getVersion());
                            configuration = configuration.replace(dollarSign, artifact.getConfiguration());

                            String organization_where = organization.length() > 0 ? " AND module.organization='" + organization + singleQuote : "";
                            String module_where = module.length() > 0 ? " AND module.name='" + module + singleQuote : "";
                            String attributes_where = attributes.length() > 0 ? " AND module.attributes='" + attributes + singleQuote : "";
                            String version_where = version.length() > 0 ? " AND module.version='" + version + singleQuote : "";
                            String configuration_where = configuration.length() > 0 ? " AND artifact.configuration='" + configuration + singleQuote : "";

                            String queryModuleInfo = String.format("SELECT module.pk_module, module.organization, module.name, module.attributes, module.version, module.status, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" +
                                                                   " JOIN module ON module.pk_module = artifact.fk_module" +
                                                                   " WHERE artifact.merge_source=0" +
                                                                   "   AND artifact.name REGEXP '%s'%s%s%s%s%s" +
                                                                   " ORDER BY module.organization, module.name, module.attributes, module.version, artifact.configuration, module.sequence DESC",
                                                                   fields[2], organization_where, module_where, attributes_where, version_where, configuration_where);
                            try (PreparedStatement psModuleInfo = this.connect.prepareStatement(queryModuleInfo);
                                 ResultSet rsModuleInfo = psModuleInfo.executeQuery()) {
                                Collection<String> found = new HashSet<String>();
                                while (rsModuleInfo.next()) {
                                    String artifact_name = rsModuleInfo.getString(10);
                                    if (found.contains(artifact_name))
                                        continue;

                                    String targetName = artifact_name;
                                    if(fields.length == 4)
                                        targetName = getTargetName(artifact_name, fields[3]);

                                    @SuppressWarnings("MagicNumber")
                                    Core.DBModule dbmod = new Core.DBModule(this.core, rsModuleInfo.getLong(1),
                                                                                       rsModuleInfo.getString(2),
                                                                                       rsModuleInfo.getString(3),
                                                                                       rsModuleInfo.getString(4),
                                                                                       rsModuleInfo.getString(5),
                                                                                       rsModuleInfo.getString(6),
                                                                                       rsModuleInfo.getString(7));
                                    @SuppressWarnings("MagicNumber")
                                    Artifact A = new Core.DBArtifact(this.core,     rsModuleInfo.getLong(8),
                                                                     dbmod,         rsModuleInfo.getString(9),
                                                                     artifact_name, rsModuleInfo.getInt(11),
                                                                     new Hash(rsModuleInfo.getBytes(12)), targetName);
                                    set.add(A);
                                    found.add(artifact_name);
                                }
                            }
                        } else {
                            throw new IllegalArgumentException("ERROR: Illegal line (" + line + ") in " + artifact.getName() + ".dep");
                        }
                    }
                } catch (FileNotFoundException fnfe) {
                    this.log.debug("DTFStorage.findDependencies(artifact) exits; does not find specified artifact file, msg: " + fnfe);
                }
            }
        }
        return set;
    }

    @Override
    public Iterable<Artifact[]> createArtifactSet(Attributes required, String configuration, String... name) throws SQLException {
        Map<Long, Artifact[]> artifactMap = new HashMap<Long, Artifact[]>();

        Map<Long, Core.DBModule> moduleMap = new HashMap<Long, Core.DBModule>(); // must endure for() and inner while() loops
        for (int name_index = 0; name_index < name.length; name_index++) {
            try {
                String artifact_name = name[name_index];
                String configuration_match = "";
                if (configuration != null)
                    configuration_match = " AND artifact.configuration='" + configuration + singleQuote;
                String query = String.format(
                  "SELECT module.pk_module, module.organization, module.name, module.attributes, module.version," +
                        " module.status, module.sequence, artifact.pk_artifact, artifact.configuration," +
                        " artifact.name, artifact.mode, artifact.fk_content" + " FROM artifact" +
                  " JOIN module ON module.pk_module = artifact.fk_module" +
                  " WHERE artifact.merge_source=0" +
                  "   AND artifact.name REGEXP '%s'%s" + // note: .name ending with $ means REGEXP matches to end of %s
                  " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC",
                  artifact_name, configuration_match);

                try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        // Verify that if requested, the module/version has all required attributes.
                        Attributes possesses = new Attributes(resultSet.getString(4));
                        if (required != null) {
                            boolean mismatch = false;
                            for (Map.Entry<String, String> entry : required.getAttributes().entrySet()) {
                                if (!possesses.get(entry.getKey())
                                        .equals(entry.getValue())) {
                                    mismatch = true;
                                    break;
                                }
                            }
                            if (mismatch)
                                continue; // Move to the next result
                        }

                        long pk_found = resultSet.getLong(1);
                        Artifact[] locArtifacts;
                        if (artifactMap.containsKey(pk_found)) {
                            locArtifacts = artifactMap.get(pk_found);
                        } else {
                            locArtifacts = new Artifact[name.length];
                            artifactMap.put(pk_found, locArtifacts);
                        }

                        Core.DBModule module = null;
                        if (moduleMap.containsKey(pk_found)) {
                            module = moduleMap.get(pk_found);
                        } else {
                            module = new Core.DBModule(this.core, pk_found, resultSet.getString(2),
                                                                            resultSet.getString(3),
                                                                            resultSet.getString(4),
                                                                            resultSet.getString(5),
                                                                            resultSet.getString(6),
                                                                            resultSet.getString(7));
                            moduleMap.put(pk_found, module);
                        }

                        if (locArtifacts[name_index] == null) {
                            @SuppressWarnings("MagicNumber")
                            Artifact A = new Core.DBArtifact(this.core, resultSet.getLong(8),
                                                             module,    resultSet.getString(9),
                                                                        resultSet.getString(10),
                                                                        resultSet.getInt(11),
                                                             new Hash(resultSet.getBytes(12)));
                            locArtifacts[name_index] = A;
                        }
                    }
                }
            } catch (Exception e) {
                this.log.error("<internal> DtfStorage.createArtifactSet() Continues even though seeing exception msg: " + e.getMessage());
                this.log.debug("stack trace: ", e);
            }
        }

        Collection<Artifact[]> set = new ArrayList<Artifact[]>();
        for (Map.Entry<Long, Artifact[]> longEntry : artifactMap.entrySet()) {
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

    @Override
    public Map<Long, String> getGenerators() throws SQLException {
        Map<Long, String> result = new HashMap<Long, String>();
        String query = "SELECT test.pk_test, test.script" + " FROM test" +
                " JOIN test_plan ON test_plan.pk_test_plan=test.fk_test_plan" +
                " WHERE test.script!=''";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                result.put(resultSet.getLong(1),
                           resultSet.getString(2));
            }
        }
        return result;
    }

    @Override
    public void updateTest(long pk_test, String stdout, String stderr) throws SQLException {
        if (!this.read_only) {
            // Mark a module as found.
            String query = "UPDATE test" +
                           " SET last_run=?, last_stdout=?, last_stderr=?" +
                           " WHERE pk_test=?";
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                preparedStatement.setString(2, stdout);
                preparedStatement.setString(3, stderr);
                preparedStatement.setLong(4, pk_test);
                preparedStatement.executeUpdate();
            }
        }
    }

    @Override
    public List<Artifact> getArtifacts(long pk_module, String name, String configuration) throws SQLException {
        String name_match = "";
        if (name != null)
            name_match = "artifact.name REGEXP '" + name + singleQuote;
        String configuration_match = "";
        if (configuration != null)
            configuration_match = "artifact.configuration = '" + configuration + singleQuote;
        String separator = "";
        if (name != null && configuration != null)
            separator = " AND ";
        String intro = "";
        if (name != null || configuration != null)
            intro = " AND ";

        // Choose Set over List because Set automatically rejects duplicate entries
        HashSet<Artifact> set = new HashSet<Artifact>();
        String query = "SELECT module.pk_module, module.organization, module.name, module.attributes, module.version," +
                "  module.status, module.sequence, artifact.pk_artifact, artifact.configuration, artifact.name," +
                "  artifact.mode, artifact.fk_content, artifact.merged_from_module FROM artifact" +
                " JOIN module ON module.pk_module=artifact.fk_module" +
                " WHERE module.pk_module=" + (pk_module + intro+name_match + separator + configuration_match) +
                " ORDER BY module.organization, module.name, module.attributes, module.version, module.sequence DESC";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            Map<Long, Core.DBModule> modules = new HashMap<Long, Core.DBModule>();
            while (resultSet.next()) {
                // Ignore dtf_test_generator artifacts that are merged from other modules
                int merged_from_module = resultSet.getInt(13);
                if (merged_from_module>0 && "dtf_test_generator".equals(configuration))
                    continue;

                Core.DBModule module = null;
                long pk_found = resultSet.getLong(1);
                if (modules.containsKey(pk_found)) {
                    module = modules.get(pk_found);
                } else {
                    module = new Core.DBModule(this.core, pk_found, resultSet.getString(2),
                                                                    resultSet.getString(3),
                                                                    resultSet.getString(4),
                                                                    resultSet.getString(5),
                                                                    resultSet.getString(6),
                                                                    resultSet.getString(7));
                    modules.put(pk_found, module);
                }

//              // set is now changed to HashSet<Artifact>, which cannot contain duplicate elements.
//              // When set was List<Artifact>, it was hard to detect matching entry: this next line of code could not return true: set is not List<String>
//              if (set.contains(resultSet.getString(8)))
//                  continue;

                @SuppressWarnings("MagicNumber")
                Artifact A = new Core.DBArtifact(this.core, resultSet.getLong(8),
                                                 module,    resultSet.getString(9),
                                                            resultSet.getString(10),
                                                            resultSet.getInt(11),
                                                 new Hash(resultSet.getBytes(12)));
                set.add(A); // ignored return value is true for "added," false for already in place
            }
        }
        return new ArrayList<Artifact>(set);
    }

    @Override
    public boolean isAssociatedWithTest(Module module) throws SQLException {
        long pk = 0;
        try {
            pk = this.findModule(module);
        } catch (Exception sqle) {
            this.log.error("<internal> DTFStorage.isAssociatedWithTest(): Continues even though .findModule() throws exception, msg: " + sqle);
        }
        if (pk == 0)
            return false;

        String query = "SELECT test.pk_test" + " FROM test" +
                " JOIN test_plan ON test_plan.pk_test_plan=test.fk_test_plan" +
                " JOIN module_to_test_plan ON module_to_test_plan.fk_test_plan=test_plan.pk_test_plan" +
                " WHERE test.pk_test=? AND module_to_test_plan.fk_module=?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setLong(1, this.core.pk_target_test);
            preparedStatement.setLong(2, pk);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.isBeforeFirst())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void updateModule(long pk_module) throws SQLException {
        if (!this.read_only) {
            // Mark a module as found.
            String query = String.format("UPDATE module SET missing_count=0" + " WHERE pk_module=%d", pk_module);
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.executeUpdate();
            }
        }
    }

    /**
     *
     * @param dt the DescribedTemplate java object
     * @param pk primary key
     * @throws SQLException on error
     */
    private void addActions(DescribedTemplate dt, long pk) throws Exception {
        for (int i=0; i<dt.getActionCount(); i++) {
            TestInstance.Action A = dt.getAction(i);
            String insertDtLineQuery = "INSERT INTO dt_line (fk_described_template,line,fk_child_dt,description) VALUES (?,?,?,?)";
            try (PreparedStatement psInsertDtLine = this.connect.prepareStatement(insertDtLineQuery)) {
                psInsertDtLine.setLong(1, pk);
                psInsertDtLine.setInt(2, i);

                DescribedTemplate child = A.getIncludedTemplate(); // throws Exception
                if (child == null)
                    psInsertDtLine.setNull(3, Types.NULL);
                else
                    psInsertDtLine.setLong(3, child.getPK());

                psInsertDtLine.setString(4,
                                         A.getDescription()); // throws Exception
                psInsertDtLine.executeUpdate();

                long linepk = 0;
                try (ResultSet keys = psInsertDtLine.getGeneratedKeys()) {
                    if (keys.next())
                        linepk = keys.getLong(1);
                }

                // TODO: This doesn't handle dependencies, which need to roll up.
                String insertDtToDtQuery = "INSERT INTO dt_to_dt (fk_parent,fk_child) VALUES (?,?)";
                try (PreparedStatement psInsertDtToDt = this.connect.prepareStatement(insertDtToDtQuery)) {
                    psInsertDtToDt.setLong(1, pk);
                    psInsertDtToDt.setLong(2, linepk);
                    psInsertDtToDt.executeUpdate();
                }

                TestInstance.Action.ArtifactUses au = A.getArtifactUses(); // throws Exception
                if (au != null) {
                    Iterator<Artifact> iter = au.getArtifacts();
                    while (iter.hasNext()) {
                        Core.DBArtifact artifact = (Core.DBArtifact)iter.next();
                        String insertArtifactToDtLineQuery = "INSERT INTO artifact_to_dt_line" +
                                                             "  (fk_artifact, fk_dt_line, is_primary, reason)" +
                                                             " VALUES (?,?,?,?)";
                        try (PreparedStatement psInsertArtifactToDtLine = this.connect.prepareStatement(insertArtifactToDtLineQuery)) {
                            psInsertArtifactToDtLine.setLong(1, artifact.getPK());
                            psInsertArtifactToDtLine.setLong(2, linepk);
                            psInsertArtifactToDtLine.setInt(3, au.getPrimary()?1:0);
                            psInsertArtifactToDtLine.setString(4, au.getReason());
                            psInsertArtifactToDtLine.executeUpdate();
                        } catch (SQLException sqle) {
                            this.log.error("<internal> DTFStorage.addActions() Failed to relate artifact to line, " + sqle);
                            throw sqle;
                        }
                    }
                }
            } // end try(psInsertDtLine)
        } // end for()
    }

    @Override
    public Core.DBDescribedTemplate check(DescribedTemplate dt) throws Exception {
        // Recursively check all dependent DescribedTemplate's. But .getDependencies() is empty.
        for (DescribedTemplate child : dt.getDependencies()) {
            // Original TODO: Figure out if this is correct. Has not been tested, since .getDependencies() is empty.
            Optional<Core.DBDescribedTemplate> dbdtAsStored;
            DescribedTemplate.Key matchKey = child.getKey();
            try {
                dbdtAsStored = this.core.getStorage().getDBDescribedTemplate(matchKey);
            } catch (SQLException sqle) {
                this.log.error("<internal> DTFStorage.check() sees exception from .getDBDescribedTemplate(), msg: " + sqle);
                this.log.debug("stack trace: ", sqle);
                throw new Exception(".check() exits with exception ", sqle);
            }

            if (!dbdtAsStored.isPresent())
                throw new Exception("Parent template exists, child does not.");
            /*DBDescribedTemplate dbdt =*/ this.check(child); // recursion
        }

        Optional<Core.DBDescribedTemplate> wrappedMe;
        DescribedTemplate.Key matchKey = dt.getKey();
        try {
            wrappedMe = this.core.getStorage().getDBDescribedTemplate(matchKey);
        } catch (SQLException sqle) {
            this.log.error("<internal> DTFStorage.check() sees exception from .getDBDescribedTemplate(), msg: " + sqle);
            this.log.debug("stack trace: ", sqle);
            throw new Exception(".check() exits with exception ", sqle);
        }

        if (!wrappedMe.isPresent())
            throw new Exception("Request to check a non-existent described template.");

        Core.DBDescribedTemplate me = wrappedMe.get();
        if (dt.getDocumentationHash().equals(me.documentationHash))
            return me;

        // Documentation (of template steps) needs to be recreated.
        String deleteQuery = "DELETE FROM dt_line WHERE fk_described_template = ?";
        try (PreparedStatement psDelete = this.connect.prepareStatement(deleteQuery)) {
            psDelete.setLong(1, me.pk);
            psDelete.executeUpdate();
        }

        // Updated documentation (of template steps) is written to db.
        String updateQuery = "UPDATE described_template SET description_hash=? WHERE pk_described_template=?";
        try (PreparedStatement psUpdate = this.connect.prepareStatement(updateQuery)) {
            this.connect.setAutoCommit(false);
            psUpdate.setBinaryStream(1, new ByteArrayInputStream(dt.getDocumentationHash().toBytes()));
            psUpdate.setLong(2, me.pk);
            psUpdate.executeUpdate();

            this.addActions(dt, me.pk);

            this.connect.commit();
        } catch (SQLException sqle) {
            this.connect.rollback();
            this.log.debug(".check() sees SQLException, rolls back commit, msg: " + sqle);
            throw sqle;
        } finally {
            this.connect.setAutoCommit(true);
        }

        return me;
    }







    @Override
    public void reportResult(String hash, Boolean result, String owner, Date start, Date ready, Date complete) throws SQLException {
        if (!this.read_only) {
            String query = "call add_run(?, ?, ?, ?, ?, ?)"; // stored procedure
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                preparedStatement.setString(1, hash);
                if (result != null)
                    preparedStatement.setBoolean(2, result);
                else
                    preparedStatement.setNull(2, Types.BOOLEAN);
                if (owner != null)
                    preparedStatement.setString(3, owner);
                else
                    preparedStatement.setNull(3, Types.VARCHAR);
                if (start != null)
                    preparedStatement.setTimestamp(4, new java.sql.Timestamp(start.getTime()));
                else
                    preparedStatement.setNull(4, Types.TIMESTAMP);
                if (ready != null)
                    preparedStatement.setTimestamp(5, new java.sql.Timestamp(ready.getTime()));
                else
                    preparedStatement.setNull(5, Types.TIMESTAMP);
                if (complete != null)
                    preparedStatement.setTimestamp(6, new java.sql.Timestamp(complete.getTime()));
                else
                    preparedStatement.setNull(6, Types.TIMESTAMP);
                preparedStatement.execute();
            } catch (SQLException sqle) {
                // TODO: handle
                throw sqle;
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
    public void addResultToRun(long runID, boolean result) throws Exception {
        if (!this.read_only) {
            String query = "Update run SET result=" + result +
                                        ", end_time=NOW()" +
                           " WHERE pk_run = " + runID;
            try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated == 0) {
                    throw new Exception("Failed to update run result. Run with id " + runID + " not found.");
                }
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
