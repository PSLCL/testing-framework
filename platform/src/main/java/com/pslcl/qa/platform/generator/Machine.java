package com.pslcl.qa.platform.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents a machine, which is a resource that can accept artifacts and
 * run programs.
 */
public class Machine extends Resource {
    private static final Hash hash = Hash.fromContent( "machine" );

    /**
     * Define a new machine associated with the specified generator and with the given name.
     * @param generator The generator that can use the machine.
     * @param name The name of the machine for logging and debugging.
     */
    public Machine( Generator generator, String name ) {
        super(generator, name, hash);
    }

    /**
     * Bind a machine to a particular platform.
     * @param platform The platform of the machine.
     * @throws Exception The bind is invalid.
     */
    public void bind( String platform ) throws Exception {
        Attributes attributes = new Attributes();
        attributes.put( "platform", platform );
        super.bind( attributes );
    }

    /**
     * Bind a machine to a particular platform and with the specified attributes.
     * @param platform The platform of the machine.
     * @param attributes Other attributes that the machine must satisfy.
     * @throws Exception The bind is invalid.
     */
    public void bind( String platform, Attributes attributes ) throws Exception {
        Attributes N = new Attributes( attributes.toString() );
        N.put( "platform", platform );
        super.bind( N );
    }

    static private class Deploy extends TestInstance.Action {
        private Machine m;
        private Artifact a;
        private Template.ResourceParameter me;
        private Template template;
        private List<Artifact> artifacts;

        private Deploy( Machine m, Artifact a ) {
            this.m = m;
            this.a = a;
            me = new Template.ResourceParameter( m );
            artifacts = new ArrayList<Artifact>();
            artifacts.add( a );
        }

        @Override
        String getCommand( Template t ) throws Exception {
            return "deploy " + t.getReference( this.m ) + " " + a.getValue(template);
        }

        @Override
        String getDescription() throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append( "Copy the file <tt>" );
            sb.append( a.getName() );
            sb.append( "</tt> from version <tt>" );
            sb.append( a.getVersion().getComponent() );
            sb.append( ":" );
            sb.append( a.getVersion().getVersion() );
            sb.append( "</tt> to machine <em>" );
            sb.append( m.name );
            sb.append( "</em>" );
            return sb.toString();
        }

        @Override
        ArtifactUses getArtifactUses() throws Exception {
            return new TestInstance.Action.ArtifactUses( "", true, artifacts.iterator() );
        }

        @Override
        Resource getBoundResource() throws Exception {
            return null;
        }

        @Override
        DescribedTemplate getIncludedTemplate() throws Exception {
            return null;
        }
    }

    /**
     * Deploy a set of artifacts and their dependencies to a machine.
     * @param artifacts A list of artifacts to deploy. The dependencies of the artifacts are also
     * deployed, recursively.
     * @throws Exception The deploy failed.
     */
    public void deploy( Artifact ... artifacts ) throws Exception {
        for ( Artifact a : artifacts ) {
            if ( a == null ) {
                System.err.println( "ERROR: Artifact is null." );
                continue;
            }
            
            generator.add( new Deploy( this, a ) );

            Iterable<Artifact> dependencies = generator.findDependencies( a );
            for ( Artifact d : dependencies ) {
                deploy( d );
            }
        }
    }

    public Cable attach( Network n ) {
        //TODO: Repair
        /*        // If the testResource doesn't include the machine, it must be added.
        if ( ! generator.testResource.contains( this ) ) {
            generator.testResource = generator.testResource.add( this );
        }

        // If the testResource doesn't include the network, it must be added.
        if ( ! generator.testResource.contains( n ) ) {
            generator.testResource = generator.testResource.add( n );
        }

        Cable c = new Cable(generator, this, n );
        //TODO: Build description.
        generator.testResource = generator.testResource.add(
                "Attach machine to network.\n",
                "connect",
                new Template.ExportParameter( c ),
                new Template.ResourceParameter( this ),
                new Template.ResourceParameter( n ) );

        return c;
         */    
        return null;
    }

    private class ProgramAction extends TestInstance.Action {
        Machine machine;
        String action;
        Artifact artifact;
        String[] params;
        Program program = new Program();
        Template.Parameter[] parameters;
        
        public ProgramAction( Machine machine, String action, Artifact artifact, String ... params ) {
            this.machine = machine;
            this.action = action;
            this.artifact = artifact;
            this.params = params;
            parameters = new Template.Parameter[ params.length + 3 ];
            parameters[0] = new Template.ExportParameter( program );
            parameters[1] = new Template.ResourceParameter( machine );
            parameters[2] = artifact;
            for ( int i = 0; i < params.length; i++ ) {
                // If the parameter is a UUID, then check for deferred parameters.
                UUID p = null;
                //try {
                //    p = UUID.fromString( params[i] );
                //}
                //catch ( Exception e ) {
                    // Ignore.
                //}

                //if ( p != null && generator.parameters.containsKey( p ) )
                //    parameters[3+i] = generator.parameters.get( p );
                //else
                parameters[3+i] = new Template.StringParameter( params[i] );
            }
        }
        
        @Override
        String getCommand(Template t) throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append( action );
            
            for ( Template.Parameter P : parameters )
                sb.append( P.getValue( t ) );

            return sb.toString();
        }

        @Override
        String getDescription() throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append( action.substring(0,1).toUpperCase() + action.substring(1) );
            sb.append( " the program <tt>" );
            sb.append( artifact.getName() );
            sb.append( "</tt>" );
            
            if ( params.length > 1 )
                sb.append( " with parameters <tt>" );
            else
                sb.append( " with parameter <tt>" );
            
            String sep = "";
            for ( String P : params ) {
                sb.append( sep );
                sb.append( P );
                sep = " ";
            }
            
            sb.append( "</tt> on machine <em>" );
            sb.append( machine.name );
            sb.append( "</em>.");
            return sb.toString();
        }

        @Override
        ArtifactUses getArtifactUses() throws Exception {
            return null;
        }

        @Override
        Resource getBoundResource() throws Exception {
            return null;
        }

        @Override
        DescribedTemplate getIncludedTemplate() throws Exception {
            return null;
        }
    }
    
    private Program programAction( String action, Artifact a, String ... params ) throws Exception {
        Program program = new Program();

        generator.add( new ProgramAction( this, action, a, params ) );
        
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

    public Program configure( Artifact a, String ... params ) throws Exception {
        Program p = programAction( "configure", a, params );
        //TODO: Dirty?
        return p;
    }

    public Program start( Artifact a, String ... params ) throws Exception {
        Program p = programAction( "start", a, params );
        return p;
    }

    public Program run( Artifact a, String ... params ) throws Exception {
        return programAction( "run", a, params );
    }

    public Program run_forever( Artifact a, String ... params ) throws Exception {
        return programAction( "run-forever", a, params );
    }

    public String getDescription() {
        return "Machine";
    }
}
