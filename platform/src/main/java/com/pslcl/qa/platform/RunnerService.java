package com.pslcl.qa.platform;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.qa.platform.process.ActionStore;
import com.pslcl.qa.platform.process.ProcessTracker;
import com.pslcl.qa.platform.process.RunnerMachine;
import com.pslcl.qa.platform.store.instance.InstanceStoreDao;
import com.pslcl.qa.platform.store.instance.Sqs;


/**
 * Control the Runner Service startup and shutdown.
 * 
 * RunnerService has no requirement that it be instantiated more than once, but it is coded to allow that possibility.
 * Static references are limited to the Action enum (holds pure code), the InstanceStore (one only), and the test Instance database (one only).
 */
public class RunnerService implements Daemon, RunnerServiceMBean, UncaughtExceptionHandler {
    
    // static declarations

    /** The logger used to log any messages. */
    private static final Logger logger = LoggerFactory.getLogger(RunnerService.class); // setup the SLF4J logger

    /**
     *
     */
    public static class HelloServlet extends HttpServlet {
        @Override
        protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Hello World from 'HelloServlet'</h1>");
        }
           
    }

    
    // class variables
    
    private InstanceStoreDao mq = null;
    
    
    /** The status tracker */
    private StatusTracker statusTracker = null;
    
    
    /** the process classes */
    public RunnerMachine runnerMachine = null;
    public ActionStore actionStore = null;  /** holds state of each test instance */
    public ProcessTracker processTracker = null;
    
    // public class methods

    /**
     * Constructor
     */
    public RunnerService() {
        // Setup what we can, prior to knowing configuration
        Thread.setDefaultUncaughtExceptionHandler(this);

    }
    
    
    // Daemon interface implementations
    
    /**
     * Init the RunnerService.
     */
    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        // Initialize the service (with elevated privileges).
        // jsvc calls this to examine configuration and to create resources
        logger.info("Initializing RunnerService.");
        
        synchronized (this) { // avoid overlap of init(), start(), stop(), or destroy()
            // Setup JMX monitoring capability.
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            try {
                mbs.registerMBean(this, new ObjectName("com.pslcl.qa.platform:type=RunnerService"));
            } catch (Exception e) {
                logger.debug(".registerMBean() failed: " + e.getMessage());
            }
            
            // init access to Test Instances DAO-referenced database (one is common to all instances of RunnerService)
            
              // jetty specific 
//            try {
//                Class.forName("org.eclipse.jetty.http.HttpField"); // not required at compile time, but required for jetty Server constructor to use at run time
//                Class.forName("org.eclipse.jetty.io.ByteBufferPool"); // not required at compile time, but required for jetty Server constructor to use at run time
//            } catch (Exception e2) {
//                logger.debug("RunnerService.init() finds unavailable class object: " + e2.getMessage());
//                throw e2;
//            }
            
            // init web server to accept incoming test instance requests
            Server server = new Server(); // If port 8080 is supplied, this would create a default Connector that listens for requests on port 8080

            // keystoreFile to use
            String keystorePath = null; // TODO: access to our real keystore
//            File keystoreFile = new File(keystorePath);
//            if (!keystoreFile.exists())
//                throw new FileNotFoundException(keystoreFile.getAbsolutePath());
            
            // setup a config object to be used in two separate Connectors
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(8443);
            httpConfig.setOutputBufferSize(32768);
            
            // an http Connector, with full configuration
            ServerConnector httpConn = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
            httpConn.setHost("localhost");
            httpConn.setPort(8080);
            httpConn.setIdleTimeout(30000);
            
            // an https Connector, with full configuration
            SslContextFactory sslContextFactory = new SslContextFactory();
//            sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
//            sslContextFactory.setKeyStorePassword(null); // these nulls are temporary, they mean: accept console input
//            sslContextFactory.setKeyManagerPassword(null); // TODO: supply real string passwords here
            
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            ServerConnector httpsConn = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
            httpsConn.setPort(8443);
            httpsConn.setIdleTimeout(600000);
            
            // set both connectors
            server.setConnectors(new Connector[] {httpConn, httpsConn});

            // Set server handler, on context "/hello". Handler therefore responds only to requests from any URI beginning with /hello.
            ContextHandler context = new ContextHandler();
            context.setContextPath("/hello");
            context.setHandler(new ExampleHandler()); // TODO: in order to handle incoming requests, decide if we even want a "handler," since our servlet might give enough capability  
            server.setHandler(context);
            
            // set a minimal raw servlet
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(HelloServlet.class, "/*");
            
//            WebSocketHandler wsHandler = new WebSocketHandler()
//                {
//                    @Override
//                    public void configure(WebSocketServletFactory factory)
//                    {
//                        factory.register(MyEchoSocket.class);
//                    }
//                };
//            server.setHandler(wsHandler);
            
            
            server.start();
            server.stop(); // temporary
            
            // init connection to one or more DAO-referenced message queues (these are to be common to all instances of RunnerService)
            //     note: the intent is that this or these queues not be created by project code- it is or they are separately created at install time
            mq = new Sqs(this); // class Sqs can be replaced with another implementation of MessageQueueDao
            try {
                mq.connect();
            } catch (JMSException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        } // end synchronized()

        // process RunnerService config
    }
    
    
//    @WebSocket
//    public class MyEchoSocket {
//        @OnWebSocketMessage
//        public void onText(WebSocketConnection conn, String message) {
//            System.out.println("text: " + message);
//            try {
//                conn.write(null, new FutureCallback(), "got: " + message);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * Start the RunnerService.
     */    
    @Override
    public void start() throws Exception {
        // jsvc calls this to start RunnerService behavior
        synchronized (this) { // avoid overlap of init(), start(), stop(), or destroy()
            logger.debug("Starting RunnerService.");
            
            // Create the Status Tracker
            statusTracker = new StatusTracker();

            // instantiate the process classes 
            actionStore = new ActionStore(); // TODO: any order required?
            runnerMachine = new RunnerMachine(this, null);
            processTracker = new ProcessTracker(this);
            
            if (mq.instanceStoreExists()) {
                // Setup InstanceStore DAO-referenced message handler (a standard callback from the JMS spec)
                mq.initInstanceStoreGet();
            } else {
                logger.warn("RunnerService.start exits- InstanceStore message queue not available.");
                throw new Exception("InstanceStore not available");
            }
            
            
            
            
            
            
            

//            long manualTestNumber = -1;
//            long manualTestInstanceNumber = -1;
//
//            // temp place to accept test number or test instance number
//            manualTestNumber = 1;
//            
//            if (manualTestInstanceNumber != -1)
//                System.out.println( "Run test number " + manualTestNumber + " on test instance number " + manualTestInstanceNumber );
//            else {
//                // Run the indicated test, according to its manualTestNumber. This will execute as many associated tests runs as are found in the database.
//                System.out.println( "Run all ready test instances, for test number " + manualTestNumber);
//            }
//            try {
//                // Instantiate class Core: the platform and test instance access.
//                Core core = new Core( manualTestNumber );
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
        }
    }

    @Override
    public void stop() throws Exception {
        synchronized (this) { // avoid overlap of init(), start(), stop(), or destroy()
            logger.debug("Stopping RunnerService.");
            
            // Destroy the Status Tracker
            statusTracker.close();
            statusTracker = null;
        }
    }

    /**
     * Cleanup objects created by the service.
     */
    @Override
    public void destroy() {
        // jsvc calls this to destroy resources created in init()
        logger.info("Destroying RunnerService.");
        synchronized (this) { // avoid overlap of init(), start(), stop(), or destroy()
        } // end synchronized()
        logger.info("RunnerService Terminated.");
    }

    
    // RunnerServiceMBean interface implementations

    @Override
    public short getStatus() {
        short status = StatusTracker.WARN;
        synchronized (this) {
            if (statusTracker != null)
                status = statusTracker.getStatus();
        }
        return status;
    }

    @Override
    public float getLoad() {
        synchronized (this) {
            // TODO Is there a better measurement of load than this?
            double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            double memLoad = (double)memUse.getUsed() / memUse.getMax();
            if (memLoad > cpuLoad)
                return (float)memLoad;
            return (float)cpuLoad;
        }
    }


    // UncaughtExceptionHandler interface implementation

    /**
     * Process information about an uncaught Exception.
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String msg = "FATAL ERROR: Uncaught exception in thread " + thread;
        logger.error(msg, ex);
        logger.debug("The FATAL ERROR message (at error log level) is issued by the handler RunnerService.uncaughtException()");
        // Since the state is unknown at this point, we may not be able to perform a graceful exit.
        System.exit(1); // forces termination of all threads in the JVM
    }
    
    /**
     * 
     * @note Instance number comes from hex string of even length from 2 to 16 chars, parsing to byte array of 1 to 8 bytes, converting to a Java long, treated as unsigned long. 
     * @param hexStrInstanceNumber Representation of test instance number.
     * @param message JMS message associated with the instance number, used for eventual message ack
     * @throws Exception
     */
    public void submitInstanceNumber_Store(String hexStrInstanceNumber, Message message) throws Exception {
        try {
            // early tests- does hexStrInstanceNumber produce a valid long integer? And is resulting iNum already completed or in process? 
            byte [] bytes = DatatypeConverter.parseHexBinary(hexStrInstanceNumber);
            if (bytes.length <= 8 && bytes.length > 0) {
                // use ByteBuffer class to compute long instanceNumber; first form eight byte array
                byte [] eightBytes = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
                int lengthDif = eightBytes.length - bytes.length;
                for (int i=bytes.length-1; i>=0; i--)
                    eightBytes[i+lengthDif] = bytes[i];
                ByteBuffer bb = ByteBuffer.wrap(eightBytes);
                long iNum = bb.getLong(); // 
                logger.debug("RunnerService.submitInstanceNumber() converts hex string to " + bytes.length + " bytes. Computed instance number is " + iNum + " (0x" + hexStrInstanceNumber + ")");
                try {
                    if (ProcessTracker.resultStored(iNum)) {
                        ackInstanceEntry(message);
                        System.out.println("RunnerService.submitInstanceNumber_Store() finds instanceNumber " + iNum + ", result already stored. Acking this iNum now.");
                    } else if (processTracker.inProcess(iNum)) {
                        System.out.println("RunnerService.submitInstanceNumber_Store() finds instanceNumber " + iNum + ", work already processing");
                    } else {
                        // This call must ack the message, or cause it to be acked out in the future. Failure to do so will repeatedly re-introduce this instanceNumber.
                        runnerMachine.initiateProcessing(iNum, message);
                    }
                } catch (Exception e) {
                    // do nothing; iNum remains in InstanceStore, we will see it again
                    System.out.println("RunnerService.submitInstanceNumber_Store() sees exception for instanceNumber " + iNum + ". Leave iNum in InstanceStore. Exception msg: " + e);
                }
            } else {
                throw new Exception("RunnerService.submitInstanceNumber_Store() rejects converted hex string byte [] of length " + bytes.length + ". Length must be from 1 to 8.");
            }
        } catch (Exception e) {
            throw e; // recipient must ack the message
        }
    }

    /**
     * 
     * @param message Original opaque message associated with the instance number, used now to ack the message
     * @throws JMSException
     */
    private void ackInstanceEntry(Object message) throws JMSException {
        // this call is for classes that do not know about JMS
        mq.ackInstanceEntry((Message)message);
    }
    
}