package com.pslcl.qa.runner.config.cli;

@SuppressWarnings("javadoc")
public class DtfCli extends CliBase
{
    public DtfCli(String[] args)
    {
        super(args, null, true, false, DefaultMaxHelpWidth);
        addCommand(new DtfCliCommand(this, null));
    }
}