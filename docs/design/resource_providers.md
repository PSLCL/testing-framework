#Resource Providers

Resource Providers provide [Resource](resources.md) instances to the Testing Framework.

##Instantiating Resources

A Resource Provider is free to define any resource that it desires by picking some resource name. It is then free to 
define whatever attributes and values apply to the resource. Resource Providers are also free to “learn” about 
the types of requests that are being made, and to optimize the creation (or even cache) resources that meet that definition. 
For example, assume that windows machines are difficult to create. A resource provider may watch the rate at which these machines are
requested and keep some ready to quickly respond to requests. If the rate goes down, then it may release the resources.

##Reserving Resources

The [Test Runner Service](test_runner_service.md) must be able to reserve a resource instance. A resource provider with limited resources must 
allocate resources based on a first-come first-served basis. Reserved resource instances may not be bound except by the owner of the
reservation.

Holding a reservation on a resource instance signifies that the resource provider guarantees that it has sufficient resources 
available to instantiate the requested resource instance. Although the resource provider may use a resource reservation as a 
trigger to begin instantiating the resource instance, it is not guaranteed to be instantiated until the resource has been bound.

Resource reservations last until the resource has been bound or until some configurable timeout period has passed.

##Template Commands

Each resource provider must be able to understand and execute [Template Commands](template_commands.md) that relate to the types
of resources that it provides. For example, A machine resource provider must be able to [Deploy](template_commands.md#deploy) 
[Artifacts](artifacts.md) and execute [program-based commands](template_commands.md#template-based-commands).


