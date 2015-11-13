#Template Commands

[Templates](templates.md) are composed of steps, each of which includes a command that must be executed by the 
[Test Runner Service](test_runner_service.md).

##Bind

The bind command requests a [Resource](resources.md) instance. The format of the line following the command is a resource
name(e.g. 'machine', 'network', 'person'), followed by a space, followed by an attribute list. The reference to the resource instance is the line index in 
the file, zero based.

##Include

The include command requests a [Template](templates.md) instance. The format of the line following the command is a hash 
(64 characters). The reference to the template instance is the line index in the file, zero based. Include commands are considered
completed once the template has been fully instantiated. 

##Deploy

The deploy command requests an [Artifact](artifacts.md) be deployed to a [Machine](resources.md) Instance. The format of the line 
following the command is a Machine reference, followed by a space, followed by an [Artifact Reference](templates.md#artifact-references). 

Deploy commands are considered completed once the artifact is fully deployed to the the machine instance.

##Inspect

The inspect command requests that a [Person](resources.md) follow a set of HTML formatted instructions sent generally via email. 
Relevant [Artifacts](artifacts.md) may also be included in an archive named attachments.tar.gz and attached to the email. 
The format of the line following the command is the person [Resource Reference](templates.md#resource-references), followed by a space, 
followed by the Hash of the instructions, followed by [Artifact References](templates.md#artifact-references) to be included as attachments 
also separated by spaces. The person performing the inspection must manually enter the results. Inspect commands are considered completed
once the instructions and attachments have been sent to the person.

##Connect

The connect command requests a connection between a [Machine](resources.md) and a [Network](resources.md). The format of the line 
following the command is the machine [Resource Reference](templates.md#resource-references), followed by a space, followed by a network 
resource reference. Connect commands are considered completed once the Machine is connected to the Network.

##Program-Based Commands

There are several ways to run programs, each with a different effect. Each command requires a Machine reference as the first 
argument, followed by the URL-encoded name of the program target, followed by URL-encoded parameters.

Programs may require parameters whose values are unknown when the [Template](templates.md) is created. 
[Value References](value_references.md) may be used to refer to values that may not be known until the test is being run,
such as the IP address of a server.

###Configure

The program configure command requests that a program be run that modifies the machine in such a way that it cannot be 
rolled back and reused. Configure commands are considered completed once the program has completed its run.

###Start

The program start command requests that a program be run that should stay running for the duration of the [Template](templates.md) 
Instance. It cannot modify the Machine. Templates may contain only a single
start command. Start commands are considered completed once the program has been started.

###Run

The program run command requests that a program be run that should complete on its own. The run command completes the test run, 
with the program result determining the test result. This cannot modify the machine. If a test run contains multiple run or 
run-forever commands, the test run will fail if any of the programs fail. Run commands are considered completed once the program 
has completed its run.

###Run-Forever

The program run-forever command requests that a program be run that will not complete until told to stop. The run-forever command 
completes the test when it completes, with the program result determining the test result. This cannot modify the Machine. If a 
test run contains multiple run or run-forever commands, the test run will fail if any of the programs fail. Run-Forever commands 
are considered completed once the program has been stopped.