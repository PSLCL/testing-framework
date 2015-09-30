#Test Runner Service

The Test Runner Service is responsible for the execution of queued tests. This involves instantiating the test's
top-level [Template](templates.md), working with [Resource Providers](resource_providers.md) to manage resources, and publishing
results to the database.

##Instantiating Templates

A [Template](templates.md) is considered instantiated once all of its steps' [commands](template_commands.md) have completed. If
the template is the top-level template for a test, then results are [published](#publishing-results). Steps must be executed
in the order outlined in the [Template](templates.md#template-instantiation) documentation.

When faced with many templates waiting to be instantiated, but limited [Resources](resource.md), the Test
Runner Service must be able to allocate the available resources using a method that is both fair and efficient. Higher priority 
tests which require many(or rare) resources must not be blocked due to the required resources being allocated to lower priority 
tests which require fewer resources. Lower priority tests should also be allowed to run if required Resources are available and
not needed by higher priority tests.

##Prioritization

Although message ordering is not guaranteed, the Test Runner Service may consider the order in which tests are received from the 
queue to approximate test run priority. However, other factors may also be considered such as component release date and 
[Resource](resources.md) availability.

##Reusing Templates

Template Instances may be used by multiple tests if they meet the criteria for Template reuse outlined in the 
[Template](templates.md#template-reuse) documentation. The Test Runner Service must track reversible changes made
to Template Instances and their associated resources and undo those changes, if there are any, before they may be reused.

Template reuse increases the efficiency of the Testing Framework as it avoids duplication of work. Tests that would
otherwise be lower priority may be executed before higher priority tests in order to maximize Template reuse. However, Template 
reuse must not indefinitely starve higher priority tests of needed resources.

##Reusing Resources

Resource Instances may be bound by multiple templates if they meet the criteria for Resource reuse outlined in the
[Resource](resources.md#resource-reuse) documentation. The Test Runner Service must track reversible changes made to
resource instances and undo those changes, if there are any, before they may be reused.

A resource instance which meets the correct criteria may be reused by any [Template](templates.md) which binds a
resource with the same name and requires a subset of the Resource Instance's attributes.

As an example, assume that a generic [Machine](resources.md) has the name "machine". All resources in the Testing Framework that are machines 
would share this name, and by asking for that name (with no additional requirements) any Machine could be returned - Windows, Linux, 
anything. That is not very useful, but it does form the basis for asking for specific things. Assume that these machines have an 
attribute for “platform”, and the request is for "machine" plus attribute “platform” with value “x86_64-pc-linux-gnu”. If the Test Runner
Service currently has several Machine Instances with this name and a matching "platform" attribute that are available and a 
template requires one then the Test Runner Service can provide it directly without going to the 
[Resource Providers](resource_providers.md). 

##Publishing Results

When a top-level [Template](templates.md) for a test has been fully instantiated, the Test Runner Service must inspect the results
of any [run](template_commands.md#run) or [run-forever](template_commands.md#run-forever) commands to determine the overall result of
the test. 

If the test includes an [inspect](template_commands.md#inspect) command, its result, as reported by a [Person](Resources.md), must also 
be incorporated into the overall result of the test.

If any run, run-forever or inspect command fails then the overall test fails.

