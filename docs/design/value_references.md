#Value References

There are times when a reference to the value of a particular attribute or property is required. The only place this is allowed 
is in the parameter lists of the [program-based commands](template_commands.md#program-based-commands).

##Attribute Values

[Resource](resources.md) Instances may have attributes in addition to those specified in the [Bind](template_commands.md#bind) command.
Execution-time attribute values may be referenced using the following format:

	*$(attribute <resource reference> <attribute name>)*

The string will be replaced at execution time with the value of the attribute, or with nothing if the attribute is not defined.

##IP Addresses

When a [Connect](template_commands.md#connect) command attaches a machine to a network, the IP address may be referenced using the following
format:

	*$(ip <machine reference> <network reference>)