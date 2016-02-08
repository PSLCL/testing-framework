Panasonic Distributed Testing Framework - Platform
===

The Panasonic Distributed Testing Framework Platform contains the command line tool(dtfexec) and Test Runner Service. These instructions assume that the Portal has already been installed.

##Build Instructions
This project uses Apache Ant and Apache Ivy to build and resolve dependencies. In addition, a Java Development Kit (JDK) of 1.8 or greater is required.

To build, make sure any dependencies are available in a configured Ivy repository, and execute `ant` in the testing-framework/platform directory.

The included ivysettings-example.xml is configured to publish the platform to the local Portal's ivy repository. Use it by either including it or renaming it to ivysettings.xml.

##`dtfexec`
The platform includes a command line tool for interacting with the testing framework. Once the platform has been built, dtfexec is used by executing the following command from the testing-framework/platform directory:

`$ bin/dtfexec`

###Commands
The following commands are supported:

 - synchronize - synchronize the artifact providers with the database.
 - result - add a result to the database.
 - run - extract tests from the database and run them.
 - populate - populate the system with made-up testing data.

Execute `bin/dtfexec --help` for more information.

###dtfexec Configuration
dtfexec utilizes the Portal's configuration found at testing-framework/portal/config/config.js. Ensure that this file exists and is in the correct relative path. 

##Test Runner Service  
The platform includes the Test Runner Service which handles requests to start tests. Once the platform has been built, the Test Runner Service can be launched by executing the following command from the testing-framework/platform directory:

`$ bin/dtfrunner`

The service is launched with jsvc.

### Runtime Requirements
The following tools must be installed on production systems (or be accessible)

1. jsvc
2. [STAF](http://prdownloads.sourceforge.net/staf/STAF3424-setup-linux-amd64-NoJVM.bin?download) version 'v3.4.24'.

### Install STAF
STAF is installed via a STAF's InstallAnywhere installation application.

`$ cd ~`

`$ wget http://prdownloads.sourceforge.net/staf/STAF3424-setup-linux-amd64-NoJVM.bin`

`$ chmod +x STAF3424-setup-linux-amd64-NoJVM.bin`

`$ sudo ./STAF3424-setup-linux-amd64-NoJVM.bin`

Take all the defaults (skip "Allow STAF to Register" with a 0 if desired).
edit the configuration file /usr/local/staf/bin/STAF.cfg

1. add "option ConnectTimeout=60000" to the end of both "interface ..." lines.
2. add a new line with "trust level 5 default" 

STAF is started with /usr/local/startSTAFProc.sh
a nohup.out log file is created at /home/ec2-user

Setup your machine to auto start STAF on reboot (i.e. /etc/rc.local)

### Create AWS AMI's

Multiple images will likely need to be created depending on the needs of the tests running on the system. Requirements like system architecture, operating system, JRE version etc. may differ from test to test. Each needed unique combination of these requirements will need a separate AMI.

**Linux/Windows**

1. Launch an EC2 instance with the desired public or private AMI 
	* if windows add this to user data without quotes: "<script\>\\STAF\\startSTAFProc.bat</script\>"
2. Install STAF
3. If windows, follow the Windows Sysprep steps outlined below
4. Save this EC2 instance as AMI
5. Modify the Test Runner Service Configuration to use the new AMI.

**Windows Sysprep**

For windows the EC2 image must be "Sysprep'ed" see http://docs.aws.amazon.com/AWSEC2/latest/WindowsGuide/ami-create-standard.html before saving the image.

1. Bring up a remote desktop client to the EC2 image establish in the above steps.
2. Run the "EC2ConfigService Settings" application
3. In the "General" tab	
	* select "Set Computer Name" check box.
	* select "User Data" check box.
	* select "Event Log" check box.
	* select "Wallpaper Information" check box.

4. In the "Image" tab:

	* create another user and assign that user administrator rights.
	* select "Keep Existing" radio button.
	* click the "Shutdown with Sysprep" button.

###Test Runner Service Configuration

The default configuration file path is testing-framework/platform/config/dtf.properties. Example configuration files may be found at testing-platform/platform/example-config/. See the example configuration for additional config documentation.

