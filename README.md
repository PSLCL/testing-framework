Panasonic Distributed Testing Framework
=======================================

The Panasonic Distributed Testing Framework enables automated testing across multiple software versions and network topologies.

Testing functionality and interoperability between many versions of software components can be daunting. Since the number of 
tests grows exponentially with each version, the challenge of creating adequate test plans may even discourage modularized 
software development. The Panasonic Distributed Testing Framework helps solve this problem by automating the task of 
generating and executing tests in a scalable way.

##Generate Tests

Tests can be generated through scripts that, when executed, would create tests that exercise functionality across a 
combination of different versions or implementations of a software component. For example, a test generator script 
could specify that for every combination of programs called “bin/client” and “bin/server,” allocate a Linux machine
and deploy the programs, then run server, run client, and check the results. If there were three libraries in different 
languages and five releases of each library, the generator would create 225 tests that would cover every combination.

##Allocate Resources

Tests require resources. A test that requires a Linux machine could be executed on an Amazon AWS Instance, a Docker 
container, or on a server running in your office. A cluster of AWS instances running Docker could grow and shrink 
with demand, hosting as many Docker containers as needed. Resources can also include people, so the system can define 
and schedule manual tests as well as automated tests.

##Execute Tests

Once tests have been generated, a test executor prioritizes them, and then allocates resources and creates the necessary 
networks to produce the test results. Tests may be prioritized based on a release schedule or even by availability of resources.
