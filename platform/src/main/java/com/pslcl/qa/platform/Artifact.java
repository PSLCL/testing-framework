package com.pslcl.qa.platform;

import java.net.URLEncoder;

/**
 * This class represents an artifact, which is named content that is associated with a component version.
 */
public class Artifact extends Content {
    private Version version;
    private String platform;
    private String variant;
    private String name;
    private long pk;
    
    /**
     * Construct an artifact associated with a component, name, version, platform and variant. The content
     * associated with the artifact is passed as a hash.
     * @param name The name of the artifact.
     * @param version The version of the artifact. This contains
     * @param platform
     * @param variant
     * @param hash
     */
    public Artifact(long pk, String name, Version version, String platform, String variant, Hash hash) {
        super( hash );
        this.pk = pk;
        this.version = version;
        this.platform = platform;
        this.variant = variant;
        this.name = name;
    }

    long getPK() {
        return pk;
    }
    
    public Version getVersion() {
        return version;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVariant() {
        return variant;
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
        return version.getComponent() + " " + getEncodedName() + " " + getHash();
    }
}
