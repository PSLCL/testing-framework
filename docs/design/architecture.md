#Panasonic Distributed Testing Framework architecture

The Distributed Testing Framework has three main components: QA Portal, Core Platform and the Test Runner Service.

![Architecture](img/architecture.png?raw=true "Architecture")

###QA Portal

The [QA Portal](qa_portal.md) provides a REST API and website for viewing and creating test plans and controlling certain aspects of the Testing 
Framework.

The QA Portal periodically tells the Core Platform to do work such as execute any new [Test Generators](test_generators.md) or execute the 
highest priority tests.

###Core Platform

The [Core Platform](core_platform.md) provides the [dtfexec](dtfexec.md) tool which gives command-line access to several Testing Framework
commands including executing [Test Generators](test_generators.md) and running tests. The Core Platform also provides a Java API for 
test generators.

The Core Platform initiates test runs by submitting tests to the Test Queue.

###Test Runner Service

The [Test Runner Service](test_runner_service.md) subscribes to the Test Queue and executes tests. This is done by executing 
[Template](templates.md) scripts and involves managing [Resources](resources.md) via [Resource Providers](resource_providers.md), 
executing programs and sending tasks to testers for manual tests. 

Test results are published and can be viewed on the QA Portal website.