#Core Platform

The Core Platform provides Java APIs for [Test Generators](test_generators.md) and [Artifact Providers](artifacts.md#artifact-providers). 
The Core Platform also provides the command-line tool [dtfexec](dtfexec.md) which gives user access to various commands such as 
executing test generators and running tests.

##Generator Use Cases

The Core Platform provides the following use cases through the Java API to developers of [Test Generators](test_generators.md). 
Requirements and use cases for the dtfexec command-line tool may be found [here](dtfexec.md).

###A Developer Binds a Resource

A [Test Generator](test_generators.md) developer requests resources needed for the tests by issuing a 
[Bind](template_commands.md#bind) command. When the [Templates](templates.md) are generated, bind commands 
are automatically prioritized over other commands which require use of the bound [Resource](resources.md).

###A Test Generator Developer Deploys an Artifact

A [Test Generator](test_generators.md) developer requests that [Artifacts](artifacts.md) be deployed to a machine [Resource](resources.md) by 
issuing a [Deploy](template_commands.md#deploy) command. When the [Templates](templates.md) are generated, 
deploy commands are automatically prioritized over other commands which require use of the 
deployed artifact.

###A Developer Connects a Machine to a Network

A [Test Generator](test_generators.md) developer requests that a machine [Resource](resources.md) be attached to a 
network resource by issuing a [Connect](template_commands.md#connect) command. When the [Templates](templates.md) are 
generated, connect commands are automatically prioritized over other commands which require that the machine
be connected to the network(e.g. A program-based command which takes the machine's IP Address as a parameter).

###A Developer Executes a Program

A [Test Generator](test_generators.md) developer requests that a program be executed on a machine [Resource](resources.md) by
issuing a [program-based command](template_commands.md#program-based-commands).

###A Developer Declares That a Command Depends on Another

A [Test Generator](test_generators.md) developer may specify ordering of command execution by declaring that one command depends on 
one or more other commands.



