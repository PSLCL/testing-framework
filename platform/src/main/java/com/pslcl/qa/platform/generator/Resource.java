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
package com.pslcl.qa.platform.generator;

import java.util.Map;
import java.util.UUID;

import com.pslcl.qa.platform.Attributes;
import com.pslcl.qa.platform.Hash;
import com.pslcl.qa.platform.generator.Template.AttributeParameter;

/**
 * This class represents a resource. Resources can represent any shared object, and are identified
 * by a type (hash) and a set of attributes. Resources are bound to become an instance of the resource.
 * At run time the resource may present additional attributes, but will always have at least the
 * attributes specified in the bind request.
 */
abstract class Resource  {
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
            if ( a != null && a.getAttributes().size() > 0 ) {
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
    BindAction bound;
    private Map<String, String> attributeMap;

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
        if ( bound != null ) {
            System.err.println( "Cannot bind the same resource twice." );
            return;
        }
        
        bound = new BindAction( this, attributes );
        generator.add( bound );
        generator.core.findResource( this );
        this.attributeMap = attributes.getAttributes();
        if ( Generator.trace )
            System.err.println(String.format("Resource %s (%s) (%s) bound with attributes '%s'.", name, hash, instance, attributes));
    }

    /**
     * Return whether the resource is bound.
     * @return True if the resource is bound, false otherwise.
     */
    public boolean isBound() {
        return bound != null;
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
    
    /**
     * Get a reference to an attribute whose value may be used as a parameter in a program action.
     * 
     * This reference will be resolved by the Generator when the Template is generated. If the value of
     * the attribute is known at that time, then the reference will be replaced by the value. If the value
     * of the attribute will not be known until the test is run, then the reference will be replaced by
     * a value reference in the form of $(attribute <resource-ref> <attribute-name>).
     * 
     * @param attributeName The name of the attribute.
     * @return a String reference to the attribute.
     */
    public String getAttributeReference(String attributeName){
    	String uuid = UUID.randomUUID().toString().toUpperCase();
    	AttributeParameter attribute;
    	if(attributeMap != null && attributeMap.containsKey(attributeName)){
    		attribute = new AttributeParameter(attributeName, attributeMap.get(attributeName));
    	}
    	else{
    		attribute = new AttributeParameter(this, attributeName);
    	}
    	generator.addParameterReference(uuid, attribute);
    	return uuid;
    }
}
