package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.Collections;
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
    public InstancedTemplate getInstancedTemplate(DBDescribedTemplate dbdt) {
        // first check our tracking- is a matching reusable template available?
        InstancedTemplate iT = availableReusableTemplates.get(dbdt.template_hash);
        if (iT != null) {
            availableReusableTemplates.remove(dbdt.template_hash); // note: this is early impl with no smarts- they asked for the described template, they get it 
        } else {
            iT = new InstancedTemplate();
            
            // populate iT with everything needed to behave as a reusable described template
            StepsParser stepsParser = new StepsParser(dbdt.steps);
            
            // Process bind steps now, since they come first; each returned list entry is self-referenced by steps line number, from 0...n
            boolean allReserved = true;
            List<ResourceWithAttributes> resources = stepsParser.computeResourceQuery(); // each element of returned list has its stepReference stored
            int stepsReference = resources.size();
            
            // reserve the resource specified by each bind step, with 360 second timeout for each reservation
            ResourceQueryResult rqr = rp.reserveIfAvailable(resources, 360);
            if (rqr != null) {
                // analyze the success/failure of each reserved resource, one for each bind step
                List<ReservedResourceWithAttributes> reservedResources = rqr.getReservedResources(); // list not in order, but each element of returned list has its stepReference stored
                
                if (reservedResources!=null) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + reservedResources.size() + " successful reserve-resource bind requests, now reserved");
                }

                List<ResourceWithAttributes> invalidResources = rqr.getInvalidResources(); // list not in order
                if (invalidResources!=null && !invalidResources.isEmpty()) {
                    allReserved = false;
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + invalidResources.size() + " invalid resource bind requests");
                }

                List<ResourceWithAttributes> unavailableResources = rqr.getUnavailableResources(); // list not in order
                if (unavailableResources!=null && !unavailableResources.isEmpty()) {
                    allReserved = false;
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + unavailableResources.size() + " unavailable resource bind requests");
                }
                
                if (allReserved) {
                    // Bind all resources of reservedResources 
                    List<? extends ResourceInstance> resourceInstances = rp.bind(reservedResources); // each element of returned list has its stepReference stored

                    // TODO: analyze the success/failure of each ResourceInstance
                    
                    // form ordered list for local use, by stepReference
                    OrderedResourceInfo orderedResourceInfo;
                    List<OrderedResourceInfo> orderedResourceInfos = new ArrayList<>();
                    for (int i=0; i<resourceInstances.size(); i++) {
                        orderedResourceInfo = new OrderedResourceInfo(resourceInstances.get(i));
                        orderedResourceInfos.add(orderedResourceInfo);
                    }
                    Collections.sort(orderedResourceInfos);
                    System.out.println("TemplateProvider.getInstancedTemplate() has orderedResourceInfos of size " + orderedResourceInfos);
                    
                    iT.setOrderedResourceInfos(orderedResourceInfos);
                    
                    // TODO: set iT with more more additional gathered info and instantiations and similar
                }
            } else {
                System.out.println("TemplateProvider.getInstancedTemplate() finds null ResourceQueryRequest");
            }
            
            // parse steps after bind
            while (true) {
                String step = stepsParser.getNextStep();
                if (step == null)
                    break;
                // process step with this rule: step command is the first characters leading up to the first space char
                int offset = 0;
                String spaceTermSubString = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                if (spaceTermSubString != null) {
                    offset += spaceTermSubString.length();
                    if (++offset > step.length())
                        offset = -1; // done parsing
                
                    switch (spaceTermSubString) {
                    case "include":
                        // templateHash
                        System.out.println("TemplateProvider.getInstancedTemplate() finds include as reference " + stepsReference);
                        String templateHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        // we do not further extract from step
                        if (templateHash != null) {
                            offset += templateHash.length();
                            if (++offset > step.length())
                                offset = -1; // done
                        }
                        
                        if (templateHash != null) {

                            // TODO: set iT with more gathered info and instantiations and similar
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds include to be incompletely specified");
                        }
                        break;
                    case "deploy":
                        // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                        System.out.println("TemplateProvider.getInstancedTemplate() finds deploy as reference " + stepsReference);
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
//                                    if (strArtifactHash != null) {
//                                        offset += strArtifactHash.length();
//                                        if (++offset > step.length())
//                                            offset = -1; // done
//                                    }
                                }
                            }
                        }
                          
                        if (strComponentName != null && strArtifactName != null && strArtifactHash != null) {
                            ArtifactInfo artifactInfo = new ArtifactInfo(strComponentName, strArtifactName, strArtifactHash);
                            int stepRef = Integer.parseInt(machineRef);
                            OrderedResourceInfo orderedResourceInfo = iT.getOrderedResourceInfo(stepRef);
                            boolean success = false;
                            if (orderedResourceInfo != null) {
                                orderedResourceInfo.setArtifactInfo(artifactInfo);
                            
                                // TODO: Call higher level api to deploy the artifact to the machine
                            
                                // TODO: set iT with more gathered info and instantiations and similar
                            
                                success = true;
                                System.out.println("TemplateProvider.getInstancedTemplate() deploys to bound resource " + stepRef + ": artifactInfo: " + strComponentName + " " + strArtifactName + " " + strArtifactHash);
                            }
                            if (!success)
                                System.out.println("TemplateProvider.getInstancedTemplate() fails to deploy to bound resource " + stepRef);
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds deploy to be incompletely specified");
                        }
                        break;                    
                    case "inspect":
                        // machineRef strInstructionsArtifactHash ArtifactInfo (strComponentName strArtifactName strArtifactHash)
                        System.out.println("TemplateProvider.getInstancedTemplate() finds inspect as reference " + stepsReference);
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
//                                        if (strArtifactHash != null) {
//                                            offset += strArtifactHash.length();
//                                            if (++offset > step.length())
//                                                offset = -1; // done
//                                        }
                                    }
                                }
                            }
                        }
                            
                        if (strInstructionsArtifactHash != null & strComponentName != null && strArtifactName != null && strArtifactHash != null) {
                            ArtifactInfo artifactInfo = new ArtifactInfo(strComponentName, strArtifactName, strArtifactHash);

                            // TODO: set iT with more gathered info and instantiations and similar
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds inspect to be incompletely specified");
                        }
                        break;
                    case "connect":
                        // machineRef strNetwork
                        System.out.println("TemplateProvider.getInstancedTemplate() finds connect as reference " + stepsReference);
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

                            // TODO: set iT with more gathered info and instantiations and similar
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds connect to be incompletely specified");
                        }
                        break;                    
                    case "configure":
                        // 
                        System.out.println("TemplateProvider.getInstancedTemplate() finds configure as reference " + stepsReference);
                        break;
                    case "start":
                        // 
                        System.out.println("TemplateProvider.getInstancedTemplate() finds start as reference " + stepsReference);
                        break;
                    case "run":
                        // machineRef ArtifactInfo (strComponentName strArtifactName strArtifactHash) strOptions
                        System.out.println("TemplateProvider.getInstancedTemplate() finds run as reference " + stepsReference);
      
                        // This os what actually comes in - it is missing some spaces - this is bug 7066 under app-test-platform
//                      run[]0emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE-providerMode
                        // is this intended?
//                      run 0 emit-oal-java bin%2FOperationsTest 562A789B9E66E0051F3DF3741985A0AFF67687C26DDC0EC93E774AA8FF6ADDEE -providerMode
    
//                      From an email                    
//                        The ordering of a command is:
//                            1.  The program action (run, start, etc.)
//                            2.  A program declaration (resource declaration, empty for run, since the reference is not used)
//                            3.  A machine reference
//                            4.  An artifact reference.
//                            5.  A possibly empty space-separated URL-encoded set of parameters.
                        
    
                        // this approximates what is needed
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
//                            if (strOptions != null) {
//                                offset += strOptions.length();
//                                if (++offset > step.length())
//                                    offset = -1; // done
//                            }
                        }
                            
                        if (strComponentName != null && strArtifactName != null && strArtifactHash != null) {
                            // strOptions is available; it may be null
                            ArtifactInfo artifactInfo = new ArtifactInfo(strComponentName, strArtifactName, strArtifactHash);
                            
                            // TODO: set iT with more gathered info and instantiations and similar
                            
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds run to be incompletely specified");
                        }
                        break;
                    case "run-forever":
                        System.out.println("TemplateProvider.getInstancedTemplate() finds run-forever as reference " + stepsReference);
                        break;
                    default:
                        System.out.println("TemplateProvider.getInstancedTemplate() finds something unexpected: " + spaceTermSubString + " as reference " + stepsReference);
                        break;
                        
                    // bind steps handled above our while(loop)
//                    case "bind":
//                        // resourceHash resourceAttributes
//                        System.out.println("TemplateProvider.getInstancedTemplate() finds bind");
//                        String strResourceAttributes = null;
//                        String resourceHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                        if (resourceHash != null) {
//                            offset += resourceHash.length();
//                            if (++offset > step.length())
//                                offset = -1; // done
//                          
//                            strResourceAttributes = StepsParser.getNextSpaceTerminatedSubString(step, offset);
//                            // we do not further extract from step
////                            if (resourceAttributes != null) {
////                                offset += resourceAttributes.length();
////                                if (++offset > step.length())
////                                    offset = -1; // done
////                            }
//                        }
//                      
//                        if (resourceHash != null && strResourceAttributes != null) {
//                          
//                        }
//                        break;
                    } // end switch()
                    stepsReference++;
                } // end if()
            } // end while()
        }
        return iT;
    }
}
