package com.pslcl.dtf.core;

import com.pslcl.dtf.core.generator.template.DescribedTemplate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
     * @throws Exception on fail
     */
    @SuppressWarnings("CaughtExceptionImmediatelyRethrown") // rethrow to take advantage of try-with-resources
    boolean match_ti_fk_described_template(Long match_pk_described_template) throws Exception {
        try (Statement statement = this.connect.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT pk_test_instance FROM test_instance" +
                                         " JOIN described_template ON fk_described_template=pk_described_template" +
                                         " WHERE pk_described_template=" + Long.toString(match_pk_described_template));)
        {
            return !resultSet.next();
        } catch (Exception e) {
            throw e;
        }
    }

    @SuppressWarnings({"CaughtExceptionImmediatelyRethrown", // rethrow to take advantage of try-with-resources
                       "ReturnOfNull"})
    Core.DBDescribedTemplate getDBDescribedTemplate_match_key(DescribedTemplate.Key matchKey) throws Exception {
        try (Statement statement = this.connect.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT pk_described_template, fk_module_set, description_hash, hash" +
                                              " FROM described_template JOIN template ON fk_template = pk_template");) {
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
