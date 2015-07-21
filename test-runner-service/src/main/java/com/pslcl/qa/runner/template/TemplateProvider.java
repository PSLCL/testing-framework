package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.qa.runner.process.DBDescribedTemplate;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceProviderImpl;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class TemplateProvider {
    
    private Map<byte[],InstancedTemplate> availableReusableTemplates; // note: this populates in the destroy method
    private ResourceProviderImpl rp;
    
    public TemplateProvider() {
        availableReusableTemplates = new HashMap<byte[],InstancedTemplate>();
        rp = new ResourceProviderImpl();
    }

    public void destroy(byte [] template_hash, InstancedTemplate iT) { 
        availableReusableTemplates.put(template_hash, iT); // note: this is early impl with no smarts
    }

    /**
     * 
     * @param dbdt
     * @return
     */
    public InstancedTemplate get(DBDescribedTemplate dbdt) {
        // first check our tracking- is a matching reusable template available?
        InstancedTemplate iT = availableReusableTemplates.get(dbdt.template_hash);
        if (iT != null) {
            availableReusableTemplates.remove(dbdt.template_hash); // note: this is early impl with no smarts- they asked for the described template, they get it 
        } else {
            iT = new InstancedTemplate();
            
            // populate iT with everything needed to behave as a reusable described template
            StepsParser stepsParser = new StepsParser(dbdt.steps);
            
            // Process bind steps now, since they come first; each list member is self-referenced by steps line number, from 0...n 
            List<ResourceWithAttributes> resources = stepsParser.computeResourceQuery();
            int stepsReference = resources.size();
            
            // reserve the resource specified by each bind step
            ResourceQueryResult rqr = rp.reserveIfAvailable(resources, 60);
            if (rqr != null) {
                // analyze the success/failure of each bind step
                List<ResourceWithAttributes> invalidResources = rqr.getInvalidResources();
                if (invalidResources!=null && !invalidResources.isEmpty()) {
                    System.out.println("TemplateProvider.get() finds " + invalidResources.size() + " invalid resource bind requests");
                }

                List<ResourceWithAttributes> unavailableResources = rqr.getUnavailableResources();
                if (unavailableResources!=null && !unavailableResources.isEmpty()) {
                    System.out.println("TemplateProvider.get() finds " + unavailableResources.size() + " unavailable resource bind requests");
                }
                
                List<ReservedResourceWithAttributes> reservedResources = rqr.getReservedResources();
                if (reservedResources!=null) {
                    System.out.println("TemplateProvider.get() finds " + reservedResources.size() + " reserved resource bind requests");
                }
                
                // Bind the reservedResources
                
                //: REVIEW: reservedResources is or is NOT indexed according to steps' line number, from 0...n
                
                List<ResourceWithAttributes> bindableResources = new ArrayList<>();
                for (int reference=0; reference<reservedResources.size(); reference++) {
                    // REVIEW: no need to .add with specified index?
                    // TODO: add reference to ResourceWithAttributes constructor
                    bindableResources.add(reference, new ResourceWithAttributes(reservedResources.get(reference).getHash(), reservedResources.get(reference).getAttributes()));
                }
                List<ResourceInstance> resourceInstances = rp.bind(bindableResources);
                
                // TODO: analyze the success/failure of each ResourceInstance
                
            } else {
                System.out.println("TemplateProvider.get() finds null ResourceQueryRequest");
            }
            
            while (true) {
                // deal with steps after bind, next will be template include
                String step = stepsParser.getNextStep();
                if (step == null)
                    break;
                // process step with this rule: step command is the first characters leading up to the first space char
                int offset = 0;
                String strCommand = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                if (strCommand != null) {
                    offset += strCommand.length();
                    if (++offset > step.length())
                        offset = -1; // done
                }
                
                switch (strCommand) {
//                case "bind":
//                    // resourceHash resourceAttributes
//                    System.out.println("TemplateProvider.get() finds bind");
//                    String strResourceAttributes = null;
//                    String resourceHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                    if (resourceHash != null) {
//                        offset += resourceHash.length();
//                        if (++offset > step.length())
//                            offset = -1; // done
//                        
//                        strResourceAttributes = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                        // we do not further extract from step
////                        if (resourceAttributes != null) {
////                            offset += resourceAttributes.length();
////                            if (++offset > step.length())
////                                offset = -1; // done
////                        }
//                    }
//                    
//                    if (resourceHash != null && strResourceAttributes != null) {
//                        ResourceWithAttributes ra = new ResourceWithAttributes(resourceHash, StepsParser.getAttributeMap(strResourceAttributes));
//                        //rp.bind(ra);
//                        
//                    }
//                    break;
                case "include":
                    // templateHash
                    System.out.println("TemplateProvider.get() finds include as reference " + stepsReference);
                    String templateHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    // we do not further extract from step
                    if (templateHash != null) {
                        offset += templateHash.length();
                        if (++offset > step.length())
                            offset = -1; // done
                    }
                    
                    if (templateHash != null) {
                        
                    }
                    break;
                case "deploy":
                    // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                    System.out.println("TemplateProvider.get() finds deploy as reference " + stepsReference);
                    String strComponentName = null;
                    String strArtifactName = null;
                    String strArtifactHash = null;
                    String machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    if (machineRef != null) {
                        offset += machineRef.length();
                        if (++offset > step.length())
                            offset = -1; // done
                      
                        // gather ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                        strComponentName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (strComponentName != null) {
                            offset += strComponentName.length();
                            if (++offset > step.length())
                                offset = -1; // done
                          
                            strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            if (strArtifactName != null) {
                                offset += strArtifactName.length();
                                if (++offset > step.length())
                                    offset = -1; // done
                              
                                strArtifactHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                // we do not further extract from step
//                                if (strArtifactHash != null) {
//                                    offset += strArtifactHash.length();
//                                    if (++offset > step.length())
//                                        offset = -1; // done
//                                }
                            }
                        }
                    }
                      
                    if (strComponentName != null && strArtifactName != null && strArtifactHash != null) {

                    }
                    break;                    
                case "inspect":
                    // machineRef strInstructionsArtifactHash ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                    System.out.println("TemplateProvider.get() finds inspect as reference " + stepsReference);
                    String strInstructionsArtifactHash = null;
                    strComponentName = null;
                    strArtifactName = null;
                    strArtifactHash = null;
                    machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    if (machineRef != null) {
                        offset += machineRef.length();
                        if (++offset > step.length())
                            offset = -1; // done
                        
                        strInstructionsArtifactHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (strInstructionsArtifactHash != null) {
                            offset += strInstructionsArtifactHash.length();
                            if (++offset > step.length())
                                offset = -1; // done
                            
                            
                            // gather ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                            strComponentName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            if (strComponentName != null) {
                                offset += strComponentName.length();
                                if (++offset > step.length())
                                    offset = -1; // done
                            
                                strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                if (strArtifactName != null) {
                                    offset += strArtifactName.length();
                                    if (++offset > step.length())
                                        offset = -1; // done
                                
                                    strArtifactHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                    // we do not further extract from step
//                                    if (strArtifactHash != null) {
//                                        offset += strArtifactHash.length();
//                                        if (++offset > step.length())
//                                            offset = -1; // done
//                                    }
                                }
                            }
                        }
                    }
                        
                    if (strInstructionsArtifactHash != null & strComponentName != null && strArtifactName != null && strArtifactHash != null) {

                    }
                    break;
                case "connect":
                    // machineRef strNetwork
                    System.out.println("TemplateProvider.get() finds connect as reference " + stepsReference);
                    String strNetwork = null;
                    machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    if (machineRef != null) {
                        offset += machineRef.length();
                        if (++offset > step.length())
                            offset = -1; // done
                        
                        strNetwork = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        // we do not further extract from step
//                        if (strNetwork != null) {
//                            offset += strNetwork.length();
//                            if (++offset > step.length())
//                                offset = -1; // done
//                        }
                    }
                        
                    if (strNetwork != null) {
                            
                    }
                    break;                    
                case "configure":
                    // 
                    System.out.println("TemplateProvider.get() finds configure as reference " + stepsReference);
                    break;
                case "start":
                    // 
                    System.out.println("TemplateProvider.get() finds start as reference " + stepsReference);
                    break;
                case "run":
                    // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash) strOptions
                    System.out.println("TemplateProvider.get() finds run as reference " + stepsReference);
  
                    // This comes in - it is missing some spaces - this is bug 7066 under app-test-platform
//                  run[]0emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE-providerMode
                    // is this intended?
//                  run 0 emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE -providerMode

//                  From an email                    
//                    The ordering of a command is:
//                        1.  The program action (run, start, etc.)
//                        2.  A program declaration (resource declaration, empty for run, since the reference is not used)
//                        3.  A machine reference
//                        4.  An artifact reference.
//                        5.  A possibly empty space-separated URL-encoded set of parameters.
                    

                    // this code approximates what is needed
                    strComponentName = null;
                    strArtifactName = null;
                    strArtifactHash = null;
                    String strOptions = null;
                    machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    if (machineRef != null) {
                        offset += machineRef.length();
                        if (++offset > step.length())
                            offset = -1; // done

                        // gather ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                        strComponentName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (strComponentName != null) {
                            offset += strComponentName.length();
                            if (++offset > step.length())
                                offset = -1; // done
                            
                            strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            if (strArtifactName != null) {
                                offset += strArtifactName.length();
                                if (++offset > step.length())
                                    offset = -1; // done
                                
                                strArtifactHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                if (strArtifactHash != null) {
                                    offset += strArtifactHash.length();
                                    if (++offset > step.length())
                                        offset = -1; // done
                                }
                            }
                        }
                        
                        strOptions = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        // we do not further extract from step
//                        if (strOptions != null) {
//                            offset += strOptions.length();
//                            if (++offset > step.length())
//                                offset = -1; // done
//                        }
                    }
                        
                    if (strComponentName != null && strArtifactName != null && strArtifactHash != null) {
                        // strOptions is available; it may be null
                        
                    }
                    break;
                case "run-forever":
                    System.out.println("TemplateProvider.get() finds run-forever as reference " + stepsReference);
                    break;
                default:
                    System.out.println("TemplateProvider.get() finds something unexpected: " + strCommand + " as reference " + stepsReference);
                    break;
                } // end switch()
                stepsReference++;
                // TODO: set iT with info gathered
            } // end while()
        }
        return iT;
    }
}