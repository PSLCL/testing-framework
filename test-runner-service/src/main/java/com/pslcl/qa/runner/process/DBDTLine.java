package com.pslcl.qa.runner.process;

import java.util.Date;

/**
 * Relevant data base info for this test instance
 */
public class DBDTLine {
    // from dt_line
    long pk_dt_line;            // INT(11) in dt_line
    int line;                   // INT in dt_line
    String dtLineDescription;   // MEDIUMTEXT in dt_line

    // from artifact_to_dt_line
    boolean is_primary;         // BOOLEAN
    String reason;              // VARCHAR(45)

    // from artifact
    long pk_artifact;           // INT(11); stored only for matching content column to correct column in artifact
    String configuration;       // VARCHAR(45)
    String artifactName;        // VARCHAR(256)
    int mode;                   // INT
    boolean merge_source;       // BOOLEAN
    long derived_from_artifact; // INT(11) in artifact
    long merged_from_module;    // INT(11) in artifact

    // from module
    long pk_module;             // INT(11): stored only for matching module column to correct column in artifact
    String organization;        // VARCHAR(100)
    String moduleName;          // VARCHAR(100)
    String version;             // VARCHAR(45)
    String sequence;            // VARCHAR(45)
    String attributes;          // VARCHAR(200)
    Date scheduled_release;     // DATE
    Date actual_release;        // DATE
    int sort_order;             // INT

    // from content
    byte[] pk_content;          // BINARY(32)
    boolean is_generated;       // BOOLEAN

    // from resource
    byte[] resourceHash;        // BINARY(32)
    String resourceName;        // VARCHAR(45)
    String resourceDescription; // LONGTEXT


    /**
     *  Constructor
     */
    DBDTLine() {
        is_primary = false;
        reason = null;
        configuration = null;
        artifactName = null;
        mode = -1;
        merge_source = false;
        derived_from_artifact = -1;
        merged_from_module = -1;
        version = null;
        sequence = null;
        attributes = null;
        scheduled_release = null;
        actual_release = null;
        sort_order = -1;
        pk_content = null;
        is_generated = false;
        resourceHash = null;
        resourceName = null;
        resourceDescription = null;
    }

}