package com.pslcl.dtf.core;

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
     * @param pk_described_template private key to match test_instance.fk_described_template
     * @return boolean
     * @throws Exception on fail
     */
    @SuppressWarnings("CaughtExceptionImmediatelyRethrown") // rethrow in order to take advantage of try-with-resources
    boolean match_ti_fk_described_template(Long pk_described_template) throws Exception {

        try (Statement statement = this.connect.createStatement();
             ResultSet resSet = statement.executeQuery("SELECT pk_test_instance FROM test_instance" +
                     " JOIN described_template ON test_instance.fk_described_template=described_template.pk_described_template" +
                     " WHERE described_template.pk_described_template=" + Long.toString(pk_described_template));)
        {
            return !resSet.next();
        } catch (Exception e) {
            throw e;
        }
    }

}
