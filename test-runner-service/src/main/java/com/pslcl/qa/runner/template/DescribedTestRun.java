package com.pslcl.qa.runner.template;

import com.pslcl.qa.runner.process.DBDescribedTemplate;

/**
 * 
 *
 */
public class DescribedTestRun {

    private DBDescribedTemplate dbDescribedTemplate;
    private TemplateProvider tp; // TODO: make this available at a higher level

    public DescribedTestRun(DBDescribedTemplate dbDescribedTemplate) {
        this.dbDescribedTemplate = dbDescribedTemplate;
        tp = new TemplateProvider();
    }
    
    /**
     * 
     */
    public void init() {
        InstancedTemplate it = tp.getInstancedTemplate(dbDescribedTemplate);

        
        
        
        
        

    }
    

}