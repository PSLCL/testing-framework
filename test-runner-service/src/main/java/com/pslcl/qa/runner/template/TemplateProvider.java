package com.pslcl.qa.runner.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.qa.runner.ArtifactNotFoundException;
import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.config.status.ResourceStatus;
import com.pslcl.qa.runner.config.status.ResourceStatusListener;
import com.pslcl.qa.runner.process.DBTemplate;
import com.pslcl.qa.runner.resource.ReservedResource;
import com.pslcl.qa.runner.resource.ResourceDescription;
import com.pslcl.qa.runner.resource.ResourceQueryResult;
import com.pslcl.qa.runner.resource.exception.IncompatibleResourceException;
import com.pslcl.qa.runner.resource.exception.ResourceNotReservedException;
import com.pslcl.qa.runner.resource.instance.MachineInstance;
import com.pslcl.qa.runner.resource.instance.NetworkInstance;
import com.pslcl.qa.runner.resource.instance.PersonInstance;
import com.pslcl.qa.runner.resource.instance.ResourceInstance;

public class TemplateProvider implements ResourceStatusListener {
    
    private final Map<byte[],InstancedTemplate> availableInstancedTemplates; // note: this populates in the destroy method
    private final ResourceProviders resourceProviders;
    
    public TemplateProvider() 
    {
        availableInstancedTemplates = new HashMap<byte[],InstancedTemplate>();
        resourceProviders = new ResourceProviders();
    }

    public ResourceProviders getResourceProviders()
    {
        return resourceProviders;
    }
    
    public void init(RunnerServiceConfig config) throws Exception
    {
        resourceProviders.init(config);
    }
    
    public void destroy() 
    {
        resourceProviders.destroy();
    }
    
    public void destroy(byte [] template_hash, InstancedTemplate iT) {
        // Can also choose to destroy iT. This is early impl with no smarts.
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
            List<ResourceDescription> reserveResourceRequests = stepsParser.computeResourceQuery(); // each element of returned list has its stepReference stored
            int stepsReference = reserveResourceRequests.size();
            int originalReserveResourceRequestsSize = stepsReference;
            
            // NOTE: A change is coming in the future. Steps will be organized in prioritized sets, each with a setID. The following code follows original rules.
            
            // reserve the resource specified by each bind step, with 360 second timeout for each reservation
            ResourceQueryResult rqr = resourceProviders.reserveIfAvailable(reserveResourceRequests, 360);
            if (rqr != null) {
                // analyze the success/failure of each reserved resource, one resource for each bind step
                List<ResourceDescription> invalidResources = rqr.getInvalidResources(); // list is not in order
                if (invalidResources!=null && !invalidResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + invalidResources.size() + " reports of invalid resource reserve requests");
                }

                List<ResourceDescription> unavailableResources = rqr.getUnavailableResources(); // list is not in order
                if (unavailableResources!=null && !unavailableResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + unavailableResources.size() + " reports of unavailable resources for the given reserve requests");
                }
                
                List<ResourceDescription> availableResources = rqr.getAvailableResources();
                if (availableResources!=null && !availableResources.isEmpty()) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + availableResources.size() + " reports of available resources for the given reserve requests");
                }

                List<ReservedResource> reservedResources = rqr.getReservedResources(); // list is not in order, but each element of returned list has its stepReference stored
                if (reservedResources!=null) {
                    System.out.println("TemplateProvider.getInstancedTemplate() finds " + reservedResources.size() + " successful reserve-resource reserve requests" + (reservedResources.size() <= 0 ? "" : "; they are now reserved"));
                }

                // Note: The size of reservedResources and its entries are the important thing. Coding is intended that any reservedResource has no entries in the other three lists.
                //       However, the ultimate rule is that an entry in the reservedResource list means that the resource is reserved, independent of what the other ResourceProviders may have placed in the alternate lists. 
                boolean synchOk = true;
                if (reservedResources.size() == originalReserveResourceRequestsSize) {
                    // bind all resources of reservedResources, and receive a ResourceInstance for each one
                    List<Future<? extends ResourceInstance>> resourceInstances;
                    try {
                        resourceInstances = resourceProviders.bind(reservedResources); // start multiple asynch binds
                        
                        // Wait for all the multiple asynch bind calls to complete 
                        // resourceInstances is a Future list that is returned quickly; i.e. without waiting for all the asynch binds to complete.
                        // Loop, waiting for each encountered asynch .bind() to complete its work; in other words convert our .bind(multiple) call to synchronous.
                        // Convert the Future list to a list of returned ResourceInstance S.
                        List<ResourceInstance> listRI = new ArrayList<>();
                        int resourceCount = resourceInstances.size();
                        while(resourceCount > 0)
                        {
                            for (int i=0; i < resourceCount; i++) 
                            {
                                Future<? extends ResourceInstance> future = resourceInstances.get(i);
                                if(future.isDone())
                                {
                                    try 
                                    {
                                        ResourceInstance resourceInstance = future.get();
                                        listRI.add(resourceInstance);
                                        resourceInstances.remove(i);
                                        break;  // just start the loop over as the size/order just changed
                                    } 
                                    catch (ExecutionException ee) 
                                    {
                                        Throwable t = ee.getCause();
                                        String msg = ee.getLocalizedMessage();
                                        if(t != null)
                                            msg = t.getLocalizedMessage();
                                        LoggerFactory.getLogger(getClass()).
                                            info(ResourceInstance.class.getSimpleName() + " failed to startup: " + msg, ee);
                                        synchOk = false;
                                        break;
                                    }
                                    catch (InterruptedException ie) 
                                    {
                                        LoggerFactory.getLogger(getClass()).info("Executor pool shutdown");
                                        synchOk = false;
                                        break;
                                    }
                                }
                            }
                            if(!synchOk)
                            {
                                /*
                                 * Note: ResourceInstance implementations need to know the cancel policy we
                                 * are using.  We will use a do cancel if running policy.  If they are the
                                 * one failing, they should clean their self up before throwing the exception.
                                 * If they are canceled out of their run/call method they need to clean
                                 * their self up as well.
                                 */
                                for(Future<? extends ResourceInstance> future :resourceInstances)
                                    future.cancel(true);
                                break;
                            }
                            resourceCount = resourceInstances.size(); 
                        }// while loop
                        if(!synchOk)
                        {
                            /*
                             * Outstanding futures have been canceled and should clean themselves up.
                             * This leaves any that completed and were "got" before the failure
                             * that needs cleaned up here.
                             */
                            Logger log = LoggerFactory.getLogger(getClass()); 
                            for(ResourceInstance resource : listRI)
                                resource.getResourceProvider().release(resource, true);
                            log.error("FIXME what to do now, return?");
                        }
                            
                        // all bind calls are now completed
                        
                        // TODO: analyze the success/failure of each returned ResourceInstance
                        //   for example: it is an error that
                        
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
                    } catch (ResourceNotReservedException e) {
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
                            // This next line takes advantage of the orderedResourceInfos list held by iT. Using index value stepRef, this call returns the proper ResourceInfo that holds the correct machine to deploy to.
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
                        // machineRef strInstructionsHash ArtifactInfo (strArtifactName strArtifactHash); October note: multiple ArtifactInfo are allowed to follow
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

    @Override
    public void resourceStatusChanged(ResourceStatus status)
    {
    }
}