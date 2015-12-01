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
     * @param platform The platform of the machine.
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
     * @param platform The platform of the machine.
     * @param attributes Other attributes that the machine must satisfy.
     * @throws Exception The bind is invalid.
     */
    @Override
    public void bind(Attributes attributes) throws Exception
    {
        super.bind(attributes);
    }

    static private class Deploy extends TestInstance.Action
    {
        private Machine m;
        private Artifact a;
        @SuppressWarnings("unused")
        private Template.ResourceParameter me;
        private Template template;
        private List<Artifact> artifacts;
        private List<Action> actionDependencies;

        private Deploy(Machine m, Artifact a)
        {
            this.m = m;
            this.a = a;
            me = new Template.ResourceParameter(m);
            artifacts = new ArrayList<Artifact>();
            artifacts.add(a);
            actionDependencies = new ArrayList<Action>();
            actionDependencies.add(m.getBindAction());
            
            synchronized(m.deployActions){
            	m.deployActions.put(a, this);
            }
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            String retStr = "deploy " + t.getReference(this.m) + " ";
            String destName = a.getName();
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

		@Override
		public List<Action> getActionDependencies() throws Exception {
			 return actionDependencies;
		}
    }

    /**
     * Deploy a set of artifacts and their dependencies to a machine.
     * @param artifacts A list of artifacts to deploy. The dependencies of the artifacts are also
     * deployed, recursively.
     * @throws Exception The deploy failed.
     */
    public void deploy(Artifact... artifacts) throws Exception
    {
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

            generator.add(new Deploy(this, a));

            Iterable<Artifact> dependencies = generator.findDependencies(a);
            for (Artifact d : dependencies)
            {
                deploy(d);
            }
        }
    }

    /**
     * Connect the machine to a {@link Network} Resource.
     * 
     * @param network The network to connect to.
     * @return A {@link Cable} which serves as a logical connection between the machine and the network.
     * @throws Any error.
     */
    public Cable connect(Network network) throws Exception
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

        Cable c = new Cable(generator, this, network);
        generator.add(new ConnectAction(this, network));

        return c;
    }
    
    private static class ConnectAction extends TestInstance.Action{
    	
    	private Machine machine;
    	private Network network;
        Template.Parameter[] parameters;
    	private List<Action> actionDependencies;
    	
    	private ConnectAction(Machine machine, Network network){
    		this.machine = machine;
    		this.network = network;

            parameters = new Template.Parameter[2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.ResourceParameter(network);
    		
    		actionDependencies = new ArrayList<Action>();
    		actionDependencies.add(machine.getBindAction());
    		actionDependencies.add(network.getBindAction());
    	}

		@Override
		public String getCommand(Template t) throws Exception {
			StringBuilder sb = new StringBuilder();
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

		@Override
		public List<Action> getActionDependencies() throws Exception {
			return actionDependencies;
		}
    	
    }

    private class ProgramAction extends TestInstance.Action
    {
        Machine machine;
        String action;
        List<Artifact> requiredArtifacts;
        String executable;
        String[] params;
        Program program = new Program();
        Template.Parameter[] parameters;
        List<Action> actionDependencies;

        public ProgramAction(Machine machine, String action, List<Artifact> requiredArtifacts, String executable, String... params)
        {
            this.machine = machine;
            this.action = action;
            this.requiredArtifacts = requiredArtifacts;
            this.executable = executable;
            this.params = params;
            parameters = new Template.Parameter[params.length + 2];
            parameters[0] = new Template.ResourceParameter(machine);
            parameters[1] = new Template.StringParameter(executable);
            for (int i = 0; i < params.length; i++)
            {
                // If the parameter is a UUID, then check for deferred parameters.
                // TODO: Repair or remove.
                //UUID p = null;
                //try {
                //    p = UUID.fromString( params[i] );
                //}
                //catch ( Exception e ) {
                // Ignore.
                //}

                //if ( p != null && generator.parameters.containsKey( p ) )
                //    parameters[3+i] = generator.parameters.get( p );
                //else
                parameters[2 + i] = new Template.StringParameter(params[i]);
            }
            this.actionDependencies = new ArrayList<Action>();
        	for(Artifact artifact: requiredArtifacts){
        		synchronized (machine.deployActions) {
					if(machine.deployActions.containsKey(artifact)){
						actionDependencies.add(machine.deployActions.get(artifact));
					}
					else{
						throw new IllegalStateException("Required artifact " + artifact.getName() + " not deployed to machine.");
					}
				}
        	}
        }

        @Override
        public String getCommand(Template t) throws Exception
        {
            StringBuilder sb = new StringBuilder();
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

		@Override
		public List<Action> getActionDependencies() throws Exception {
			return actionDependencies;
		}
    }

    private Program programAction(String action, List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
    {
        Program program = new Program();
        generator.add(new ProgramAction(this, action, requiredArtifacts, executable, params));

        /*        Template.Parameter[] parameters = new Template.Parameter[ params.length + 3 ];
                parameters[0] = new Template.ExportParameter( program );
                parameters[1] = new Template.ResourceParameter( this );
                parameters[2] = a;
                for ( int i = 0; i < params.length; i++ ) {
                    // If the parameter is a UUID, then check for deferred parameters.
                    UUID p = null;
                    try {
                        p = UUID.fromString( params[i] );
                    }
                    catch ( Exception e ) {
                        // Ignore.
                    }

                    if ( p != null && generator.parameters.containsKey( p ) )
                        parameters[3+i] = generator.parameters.get( p );
                    else
                        parameters[3+i] = new Template.StringParameter( params[i] );

                    description += "<tt>" + params[i] + "</tt>";
                }

                generator.testResource = generator.testResource.add( description, action, parameters );
                generator.testResource.close();
        */
        return program;
    }

    public Program configure(List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
    {
        Program p = programAction("configure", requiredArtifacts, executable, params);
        //TODO: Dirty?
        return p;
    }

    public Program start(List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
    {
        Program p = programAction("start", requiredArtifacts, executable, params);
        return p;
    }

    public Program run(List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
    {
        return programAction("run", requiredArtifacts, executable, params);
    }

    public Program run_forever(List<Artifact> requiredArtifacts, String executable, String... params) throws Exception
    {
        return programAction("run-forever", requiredArtifacts, executable, params);
    }

    @Override
    public String getDescription()
    {
        return "Machine";
    }
}
