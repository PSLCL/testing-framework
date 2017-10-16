package com.pslcl.dtf.core.storage;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.SQLException;
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
