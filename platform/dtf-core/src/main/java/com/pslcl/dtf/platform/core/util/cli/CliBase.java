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
package com.pslcl.dtf.platform.core.util.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import com.pslcl.dtf.platform.core.util.ClassInfo;
import com.pslcl.dtf.platform.core.util.StrH;

/**
 * Command line Interface Base.
 * <p>Provides a base class that command line applications can extend to meet 
 * proposed OpenDOF.org tools work group standards of command structure and help 
 * presentation.  
 * <p>Apache commmons-cli is used for command line switch handling and switch help
 * presentation.  An optional command structure leading to leaf switches is 
 * supported.  The following switches are reserved for all extending applications
 * with some caveats:
 * <ul>
 *  <li>-h, --help</li> 
 *  <li>-v, --version</li> 
 *  <li>-c, --config-path</li> 
 *  <li>-l, --log-path</li> 
 * </ul>   
 * Help is always reserved.  Version is only reserved (and implemented) at the 
 * executable level (i.e. <code>args[0]</code>) and can be used for other purposes 
 * at any command leaf. 
 * config-path and log-path are optional.  Both are implemented to initialize 
 * application wide <code>Properties</code> and logging.
 * 
 * <p>The following structures are supported
 * <ol>
 *  <li>ExecutableClass [switches]</li> 
 *  <li>ExecutableClass verb [switches]</li> 
 *  <li>ExecutableClass noun verb [switches]</li> 
 *  <li>ExecutableClass noun1 noun2 verb [switches]</li> 
 * </ol>
 * If any of the above are given with no switches, help is called.
 * For item 1, commons-cli help is used.  For the others help showing the possible
 * commands for the level given is displayed.
 *   
 * <p>commons-cli switch support is only available at the command leafs.  However, 
 * -v,--version is supported at the ExecutableClass level and -h,--help
 * is supported at all levels.
 * 
 * <p>To use, the extending class uses one of the constructors to configure the
 *  base as needed.  It then calls the <code>addCommand</code> method on this class
 *  once for each <code>args[0]</code> custom command needed.  If structure item 1 
 *  from the above list is desired (non-command) then only one <code>CliCommand</code>
 *  is added where its <code>commandName</code> is set to null.
 *  
 *  <p>Nested commands (structure items 2-4) can be added as needed to the level one 
 *  commands with the <code>CliCommand.addCommand</code> method.
 *  
 *  <p>The extending class then calls the <code>validateCommands</code> method of
 *   this class to obtain the selected <code>CliCommand</code> and the commons-cli
 *   <code>CommandLine</code> object which it contains.
 *  @see CliCommand
 */
public class CliBase extends Thread
{
    /**  LogFilePathKey. <p>Environment key used to obtain the output log file path. */
    public static final String LogFilePathKey = "opendof.tools.log-file-path";
    /**  LogFileBaseKey. <p>Environment key used to obtain the output log file base path/name to use for rolling appender. */
    public static final String LogFileBaseKey = "opendof.tools.log-file-base";
    /**  ConfigPathKey. <p>Environment key used to obtain the configuration file. */
    public static final String ConfigPathKey = "opendof.tools.cli.config";
    
    /**  DefaultLogPath. <p>Default value used if logging is enabled and <code>LogFilePathKey</code> is not specified. */
    public static final String DefaultLogPath = "/var/opt/opendof/tools/log/cli.log";
    /**  DefaultMaxHelpWidth. <p>Default commons-cli maxium help width used if not specified. */
    public static final int DefaultMaxHelpWidth = 80;

    /**  ApplicationError. <p><code>System.exit</code> application error exit code. */
    public static final int ApplicationError = 1;
    /**  InvalidCommand. <p><code>System.exit</code> invalid command/sub-command error exit code. */
    public static final int InvalidCommand = 2;
    /**  InvalidCommand. <p><code>System.exit</code> commons-cli switch handling error exit code. */
    public static final int InvalidCommandLine = 3;
    /**  ConfigNotFound. <p><code>System.exit</code> invalid configuration file error exit code. */
    public static final int ConfigNotFound = 4;

    /**  HelpShortCl. <p>Short, single hyphened help switch. */
    public static final String HelpShortCl = "h";
    /**  HelpLongCl. <p>Long, double hyphened help switch. */
    public static final String HelpLongCl = "help";
    /**  VersionShortCl. <p>Short, single hyphened version switch. */
    public static final String VersionShortCl = "v";
    /**  VersionLongCl. <p>Long, double hyphened version switch. */
    public static final String VersionLongCl = "version";
    /**  LoggerShortCl. <p>Short, single hyphened output log file path switch. */
    public static final String LoggerNameShortCl = "l";
    /**  LoggerLongCl. <p>Long, double hyphened output log file path switch. */
    public static final String LoggerNameLongCl = "log-path";
    /**  ConfigurationPathShortCl. <p>Short, single hyphened configuration file path switch. */
    public static final String ConfigurationPathShortCl = "c";
    /**  ConfigurationPathLongLongCl. <p>Long, double hyphened configuration file path switch. */
    public static final String ConfigurationPathLongCl = "config-path";

    private final String[] originalArgs;
    private final String executableName;
    private final boolean configSwitch;
    private final boolean logToFile;
    private final String configPathKey;
    private final int maxHelpWidth;
    private final Map<String, CliCommand> commands;
    private final String version;
    private final StringBuilder initsb;
    public final Properties properties;
    private AtomicBoolean nonCommandCli;
    protected volatile Logger log;
    protected volatile CliCommand activeCommand;
    
    /**
     * Simple CliBase constructor.
     * @param args the given command line arguments.  Must not be null.
     * @param configPathKey environment key to use for obtaining the configuration 
     * file.  Default is used if null.
     */
    public CliBase(String[] args, String configPathKey)
    {
        this(args, configPathKey, true, true, DefaultMaxHelpWidth);
    }
    
    /**
     * Full CliBase constructor.
     * <p>If <code>logToFileSwitch</code> is false but <code>configSwitch</code> 
     * is true, the configuration file will be checked for <code>LogFilePathKey</code>
     * to obtain logging information.  The value of this key is used in a common
     * logback.xml configuration file that supports a rolling file appender which can
     * be used for all applications. 
     *  
     * @param args the given command line arguments.  Must not be null.
     * @param configPathKey environment key to use for obtaining the configuration file.  Default is used if null.
     * @param configSwitch if true enable the configSwitch, otherwise disable it.
     * @param logToFileSwitch if true enable the logToFileSwitch, otherwise disable it. 
     * @param maxHelpWidth the maximum help width for commons-cli to use.
     */
    public CliBase(String[] args, String configPathKey, boolean configSwitch, boolean logToFileSwitch, int maxHelpWidth)
    {
        this.originalArgs = args;
        this.executableName = getClass().getSimpleName();
        this.configPathKey = configPathKey == null ? ConfigPathKey : configPathKey;
        this.configSwitch = configSwitch;
        this.logToFile = logToFileSwitch;
        this.maxHelpWidth = maxHelpWidth;
        properties = new Properties();
        commands = new HashMap<String, CliCommand>();
        version = ClassInfo.getInfo(getClass()).getPackage().getImplementationVersion();
        initsb = new StringBuilder("\n" + getClass().getSimpleName() + " init:\n");
        StrH.ttl(initsb, 1, "executableName", " = ", executableName);
        StrH.ttl(initsb, 1, "version", " = ", version);
        StrH.ttl(initsb, 1, "configSwitch", " = ", configSwitch);
        StrH.ttl(initsb, 1, "logToFileSwitch", " = ", logToFileSwitch);
    }
    
    /**
     * Return Active Command.
     * @return the <code>CliCommand</code> specified in the given command line arguments.
     * @see CliCommand
     */
    public CliCommand getActiveCommand()
    {
        return activeCommand;
    }
    
    /**
     * Return Command Line.
     * @return the <code>CommandLine</code>from the active <code>CliCommand</code> 
     * specified in the given command line arguments.  Will never be null.
     * @see CliCommand
     */
    public CommandLine getCommandLine()
    {
        return activeCommand.getCommandLine();
    }
    
    /**
     * Return Executable class name.
     * @return the simple name of the class extending this base.  
     * Will never be null. 
     */
    public String getExecutableName()
    {
        return executableName;
    }
    
    /**
     * Return Version.
     * Display the version of the <code>Package</code> containing the class 
     * extending this base to <code>System.out</code>. 
     */
    protected void version()
    {
        System.out.println(executableName + " version: " + version);
        System.exit(0);
    }
    
    /**
     * Return given command line arguments.
     * @return the originally given application command line arguments.  
     * Will never be null 
     */
    public String[] getOriginalArgs()
    {
        return originalArgs;
    }
    
    /**
     * Return Initialization <code>StringBuilder</code>.
     * @return the <code>StringBuilder</code> capturing all initialization 
     * information used for debug logging.  Will never be null. 
     */
    public StringBuilder getInitsb()
    {
        return initsb;
    }
    
    /**
     * Return <code>Properties</code>.
     * <p>The returned <code>Properties</code> will be initialized with the
     * contents of the configuration file if enabled.  Otherwise this will be empty.
     * @return the application wide <code>Properties</code>.  
     * Will never be null, maybe empty. 
     */
//    public Properties getProperties()
//    {
//        return properties;
//    }
    
    /**
     * Return Configuration file path.
     * @return the configuration file path.  May be null.  
     */
    public String getConfigPathKey()
    {
        return configPathKey;
    }
    
    /**
     * Return the maximum help width.
     * @return the specified maximum help width for commons-cli to use.  
     */
    public int getMaxHelpWidth()
    {
        return maxHelpWidth;
    }
    
    /**
     * Return the <code>logToFile</code> value.
     * @return true if <code>logToFile</code> was specified true, otherwise return false.  
     */
    public boolean isLogToFile()
    {
        return logToFile;
    }
    
    /**
     * Return the <code>configSwitch</code> value.
     * @return true if <code>configSwitch</code> was specified true, otherwise return false.  
     */
    public boolean isConfigSwitch()
    {
        return configSwitch;
    }
    
    /**
     * Add a <code>CliCommand</code>.
     * <p>Called by the extending application class to add their custom commands.
     * <p>if the given <code>CliCommand.commandName</code> is null then a 
     * non-command simple switch only application is declared.  Otherwise a 
     * command driven application is declared.
     * <p>Commands added here are the first level commands expected at 
     * <code>args[0]</code>.
     * <p>Nested commands are supported to any level.
     */
    protected void addCommand(CliCommand command)
    {
        if(nonCommandCli != null)
        {
            if(nonCommandCli.get() && command.getCommandName() != null)
                throw new RuntimeException("Trying to add a command when already in non-command mode");
            if(nonCommandCli.get() && command.getCommandName() == null)
                throw new RuntimeException("Trying to add two non-commands");
            if(!nonCommandCli.get() && command.getCommandName() == null)
                throw new RuntimeException("Trying to add a non-command when already set to command mode");
        }
        if(command.getCommandName() != null)
            nonCommandCli = new AtomicBoolean(false);
        else
            nonCommandCli = new AtomicBoolean(true);
        commands.put(command.getCommandName(), command);
        command.cliSetup();
    }
    
    /**
     * Validate command line <code>args</code>. 
     * <p>The given command line <code>args</code> are validated against the
     * custom <code>CliCommand</code>'s added by the application.  Help is 
     * displayed and the application exits if there are errors.  Otherwise, the 
     * specified <code>activeCommand<code> is set, making its commons-cli 
     * <code>CommandLine</code> available for the application.
     * @return the selected <code>CliCommand</code>
     */
    @SuppressWarnings("null")
    public CliCommand validateCommands()
    {
        // check for no arguments help
        if(originalArgs.length == 0)
        {
            if(nonCommandCli.get())
                commands.get(null).help(0, null);
            commandHelp(0, null);
        }
        
        // check for simple no commands command line
        if(nonCommandCli.get())
        {
            CliCommand command = commands.get(null); 
            command.parseCommandLine(originalArgs);
            boolean gotConfig = command.handleConfiguration();
            handleLogger(command.getCommandLine(), gotConfig);
            command.customInit();
            activeCommand = command;
            return command;
        }

        // check for -v --version at first argument
        if(originalArgs[0].equals("-"+VersionShortCl) || originalArgs[0].equals("--"+VersionLongCl))
            version();
        if(originalArgs[0].equals("-"+CliBase.HelpShortCl) || originalArgs[0].equals("--"+CliBase.HelpLongCl))
            commandHelp(0, null);
        
        // check if first command given is known
        CliCommand cmd = null;
        for(Entry<String, CliCommand> entry : commands.entrySet())
        {
            if(entry.getKey().equals(originalArgs[0]))
            {
                cmd = entry.getValue();
                break;
            }
        }
        if(cmd == null)
            commandHelp(InvalidCommand, "Unknown command: " + originalArgs[0]);
        // check for first command given but no arguments help
        if(originalArgs.length == 1)
        {
            if(cmd.hasChildren())
                cmd.commandHelp(0, null);
            if(cmd.hasOptions())
                cmd.help(0, null);
            return cmd;
        }
                
        // must be more than 1 argument given, see if 2nd is another level command
        CliCommand command = null;
        for(Entry<String, CliCommand> entry : commands.entrySet())
        {
            command = entry.getValue();
            activeCommand = command.validateCommands(0);
            if(activeCommand != null)
               break;
        }
        
        boolean gotConfig = activeCommand.handleConfiguration();
        handleLogger(activeCommand.getCommandLine(), gotConfig);
        if(log.isDebugEnabled())
        {
            StrH.ttl(initsb, 1, "java.class.path:");
            String path = System.getProperty("java.class.path");
            do
            {
                int index = path.indexOf(';');
                if (index == -1)
                    break;
                String element = path.substring(0, index);
                StrH.ttl(initsb, 2, element);
                path = path.substring(++index);
            } while (true);
            path = path.replace(';', '\n');
        }
        log.debug(initsb.toString());
        return activeCommand;
    }
    
    private void commandHelp(int exitCode, String msg)
    {
        if(exitCode != 0 && msg != null)
            System.err.println(msg);
        StringBuilder sb = new StringBuilder("\nValid commands: " + executableName + " [-v|--version|-h|--help]|<");
        boolean first = true;
        for(Entry<String, CliCommand> entry : commands.entrySet())
        {
            if(!first)
                sb.append("|");
            sb.append(entry.getKey());
            first = false;
        }
        sb.append(">\n");
        
        sb.append(" -h,--help        Display " + executableName + " help.\n");
        sb.append(" -v,--version     Display " + executableName +"'s version information\n");
        sb.append("\n  Help for any given command can be obtained by:\n");
        for(Entry<String, CliCommand> entry : commands.entrySet())
            sb.append("    " + entry.getKey() + " [-" + HelpShortCl + "|--" + HelpLongCl + "]\n");
        System.out.println(sb.toString());
        System.exit(exitCode);
    }

    private String handleLogger(CommandLine commandline, boolean gotConfig)
    {
        String name = null;
        String logPath = null;
        if(!logToFile)
        {
            if(gotConfig)
            {
                logPath = properties.getProperty(LogFilePathKey);
                StrH.ttl(initsb, 1, LogFilePathKey, " = ", logPath);
                if(logPath != null)
                {
                    System.setProperty(LogFilePathKey, logPath);
                    String base = logPath;
                    int idx = logPath.lastIndexOf('.');
                    if(idx != -1)
                        base = logPath.substring(0, idx);
                    StrH.ttl(initsb, 1, LogFileBaseKey, " = ", base);
                    System.setProperty(LogFileBaseKey, logPath);
                    name = StrH.getAtomicNameFromPath(base);
                }
            }
        }else
        {
            logPath = commandline.getOptionValue(LoggerNameShortCl, DefaultLogPath);
            StrH.ttl(initsb, 1, "--",LoggerNameLongCl, " = ", logPath);
            System.setProperty(LogFilePathKey, logPath);
            String base = logPath;
            int idx = logPath.lastIndexOf('.');
            if(idx != -1)
                base = logPath.substring(0, idx);
            StrH.ttl(initsb, 1, LogFileBaseKey, " = ", base);
            System.setProperty(LogFileBaseKey, logPath);
            name = StrH.getAtomicNameFromPath(base);
        }
        
        log = LoggerFactory.getLogger(getClass());
        if(log.isDebugEnabled())
        {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            // TODO: StatusPrinter.setPrintStream
            StatusPrinter.print(lc);
        }
        return name;
    }
    
    /**
     * Add reserved cli switches.
     * <p>Base reserved switches are added to the given commons-cli <code>Options</code>.
     * <p>Note that -v and --version are reserved switches at the <code>args[0]<code>
     * level, but are manually handled instead of using commons-cli.  Version is always 
     * supported but is only valid for the executable.  Thus these values are available 
     * for other uses in other command leafs.
     * @param options the commons-cli <code>Options</code> to add the switches to.
     * @param logToFile if true, add logging switch, otherwise do not.
     * @param configSwitch if true, add configuration switch, otherwise do not.
     */
    public void cliReservedSetup(Options options, boolean logToFile, boolean configSwitch)
    {
        //@formatter:off
        options.addOption(
            Option.builder(HelpShortCl)
                .desc("Display help.")
                .longOpt(HelpLongCl)
                .build());

        if(logToFile)
        {
            options.addOption(
                Option.builder(LoggerNameShortCl)
                    .desc("The log files base name.")
                    .longOpt(LoggerNameLongCl)
                    .hasArg()
                    .build());
        }

        if(configSwitch)
        {
            options.addOption(
                Option.builder(ConfigurationPathShortCl)
                    .desc("Path to configuration file.")
                    .longOpt(ConfigurationPathLongCl)
                    .hasArg()
                    .required()
                    .build());
        }
        //@formatter:on
    }
    
    @Override
    public void run()
    {
        JOptionPane.showConfirmDialog(null, "Press any key to exit " + executableName);
        close(0);
    }

    /**
     * Close application cleanly.
     * <p>For convenience, an extending application is executed in its own thread
     * after successful constructor initialization.  The extending classes run method
     * can call <code>super.run()</code> to keep the application paused on a 
     * <code>JOptionPane</code>    
     * @param ccode <code>System.exit</code> code to use.
     */
    public void close(int ccode)
    {
        System.exit(ccode);
    }

}