package com.pslcl.qa.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.pslcl.qa.platform.TestInstance.Action;

/**
 * This class represents the combination of a set of versions, a template, and documentation. It is the primary
 * class that is synchronized between the generators and the database. Each TestInstance class is related to exactly
 * one DescribedTemplate, although the DescribedTemplates themselves form a tree.
 */
public class DescribedTemplate {
    static class Key {
        private Hash template;
        private Hash versions;
        
        Key( Hash template, Hash versions ) {
            this.template = template;
            this.versions = versions;
        }
        
        public Hash getTemplateHash() {
            return template;
        }
        
        public Hash getVersionHash() {
            return versions;
        }
        
        public int hashCode() {
            return template.hashCode() + versions.hashCode();
        }
        
        public boolean equals( Object o ) {
            if ( ! (o instanceof Key) )
                return false;
            
            Key that = (Key) o;
            return this.template.equals( that.template ) && this.versions.equals( that.versions );
        }
    }
    
    /**
     * The primary database key for this described template.
     */
    private long pk;
    
    /**
     * The primary java key for this described template.
     */
    private Key key;
    
    /**
     * Contains the template that is described.
     */
    private Template template;
    
    /**
     * Contains the list of actions that are represented by the template.
     */
    private List<TestInstance.Action> actions;
    
    /**
     * Contains a list of dependencies.
     */
    private List<DescribedTemplate> dependencies;

    private String documentation;
    private Hash documentationHash;
    private String versionDescription;
    
    private Hash buildDocumentation() {
        StringBuilder sb = new StringBuilder();
        
        List<String> versionsUsed = new ArrayList<String>();
        for ( TestInstance.Action A : actions ) {
            try {
                sb.append( A.getDescription() );
                
                TestInstance.Action.ArtifactUses au = A.getArtifactUses(); 
                Iterator<Artifact> iter = au.getArtifacts();
                while ( iter.hasNext() ) {
                    Artifact a = iter.next();
                    Version v = a.getVersion();
                    String vstr = v.toString();
                    if ( ! versionsUsed.contains( vstr ) )
                        versionsUsed.add( vstr );
                }
            }
            catch ( Exception e ) {
                sb.append( "ERROR: " + e.getMessage() );
            }
            
            sb.append( '\n' );
        }

        Collections.sort( versionsUsed );
        
        documentation = sb.toString();
        documentationHash = Hash.fromContent( documentation );
        
        sb = new StringBuilder();
        StringBuilder sbh = new StringBuilder();
        for ( String v : versionsUsed ) {
            sb.append( '\t' );
            sb.append( v );
            sb.append( '\n' );
            sbh.append( v );
            sbh.append( '\n' );
        }
        
        versionDescription = sb.toString();
        return Hash.fromContent( sbh.toString() );
    }
    
    /**
     * Construct a described template from existing information. The described template is identified by
     * two hashes - the template hash and the documentation hash. These are used to be able to independently 
     * determine if the template or documentation needs to be synchronized.
     * @param template The template that is described, which is already completely created.
     * @param actions The actions represented by the template, in the correct sorted order.
     * @param dependencies The dependencies of the template.
     */
    public DescribedTemplate( Template template, List<TestInstance.Action> actions, List<DescribedTemplate> dependencies ) {
        this.template = template;
        this.actions = actions;
        this.dependencies = dependencies;
        
        Hash versionHash = buildDocumentation();
        this.key = new Key( template.hash, versionHash );
    }

    public Template getTemplate() {
        return template;
    }

    List<DescribedTemplate> getDependencies() {
        return dependencies;
    }
    
    public int getActionCount() {
        return actions.size();
    }

    public TestInstance.Action getAction( int index ) {
        return actions.get( index );
    }

    long getPK() {
        // TODO: Implement
        return 0;
    }

    Key getKey() {
        return key;
    }
    
    Hash getDocumentationHash() {
        return documentationHash;
    }
    
    void sync( Core core ) throws Exception {
        
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append( "Template Hash: " );
        sb.append( template.getHash().toString() );
        sb.append( '\n' );

        sb.append( "Version Hash: " );
        sb.append( key.versions.toString() );
        sb.append( '\n' );
        
        sb.append( "Documentation Hash: " );
        sb.append( documentationHash.toString() );
        sb.append( '\n' );
        
        sb.append( "Resource used:\n" );
        for ( Action a : actions ) {
            Resource R = null;
            try {
                R = a.getBoundResource();
            }
            catch (Exception e) {
                // Ignore
            }
            
            if ( R != null ) {
                sb.append( '\t' );
                sb.append( R.getHash().toString() );
                sb.append( '\n' );
            }
        }

        sb.append( "Depends on:\n" );
        for ( DescribedTemplate dt : dependencies ) {
            sb.append( '\t' );
            sb.append( dt.getTemplate().getHash().toString() );
            sb.append( '\n' );
        }
        
        sb.append( "Versions:\n" );
        sb.append( versionDescription );
        sb.append( "Steps:\n" );
        sb.append( template.toStandardString() );
        
        sb.append( "Description:" );
        sb.append( '\n' );
        sb.append( "<html><body><ol>\n" );

        sb.append( documentation );
        
        sb.append( "</ol></body></html>\n" );
        
        return sb.toString();
    }
    
    public boolean equals( Object o ) {
        if ( ! (o instanceof DescribedTemplate) )
            return false;
        
        DescribedTemplate that = (DescribedTemplate) o;
        return this.key.equals( that.key )
                && this.documentationHash.equals( that.documentationHash );
    }
    
    public int hashCode() {
        return this.key.hashCode()
                + this.documentationHash.hashCode();
    }
}
