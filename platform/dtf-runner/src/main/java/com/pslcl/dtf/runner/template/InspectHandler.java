package com.pslcl.dtf.runner.template;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.runner.QAPortalAccess;

public class InspectHandler {
    static String tempArtifactDirectory = new String("tempArtifactDirectory");
    static String archiveFilename = new String("attachments.tar.gz"); // hard coded per design docs for PersonInstance
    static String archiveTopDirectory = new String("attachments");    // implied by design docs; impl chooses "attachments" anyway, when archiveTopDirectory is an empty string

    private final InstancedTemplate iT;
    private final List<String> setSteps;
    private List<InspectInfo> inspectInfos = null;
    private List<Future<? extends Void>> futuresOfInspects;
    private int iBeginSetOffset = -1;
    private int iFinalSetOffset = -1; // always non-negative when iBegin... becomes non-negative; never less than iBegin
    private int indexNextInspectInfo = 0;
    private boolean done;
    private final Logger log;
    private final String simpleName;

    /**
     * Constructor: Identify consecutive inspect steps in a set of steps
     * @param iT
     * @param setSteps
     */
    InspectHandler(InstancedTemplate iT, List<String> setSteps) throws NumberFormatException {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.iT = iT;
        this.setSteps = setSteps;
        this.futuresOfInspects = new ArrayList<Future<? extends Void>>();
        this.done = false;

		int iTempFinalSetOffset = 0;
		int iSetOffset = 0;
		while (true) {
			SetStep setStep = new SetStep(setSteps.get(iSetOffset));
			if (!setStep.getCommand().equals("inspect"))
				break;
			this.iBeginSetOffset = 0;
			this.iFinalSetOffset = iTempFinalSetOffset;
			if (++iTempFinalSetOffset >= setSteps.size())
				break;
			iSetOffset = iTempFinalSetOffset; // there is another step in this set
		}
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    int getInspectRequestCount() throws Exception {
        if (this.inspectInfos != null) {
            int retCount = this.inspectInfos.size();
            if (retCount > 0)
                return retCount;
        }
        throw new Exception("InspectHandler unexpectedly finds no inpect requests");
    }

    /**
     * 
     * @return
     */
    boolean isDone() {
        return done;
    }

    int computeInspectRequests() throws Exception { // setID inspect 0-based-person-ref instructionsHash [strArtifactName strArtifactHash] ...
        this.inspectInfos = new ArrayList<>();
        if (this.iBeginSetOffset != -1) {
            for (int i=this.iBeginSetOffset; i<=this.iFinalSetOffset; i++) {
                try {
                    ResourceInstance resourceInstance = null;
                    String strInstructionsHash = null;
                    Map<String, String> artifacts = new HashMap<>();
                    
                    String inspectStep = setSteps.get(i);
                    SetStep parsedSetStep = new SetStep(inspectStep); // setID inspect 0-based-person-ref instructionsHash strArtifactName strArtifactHash [strArtifactName strArtifactHash] ...
                                                                      // 8 inspect 0 A4E1FBEBC0F8EF188E444F4C62A1265E1CCACAD5E0B826581A5F1E4FA5FE919C
                    log.debug(simpleName + "computeInspectRequests() finds inspect in stepSet " + parsedSetStep.getSetID() + ": " + inspectStep);
                    int parsedSetStepParameterCount = parsedSetStep.getParameterCount();
                    if (parsedSetStepParameterCount < 4) // after setID and command, must have 0-based-person-ref, instructionsHash, and at least this couple "strArtifactName strArtifactHash", each of which adds 2 to parameter count
                        throw new IllegalArgumentException("inspect step did not specify all needed person reference, instructionsHash, artifact name, and artifact hash");
                    if (parsedSetStepParameterCount%2 != 0) { // odd parameter count means a strArtifactName is missing its associated strArtifactHash
                        throw new IllegalArgumentException("InspectHandler.computeInspectRequests() finds its final artifact name parameter is missing its required artifact hash paramenter");
                    }
                	
                    String strPersonReference = parsedSetStep.getParameter(0);
                    int personReference = Integer.valueOf(strPersonReference).intValue();
                    resourceInstance = iT.getResourceInstance(personReference);
                    if (resourceInstance != null)
                    {
                        // Note: In bind handling (that came before), we haven't had an indication as to what this resourceInstance would be used for, and we haven't been able to know its type (Machine vs. Person vs. Network).
                        //       Now that we know it is used for inspect, check resourceInstance for required type: person
                        // riRP: resourceInstanceResourceProvider, which has self-knowledge of resource-provider type
                        ResourceProvider riRP = resourceInstance.getResourceProvider();
                        String resourceType = ResourceProvider.getTypeName(riRP);
                        if (resourceType==null || resourceType!=ResourceProvider.PersonName)
                            throw new Exception("InspectHandler processing asked to deploy to non 'person' resource");
                        strInstructionsHash = parsedSetStep.getParameter(1);
                        for (int j=2; j<parsedSetStepParameterCount; j+=2) {
                        	// for the case where duplicate filenames are encountered (an error in the template step), this .put() will not object, i.e. will not throw an exception
                            artifacts.put(parsedSetStep.getParameter(j),    // artifact filename
                                          parsedSetStep.getParameter(j+1)); // artifact hash
                        }
                        this.inspectInfos.add(new InspectInfo(resourceInstance, strInstructionsHash, artifacts));
                    } else {
                        throw new Exception("InspectHandler.computeInspectRequests() finds null bound ResourceInstance at reference " + strPersonReference);
                    }
                } catch (Exception e) {
                    log.debug(simpleName + "inspect step processing error, msg: " + e);
                    this.done = true;
                    throw e;
                }
            }
        }
        return this.inspectInfos.size();
    }

    /**
     * Proceed to obtain inspect instructions and files to inspect, then issue inspect command(s), then return. Set done when inspects complete or error out.
     *
     * @throws Exception
     */
    void proceed() throws Exception {
        if (this.inspectInfos==null || this.inspectInfos.isEmpty()) {
        	this.done = true;
            throw new Exception("InspectHandler processing has no inspectInfo");
        }

        try {
            QAPortalAccess qapa = this.iT.getQAPortalAccess();
            while (!done) {
            	if (futuresOfInspects.isEmpty()) {
            		if (this.indexNextInspectInfo < this.inspectInfos.size()) {
            			// The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
            			while (this.indexNextInspectInfo < this.inspectInfos.size()) {
                            // place new empty temp directory for this particular inspectInfo, with delete if needed, to hold all specified artifacts to inspect
                            File fileTempArtifactDirectory = new File(InspectHandler.tempArtifactDirectory);
                            FileUtils.deleteDirectory(fileTempArtifactDirectory); // whether directory is present, or not, this operates without exception
                            fileTempArtifactDirectory.mkdirs();
                            // REVIEW: How to place code here that will yield until proven that the directory at .tempArtifactDirectory is actually in place. Have not seen the problem, but it could happen that code will write to disk before the disk is fully set up.
                            
                            // TODO: Add async behavior here. It is setup for that, to involve futures in the two QA Portal data accesses.
                            
                			// for this one inspectInfo, get inspect information from QA Portal web server
                            InspectInfo inspectInfo = this.inspectInfos.get(this.indexNextInspectInfo);
                            
                            // obtain instructions for this.inspectInfo
                            String instructionsHash = inspectInfo.getInstructionsHash();
                            String instructions = qapa.getContentAsString(instructionsHash); // TODO: needs to be asynchronous
                            inspectInfo.setInstruction(instructions);

                            // into local temp directory, place n filename/contents and place
                            for (Entry<String, String> artifact: inspectInfo.getArtifacts().entrySet()) {
                                String contentFilename = artifact.getKey();
                                File contentFile = new File(contentFilename); // empty File

                                // TODO: asynch, gather in .waitComplete()
                                String contentHash = artifact.getValue();
                                InputStream streamContent = qapa.getContentAsStream(contentHash);
                                Path dest = Paths.get(fileTempArtifactDirectory.getPath() + File.separator + contentFile.getPath());
                                // It should never happen that a file is copied over a file of the same filenmaem, bbecause:
                                //     first, the tempArtifactDirectory always starts empty, and second, duplicated filenames are not reflected in inspectInfo.artifacts.   
                                Files.copy(streamContent, dest/*, StandardCopyOption.REPLACE_EXISTING*/); // On duplicate filename, we want the exception. We could place .REPLACE_EXISTING, to avoid throwing that exception.
                            }

                            // from local temp directory, place the tarGz file in local temp directory
                            ToTarGz toTarGz = new ToTarGz(InspectHandler.archiveFilename, fileTempArtifactDirectory.getName());
                            toTarGz.CreateTarGz();
                            TarArchiveInputStream tais = toTarGz.getTarArchiveInputStream();
                            inspectInfo.setContentStream(tais);

                            if (false) { // true: temporarily, which consumes tais and makes it useless to send to the inspect call, as a test, use tais to write a decoded file structure to disk
                            	// from our actual TarArchiveInputStream
                            	TarArchiveInputStream testTais = (inspectInfo.getContentStream());
                            	toTarGz.writeFileStructure(testTais);
                            } else if (false) { // true: temporarily, which consumes tais and makes it useless to send to the inspect call, as a test, use tais to write a decoded file structure to disk
                                // from an InputStream; note: this path does not prove anything yet, need to create InputStream from scratch
                            	InputStream is = InputStream.class.cast(inspectInfo.getContentStream()); // upcasting
//                            	InputStream is = (InputStream)(inspectInfo.getContentStream());          // also upcasting, same behavior
                            	toTarGz.writeFileStructure(is, 0);
                            }
                            
                            ++this.indexNextInspectInfo;
                			return;	// Fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks. 
            			} // end while()
            		} else {
            			// call the full set of (async) PersonInstance.inspect() method calls
            			for (InspectInfo inspectInfo : this.inspectInfos) {
                            ResourceInstance resourceInstance = inspectInfo.getResourceInstance();
                            // We know that resourceInstance is a PersonInstance, because an inspect step must always direct its work to a PersonInstance.
                            //     Still, check that condition to avoid problems that arise when template steps are improper.
                            if (!PersonInstance.class.isAssignableFrom(resourceInstance.getClass()))
                                throw new Exception("Specified inspect target is not a PersonInstance");
                            PersonInstance pi = PersonInstance.class.cast(resourceInstance);
            				
                        	// initiate person.inspect() for this one inspectInfo; fills futuresOfInspects
                            Future<? extends Void> future = pi.inspect(inspectInfo.getInstructions(), inspectInfo.getContentStream(), InspectHandler.archiveFilename);
                            futuresOfInspects.add(future);
                            // TODO: close this Stream when the Future<Void> comes back; will have to track stream is against each future
            			}
                        // List futuresOfInspects is now full
                        // Each list element:
                        //     can be a null (inspect failed while in the act of creating a Future), or
                        //     can be a Future<Void>, for which future.get():
                        //        returns a Void on inspect success, or
                        //        throws an exception on inspect failure
            			
            			return; // allow time for futures to resolve
            		}
            	} // end if(futuresOfInspects.isEmpty())
            	  else {
                    // complete work by blocking until all the futures complete
                    this.waitComplete();
                    this.done = true;
            	}
            } // end while()
        } catch (Exception e) {
            this.done = true;
            throw e;
        }
    }
    
    /**
     * thread blocks
     */
    public void waitComplete() throws Exception {
        // At this moment, this.futuresOfInspects is filled. It's Future's each give us a Void.
        //     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full inspect success, and otherwise we back out and release whatever other activity had been put in place, along the way.

        // Gather all results from futuresOfInspects, a list of Futures. This thread yields, in waiting for each of the multiple asynch inspect calls to complete.
        boolean allInspects = true;
        for (Future<? extends Void> future : this.futuresOfInspects) {
            if (future != null) {
                try {
                    future.get(); // blocks until asynch answer comes, or exception, or timeout
                } catch (InterruptedException ee) {
                    Throwable t = ee.getCause();
                    String msg = ee.getLocalizedMessage();
                    if(t != null)
                        msg = t.getLocalizedMessage();
                    log.debug(simpleName + "waitComplete(), inspect failed: " + msg, ee);
                    allInspects = false;
                } catch (ExecutionException e) {
                    log.info(simpleName + "Executor pool shutdown"); // TODO: need new msg
                    allInspects = false;
                }
            } else {
                allInspects = false;
            }
        }

        if (!allInspects) {
            throw new Exception("InspectHandler.waitComplete() finds one or more inspect steps failed");
        }
    }

}