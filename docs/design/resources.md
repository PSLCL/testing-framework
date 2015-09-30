#Resources

A primary design goal of the Testing Framework is to allow flexible resource handling, including the ability to easily define and 
use new types of resources.

##Resource Definition

A resource is uniquely identified by its name and attributes. Note that a resource is not an instance of a resource. A resource
defined by its name and attributes may match any number of resource instances which have the same name and a set of
attributes containing at least those specified by the resource.

During the execution of a test run, resource instances are requested. This request ultimately includes the name and attributes. 
The name identifies the type of resource; the attributes further qualify the specific resource desired. The request is memorized, 
and during resource instantiation the request will be made to the [Resource Providers](resource_providers.md) which create actual 
instances of the resource. In addition to the required attributes, resource instances may have additional non-required attributes.

##Resource Types

There are three main types of resources that the Testing Framework can interact with: Machine, Network 
and Person. Other resources may be defined that do not interact directly with the Testing Framework but instead simply provide 
attributes.

For example, a certain class of resource (identified by its name) may be understood by the harness as 
a machine that it can operate against (connect to networks, deploy software to). A different name may be understood equally as a 
resource, but not as one that the test platform can interoperate with other than getting its attributes. An external server might be 
a resource with an attribute for the IP address, but the test platform could not deploy additional software on the resource.

##Resource Reuse

At the completion of a test run, it may be possible for the [Test Runner Service](test_runner_service.md) to reuse a 
[Resource](resources.md)Instance if it has not been permanently altered by using the program-based 
[*configure*](template_commands.md) command. 

Results of a test run must not be changed by the reuse of a resource instance. Consider the initial state of a 
[Resource](resources.md) Instance to be its state at the moment it was bound. In order to keep Resource Instance reuse from
affecting the outcome of a test run, a Resource Instance may only be reused if it is at its initial state.

In the simplest case, a [Template](templates.md) binds a [Resource](resources.md) Instance but does not change its state. A Template 
may run a program on a bound Resource Instance without altering its state. The [Run](template_commands.md#run), 
[Run-Forever](template_commands.md#run-forever) and [Start](template_commands.md#start) commands, 
when completed, must leave the Machine in the same state as before the program was run. When the Template Instance is no longer 
needed and releases the Resource Instance, the Resource Instance may immediately be reused by another parent template.

If, instead, the [Template](templates.md) changes the state of the [Resource](resources.md) Instance, then those changes must be 
reversed before the Resource Instance can be reused. For example, if the Template [Deploys](template_commands.md#deploy) an 
[Artifact](artifacts.md) to a Machine Instance, that Artifact must be deleted before the Machine Instance may be reused. Likewise, 
if the Template adds a Machine Instance to a Network, the Machine Instance must be removed from that Network.

It should be noted that the [Configure](template_commands.md#configure) command may irreversibly change the state of a
Machine Instance. If a [Template](templates.md) *configures* a Machine Instance, then the Machine Instance must not be reused.