/*
 * Copyright (c) 2010-2015, Panasonic Corporation.
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.pslcl.dtf.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.dtf.core.runner.resource.ResourceCoordinates;
import com.pslcl.dtf.core.runner.resource.ResourceDescImpl;
import com.pslcl.dtf.core.runner.resource.ResourceDescription;

public class StepsParser {
    
    /**
     * From offset, return additional offset to next space.
     * 
     * @param step
     * @param offset
     * @return
     */
	public static int offsetToSpace(String step, int offset) {
        int spaceOffset = step.indexOf(" ", offset);
        return spaceOffset;
    }
    
    /**
     * Return the next substring, which ends at the char before next space char, or at last char of the full Step String.
     * 
     * @note Internal state is not available for this static method.
     * @param step The full String being parsed.
     * @param offset Begin point of the step String; a negative offset eliminates processing. 
     * @return null for negative offset
     */
    public static String peekNextSpaceTerminatedSubString(String step, int offset) {
        if (offset >= 0) {
            int spaceOffset = StepsParser.offsetToSpace(step, offset);
            if (spaceOffset >= 0) {
                return step.substring(offset, spaceOffset);
            }
            return step.substring(offset);
        }
        return null;
    }
    
    /**
     * 
     * @note Internal state is not available for this static method.
     * @param strResourceAttributes Must be space terminated; must follow K=V&K=V%K=V pattern; expecting no duplicate K (or if so only 1 is stored); empty K or V not expected but allowed
     * @return KVP Map, possibly empty, but without a null key entry
     */
    public static Map<String,String> peekAttributeMap(String strResourceAttributes) {
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
    private int stepReference;
    
    /**
     * Constructor. Set internal offset to 0.
     * @param steps
     */
    StepsParser(String steps) {
        this.steps = steps;
        offset = 0;
        stepReference = 0;
    }

    /**
     * 
     * @return
     */
    int getStepReference() {
    	return stepReference;
    }
    
    /**
     * Get next step.
     * Adjusts internal offset to match.
     * 
     * @return null for internal offset initially negative, or \n not found at or after internal offset 
     * @return empty String when \n is at offset
     * @return a real step when first \n found after offset
     */
    String getNextStep() {
        // 2 rules: every step is terminated by \n; any step can be empty (i.e. its first character can be \n)
        // parse one step
        String retStep = null;
        if (this.offset>=0 && steps!=null) {
            int termOffset = steps.indexOf('\n', this.offset); // -1 return means steps has no \n at or after offset
            if (termOffset >= 0) {
                retStep = steps.substring(this.offset, termOffset); // trailing \n not included
                System.out.println("StepsParser.getNextStep() returns stepReference " + stepReference + ", for " + ((this.offset==termOffset) ? "EMPTY " : "") + "step: " + retStep);
                ++this.stepReference;
                if (++termOffset >= steps.length())
                    termOffset = -1;
            }
            this.offset = termOffset;
        }
        return retStep;
    }
    
    /**
     * Get list of next steps of a specified step type.
     * Adjusts internal offset to match.
     * 
     * @param stepTag String such as "bind " or "include "; trailing space is important
     * @return null for internal offset initially negative, or \n not found at or after internal offset
     * @return empty List<String> when the first step does not match param stepType
     * @return In-order List<String> when at least the first step matches param stepType  
     */
    List<String> getNextSteps(String stepTag) {
        List<String> retList = null; // capacity grows as elements are added; null elements are permitted
        if (steps == null) {
            System.out.println("StepsParser.getNextSteps() finds null steps member variable"); // This has been seen before, fix it
        } else {
            // process any one step with this rule: stepTag is the first characters leading up and including the first space char
            if (this.offset >= 0 &&
                steps.indexOf('\n', this.offset) >= 0) // -1 return means steps has no \n at or after offset
            {
                retList = new ArrayList<String>(); // empty list
                for (int i=0; ; i++) {
                    if (!steps.substring(this.offset, this.offset+stepTag.length()).equals(stepTag))
                        break;
                    String strStep = getNextStep(); // adjusts this.offset
                    retList.add(i, strStep); // i: 0 ... n
                }
            }
        }
        return retList;
    }
    
    //TODO: after the merge of our two branches, the computeResourceQuery method is not being called anymore.
    // I assume you were just in between stuff on Friday.  Before the merge this method was the only method 
    // that would instantiate a ResourceDescImpl.  After the merge nobody is instantiating the ResourceDescImpl.
    // Just realize that pre-merge I assumed that "reference" (bindReference below) would be a valid unique 
    // resourceId.  I now assume this would not be the case, as the parse of a different template using the 
    // same resource would not have the have step number.  Thus throughout all of non-runner code "reference" 
    // has changed names to "resourceId".  Thus we now have templateId, resourceId and runId which are global
    // publicly known values, where the reference/bindReference/stepnumber is a local implementation value
    // only used by the TemplateProvider and does not belong in any of the dtf-core code.
    //
    // what you now have after the merging is a new ResourceCoordinate object which contains the templateId, 
    // resourceId and runId in a class with hash/equals methods that allow it to be an excellent key for any
    // map.
    //
    // Since the computeResourceQuery method has been disconnected, I follow back the calls to find a reasonable
    // place to stub in the newly needed runId.  I did have the templateId wired back to the DBTemplate (which 
    // now has code to hex string the hash), so when this gets tied back in and you get the needed new db access 
    // in to obtain it, the runId (pk_something) wired in as well.
    
    /*
     * The computeResourceQuery method is the master/original instantiator of all ResourceDescImpl objects.
     * It will use the ResourceDescriptions.resourceIdMaster AtomicLong to obtain a jvm wide unique long id
     * to associate with the new ResourceDescImpl as the resouceId.  The DBTemplate will provide the templateId
     * and some other db access will provide the runId.  These three comprise the hash/equals of the coordinates.
     *
     * These coordinates will remain consistent through out unless the dtf-runner modifies the runId (reuse).
     * These coordinates can get back to you in several ways
     *  1. simply via the ResourceDefination interface of any resource you have hold of.
     *  2. via the ResourceStatusListener callbacks (see bottom of TemplateProvider for example)
     *  3. via any FatalResourceException.
     *  
     *  In all of these cases, you can use the coordinates to get the step number of the executing template
     *  out of the following resourceToLineMap
     */
    Map<ResourceCoordinates, Integer> resourceToLineMap = new HashMap<ResourceCoordinates, Integer>();
    
    /**
     * Parse consecutive bind steps to form a ResourceDescription.
     * Adjusts internal offset to match.
     * 
     * @return
     */
    List<ResourceDescription> computeResourceQuery(String templateId, long runId) {
        List<ResourceDescription> retList = new ArrayList<>();
        List<String> bindList = getNextSteps("bind "); // list is indexed by original line number
        for (int bindReference=0; bindList!=null && bindReference<bindList.size(); bindReference++) {
            // add next bind resource to the resource request
            String bindStep = bindList.get(bindReference);
            System.out.println("StepsParser.computeResourceQuery() finds bind step " + bindReference + ": " + bindStep);
            int offset = 0;
            String bindText = StepsParser.peekNextSpaceTerminatedSubString(bindStep, offset); // get past "bind " substring
            offset += (bindText.length() + 1);
            String resourceHash = StepsParser.peekNextSpaceTerminatedSubString(bindStep, offset);
            if (resourceHash != null) {
                String strResourceAttributes = null;
                offset += resourceHash.length();
                if (++offset > bindStep.length())
                    offset = -1; // done
                
                strResourceAttributes = StepsParser.peekNextSpaceTerminatedSubString(bindStep, offset);
                // we do not extract further from bindStep
//                if (resourceAttributes != null) {
//                    offset += resourceAttributes.length();
//                    if (++offset > step.length())
//                        offset = -1; // done
//                }
                ResourceCoordinates coord = new ResourceCoordinates(templateId, ResourceDescription.resourceIdMaster.incrementAndGet(), runId);
                resourceToLineMap.put(coord, bindReference);
                ResourceDescription rwa = new ResourceDescImpl(resourceHash, coord, StepsParser.peekAttributeMap(strResourceAttributes));
                retList.add(rwa); // or retList.add(i, ra);
            }
        }
        return retList;
    }
}
