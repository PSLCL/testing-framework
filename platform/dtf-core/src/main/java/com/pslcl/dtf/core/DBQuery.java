package com.pslcl.dtf.core;

import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DBQuery {
    private Connection connect;

    DBQuery(Connection connect) {
        this.connect = connect;
    }

    /**
     * See if test_instance.fk_described_template exists to match known primary key pk_described_template
     *
     * @param match_pk_described_template private key to match test_instance.fk_described_template
     * @return true on match
     * @throws SQLException on error
     */
    boolean testInstanceHasDescribedTemplateMatch(long match_pk_described_template) throws SQLException {
        String query = "SELECT pk_test_instance FROM test_instance" +
                       " JOIN described_template ON fk_described_template=pk_described_template" +
                       " WHERE pk_described_template=?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            preparedStatement.setLong(1, match_pk_described_template);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return !resultSet.next();
            }
        }
    }

    /**
     * Get the matching DBDescribedTemplate that matches the given key.
     *
     * @param matchKey The key to match.
     * @return The matching DBDescribedTemplate object, wrapped in Optional, which may be empty.
     * @throws SQLException on error
     */
    Optional<Core.DBDescribedTemplate> getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws SQLException {
        String query = "SELECT pk_described_template, fk_module_set, description_hash, hash" +
                       " FROM described_template JOIN template ON fk_template = pk_template";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery())
        {
            // resultSet holds every described_template/template pair
            while (resultSet.next()) {
                DescribedTemplate.Key key = new DescribedTemplate.Key(new Hash(resultSet.getBytes("hash")),
                                                                      new Hash(resultSet.getBytes("fk_module_set")));
                if (key.equals(matchKey)) {
                    return Optional.of(new Core.DBDescribedTemplate(resultSet.getLong("pk_described_template"), key,
                                                                    new Hash(resultSet.getBytes("description_hash"))));
                }
            }
            return Optional.empty();
        }
    }

}
