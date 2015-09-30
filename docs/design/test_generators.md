#Test Generators

Generators are ways of creating [Templates](templates.md) and associated test runs to be executed at some point in the future. A single generator 
may create multiple templates and test runs, possibly with only minor differences.

Templates created by generators may be specific to a particular [Module](artifacts.md#modules), depending on the needs of the test. However, 
templates may also apply more broadly to multiple modules. For example, consider a generator that creates a test(template) to verify that a
particular [Artifact](artifacts.md) is present. If that artifact needs to be present in 10 different modules each with 10 different versions, the
generator only needs to create a single template which would apply to all 100 module version.

##Creating Test Generators

Test generators utilize the Java API provided by the [Core Platform](core_platform.md).

##Executing Test Generators

Test generators that are provided by an Artifact Provider as a [Test Generator Artifact](artifacts.md#test-generator-artifacts)
can be discovered by the Core Platform and executed using the [dtfexec](dtfexec.md) command line tool.