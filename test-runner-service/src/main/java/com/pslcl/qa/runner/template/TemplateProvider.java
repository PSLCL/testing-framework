package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.pslcl.qa.runner.ArtifactNotFoundException;
import com.pslcl.qa.runner.process.DBTemplate;
import com.pslcl.qa.runner.resource.BindResourceFailedException;
import com.pslcl.qa.runner.resource.IncompatibleResourceException;
import com.pslcl.qa.runner.resource.MachineInstance;
import com.pslcl.qa.runner.resource.NetworkInstance;
import com.pslcl.qa.runner.resource.PersonInstance;
import com.pslcl.qa.runner.resource.ReservedResourceWithAttributes;
import com.pslcl.qa.runner.resource.ResourceInstance;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.ResourceWithAttributes;

public class TemplateProvider {
    
    private Map<byte[],InstancedTemplate> availableInstancedTemplates; // note: this populates in the destroy method
    private ResourceProviders resourceProviders;
    
    public TemplateProvider(ExecutorService templateExecutorService) {
        availableInstancedTemplates = new HashMap<byte[],InstancedTemplate>();
        resourceProviders = new ResourceProviders(templateExecutorService); // determines these individual ResourceProvider S, such as AWSMachineProvider, and instantiates them
    }

    public void destroy(byte [] template_hash, InstancedTemplate iT) {
        // Can instead destroy iT. This is early impl with no smarts.
        availableInstancedTemplates.put(template_hash, iT);
    }

    /**
     * re-entrant
     * 
     * @param dbdt
     * @return
     */
    public InstancedTemplate getInstancedTemplate(DBTemplate dbT) {
        // first check our tracking- is a matching reusable template available?
        InstancedTemplate iT = availableInstancedTemplates.get(dbT.hash);
        // At any one time, there can be multiple instanced templates of the same .template_hash. They differ at least in which test run (or parent template) uses each, and in deployed artifacts.
        
        if (iT != null) {
            // iT is now in use
            availableInstancedTemplates.remove(dbT.hash);
            // Note: This is early impl with no smarts to optimize anything. At this line, they asked for the described template, they get it, and now it is not available to another user  
        } else {
            iT = new InstancedTemplate(String.valueOf(dbT.hash));
            // populate iT with everything needed to behave as a reusable described template
            StepsParser stepsParser = new StepsParser(dbT.steps);
            
            // Process bind steps now, since they come first; each returned list entry is self-referenced by steps line number, from 0...n
            List<ResourceWithAttributes> reserveResourceRequests = stepsParser.computeResourceQuery(); // each element of returned list has its stepReference stored
            int stepsReference = reserveResourceRequests.size();
            int originalReserveResourceRequestsSize = stepsReference;
            
            // reserve the resource specified by each bind step, with 360 second timeout for each reservation
            ResourceQueryResult rqr = resourceProviders.reserveIfAvailable(reserveResourceRequests, 360);
            if (rqr != null) {
                // analyze the success/failure of each reserved resource, one resource for each bind step
                List<ResourceWithAttributes> invalidResources = rqr.getInvalidResources(); // list is not in order
                if (invalidResources!=null && !invalidResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + invalidResources.size() + " reports of invalid resource bind requests");
                }

                List<ResourceWithAttributes> unavailableResources = rqr.getUnavailableResources(); // list is not in order
                if (unavailableResources!=null && !unavailableResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + unavailableResources.size() + " reports of unavailable resources for the given bind requests");
                }
                
                List<ResourceWithAttributes> availableResources = rqr.getAvailableResources();
                if (availableResources!=null && !availableResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + availableResources.size() + " reports of available resources for the given bind requests");
                }

                List<ReservedResourceWithAttributes> reservedResources = rqr.getReservedResources(); // list is not in order, but each element of returned list has its stepReference stored
                if (reservedResources!=null) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + reservedResources.size() + " successful reserve-resource bind requests; they are now reserved");
                }

                // Note: The size of reservedResources and its entries are the important thing. Coding is intended that any reservedResource has no entries in the other three lists.
                //       However, the ultimate rule is that an entry in the reservedResource list means that the resource is reserved, independent of what many ResourceProviders may have placed in the alternate lists. 
                if (reservedResources.size() == originalReserveResourceRequestsSize) {
                    // bind all resources of reservedResources, and receive a ResourceInstance for each one
                    List<Future<? extends ResourceInstance>> resourceInstances;
                    try {
                        resourceInstances = resourceProviders.bind(reservedResources, null);
                        
                        // resourceInstances is a Future list that is returned quickly; i.e. without waiting for all the asynch binds to complete
                        // loop, waiting for each encountered asynch .bind() to complete its work; in other words convert our .bind(multiple) call to synchronous
                        // convert the Future list to a list of returned ResourceInstance S
                        List<ResourceInstance> listRI = new ArrayList<>();
                        for (Future<? extends ResourceInstance> future : resourceInstances) {
                            try {
                                MachineInstance machineInstance = MachineInstance.class.cast(future.get());
                                listRI.add(machineInstance);
                            } catch (InterruptedException | ExecutionException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        
                        
                        // TODO: analyze the success/failure of each returned ResourceInstance
                        //   for example: it is an error that resou
                        
                        // for local use, form bound resource list, ordered by stepReference
                        List<ResourceInfo> orderedResourceInfos = new ArrayList<>();
                        for (int i=0; i<listRI.size(); i++) {
                            ResourceInfo resourceInfo = new ResourceInfo(listRI.get(i));
                            orderedResourceInfos.add(resourceInfo);
                        }
                        Collections.sort(orderedResourceInfos);
                        System.out.println("TemplateProvider.getInstancedTemplate() has orderedResourceInfos of size " + orderedResourceInfos.size());
                        
                        iT.setOrderedResourceInfos(orderedResourceInfos);
                        // TODO: set iT with more gathered info and instantiations and similar
                    } catch (BindResourceFailedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } // each element of returned list has its stepReference stored
                    
                } else {
                    // TODO: deal with this failure to reserve some of the resources, including cleanup of whatever resources are reserved but might no longer be needed
                }
            } else {
                System.out.println("TemplateProvider.getInstancedTemplate() finds null ResourceQueryRequest");
            }
            
            // parse steps after bind
            while (true) {
                String step = stepsParser.getNextStep();
                if (step == null)
                    break;
                // process step with this rule: step command is the first set of characters, leading up to the first space char
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
                        // machineRef ArtifactInfo (strArtifactName strArtifactHash)
                        System.out.println("TemplateProvider.getInstancedTemplate() finds deploy as reference " + stepsReference);
                        String strArtifactName = null;
                        String strArtifactHash = null;
                        String machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (machineRef != null) {
                            offset += machineRef.length();
                            if (++offset > step.length())
                                offset = -1; // done
                          
                            // gather ArtifactInfo (strArtifactName strArtifactHash)
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
                          
                        if (strArtifactName != null && // note: %2F within is URL encoding for "/"
                            strArtifactHash != null) {
                            int stepRef = Integer.parseInt(machineRef);
                            // This next line takes advantage of the prior ordering of the orderedResourceInfos list held by iT. Using index value stepRef, this call returns the proper ResourceInfo that holds the correct machine to deploy to.
                            ResourceInfo resourceInfo = iT.getResourceInfo(stepRef);
                            boolean success = false;
                            if (resourceInfo != null) {
                                ResourceInstance resourceInstance = resourceInfo.getResourceInstance();
                                if (MachineInstance.class.isInstance(resourceInstance)) {
                                    // deploy artifact to the machine instance
                                    MachineInstance machineInstance = MachineInstance.class.cast(resourceInstance);
//                                  try {
                                        machineInstance.deploy(strArtifactName, strArtifactHash);
                                        ArtifactInfo artifactInfo = new ArtifactInfo(strArtifactName, strArtifactHash);
                                        resourceInfo.setArtifactInfo(artifactInfo);
                                        // TODO: set iT with more gathered info and instantiations and similar

                                        success = true;
                                        System.out.println("TemplateProvider.getInstancedTemplate() deploys to bound resource " + stepRef + ": artifactInfo: " + strArtifactName + " " + strArtifactHash);
//                                  } finally {
//                                  }
                                }
                            }
                            if (!success)
                                System.out.println("TemplateProvider.getInstancedTemplate() fails to deploy to bound resource " + stepRef);
                        } else {
                            System.out.println("TemplateProvider.getInstancedTemplate() finds deploy to be incompletely specified");
                        }
                        break;                    
                    case "inspect":
                        // machineRef strInstructionsHash ArtifactInfo (strArtifactName strArtifactHash); October note: multiple ArtifactInfo may be allowed to follow
                        System.out.println("TemplateProvider.getInstancedTemplate() finds inspect as reference " + stepsReference);
                        String strInstructionsHash = null;
                        strArtifactName = null;
                        strArtifactHash = null;
                        Map<String, String> mapArtifacts = new HashMap<>();
                        machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (machineRef != null) {
                            offset += machineRef.length();
                            if (++offset > step.length())
                                offset = -1; // done
                            
                            strInstructionsHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            if (strInstructionsHash != null) {
                                offset += strInstructionsHash.length();
                                if (++offset > step.length())
                                    offset = -1; // done
                                
                                // gather ArtifactInfo (strArtifactName strArtifactHash)
                                strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                if (strArtifactName != null) {
                                    offset += strArtifactName.length();
                                    if (++offset > step.length())
                                        offset = -1; // done
                                    strArtifactHash = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                                    mapArtifacts.put(strArtifactName, strInstructionsHash); // new API says we pass artifacts as a map

                                    // we do not further extract from step // Oct 1 note: we might now have more artifacts being specified, and for each we would .put to mapArtifacts
//                                        if (strArtifactHash != null) {
//                                            offset += strArtifactHash.length();
//                                            if (++offset > step.length())
//                                                offset = -1; // done
//                                        }
                                }
                            }
                        }
                            
                        if (strInstructionsHash != null && strArtifactName != null && strArtifactHash != null) {
                            int stepRef = Integer.parseInt(machineRef);
                            ResourceInfo resourceInfo = iT.getResourceInfo(stepRef);
                            boolean success = false;
                            if (resourceInfo != null) {
                                ResourceInstance resourceInstance = resourceInfo.getResourceInstance();
                                if (PersonInstance.class.isInstance(resourceInstance)) {
                                    // get the string of actual instructions from strInstructionHash
                                    String strInstructions = ArtifactInfo.getContent(strInstructionsHash);
                                    
                                    // inspect: give artifact to the person instance
                                    PersonInstance personInstance = PersonInstance.class.cast(resourceInstance);
                                    try {
                                        personInstance.inspect(strInstructions, mapArtifacts);
                                        // TODO: now that there can be more than 1 artifact (as found in mapArtifacts), change this code that sets resourceInfo
                                        ArtifactInfo artifactInfo = new ArtifactInfo(strArtifactName, strArtifactHash);
                                        resourceInfo.setInstructionsHash(strInstructionsHash);
                                        resourceInfo.setArtifactInfo(artifactInfo);
                                        
                                        // TODO: set iT with more gathered info and instantiations and similar

                                        success = true;
                                        System.out.println("TemplateProvider.getInstancedTemplate() initiates inspect to bound resource " + stepRef);
                                    } catch (ArtifactNotFoundException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (!success)
                                System.out.println("TemplateProvider.getInstancedTemplate() fails inspect to bound resource " + stepRef);
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
                            boolean success = false;
                            int stepRef = Integer.parseInt(machineRef);
                            ResourceInfo resourceInfo = iT.getResourceInfo(stepRef);
                            if (resourceInfo != null) {
                                ResourceInstance resourceInstance = resourceInfo.getResourceInstance();
                                if (MachineInstance.class.isInstance(resourceInstance)) {
                                    // connect network machineInstance
                                    MachineInstance machineInstance = MachineInstance.class.cast(resourceInstance);
                                    // TODO: for new API, we need to instantiate something that implements java interface NetworkInstance; strNetwork is available to help that instantiation
                                    NetworkInstance network = null;    
                                    try {
                                        machineInstance.connect(network);
                                        resourceInfo.setNetwork(strNetwork);
                                        
                                        // TODO: set iT with more gathered info and instantiations and similar

                                        success = true;
                                        System.out.println("TemplateProvider.getInstancedTemplate() connects network " + strNetwork + "to bound resource " + stepRef);
                                    } catch (IncompatibleResourceException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            }
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
                        // machineRef strArtifactName strOptions
                        System.out.println("TemplateProvider.getInstancedTemplate() finds run as reference " + stepsReference);
      
                        // This is what actually comes in - it is missing some spaces - this is bug 7066 under app-test-platform
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
                        strArtifactName = null;
                        String strRunParams = null;
                        machineRef = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                        if (machineRef != null) {
                            offset += machineRef.length();
                            if (++offset > step.length())
                                offset = -1; // done
    
                            // gather strArtifactName
                            strArtifactName = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            if (strArtifactName != null) {
                                offset += strArtifactName.length();
                                if (++offset > step.length())
                                    offset = -1; // done
                                }
                            }
                            
                            strRunParams = StepsParser.getNextSpaceTerminatedSubString(step, offset);
                            // we do not further extract from step
//                            if (strOptions != null) {
//                                offset += strOptions.length();
//                                if (++offset > step.length())
//                                    offset = -1; // done
//                            }
                            
                        if (strArtifactName != null) {
                            // strOptions is available; it may be null
                            int stepRef = Integer.parseInt(machineRef);
                            ResourceInfo resourceInfo = iT.getResourceInfo(stepRef);
                            boolean success = false;
                            if (resourceInfo != null) {
                                ResourceInstance resourceInstance = resourceInfo.getResourceInstance();
                                if (MachineInstance.class.isInstance(resourceInstance)) {
                                    // run program on machineInstance
                                    MachineInstance machineInstance = MachineInstance.class.cast(resourceInstance);
//                                  try {
                                        String command = strArtifactName + (strRunParams!=null ? (" " +strRunParams): ""); // command is an executable command, including arguments, to be run on the machine
                                        machineInstance.run(command);
                                        ArtifactInfo artifactInfo = new ArtifactInfo(strArtifactName, null); // null because we do not know its value
                                        resourceInfo.setArtifactInfo(artifactInfo);
                                        resourceInfo.setRunParams(strRunParams);
                                        
                                        // TODO: set iT with more gathered info and instantiations and similar

                                        success = true;
                                        System.out.println("TemplateProvider.getInstancedTemplate() runs to bound resource " + stepRef + ": artifactInfo: " + strArtifactName);
//                                  }
                                }
                            }
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
                        
                    // bind steps are already handled above our while(loop)
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