#Test Generators

Generators are ways of generating [templates](templates.md) and associated test runs to be executed at some point in the future. A single generator 
may create multiple templates and test runs, possibly with only minor differences.

Templates created by Generators may be specific to a particular [module](artifacts.md#modules) or set of Modules, depending on the needs of the
test. However, templates may also apply to multiple modules. For example, consider a generator that creates a test(template) to verify that a
particular [artifact](artifacts.md) is present. If that artifact needs to be present in 10 different modules each with 10 different versions, the
generator only needs to create a single template which would apply to all 100 module version.

##Creating Test Generators

Test Generators utilize the Java API provided by the [Core Platform](core_platform.md).

##Executing Test Generators

Test Generators that are provided by an Artifact Provider as a [Test Generator Artifact](artifacts.md#test-generator-artifacts)
can be discovered by the Core Platform and executed using the [dtfexec](dtfexec.md) command line tool.