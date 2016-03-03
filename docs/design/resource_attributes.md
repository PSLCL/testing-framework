#Resource Attributes

Resource attributes may be specified as part of the [Bind](template_commands.md#bind) command in order to specify additional requirements for the requested attribute. Custom attributes can be used. However, all resource providers must support the following resource attributes.

##Machine Attributes
 
* `pslcl.dtf.resource.machine.cores=2` - Requests that the returned machine instance has at least 2 CPU cores. A range(inclusive) may also be specified(`2-4`) to request that the returned machine instance has at least 2 CPU cores but no more than 4.
* `pslcl.dtf.resource.machine.memory-size=2.0` - Request that the returned machine instance has at least 2.0 GB of system memory. A range(inclusive) may also be specified(`1.0-3.0`) to request that the returned machine instance has at least 1.0 GB of system memory but no more than 3.0 GB.
* `pslcl.dtf.resource.machine.disk-size=8.0` - Request that the returned machine instance has at least 8.0 GB of disk space. A range(inclusive) may also be specified(`8.0-16.0`) to request that the returned machine instance has at least 8.0 GB of system memory but no more than 16.0 GB.
* `pslcl.dtf.resource.machine.image-id=`- Request a specific image id. For example, this attribute could be used to request a specific ec2 AMI or Docker image.
* `pslcl.dtf.resource.machine.os=` - Request that the returned machine instance has the specified operating system such as `windows` or `linux`.
* `pslcl.dtf.resource.machine.hostname` - May be used as an [Attribute Value Reference](value_references.md#attribute-values) by a test generator to use the hostname of a machine instance as a parameter in a program-based command.

##Person Attributes
* `pslcl.dtf.resource.person.email=` - Request that the returned Person resource has the specified email address.
