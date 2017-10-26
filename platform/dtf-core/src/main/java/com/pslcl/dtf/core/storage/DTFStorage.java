package com.pslcl.dtf.core.storage;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.Hash;
import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
     * @return The set of modules
     */
    Iterable<Module> createModuleSet() throws SQLException;

    /**
     * Create a set of all modules that match the specified organization and module name.
     * @param organization The organizations to filter on.
     * @param name The module name to filter on.
     * @return A set of modules.
     */
    Iterable<Module> createModuleSet(String organization, String name) throws SQLException;

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
    Iterable<Artifact> findDependencies(Artifact artifact) throws SQLException;

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
    Iterable<Artifact[]> createArtifactSet(Attributes required, String configuration, String... name) throws SQLException;

    /**
     * Get the list of generators configured for all the tests.
     * @return A map where the keys are the primary keys of the tests and the values are the string to run the generator.
     */
    Map<Long, String> getGenerators() throws SQLException;

    /**
     * Update the generator status for a test, setting the last execute time and output.
     * @param pk_test The primary key of the test.
     * @param stdout The standard output of the generator run.
     * @param stderr The standard error of the generator run.
     */
    void updateTest(long pk_test, String stdout, String stderr) throws SQLException;

    /**
     * Return artifacts associated with a particular module (including version). Both name and configuration optional.
     * @param pk_module The primary key of the module to return artifacts for.
     * @param name The name, which can include MySQL REGEXP patterns and is also optional.
     * @param configuration The configuration, or null to include all.
     * @return The list of matching artifacts.
     */
    List<Artifact> getArtifacts(long pk_module, String name, String configuration) throws SQLException;

    /**
     * Return whether the specified module is associated with the current core target test. This is true
     * if there is a relationship from the test through the test plan to the component and version.
     * @param module The module.
     * @return boolean
     */
    boolean isAssociatedWithTest(Module module) throws SQLException;

    /**
     *
     * @param pk_module private key of table module
     * @throws SQLException on error
     */
    void updateModule(long pk_module) throws SQLException;

    /**
     * Check that an existing template is correct. If the template exists then the children
     * may also exist, but their documentation (of template steps) may be out of date, so update that.
     * @param dt The described template to check. Results are not currently checked.
     * @return DBDescribedTemplate
     */
    Core.DBDescribedTemplate check(DescribedTemplate dt) throws Exception;





    void reportResult(String hash, Boolean result, String owner, Date start, Date ready, Date complete)throws SQLException;

    /**
     *
     * @param runID The runID
     * @param result The result
     * @throws SQLException on failure
     */
    void addResultToRun(long runID, boolean result) throws Exception;

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
