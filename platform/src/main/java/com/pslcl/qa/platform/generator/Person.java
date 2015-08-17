package com.pslcl.qa.platform.generator;

import java.util.Arrays;

import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.generator.Template.Parameter;

/**
 * This class represents a person, which is a resource that can perform manual inspections.
 */
public class Person extends Resource {
    private static class InspectAction extends TestInstance.Action {
        private Resource inspector;
        private Content body;
        private Artifact[] attachments;

        private InspectAction( Resource inspector, Content body, Artifact[] attachments ) {
            this.inspector = inspector;
            this.body = body;
            this.attachments = attachments;
        }

        /*        @Override
        void apply(DescribedTemplate t) {
            this.t = t.getTemplate();
            t.add( this );
        }
         */
        @Override
        String getCommand( Template t ) throws Exception {
            Template.Parameter[] params = new Template.Parameter[ 2 + attachments.length ];
            params[0] = new Template.ResourceParameter( inspector );
            params[1] = body;
            int i = 2;
            for ( Artifact a : attachments ) {
                params[i++] = a.getContent();
            }

            StringBuilder sb = new StringBuilder();
            sb.append( "inspect" );
            for ( Parameter p : params ) {
                String v = p.getValue( t );
                if ( v.length() > 0 ) {
                    sb.append( " " );
                    sb.append( v );
                }
            }

            return sb.toString();
        }

        @Override
        String getDescription() throws Exception {
            StringBuilder sb = new StringBuilder();
            if ( attachments.length > 0 ) {
                sb.append( "Create an archive of the following files called <tt>attachments.tar.gz</tt>:<ul>" );
                for ( Artifact a : attachments ) {
                    sb.append( "<li><tt>" );
                    sb.append( a.getName() );
                    sb.append( "</tt> from " );
                    sb.append( a.getModule().getName() );
                    sb.append( ":" );
                    sb.append( a.getModule().getVersion() );
                    sb.append( "</li>" );
                }

                sb.append( "</ul>\n" );

                sb.append( "Send <tt>attachments.tar.gz</tt> to <em>" );
                sb.append( inspector.name );
                sb.append( "</em> with the following instructions:\n");
            }
            else {
                sb.append( "<li>Have <em>" );
                sb.append( inspector.name );
                sb.append( "</em> follow these directions:" );
            }

            sb.append( body.asBytes() );

            return sb.toString();
        }

        @Override
        ArtifactUses getArtifactUses() throws Exception {
            return new ArtifactUses( "inspection", true, Arrays.asList( attachments ).iterator() );
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

    private static final Hash hash = Hash.fromContent( "person" );

    /**
     * Define a new person for use by a generator and with a given name.
     * @param generator The generator that can use the person.
     * @param name The name of the person for logging and debugging.
     */
    public Person(Generator generator, String name) {
        super( generator, name, hash );
    }

    /**
     * Have a person inspect artifacts according to specified directions.
     * @param body Content that contains an HTML snippet that will be included in the instructions
     * to the person performing the inspection.
     * @param attachments Artifacts that will be passed to the inspector.
     * @throws Exception If the inspection request is invalid.
     */
    public void inspect( Content body, Artifact[] attachments ) throws Exception {
        generator.add( new InspectAction( this, body, attachments ) );
    }

    @Override
    public String getDescription() {
        return "Person";
    }
}
