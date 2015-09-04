package com.pslcl.qa.runner.process;

import java.util.HashMap;
import java.util.Map;



/**
 * Relevant data base info for this template number
 */
public class DBDescribedTemplate {
    // fields of table described_template
    public long pk_described_template;      // INT(11)
    public byte[] fk_module_set = null;     // BINARY(32)
    public long fk_template = -1;           // INT(11)
    public byte[] description_hash = null;  // BINARY(32)
    public boolean dtSynchronized = false;  // BOOLEAN

    // corresponding fields of table template
    public byte[] template_hash = null;     // BINARY(32)
    public boolean enabled = false;         // BOOLEAN
    public String steps = null;             // MEDIUMTEXT

    // map of corresponding records in table run
    public Map<Long, DBRun> pkdtToDBRun;

    // map of corresponding records in table test_instance
    public Map<Long, DBTestInstance> pkdtToDBTestInstance;
    
    // map of corresponding records in table dt_line
    public Map<Long, DBDTLine> pkdtToDTLine;

    /**
     *  Constructor
     */
    DBDescribedTemplate(long pk_described_template) {
        this.pk_described_template = pk_described_template;
        pkdtToDBRun = new HashMap<Long,DBRun>();
        pkdtToDBTestInstance = new HashMap<Long,DBTestInstance>();
        pkdtToDTLine = new HashMap<Long,DBDTLine>();
    }

}