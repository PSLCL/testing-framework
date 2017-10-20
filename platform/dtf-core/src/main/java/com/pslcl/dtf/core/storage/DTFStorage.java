package com.pslcl.dtf.core.storage;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * The storage interface for the Distributed Testing Framework.
 */
public interface DTFStorage {

    void close();
    Connection getConnect();

    /**
     *
     * @return boolean
     */
    boolean isReadOnly();


    /**
     * Get a list of artifact providers.
     * @return The list of provider class names. Will not be null, may be empty
     * @throws SQLException on error
     */
    List<String> getArtifactProviders() throws SQLException;

    /**
     *
     */
    void updateModulesMissingCount() throws SQLException;

    /**
     *
     * @param deleteThreshold delete module when synchronize() passes exceed this number
     */
    void pruneModules(int deleteThreshold) throws SQLException;

    /**
     * Add a test plan to the database.
     * @param name The name of the test plan.
     * @param description The description of the test plan.
     * @return The primary key of the new test plan, or zero if there is an error or in read-only mode.
     * If the test plan already exists then the existing primary key is returned.
     * @throws SQLException on error
     */
    long addTestPlan(String name, String description) throws SQLException;

    /**
     * Add a test  to the database.
     * @param pk_test_plan The primary key of the test plan.
     * @param name The name of the test.
     * @param description The description of the test.
     * @param script The script of the test.
     * @return The primary key of the new test, or zero if there is an error or in read-only mode.
     * If the test already exists then the existing primary key is returned;
     * @throws SQLException on error
     */
    long addTest(long pk_test_plan, String name, String description, String script) throws SQLException;

    /**
     * Add a module to the database.
     * @param module The module to add.
     * @return The primary key of the new module, or zero if there is an error or in read-only mode.
     * If the module already exists then the existing primary key is returned.
     * @throws SQLException on error
     */
    long addModule(Module module) throws SQLException;

    /**
     * Find a module in the database.
     * @param module The module to find.
     * @return The primary key of the found module or zero for none
     * @throws SQLException on error
     */
    long findModule(Module module) throws SQLException;

    /**
     *
     * @param h The hash to find in database
     * @return true/false
     * @throws SQLException on error
     */
    boolean artifactFileHashStoredInDB(Hash h) throws SQLException;

    /**
     *
     * @param module The module to find.
     * @return The primary key of the found module or zero for none
     * @throws SQLException on error
     */
    long findModuleWithoutPriorSequence(Module module) throws SQLException;

    /**
     * Delete a module.
     * @param pk_module The primary key of the module to delete.
     */
    void deleteModule(long pk_module) throws SQLException;

    /**
     * Add content to the database.
     * @param content The content to add.
     * @throws SQLException on error
     */
    void addContent(Hash content) throws SQLException;

    /**
     * Add an artifact to a particular module and configuration, given a name and hash of the content.
     * @param pk_module The module the artifact relates to. Should not be 0 (not a legal primary key for module table)
     * @param configuration The configuration the artifact is part of.
     * @param name The name of the artifact.
     * @param mode The POSIX mode of the artifact.
     * @param content The hash of the file content, which must already exist in the system.
     * @param merge_source True if the artifact is associated with a merged module.
     * @param derived_from_artifact If non-zero, the primary key of the artifact that this artifact is derived from (for example, an archive file).
     * @param merged_from_module If non-zero, the primary key of the module that this artifact is merged from.
     * @return The primary key of the added artifact, as stored in the artifact table; 0 means not stored (like if db is read-only)
     */
    long addArtifact(long pk_module, String configuration, String name, int mode, Hash content, boolean merge_source, long derived_from_artifact, long merged_from_module) throws SQLException;

    /**
     * Clear the is_generated flag on all content. If not set before pruneContent() is called, then the content
     * will be deleted by pruneContent() unless associated with an artifact.
     */
    void clearGeneratedContent() throws SQLException;

    /**
     * In database, remove all non-generated content that is not referenced by any artifact.
     */
    void pruneContent() throws SQLException;

    /**
     * In database, remove all templates that are not referenced by any test instance.
     */
    void pruneTemplates() throws SQLException;

    /**
     * Return a set of all modules known to the database.
     * @param core The core instance.
     * @return The set of modules
     */
    Iterable<Module> createModuleSet(Core core) throws SQLException;

    /**
     * Create a set of all modules that match the specified organization and module name.
     * @param core The core instance.
     * @param organization The organizations to filter on.
     * @param name The module name to filter on.
     * @return A set of modules.
     */
    public Iterable<Module> createModuleSet(Core core, String organization, String name) throws SQLException;

    /**
     * See if test_instance.fk_described_template exists to match known primary key pkDescribedTemplate
     *
     * @param pkDescribedTemplate private key to match test_instance.fk_described_template
     * @return true on match
     * @throws SQLException on error
     */
    boolean describedTemplateHasTestInstanceMatch(long pkDescribedTemplate) throws SQLException;

    /**
     * Get the matching DBDescribedTemplate that matches the given key.
     *
     * @param matchKey The key to match.
     * @return The matching DBDescribedTemplate object, wrapped in Optional, which may be empty.
     * @throws SQLException on error
     */
    Optional<Core.DBDescribedTemplate> getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws SQLException;

}
