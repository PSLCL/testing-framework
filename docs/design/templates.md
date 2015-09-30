#Templates

Templates represent a sequence of steps that the Testing Framework must execute to generate a result. They are managed by the Testing 
Framework and are identified by a Hash. They are created by running [Test Generators](test_generators.md). Templates are just definitions and 
do not actually utilize resources until they are instantiated, which involves instantiating any required resources and templates, as well as 
running specified steps, or [Commands](template_commands.md), on those resources.

##Template Definitions

A template is defined by the steps it takes to instantiate the template, sorted in an order that produces 
the same result each time when created by a [Test Generator](test_generators.md). Specifically, the definition is:

1.	A text string, ASCII, with UNIX line termination. No commenting or blank lines are allowed, and each line has a line terminator.
2.	Each line begins with a set ID, followed by a space, followed by a command, followed by another space, followed by parameters 
determined by the command. Parameters are URL encoded and separated by spaces. [*Include*](template_commands.md#include) commands 
do not require a set ID.
3.	Each hash is 64 upper-case hex characters, no quotes.
4.	Attribute strings are A=V&A=V, with each V being URL encoded.

Templates are considered identical if they have the same definition and are identifed by the same hash.

###Step Ordering

Step ordering must be consistent in order to maximize [Template Reuse](templates.md#template-reuse). Steps are organized into
sets. 

The first set is composed of all [*include*](template_commands.md#include) commands in the template and may not include any other
command. The following sets are organized by set ID's, starting with '0', each set incrementing by 1, and may contain steps with 
any command except for [*include*](template_commands.md#include).

Each set of commands is sorted in increasing alphanumeric order.

###Resource References

Templates are free to request multiple instances of the same [Resource](resources.md) or Template using identical [Bind](template_commands.md#bind)
or [Include](template_commands.md#include) commands. Note that because these commands will be identical, the sort order of each individual 
command is ambiguous. Because multiple instances of the same Resource or Template are equivalent, identical lines in the Template are 
interchangeable.

Specific Resource Instances may be referenced by other commands using the line index, zero based, of the step containing the 
[Bind](template_commands.md#bind) command. Resources bound by an included Template may be referenced using the line index of the 
[Include](template_commands.md#include) command followed by a forward slash and then by an index in the included Template, 
continuing until a Bind command is referenced.

###Artifact References

Artifact references are required by [Inspect](template_commands.md#inspect) and [Deploy](template_commands.md#deploy) template commands. Artifact 
references are the destination filename and Hash, separated by a space. Note that the version is not specified, 
so if the file does not change from version to version then the reference will be identical.

##Template Building

Templates can be constructed by hand, but are more easily created programmatically, such as using a 
[Test Generator](test_generator.md). Part of the [Core Platform](core_platform.md) is an API implementation that will interact 
with the database to programmatically create templates and test runs.

Each template must contain a [Bind](template_commands.md#bind) or [Include](template_commands.md#include). All other commands in a 
template require a [Resource](resource.md) reference, and these references require either the resource be 
included directly in the template ([bind](template_commands.md#bind)) or indirectly via another template 
([include](template_commands.md#include)).

##Template Instantiation

A template is instantiated by the [Test Runner Service](test_runner_service.md) in order to use it as a test run or to be used by 
another template. Template instances continue to exist until the [Test Runner Service](test_runner_service.md) releases them. 

A templates is instantiated by executing all of its steps. Steps within the same set may be executed in parallel. All steps
in a set must [complete](template_commands.md) before the next set of steps are executed. This means that each step should be in a 
set that is executed after any sets containing steps that it is dependent on. For example, A [*deploy*](template_commands.md#deploy) 
command should belong to a set that is executed after the set containing the [*bind*](template_commands.md#bind) command for the 
[Machine](resources.md) that it references.

##Template Reuse

Consider each test run as equivalent to a template that forms the root of a tree. This top-level tree will not be equivalent to any 
other template (because each test run is distinct), *but the templates used to form it may very well be reusable*.

Results of a test run must not be changed by the reuse of a template. Consider the initial state of a template to be its state at 
the moment it was fully instantiated. In order to keep template reuse from affecting the outcome of a test run, a template may only
be reused if it is at its initial state.

In the simplest case, a parent template includes a child template but does not change its state. The [Run](template_commands.md#run), 
[Run-Forever](template_commands.md#run-forever) and [Start](template_commands.md#start) commands, when completed, must leave the 
Machine in the same state as before the program was run. A parent may run a program on a resource bound by a child template without 
altering its state. When the parent template instance is no longer needed and releases all of its resources and child templates, the 
child template may immediately be reused by another parent template.

If, instead, the parent template changes the state of the child template, then those changes must be reversed before the child
template can be reused. For example, if the parent template [Deploys](template_commands.md#deploy) an [Artifact](artifacts.md)
to a Machine bound by the child template, that artifact must be deleted before the template may be reused. Likewise, if the
parent template adds a Machine bound by the child template to a Network, the Machine must be removed from that Network.

It should be noted that the [Configure](template_commands.md#configure) command may irreversibly change the state of a
Machine. If a parent template *configures* a resource bound by a child template, then the state of that template is also
irreversibly changed and may not be reused. The parent template, however, may be reused as long as its state is not irreversibly
changed because the configure command was a part of the series of steps required to reach its initial state.