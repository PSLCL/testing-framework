package com.pslcl.qa.platform.generator;

import java.io.InputStream;
import java.util.Set;

import com.pslcl.qa.platform.Hash;

/**
 * This interface represents providers of artifacts. Artifact providers will return sets of components,
 * versions, platforms, and artifacts. It is assumed that the results of these methods may change over
 * time, and so the artifact provider should periodically be polled.
 */
public interface ArtifactProvider {
    /**
     * This interface represents an asynchronous notification that is called for each artifact.
     */
    public interface ArtifactNotifier {
        /**
         * Notify the implementer that an artifact exists. The implementation should be thread-safe and
         * handle multiple simultaneous notifications.
         * @param project The name of the project.
         * @param component The name of the component.
         * @param version The name of the version.
         * @param platform The name of the platform.
         * @param internal_build The name of the internal build.
         * @param name The name of the artifact.
         * @param hash The hash of the content.
         * @param content The provider of the content. If the content is needed then this can be used.
         */
        void artifact( String project, String component, String version, String platform, String internal_build, String name, Hash hash, Content content );
    }

    /**
     * This interface represents the provider of content.
     */
    public interface Content {
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

    /**
     * This interface represents an asynchronous notification that is called for each generator.
     */
    public interface GeneratorNotifier {
        /**
         * Notify the implementer that a generator exists. The implementation should be thread-safe and
         * handle multiple simultaneous notifications.
         * @param name The unique name of the generator.
         * @param content The content provider of the generator.
         */
        void generator( String name, Content content );
    }

    /**
     * Initialize the artifact provider. This should be paired with a call to close(). The same instance may be reused if close()
     * is called and then followed by another call to init().
     * @throws Exception Thrown if the artifact provider cannot be initialized. This can indicate either permanent or temporary failures.
     */
    void init() throws Exception;

    /**
     * Obtain a set of components given a project.
     * @param project The project to return components of.
     * @return A set of components. This assumes a relatively small number of components (up to thousands).
     */
    Set<String> getComponents( String project );

    /**
     * Obtain a set of versions for a project and component.
     * @param project The project to limit results to.
     * @param component The component to return versions of.
     * @return A set of versions. This assumes a relatively small number of versions (up to thousands).
     */
    Set<String> getVersions( String project, String component );

    /**
     * Obtain a set of platforms for a project, component, and version.
     * @param project The project to limit results to.
     * @param component The component to limit results to.
     * @param version The version to return platforms of.
     * @return A set of platforms. This assumes a relatively small number of platforms (hundreds).
     */
    Set<String> getPlatforms( String project, String component, String version );

    /**
     * Iterate over the artifacts of a project, component, version and platform combination. This routine will not
     * return until all of the artifacts have been passed to the callback. Synchronization of the data structures
     * holding the artifacts is the responsibility of the caller.
     * @param project The project to limit results to.
     * @param component The component to limit results to.
     * @param version The version to limit results to.
     * @param platform the platform to return artifacts of.
     * @param callback A callback that will be called with each artifact.
     */
    void iterateArtifacts( String project, String component, String version, String platform, ArtifactNotifier callback );

    /**
     * Iterate over the generators associated with an artifact provider. This routine will not
     * return until all of the generators have been passed to the callback. Synchronization of the data structures
     * holding the generators is the responsibility of the caller.
     * @param callback A callback that will be called with each generator.
     */
    void iterateGenerators( GeneratorNotifier callback );

    /**
     * Close an artifact provider. The same instance may be reused if init() is called again.
     */
    void close();
}
