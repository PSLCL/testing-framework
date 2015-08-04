package com.pslcl.qa.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;

import com.pslcl.qa.platform.generator.Artifact;
import com.pslcl.qa.platform.generator.ArtifactProvider;
import com.pslcl.qa.platform.generator.Core;
import com.pslcl.qa.platform.generator.Module;

public class CommandLine {
    private static <T extends Comparable<? super T>> List<T> asSortedList( Collection<T> c ) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    private static void generalHelp() {
        System.out.println( "test-platform General Help" );
        System.out.println( "[program] --help (this output)" );
        System.out.println( "[program] command --help (help on command)" );
        System.out.println( "[program] command <arguments> (run command with arguments)" );
        System.out.println( "commands are:" );
        System.out.println( "  synchronize - synchronize the artifact providers with the database." );
        System.out.println( "  result - add a result to the database." );
        System.out.println( "  run - extract tests from the database and run them." );
        System.exit( 1 );
    }

    private static void synchronizeHelp() {
        System.out.println( "test-platform Synchronize Help" );
        System.out.println( "This command synchronizes artifact providers with the database and runs generators." );
        System.out.println( "[program] synchronize --help (this output)" );
        System.out.println( "[program] synchronize <arguments>" );
        System.out.println( "arguments are:" );
        System.out.println( "  --no-synchronize - optional - disable synchronization." );
        System.out.println( "  --no-generators - optional - disable running generators." );
        System.out.println( "  --artifact-endpoint - required unless no-synchronize specified - The endpoint of the artifact provider." );
        System.out.println( "environment requirements are:" );
        System.out.println( "  DTF_TEST_ARTIFACTS - required - the full path of the directory that contains the artifact cache." );
        System.out.println( "  DTF_TEST_GENERATORS - required - the full path of the directory where generators are written." );
        System.exit( 1 );
    }
    
    private static void runHelp() {
        System.out.println( "test-platform Run Help" );
        System.out.println( "This command extracts tests from the database and runs them." );
        System.out.println( "[program] run --help (this output)" );
        System.out.println( "[program] run <arguments>" );
        System.out.println( "arguments are:" );
        System.out.println( "  <runcount> - required except for manual mode - the number of tests to run." );
        System.out.println( "  --manual i - optional, to run one test on all test instances soon - supply the id number assigned for the test." );
        System.out.println( "  --manual i j - optional, to run one test on one test instance soon - supply the id number assigned for the test, followed by the id number assigned to the test run." );
        System.out.println( "environment requirements are:" );
        System.out.println( "  TODO" );
        System.exit( 1 );
    }

    private static void resultHelp() {
        System.out.println( "test-platform Result Help" );
        System.out.println( "This command sets a result for a given component, version, and test instance." );
        System.out.println( "[program] result --help (this output)" );
        System.out.println( "[program] result <arguments> <result>" );
        System.out.println( "  <result> - either 'pass' or 'fail'" );
        System.out.println( "arguments are:" );
        System.out.println( "  --template <hash> - required - the template to apply the result to." );
        System.exit(1);
    }

    private static class HandleModule implements ArtifactProvider.ModuleNotifier {
        private static class Delayed {
            ArtifactProvider source;
            String merge;
            Module module;
        }
        
        private Core core;
        private List<Delayed> delayed = new ArrayList<Delayed>();
        
        public HandleModule( Core core ) {
            this.core = core;
        }
        
        private void decompress( Hash hash, long pk_version, long pk_parent, String configuration, boolean merge_source, long pk_source_module ) {
            TarArchiveInputStream ti = null;
            try {
                InputStream archive = new FileInputStream( core.getContentFile( hash ) );
                /* Uncompress and unarchive the file, creating entries for each artifact found inside. */
                InputStream is = new GzipCompressorInputStream(archive);
                ti = new TarArchiveInputStream(is);
                TarArchiveEntry entry;
                while ((entry = ti.getNextTarEntry()) != null) {
                    if (entry.isDirectory())
                        continue;

                    String artifact = entry.getName();
                    /* Standardize on no leading dot. */
                    if (artifact.startsWith("./"))
                        artifact = artifact.substring(2);

                    /* Extract the contents to a memory cache, determine the hash, and see if the file is already cached. */
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final int BUFFER_MAX_SIZE = 8192;
                    byte[] buffer = new byte[BUFFER_MAX_SIZE];

                    int count;
                    while ((count = ti.read(buffer, 0, BUFFER_MAX_SIZE)) != -1) {
                        bos.write(buffer, 0, count);
                    }

                    bos.close();

                    Hash h = core.addContent( ti );
                    core.addArtifact( pk_version, configuration, artifact, h, merge_source, pk_parent, pk_source_module );
                }
            }
            catch ( Exception e ) {
                
            }
            finally {
                if ( ti != null ) try {
                    ti.close();
                }
                catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        @Override
        public void module( ArtifactProvider source, Module module, String merge ) {
            try {
                // Check to see if the module exists. If it does then return (assuming that artifacts do not change).
                // If it does not exist then add the module and iterate the artifacts.
                if ( core.findModule( module ) != 0 )
                    return;
                
                boolean merge_source = false;
                if ( merge != null && merge.length() > 0 ) {
                    merge_source = true;
                    Delayed D = new Delayed();
                    D.source = source;
                    D.merge = merge;
                    D.module = module;
                    delayed.add( D );
                }
                
                long pk_module = core.addModule( module );
                List<Artifact> artifacts = module.getArtifacts();
                for ( Artifact artifact : artifacts ) {    
                    Hash h = core.addContent( artifact.getContent().asStream() );
                    long pk_artifact = core.addArtifact( pk_module, artifact.getConfiguration(), artifact.getName(), h, merge_source, 0, 0 );
                    if ( artifact.getName().endsWith(".tar.gz") ) {
                        decompress( h, pk_module, pk_artifact, artifact.getConfiguration(), merge_source, 0 );
                    }
                }
            }
            catch ( Exception e ) {
                
            }
        }
        
        public void finalize() {
            Iterable<Module> modules = core.createModuleSet();
            for ( Delayed d : delayed ) {
                Module dmod = d.module;
                long pk_source_module = core.findModule( dmod );
                
                for ( Module m : modules ) {
                    // Since the actual types may differ, we compare fields to determine if they are the same.
                    boolean same = true;
                    if ( ! m.getOrganization().equals( dmod.getOrganization() ) )
                        same = false;
                    if ( ! m.getName().equals( dmod.getName() ))
                        same = false;
                    if ( ! m.getVersion().equals( dmod.getVersion() ))
                        same = false;
                    if ( ! m.getSequence().equals( dmod.getSequence() ))
                        same = false;
                    
                    if ( same )
                        continue;
                    
                    if ( d.source.merge( d.merge, dmod, m ) ) {
                        long pk_module = core.findModule( m );
                        
                        List<Artifact> artifacts = dmod.getArtifacts();
                        for ( Artifact artifact : artifacts ) {   
                            Hash h = core.addContent( artifact.getContent().asStream() );
                            long pk_artifact = core.addArtifact( pk_module, artifact.getConfiguration(), artifact.getName(), h, false, 0, pk_source_module );
                            if ( artifact.getName().endsWith(".tar.gz") ) {
                                decompress( h, pk_module, pk_artifact, artifact.getConfiguration(), false, pk_source_module );
                            }
                        }
                    }
                }               
            }
        }
    }
    
    private static void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            public void run() {
                Scanner sc = new Scanner(src);
                while (sc.hasNextLine()) {
                    dest.println(sc.nextLine());
                }
                
                sc.close();
            }
        }).start();
    }

    private static class GeneratorExecutor implements Runnable {
        private File generators;
        private String base;
        private String shell;
        private long id;
        private String script;

        public GeneratorExecutor( File generators, String base, String shell, long id, String script ) {
            this.generators = generators;
            this.base = base;
            this.shell = shell;
            this.id = id;
            this.script = script;
        }

        public void run() {
            System.out.println( "Script: " + this.toString() + " started." );
            process();
            System.out.println( "Script: " + this.toString() + " completed." );
        }

        private void process() {
            try {
                String augmented_params = shell + " " + script;
                String[] params = augmented_params.split( " " );
                params[1] = generators.getAbsolutePath() + "/bin/" + params[1];
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command(params);
                processBuilder.environment().put("CLASSPATH", generators.getAbsolutePath() + "/lib/*" + File.pathSeparator + base + "/lib/*" );
                processBuilder.environment().put("DTF_TEST_ID", Long.toString( id ) );

                Process run = processBuilder.start();
                inheritIO(run.getInputStream(), System.out);
                inheritIO(run.getErrorStream(), System.err);

                boolean running = true;
                while (running == true) {
                    try {
                        run.exitValue();
                        running = false;
                    } catch (IllegalThreadStateException its) {
                        try { Thread.sleep(250); } catch ( Exception ex ) {}
                    }
                }
            }
            catch ( Exception e ) {
                System.err.println( "ERROR: Could not run script " + script + ", " + e );
            }
        }

        public String toString() {
            return Long.toString( id ) + "/" + script;
        }
    }

    private static void synchronize( String[] args ) {
        boolean synchronize = true;
        boolean generate = true;
        String endpoint = null;

        if (args.length > 1 && args[1].compareTo( "--help" ) == 0)
            synchronizeHelp();

        for (int i = 1; i < args.length; i++) {
            if ( args[i].compareTo( "--no-synchronize" ) == 0 ) {
                synchronize = false;
            }
            else if ( args[i].compareTo( "--no-generators" ) == 0 ) {
                generate = false;
            }
            else if ( args[i].compareTo( "--artifact-endpoint" ) == 0 ) {
                endpoint = args[++i];
            }
            else
                synchronizeHelp();
        }

        if ( System.getenv("DTF_TEST_ARTIFACTS") == null || System.getenv("DTF_TEST_GENERATORS") == null || (synchronize && endpoint == null) )
            synchronizeHelp();

        Core core = null;
        ArtifactProvider artifactProvider = null;

        try {
            /* Instantiate the platform and artifact provider. */
            core = new Core( 0 );

            if ( synchronize ) {
                File generators = new File( System.getenv("DTF_TEST_GENERATORS" ) );
                if ( generators.exists() )
                    //noinspection ResultOfMethodCallIgnored
                    org.apache.commons.io.FileUtils.deleteQuietly(generators);
                //noinspection ResultOfMethodCallIgnored
                generators.mkdirs();
                //noinspection ResultOfMethodCallIgnored
                generators.setWritable( true );

                // Get the list of artifact providers from the database.
                List<String> providers = core.readArtifactProviders();
                HandleModule handler = new HandleModule( core );
                List<ArtifactProvider> to_close = new ArrayList<ArtifactProvider>();
                
                for ( String providerName : providers ) {
                    ArtifactProvider provider = null;
                    try {
                        Class<?> P = Class.forName( providerName );
                        provider = (ArtifactProvider) P.newInstance();
                        
                        provider.init();
                        
                        /* Handle generators from this provider. */
                        //core.startArtifactProvider( providerName );
                        provider.iterateModules( handler );
                        //core.finishArtifactProvider( providerName );
                    }
                    catch ( Exception e ) {
                        System.err.println( e.getMessage() );
                    }
                }
                
                handler.finalize();
                
                for ( ArtifactProvider p : to_close ) {
                    p.close();
                }

                // Extract all generators
                Iterable<Module> find_generators = core.createModuleSet();
                for ( Module M : find_generators ) {
                    List<Artifact> artifacts = M.getArtifacts( null, "dth_test_generator" );
                    for ( Artifact A : artifacts ) {
                        File f = core.getContentFile( A.getContent().getHash() );
                        File P = new File( generators, A.getName() );
                        FileUtils.copyFile( f, P );
                    }
                }
                
                // Remove all unreferenced templates and descriptions
                core.pruneTemplates();
            }

            if ( generate ) {
                /* Mark all content as not generated. */
                core.clearGeneratedContent();

                /* Instantiate the platform and artifact provider. */
                Map<Long,String> scripts = core.getGenerators();
                String shell = System.getenv("DTF_TEST_SHELL");
                String base = System.getenv("DTF_TEST_BASE");
                if ( shell == null )
                    shell = "/bin/bash";
                File generators = new File( System.getenv("DTF_TEST_GENERATORS") );

                ExecutorService executor = Executors.newFixedThreadPool( 25 );

                for ( Map.Entry<Long,String> script : scripts.entrySet() ) {
                    Runnable worker = new GeneratorExecutor( generators, base, shell, script.getKey(), script.getValue() );
                    executor.execute( worker );
                }

                executor.shutdown();
                while ( ! executor.isTerminated() ) {
                    try { Thread.sleep( 500 ); } catch ( Exception e ) {}
                }

                /* Remove all content that is not referenced or generated. */
                core.pruneContent();
            }
        }
        catch (Exception e) {
            System.err.println("ERROR: Exception " + e.getMessage());
            e.printStackTrace( System.err );
        }
        finally {
            if (artifactProvider != null) {
                artifactProvider.close();
                artifactProvider = null;
            }

            if (core != null) {
                core.close();
                core = null;
            }
        }
    }
    
    private static void runner( String[] args ) {
        if (args.length < 2 || System.getenv("DTF_TEST_ARTIFACTS") == null || System.getenv("DTF_TEST_GENERATORS") == null)
            runHelp(); // exits app

        for (int i = 1; i < args.length; i++) {
            if (args[i].compareTo("--help") == 0) {
                runHelp(); // exits app
            }
        }
       
        int runCount = -1;
        boolean manual = false;
        long manualTestNumber = -1;
        long manualTestInstanceNumber = -1;
        boolean help = true;
        if (args[1].compareTo("--manual") != 0) {
            // not manual mode
            if (args.length == 2) {
                try {
                    runCount = Integer.parseInt(args[1]);
                    help = false;
                } catch (NumberFormatException e) {
                    // help true
                }
            }
        } else if (args.length == 3 || args.length == 4) {
            manual = true;
            try {
                manualTestNumber = Long.parseLong(args[2]);
                if (args.length == 4)
                    manualTestInstanceNumber = Long.parseLong(args[3]);
                help = false;
            } catch (NumberFormatException e) {
                // help true
            }
        }
        if (help == true)
            runHelp(); // exits app
        
//        // setup http client
//        HttpClient httpClient = new HttpClient();
//        httpClient.setFollowRedirects(false); // an example of configuring the client
//        try {
//            httpClient.start();
//            httpClient.stop();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        
//        System.out.println( "manual runner has setup the http client and exits" );

        
        
        
        
        
//        // connect to our message queue
//        MessageQueueDao mq = new Sqs(); // class Sqs can be replaced with another implementation of MessageQueueDao
//        try {
//            mq.connect();
//        } catch (JMSException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//        
//        Core core = null;
//        // run something
//        if (!manual) {
//            // ask for runcount prioritized tests and run them in the order received
//                // TODO decide: we analyze priority first, then take top runcount of them?
//                //              or, a separate process analyzes priority in real time, and we ask it for the top runcount of them.
//            System.out.println( "run " + runCount + " tests. Not implemented- use 'run --manual' command line with its options for now.");
//        } else {
//            // manual: Run the indicated test, according to its manualTestID. This will execute as many associated tests runs as are found in the database.
//            if (manualTestInstanceNumber != -1)
//                System.out.println( "runner: Run test number " + manualTestNumber + " on test instance number " + manualTestInstanceNumber );
//            else
//                System.out.println( "runner: Run all ready test instances, for test number " + manualTestNumber);
//            try {
//                /* Instantiate the platform and test instance access. */
//                core = new Core( manualTestNumber );
//
//                // read information about the set of test instances (aka test runs), to match command-line specified manualTestNumber
//                Set<Long> set;
//                if (manualTestInstanceNumber != -1) {
//                    set = core.readReadyTestInstances_test(manualTestNumber, manualTestInstanceNumber);
//                    System.out.println( "For test number " + manualTestNumber + " and test instance number " + manualTestInstanceNumber + ", " + set.size() + " test instance(s) is/are ready and not yet run");                
//                } else {
//                    set = core.readReadyTestInstances_test(manualTestNumber);
//                    System.out.println( "For test number " + manualTestNumber + ", " + set.size() + " test run(s) is/are ready and not yet run");
//                }
//                System.out.println("");
//                
//                // schedule running the n test instances in set: each becomes an independent thread
//                int setupFailure = 0;
//                for (Long setMember: set) {
//                    try {
//                        new RunnerInstance(core, setMember.longValue()); // launch independent thread, self closing
//                    } catch (Exception e) {
//                        setupFailure++;
//                    }
//                }
//                System.out.println( "manual runner launched " + set.size() + " test run(s)" + (setupFailure==0 ? "" : (" ********** but " + setupFailure + " failed in the setup phase")) );
//                
//                // This must be called, but only after every test instance has been scheduled. 
//                RunnerInstance.setupAutoThreadCleanup(); // non-blocking
//                // this app concludes, but n processes will continue to run until their work is complete
//            } catch (Exception e) {
//                System.out.println( "manual runner sees Exception " + e);
//                // TODO: kill runner thread with its thread pool executor
//            }
//        }
        System.out.println( "runner() exits");
    }
    
    private static void result( String[] args ) {
        String hash = null;
        Boolean result = null;

        if (args.length != 2 || args[1].compareTo("--help") == 0)
            resultHelp();

        for (int i = 1; i < args.length; i++) {
            if (args[i].compareTo("--hash") == 0 && args.length > i) {
                if ( hash != null )
                    resultHelp();

                hash = args[i + 1];
                i += 1;
            }
            else if (args[i].compareTo("pass") == 0 && args.length == i+1) {
                result = true;
            }
            else if (args[i].compareTo("fail") == 0 && args.length == i+1) {
                result = false;
            }
            else
                resultHelp();
        }

        if ( result == null || hash == null  )
            resultHelp();

        Core core = new Core( 0 );
        core.reportResult( hash, result );
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
    public static void main( String[] args ) {

        // Check for no parameters, or --help
        if ( args.length == 0 || args[0].compareTo( "--help" ) == 0 )
            generalHelp();

        if ( args[0].compareTo( "synchronize" ) == 0 ) {
            synchronize( args );
        }
        else if ( args[0].compareTo( "run" ) == 0 ) {
            runner( args );
        }
        else if ( args[0].compareTo( "result" ) == 0 ) {
            result( args );
        }
        System.exit(0);
    }
    
}
