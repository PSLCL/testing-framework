#Template Commands

[Templates](templates.md) are composed of steps, each of which includes a Command that must be executed by the 
[Test Runner Service](test_runner_service.md).

##Bind

The *bind* Command requests a [Resource](resources.md) Instance. The format of the line following the command is a hash 
(64 characters), followed by a space, followed by an attribute list. The reference to the Resource Instance is the line index in 
the file, zero based.

##Include

The *include* Command requests a [Template](templates.md) Instance. The format of the line following the command is a hash 
(64 characters). The reference to the Template Instance is the line index in the file, zero based. Include commands are considered
completed once the [Template](templates.md) has been fully instantiated. 

##Deploy

The *deploy* Command requests an [Artifact](artifacts.md) be deployed to a [Machine](resources.md) Instance. The format of the line 
following the command is a Machine reference, followed by a space, followed by an [Artifact Reference](templates.md#artifact-references). 

Deploy commands are considered completed once the [Artifact](artifacts.md) is fully deployed to the the [Machine](resources.md) Instance.

##Inspect

The *inspect* Command requests that a [Person](resource.md) follow a set of HTML formatted instructions sent generally via email. 
Relevant [Artifacts](artifacts.md) may also be included in an archive named attachments.tar.gz and attached to the email. 
The format of the line following the command is a Person reference, followed by a space, followed by the Hash of 
the instructions, followed by [Artifact References](templates.md#artifact-references) to be included as attachments also separated by spaces. The 
[Person](resources.md) performing the inspection must manually enter the results. Inspect commands are considered completed
once the instructions and attachments have been sent to the [Person](resource.md).

##Connect

The *connect* command requests a connection between a [Machine](resources.md) and a [Network](resources.md). The format of the line 
following the command is a Machine reference, followed by a space, followed by a Network reference. Connect commands are considered
completed once the Machine is connected to the Network.

##Program-Based Commands

There are several ways to run programs, each with a different effect. Each command requires a Machine reference as the first 
argument, followed by the URL-encoded name of the program target, followed by URL-encoded parameters.

Programs may require parameters whose values are unknown when the [Template](templates.md) is created. 
[Value References](value_references.md) may be used to refer to values that may not be known until the test is being run,
such as the IP address of a server.

###Configure

The program *configure* Command requests that a program be run that modifies the Machine in such a way that it cannot be 
rolled back and reused. Configure commands are considered completed once the program has completed its run.

###Start

The program *start* Command requests that a program be run that should stay running for the duration of the [Template](templates.md) 
Instance. It can be stopped, and restarted, and it cannot modify the Machine. [Templates](templates.md) may contain only a single
*start* command. Start commands are considered completed once the program has been started.

###Run

The program *run* Command requests that a program be run that should complete on its own. The Run Command completes the test run, 
with the program result determining the test result. This cannot modify the machine. If a test run contains multiple *run* or 
*run-forever* commands, the test run will fail if any of the programs fail. Run commands are considered completed once the program 
has completed its run.

###Run-Forever

The program *run-forever* Command requests that a program be run that will not complete until told to stop. The run-forever command 
completes the test when it completes, with the program result determining the test result. This cannot modify the Machine. If a 
test run contains multiple *run* or *run-forever* commands, the test run will fail if any of the programs fail. Run-Forever commands 
are considered completed once the program has been stopped.