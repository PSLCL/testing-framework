Sample Test
===

Included are several sample test generators which can be used to validate the Distributed Testing Framework installation.

##Build Instructions

This project uses Apache Ant and Apache Ivy to build and resolve dependencies. In addition, a Java Development Kit (JDK) of 1.8 or greater is required.

To build, make sure any dependencies are available in a configured Ivy repository, and execute `ant dist-src dist` in the testing-framework/sample-test directory in order to build the sample test generators. To publish the test generators, execute `ant publish`.

The included ivysettings-example.xml is configured to publish the platform to the local Portal's ivy repository if run on the same machine. Use it by either including it or renaming it to ivysettings.xml.

##Running the Tests

Using the Portal website, create a test plan with tests for each of the following included generator scripts:

* TestRunGenerator.sh
* TestConfigGenerator.sh
* TestInspectGenerator.sh

The generator scripts will be executed on the next 'synchronize' which will create the individual test instances.

After the test instances have been generated, request that they be run by executing the following command from the testing-framework/platform directory:

`bin/dtfexec run --manual <test_id>`


