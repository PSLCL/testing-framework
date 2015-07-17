package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.List;

public class StepsParser {
    
    public static int parseToSpace(String step, int offset) {
        int spaceOffset = step.indexOf(" ", offset);
        return spaceOffset;
    }
    
    /**
     * Next substring ends at char before space char, or at last char.
     * 
     * @note
     * @param step
     * @param offset
     * @return null for negative offset
     */
    public static String getNextSpaceTerminatedSubString(String step, int offset) {
        if (offset >= 0) {
            int spaceOffset = StepsParser.parseToSpace(step, offset);
            if (spaceOffset >= 0) {
                return step.substring(offset, spaceOffset);
            }
            return step.substring(offset);
        }
        return null;
    }
    
    
    // private instance members
    
    private String steps;
    private int offset;
    
    /**
     * Constructor. Set internal offset to 0.
     * @param steps
     */
    StepsParser(String steps) {
        this.steps = steps;
        offset = 0;
    }

    /**
     * Get next step, then increment internal offset to match.
     * 
     * @return null for internal offset initially negative, or \n not found at or after internal offset 
     * @return empty String when \n is at offset
     * @return a real step when first \n found after offset
     */
    String getNextStep() {
        // 2 rules: every step is terminated by \n; any step can be empty (i.e. its first character can be \n)
        // parse one step
        String retStep = null;
        if (offset >= 0) {
            int termOffset = steps.indexOf('\n', offset); // -1 return means steps has no \n at or after offset
            if (termOffset >= 0) {
                retStep = steps.substring(offset, termOffset); // trailing \n not included
                System.out.println("TemplateProvider.get() returns " + ((offset==termOffset) ? "EMPTY " : "") + "step: " + retStep);
                if (++termOffset >= steps.length())
                    termOffset = -1;
            }
            offset = termOffset;
        }
        return retStep;
    }
    
    /**
     * Get list of next steps of a specified step type, then increment internal offset to match.
     * 
     * @note In any template set of steps, all bind steps are placed first, identified by "bind ".
     * @note Normal use is: To get all bind steps, call this, after constructor, with "bind " param.
     * 
     * @param stepTag String such as "bind " or "include "
     * @return null for internal offset initially negative, or \n not found at or after internal offset
     * @return empty List<String> when the first step does not match param stepType
     * @return In-order List<String> when at least the first step matches param stepType  
     */
    List<String> getNextSteps(String stepTag) {
        List<String> retList = null; // capacity grows as elements are added; null elements are permitted
        if (offset >= 0 &&
            steps.indexOf('\n', offset) >= 0) // -1 return means steps has no \n at or after offset
        {
            retList = new ArrayList<String>(); // empty list
            for (int i=0; ; i++) {
                if (!steps.substring(offset, offset+4).equals(stepTag))
                    break;
                String strBindStep = getNextStep();
                retList.set(i, strBindStep);
            }
        }
        return retList;
    }
    
}