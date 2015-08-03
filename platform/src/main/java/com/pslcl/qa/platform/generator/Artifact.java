package com.pslcl.qa.platform.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.commons.compress.utils.IOUtils;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents an artifact, which is named content that is associated with a component version.
 */
public class Artifact {
    private Module module;
    private String configuration;
    private String name;
    private Hash hash;
    private ContentProvider provider;
    
    /**
     * Construct an artifact associated with a component, name, version, platform and variant. The content
     * associated with the artifact is passed as a hash.
     * @param name The name of the artifact.
     * @param version The version of the artifact. This contains
     * @param platform
     * @param variant
     * @param hash
     */
    public Artifact( Module module, String configuration, String name, Hash hash ) {
        this.module = module;
        this.configuration = configuration;
        this.name = name;
        this.hash = hash;
    }

    Artifact( Module module, String configuration, String name, ContentProvider provider ) {
        this.module = module;
        this.configuration = configuration;
        this.name = name;
        this.provider = provider;
        
    }
    
    public Hash getHash( Core core ) {
        if ( hash != null )
            return hash;
        
        if ( provider == null )
            return null;
        
        try {
            File a = File.createTempFile("artifact", "");
            FileOutputStream os = new FileOutputStream( a );
            InputStream is = provider.asStream();
            IOUtils.copy(is, os);
            is.close();
            os.close();

            Hash h = Hash.fromContent( a );
            core.addContent( h, a );
            hash = h;
            provider = null;
        }
        catch ( Exception e ) {
            
        }
        
        return hash;
    }
    
    public Hash getHash() {
        return hash;
    }
    
    public Content getContent() {
        return new Content( hash );
    }
    
    public Module getModule() {
        return module;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getName() {
        return name;
    }

    public String getEncodedName() {
        try {
            return URLEncoder.encode(name, "UTF-8");
        }
        catch ( Exception e ) {
            // This should never happen, as UTF-8 is a required charset.
            return "error";
        }
    }

    public String getValue( Template template ) {
        return module.getOrganization() + "#" + module.getName() + " " + getEncodedName() + " " + getHash();
    }
}
