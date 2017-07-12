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
package com.pslcl.dtf.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.artifact.ArtifactProvider;
import com.pslcl.dtf.core.artifact.ArtifactProvider.ModuleNotifier;
import com.pslcl.dtf.core.artifact.Content;
import com.pslcl.dtf.core.artifact.Module;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.resource.Attributes;
import com.pslcl.dtf.core.generator.resource.Machine;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.runner.messageQueue.SQSTestPublisher;

public class DistributedTestingFramework
{
    private static void generalHelp()
    {
        System.out.println("test-platform General Help");
        System.out.println("[program] --help (this output)");
        System.out.println("[program] command --help (help on command)");
        System.out.println("[program] command <arguments> (run command with arguments)");
        System.out.println("commands are:");
        System.out.println("  synchronize - synchronize the artifact providers with the database.");
        System.out.println("  result - add a result to the database.");
        System.out.println("  run - extract tests from the database and run them.");
        System.out.println("  populate - populate the system with made-up testing data.");
        System.exit(1);
    }

    private static void synchronizeHelp()
    {
        System.out.println("test-platform Synchronize Help");
        System.out.println("This command synchronizes artifact providers with the database and runs generators.");
        System.out.println("[program] synchronize --help (this output)");
        System.out.println("[program] synchronize <arguments>");
        System.out.println("arguments are:");
        System.out.println("  --no-synchronize - optional - disable synchronization.");
        System.out.println("  --no-generators - optional - disable running generators.");
        System.out.println("  --prune <count> - optional - enable deleting of missing modules.");
        System.out.println("               <count> is the number of synchronize runs that the module has been missing. ");
        System.out.println("                       Count must be greater than 0.");
        System.out.println("  --generator-process-count <count> - optional - set the number of generator processes that may execute in parallel.");
        System.out.println("               Count must be greater than 0. Defaults to 5.");

        System.exit(1);
    }

    private static void runHelp()
    {
        System.out.println("test-platform Run Help");
        System.out.println("This command extracts tests from the database and runs them.");
        System.out.println("[program] run --help (this output)");
        System.out.println("[program] run <arguments>");
        System.out.println("arguments are:");
        System.out.println("  <runcount> - required unless --test or --test-instance are specified - the number of tests to run.");
        System.out.println("  --test i - optional, to run one test on all test instances soon - supply the id number assigned for the test.");
        System.out.println("  --test-instance j - optional, to run one test on one test instance soon - supply the id number assigned to the test instance.");
        System.out.println("  --owner - optional, to specify the owner for all test runs started by this command.");

        System.exit(1);
    }

    private static void resultHelp()
    {
        System.out.println("test-platform Result Help");
        System.out.println("This command sets a result for a given component, version, and test instance.");
        System.out.println("[program] result --help (this output)");
        System.out.println("[program] result <arguments> <result>");
        System.out.println("  <result> - either 'pass' or 'fail'");
        System.out.println("arguments are:");
        System.out.println("  --hash <hash> - required unless --run is specified - the template(identifed by its hash) to apply the result to. This argument creates a new test run.");
        System.out.println("  --run <run-id> - required unless --hash is specified - the test run to apply the result to.");
        System.exit(1);
    }

    private static void populateHelp()
    {
        System.out.println("test-platform Populate Help");
        System.out.println("This command populates the database with made-up testing data.");
        System.out.println("[program] populate --help (this output)");
        System.out.println("[program] populate <arguments>");
        System.out.println("arguments are:");
        System.out.println("  --plans <num> - optional, specifies how many test plans to populate.");
        System.out.println("  --tests <num> - optional, specifies how many tests to populate in each test plan (requires --plans).");
        System.out.println("  --instances <num> - optional, specifies how many test instances to populate for each test (requires --tests).");
        System.out.println("  --orgs <num> - optional, specifies how many organizations to populate.");
        System.out.println("  --modules <num> - optional, specifies how many modules to populate for each organization (requires --orgs).");
        System.out.println("  --versions <num> - optional, specifies how many versions to populate for each module (requires -modules).");
        System.out.println("  --artifacts <num> - optional, specifies how many artifacts to populate in each module (requires --modules).");
        System.out.println("  --owner <email> - optional, specifies that some pending tests should be assigned to this owner (requires --instances).");
        System.out.println("  --results - optional, specifies whether or not to populate results (requires --instances).");
        System.out.println("  --start - optional, specifies the date/time to begin creating results at (requires --results).");
        System.out.println("  --end - optional, specifies the date/time to end creating results at (requires --results).");
        System.out.println("When creating modules, the populator will create organizations, names, and versions. A new organization");
        System.out.println("is created for each 10 modules, a new name for each 5.");
        System.out.println("When creating instances, each will get progressively more complex and use different combinations of modules");
        System.out.println("and artifacts.");
        System.out.println("If results are generated and begin and end times are not specified then the results will be created uniformly");
        System.out.println("during the previous week.");
        System.exit(1);
    }

    private static class HandleModule implements ArtifactProvider.ModuleNotifier
    {
        private static class DelayedModuleMergeAction
        {
            ArtifactProvider source;
            String merge;
            Module module;
        }

        private Core core;
        private List<DelayedModuleMergeAction> delayedModuleMergeAction = new ArrayList<DelayedModuleMergeAction>();

        public HandleModule(Core core)
        {
            this.core = core;
        }

        private void decompress(Hash hash, long pk_version, long pk_parent, String configuration, boolean merge_source, long pk_source_module)
        {
            TarArchiveInputStream ti = null;
            try
            {
                InputStream archive = new FileInputStream(core.getContentFile(hash));
                /* Uncompress and unarchive the file, creating entries for each artifact found inside. */
                InputStream is = new GzipCompressorInputStream(archive);
                ti = new TarArchiveInputStream(is);
                TarArchiveEntry entry;
                while ((entry = ti.getNextTarEntry()) != null)
                {
                    if (entry.isDirectory())
                        continue;

                    String artifact = entry.getName();
                    /* Standardize on no leading dot. */
                    if (artifact.startsWith("./"))
                        artifact = artifact.substring(2);

                    int mode = entry.getMode();
                    Hash h = core.addContent(ti, entry.getSize());
                    core.addArtifact(pk_version, configuration, artifact, mode, h, merge_source, pk_parent, pk_source_module);
                }
            } catch (Exception e)
            {
                System.err.println("ERROR: Failure extracting file, " + e.getMessage());
            } finally
            {
                if (ti != null)
                    try
                    {
                        ti.close();
                    } catch (IOException e)
                    {
                        // Ignore
                    }
            }
        }

        @Override
        public void module(ArtifactProvider source, Module module, String merge)
        {
            try
            {
                // Check to see if the module exists. If it does then return.
                // If it does not exist then add the module and iterate the artifacts.
                long pk_module = core.findModule(module);
                if (pk_module != 0)
                {
                    core.updateModule(pk_module); // clear module.missing_count
                    return;
                }

                // Determine if the module contains a test generator - this triggers deletion of prior stored modules of same version number.
                List<Artifact> artifacts = module.getArtifacts();
                boolean contains_generator = false;
                for (Artifact artifact : artifacts)
                {
                    if (artifact.getConfiguration().equals("dtf_test_generator"))
                    {
                        contains_generator = true;
                        break;
                    }
                }

                boolean merge_source = false;
                if (merge != null && merge.length() > 0)
                {
                    // this ALSO triggers deletion of prior stored modules of same version number
                    merge_source = true;
                    DelayedModuleMergeAction D = new DelayedModuleMergeAction();
                    D.source = source;
                    D.merge = merge;
                    D.module = module;
                    delayedModuleMergeAction.add(D);
                    // delayedModuleMergeAction list is used in .markMergeFromModule(), which is called after IvyArtifactProvider.iterateModules() completes
                }

                /* Generator and merged components are of "one only" build sequence number.
                 * This is done because they are not meant to be the modules that are tested,
                 * but rather modules that provide testing applications.
                 *
                 * Merging multiple sequence numbers of the same component would cause conflicts.
                 * The same is true for generators, which are extracted and executed.
                 * However, we do allow different versions of the component that will merge
                 * into different target modules. This means that we consider only modules
                 * where the build sequence number is different.
                 */
                // THIS CALL MUST HAPPEN BEFORE THE MODULE IS ADDED TO TABLS module, BELOW.
                // IT IS ASSUMED THAT THE MODULE'S BUILD SEQUENCE NUMBER IS LATER THAN ALL EXISTING.
                if (contains_generator || (merge != null && merge.length() > 0))
                    core.deletePriorBuildSequenceNumbers(module);

                pk_module = core.addModule(module);
                for (Artifact artifact : artifacts)
                {
                    Content content = artifact.getContent();
                    if (content != null)
                    {
                        InputStream is = content.asStream();
                        if (is != null)
                        {
                            Hash h = core.addContent(is, -1);
                            long pk_artifact = core.addArtifact(pk_module, artifact.getConfiguration(), artifact.getName(), artifact.getPosixMode(), h, merge_source, 0, 0);
                            if (artifact.getName().endsWith(".tar.gz"))
                            {
                                decompress(h, pk_module, pk_artifact, artifact.getConfiguration(), merge_source, 0);
                            }
                        } else {
                            System.out.println("DEBUG: .module() skipping one artifact having null InputStream");
                        }
                    } else {
                        System.out.println("ERROR: .module() skipping one artifact having null Content");
                    }
                }
            } catch (Exception e)
            {
                // TODO
            }
        }

        @Override
        public void finalize() // this overrides java's Object.finalize()
        {
            markMergeFromModule();
        }

        private void markMergeFromModule()
        {
            Iterable<Module> modules = core.createModuleSet();
            for (DelayedModuleMergeAction d : delayedModuleMergeAction)
            {
                Module dmod = d.module;
                long pk_source_module = core.findModule(dmod);

                for (Module m : modules)
                {
                    // Since the actual types may differ, we compare fields to determine if they are the same.
                    boolean same = true;
                    if (!m.getOrganization().equals(dmod.getOrganization()))
                        same = false;
                    if (!m.getName().equals(dmod.getName()))
                        same = false;
                    if (!m.getVersion().equals(dmod.getVersion()))
                        same = false;
                    if (!m.getSequence().equals(dmod.getSequence()))
                        same = false;

                    if (same)
                        continue;

                    if (d.source.merge(d.merge, dmod, m))
                    {
                        long pk_module = core.findModule(m);

                        List<Artifact> artifacts = dmod.getArtifacts();
                        for (Artifact artifact : artifacts)
                        {
                            InputStream is = artifact.getContent().asStream();
                            if (is != null)
                            {
                                Hash h = core.addContent(is, -1);
                                long pk_artifact = core.addArtifact(pk_module, artifact.getConfiguration(), artifact.getName(), artifact.getPosixMode(), h, false, 0, pk_source_module);
                                if (artifact.getName().endsWith(".tar.gz"))
                                {
                                    decompress(h, pk_module, pk_artifact, artifact.getConfiguration(), false, pk_source_module);
                                }
                            }
                        }
                    }
                }
            }
        }

    } // end class HandleModule

    private static void inheritIO(final InputStream src, final PrintStream dest, final PrintStream save)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine())
                {
                    String line = sc.nextLine();
                    if (dest != null)
                        dest.println(line);
                    if (save != null)
                        save.println(line);
                }

                sc.close();
            }
        }).start();
    }

    private static class GeneratorExecutor implements Runnable
    {
        private Core core;
        private File generators;
        private String base;
        private String shell;
        private long id;
        private String script;

        public GeneratorExecutor(Core core, File generators, String base, String shell, long id, String script)
        {
            this.core = core;
            this.generators = generators;
            this.base = base;
            this.shell = shell;
            this.id = id;
            this.script = script;
        }

        @Override
        public void run()
        {
            String tname = Thread.currentThread().getName();
            Thread.currentThread().setName("GeneratorFuture");
            System.out.println("Script: " + this.toString() + " started.");
            process();
            System.out.println("Script: " + this.toString() + " completed.");
            Thread.currentThread().setName(tname);
        }

        private void process()
        {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            try
            {
                String augmented_params = shell + " " + script;
                String[] params = augmented_params.split(" ");
                params[1] = generators.getAbsolutePath() + "/bin/" + params[1];
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command(params);
                processBuilder.environment().put("CLASSPATH", generators.getAbsolutePath() + "/lib/*" + File.pathSeparator + base + "/platform/lib/*" + File.pathSeparator + base + "/platform/*");
                processBuilder.environment().put("DTF_TEST_ID", Long.toString(id));

                Process run = processBuilder.start();
                inheritIO(run.getInputStream(), System.out, new PrintStream(stdout));
                inheritIO(run.getErrorStream(), System.err, new PrintStream(stderr));

                boolean running = true;
                while (running == true)
                {
                    try
                    {
                        run.exitValue();
                        running = false;
                    } catch (IllegalThreadStateException its)
                    {
                        try
                        {
                            Thread.sleep(250);
                        } catch (Exception ex)
                        {
                        }
                    }
                }

                core.updateTest(id, stdout.toString(), stderr.toString());
            } catch (Exception e)
            {
                String log = "ERROR: Could not run script '" + script + "', " + e;
                System.err.println(log);
                new PrintStream(stderr).println(log);
            } finally
            {
                core.updateTest(id, stdout.toString(), stderr.toString());
            }
        }

//      @Override
//      public String toString()
//      {
//          return Long.toString(id) + "/" + script;
//      }
    }

    private static Set<PosixFilePermission> toPosixPermissions(int mode)
    {
        Set<PosixFilePermission> permissions = new HashSet<>();
        if ((mode & 0b001_000) == 0b001_000)
        {
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((mode & 0b100_000) == 0b100_000)
        {
            permissions.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 0b010_000) == 0b010_000)
        {
            permissions.add(PosixFilePermission.GROUP_WRITE);
        }

        if ((mode & 0b001) == 0b001)
        {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        if ((mode & 0b100) == 0b100)
        {
            permissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 0b010) == 0b010)
        {
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        }

        if ((mode & 0b001_000_000) == 0b001_000_000)
        {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((mode & 0b100_000_000) == 0b100_000_000)
        {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        if ((mode & 0b010_000_000) == 0b010_000_000)
        {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }

        return permissions;
    }

    private static void synchronize(String[] args)
    {
        boolean synchronize = true;
        boolean generate = true;
        int prune = -1;
        int generatorProcessCount = 5;

        if (args.length > 1 && args[1].compareTo("--help") == 0)
            synchronizeHelp();

        for (int i = 1; i < args.length; i++)
        {
            if (args[i].compareTo("--no-synchronize") == 0)
            {
                synchronize = false;
            } else if (args[i].compareTo("--no-generators") == 0)
            {
                generate = false;
            } else if (args[i].compareTo("--prune") == 0)
            {
                if (i == args.length)
                    synchronizeHelp();

                prune = Integer.parseInt(args[i + 1]);
                if (prune <= 0)
                    synchronizeHelp();

                i += 1;
            } else if (args[i].compareTo("--generator-process-count") == 0){
                if (i == args.length)
                    synchronizeHelp();

                generatorProcessCount = Integer.parseInt(args[i + 1]);
                if (generatorProcessCount <= 0)
                    synchronizeHelp();

                i += 1;
            } else
                synchronizeHelp();
        }

        Core core = null;
//      ArtifactProvider artifactProvider = null;

        try
        {
            /* Instantiate the platform and artifact provider. */
            core = new Core(0);

            if (synchronize)
            {
                File generators = new File(core.getConfig().dirGenerators());
                if (generators.exists())
                    //noinspection ResultOfMethodCallIgnored
                    org.apache.commons.io.FileUtils.deleteQuietly(generators);
                //noinspection ResultOfMethodCallIgnored
                generators.mkdirs();
                //noinspection ResultOfMethodCallIgnored
                generators.setWritable(true);

                // Get the list of artifact providers from the database, prepare the modules table for updates.
                List<String> providers = core.readArtifactProviders();
                core.prepareToLoadModules();
                boolean noModuleErrors = true;

                HandleModule handler = new HandleModule(core);
                List<ArtifactProvider> to_close = new ArrayList<ArtifactProvider>();

                for (String providerName : providers)
                {
                    ArtifactProvider provider = null;
                    try
                    {
                        Class<?> P = Class.forName(providerName);
                        provider = (ArtifactProvider) P.newInstance();

                        provider.init(); // setup to follow config in ../portal/config/ivysettings.xml
                        to_close.add(provider);

                        // Identify artifacts from this artifact provider. Fill tables modules/artifacts/content.
                        provider.iterateModules(handler);
                    } catch (Exception e)
                    {
                        System.err.println(e.getMessage());
                        noModuleErrors = false;
                    }
                }

                handler.markMergeFromModule();

                for (ArtifactProvider p : to_close)
                {
                    p.close();
                }

                // Finalize module loading
                if (noModuleErrors && prune > 0)
                    core.finalizeLoadingModules(prune);

                // Extract all generators to new generator (configured) directory
                Iterable<Module> find_generators = core.createModuleSet();
                for (Module M : find_generators)
                {
                    List<Artifact> artifacts = M.getArtifacts(null, "dtf_test_generator");
                    for (Artifact A : artifacts)
                    {
                        File f = core.getContentFile(A.getContent().getHash());
                        File P = new File(generators, A.getName());
                        FileUtils.copyFile(f, P);
                        try
                        {
                            Files.setPosixFilePermissions(P.toPath(), toPosixPermissions(A.getPosixMode()));
                        } catch (UnsupportedOperationException e)
                        {
                            // Windows does not support setPosixFilePermissions. Fall back.
                            Set<PosixFilePermission> perms = toPosixPermissions(A.getPosixMode());

                            P.setExecutable(perms.contains(PosixFilePermission.GROUP_EXECUTE) || perms.contains(PosixFilePermission.OTHERS_EXECUTE), false);
                            P.setExecutable(perms.contains(PosixFilePermission.OWNER_EXECUTE), true);

                            P.setReadable(perms.contains(PosixFilePermission.GROUP_READ) || perms.contains(PosixFilePermission.OTHERS_READ), false);
                            P.setReadable(perms.contains(PosixFilePermission.OWNER_READ), true);

                            P.setWritable(perms.contains(PosixFilePermission.GROUP_WRITE) || perms.contains(PosixFilePermission.OTHERS_WRITE), false);
                            P.setWritable(perms.contains(PosixFilePermission.OWNER_WRITE), true);
                        }
                    }
                }

                // Remove all unreferenced templates and descriptions
                core.pruneTemplates();
            }

            if (generate)
            {
                /* Mark all content as not generated. */
                core.clearGeneratedContent();

                /* Instantiate the platform and artifact provider. */
                Map<Long, String> scripts = core.getGenerators();
                String shell = core.getConfig().shell();
                Path currentRelativePath = Paths.get("");
                String base = currentRelativePath.toAbsolutePath().toString();

                if (shell == null)
                    shell = "/bin/bash";
                File generators = new File(core.getConfig().dirGenerators());

                ExecutorService executor = Executors.newFixedThreadPool(generatorProcessCount);

                for (Map.Entry<Long, String> script : scripts.entrySet())
                {
                    Runnable worker = new GeneratorExecutor(core, generators, base, shell, script.getKey(), script.getValue());
                    executor.execute(worker);
                }

                executor.shutdown();
                while (!executor.isTerminated())
                {
                    try
                    {
                        Thread.sleep(500);
                    } catch (Exception e)
                    {
                    }
                }

                /* Remove all content that is not referenced or generated. */
                core.pruneContent();
            }
        } catch (Exception e)
        {
            System.err.println("ERROR: Exception " + e.getMessage());
            e.printStackTrace(System.err);
        } finally
        {
            // artifactProvider is ever used
//          if (artifactProvider != null)
//          {
//              artifactProvider.close();
//              artifactProvider = null;
//          }

            if (core != null)
            {
                core.close();
                core = null;
            }
        }
    }

    @SuppressWarnings("unused")
    private static void runner(String[] args)
    {
        if (args.length < 2)
            runHelp(); // exits app

        for (int i = 1; i < args.length; i++)
        {
            if (args[i].compareTo("--help") == 0)
            {
                runHelp(); // exits app
            }
        }        

        int runCount = -1;
        long manualTestNumber = -1;
        long manualTestInstanceNumber = -1;
        String owner = null;
        boolean help = true;
        
        for (int i = 1; i < args.length; i++){
            try{
                if (args[i].compareTo("--test") == 0 && args.length > i)
                {
                    i += 1;
                    manualTestNumber = Long.parseLong(args[i]);
                } else if (args[i].compareTo("--test-instance") == 0 && args.length > i){
                    i += 1;
                    manualTestInstanceNumber = Long.parseLong(args[i]);
                } else if (args[i].compareTo("--owner") == 0 && args.length > i){
                    i += 1;
                    owner = args[i];
                } else{
                    System.err.println("WARN: Only manual tests supported. Use the --test or --test-instance options instead.");
                    runHelp();
                    runCount = Integer.parseInt(args[1]);
                }
               }catch(NumberFormatException e){
                System.err.println("Invalid argument " + args[i] + ". Expected number instead.");
                runHelp();
               }
        }
        
        try{
            Core core = null;

            if(manualTestNumber > -1){
                core = new Core(manualTestNumber);
            }
            else{
                core = new Core(0);
            }

            String accessKeyID = core.getConfig().sqsAccessKeyID();
            String secretKey = core.getConfig().sqsSecretAccessKey();
            if(accessKeyID != null && !accessKeyID.isEmpty()){
                System.setProperty("aws.accessKeyId", accessKeyID);
                System.setProperty("aws.secretKey", secretKey);
            }
            SQSTestPublisher sqs = new SQSTestPublisher(core.getConfig().sqsEndpoint(), null, null, core.getConfig().sqsQueueName());
            sqs.init();
            List<Long> testRuns = new ArrayList<Long>();
            if(manualTestInstanceNumber > -1){
                    testRuns.add(core.createInstanceRun(manualTestInstanceNumber, owner));
            } else if(manualTestNumber > -1){
                for(long testInstance: core.getTestInstances(manualTestNumber)){
                    Long run = core.createInstanceRun(testInstance, owner);
                    if(run != null)
                        testRuns.add(run);
                }
            }
            for(Long runID: testRuns){
                System.out.println("Queueing test run: " + runID);
                sqs.publishTestRunRequest(runID);
            }
        } catch(Exception e){
            System.err.println("Failed to queue test run - " + e);
        }
    }

    private static void result(String[] args)
    {
        String hash = null;
        Long run = null;
        Boolean result = null;

        if (args.length < 2 || args[1].compareTo("--help") == 0)
            resultHelp();
                

        for (int i = 1; i < args.length; i++)
        {
            if (args[i].compareTo("--hash") == 0 && args.length > i)
            {
                if (hash != null || run != null)
                    resultHelp();

                hash = args[i + 1];
                i += 1;
            } else if (args[i].compareTo("--run") == 0 && args.length > i)
            {
                if (hash != null || run != null)
                    resultHelp();

                run = Long.parseLong(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("pass") == 0 && args.length == i + 1)
            {
                result = true;
            } else if (args[i].compareTo("fail") == 0 && args.length == i + 1)
            {
                result = false;
            } else
                resultHelp();
        }

        if (result == null || (hash == null && run == null)){
            System.err.println("Missing required argument");
            resultHelp();
        }

        Core core = new Core(0);
        try{
            if(hash != null){
                core.reportResult(hash, result, null, null, null, null);
            } else{
                core.addResultToRun(run, result);
            }
        } catch(Exception e){
            System.err.println("Failed to add result: " + e);
        } finally{
            core.close();
        }
    }

    private static class PopulateArtifact implements Artifact
    {
        private Module module;
        private String name;
        private int artifacts;
        private long start;
        private String targetFilePath;

        PopulateArtifact(Module module, String name, int artifacts, long start)
        {
            this.module = module;
            this.name = name;
            this.artifacts = artifacts;
            this.start = start;
        }

        @Override
        public Module getModule()
        {
            return module;
        }

        @Override
        public String getConfiguration()
        {
            return "default";
        }

        @Override
        public String getName()
        {
            return name;
        }

        private static class TarContent implements Content
        {
            private byte[] content;
            private Hash hash;

            TarContent(String name, int artifacts, long start)
            {
                try
                {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    GzipCompressorOutputStream cos = new GzipCompressorOutputStream(bos);
                    TarArchiveOutputStream os = new TarArchiveOutputStream(cos);

                    for (int artifact = 0; artifact < artifacts; artifact++)
                    {
                        String content_str = name + "-" + Integer.toString(artifact + 1);
                        TarArchiveEntry entry = new TarArchiveEntry(content_str);
                        entry.setModTime(start);
                        byte[] content_bytes = content_str.getBytes();
                        entry.setSize(content_bytes.length);
                        os.putArchiveEntry(entry);
                        os.write(content_bytes);
                        os.closeArchiveEntry();
                    }

                    os.finish();
                    os.close();
                    cos.close();
                    bos.close();

                    content = bos.toByteArray();
                    hash = new Hash(content);
                } catch (Exception e)
                {
                    System.err.println("ERROR: Failure to create populated artifact, " + e.getMessage());
                }
            }

            @Override
            public String getValue(Template template) throws Exception
            {
                return "";
            }

            @Override
            public Hash getHash()
            {
                return hash;
            }

            @Override
            public InputStream asStream()
            {
                return new ByteArrayInputStream(content);
            }

            @Override
            public byte[] asBytes()
            {
                return content;
            }
        }

        private TarContent content = null;

        @Override
        public Content getContent()
        {
            if (content != null)
                return content;

            content = new TarContent(name, artifacts, start);
            return content;
        }

        @Override
        public int getPosixMode()
        {
            return 0b100_100_100;
        }

        @Override
        public String getTargetFilePath() {
            return this.targetFilePath;
        }

        @Override
        public void setTargetFilePath(String targetFilePath) {
            this.targetFilePath = targetFilePath;
        }
    }

    private static class PopulateModule implements Module
    {
        private String org;
        private String name;
        private String version;
        private String status;
        private int sequence;
        private int artifacts;
        private long start;

        PopulateModule(String org, String name, String version, String status, int sequence, int artifacts, long start)
        {
            this.org = org;
            this.name = name;
            this.version = version;
            this.sequence = sequence;
            this.status = status;
            this.artifacts = artifacts;
            this.start = start;
        }

        @Override
        public String getOrganization()
        {
            return org;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public String getVersion()
        {
            return version;
        }

        @Override
        public String getStatus()
        {
            return status;
        }

        @Override
        public String getSequence()
        {
            return String.format("%05d", sequence);
        }

        @Override
        public Map<String, String> getAttributes()
        {
            return new HashMap<String, String>();
        }

        @Override
        public List<Artifact> getArtifacts()
        {
            List<Artifact> result = new ArrayList<Artifact>();
            result.add(new PopulateArtifact(this, org + "-" + name + "-" + version + ".tar.gz", artifacts, start));
            return result;
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern)
        {
            return new ArrayList<Artifact>();
        }

        @Override
        public List<Artifact> getArtifacts(String namePattern, String configuration)
        {
            return new ArrayList<Artifact>();
        }
    }

    private static class PopulateProvider implements ArtifactProvider
    {
        private int orgs;
        private int modules;
        private int versions;
        private int artifacts;
        private long start;

        PopulateProvider(int orgs, int modules, int versions, int artifacts, long start)
        {
            this.orgs = orgs;
            this.modules = modules;
            this.versions = versions;
            this.artifacts = artifacts;
            this.start = start;
        }

        @Override
        public void init() throws Exception
        {
        }

        @Override
        public void iterateModules(ModuleNotifier moduleNotifier) throws Exception
        {
            for (int org = 0; org < orgs; org++)
            {
                String org_str = "com.pslcl.testing.org" + Integer.toString(org + 1);
                for (int module = 0; module < modules; module++)
                {
                    String module_str = "module" + Integer.toString(module + 1);
                    for (int version = 0; version < versions; version++)
                    {
                        String version_str = "v" + Integer.toString(version + 1);
                        String status_str = "release";
                        switch (version % 3)
                        {
                            case 0:
                                status_str = "nightly";
                                break;

                            case 1:
                                status_str = "integration";
                                break;
                        }

                        moduleNotifier.module(this, new PopulateModule(org_str, module_str, version_str, status_str, version + 1, artifacts, start), null);
                    }
                }
            }
        }

        @Override
        public boolean merge(String merge, Module module, Module target)
        {
            return false;
        }

        @Override
        public void close()
        {
        }
    }

    /**
     * Populate the database with fictional information for testing purposes. The following
     * data can be created based on input parameters:
     *   test plans: enabled with the --plans option, creates the number of specified plans.
     *   tests: enabled with the --tests option, creates the number of specified tests in each created plan.
     *   modules: enabled with the --modules option, creates the number of specified modules.
     *   artifacts: enabled with the --artifacts option, creates the number of specified artifacts in each module.
     *   instances: enabled with the --instances option, creates test instances like a generator would.
     *   results: enabled with the --results option, creates test results.
     * @param args
     */
    private static void populate(String[] args)
    {
        if (args.length < 2 || args[1].compareTo("--help") == 0)
            populateHelp();

        int plans = 0;
        int tests = 0;
        int instances = 0;
        int orgs = 0;
        int modules = 0;
        int versions = 0;
        int artifacts = 0;
        String owner = null;
        boolean results = false;
        long end = 0;
        long start = 0;

        for (int i = 1; i < args.length; i++)
        {
            if (args[i].compareTo("--plans") == 0 && args.length > i)
            {
                plans = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--tests") == 0 && args.length > i)
            {
                tests = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--instances") == 0 && args.length > i)
            {
                instances = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--orgs") == 0 && args.length > i)
            {
                orgs = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--modules") == 0 && args.length > i)
            {
                modules = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--versions") == 0 && args.length > i)
            {
                versions = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--artifacts") == 0 && args.length > i)
            {
                artifacts = Integer.parseInt(args[i + 1]);
                i += 1;
            } else if (args[i].compareTo("--owner") == 0 && args.length > i)
            {
                owner = args[i + 1];
                i += 1;
            } else if (args[i].compareTo("--results") == 0)
            {
                results = true;
            } else if (args[i].compareTo("--start") == 0)
            {
                SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try
                {
                    Date d = parse.parse(args[i + 1]);
                    start = d.getTime();
                } catch (Exception e)
                {
                    System.err.println("ERROR: Could not parse date, " + e.getMessage());
                }

                i += 1;
            } else if (args[i].compareTo("--end") == 0)
            {
                SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                try
                {
                    Date d = parse.parse(args[i + 1]);
                    end = d.getTime();
                } catch (Exception e)
                {
                    System.err.println("ERROR: Could not parse date, " + e.getMessage());
                }

                i += 1;
            } else{
                System.err.println("Invalid argument " + args[i]);
                populateHelp();
            }
        }

        // Check for illegal combinations
        if (plans == 0 && tests > 0)
            populateHelp();
        if (tests == 0 && instances > 0)
            populateHelp();
        if (orgs == 0 && modules > 0)
            populateHelp();
        if (modules == 0 && artifacts > 0)
            populateHelp();
        if ((artifacts == 0 || instances == 0) && results)
            populateHelp();
        if ((owner != null) && instances == 0)
            populateHelp();
        if ((end != 0 || start != 0) && !results)
            populateHelp();

        if (results && end == 0 & start == 0)
        {
            end = System.currentTimeMillis();
            start = end - (7L * 24 * 60 * 60 * 1000);
        }

        if (results && end == 0)
            end = start + (7L * 24 * 60 * 60 * 1000);

        if (results && start == 0)
            start = end - (7L * 24 * 60 * 60 * 1000);

        if (start >= end)
            populateHelp();

        Core core = new Core(0);
        int total_artifacts = orgs * modules * versions * artifacts;

        // Populate the modules and artifacts.
        ModuleNotifier moduleNotifier = new HandleModule(core);
        ArtifactProvider populateProvider = new PopulateProvider(orgs, modules, versions, artifacts, start);
        try
        {
            populateProvider.init();
            populateProvider.iterateModules(moduleNotifier);
        } catch (Exception e)
        {
            System.err.println("ERROR: Failed to iterate modules, " + e.getMessage());
        } finally
        {
            populateProvider.close();
        }

        // We will generate a test instance for each result
        //   A test for each 10 test instances
        //   A test case for each 10 tests.
        // However, all this command does is populate the test plans and tests.
        // The rest of the work is done as a generator.
        int test_sequence = 0;
        int total_runs = plans * tests * instances;

        for (int test_plan = 0; test_plan < plans; test_plan++)
        {
            String test_plan_str = Integer.toString(test_plan + 1);
            long pk_test_plan = core.addTestPlan("Test Plan " + test_plan_str, "Description for test plan " + test_plan_str);
            for (int test = 0; test < tests; test++)
            {
                String test_str = test_plan_str + "/" + Integer.toString(test + 1);
                long pk_test = core.addTest(pk_test_plan, "Test " + test_str, "Description for test " + test_str, "");

                // Create instances by acting as a generator.
                Generator generator = new Generator(pk_test);
                for (int instance = 0; instance < instances; instance++)
                {
                    generator.startTest();
                    Machine simpleMachine = new Machine(generator, "simpleMachine");

                    Artifact[] artifact_list = new Artifact[instance + 1];
                    for (int name = 0; name < instance + 1; name++)
                    {
                        // Pick the appropriate artifacts to use in this instance.
                        // We will pick the number of artifacts based on the index of the instance.
                        // So, instance 2 will use 3 artifacts.
                        // We will pick artifacts based on the test we are generating sequentially.
                        //  A1, A2, A3, A4, A5, ...
                        //  com.pslcl.testing.org1-module1-v1.tar.gz-1
                        int artifact_index = (test_plan + 1) * (test + 1) * (instance + 1) * (name + 1) * 13;
                        artifact_index %= total_artifacts;
                        int organization_index = artifact_index / (modules * versions * artifacts);
                        artifact_index -= organization_index * (modules * versions * artifacts);
                        int module_index = artifact_index / (versions * artifacts);
                        artifact_index -= module_index * (versions * artifacts);
                        int version_index = artifact_index / artifacts;
                        artifact_index -= version_index * artifacts;

                        String[] names = new String[1];
                        names[0] = String.format("com.pslcl.testing.org%d-module%d-v%d.tar.gz-%d", organization_index + 1, module_index + 1, version_index + 1, artifact_index + 1);
                        Iterable<Artifact[]> simple = generator.createArtifactSet(null, null, names);
                        artifact_list[name] = simple.iterator().next()[0];
                    }

                    try
                    {
                        simpleMachine.bind(new Attributes());
                        simpleMachine.deploy(artifact_list);
                    } catch (Exception e)
                    {
                        System.err.println("ERROR: Couldn't bind an deploy instance, " + e.getMessage());
                    }

                    // Log results if either we need to have results or if there is an owner. For repeatability,
                    // We alternate pass/fail/pending for every 3 results. We alternate assigning an owner every 7
                    // results.
                    if (results || owner != null)
                    {
                        boolean need_start = false, need_complete = false;

                        // Pass/Fail generation. Pass groups of 3 tests, then fail 3, then pend 3.
                        // Owner generation. Assign owner for groups of 7, then skip 7.                    
                        if (((test_sequence / 3) % 3) == 0)
                        {
                            generator.pass();
                            need_start = need_complete = true;
                        } else if (((test_sequence / 3) % 3) == 1)
                        {
                            generator.fail();
                            need_start = need_complete = true;
                        }

                        if (((test_sequence / 7) % 2) == 0)
                        {
                            generator.assign(owner);
                            need_start = true;
                        }

                        if (need_complete)
                            need_start = true;

                        // Dates are set based on the test_sequence and whether or not a result or owner is assigned.
                        Date tstart = null;
                        if (need_start)
                            tstart = new Date(start + test_sequence * ((end - start) / total_runs));

                        Date tready = null;
                        if (need_start)
                            tready = new Date(tstart.getTime() + 5L * 60 * 1000);

                        Date tcomplete = null;
                        if (need_complete)
                            tcomplete = new Date(tstart.getTime() + 10L * 60 * 1000);

                        generator.setRunTimes(tstart, tready, tcomplete);
                    }

                    test_sequence += 1;
                    try {
                        generator.completeTest();
                    } catch (Exception e) {
                        System.err.println("ERROR: Unable to complete test, " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                generator.close();
            }
        }

        core.close();
    }

    /**
     * This is the main entry point for the test platform. All command-line requests to the platform come through this entry
     * point. The following commands are understood, each having its own set of parameters.
     *   --help - show help for the command itself.
     *   synchronize - synchronize the database with artifact providers.
     *
     *   check_release - check whether a component version can be released.
     *   check_test_runs - runs all tests and compares the results with the current database state, synchronizing them.
     * @param args Arguments passed to the program.
     */
    public static void main(String[] args)
    {

        // Check for no parameters, or --help
        if (args.length == 0 || args[0].compareTo("--help") == 0)
            generalHelp();

        if (args[0].compareTo("synchronize") == 0)
        {
            synchronize(args);
        } else if (args[0].compareTo("run") == 0)
        {
            runner(args);
        } else if (args[0].compareTo("result") == 0)
        {
            result(args);
        } else if (args[0].compareTo("populate") == 0)
        {
            populate(args);
        }
        System.exit(0);
    }

}
