package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.resource.ResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceWithAttributesInstance;

public class StepsParser {
    
    public static int parseToSpace(String step, int offset) {
        int spaceOffset = step.indexOf(" ", offset);
        return spaceOffset;
    }
    
    /**
     * Get the next substring, which end at the char before next space char, or at last char of the full Step String.
     * 
     * @param step The full String being parsed.
     * @param offset Begin point of the step String; a negative offset eliminates processing. 
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
    
    /**
     * 
     * @param strResourceAttributes Must be space terminated; must follow K=V&K=V%K=V pattern; expecting no duplicate K (or if so only 1 is stored); empty K or V not expected but allowed
     * @return KVP Map, possibly empty, but without a null key entry
     */
    public static Map<String,String> getAttributeMap(String strResourceAttributes) {
        Map<String,String> ret = new HashMap<>(); // HashMap operates on unique keys; allows 1 null key, but we will not have any; values may be null, but we will not have any
        int offset = 0;
        int ampersandOffset = 0;
        while (ampersandOffset < strResourceAttributes.length()) {
            ampersandOffset = strResourceAttributes.indexOf("&", offset);
            if (ampersandOffset == -1)
                ampersandOffset = strResourceAttributes.length(); // last attribute
            int equalsOffset = strResourceAttributes.indexOf("=", offset);
            
            if (equalsOffset >= 0) // allow empty K to be extracted (will become an empty string in the returned HashMap)
//          if (equalsOffset >  0) // deny empty K
            {
                // get K
                String K = strResourceAttributes.substring(offset, equalsOffset);
                // get V
                String V = strResourceAttributes.substring(equalsOffset+1, ampersandOffset); // could be an empty String
                offset = ++ampersandOffset;
                ret.put(K, V);
            }
        }
        return ret;
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
                System.out.println("StepsParser.getNextStep() returns " + ((offset==termOffset) ? "EMPTY " : "") + "step: " + retStep);
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
     * @param stepTag String such as "bind " or "include "; trailing space is important
     * @return null for internal offset initially negative, or \n not found at or after internal offset
     * @return empty List<String> when the first step does not match param stepType
     * @return In-order List<String> when at least the first step matches param stepType  
     */
    List<String> getNextSteps(String stepTag) {
        if (steps == null) {
            System.out.println("StepsParser.getNextStep() finds null steps member variable"); // I never see this; it has beenn seen before
        }
        
        // process any one step with this rule: step command is the first characters leading up to the first space char
        List<String> retList = null; // capacity grows as elements are added; null elements are permitted
        if (offset >= 0 &&
            steps.indexOf('\n', offset) >= 0) // -1 return means steps has no \n at or after offset
        {
            retList = new ArrayList<String>(); // empty list
            for (int i=0; ; i++) {
                if (!steps.substring(offset, offset+stepTag.length()).equals(stepTag))
                    break;
                String strBindStep = getNextStep();
                retList.add(i, strBindStep); // i: 0 ... n
            }
        }
        return retList;
    }
    
    /**
     * 
     * @return
     */
    List<ResourceWithAttributes> computeResourceQuery() {
        List<ResourceWithAttributes> retList = new ArrayList<>();
        List<String> bindList = getNextSteps("bind "); // list is indexed by original line number
        for (int bindReference=0; bindReference<bindList.size(); bindReference++) {
            // add next bind resource to the resource request
            String bindStep = bindList.get(bindReference);
            System.out.println("StepsParser.computeResourceQuery() finds bind step " + bindReference + ": " + bindStep);
            int offset = 0;
            String bindText = StepsParser.getNextSpaceTerminatedSubString(bindStep, offset); // get past "bind " substring
            offset += (bindText.length() + 1);
            String resourceHash = StepsParser.getNextSpaceTerminatedSubString(bindStep, offset);
            if (resourceHash != null) {
                String strResourceAttributes = null;
                offset += resourceHash.length();
                if (++offset > bindStep.length())
                    offset = -1; // done
                
                strResourceAttributes = StepsParser.getNextSpaceTerminatedSubString(bindStep, offset);
                // we do not extract further from bindStep
//                if (resourceAttributes != null) {
//                    offset += resourceAttributes.length();
//                    if (++offset > step.length())
//                        offset = -1; // done
//                }

                ResourceWithAttributes rwa = new ResourceWithAttributesInstance(resourceHash, StepsParser.getAttributeMap(strResourceAttributes), bindReference);
                retList.add(rwa); // or retList.add(i, ra);
            }
        }
        return retList;
    }
    
}
