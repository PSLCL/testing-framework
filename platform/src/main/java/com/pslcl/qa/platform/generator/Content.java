package com.pslcl.qa.platform.generator;

import java.io.InputStream;

import com.pslcl.qa.platform.Hash;

/**
 * This class represents content. All content is identified by a hash, which allows
 * multiple references to the same content even if other metadata is different.
 */
public interface Content extends Template.Parameter {
    /**
     * Obtain the hash of the contents.
     * @return The hash of the contents.
     */
    Hash getHash();
    
    /**
     * Obtain a stream that will return the content.
     * @return A stream that will return the content.
     */
    InputStream asStream();

    /**
     * Obtain the content as a byte array.
     * @return A byte array containing the content.
     */
    byte[] asBytes();
}
