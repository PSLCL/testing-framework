#dtfexec

*dtfexec* is a tool that gives command-line access to Testing Framework commands such as executing 
[Test Generators](test_generators.md) and running tests via the [Core Platform](core_platform.md).

##Use Cases

###A User Synchronizes the Local Artifact Cache

A User synchronizes the local [Artifact](artifacts.md) cache with an [Artifact Provider](artifacts.md#artifact-providers).

###A User executes known [Test Generators](test_generators.md)

A User specifies that [Test Generators](test_generators.md) known by the [Artifact Provider](artifact.md#artifact-providers) 
should be executed.

###A User Runs a Test

A User specifies that a test should be run. The Core Platform adds the test to the test
queue.

###A User Runs Prioritized Tests

A User specifies that a number of unspecified high priority tests should be run. The [Core Platform](core_platform.md) queries 
the database in order to determine the highest priority tests and adds them to the test queue.

###A User Queries the Status of a Test Run

A User queries the current status of a test run.

###A User Cancels a Test Run

A User cancels a test run that has been queued or is currently running.

###A User Publishes Results for a Test Run

A User publishes results for a completed test run.

