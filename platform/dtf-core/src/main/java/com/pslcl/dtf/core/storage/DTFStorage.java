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
     */
    long addTest(long pk_test_plan, String name, String description, String script) throws SQLException;

    /**
     * Add a module to the database.
     * @param module The module to add.
     * @return The primary key of the new module, or zero if there is an error or in read-only mode.
     * If the module already exists then the existing primary key is returned.
     */
    long addModule(Module module) throws SQLException;

    /**
     * Find a module in the database.
     * @param module The module to find.
     * @return The primary key of the found module or zero for none
     */
    long findModule(Module module) throws SQLException;

    /**
     *
     * @param h The hash to find in database
     * @return true/false
     */
    boolean artifactFileHashStoredInDB(Hash h) throws SQLException;

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
