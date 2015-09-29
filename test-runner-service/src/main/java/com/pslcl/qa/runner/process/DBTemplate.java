package com.pslcl.qa.runner.process;

import java.util.Date;

public class DBTemplate {
    
    long pk_template;       // INT(11) in template
    public byte [] hash;           // BINARY(32) in template
    public String steps;           // MEDIUMTEXT in template
    boolean enabled;        // BOOLEAN in template
    long reNum;             // referencing run.pk_run
    byte[] artifacts;       // LONGBLOB in run
    Date start_time;        // DATETIME in run
    Date ready_time;        // DATETIME in run
    Date end_time;          // DATETIME in run
    Boolean result = false; // nullable BOOLEAN in run
    String owner;           // VARCHAR(128) in run
    
    /**
     *  Constructor
     */
    DBTemplate(long reNum) {
        this.reNum = reNum;
    }

}
