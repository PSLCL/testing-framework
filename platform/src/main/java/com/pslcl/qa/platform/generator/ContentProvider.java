package com.pslcl.qa.platform.generator;

import java.io.InputStream;

/**
 * This interface represents the provider of content.
 */
public interface ContentProvider {
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