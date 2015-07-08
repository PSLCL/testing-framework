package com.pslcl.qa.runner.process;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;



/**
 * Relevant data base info for this test instance
 */
public class DBTestInstance {
    public long pk_test_instance;
    public long pk_described_template;
    public long fk_run;
    public Date due_date;
    public int phase;
    public boolean iSynchronized;
    public byte[] fk_version_set;
    public long fk_template;
    public byte[] description_hash;
    public boolean dtSynchronized;
    public byte[] template_hash;
    public boolean enabled;
    public String steps;
    public Map<Long, DBDTLine> pkToDTLine;
    
    /**
     *  Constructor
     */
    DBTestInstance(long pk_test_instance) {
        this.pk_test_instance = pk_test_instance;
        pkToDTLine = new HashMap<Long,DBDTLine>();
    }
    
}