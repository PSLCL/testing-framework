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
    boolean aSynchronized;      // BOOLEAN
    String platform;            // VARCHAR(45)
    String internal_build;      // VARCHAR(45)
    String artifactName;        // VARCHAR(45)

    // from version
    long pk_version;            // INT(11); stored only for matching version column to correct column in artifact
    String version;             // VARCHAR(45)
    Date scheduled_release;     // DATE
    Date actual_release;        // DATE
    int sort_order;             // INT

    // from content
    byte[] pk_content;          // BINARY(32)
    boolean is_generated;       // BOOLEAN

    // from component
    String componentName;       // VARCHAR(50)

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
        aSynchronized = false;
        platform = null;
        internal_build = null;
        artifactName = null;
        version = null;
        scheduled_release = null;
        actual_release = null;
        sort_order = -1;
        pk_content = null;
        is_generated = false;
        resourceHash = null;
        resourceName = null;
        resourceDescription = null;
        componentName = null;
    }

}