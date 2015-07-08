package com.pslcl.qa.platform;

import java.util.Scanner;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.daemon.support.DaemonLoader;

/**
 * This class is a way to start the Basic Service.
 * This is not the preferred way to run a service and is included only for testing and development purposes.
 */
public class RunnerServiceNodeRunner {

    public static void main(String args[]) {
        Daemon service = new RunnerService();
        DaemonLoader.Context context = new DaemonLoader.Context();
        context.setArguments(args);
        context.setController(new Controller(service));
        int exitStatus = 1;
        try {
            service.init(context);
            service.start();

            // Wait for stop command from stdin
            Scanner sc = new Scanner(System.in);
            System.out.printf("Enter 'stop' to halt:\n");
            while (!sc.nextLine().toLowerCase().equals("stop"))
                ;

            context.getController().shutdown();
            sc.close();
            exitStatus = 0;
        } catch (DaemonInitException e) {
            context.getController().fail("Initialization failure", e);
        } catch (Exception e) {
            context.getController().fail("General failure", e);
        }
        System.out.println("main() calls System.exit() with exitStatus " + exitStatus);
        System.exit(exitStatus);
    }

    private static class Controller implements DaemonController {

        private final Daemon service;

        public Controller(Daemon service) {
            this.service = service;
        }

        @Override
        public void fail() throws IllegalStateException {
            fail("");
        }

        @Override
        public void fail(String str) throws IllegalStateException {
            System.err.println("Service Failed: " + str);
            System.exit(1);
        }

        @Override
        public void fail(Exception e) throws IllegalStateException {
            fail(e.getMessage());
        }

        @Override
        public void fail(String str, Exception e) throws IllegalStateException {
            fail(str + " (" + e.getMessage() + ")");
        }

        @Override
        public void reload() throws IllegalStateException {
            try {
                this.service.stop();
            } catch (Exception e) {
                System.err.println("Stop failure (" + e.getMessage() + ")");
            }
            try {
                this.service.start();
            } catch (Exception e) {
                fail("Start failure", e);
            }
        }

        @Override
        public void shutdown() throws IllegalStateException {
            try {
                this.service.stop();
            } catch (Exception e) {
                fail("Stop failure in shutdown() handler", e);
            }
            this.service.destroy();
            System.out.println("shutdown() called; now exits");
        }
    }

}
