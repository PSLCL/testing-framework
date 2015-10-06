package com.pslcl.qa.runner.config.cli;

import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonController;

@SuppressWarnings("javadoc")
public class CliDaemonContext implements DaemonContext
{
    private final DaemonController controller;
    private String[] args;
    
    public CliDaemonContext(CliBase cliBase)
    {
        controller = new CliDaemonController(cliBase);
    }

    @Override
    public DaemonController getController()
    {
        return controller;
    }

    @Override
    public synchronized String[] getArguments()
    {
        return args;
    }

    public synchronized void setArguments(String[] args)
    {
        this.args = new String[args.length];
        System.arraycopy(args, 0, this.args, 0, args.length);
    }
    
    public static class CliDaemonController implements DaemonController
    {
        public final CliBase cliBase;
        public CliDaemonController(CliBase cliBase)
        {
            this.cliBase = cliBase;
        }
        
        @Override
        public void shutdown() throws IllegalStateException
        {
        }

        @Override
        public void reload() throws IllegalStateException
        {
        }

        @Override
        public void fail() throws IllegalStateException
        {
        }

        @Override
        public void fail(String message) throws IllegalStateException
        {
        }

        @Override
        public void fail(Exception exception) throws IllegalStateException
        {
        }

        @Override
        public void fail(String message, Exception exception) throws IllegalStateException
        {
        }
    }
}