#Artifacts

Artifacts are files produced by modules. Each artifact is identified by a module, name, and configuration, and has a content-based hash.

##Modules

Modules in the Testing Framework are similar conceptually to Ivy or Maven modules. They are self-contained sets of artifacts.

Each Module provides the following information:

* Organization - The organization that owns the module.
* Name - The name of the module.
* Version - The version of the module.
* Sequence - The sequence of the module, used in addition to the version in order to differentiate versions of a module. Sequences are ordered alphabetically, with the latest being the most recent.
* Attributes - A set of key-value pairs providing additional information about a module.
* Artifacts - A set of artifacts associated with the module.

##Artifact Providers

Artifact providers implement the [Core Platform's](core_platform.md) ArtifactProvider Java interface, providing a set of
modules which can be iterated by the Core Platform.

Artifact providers also provide information about modules which may be merged on the local cache.

##Artifact Cache

A local artifact cache may be built using the [dtfexec synchronize](dtfexec.md#a-user-synchronizes-the-local-artifact-cache) functionality.

Typically this is done by the [QA Portal](qa_portal.md) which provides a Rest API for downloading artifacts.

##Test Generator Artifacts

Artifacts which contain [Test Generators](test_generators.md) should have their configuration set to "dtf_test_generator". This enables 
[dtfexec](dtfexec.md) command to identify and execute test generators provided by an artifact provider.

##Artifact Dependencies

Artifacts may specify dependencies by listing them in another artifact named `<artifact_name>.dep`. When the artifact is deployed, all of its dependencies will also be deployed. Each line contains a reference to an artifact in one of the following formats:

* The name of an artifact in the same module.
* Three fields separated by commas:
	1. A module reference, specified as 'org#module#attributes'.
	2. A version, specified as 'version/configuration'.
	3. The name of the artifact.

### Dependency Artifact Name Matching

Dependency artifact names are matched using the MySQL REGEXP comparitor. See the MySQL documentation for valid formatting.

### Artifact Field Substitution

If a dependency shares any part of their module reference, version or name with the dependent artifact, it may be substituted using the `$` character.

### Dependency Examples:

Consider an artifact named 'artifact.jar' and corresponding dependency artifact named 'artifact.jar.dep' with the following possible dependencies listed.

1. `artifact2.jar` - artifact.jar depends on artifact2.jar in the same module.
2. `org.mycompany#mymodule#,1.0/,artifact3.jar` - artifact.jar depends on artifact3.jar in the module mymodule with organization org.mycompany and version 1.0.
3. `$#mymodule2#,$/$,artifact4.jar` - artifact.jar depends on artifact4.jar in the module mymodule2 with artifact.jar's same organization, version and configuration.

