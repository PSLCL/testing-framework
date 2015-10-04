package com.pslcl.qa.runner;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.LoggerFactory;

import com.pslcl.qa.runner.config.RunnerServiceConfig;
import com.pslcl.qa.runner.process.ActionStore;
import com.pslcl.qa.runner.process.ProcessTracker;
import com.pslcl.qa.runner.process.RunnerMachine;
import com.pslcl.qa.runner.store.instance.QueueStoreDao;
import com.pslcl.qa.runner.store.instance.Sqs;
//import org.eclipse.jetty.server.Connector;
//import org.eclipse.jetty.server.HttpConfiguration;
//import org.eclipse.jetty.server.HttpConnectionFactory;
//import org.eclipse.jetty.server.SecureRequestCustomizer;
//import org.eclipse.jetty.server.Server;
//import org.eclipse.jetty.server.ServerConnector;
//import org.eclipse.jetty.server.SslConnectionFactory;
//import org.eclipse.jetty.server.handler.ContextHandler;
//import org.eclipse.jetty.servlet.ServletHandler;
//import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Control the Runner Service startup and shutdown.
 * 
 * RunnerService has no requirement that it be instantiated more than once, but it is coded to allow that possibility.
 * Static references are limited to the Action enum (holds pure code), the QueueStore (one only), and the template database (one only).
 */
public class RunnerService implements Daemon, RunnerServiceMBean, UncaughtExceptionHandler
{
    // static declarations

    public static final String QueueStoreDaoClassKey = "com.pslcl.qa.runner.mq-class";
    public static final String QueueStoreDaoClassDefault = Sqs.class.getName();

    private volatile QueueStoreDao mq;
    private volatile RunnerServiceConfig config;

    /** the process classes */
    public volatile RunnerMachine runnerMachine;
    public volatile ActionStore actionStore; // holds state of each template; TODO: actionStore is instantiated here, but not yet otherwise used 
    public volatile ProcessTracker processTracker;

    // public class methods

    /**
     * Constructor
     */
    public RunnerService()
    {
        // Setup what we can, prior to knowing configuration
        // Most setup should be done in the init method.
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    // Daemon interface implementations

    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException
    {
        try
        {
            // The RunnerServiceConfig may initialize logging based on commandline switches and/or configuration file
            // and yet this method can fail with a DaemonInitException, in which case other methods of this class
            // will be called (stop, destroy).  Because of this, its best to not declare a local instance or static
            // logger variable in this class, always use the factory for every log.
            config = new RunnerServiceConfig(daemonContext, this);
            config.init();

            runnerMachine = new RunnerMachine();
            actionStore = new ActionStore(); // TODO: any order required?
            processTracker = new ProcessTracker(this);

            config.initsb.ttl("Initialize JMX: ");
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(this, new ObjectName("com.pslcl.qa.platform:type=RunnerService"));
            config.initsb.indentedOk();
            config.initsb.ttl("Initialize RunnerMachine:");
            runnerMachine.init(config);
            config.initsb.indentedOk();

            config.initsb.ttl("Initialize QueueStoreDao:");
            config.initsb.level.incrementAndGet(); // l2
            String daoClass = config.properties.getProperty(QueueStoreDaoClassKey, QueueStoreDaoClassDefault);
            config.initsb.ttl(QueueStoreDaoClassKey, " = ", daoClass);
            mq = (QueueStoreDao) Class.forName(daoClass).newInstance();
            mq.init(config);
            config.initsb.indentedOk();
            config.initsb.level.decrementAndGet();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(config.initsb.sb.toString(), e);
            throw new DaemonInitException(getClass().getSimpleName() + " failed:", e);
        }
        // process RunnerService config
    }

    // TODO: clean this up if done with it
    //  /**
    //  *
    //  */
    // public static class HelloServlet extends HttpServlet {
    //     @Override
    //     protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
    //         response.setContentType("text/html");
    //         response.setStatus(HttpServletResponse.SC_OK);
    //         response.getWriter().println("<h1>Hello World from 'HelloServlet'</h1>");
    //     }
    //        
    // }

    // TODO: the following was moved from the init method    
    //public void init(DaemonContext context)
    //{
    // init access to the template DAO-referenced database (one is common to all instances of RunnerService)

    //            // init web server to accept incoming template requests
    //            Server server = new Server(); // If port 8080 is supplied, this would create a default Connector that listens for requests on port 8080

    //            // keystoreFile to use
    //            String keystorePath = null; // TODO: access to our real keystore
    //            File keystoreFile = new File(keystorePath);
    //            if (!keystoreFile.exists())
    //                throw new FileNotFoundException(keystoreFile.getAbsolutePath());

    //            // setup a config object to be used in two separate Connectors
    //            HttpConfiguration httpConfig = new HttpConfiguration();
    //            httpConfig.setSecureScheme("https");
    //            httpConfig.setSecurePort(8443);
    //            httpConfig.setOutputBufferSize(32768);
    //            
    //            // an http Connector, with full configuration
    //            ServerConnector httpConn = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    //            httpConn.setHost("localhost");
    //            httpConn.setPort(8080);
    //            httpConn.setIdleTimeout(30000);

    //            // an https Connector, with full configuration
    //            SslContextFactory sslContextFactory = new SslContextFactory();
    ////            sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
    ////            sslContextFactory.setKeyStorePassword(null); // these nulls are temporary, they mean: accept console input
    ////            sslContextFactory.setKeyManagerPassword(null); // TODO: supply real string passwords here

    //            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
    //            httpsConfig.addCustomizer(new SecureRequestCustomizer());
    //            ServerConnector httpsConn = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
    //            httpsConn.setPort(8443);
    //            httpsConn.setIdleTimeout(600000);
    //            
    //            // set both connectors
    //            server.setConnectors(new Connector[] {httpConn, httpsConn});
    //
    //            // Set server handler, on context "/hello". Handler therefore responds only to requests from any URI beginning with /hello.
    //            ContextHandler context = new ContextHandler();
    //            context.setContextPath("/hello");
    //            context.setHandler(new ExampleHandler()); // TODO: in order to handle incoming requests, decide if we even want a "handler," since our servlet might give enough capability  
    //            server.setHandler(context);
    //            
    //            // set a minimal raw servlet
    //            ServletHandler servletHandler = new ServletHandler();
    //            server.setHandler(servletHandler);
    //            servletHandler.addServletWithMapping(HelloServlet.class, "/*");

    //            WebSocketHandler wsHandler = new WebSocketHandler()
    //                {
    //                    @Override
    //                    public void configure(WebSocketServletFactory factory)
    //                    {
    //                        factory.register(MyEchoSocket.class);
    //                    }
    //                };
    //            server.setHandler(wsHandler);

    //            server.start();
    //            server.stop(); // temporary
    //}            

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

    @Override
    public void start() throws Exception
    {
        try
        {
            config.initsb.ttl(getClass().getSimpleName() + " start");
            if (mq.queueStoreExists())
            {
                // Setup QueueStore DAO-referenced message handler (a standard callback from the JMS spec)
                mq.initQueueStoreGet();
            } else
            {
                LoggerFactory.getLogger(getClass()).warn("RunnerService.start exits- QueueStore message queue not available.");
                throw new Exception("QueueStore not available");
            }
            config.initsb.indentedOk();
        } catch (Exception e)
        {
            LoggerFactory.getLogger(getClass()).error(config.initsb.sb.toString(), e);
            throw e;
        }
        LoggerFactory.getLogger(getClass()).debug(config.initsb.sb.toString());
    }

    //TODO: this came from start() method, needs cleaned up
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

    @Override
    public void stop() throws Exception
    {
        LoggerFactory.getLogger(getClass()).debug("Stopping RunnerService.");
        if (runnerMachine != null)
            runnerMachine.destroy();
        // Destroy the Status Tracker
        if (config != null)
            config.statusTracker.destroy();
    }

    /**
     * Cleanup objects created by the service.
     */
    @Override
    public void destroy()
    {
        // jsvc calls this to destroy resources created in init()
        LoggerFactory.getLogger(getClass()).info("Destroying RunnerService.");
    }

    // RunnerServiceMBean interface implementations

    @Override
    public short getStatus()
    {
        return (short) config.statusTracker.getStatus().ordinal();
    }

    @Override
    public float getLoad()
    {
        synchronized (this)
        {
            // TODO Is there a better measurement of load than this?
            double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            MemoryUsage memUse = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            double memLoad = (double) memUse.getUsed() / memUse.getMax();
            if (memLoad > cpuLoad)
                return (float) memLoad;
            return (float) cpuLoad;
        }
    }

    // UncaughtExceptionHandler interface implementation

    /**
     * Process information about an uncaught Exception.
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        String msg = "FATAL ERROR: Uncaught exception in thread " + thread;
        LoggerFactory.getLogger(getClass()).error(msg, ex);
        LoggerFactory.getLogger(getClass()).debug("The FATAL ERROR message (at error log level) is issued by the handler RunnerService.uncaughtException()");
        // Since the state is unknown at this point, we may not be able to perform a graceful exit.
        System.exit(1); // forces termination of all threads in the JVM
    }

    /**
     * 
     * @param strRunEntryNumber String representation of the run entry number, or reNum (pk_run of this entry in table run).
     * @param message JMS message associated with reNum, used for eventual message ack
     * @throws Exception
     */
    public void submitQueueStoreNumber(String strRunEntryNumber, Message message) throws Exception
    {
        try
        {
            // the ordinary method
            long reNum = Long.parseLong(strRunEntryNumber);
            LoggerFactory.getLogger(getClass()).debug("RunnerService.submitQueueStoreNumber() finds reNum " + reNum);
            try
            {
                if (ProcessTracker.resultStored(reNum))
                {
                    ackRunEntry(message);
                    System.out.println("RunnerService.submitQueueStoreNumber() finds reNum " + reNum + ", result already stored. Acking this reNum now.");
                } else if (processTracker.inProcess(reNum))
                {
                    System.out.println("RunnerService.submitInstanceNumber_Store() finds reNum " + reNum + ", work already processing");
                } else
                {
                    // This call must ack the message, or cause it to be acked out in the future. Failure to do so will repeatedly re-introduce this reNum.
                    runnerMachine.initiateProcessing(reNum, message);
                }
            } catch (Exception e)
            {
                // do nothing; reNum remains in InstanceStore, we will see it again
                System.out.println("RunnerService.submitInstanceNumber_Store() sees exception for reNum " + reNum + ". Leave reNum in QueueStore. Exception msg: " + e);
            }
        } catch (Exception e)
        {
            throw e; // recipient must ack the message
        }

        // the hexStr method
        //      * @note Instance number comes from hex string of even length from 2 to 16 chars, parsing to byte array of 1 to 8 bytes, converting to a Java long, treated as unsigned long. 
        //        try {
        //            // early tests- does hexStrInstanceNumber produce a valid long integer? And is resulting iNum already completed or in process?
        //            byte [] bytes = DatatypeConverter.parseHexBinary(hexStrInstanceNumber);
        //            if (bytes.length <= 8 && bytes.length > 0) {
        //                // use ByteBuffer class to compute long instanceNumber; first form eight byte array
        //                byte [] eightBytes = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
        //                int lengthDif = eightBytes.length - bytes.length;
        //                for (int i=bytes.length-1; i>=0; i--)
        //                    eightBytes[i+lengthDif] = bytes[i];
        //                ByteBuffer bb = ByteBuffer.wrap(eightBytes);
        //                long iNum = bb.getLong(); // 
        //                logger.debug("RunnerService.submitInstanceNumber() converts hex string to " + bytes.length + " bytes. Computed instance number is " + iNum + " (0x" + hexStrInstanceNumber + ")");
        //                try {
        //                    if (ProcessTracker.resultStored(iNum)) {
        //                        ackInstanceEntry(message);
        //                        System.out.println("RunnerService.submitInstanceNumber_Store() finds instanceNumber " + iNum + ", result already stored. Acking this iNum now.");
        //                    } else if (processTracker.inProcess(iNum)) {
        //                        System.out.println("RunnerService.submitInstanceNumber_Store() finds instanceNumber " + iNum + ", work already processing");
        //                    } else {
        //                        // This call must ack the message, or cause it to be acked out in the future. Failure to do so will repeatedly re-introduce this instanceNumber.
        //                        runnerMachine.initiateProcessing(iNum, message);
        //                    }
        //                } catch (Exception e) {
        //                    // do nothing; iNum remains in InstanceStore, we will see it again
        //                    System.out.println("RunnerService.submitInstanceNumber_Store() sees exception for instanceNumber " + iNum + ". Leave iNum in InstanceStore. Exception msg: " + e);
        //                }
        //            } else {
        //                throw new Exception("RunnerService.submitInstanceNumber_Store() rejects converted hex string byte [] of length " + bytes.length + ". Length must be from 1 to 8.");
        //            }
        //        } catch (Exception e) {
        //            throw e; // recipient must ack the message
        //        }
    }

    /**
     * 
     * @param message Original opaque message associated with a run entry number, used now to ack the message
     * @throws JMSException
     */
    private void ackRunEntry(Object message) throws JMSException
    {
        // this call is for classes that do not know about JMS
        mq.ackQueueStoreEntry((Message) message);
    }

}