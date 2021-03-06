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
    private static final String codename = "machine";
    
    private Map<Artifact, Action> deployActions;

    /**
     * Define a new machine associated with the specified generator and with the given name.
     * @param generator The generator that can use the machine.
     * @param name The name of the machine for logging and debugging.
     */
    public Machine(Generator generator, String name)
    {
        super(generator, name, codename);
        
        deployActions = new HashMap<Artifact, Action>();
    }

    /**
     * Bind a machine to a particular platform.
     * @throws Exception The bind is invalid.
     */
    @Override
    public void bind() throws Exception
    {
        Attributes attributes = new Attributes();
        super.bind(attributes);
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
        return super.bind(attributes);
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
            String retStr = getSetID() + " deploy " + t.getReference(this.m) + " ";
            String destName = a.getTargetFilePath();
            if(destName == null || destName.isEmpty()){
            	destName = a.getName();
            }
            retStr += destName;
            retStr += (" " + a.getContent().getValue(template));
            return retStr;
        }

        @Override
        public String getDescription() throws Exception
        {
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
        }

        @Override
        public ArtifactUses getArtifactUses() throws Exception
        {
            return new TestInstance.Action.ArtifactUses("", true, artifacts.iterator());
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
    	return deploy(null, artifacts);
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
    	List<TestInstance.Action> deploys = new ArrayList<TestInstance.Action>();
    	if(!isBound()){
    		throw new IllegalStateException("Cannot deploy to unbound machine.");
    	}
        for (Artifact a : artifacts)
        {
            if (a == null)
            {
                System.err.println("ERROR: Artifact is null.");
                continue;
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
    }

    /**
     * Connect the machine to a {@link Network} Resource.
     * 
     * @param network The network to connect to.
     * @return A {@link Cable} which serves as a logical connection between the machine and the network.
     * @throws Exception on any error.
     */
    public Cable connect(Network network) throws Exception
    {
         return connect(network, null);
    }
    
    /**
     * Connect the machine to a {@link Network} Resource.
     * 
     * @param network The network to connect to.
     * @param actionDependencies A list of actions that should be completed before the connect.
     * @return A {@link Cable} which serves as a logical connection between the machine and the network.
     * @throws Exception on any error.
     */
    public Cable connect(Network network, List<Action> actionDependencies) throws Exception
    {
        if (!isBound())
        {
            System.err.println("Cannot connect an unbound machine.");
            return null;
        }

        if (!network.isBound())
        {
            System.err.println("Cannot connect a machine to an unbound network.");
            return null;
        }

        ConnectAction action = new ConnectAction(this, network, actionDependencies);
        Cable c = new Cable(generator, this, network, action);
        generator.add(action);

        return c;
    }
    
    private static class ConnectAction extends TestInstance.Action{
    	
    	private Machine machine;
    	private Network network;
        Template.Parameter[] parameters;
        
    	private ConnectAction(Machine machine, Network network, List<Action> actionDependencies){
    		this.machine = machine;
    		this.network = network;

            parameters = new Template.Parameter[2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.ResourceParameter(network);
    		

            if(actionDependencies != null){
            	this.actionDependencies.addAll(actionDependencies);
            }
            Action machineBindAction = machine.getBindAction();
            Action networkBindAction = network.getBindAction();
            if(!this.actionDependencies.contains(machineBindAction)){
            	this.actionDependencies.add(machineBindAction);
            }
            if(!this.actionDependencies.contains(networkBindAction)){
            	this.actionDependencies.add(networkBindAction);
            }
    	}

		@Override
		public String getCommand(Template t) throws Exception {
			StringBuilder sb = new StringBuilder();
			sb.append(getSetID() + " ");
            sb.append("connect");

            for (Template.Parameter P : parameters){
                sb.append(" ");
                sb.append(P.getValue(t));
            }

            return sb.toString();
		}

		@Override
		public String getDescription() throws Exception {
			StringBuilder sb = new StringBuilder();
            sb.append("Connect the machine <em>");
            sb.append(machine.name);
            sb.append("</em> to the network <em>");
            sb.append(network.name);
            sb.append("</em>.");
            return sb.toString();
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

    private class ProgramAction extends TestInstance.Action
    {
        Machine machine;
        String action;
        String executable;
        String[] params;
        Template.Parameter[] parameters;
        
        public ProgramAction(Machine machine, String action, List<Action> actionDependencies, String executable, String... params)
        {
            this.machine = machine;
            this.action = action;
            this.executable = executable;
            this.params = params;
            parameters = new Template.Parameter[params.length + 2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.StringParameter(executable);
            for (int i = 0; i < params.length; i++)
            {
            	Template.Parameter referenceParameter = machine.generator.getReferencedParameter(params[i]);
            	if(referenceParameter != null){
            		parameters[2 + i] = referenceParameter;
            	} else{
            		parameters[2 + i] = new Template.StringParameter(params[i]);
            	}
            }


            if(actionDependencies != null){
            	this.actionDependencies.addAll(actionDependencies);
            }
            Action machineBindAction = machine.getBindAction();
            if(!this.actionDependencies.contains(machineBindAction)){
            	this.actionDependencies.add(machineBindAction);
            }
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getSetID());
            sb.append(" ");
            sb.append(action);

            for (Template.Parameter P : parameters){
            	String value = P.getValue(t);
            	if (value.length() > 0)
                {
                    sb.append(" ");
                    sb.append(value);
                }
            }

            return sb.toString();
        }

        @Override
        public String getDescription() throws Exception
        {
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
    	ProgramAction programAction = new ProgramAction(this, action, actionDependencies, executable, params);
        Program program = new Program(programAction);
        generator.add(programAction);
        return program;
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
        Program p = programAction("configure", actionDependencies, executable, params);
        return p;
    }
    
    /**
     * The program start command requests that a program be run that should stay running for the duration of the Template Instance. 
     * It cannot modify the Machine.
     * 
     * @param actionDependencies A list of actions that must be complete before the command may be executed. Most often deploy actions or other program options. 
     * This machine's bind action is automatically added as a dependency. 
     * @param executable A string containing the name of an executable program.
     * @param params Any string parameters that should be passed as arguments to the executable program.
     * @return A program.
     * @throws Exception Any error.
     */
    public Program start(List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        Program p = programAction("start", actionDependencies, executable, params);
        return p;
    }

    /**
     * The program run command requests that a program be run that should complete on its own. The run command completes the test run, with the 
     * program result determining the test result. This cannot modify the machine. If a test run contains multiple run or run-forever commands, 
     * the test run will fail if any of the programs fail.
     * 
     * @param actionDependencies A list of actions that must be complete before the command may be executed. Most often deploy actions or other program options.
     * This machine's bind action is automatically added as a dependency.  
     * @param executable A string containing the name of an executable program.
     * @param params Any string parameters that should be passed as arguments to the executable program.
     * @return A program.
     * @throws Exception Any error.
     */
    public Program run(List<Action> actionDependencies, String executable, String... params) throws Exception
    {
        return programAction("run", actionDependencies, executable, params);
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
