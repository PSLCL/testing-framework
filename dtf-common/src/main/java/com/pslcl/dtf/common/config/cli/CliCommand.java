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
package com.pslcl.dtf.common.config.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.AmbiguousOptionException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.pslcl.dtf.common.config.util.PropertiesFile;
import com.pslcl.dtf.common.config.util.StrH;

/**
 * Command line Interface Command
 * <p>Provides an abstract base class that command line applications can extend 
 * to declare custom commands and switches.
 *   
 * <p>This class utilizes the Composite pattern where a parent/child relationship 
 * exists and the child is of the same type as the parent (base type in this case).  
 * This allows for any number width and depth nodes in a command tree.  The
 * <code>CliBase</code> provides the root of the command tree, with this class
 * being the parent/child nodes coming from the root.  
 *   
 * <p>A switch is an Apache commons-cli "option".  The <code>CliBase</code> class 
 * and this class assume these are single hyphen prefixed short names (typically a 
 * single character) with a corresponding double hyphened long name (human readable equivalent).   
 * 
 * <p>A command is a leading command line argument which provides a hierarchy where   
 * a set of custom switches are scoped unique to the leaf commands of a command tree.
 * Because of the composite pattern mentioned above anY level of width and depth is 
 * possible.  However, in practice it is likely that the tree will only be one level 
 * deep (i.e. ExecutableClass verbCmd) or two levels deep (i.e. ExecutableClass nounCmd verbCmd).
 * 
 * <p>The <code>commandName</code> used to instantiate an object of this class is used 
 * as the leading command line argument command node names.  An application can instantiate
 * a single <code>CliCommand</code> with a null <code>commandName</code> to create a simple 
 * switch driven application with no command structure.
 * 
 * <p>The following switches are reserved for all extending applications
 * with some caveats:
 * <ul>
 *  <li>-h, --help</li> 
 *  <li>-v, --version</li> 
 *  <li>-c, --config-path</li> 
 *  <li>-l, --log-path</li> 
 * </ul>   
 * Help is always reserved at every command level.  Version is only reserved (and implemented) 
 * at the executable level (i.e. <code>args[0]</code>) and can be used for other purposes 
 * at any command leaf.  The config-path and log-path switches are optional.  Both are implemented 
 * to initialize application wide <code>Properties</code> and logging.
 *  @see CliBase
 */
public abstract class CliCommand
{
    protected final String commandName;
    protected final Options options;
    protected final CliBase cliBase;
    private final Hashtable<String, CliCommand> commands;
    protected volatile CommandLine commandline;
    private volatile CliCommand parent;
    
    /**
     * Simple constructor.
     * @param cliBase the <code>Object</code> of the class extending <code>CliBase</code>.  
     * The root of the command tree.  Must not be null
     * @param commandName the command nodes name.  If null a non-command application is declared.
     * Otherwise, the value given is used for the name of the command node.
     */
    public CliCommand(CliBase cliBase, String commandName)
    {
        this(cliBase, null, commandName);
    }
    
    /**
     * Full constructor.
     * @param cliBase the <code>Object</code> of the class extending <code>CliBase</code>.  
     * The root of the command tree.  Must not be null
     * @param parent the parent command of this node.  Null if the parent is the <code>CliBase</code>.
     * @param commandName the command nodes name.  If null a non-command application is declared.
     * Otherwise, the value given is used for the name of the command node.
     */
    public CliCommand(CliBase cliBase, CliCommand parent, String commandName)
    {
        this.cliBase = cliBase;
        this.commandName = commandName;
        this.parent = parent;
        options = new Options();
        commands = new Hashtable<String, CliCommand>();
    }
    
    /**
     * Return Leading Command String.
     * @param command the node to determine the path to.
     * @return a <code>String</code> of the executable class and command names 
     * leading to the given command.  Will never return null.
     */
    public String getLeadingCommandString(CliCommand command)
    {
        List<String> names = new ArrayList<String>();
        while (command != null)
        {
            command = command.getParent();
            if(command != null)
                names.add(command.getCommandName());
        }
        StringBuilder sb = new StringBuilder(cliBase.getExecutableName());
        int size = names.size();
        for(int j=size-1; j >= 0; j--)
            sb.append(" " + names.get(j));
        return sb.toString();
    }
    
    /**
     * Return Parent.
     * @return my parent.  Null if my parent is the <code>CliBase</code>.
     */
    public CliCommand getParent()
    {
        return parent;
    }

    /**
     * Return Command Name.
     * @return my command/node name.  Null if application is in non-command mode.
     */
    public String getCommandName()
    {
        return commandName;
    }

    /**
     * Return the CommandLine object.
     * <p>Only leaf node commands can have a commons-cli CommandLine object.  
     * It must also be the active command, based on the given command line arguments.
     * In the case of non-command mode, there is only one node and it will have
     * the <code>CommandLine</code> object.
     * <p>Note that the <code>CommandLine</code> object is never available until
     * after the <code>validateCommands</code> method has been called.
     * @return my <code>CommandLine</code> object.  Returns non-null 
     * if <code>validateCommands</code> has been called and this node is the <code>activeCommand</code>.
     * Otherwise, null is returned.
     */
    public CommandLine getCommandLine()
    {
        return commandline;
    }

    /**
     * Report if this node has children.
     * @return true if this node contains child commands, false otherwise.
     */
    public boolean hasChildren()
    {
        return commands.size() > 0;
    }
    
    /**
     * Report if this node has <code>Options</code>.
     * <p>A command node might not have any switches (i.e. clear).
     * @return true if this node has commons-cli <code>Option</code>'s set, false otherwise.
     */
    public boolean hasOptions()
    {
        return options.getOptions().size() > 0;
    }
    
    /**
     * Add a command child.
     * @param command the child command to add.
     */
    public void addChild(CliCommand command)
    {
        command.parent = this;
        commands.put(command.getCommandName(), command);
        command.cliSetup();
    }

    /**
     * Validate the command line <code>args</code>.
     * 
     * <p>This method will cause Help to be called if invalid
     * command line <code>args</code> are given.  Help exits 
     * the application and this method will never return in this case.
     * 
     * @param commandDepth the zero based depth of this command.
     * @return the <code>activeCommand</code> if the command line was valid and
     * the requested command path is down my branch. Returns null otherwise.
     */
    public CliCommand validateCommands(int commandDepth)
    {
        String[] originalArgs = cliBase.getOriginalArgs();
        // if no more nested commands, get this commands commons-cli CommandLine
        if(commands.size() == 0)
        {
            originalArgs = cliBase.getOriginalArgs();
            if(!originalArgs[commandDepth].equals(commandName)) // -1 because CliBase holds 0th
                return null;
            commandDepth++;
            if(commandDepth >= originalArgs.length && hasOptions())
                help(0, null);
            String[] myArgs = new String[originalArgs.length - commandDepth];
            int j = commandDepth;
            for(int i = 0; i < myArgs.length; i++)
                myArgs[i] = originalArgs[j++];
            parseCommandLine(myArgs);
            customInit();
            return this;
        }

        String[] args = cliBase.getOriginalArgs();
        if(!args[commandDepth].equals(commandName))
            return null; // not my command.
        
        if(commandDepth + 1 > args.length)
            commandHelp(0, null);
        
        String arg = args[commandDepth+1];
        if(arg.equals("-" + CliBase.HelpShortCl) || arg.equals("--" + CliBase.HelpLongCl))
            commandHelp(0, null);
        
        commandDepth++;
        for(Entry<String, CliCommand> entry : commands.entrySet())
        {
            if(entry.getKey().equals(arg))
            {
                CliCommand cliCommand = entry.getValue().validateCommands(commandDepth);
                if(cliCommand != null)
                  return cliCommand;
            }
        }
        if(hasChildren())
            commandHelp(CliBase.InvalidCommand, "Invalid command: " + arg);
        return null;
    }
    
    /**
     * Handle Configuration file.
     * <p>The method will attempt to obtain the properties file from the reserved
     * configuration switch if it is enabled.  The application wide <code>Properties</code>
     * obtained from the <code>CliBase</code> will be loaded with the configuration if 
     * enabled and found.
     * <p>If the configuration switch is enabled and the <code>Properties</code> can not be 
     * loaded with the given file, this method will call Help with error information.  Help
     * will exit the application and this method will never return in this case.
     * @return true if the configuration switch is enabled and the configuration was obtained.  
     * Otherwise, false is returned. 
     */
    public boolean handleConfiguration()
    {
        boolean gotConfig = false;
        if(cliBase.isConfigSwitch() || !cliBase.isLogToFile())
        {
            // if !logToFileSwitch try looking in the configuration file for configPathKey
            gotConfig = true;
            if(!commandline.hasOption(CliBase.ConfigurationPathShortCl))
            {
                gotConfig = false;
                if(cliBase.isConfigSwitch() && commandline.hasOption(CliBase.HelpShortCl))
                {
                    System.err.println("-" + CliBase.ConfigurationPathShortCl + " | --" + CliBase.ConfigurationPathLongCl + " required\n");
                    help(CliBase.ConfigNotFound, "Given configuration file not found");
                }
            }
            if(gotConfig)
            {
                StringBuilder sb = cliBase.getInitsb();
                Properties properties = cliBase.properties;
                String configPathKey = cliBase.getConfigPathKey();
                String configPath = commandline.getOptionValue(CliBase.ConfigurationPathShortCl);
                StrH.ttl(sb, 1, "--",CliBase.ConfigurationPathLongCl, " = ", configPath);
                try
                {
                    PropertiesFile.loadFile(properties, new File(configPath));
                }catch(Exception e)
                {
                    help(CliBase.ConfigNotFound, "Given configuration file not found");
                }
                if(configPathKey != null)
                {
                    StrH.ttl(sb, 1, configPathKey, " = ", configPath);
                    System.setProperty(configPathKey, configPath);
                }
            }
        }
        return gotConfig;
    }

    /**
     * commons-cli setup.
     * The <code>protected Options options</code> variable of this base case should 
     * be used by extending class to register it's custom switches for this node.
     */
    protected abstract void cliSetup();
    
    /**
     * Custom extending class Init.
     * After this objects <code>validateCommands</code> method has determined 
     * this is the activeCommand and the commons-cli <code>CommandLine</code> 
     * object has been created for the custom <code>Option</code>'s set in the
     * <code>cliSetup</code> method above, this method will be called allowing
     * the customizing class to do an needed initialization given a valid
     * <code>CommandLine</code> object.
     */ 
    protected abstract void customInit();
    
    void parseCommandLine(String[] args)
    {
        //@formatter:on
        CommandLineParser parser = new DefaultParser(); 
        try
        {
            commandline = parser.parse(options, args);
        }
        catch(AlreadySelectedException ase)
        {
            help(CliBase.InvalidCommandLine, "Already Selected: " + ase.getOption());
        }
        catch(AmbiguousOptionException aoe)
        {
            help(CliBase.InvalidCommandLine, "Ambiguous Option: " + aoe.getMatchingOptions());
        }
        catch(MissingArgumentException mae)
        {
            help(CliBase.InvalidCommandLine, "Missing Argument: " + mae.getOption());
        }
        catch(MissingOptionException moe)
        {
            help(CliBase.InvalidCommandLine, "Missing Option: " + moe.getMissingOptions());
        }
        catch(UnrecognizedOptionException uoe)
        {
            help(CliBase.InvalidCommandLine, "Unrecongnized Option: " + uoe.getOption());
        }
        catch (Exception e)
        {
            help(CliBase.InvalidCommandLine, "Unexpected Exception: " + e.getClass().getName());
        }
        if (commandline.hasOption(CliBase.HelpShortCl))
            help(0, null);
        if (commandline.hasOption(CliBase.VersionShortCl))
            cliBase.version();
    }
    
    /**
     * Non-Leaf command help.
     * <p>This method is called if the given command line <code>args</code>
     * calls for help on a non-leaf (i.e. no commons-cli switches) node.  
     * @param exitCode the <code>System.exit</code> value to use.
     * @param msg Optional error message.  Null if only help is to be displayed, 
     * Message is sent to standard err before the help if not null.
     */
    protected void commandHelp(int exitCode, String msg)
    {
        if(exitCode != 0 && msg != null)
            System.err.println(msg);
        String cmdStr = getLeadingCommandString(this);
        if(commandName != null)
            cmdStr += " " + commandName;
        StringBuilder sb = new StringBuilder("\nValid commands: " + cmdStr + " [-h|--help]|<");
        boolean first = true;
        for(Entry<String, CliCommand> entry : commands.entrySet())
        {
            if(!first)
                sb.append("|");
            sb.append(entry.getKey());
            first = false;
        }
        sb.append(">\n");
        sb.append(" -h,--help        Display " + commandName + " help.\n");
        sb.append("\n  Help for any given " + commandName + " sub-command can be obtained by:\n");
        for(Entry<String, CliCommand> entry : commands.entrySet())
            sb.append("    " + entry.getKey() + " [-" + CliBase.HelpShortCl + " | --" + CliBase.HelpLongCl + "]\n");
        System.out.println(sb.toString());
//        System.exit(exitCode);
        throw new RuntimeException(getClass().getSimpleName() + " Help displayed, see logs");
    }

    /**
     * Leaf command help.
     * <p>This method is called if the given command line <code>args</code>
     * calls for help on a leaf (i.e. commons-cli switches) node.  
     * @param exitCode the <code>System.exit</code> value to use.
     * @param msg Optional error message.  Null if only help is to be displayed, 
     * Message is sent to standard err before the help if not null.
     */
    protected void help(int exitCode, String msg)
    {
        help(exitCode, msg, null, null);
    }
    
    /**
     * Leaf command help.
     * <p>This method is called if the given command line <code>args</code>
     * calls for help on a leaf (i.e. commons-cli switches) node.  
     * @param exitCode the <code>System.exit</code> value to use.
     * @param msg Optional error message.  Null if only help is to be displayed, 
     * Message is sent to standard err before the help if not null.
     * @param header message to be displayed pre-help.
     * @param footer message to be displayed post-help.
     */
    protected void help(int exitCode, String msg, String header, String footer)
    {
        StringBuilder command = new StringBuilder(getLeadingCommandString(this));
        if(commandName != null)
            command.append(" " + commandName);
        
        if(exitCode != 0)
        {
            String[] args = cliBase.getOriginalArgs();
            StringBuilder sb = new StringBuilder(msg == null ? "commandline: " : msg+"\ncommandline: ");
            for(int i=0; i < args.length; i++)
                sb.append(" " + args[i]);
            sb.append("\n");
            System.err.println(sb.toString());
        }
        
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(cliBase.getMaxHelpWidth(), command.toString(), header, options, footer, true);
//        System.exit(exitCode);
        throw new RuntimeException(getClass().getSimpleName() + " Help displayed, see logs");
    }
    
    
    /**
     * Convert String array into space separated name. 
     * @param spacedName the string array to be space seperated. 
     * @return the space separated string array values. 
     */
    public String getSpacedName(String[] spacedName)
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < spacedName.length; i++)
        {
            if(i != 0)
                sb.append(" ");
            sb.append(spacedName[i]);
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {
        return "commandName=" + commandName + " subCommands=" + commands.size() + " active=" + (commandline != null);
    }
}