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