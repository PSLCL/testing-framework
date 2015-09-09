package com.pslcl.qa.runner.template;

import java.util.Map;

import com.pslcl.qa.runner.process.DBDescribedTemplate;
import com.pslcl.qa.runner.process.DBRun;

/**
 * 
 *
 */
public class DescribedTestRun {

    private DBDescribedTemplate dbDescribedTemplate;
    private TemplateProvider tp;

    public DescribedTestRun(DBDescribedTemplate dbDescribedTemplate, TemplateProvider tp) {
        this.dbDescribedTemplate = dbDescribedTemplate;
        this.tp = tp;
    }
    
    /**
     * 
     */
    public void init() {
        InstancedTemplate it = tp.getInstancedTemplate(dbDescribedTemplate);

    }
    
    /**
     * 
     */
    public void initRunInfo() {
        
        Map<Long, DBRun> pkdtToDBRun;
        
        for (DBRun dbRun : this.dbDescribedTemplate.pkdtToDBRun.values()) {
            
        }
    }
    

}