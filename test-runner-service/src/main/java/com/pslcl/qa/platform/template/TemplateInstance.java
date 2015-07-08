package com.pslcl.qa.platform.template;

import com.pslcl.qa.platform.process.DBTestInstance;

public class TemplateInstance {
    
    DBTestInstance dbTestInstance;
    
    public TemplateInstance(DBTestInstance dbTestInstance) {
        this.dbTestInstance = dbTestInstance;
    }

    public void instantiate() {
        // parse steps; each step is terminated by \n
        String steps = dbTestInstance.steps;
        String step;
        for (int offset=0; offset>=0; offset+=step.length()) {
            int termOffset = steps.indexOf('\n', offset+1);
            if (termOffset < 0)
                break;
            step = steps.substring(offset, termOffset);
            System.out.println("TemplateInstance.instantiate() finds step " + step);
            
            //MachineImpl mi = new MachineImpl(); TODO: MachineImpl deleted
            
        }
    }
}
