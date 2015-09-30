package com.pslcl.qa.runner.process;

import java.util.Date;



/**
 * Relevant data base info for this template number
 */
public class DBRun {

    // fields of table run
    public long pk_run;                 // INT(11)
    public byte[] artifacts = null;     // LONGBLOB
    public Date start_time = null;      // DATETIME
    public Date ready_time = null;      // DATETIME
    public Date end_time = null;        // DATETIME
    public boolean passed = false;      // BOOLEAN

    /**
     *  Constructor
     */
    DBRun(long pk_run) {
        this.pk_run = pk_run;
    }

}