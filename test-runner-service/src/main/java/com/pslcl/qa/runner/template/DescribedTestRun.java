package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.process.DBDescribedTemplate;

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
    

}