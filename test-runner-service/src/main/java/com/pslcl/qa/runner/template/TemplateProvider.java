package com.pslcl.qa.runner.template;

import java.util.HashMap;
import java.util.Map;

import com.pslcl.qa.runner.process.DBDescribedTemplate;

public class TemplateProvider {
    
    private Map<byte[],InstancedTemplate> availableReusableTemplates; // note: this populates in the destroy method
    
    public TemplateProvider() {
        availableReusableTemplates = new HashMap<byte[],InstancedTemplate>();
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
            int reference = -1;
            while (true) {
                reference++;
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
                case "bind":
                    // resourceHash resourceAttributes
                    System.out.println("InstancedTemplate.get() finds bind");
                    String resourceAttributes = null;
                    String resourceHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    if (resourceHash != null) {
                        offset += resourceHash.length();
                        if (++offset > step.length())
                            offset = -1; // done
                        
                        resourceAttributes = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        // we do not further extract from step
//                        if (resourceAttributes != null) {
//                            offset += resourceAttributes.length();
//                            if (++offset > step.length())
//                                offset = -1; // done
//                        }
                    }
                    
                    if (resourceHash != null && resourceAttributes != null) {
                        
                        
                        
//                        MachineImpl mi = new MachineImpl();
//                        try {
//                            mi.bind(resourceHash, resourceAttributes);
//                        } catch (ResourceNotFoundException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
                    }
                    break;
                case "include":
                    // templateHash
                    System.out.println("InstancedTemplate.get() finds include");
                    String templateHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                    // we do not further extract from step
//                    if (templateHash != null) {
//                        offset += templateHash.length();
//                        if (++offset > step.length())
//                            offset = -1; // done
//                    }
                    
                    if (templateHash != null) {
                        
                    }
                    break;
                case "deploy":
                    // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                    System.out.println("InstancedTemplate.get() finds deploy");
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
                    System.out.println("InstancedTemplate.get() finds inspect");
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
    //                                if (strArtifactHash != null) {
    //                                    offset += strArtifactHash.length();
    //                                    if (++offset > step.length())
    //                                        offset = -1; // done
    //                                }
                                }
                            }
                            
                            
                        }
                    }
                        
                    if (strInstructionsArtifactHash != null & strComponentName != null && strArtifactName != null && strArtifactHash != null) {

                    }
                    break;
                case "connect":
                    // machineRef strNetwork
                    System.out.println("InstancedTemplate.get() finds connect");
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
                    System.out.println("InstancedTemplate.get() finds configure");
                    break;
                case "start":
                    // 
                    System.out.println("InstancedTemplate.get() finds start");
                    break;
                case "run":
                    // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash) strOptions
                    System.out.println("InstancedTemplate.get() finds run");
  
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
                    System.out.println("InstancedTemplate.get() finds run-forever");
                    break;
                default:
                    System.out.println("InstancedTemplate.get() finds something unexpected");
                    break;
                } // end switch()
            }
        }
        return iT;
    }

}