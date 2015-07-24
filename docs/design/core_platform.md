#Core Platform

The Core Platform provides Java APIs for [Test Generators](test_generators.md) and [Artifact Providers](artifact_providers.md). 
The Core Platform also provides the command-line tool [dtfexec](dtfexec.md) which gives user access to various commands such as 
executing [Test Generators](test_generators.md) and running tests.

##Use Cases

The Core Platform provides the following use cases through the Java API to developers of [Test Generators](test_generators.md). 
Requirements and use cases for the dtfexec command-line tool may be found [here](dtfexec.md).

###A Developer [Binds](template_commands.md) a [Resource](resources.md)

A [Test Generator](test_generators.md) Developer requests resources needed for the tests by issuing a 
[bind command](template_commands.md). When the [Test Templates](templates.md) are generated, [bind commands](template_commands.md)) 
are automatically prioritized over other commands which require use of the bound [Resource](resources.md).

###A [Test Generator](test_generators.md) Developer [Deploys](template_commands.md) an [Artifact](artifacts.md)

A Developer requests that artifacts be deployed to a [Machine Resource](resources.md) by issuing a 
[Deploy command](template_commands.md). When the [Test Templates](templates.md) are generated, 
[deploy commands](template_commands.md)) are automatically prioritized over other commands which require use of the 
deployed [artifact](artifacts.md).

###A Developer [Connects](template_commands.md) a [Machine](resources.md) to a [Network](resources.md)

A [Test Generator](test_generators.md) Developer Requests That a [Machine Resource](resources.md) be Attached to a 
[Network Resource](resources.md) by issuing a [Connect command](template_commands.md). When the [Test Templates](templates.md) are 
generated, [connect commands](template_commands.md)) are automatically prioritized over other commands which require that the machine
be connected to the network(e.g. A program-based command which takes the machine's IP Address as a parameter).

###A Developer Executes a [Program](template_commands.md)

A [Test Generator](test_generators.md) Developer requests that a program be executed on a [Machine Resource](resources.md) by
issuing a [program-based command](template_commands.md).

###A Developer Declares That a [Command](template_commands.md) Depends on Another

A [Test Generator](test_generators.md) Developer may specify ordering of command execution by declaring that one command depends on 
one or more other commands.



