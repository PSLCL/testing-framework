package com.pslcl.qa.platform.generator;

import java.util.List;
import java.util.Map;

/**
 * This interface represents a module, which in turn is a set of artifacts. Modules follow the
 * definition used by Ivy, which is similar to Maven. Each module is uniquely identified by the
 * set of fields that can be returned.
 */
public interface Module {
    /**
     * Return the name of the organization that owns the module. This is usually a reverse domain representation,
     * but there is no requirement that this be the case.
     * @return The organization.
     */
    public String getOrganization();
    
    /**
     * Return the name of the module.
     * @return The module name.
     */
    public String getName();
    
    /**
     * Return the version of the module. This is usually a dotted version, but this isn't required.
     * @return The version.
     */
    public String getVersion();
    
    /**
     * Return the sequence of the module. The sequence is used in addition to the version in order to differentiate
     * versions of a module. The sequence must sort all instances of a single version into increasing order, with
     * the latest being the most recent. The sort is done as strings, not as numbers.
     * @return The unique sequence of a version to differentiate multiple instances.
     */
    public String getSequence();
    
    /**
     * Return a set of additional attributes that apply to the module. This can be used by the generator
     * to search for different modules. For example, if there is an associated platform for a module
     * then there could be a "platform" attribute. In general the attributes have meaning only between
     * a provider and the generators associated with that provider.
     * @return
     */
    public Map<String,String> getAttributes();
    
    /**
     * Return a list of all artifacts associated with the module.
     * @return A list of all associated artifacts.
     */
    public List<Artifact> getArtifacts();
    
    /**
     * Return a list of all artifacts associated with the module that match the specified pattern.
     * The patterns allowed follow the MySQL REGEX patterns, which is a subset of the Java util.regex syntax.
     * @param namePattern The pattern that the artifact name must match to be included in the result.
     * @return The list of matching artifacts.
     */
    public List<Artifact> getArtifacts( String namePattern );
    
    /**
     * Return a list of artifacts with the given name pattern and in the specified configuration.
     * @param namePattern The pattern that the artifact name must match to be included in the result.
     * @param configuration The configuration that the artifact must be included in.
     * @return The list of matching artifacts.
     */
    public List<Artifact> getArtifacts( String namePattern, String configuration );
}