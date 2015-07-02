package com.pslcl.qa.platform.generator;

import java.util.UUID;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents a resource. Resources can represent any shared object, and are identified
 * by a type (hash) and a set of attributes. Resources are bound to become an instance of the resource.
 * At run time the resource may present additional attributes, but will always have at least the
 * attributes specified in the bind request.
 */
abstract class Resource {
    /**
     * This class represents an action that requests a resource be bound.
     */
    private static class BindAction extends TestInstance.Action {
        private Resource r;
        private Attributes a;

        private BindAction( Resource r, Attributes a ) {
            this.r = r;
            this.a = a;
        }

        @Override
        String getCommand( Template t ) throws Exception {
            return String.format("bind %s %s", r.getHash().toString(), a.toString());
        }

        @Override
        String getDescription() throws Exception {
            StringBuilder sb = new StringBuilder();
            sb.append( "Assign a resource <tt>" );
            sb.append( r.getHash().toString() );
            sb.append( "</tt> (" );
            sb.append( r.getDescription() );
            sb.append( ")" );
            if ( a != null && a.map != null && a.map.size() > 0 ) {
                sb.append( " having attributes <tt>" );
                sb.append( a );
                sb.append( "</tt>" );
            }
            
            sb.append( " and call it <em>" );
            sb.append( r.name );
            sb.append( "</em>." );
            return sb.toString();
        }

        @Override
        ArtifactUses getArtifactUses() throws Exception {
            return null;
        }

        @Override
        Resource getBoundResource() throws Exception {
            return r;
        }

        @Override
        DescribedTemplate getIncludedTemplate() throws Exception {
            return null;
        }

    }

    Generator generator;
    long pk;
    String name;
    Hash hash;
    UUID instance;

    Resource(Generator generator, String name, Hash hash) {
        this.instance = UUID.randomUUID();
        this.generator = generator;
        this.name = name;
        this.hash = hash;
        generator.core.findResource( this );
        if ( pk == 0 )
            System.err.println( "ERROR: Resource " + hash.toString() + " not in database." );
    }

    long getPK() {
        return pk;
    }
    
    /**
     * Bind a resource with no attributes. Each resource must be bound in each test instance before it may be used.
     * @throws Exception The bind failed.
     */
    public void bind() throws Exception {
        bind( new Attributes() );
    }

    /**
     * Bind a resource. Each resource must be bound in each test instance before it may be used.
     * @param attributes The attributes that the resource must have to be acceptable.
     * @throws Exception The bind failed.
     */
    public void bind( Attributes attributes ) throws Exception {
        generator.add( new BindAction( this, attributes ) );
        generator.core.findResource( this );
        
        if ( Generator.trace )
            System.err.println(String.format("Resource %s (%s) (%s) bound with attributes '%s'.", name, hash, instance, attributes));
    }

    /**
     * Return the hash that identifies the type of the resource. Each resource type is associated
     * with a set of resource providers that know how to create instances of the resource type.
     * @return The unique hash of the resource type.
     */
    public Hash getHash() {
        return hash;
    }
    
    /**
     * Return a short description of the resource to be used in documentation.
     * @return The short name of the resource.
     */
    public abstract String getDescription();
}
