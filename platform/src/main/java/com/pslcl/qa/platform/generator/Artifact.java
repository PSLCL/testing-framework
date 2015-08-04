package com.pslcl.qa.platform.generator;

/**
 * This class represents an artifact, which is named content that is associated with a component version.
 */
public interface Artifact {
    Module getModule();

    String getConfiguration();

    String getName();

    Content getContent();
}
