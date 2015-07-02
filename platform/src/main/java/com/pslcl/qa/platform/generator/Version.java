package com.pslcl.qa.platform.generator;

import java.util.List;
import java.util.Set;

/**
 * This class represents a version, which is a set of artifacts with a version name and related
 * component. This class also represents the database identifiers for the version and component.
 */
public class Version {
    private Core core;
    private String component;
    private String version;
    private long pk;
    private long fk_component;

    public String toString() {
        return component + ":" + version;
    }

    public boolean equals( Object o ) {
        if ( ! (o instanceof Version) )
            return false;

        Version ver = (Version) o;
        String C = ver.getComponent();
        String V = ver.getVersion();

        if ( ! component.equals( C ) )
            return false;
        if ( ! version.equals( V ) )
            return false;

        return true;
    }

    public int hashCode() {
        return component.hashCode() + version.hashCode();
    }

    /**
     * Construct a version given a core, component, and version strings.
     * @param core The core to use for database access.
     * @param component The component name.
     * @param version The version name.
     */
    Version(Core core, String component, String version) {
        this.core = core;
        this.component = component;
        this.version = version;
    }

    /**
     * Return the name of the component associated with the version.
     * @return The component string associated with the version.
     */
    public String getComponent() {
        return component;
    }

    /**
     * Return the name of the version.
     * @return The name of the version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Return a set of platforms that are associated with the version.
     * @return A set of platform strings.
     */
    public Set<String> getPlatforms() {
        return core.getPlatforms( component, version );
    }

    /**
     * Return the artifact with a specified name for the specified platform. The name may
     * contain SQL wildcards, but only the first match is returned.
     * @param platform The platform to search in.
     * @param name The name of the artifact to return, which may contain SQL wildcards.
     * @return The first matching artifact.
     */
    public Artifact getArtifact( String platform, String name ) {
        List<Artifact> result = getArtifacts( platform, name );
        if ( result.size() == 0 )
            return null;

        return result.get(0);
    }

    /**
     * Return a list of artifacts from the specified platform that match the specified name.
     * @param platform The platform to search in.
     * @param name The name of the artifacts to return, which may contain SQL wildcards.
     * @return The list of matching artifacts.
     */
    public List<Artifact> getArtifacts( String platform, String name ) {
        return core.getArtifacts( version, platform, name );
    }

    /**
     * Return a list of all artifacts associated with a platform.
     * @param platform The platform to restrict matches to.
     * @return The list of artifacts.
     */
    public List<Artifact> getArtifacts( String platform ) {
        return core.getArtifacts( version, platform, null );
    }

    long getPK() {
        return pk;
    }

    long getComponentPK() {
        return fk_component;
    }

    boolean isAssociatedWithTest() {
        sync();
        boolean result = core.isAssociatedWithTest( this );
        return result;
    }

    void sync() {
        if ( pk == 0 ) {
            fk_component = core.findComponent( component );
            pk = core.findVersion( fk_component, version );
        }
    }
}
