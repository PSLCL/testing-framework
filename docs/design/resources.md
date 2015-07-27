#Resources

A primary design goal of the Testing Framework is to allow flexible resource handling, including the ability to easily define and 
use new types of resources.

##Resource Definitions

A resource is identified uniquely by a hash. The hash itself is created from a textual definition of a resource in a standardized 
format that a [Resource Provider](resource_providers.md) can use to recreate the resource itself.

Note that a resource is not an instance of a resource, and the hash of a resource matches any instance of the resource.

During the execution of a test run, resource instances are requested. This request ultimately includes a hash and attributes. 
The hash identifies the type of resource; the attributes further qualify the specific resource desired. The request is memorized, 
and during resource instantiation the request will be made to the [Resource Providers](resource_providers.md) which create actual 
instances of the resource. In addition to the required attributes, resource instances may have additional non-required attributes.

With this definition, a [Resource Provider](resource_providers.md) is free to define any resource that it desires by picking some 
definition and hashing it. It is then free to define whatever attributes and values apply to the resource. A 
[Resource Provider](resource_providers.md) is also free to “learn” about the types of requests that are being made, and to optimize 
the creation (or even cache) resources that meet that definition. For example, assume that windows machines are difficult to create. 
A resource provider may watch the rate at which these machines are requested and keep some ready to quickly respond to requests. 
If the rate goes down, then it may release the resources.

##Resource Types

There are three main types of resources that the Testing Framework can interact with: Machine, Network 
and Person. Other resources may be defined that do not interact directly with the Testing Framework but instead simply provide 
attributes.

For example, a certain class of resource (identified by its hash) may be understood by the harness as 
a machine that it can operate against (connect to networks, deploy software to). A different hash may be understood equally as a 
resource, but not as one that the test platform can interoperate with other than getting its attributes. An external server might be 
a resource with an attribute for the IP address, but the test platform could not deploy additional software on the resource.

##Resource Reuse

At the completion of a test run, it may be possible for the Testing Framework to reuse a resource instance if it has 
not been permanently altered by using the program-based [*configure*](template_commands.md) command. The resource instance must also
have a matching hash and all required attributes specified by the resource.

As an example, assume that a generic “machine” has hash H1. All resources in the platform that are machines would share this hash, 
and by asking for that hash (with no additional requirements) any machine could be returned - Windows, Linux, anything. That is not 
very useful, but it does form the basis for asking for specific things. Assume that these machines have an attribute for “platform”, 
and the request is for H1 plus attribute “platform” with value “x86_64-pc-linux-gnu”. If the platform currently has several 
machines with this hash and a matching "platform" attribute that are available and a template requires one then the platform can 
provide it directly without going to the resource providers. 
