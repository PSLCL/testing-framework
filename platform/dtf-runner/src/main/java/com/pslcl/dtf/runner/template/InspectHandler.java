package com.pslcl.dtf.runner.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.resource.instance.PersonInstance;
import com.pslcl.dtf.core.runner.resource.instance.ResourceInstance;
import com.pslcl.dtf.core.runner.resource.provider.ResourceProvider;
import com.pslcl.dtf.runner.QAPortalAccess;
import com.pslcl.dtf.runner.process.RunnerMachine;


/**
 *
 *
 */
public class InspectHandler {
    static String inspectWorkDir = new String("inspectTemp");
    static String tempArtifactDir = new String(inspectWorkDir + "/" + "tempArtifactDirectory");
    static String archiveFilename = new String("attachments.tar.gzip"); // hard coded per design docs for PersonInstance
    static String archiveTopDirectory = new String("attachments");    // implied by design docs; impl chooses "attachments" anyway, when archiveTopDirectory is an empty string

    private final InstancedTemplate iT;
    private final RunnerMachine runnerMachine;
    private final List<String> setSteps;

    private boolean qapaResponseLaunched;
    private QAPaResponse qapaResponse; // as a flag, this starts out instantiated, but empty (with null member qaPortalResponse member)
    private Iterator<Map.Entry<String, String>> artifactEntriesIterator;
    private Entry<String, String> currArtifact;
    private List<InspectInfo> resultInspectInfos;
    private List<InspectInfo> inspectInfos = null;

    private File topWorkDir = null;
    private File reNumWorkDir = null;
    private File inspectInfoIndexWorkDir = null;

    private StepSetOffsets stepSetOffsets;
    private int indexNextInspectInfo = 0;
    private boolean done;
    private final Logger log;
    private final String simpleName;

    /**
     * Constructor: Identify consecutive inspect steps in a set of steps
     * @param iT The InstancedTemplate
     * @param runnerMachine The RunnerMachine
     * @param setSteps List of steps in the step set
     * @param initialSetStepCount The offset of the first inspect step in the steps of @param setSteps
     */
    InspectHandler(InstancedTemplate iT, RunnerMachine runnerMachine, List<String> setSteps, int initialSetStepCount) {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
        this.iT = iT;
        this.runnerMachine = runnerMachine;
        this.setSteps = setSteps;
        this.qapaResponseLaunched = false;
        this.qapaResponse = new QAPaResponse();
        this.artifactEntriesIterator = null;
        this.currArtifact = null;
        this.resultInspectInfos = new ArrayList<InspectInfo>();
        this.done = false;
        this.stepSetOffsets = new StepSetOffsets("inspect", setSteps, initialSetStepCount);
    }

    /**
     *
     * @param qapaResponse The QAPAResponse
     */
    public void setQAPaResponse(QAPaResponse qapaResponse) {
        this.qapaResponse = qapaResponse;
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
     * @return The RunnerMachine
     */
    public RunnerMachine getRunnerMachine() {
        return this.runnerMachine;
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
        int beginSetOffset = this.stepSetOffsets.getBeginSetOffset();
        if (beginSetOffset >= 0) {
            for (int i=beginSetOffset; i<=this.stepSetOffsets.getFinalSetOffset(); i++) {
                try {
                    ResourceInstance resourceInstance = null;
                    String strInstructionsHash = null;
                    Map<String, String> artifacts = new LinkedHashMap<>(); // iterates in the order in which entries are put in the map; HashMap makes no guarantee about iteration order

                    String inspectStep = setSteps.get(i);
                    SetStep parsedSetStep = new SetStep(inspectStep); // setID inspect 0-based-person-ref instructionsHash strArtifactName strArtifactHash [strArtifactName strArtifactHash] ...
                                                                      // 8 inspect 0 A4E1FBEBC0F8EF188E444F4C62A1265E1CCACAD5E0B826581A5F1E4FA5FE919C
                    log.debug(simpleName + "computeInspectRequests() finds inspect in stepSet " + parsedSetStep.getSetID() + ": " + inspectStep);
                    int parsedSetStepParameterCount = parsedSetStep.getParameterCount();
                    if (parsedSetStepParameterCount < 2) // after setID and command, must have at least 0-based-person-ref and instructionsHash; each couplet thereafter adds 2: "strArtifactName strArtifactHash"
                        throw new IllegalArgumentException("inspect step did not specify all needed person reference, instructionsHash, artifact name, and artifact hash");
                    if (parsedSetStepParameterCount%2 != 0) // odd parameter count means a strArtifactName is missing its associated strArtifactHash
                        throw new IllegalArgumentException("InspectHandler.computeInspectRequests() finds its final artifact name parameter is missing its required artifact hash paramenter");

                    String strPersonReference = parsedSetStep.getParameter(0);
                    resourceInstance = iT.getResourceInstance(strPersonReference);
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
                        if (artifacts.isEmpty())
                            artifacts = null; // flag further processing that we will not supply artifacts to the Person.inspect() call
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
        if (this.qapaResponse == null)
            return;

        try {
            QAPortalAccess qapa = this.iT.getQAPortalAccess();

            // establish working directory for forming (possibly parallel) collections of files to inspect
            if (this.topWorkDir == null) {
                this.topWorkDir = new File(InspectHandler.inspectWorkDir);
                this.topWorkDir.mkdirs(); // does not blow away existing content, which is work from other test runs
            }

            while (!done) {
                if (this.resultInspectInfos.isEmpty())
                {
                    if (this.indexNextInspectInfo < this.inspectInfos.size()) {
                        // The pattern is that this first work, accomplished at the first .proceed() call, must not block. We return before performing any blocking work, knowing that .proceed() will be called again.
                        while (this.indexNextInspectInfo < this.inspectInfos.size()) {
                            // for this one inspectInfo . . .
                            InspectInfo inspectInfo = this.inspectInfos.get(this.indexNextInspectInfo);

                            if (inspectInfo.getInstructions() == null) {
                                if (!this.qapaResponseLaunched) {
                                    // request instructions
                                    String instructionsHash = inspectInfo.getInstructionsHash();
                                    this.qapaResponse = null; // arm QA Portal response discovery flag
                                    qapa.launchReadContent(this, instructionsHash);
                                    this.qapaResponseLaunched = true;
                                    return; // allow http return time; fulfill the pattern that this first work, accomplished at the first .proceed() call, returns before performing any work that blocks
                                } else {
                                    // accept instructions
                                    this.qapaResponseLaunched = false;
                                    // this.qapaResponse is guaranteed not null, but exception out if it is not filled
                                    if (!this.qapaResponse.isFilled()) {
                                        log.warn(this.simpleName + "proceed() finds empty qapaResponse");
                                        throw new Exception("QAPortal access failure");
                                    }
                                    String instruction = this.qapaResponse.getContentAsString();
                                    inspectInfo.setInstruction(instruction);
                                    continue;
                                }
                            } else if (inspectInfo.getContentStream()==null && // need to fill this
                                       inspectInfo.getArtifacts()!=null) {     // unless we don't have artifacts to give to the Person.inspect() call
                                if (this.artifactEntriesIterator == null) {
                                    Map<String, String> artifacts = inspectInfo.getArtifacts();
                                    this.artifactEntriesIterator = artifacts.entrySet().iterator();
                                }
                                if (this.currArtifact==null && this.artifactEntriesIterator.hasNext())
                                        this.currArtifact = this.artifactEntriesIterator.next();

                                if (this.reNumWorkDir == null) {
                                    this.reNumWorkDir = new File(this.topWorkDir.getAbsolutePath() + File.separator + "reNum"+ this.iT.getRunID());
                                    FileUtils.deleteDirectory(this.reNumWorkDir); // whether directory is present, or not, this operates without exception
                                    this.reNumWorkDir.mkdirs();
                                }
                                if (this.inspectInfoIndexWorkDir == null) {
                                    this.inspectInfoIndexWorkDir = new File(this.reNumWorkDir.getAbsolutePath() + File.separator + "iii"+ this.indexNextInspectInfo);
                                    this.inspectInfoIndexWorkDir.mkdirs();
                                }

                                if (this.currArtifact != null) {
                                    if (!this.qapaResponseLaunched) {
                                        // request file content of currArtifact
                                        String artifactContentHash = currArtifact.getValue();
                                        this.qapaResponse = null; // arm QA Portal response discovery flag
                                        qapa.launchReadContent(this, artifactContentHash);
                                        this.qapaResponseLaunched = true;
                                        return; // allow http return time
                                    } else {
                                        // accept file content of currArtifact
                                        this.qapaResponseLaunched = false;
                                        // this.qapaResponse is guaranteed not null, but exception out if it is not filled
                                        if (!this.qapaResponse.isFilled()) {
                                            log.warn(this.simpleName + "proceed() finds empty qapaResponse");
                                            throw new Exception("QAPortal access failure");
                                        }
                                        InputStream streamContent = this.qapaResponse.getContentAsStream();
                                        String contentFilename = currArtifact.getKey();

                                        // establish directories stipulated in contentFilename
                                        int finalBackSlash = contentFilename.lastIndexOf('\\');
                                        int finalForwardSlash = contentFilename.lastIndexOf('/');
                                        int finalSlash = (finalBackSlash > finalForwardSlash) ? finalBackSlash : finalForwardSlash;
                                        if (finalSlash >= 0) {
                                            String dirString = contentFilename.substring(0, finalSlash);
                                            File destPath = new File((this.inspectInfoIndexWorkDir.getAbsolutePath() + File.separator), dirString); // appends a path of directories
                                            destPath.mkdirs(); // does not blow away existing content in these directories
                                        }

                                        // copy actual content to the content file
                                        File contentFile = new File(contentFilename); // empty File
                                        Path dest = Paths.get(this.inspectInfoIndexWorkDir + File.separator + contentFile.getPath());
                                        // It should never happen that a file is copied over a file of the same filename, because:
                                        //     first, the tempArtifactDirectory always starts empty, and second, duplicated filenames are not reflected in inspectInfo.artifacts.
                                        Files.copy(streamContent, dest/*, StandardCopyOption.REPLACE_EXISTING*/); // On duplicate filename, we want the exception. We could place .REPLACE_EXISTING, to avoid throwing that exception.

                                        this.currArtifact = null;
                                        continue;
                                    }
                                } else {
                                    // create a tarGz of .archiveFilename, specified with its full path; the output file has filename .archiveFilename, but is placed at the fully specified path
                                    // fill the tarGz file with the directory structure found under this.inspectInfoIndexWorkDir
                                    String tarGzipFullPathFilename = this.reNumWorkDir + File.separator + InspectHandler.archiveFilename;
                                    ToTarGz toTarGz = new ToTarGz(tarGzipFullPathFilename, this.inspectInfoIndexWorkDir.getAbsolutePath());
                                    File fileTarGz = toTarGz.CreateTarGz();
                                    // fileTarGz (attachments.tar.gzip) is placed and filled, using GzipCompressorOutputStream and TarArchiveOutputStream

                                    // setContentStream
                                    FileInputStream fis = toTarGz.getFileInputStream(fileTarGz);
                                    inspectInfo.setContentStream(fis);
                                }
                            }
                            ++this.indexNextInspectInfo;
                            this.artifactEntriesIterator = null;
                        } // end while(inspectInfo available)
                    } else {
                        // call the full set of (async) PersonInstance.inspect() method calls
                        for (InspectInfo inspectInfo : this.inspectInfos) {
                            ResourceInstance resourceInstance = inspectInfo.getResourceInstance();
                            // We know that resourceInstance is a PersonInstance, because an inspect step must always direct its work to a PersonInstance.
                            //     Still, check that condition to avoid problems that arise when template steps are improper.
                            if (!PersonInstance.class.isAssignableFrom(resourceInstance.getClass()))
                                throw new Exception("Specified inspect target is not a PersonInstance");
                            PersonInstance pi = PersonInstance.class.cast(resourceInstance);

                            // for this one inspectInfo, initiate person.inspect() and store the returned future
                            Future<? extends Void> future = pi.inspect(inspectInfo.getInstructions(),
                                                                       inspectInfo.getContentStream(), // null only if inspectInfo.getArtifacts() is null
                                                                       InspectHandler.archiveFilename);
                            inspectInfo.setInspectFuture(future);
                            this.resultInspectInfos.add(inspectInfo);
                        } // end for(submit futures)
                        // List resultInspectInfos is now full
                        // Each list element:
                        //     can be a null (inspect failed while in the act of creating a Future), or
                        //     can be a Future<Void>, for which future.get():
                        //        returns a Void on inspect success, or
                        //        throws an exception on inspect failure

                        return; // allow time for futures to resolve
                    }
                } // end if(resultInspectInfos.isEmpty())
                  else {
                    // complete work by blocking until all the futures complete
                    this.waitComplete();
                    this.done = true;
                }
            } // end while(!done)
        } catch (Exception e) {
            this.done = true;
            throw e;
        }
    }

    /**
     * thread blocks
     *
     * @throws Exception on any error
     */
    public void waitComplete() throws Exception {
        // At this moment, this.resultInspectInfos is filled. It's Future's each give us a Void.
        //     Although this code receives Futures, it does not cancel them or test their characteristics (.isDone(), .isCanceled()).

        // Note: As an initial working step, this block is coded to a safe algorithm:
        //       We expect full inspect success, and otherwise we back out and release whatever other activity had been put in place, along the way.

        // Gather all results from this.resultInspectInfos, a list of inspectInfo, each holding a future. This thread yields, in waiting for each of the multiple asynch inspect calls to complete.
        boolean allInspects = true;
        try {
            for (InspectInfo inspectInfo : this.resultInspectInfos) {
                Future<? extends Void> future = inspectInfo.getInspectFuture();
                if (future != null) {
                    try {
                        future.get(); // blocks until asynch answer comes, or exception, or timeout
                    } catch (InterruptedException | ExecutionException ioreE) {
                        Throwable t = ioreE.getCause();
                        String msg = ioreE.getLocalizedMessage();
                        if(t != null)
                            msg = t.getLocalizedMessage();
                        log.debug(simpleName + "waitComplete(), inspect failed future.get() with computed msg: " + msg + "; original msg: " + ioreE.getMessage());
                        allInspects = false;
                        // stay in loop to gather other futures
                    }
                } else {
                    allInspects = false;
                    log.debug(simpleName + "waitComplete(), one inspect failed- future returned as null");
                    // stay in loop to gather other futures
                }
                InputStream contentStream = inspectInfo.getContentStream();
                if (contentStream != null)
                    contentStream.close(); // cleanup original InputStream, regardless of our success or failure
            }
        } catch (Exception e) {
            throw e;
        }

        if (!allInspects) {
            if (false) // true: temporarily for test
                this.cleanup();
            throw new Exception("InspectHandler.waitComplete() finds one or more inspect steps failed");
        }
    }

    void cleanup() {
        try {
            FileUtils.deleteDirectory(this.reNumWorkDir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}