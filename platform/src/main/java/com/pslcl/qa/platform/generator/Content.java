package com.pslcl.qa.platform.generator;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents content. All content is identified by a hash, which allows
 * multiple references to the same content even if other metadata is different.
 */
public class Content implements Template.Parameter {
    private Core core = null;
    Hash hash;
    private String content = null;

    Content() {
    }

    Content( Hash hash ) {
        this.hash = hash;
    }

    Content( Core core, String content ) {
        this.core = core;
        this.content = content;
        this.hash = Hash.fromContent( content );
    }

    public Hash getHash() {
        return hash;
    }

    public String getContent() {
        if ( content != null )
            return content;

        File f = core.getContentFile( hash );
        try {
            content = FileUtils.readFileToString( f );
            return content;
        }
        catch ( Exception e ) {
            // Ignore
        }
        
        return null;
    }

    public String getValue( Template template ) {
        return getHash().toString();
    }

    public void sync() {
        if ( core != null )
            core.syncGeneratedContent( this );
    }
}
