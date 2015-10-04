package com.pslcl.qa.runner.config.cli;

import javax.swing.JOptionPane;

import org.apache.commons.daemon.DaemonContext;

import com.pslcl.qa.runner.RunnerService;

@SuppressWarnings("javadoc")
public class DtfCliApp extends Thread
{
    private final DaemonContext context;
    private final RunnerService dtfService;
    
    public DtfCliApp(String[] args)
    {
        context = new WindowsDaemonContext();
        ((WindowsDaemonContext)context).setArguments(args);
        dtfService = new RunnerService();
    }

    @Override
    public void run()
    {
        try
        {
            Thread.currentThread().setName(getClass().getSimpleName());
            dtfService.init(context);
            dtfService.start();
            JOptionPane.showConfirmDialog(null, "Any button Exits " + getClass().getSimpleName());
            close(0);
        } catch (Throwable t)
        {
            System.err.println("\n"+getClass().getSimpleName() + " failed: " + t.toString());
            t.printStackTrace();
            close(CliBase.ApplicationError);
        }
    }

    public void close(int ccode)
    {
        try
        {
            dtfService.stop();
        } catch (Throwable e)
        {
            System.err.println(getClass().getSimpleName() + ".close dtfService.stop failed: " + e.toString());
            e.printStackTrace();
        }
        dtfService.destroy();
        System.exit(ccode);
    }

    public static void main(String args[])
    {
        new DtfCliApp(args).start();
    }
}