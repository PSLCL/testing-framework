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
package com.pslcl.dtf.core.generator.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pslcl.dtf.core.artifact.Artifact;
import com.pslcl.dtf.core.generator.Generator;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.Program;
import com.pslcl.dtf.core.generator.template.Template;
import com.pslcl.dtf.core.generator.template.TestInstance;
import com.pslcl.dtf.core.generator.template.TestInstance.Action;

/**
 * This class represents a machine, which is a resource that can accept artifacts and
 * run programs.
 */
public class Machine extends Resource
{
    private final Logger log;
    private static final String codename = "machine";
    private Map<Artifact, Action> deployActions;

    /**
     * Define a new machine associated with the specified generator and with the given name.
     * @param generator The generator that can use the machine. Must not be null.
     * @param name The name of the machine for logging and debugging. Must not be null.
     */
    public Machine(Generator generator, String name)
    {
        super(generator, name, codename);
        this.log = LoggerFactory.getLogger(getClass());
        deployActions = new HashMap<Artifact, Action>();
    }

    /**
     * Bind a machine to a particular platform.
     * @throws Exception The bind is invalid.
     */
    @Override
    public void bind() throws Exception
    {
        try {
            Attributes attributes = new Attributes();
            super.bind(attributes);
        } catch (Exception e) {
            this.log.error("<internal> Machine.bind() exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * Bind a machine to a particular platform and with the specified attributes.
     * @param attributes Other attributes that the machine must satisfy.
     * @return The bind action.
     * @throws Exception The bind is invalid.
     */
    @Override
    public TestInstance.Action bind(Attributes attributes) throws Exception
    {
        try {
            return super.bind(attributes);
        } catch (Exception e) {
            this.log.error("<internal> Machine.TestInstance.Action.bind(Attributes) exits after catching exception, msg: " + e);
            throw e;
        }
    }

    static private class Deploy extends TestInstance.Action
    {
        private Machine m;
        private Artifact a;
        @SuppressWarnings("unused")
        private Template.ResourceParameter me;
        private Template template;
        private List<Artifact> artifacts;

        private Deploy(Machine m, Artifact a, List<Action> actionDependencies)
        {
            this.m = m;
            this.a = a;
            me = new Template.ResourceParameter(m);
            artifacts = new ArrayList<Artifact>();
            artifacts.add(a);
            if(actionDependencies != null){
                this.actionDependencies.addAll(actionDependencies);
            }
            Action machineBindAction = m.getBindAction();
            if(!this.actionDependencies.contains(machineBindAction)){
                this.actionDependencies.add(machineBindAction);
            }

            synchronized(m.deployActions){
                m.deployActions.put(a, this);
            }
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            if (t == null) {
                // not an IllegalArgument, this happens often when Template t has not been created: return empty command
                // Not put out message- this is too common
//              String msg = "Machine.Deploy.getCommand(Template) sees null Template parameter, returns empty string as command";
//              System.out.println(msg);

                return "";
            }
            try {
                String retStr = getSetID() + " deploy " + t.getReference(this.m) + " ";
                String destName = a.getTargetFilePath();
                if(destName == null || destName.isEmpty()){
                    destName = a.getName();
                }
                retStr += destName;
                retStr += (" " + a.getContent().getValue(template));
                return retStr;
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.Deploy.getCommand(Template) exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public String getDescription() throws Exception
        {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("Copy the file <tt>");
                sb.append(a.getName());
                sb.append("</tt> from module <tt>");
                sb.append(a.getModule().getName());
                sb.append(":");
                sb.append(a.getModule().getVersion());
                sb.append("</tt> to machine <em>");
                sb.append(m.name);
                sb.append("</em>");
                return sb.toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.Deploy.getDescription() exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception
        {
            try {
                return new ArtifactUses("", true, artifacts.iterator());
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.Deploy.getArtifactUses() exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public Resource getBoundResource() throws Exception
        {
            return null;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception
        {
            return null;
        }

    }

    /**
     * Deploy a set of artifacts and their dependencies to a machine.
     * @param artifacts A list of artifacts to deploy. The dependencies of the artifacts are also
     * deployed, recursively.
     * @return A list of deploy actions.
     * @throws Exception The deploy failed.
     */
    public List<TestInstance.Action> deploy(Artifact... artifacts) throws Exception
    {
        if (artifacts == null) {
            String msg = ".deploy() called with null artifacts parameter";
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        try {
            return deploy(null, artifacts);
        } catch (Exception e) {
            this.log.error("<internal> Machine.deploy(Artifact...) exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * Deploy a set of artifacts and their dependencies to a machine.
     * @param actionDependencies A list of actions that should be completed before the deploy.
     * @param artifacts A list of artifacts to deploy. The dependencies of the artifacts are also
     * deployed, recursively.
     * @return A list of deploy actions.
     * @throws Exception The deploy failed.
     */
    public List<TestInstance.Action> deploy(List<Action> actionDependencies, Artifact... artifacts) throws Exception
    {
        if (actionDependencies==null || artifacts==null) {
            String msg = ".deploy() called with null param(s); actionDependencies/artifacts: " + actionDependencies + "/" + artifacts;
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }


        try {
            List<Action> deploys = new ArrayList<Action>();
            if(!isBound()){
                throw new IllegalStateException("Cannot deploy to unbound machine.");
            }
            for (Artifact a : artifacts)
            {
                if (a == null)
                {
                    this.log.error("<internal> Machine.deploy(List<Action>, Artifact...): Artifact is null.");
                    continue;
                }
                synchronized(deployActions){
                    if(deployActions.containsKey(a)){
                        continue; //duplicate
                    }
                }

                Deploy deploy = new Deploy(this, a, actionDependencies);
                generator.add(deploy);
                deploys.add(deploy);

                Iterable<Artifact> dependencies = generator.findDependencies(a);
                for (Artifact d : dependencies)
                {
                    deploys.addAll(deploy(d));
                }
            }
            return deploys;
        } catch (Exception e) {
            this.log.error("<internal> Machine.deploy(List<Action>, Artifact...) exits after catching exception, msg: " + e);
            throw e;
        }

    }

    /**
     * Connect the machine to a {@link Network} Resource.
     *
     * @param network The network to connect to. Must not be null.
     * @return A {@link Cable} which serves as a logical connection between the machine and the network.
     * @throws Exception on any error.
     */
    public Cable connect(Network network) throws Exception
    {
        if (network == null) {
            String msg = ".connect() called with null network param";
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            return connect(network, null);
        } catch (Exception e) {
            this.log.error("<internal> Machine.connect(Network) exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * Connect the machine to a {@link Network} Resource.
     *
     * @param network The network to connect to.
     * @param actionDependencies A list of actions that should be completed before the connect. Must not be null.
     * @return A {@link Cable} which serves as a logical connection between the machine and the network.
     * @throws Exception on any error.
     */
    public Cable connect(Network network, List<Action> actionDependencies) throws Exception
    {
        if (network == null) {
            String msg = ".connect() called with null network parameter";
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            if (!isBound())
            {
                this.log.error("<internal> Machine.connect(): Cannot connect an unbound machine.");
                return null;
            }

            if (!network.isBound())
            {
                this.log.error("<internal> Machine.connect(): Cannot connect a machine to an unbound network.");
                return null;
            }

            ConnectAction action = new ConnectAction(this, network, actionDependencies);
            Cable c = new Cable(generator, this, network, action);
            generator.add(action);

            return c;
        } catch (Exception e) {
            this.log.error("<internal> Machine.connect(Network, List<Action>) exits after catching exception, msg: " + e);
            throw e;
        }
    }

    private static final class ConnectAction extends TestInstance.Action {

        private Machine machine;
        private Network network;
        private Template.Parameter[] parameters;

        private ConnectAction(Machine machine, Network network, List<Action> actionDependencies) {
            this.machine = machine;
            this.network = network;

            parameters = new Template.Parameter[2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.ResourceParameter(network);

            if(actionDependencies != null)
                this.actionDependencies.addAll(actionDependencies);
            Action machineBindAction = machine.getBindAction();
            Action networkBindAction = network.getBindAction();
            if(!this.actionDependencies.contains(machineBindAction))
                this.actionDependencies.add(machineBindAction);
            if(!this.actionDependencies.contains(networkBindAction))
                this.actionDependencies.add(networkBindAction);
        }

        @Override
        public String getCommand(Template t) throws Exception {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(getSetID() + " ");
                sb.append("connect");

                for (Template.Parameter P : parameters) {
                    String value = P.getValue(t);
                    if (value == null) {
                        String msg = "<internal> Machine.ConnectAction.getCommand(Template) finds Template.Parameter with null retrieved value";
                        String detail = t.std_string!=null ? t.std_string : "";
                        LoggerFactory.getLogger(getClass()).error(msg + detail);
                        throw new IllegalStateException(msg);
                    }
                    sb.append(" ");
                    sb.append(value);
                }
                return sb.toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.ConnectAction.getCommand(Template) exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public String getDescription() throws Exception {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("Connect the machine <em>");
                sb.append(machine.name);
                sb.append("</em> to the network <em>");
                sb.append(network.name);
                sb.append("</em>.");
                return sb.toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.ConnectAction.getDescription() exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception {
            return null;
        }

        @Override
        public Resource getBoundResource() throws Exception {
            return null;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception {
            return null;
        }

    }

    private static final class ProgramAction extends TestInstance.Action
    {
        private Machine machine;
        private String action;
        private String executable;
        private String[] params;
        private Template.Parameter[] parameters;

        private ProgramAction(Machine machine, String action, List<Action> actionDependencies, String executable, String... params) {
            this.machine = machine;
            this.action = action;
            this.executable = executable;
            this.params = params;
            parameters = new Template.Parameter[params.length + 2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.StringParameter(executable);
            for (int i = 0; i < params.length; i++) {
                Template.Parameter referenceParameter = machine.generator.getReferencedParameter(params[i]);
                if(referenceParameter != null) {
                    parameters[2 + i] = referenceParameter;
                } else {
                    parameters[2 + i] = new Template.StringParameter(params[i]);
                }
            }

            if(actionDependencies != null)
                this.actionDependencies.addAll(actionDependencies);
            Action machineBindAction = machine.getBindAction();
            if(!this.actionDependencies.contains(machineBindAction))
                this.actionDependencies.add(machineBindAction);
        }

        @Override
        public String getCommand(Template t) throws Exception {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(getSetID());
                sb.append(" ");
                sb.append(action);

                for (Template.Parameter P : parameters) {
                    String value = P.getValue(t);
                    if (value == null) {
                        String msg = "<internal> Machine.ProgramAction.getCommand(Template) finds Template.Parameter with null retrieved value";
                        String detail = t.std_string!=null ? t.std_string : "";
                        LoggerFactory.getLogger(getClass()).error(msg + detail);
                        throw new IllegalStateException(msg);
                    }
                    if (value.length() > 0) {
                        sb.append(" ");
                        sb.append(value);
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.ProgramAction.getCommand(Template) exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public String getDescription() throws Exception
        {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(action.substring(0, 1).toUpperCase() + action.substring(1));
                sb.append(" the program <tt>");
                sb.append(executable);
                sb.append("</tt>");

                if (params.length > 1)
                    sb.append(" with parameters <tt>");
                else
                    sb.append(" with parameter <tt>");

                String sep = "";
                for (String P : params)
                {
                    sb.append(sep);
                    sb.append(P);
                    sep = " ";
                }

                sb.append("</tt> on machine <em>");
                sb.append(machine.name);
                sb.append("</em>.");
                return sb.toString();
            } catch (Exception e) {
                LoggerFactory.getLogger(getClass()).error("<internal> Machine.ProgramAction.getDescription() exits after catching exception, msg: " + e);
                throw e;
            }
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception
        {
            return null;
        }

        @Override
        public Resource getBoundResource() throws Exception
        {
            return null;
        }

        @Override
        public DescribedTemplate getIncludedTemplate() throws Exception
        {
            return null;
        }

    }

    private Program programAction(String action, List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        try {
            ProgramAction programAction = new ProgramAction(this, action, actionDependencies, executable, params);
            Program program = new Program(programAction);
            generator.add(programAction);
            return program;
        } catch (Exception e) {
            this.log.error("<internal> Machine.programAction() exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * The program configure command requests that a program be run that modifies the machine in such a way that it cannot be rolled back and reused.
     *
     * @param actionDependencies A list of actions that must be complete before the command may be executed. Most often deploy actions or other program options.
     * This machine's bind action is automatically added as a dependency.
     * @param executable A string containing the name of an executable program.
     * @param params Any string parameters that should be passed as arguments to the executable program.
     * @return A program.
     * @throws Exception Any error.
     */
    public Program configure(List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        if (actionDependencies==null || executable==null || params==null) {
            String msg = ".configure() called with null param(s); actionDependencies/executable/params: " + actionDependencies + "/" + executable + "/" + params;
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            Program p = programAction("configure", actionDependencies, executable, params);
            return p;
        } catch (Exception e) {
            this.log.error("<internal> Machine.configure() exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * The program start command requests that a program be run that should stay running for the duration of the Template Instance.
     * It cannot modify the Machine.
     *
     * @param actionDependencies A list of actions that must be complete before the command may be executed. Most often deploy actions or other program options.
     * This machine's bind action is automatically added as a dependency. Must not be null.
     * @param executable A string containing the name of an executable program.
     * @param params Any string parameters that should be passed as arguments to the executable program.
     * @return A program.
     * @throws Exception Any error.
     */
    public Program start(List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        if (actionDependencies==null || executable==null || params==null) {
            String msg = ".start() called with null param(s); actionDependencies/executable/params: " + actionDependencies + "/" + executable + "/" + params;
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            Program p = programAction("start", actionDependencies, executable, params);
            return p;
        } catch (Exception e) {
            this.log.error("<internal> Machine.start() exits after catching exception, msg: " + e);
            throw e;
        }
    }

    /**
     * The program run command requests that a program be run that should complete on its own. The run command completes the test run, with the
     * program result determining the test result. This cannot modify the machine. If a test run contains multiple run or run-forever commands,
     * the test run will fail if any of the programs fail.
     *
     * @param actionDependencies A list of actions that must be complete before the command may be executed. Most often deploy actions or other program options.
     * This machine's bind action is automatically added as a dependency.
     * @param executable A string containing the name of an executable program. Must not be null.
     * @param params Any string parameters that should be passed as arguments to the executable program. Must not be null.
     * @return A program.
     * @throws Exception Any error.
     */
    public Program run(List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        if (actionDependencies==null || executable==null || params==null) {
            String msg = ".run() called with null param(s); actionDependencies/executable/params: " + actionDependencies + "/" + executable + "/" + params;
            this.log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            return programAction("run", actionDependencies, executable, params);
        } catch (Exception e) {
            this.log.error("<internal> Machine.run() exits after catching exception, msg: " + e);
            throw e;
        }
    }

    //TODO: Not currently supported.
//    /**
//     * The program run-forever command requests that a program be run that will not complete until told to stop. The run-forever command completes
//     * the test when it completes, with the program result determining the test result. This cannot modify the Machine. If a test run contains multiple
//     * run or run-forever commands, the test run will fail if any of the programs fail.
//     *
//     * @param requiredArtifacts A list of artifacts that must be deployed to the machine before the command may be executed.
//     * @param executable A string containing the name of an executable program.
//     * @param params Any string parameters that should be passed as arguments to the executable program.
//     * @return A program.
//     * @throws Exception Any error.
//     */
//    public Program run_forever(List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
//    {
//
//        return programAction("run-forever", requiredArtifacts, executable, params);
//    }

    @Override
    public String getDescription()
    {
        return "Machine";
    }

}
