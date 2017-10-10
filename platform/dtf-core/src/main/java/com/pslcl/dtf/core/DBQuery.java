package com.pslcl.dtf.core;

import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBQuery {
    private Connection connect;

    DBQuery(Connection connect) {
        this.connect = connect;
    }

    /**
     * See if test_instance.fk_described_template exists to match known primary key pk_described_template
     *
     * @param match_pk_described_template private key to match test_instance.fk_described_template
     * @return boolean
     * @throws Exception on error
     */
    @SuppressWarnings("CaughtExceptionImmediatelyRethrown") // rethrow to take advantage of try-with-resources
    boolean testInstanceHasDescribedTemplateMatch(long match_pk_described_template) throws Exception {
        String query = "SELECT pk_test_instance FROM test_instance" +
                       " JOIN described_template ON fk_described_template=pk_described_template" +
                       " WHERE pk_described_template=?";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query)) {
            // ResultSet cleans up when its PreparedStatement cleans up.
            //      This allows us to keep these next 2 lines out of the above try-with-resources line.
            //      This avoids a strange IntelliJ compiler message for the .setLong() call.
            preparedStatement.setLong(1, match_pk_described_template);
            ResultSet resultSet = preparedStatement.executeQuery();

            return !resultSet.next();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get the matching DBDescribedTemplate that matches the given key.
     *
     * @param matchKey The given key to match.
     * @return The matching DBDescribedTemplate object, or null.
     * @throws Exception on error
     */
    @SuppressWarnings({"CaughtExceptionImmediatelyRethrown", // rethrow to take advantage of try-with-resources
                       "ReturnOfNull"}) // null is a specified api legal return value
    Core.DBDescribedTemplate getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws Exception {
        String query = "SELECT pk_described_template, fk_module_set, description_hash, hash" +
                       " FROM described_template JOIN template ON fk_template = pk_template";
        try (PreparedStatement preparedStatement = this.connect.prepareStatement(query);
             ResultSet resultSet = preparedStatement.executeQuery();)
        {
            // resultSet holds every described_template/template pair
            while (resultSet.next()) {
                DescribedTemplate.Key key = new DescribedTemplate.Key(new Hash(resultSet.getBytes("hash")),
                                                                      new Hash(resultSet.getBytes("fk_module_set")));
                if (key.equals(matchKey))
                    return new Core.DBDescribedTemplate(resultSet.getLong("pk_described_template"), key,
                                                        new Hash(resultSet.getBytes("description_hash")));
            }
            return null;
        } catch (Exception e) {
            throw e;
        }
    }

//    @SuppressWarnings("CaughtExceptionImmediatelyRethrown") // rethrow to take advantage of try-with-resources
//    boolean match_described_template_key(DescribedTemplate.Key matchKey) throws Exception {
//        try (Statement statement = this.connect.createStatement();
//             ResultSet resultSet = statement.executeQuery("SELECT fk_module_set, hash FROM described_template" +
//                                                          " JOIN template ON fk_template = pk_template");)
//        {
//            // resultSet holds every described_template/template pair
//            while (resultSet.next()) {
//                DescribedTemplate.Key key = new DescribedTemplate.Key(new Hash(resultSet.getBytes("hash")),
//                                                                      new Hash(resultSet.getBytes("fk_module_set")));
//                if (key.equals(matchKey))
//                    return true;
//            }
//            return false;
//        } catch (Exception e) {
//            throw e;
//        }
//    }

}
